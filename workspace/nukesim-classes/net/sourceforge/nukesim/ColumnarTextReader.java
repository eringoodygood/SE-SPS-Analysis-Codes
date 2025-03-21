/***************************************************************
 * Nuclear Simulation Java Class Libraries
 * Copyright (C) 2003 Yale University
 * 
 * Original Developer
 *     Dale Visser (dale@visser.name)
 * 
 * OSI Certified Open Source Software
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the University of Illinois/NCSA 
 * Open Source License.
 * 
 * This program is distributed in the hope that it will be 
 * useful, but without any warranty; without even the implied 
 * warranty of merchantability or fitness for a particular 
 * purpose. See the University of Illinois/NCSA Open Source 
 * License for more details.
 * 
 * You should have received a copy of the University of 
 * Illinois/NCSA Open Source License along with this program; if 
 * not, see http://www.opensource.org/
 **************************************************************/
package net.sourceforge.nukesim;
import java.io.*;

/**
 * Class for reading in spreadsheet-style text files.
 */
public class ColumnarTextReader extends InputStreamReader {

    private StringReader sr;
    private LineNumberReader lnr;

    public ColumnarTextReader(InputStream is) throws FileNotFoundException, IOException {
        super(is);
        lnr=new LineNumberReader(this);
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
