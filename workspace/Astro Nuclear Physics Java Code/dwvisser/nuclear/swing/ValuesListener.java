package dwvisser.nuclear.swing;

/**
* Class to listen for changes in values.
*
* @author Dale Visser
* @version
*/
public interface ValuesListener {

  public void receiveValues(ValuesChooser vc, double [] values);

}
