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
package dwvisser;
import java.io.*;

/**
 * 
 * Reads in values from a text file separated by whitespace (spaces,
 * tabs, carriage returns & linefeeds). Everything after the # sign on any line
 * is ignored as a comment. Anything between quotation marks (" "), is read in as 
 * a single string. 
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 */
public class SimpleTokenReader {

	private StreamTokenizer st;
	FileReader fr;


    /**
     * Create an instance for reading in the file f.
     * 
	 * @param f text file to read from
	 * @throws IOException if something goes wrong opening the file
	 */
	public SimpleTokenReader(File f) throws IOException {
		fr=new FileReader(f);
		st = new StreamTokenizer(new BufferedReader(fr));
		st.commentChar('#');
		st.quoteChar('\"');
		st.eolIsSignificant(false);
    }
    
	/**
	 * Reads an integer from the file, if the next token is a number. Floating
	 * point numbers are cast to integer.
	 * 
	 * @return integer value
	 * @throws IOException if the next token is not a number
	 */
	public int readInteger() throws IOException {
		st.nextToken();
		if (st.ttype != StreamTokenizer.TT_NUMBER) throw new IOException(this.getClass().getName()+
		".readInteger(): Wrong token type: "+st.ttype);
		return (int)st.nval;
	}

	/**
	 * Reads an double from the file, if the next token is a number. 
	 * 
	 * @return floating point value
	 * @throws IOException if the next token is not a number
	 */
	public double readDouble() throws IOException {
		st.nextToken();
		if (st.ttype != StreamTokenizer.TT_NUMBER) throw new IOException(this.getClass().getName()+
		".readInteger(): Wrong token type: "+st.ttype);
		return st.nval;
	}

	/**
	 * Reads a string from the file, either a single word or the entire between
	 * two quotation marks (" ").
	 * 
	 * @return the next token, if it's a String
	 * @throws IOException if the next token is not a String
	 */
	public String readString() throws IOException {
		st.nextToken();
		if (st.ttype != StreamTokenizer.TT_WORD) throw new IOException(this.getClass().getName()+
		".readString(): Wrong token type: "+st.ttype);
		System.out.println(st.sval);
		return st.sval;
	}
	
	/**
	 * One may call this to close the underlying file.
	 * 
	 * @throws IOException if there's a problem closing the file
	 */
	public void close() throws IOException {
		fr.close();
	}
        
}
