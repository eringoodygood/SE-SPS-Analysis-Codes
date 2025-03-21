package dwvisser;
import java.io.*;

/**
 * Class for reading in spreadsheet-style text files.
 */
public class ColumnarTextReader extends InputStreamReader {

    StringReader sr;
    LineNumberReader lnr;

    public ColumnarTextReader(InputStream is) throws FileNotFoundException, IOException {
        super(is);
        lnr=new LineNumberReader(this);
        nextLine();
    }
    
    
    
    public void nextLine() throws IOException {
        sr=new StringReader(lnr.readLine());
    }
    
    public String readString(int len) throws IOException{
        char [] temp;
        temp = new char[len];
        sr.read(temp);
        return new String(temp).trim();
    }
    
    public int readInt(int len) throws IOException {
        return Integer.parseInt(readString(len));
    }
        
    public double readDouble(int len) throws IOException{
        return Double.parseDouble(readString(len));
    }
    
    public void skipChars(int len) throws IOException{
        readString(len);
    }
        
}
