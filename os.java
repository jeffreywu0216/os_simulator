/*
 * Simulation of os
 */
import java.util.ArrayList;
import java.util.LinkedList;

public class os{
  private static ArrayList<PCB> jobTable;
  private static ArrayList<FreeSpaceTable> freeSpaceTable;

  private static LinkedList<PCB> readyQ;
  private static LinkedList<PCB> diskQ;
  private static LinkedList<PCB> drumInQ;
  private static LinkedList<PCB> drumOutQ;

  private static PCB runningJob;
  private static PCB lastJob;
  private static PCB IOJob;
  private static PCB drumJob;

  private static int blockCount;  //counts how many jobs are blocked

  private static final int MAX_MEMORY_SIZE = 100;

  /*
   * initializing all variables
   *
   * and insert the initial data of size: 100, address: 0 into freeSpaceTable
   */
  public static void startup(){
    jobTable = new ArrayList<PCB>();

    freeSpaceTable = new ArrayList<FreeSpaceTable>();
    FreeSpaceTable initTable = new FreeSpaceTable(MAX_MEMORY_SIZE, 0);
    freeSpaceTable.add(initTable);

    readyQ= new LinkedList<PCB>();
    diskQ = new LinkedList<PCB>();
    drumInQ = new LinkedList<PCB>();
    drumOutQ = new LinkedList<PCB>();

    runningJob = null;
    lastJob = null;
    IOJob = null;
    drumJob = null;

    blockCount = 0;

    sos.offtrace();
  }

