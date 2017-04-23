import java.util.LinkedList;
//best fit
public class FreeSpaceTable{

  private int size;
  private int address;

  public FreeSpaceTable(int size, int address){
    this.size = size;
    this.address = address;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getAddress() {
    return address;
  }

  public void setAddress(int address) {
    this.address = address;
  }
}
