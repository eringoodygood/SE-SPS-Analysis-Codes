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
 * Simple class to encapsulate reading text files containing only 
 * numbers. No formatting other than numbers separated by whitespace 
 * is assumed. It is up to the user to interpret the numbers read 
 * properly.
 * 
 * @author  Dale Visser
 * @version 1.0
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