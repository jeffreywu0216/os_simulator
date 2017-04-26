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

  private static int blockCount;

  private static final int MAX_MEMORY_SIZE = 100;

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

  public static boolean checkAvalibilityFST(PCB job){
    if(freeSpaceTable.isEmpty())
      return false;

    for(int i = 0; i < freeSpaceTable.size(); i++) {
      if (freeSpaceTable.get(i).getSize() >= job.getJobSize())
        return true;
    }
    return false;
  }

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

  public static void FSTRemoveJob(int size, int address){
    FreeSpaceTable releaseFST = new FreeSpaceTable(size, address);
    freeSpaceTable.add(releaseFST);

    updateFST();
  }

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

  public static void sortFST(){
    for (int i = freeSpaceTable.size() - 1; i >= 1; i--){
      if(freeSpaceTable.get(i).getSize() < freeSpaceTable.get(i - 1).getSize()){
        FreeSpaceTable temp = freeSpaceTable.get(i);
        freeSpaceTable.set(i, freeSpaceTable.get(i - 1));
        freeSpaceTable.set(i - 1, temp);
      }
    }
  }

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
