public class PCB{

  private int jobNumber;
  private int priority;
  private int jobSize;
  private int maxCpuTime;
  private int currentTime;

  private int processStartTime;
  private int timeUsed;

  private boolean isIO;
  private boolean isBlocked;

  public PCB(int[] p){
    this.jobNumber = p[1];
    this.priority = p[2];
    this.jobSize = p[3];
    this.maxCpuTime = p[4];
    this.currentTime = p[5];

    this.timeUsed = 0;
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
}
