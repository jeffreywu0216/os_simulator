import java.util.ArrayList;
import java.util.LinkedList;


public class os{

  public static ArrayList<PCB> jobTable;

  private static LinkedList<PCB> readyQ;
  private static LinkedList<PCB> diskQ;

  private static PCB runningJob;
  private static PCB IOJob;
  private static PCB drumJob;

  private final int TIMESLICE = 1000; //TBD

  public static void startup(){
    jobTable = new ArrayList<PCB>();
    readyQ = new LinkedList<PCB>();
    diskQ = new LinkedList<PCB>();

    sos.ontrace();
    //sos.offtrace();
  }

  public static void Crint(int a[], int p[]){
    if (jobTable.size() >= 50){
      return;   // return??????
    }
    BookKeeping();

    PCB newJob = new PCB(p);
    // TODO: placed in memory???
    jobTable.add(newJob);
    readyQ.add(newJob);

    Swapper();
    Scheduler();
    RunJob(a, p);
  }

  public static void Dskint(int a[], int p[]){
    BookKeeping();
    // Disk interrupt.
    // At call: p [5] = current time
    PCB job = diskQ.remove();
    if(job.getMaxCpuTime() > 0){
      jobTable.add(job);
    }

    Swapper();
    Scheduler();
    RunJob(a, p);
  }

  public static void Drmint(int a[], int p[]){
    BookKeeping();
    // Drum interrupt.
    // At call: p [5] = current time

    Swapper();
    Scheduler();
    RunJob(a, p);
  }

  public static void Tro(int a[], int p[]){
    BookKeeping();
    // Timer-Run-Out.
    // At call: p [5] = current time

    Swapper();
    Scheduler();
    RunJob(a, p);
  }

  public static void Svc(int a[], int p[]){
    BookKeeping();
    // Supervisor call from user program.
    // At call: p [5] = current time
    // a = 5 => job has terminated
    // a = 6 => job requests disk i/o
    // a = 7 => job wants to be blocked until all its pending
    // I/O requests are completed
    if (a[0] == 5){

    } else if (a[0] == 6){

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
      p[2] = //memory address
      p[3] = //job size
      p[4] = TIMESLICE;
    } else {
      a[0] = 1;
    }
  }
}
