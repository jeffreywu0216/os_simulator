public class PCB{

  private int jobNumber;
  private int priority;
  private int jobSize;
  private int maxCpuTime;
  private int currentTime;

  private int processStartTime;
  private int timeUsed;
  private int coreAddress;
  private int ioCount;
  private int nextUpgradePriorityTime;  //upgrage prioriity if pass is in over 100000

  private boolean inOrOut;              //tells drumInt weather the job is swaping in or swaping out
  private boolean isTerminated;         //is true if job is terminated but io pending
  private boolean isBlocked;
  private boolean inCore;

  public PCB(int[] p){
    this.jobNumber = p[1];
    this.priority = p[2];
    this.jobSize = p[3];
    this.maxCpuTime = p[4];
    this.currentTime = p[5];

    this.timeUsed = 0;
    this.ioCount = 0;
    this.nextUpgradePriorityTime = this.currentTime + 100000; //upgrage prioriity if pass is in over 100000

    this.isBlocked = false;
    this.inCore = false;
  }

  //updateing the time used for the job
  public void updateTimeUsed(int time){
    timeUsed = timeUsed + (time - processStartTime);
  }
  //check weather the job is done
  public boolean isDone(){
    return (maxCpuTime - timeUsed <= 0) ? true : false;
  }
  //returns the remaining time for the job
  public int remainTime(){
    return maxCpuTime - timeUsed;
  }

  public int getJobNumber() {
    return jobNumber;
  }

  public void setJobNumber(int jobNumber) {
    this.jobNumber = jobNumber;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public int getJobSize() {
    return jobSize;
  }

  public void setJobSize(int jobSize) {
    this.jobSize = jobSize;
  }

  public int getMaxCpuTime() {
    return maxCpuTime;
  }

  public void setMaxCpuTime(int maxCpuTime) {
    this.maxCpuTime = maxCpuTime;
  }

  public int getCurrentTime() {
    return currentTime;
  }

  public void setCurrentTime(int currentTime) {
    this.currentTime = currentTime;
  }

  public int getProcessStartTime() {
    return processStartTime;
  }
  //set the processStartTime to current time whenever a job start running on CPU
  public void setProcessStartTime(int processStartTime) {
    this.processStartTime = processStartTime;
  }

  public int getTimeUsed() {
    return timeUsed;
  }

  public void setTimeUsed(int timeUsed) {
    this.timeUsed = timeUsed;
  }

  public int getCoreAddress() {
    return coreAddress;
  }

  public void setCoreAddress(int coreAddress) {
    this.coreAddress = coreAddress;
  }

  public boolean isBlocked() {
    return isBlocked;
  }

  public void setBlocked(boolean blocked) {
    isBlocked = blocked;
  }

  public boolean isInCore() {
    return inCore;
  }

  public void setInCore(boolean inCore) {
    this.inCore = inCore;
  }

  public int getIoCount() {
    return ioCount;
  }

  public void setIoCount(int ioCount) {
    this.ioCount = ioCount;
  }
  // add one to ioCount
  public void ioCountPlusOne() {
    this.ioCount++;
  }
  // minus one to ioCount
  public void ioCountMinusOne() {
    this.ioCount--;
  }

  public boolean isTerminated() {
    return isTerminated;
  }

  public void setTerminated(boolean terminated) {
    isTerminated = terminated;
  }
  //return true if job just swap in, false if swap out
  public boolean isInOrOut() {
    return inOrOut;
  }

  public void setInOrOut(boolean inOrOut) {
    this.inOrOut = inOrOut;
  }

  public int getNextUpgradePriorityTime() {
    return nextUpgradePriorityTime;
  }
  //reset time after upgrading the job
  public void setNextUpgradePriorityTime(int nextUpgradePriorityTime) {
    this.nextUpgradePriorityTime = nextUpgradePriorityTime;
  }
}
