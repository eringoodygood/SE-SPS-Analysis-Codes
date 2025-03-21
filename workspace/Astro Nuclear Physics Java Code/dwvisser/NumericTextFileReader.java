/*
 * NumericTextFileReader.java
 *
 * Created on May 14, 2001, 2:36 PM
 */

package dwvisser;
import java.io.*;

/**
 * Simple class to encapsulate reading text files containing only numbers.
 * No formatting other than numbers separated by whitespace is assumed.
 * It is up to the user to interpret the numbers read properly.
 * @author  dwvisser
 * @version
 */
public class NumericTextFileReader extends Object {

    private StreamTokenizer tokenizer;
    private double [] numberList = new double[5000];
    private int size=0;
    private int nextAccess=0;

    /** Creates new NumericTextFileReader */
    public NumericTextFileReader(File input) {
        try {
            tokenizer=new StreamTokenizer(new FileReader(input));
            tokenizer.eolIsSignificant(false); //treat end of line as white space
            processFile();
        } catch (IOException ioe) {
            System.err.println(getClass().getName()+" constructor: "+ioe);
        }
    }

    private void processFile() throws IOException {
        tokenizer.nextToken();
        while (tokenizer.ttype != StreamTokenizer.TT_EOF){
            if (tokenizer.ttype != StreamTokenizer.TT_NUMBER) throw new
            IOException(this.getClass().getName()+".read(): Wrong token type: "
            +tokenizer.ttype);
            numberList[size]=tokenizer.nval;
            size++;
            tokenizer.nextToken();
        }
    }

    public int getSize(){
        return size;
    }
    
    public double read() {
        nextAccess++;
        return numberList[nextAccess-1];
    }


}