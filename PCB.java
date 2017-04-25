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

  private boolean inOrOut;
  private boolean isTerminated;
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

    //this.isIO = false;
    this.isBlocked = false;
    this.inCore = false;
  }

  public void updateTimeUsed(int time){
    timeUsed = timeUsed + (time - processStartTime);
  }

  public boolean isDone(){
    if(maxCpuTime - timeUsed <= 0){
      return true;
    }
    return false;
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

//  public boolean isIO() {
//    return isIO;
//  }
//
//  public void setIO(boolean IO) {
//    isIO = IO;
//  }

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

  public void ioCountPlusOne() {
    this.ioCount++;
  }

  public void ioCountMinusOne() {
    this.ioCount--;
  }

  public boolean isTerminated() {
    return isTerminated;
  }

  public void setTerminated(boolean terminated) {
    isTerminated = terminated;
  }

  public boolean isInOrOut() {
    return inOrOut;
  }

  public void setInOrOut(boolean inOrOut) {
    this.inOrOut = inOrOut;
  }
}
