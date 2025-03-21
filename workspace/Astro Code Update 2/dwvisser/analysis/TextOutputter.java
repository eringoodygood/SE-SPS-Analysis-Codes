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
package dwvisser.analysis;
import java.io.*;

/**
 * Gives a framework for subclasses to use System.out to create their
 * output.
 * 
 * @author  Dale Visser
 * @version 1.0 (18 Jun 2001)
 */
public  class TextOutputter extends Object {

    static public String DEFAULT = "default";
    PrintStream defaultOutput = System.out;
    FileOutputStream fos;
    
    /** Creates new TextOutputter */
    public TextOutputter(String output) throws FileNotFoundException {
        setOutput(output);
    }
    
    final void setOutput(String output) throws FileNotFoundException {
        if (!output.equals(DEFAULT)) {
            fos = new FileOutputStream(output);
            System.setOut(new PrintStream(fos));
            defaultOutput.println("Output now going to '"+output+"'");
        }//else if DEFAULT, leave it
    }
    
    public final void closeOutput() throws IOException {
        if (fos !=null){
            fos.flush();
            fos.close();
        }
    }
    
    public final void revertToDefaultOutput(){
        System.setOut(defaultOutput);
    }

}