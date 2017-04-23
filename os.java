import java.util.ArrayList;
import java.util.LinkedList;


public class os{

  public static ArrayList<PCB> jobTable;
  public static ArrayList<FreeSpaceTable> freeSpaceTable;

  private static LinkedList<PCB> readyQ;
  private static LinkedList<PCB> diskQ;

  private static PCB runningJob;
  private static PCB IOJob;
  private static PCB drumJob;

  private static final int TIMESLICE = 1000; //TBD
  private static final int MAX_MEMORY_SIZE = 100;
  private static final int MAX_JOB_NUM = 50;


  public static void startup(){
    jobTable = new ArrayList<PCB>();

    freeSpaceTable = new ArrayList<FreeSpaceTable>();
    FreeSpaceTable initTable = new FreeSpaceTable(MAX_MEMORY_SIZE, 1);
    freeSpaceTable.add(initTable);

    readyQ = new LinkedList<PCB>();
    diskQ = new LinkedList<PCB>();

    sos.ontrace();
    //sos.offtrace();
  }

  public static void Crint(int a[], int p[]){
    if (jobTable.size() >= MAX_JOB_NUM){
      return;   // return??????
    }
    BookKeeping(p[5]);

    PCB newJob = new PCB(p);
    // TODO: placed in memory???
    jobTable.add(newJob);
    readyQ.add(newJob);

    Swapper();
    Scheduler();
    RunJob(a, p);
  }

  public static void Dskint(int a[], int p[]){
    BookKeeping(p[5]);
    // Disk interrupt.
    // At call: p [5] = current time
    IOJob = null;
    PCB job = diskQ.remove();
    if(!job.isDone()){
      jobTable.add(job);
    }

    Swapper();
    Scheduler();
    RunJob(a, p);
  }

  public static void Drmint(int a[], int p[]){
    BookKeeping(p[5]);
    // Drum interrupt.
    // At call: p [5] = current time

    Swapper();
    Scheduler();
    RunJob(a, p);
  }

  public static void Tro(int a[], int p[]){
    BookKeeping(p[5]);
    // Timer-Run-Out.
    // At call: p [5] = current time

    Swapper();
    Scheduler();
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

    } else if (a[0] == 6){
      IOJob = diskQ.remove();
      sos.siodisk(IOJob.getJobNumber());
    } else if (a[0] == 7){

    }

    Swapper();
    Scheduler();
    RunJob(a, p);
  }

  public static void BookKeeping(int time){
    //stop the job and put back to readyQ
    if (runningJob != null){
      runningJob.updateTimeUsed(time);
      readyQ.addFirst(runningJob);
      runningJob = null;
    }
  }

  public static void Swapper(){

  }

  //FIFO
  public static void Scheduler(){
    //runningJob = readyQ.remove();
    // TODO: start counting time
  }

  public static void RunJob(int[] a, int[] p){
    if(runningJob != null){
      a[0] = 2;
      //p[2] = //memory address
      //p[3] = //job size
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

  public static void FSTInsertJob(int size){
    if(checkAvalibilityFST(size)){
      for(int i = 0; i < freeSpaceTable.size(); i++){
        if(freeSpaceTable.get(i).getSize() > size){
          freeSpaceTable.get(i).setSize(freeSpaceTable.get(i).getSize() - size);
          freeSpaceTable.get(i).setAddress(freeSpaceTable.get(i).getAddress() + size);
          break;
        } else if (freeSpaceTable.get(i).getSize() == size){
          freeSpaceTable.remove(i);
          break;
        }
      }

      sortFST();
    }
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
        if (freeSpaceTable.get(i).getAddress() == 1) {
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
}
