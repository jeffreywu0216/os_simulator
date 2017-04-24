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

  private static final int TIMESLICE = 10;
  private static final int MAX_MEMORY_SIZE = 100;
  private static final int MAX_JOB_NUM = 50;

  public static void startup(){
    jobTable = new ArrayList<PCB>();

    freeSpaceTable = new ArrayList<FreeSpaceTable>();
    FreeSpaceTable initTable = new FreeSpaceTable(MAX_MEMORY_SIZE, 0);
    freeSpaceTable.add(initTable);

    readyQ = new LinkedList<PCB>();
    diskQ = new LinkedList<PCB>();
    drumInQ = new LinkedList<PCB>();
    drumOutQ = new LinkedList<PCB>();

    runningJob = null;
    lastJob = null;
    IOJob = null;
    drumJob = null;

    sos.ontrace();
    //sos.offtrace();
  }

  public static void Crint(int a[], int p[]){
//    if (jobTable.size() >= MAX_JOB_NUM){
//      return;   // return??????
//    }
    BookKeeping(p[5]);

    PCB newJob = new PCB(p);
    jobTable.add(newJob);
//    if(checkAvalibilityFST(p[3])){
//      newJob.setCoreAddress(FSTInsertJob(p[3]));
//      newJob.setInCore(true);
//      readyQ.add(newJob);   //put on readyQ if in memory?
//    } else {
    drumInQ.add(newJob);
//    }

    Swapper();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  public static void Dskint(int a[], int p[]){
    BookKeeping(p[5]);

    if(IOJob != null) {
      IOJob.ioCountMinusOne();
      if (IOJob.getIoCount() <= 0 && !IOJob.isBlocked()) {
        IOJob.setBlocked(false);
      }

      if (!IOJob.isInCore()) {    //if not in memory, move in momory
        drumInQ.add(IOJob);
      } else if (IOJob.getIoCount() == 0 && !IOJob.isBlocked()) {  //if not waiting for IO, add to readyQ
        readyQ.add(IOJob);
      }
      IOJob = null;
    }
    // Disk interrupt.
    // At call: p [5] = current time

    if(!diskQ.isEmpty()) {    //be in memory?
      for(int i = 0; i < diskQ.size(); i++) {
        if(diskQ.get(i).isInCore()) {
          IOJob = diskQ.remove(i);
          IOJob.ioCountMinusOne();
          sos.siodisk(IOJob.getJobNumber());
        }
      }
    }

    Swapper();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  public static void Drmint(int a[], int p[]){
    BookKeeping(p[5]);
    readyQ.add(drumJob);
    drumJob = null;
    // Drum interrupt.
    // At call: p [5] = current time

    Swapper();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  public static void Tro(int a[], int p[]){
    BookKeeping(p[5]);

    if(lastJob.isDone()){       //can the job used up all time but sill waiting for IO?
      terminate(lastJob);
    }
    // Timer-Run-Out.
    // At call: p [5] = current time


    Swapper();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  public static void Svc(int a[], int p[]){
    BookKeeping(p[5]);
    // Supervisor call from user program.
    // At call: p [5] = current time
    // a = 5 => job has terminated
    // a = 6 => job requests disk i/o
    // a = 7 => job wants to be blocked until all its pending
    // I/O requests are completed
    if (a[0] == 5){
      terminate(lastJob);
    } else if (a[0] == 6){
      lastJob.ioCountPlusOne();
      diskQ.add(lastJob);
      readyQ.remove(lastJob);
    } else if (a[0] == 7){
      if(lastJob.getIoCount() != 0) {
        lastJob.setBlocked(true);
        readyQ.remove(lastJob);
      }
    }

    Swapper();
    Scheduler(p[5]);
    RunJob(a, p);
  }

  public static void BookKeeping(int time){
    //stop the job and put back to readyQ
    if (runningJob != null){
      runningJob.updateTimeUsed(time);
      readyQ.addFirst(runningJob);
      lastJob = runningJob;
      runningJob = null;
    }
  }

  public static void Swapper(){
    //swap in
    if(drumJob == null && !drumInQ.isEmpty()){
      for(int i = 0; i < drumInQ.size(); i++){
        if(checkAvalibilityFST(drumInQ.get(i).getJobSize())){
          drumJob = drumInQ.remove(i);
          drumJob.setCoreAddress(FSTInsertJob(drumJob.getJobSize()));
          drumJob.setInCore(true);
          sos.siodrum(drumJob.getJobNumber(), drumJob.getJobSize(), drumJob.getCoreAddress(), 0);
          break;
        }
      }
    }
    // swap out
    if(drumJob == null && !drumOutQ.isEmpty()){
      for(int i = 0; i < drumOutQ.size(); i++){
        if(runningJob != drumOutQ.get(i)) {
          drumJob = drumOutQ.remove(i);
          FSTRemoveJob(drumJob.getJobSize(), drumJob.getCoreAddress());
          drumJob.setInCore(false);
          sos.siodrum(drumJob.getJobNumber(), drumJob.getJobSize(), drumJob.getCoreAddress(), 1);
          break;
        }
      }
    }
  }

  //FCFS
  public static void Scheduler(int time) {
    for (int i = 0; i < readyQ.size(); i++) {
      if (!readyQ.get(i).isBlocked() && !readyQ.isEmpty()) {
        runningJob = readyQ.remove(i);
        runningJob.setProcessStartTime(time);
        break;
      }
    }
  }

  public static void RunJob(int[] a, int[] p){
    if(runningJob != null){
      a[0] = 2;
      p[2] = runningJob.getCoreAddress();
      p[3] = runningJob.getJobSize();
      p[4] = TIMESLICE;
    } else {
      a[0] = 1;
    }
  }

  public static boolean checkAvalibilityFST(int size){
    if(freeSpaceTable.get(freeSpaceTable.size() - 1).getSize() >= size){
      return true;
    }
    return false;
  }

  public static int FSTInsertJob(int size){
    int returnAddress = 0;
    if(checkAvalibilityFST(size)){
      for(int i = 0; i < freeSpaceTable.size(); i++){
        if(freeSpaceTable.get(i).getSize() > size){
          returnAddress = freeSpaceTable.get(i).getAddress();
          freeSpaceTable.get(i).setSize(freeSpaceTable.get(i).getSize() - size);
          if(freeSpaceTable.get(i).getAddress() == 0) {
            freeSpaceTable.get(i).setAddress(freeSpaceTable.get(i).getAddress() + size - 1);
          } else {
            freeSpaceTable.get(i).setAddress(freeSpaceTable.get(i).getAddress() + size);
          }
          break;
        } else if (freeSpaceTable.get(i).getSize() == size){
          returnAddress = freeSpaceTable.get(i).getAddress();
          freeSpaceTable.remove(i);
          break;
        }
      }
      sortFST();
    }
    return returnAddress;
  }

  public static void FSTRemoveJob(int size, int address){
    FreeSpaceTable releaseFST = new FreeSpaceTable(size, address);
    freeSpaceTable.add(releaseFST);

    updateFST();
    sortFST();
  }

  public static void updateFST(){
    boolean changeFlag = true;
    while(changeFlag) {
      changeFlag = false;
      for (int i = 0; i < freeSpaceTable.size(); i++) {
        int targetAddress = freeSpaceTable.get(i).getSize() + freeSpaceTable.get(i).getAddress();
        if (freeSpaceTable.get(i).getAddress() == 0) {
          targetAddress -= 1;
        }
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

  // will be called whenever FST is modify, so only one element not in order
  // only needs to iterate once
  public static void sortFST(){
    for (int i = freeSpaceTable.size() - 1; i >= 1; i--){
      if(freeSpaceTable.get(i).getSize() < freeSpaceTable.get(i - 1).getSize()){
        freeSpaceTable.add(i + 1, freeSpaceTable.get(i - 1));
        freeSpaceTable.remove(i - 1);
      }
    }
  }

  public static void terminate(PCB job){
    readyQ.remove(job);
    jobTable.remove(job);
    if(job.isInCore()) {
      drumOutQ.add(job);
    }
  }
}
