/*
 * TextOutputter.java
 *
 * Created on June 18, 2001, 12:08 PM
 */

package dwvisser.analysis;
import java.io.*;

/**
 * Gives a framework for subclasses to use System.out to creat their output
 * @author  dwvisser
 * @version 
 */
public  class TextOutputter extends Object {

    static public String DEFAULT = "default";
    PrintStream defaultOutput = System.out;
    FileOutputStream fos;
    
    /** Creates new TextOutputter */
    public TextOutputter(String output) throws FileNotFoundException {
        setOutput(output);
    }
    
    void setOutput(String output) throws FileNotFoundException {
        if (output.equals(DEFAULT)) {
            //do nothing
        } else {
            fos = new FileOutputStream(output);
            System.setOut(new PrintStream(fos));
            defaultOutput.println("Output now going to '"+output+"'");
        }
    }
    
    public void closeOutput() throws IOException {
        if (fos !=null){
            fos.flush();
            fos.close();
        }
    }
    
    public void revertToDefaultOutput(){
        System.setOut(defaultOutput);
    }

}