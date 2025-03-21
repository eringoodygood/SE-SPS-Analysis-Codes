package dwvisser.nuclear.swing;

/**
* Interface that Dialogs returning an array of numerical values must obey.
*
* @author Dale Visser
* @version 1.0
*/
public interface ValuesDialog {

  public double [] getValues();

  public void display();

}

