package sort;
import java.io.*;
import java.text.*;
import java.util.*;
import jam.util.*;
import jam.io.*;
import jam.global.RunInfo;
import jam.sort.stream.*;

/**
 * This class knows how to handle Oak Ridge tape format.  It extends
 * EventOutputStream, adding methods for reading events and returning them
 * as int arrays which the sorter can handle.
 *
 * @version	0.5 April 98
 * @author 	Dale Visser, Ken Swartz
 * @see         EventOutputStream
 * @since       JDK1.1
 */
public class TextEventOutputStream extends EventOutputStream  {
  int status;
  int parameter;
  public int value;

  /**
   * Default constructor.
   */
  public TextEventOutputStream(){
    super();
  }

  /**
   * Creates the output stream with the given event size.
   *
   * @param eventSize the number of parameters per event
   */
  public TextEventOutputStream(int eventSize){
    super(eventSize);
  }
  /**
   * Writes the header block.
   *
   * @exception   EventException    thrown for errors in the event stream
   */
  public void writeHeader() throws EventException {
    //not implemented
  }

  /**
   * Implemented <code>EventOutputStream</code> abstract method.
   *
   * @exception EventException thrown for unrecoverable errors
   */
  public void writeEvent(int[] input) throws EventException {
    String s="";
    for (short i=0;i<eventSize;i++){
      s=s+input[i]+" ";
    }
    s=s+"\n";
    try{
      dataOutput.writeBytes(s);
    } catch (IOException ie) {
      throw new EventException("Can't write event: "+ie.toString());
    }
  }

  /**
   * Writes out a event in the L002 format
   * Implemented <code>EventOutputStream</code> abstract method.
   *
   * @exception EventException thrown for unrecoverable errors
   */
  public void writeEvent(short[] input) throws EventException {
    String s="";
    for (short i=0;i<eventSize;i++){
      s=s+input[i]+" ";
    }
    s=s+"\n";
    try{
      dataOutput.writeBytes(s);
    } catch (IOException ie) {
      throw new EventException("Can't write event: "+ie.toString());
    }
  }

  /**
   * Check for end of run word
   */
  public boolean isEndRun(short dataWord){
    return false;//???
  }

  /**
   *Write the character that signifies the end of the run data.
   */
  public void writeEndRun() throws EventException {
    //not implemented
  }

  /**
   * Checks whether a valid parameter number (should be 1 to 512 according to ORNL documentation).
   */
  /*protected boolean isValidParameterNumber(short number){
  //System.out.println("valid param '"+number+"': "+((number>=1 )&& (number <= 512)));
  return ((number>=1 )&& (number <= 512));
  }*/

  /**
   * Converts a short to a valid parameter marker for the stream.
   */
  /*protected short parameterMarker(short number) {
  return (short)((EVENT_PARAMETER_MARKER | number)&0xFFFF);
  }*/
}