  /*
   * when new job comes in, call BookKeeping to stop the runningJob and calculate timeUsed for the job
   *
   * initialize the new job and add it in jobTable and drumInQ
   *
   * then check if something is available for swaping
   * and schedule the next running job and retern the jobs info to sos
   */
  public static void Crint(int a[], int p[]){
    BookKeeping(p[5]);
    PCB newJob = new PCB(p);

    if(!jobTable.contains(newJob))
      jobTable.add(newJob);
    if(!drumInQ.contains(newJob))
      drumInQ.add(newJob);

    Swapper();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  /*
   * is called when a job finishes IO
   *
   * call BookKeeping
   *
   * decrease the ioCount and remove the job from diskQ
   *
   * if ioCount is <= 0, and terminated (only occurs when the job is terminated already but still waiting for IO)
   * call terminate() to terminate the job
   *
   * if ioCount is <= 0, and the job is blocked, unblocked the job and decrease one blockCount and add the job back to readyQ
   *
   * if ioCount is <= 0, and not terminated or blocked, than add the job back to readyQ
   *
   * set IOJob = null so can do the next IO.
   *
   * call ioScheduler to schedule the next job to do IO
   * call scheduler to schedule the next job to run
   * call RunJob to run the job that was selected by Scheduler
   *
   */
  public static void Dskint(int a[], int p[]){
    BookKeeping(p[5]);
    IOJob.ioCountMinusOne();
    diskQ.remove(IOJob);

    if (IOJob.getIoCount() <= 0) {
      if (IOJob.isTerminated()) {
        terminate(IOJob);
      } else if (IOJob.isBlocked()) {
        IOJob.setBlocked(false);
        blockCount--;
        readyQ.add(IOJob);
      } else {
        readyQ.add(IOJob);
      }
    }

    IOJob = null;

    ioScheduler();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  /*
   * is called when a job fininshed swaping in or out of memory
   *
   * call bookKeeping to stop the running job and some regular checking
   *
   * check if the job is swaping in or swaping out
   *
   * if swap in, remove from drumInQ, setInCore to true, and add the job back to readyQ
   * if swap out, remove from drumOutQ, setInCore to false, and add the job to drumInQ
   *
   * set the drumJob to null so system can pick the next job to do drum
   *
   */
  public static void Drmint(int a[], int p[]){
    BookKeeping(p[5]);

    if (drumJob.isInOrOut()) {
      drumInQ.remove(drumJob);
      drumJob.setInCore(true);
      readyQ.add(drumJob);
    } else {
      drumOutQ.remove(drumJob);
      drumJob.setInCore(false);
      FSTRemoveJob(drumJob.getJobSize(), drumJob.getCoreAddress());
      drumInQ.add(drumJob);
    }

    drumJob = null;

    Swapper();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  /*
   * called when a job is has used up its time slice
   *
   * check if the job is done, if it is, call terminate() to terminate the job
   * otherwise, add the job to readyQ if not already in
   *
   */
  public static void Tro(int a[], int p[]){
    BookKeeping(p[5]);

    if(lastJob.isDone()){
      terminate(lastJob);
    } else if(!readyQ.contains(lastJob)){
        readyQ.add(lastJob);
    }

    ioScheduler();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  /*
   * SVC is called when job request for IO
   *
   * do BookKeeping at the begining and Schedular and RunJob at the last like all other interrupts
   *
   * check the value of a[0]
   *
   * if equal to 5 (job terminated signal), then terminate the job
   *
   * if equal to 6 (job request for io signal), then add one to the job's ioCount,
   * and add the job to diskQ, and call ioScheduler
   *
   * if equal to 7 (job request for blocking), then check if there is io pending, do nothing if there is no io pending
   * if there is io pending, set the job blocked status to true,
   * add one the the total blockCount, remove the job from readyQ
   * and if the job is not doing IO and blockCount > 1, add the job on the drumOutQ to swap out from memory
   *
   */
  public static void Svc(int a[], int p[]){
    BookKeeping(p[5]);

    if (a[0] == 5){
      terminate(lastJob);
    } else if (a[0] == 6){
      lastJob.ioCountPlusOne();
      diskQ.add(lastJob);
      ioScheduler();
    } else if (a[0] == 7){
      if(lastJob.getIoCount() > 0) {
        lastJob.setBlocked(true);
        blockCount++;
        if (lastJob != IOJob && blockCount > 1) {
          lastJob.setInCore(false);
          drumOutQ.add(lastJob);
          Swapper();
        }
        readyQ.remove(lastJob);
      }
    }

    Scheduler(p[5]);
    RunJob(a, p);
  }

  /*
   * called after each interrupt occurs
   *
   * if there is a running job, update the time it used, and add it back to readyQ if not already in the readyQ
   *
   * set the lastJob to runningJob and set runningJob to null
   *
   * then update priority for all jobs in jobTable if reached nextUpgradePriotityTime and priority is greater than 1
   */
  public static void BookKeeping(int time){
    if (runningJob != null){
      runningJob.updateTimeUsed(time);
      if(!readyQ.contains(runningJob))
        readyQ.addFirst(runningJob);

      lastJob = runningJob;
      runningJob = null;
    }
    for (int i = 0; i < jobTable.size(); i++){
      if(time > jobTable.get(i).getNextUpgradePriorityTime() && jobTable.get(i).getPriority() > 1){
        jobTable.get(i).setPriority(jobTable.get(i).getPriority() - 1);
        jobTable.get(i).setNextUpgradePriorityTime(jobTable.get(i).getNextUpgradePriorityTime() + 100000);
      }
    }
  }

  /*
   * first check if there is job doing drum or there is job waiting for drum in drumOutQ, if no job doing drum,
   * and job waiting for drum in drumOutQ, call sos.siodrum for the fisrt job in drumOutQ.
   *
   * then check if there is job waiting for drum in drumInQ
   * go through the whole list to find a job that can fit into the memory and has higher priority than others
   * and shortest remaining time and not blocked. then do sos.siodrum for that job.
   *
   */
  public static void Swapper(){
    // swap out
    if(drumJob == null && !drumOutQ.isEmpty()){
      for(int i = 0; i < drumOutQ.size(); i++){
        drumJob = drumOutQ.get(i);
        drumJob.setInOrOut(false);
        sos.siodrum(drumJob.getJobNumber(), drumJob.getJobSize(), drumJob.getCoreAddress(), 1);
        break;
      }
    }
    //swap in
    if(drumJob == null && !drumInQ.isEmpty()){
      PCB temp = null;
      for(int i = 0; i < drumInQ.size(); i++){
        if(drumInQ.get(i) != null && checkAvalibilityFST(drumInQ.get(i))){
          if(temp == null)
            temp = drumInQ.get(i);
          else if(temp.getPriority() >= drumInQ.get(i).getPriority()
                  && temp.remainTime() > drumInQ.get(i).remainTime()
                  && !drumInQ.get(i).isBlocked())
            temp = drumInQ.get(i);
        }
      }
      if(temp != null){
        drumJob = temp;
        drumJob.setCoreAddress(FSTInsertJob(drumJob.getJobSize()));
        drumJob.setInOrOut(true);
        sos.siodrum(drumJob.getJobNumber(), drumJob.getJobSize(), drumJob.getCoreAddress(), 0);
      }
    }
  }

  /*
   * goes through the readyQ and find a job that is not blocked, and is inCore, and not used up its time,
   * and has higher priority and shortest remaining time
   * than select that job as the next running job
   */
  public static void Scheduler(int time) {
    PCB temp = null;

    for (int i = 0; i < readyQ.size(); i++) {
      if (!readyQ.get(i).isBlocked() && !readyQ.isEmpty()
              && readyQ.get(i).isInCore() && !readyQ.get(i).isDone()) {
        if(temp == null)
          temp = readyQ.get(i);
        else if (temp.getPriority() >= readyQ.get(i).getPriority()
                && temp.remainTime() > readyQ.get(i).remainTime())
          temp = readyQ.get(i);
      }
    }

    if(temp != null){
      runningJob = temp;
      readyQ.remove(temp);
      runningJob.setProcessStartTime(time);
    }
  }

  /*
   * Acutally running the job by setting both a and p, and return to sos
   *
   * if there is job to run, set a[0] to 2, and p[2] to the starting core address
   * and p[3] to job size, and p[4] to time slice (time remaining in my program)
   *
   * otherwise, set a[0] to 1
   */
  public static void RunJob(int[] a, int[] p){
    if(runningJob != null){
      a[0] = 2;
      p[2] = runningJob.getCoreAddress();
      p[3] = runningJob.getJobSize();
      p[4] = runningJob.getMaxCpuTime() - runningJob.getTimeUsed();
    } else {
      a[0] = 1;
    }
  }

  /*
   * decides which io job to be run the next
   *
   * go through the entire list of diskQ and find the one that is inCore
   * and has higher priority and shortest remaining time
   *
   * and call sos.siodisk to do IO for the job
   */
  public static void ioScheduler(){
    if(!diskQ.isEmpty() && IOJob == null) {
      PCB temp = null;

      for(int i = 0; i < diskQ.size(); i++) {
        if (diskQ.get(i).isInCore()){
          if(temp == null)
            temp = diskQ.get(i);
          else if (temp.getPriority() >= diskQ.get(i).getPriority() && temp.remainTime() > diskQ.get(i).remainTime())
            temp = diskQ.get(i);
        }
        else {
          if(!drumInQ.contains(IOJob))
            drumInQ.add(IOJob);
          Swapper();
        }
      }

      if(temp != null) {
        IOJob = temp;
        sos.siodisk(IOJob.getJobNumber());
      }
    }
  }

  /*
   * checks the avalibility of the freeSpaceTable
   *
   * it accepts a job and go through the list of free space table, if the job can fit in memory, return ture,
   *
   * otherwise, return false
   */
  public static boolean checkAvalibilityFST(PCB job){
    if(freeSpaceTable.isEmpty())
      return false;

    for(int i = 0; i < freeSpaceTable.size(); i++) {
      if (freeSpaceTable.get(i).getSize() >= job.getJobSize())
        return true;
    }
    return false;
  }

  /*
   * insert a job into fst
   *
   * gets a size as param, and go through the list of fst and find the first element that can fit the job size (best fit)
   * then fix the size and address of that particular fst element, then call updateFST() to update FST to the right order
   *
   * and return the address of the job
   */
  public static int FSTInsertJob(int size){
    int returnAddress = 0;

    for(int i = 0; i < freeSpaceTable.size(); i++){
      if(freeSpaceTable.get(i).getSize() >= size) {
        returnAddress = freeSpaceTable.get(i).getAddress();
        freeSpaceTable.get(i).setSize(freeSpaceTable.get(i).getSize() - size);
        freeSpaceTable.get(i).setAddress(freeSpaceTable.get(i).getAddress() + size);
        break;
      }
    }

    updateFST();
    return returnAddress;
  }

  /*
   * removes a job from FST
   *
   * adds the size and address to FST and call updateFST to fix FST to the correct order
   */
  public static void FSTRemoveJob(int size, int address){
    FreeSpaceTable releaseFST = new FreeSpaceTable(size, address);
    freeSpaceTable.add(releaseFST);

    updateFST();
  }

  /*
   * this function combines all fst elements if they are adjacent to each other
   *
   * it first remove all elements that have the size of 0
   *
   * then, go through freeSpaceTable to see if any job is adjacent to another job,
   * if there is, add the size from the higher address one to the lower address one,
   * and remove the higher address one from freeSpaceTable
   *
   * than call sortFST to sort freeSpaceTable in best fit order
   */
  public static void updateFST(){
    int k = 0;
    while (k < freeSpaceTable.size()){
      if(freeSpaceTable.get(k).getSize() == 0){
        freeSpaceTable.remove(k);
        k--;
      }
      k++;
    }

    boolean changeFlag = true;
    while(changeFlag) {
      changeFlag = false;

      for (int i = 0; i < freeSpaceTable.size(); i++) {
        int targetAddress = freeSpaceTable.get(i).getSize() + freeSpaceTable.get(i).getAddress();
        for (int j = 0; j < freeSpaceTable.size(); j++) {
          if(freeSpaceTable.get(j).getAddress() == targetAddress){
            freeSpaceTable.get(i).setSize(freeSpaceTable.get(i).getSize() + freeSpaceTable.get(j).getSize());
            freeSpaceTable.remove(j);
            changeFlag = true;
          }
        }
      }
    }
    sortFST();
  }

  /*
   * sorting FST in best fit order
   *
   * smaller size in the front and larger ones in the back of the list
   */
  public static void sortFST(){
    for (int i = freeSpaceTable.size() - 1; i >= 1; i--){
      if(freeSpaceTable.get(i).getSize() < freeSpaceTable.get(i - 1).getSize()){
        FreeSpaceTable temp = freeSpaceTable.get(i);
        freeSpaceTable.set(i, freeSpaceTable.get(i - 1));
        freeSpaceTable.set(i - 1, temp);
      }
    }
  }

  /*
   * terminates the job
   *
   * remove from readyQ
   *
   * if still waiting for IO, set terminated to true, so the job can be terminated once finished IO
   *
   * otherwise, remove from the jobTable and remove from core
   */
  public static void terminate(PCB job){
    readyQ.remove(job);

    if(job.getIoCount() > 0){
      job.setTerminated(true);
    } else {
      job.setInCore(false);
      jobTable.remove(job);
      FSTRemoveJob(job.getJobSize(), job.getCoreAddress());
    }
  }
}
