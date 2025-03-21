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
package cmdeibel.nuclear;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;

import dwvisser.math.UncertainNumber;

/**
 * Class for storing binding energies. Has a hashtable storing binding energies
 * (as <code>UncertainNumber</code> objects) with the <code>
 * Nucleus</code> objects as keys. It also has a table mapping element 
 * symbols to element numbers.
 */
public class MassExcessTable extends Hashtable {
	
	TableText tableUsed=TableText.TABLE_2003;//default value
	
	/**
	 * name of file to load stored binding energies as and object
	 */
	private static final File HOME=new File(System.getProperty("user.home"));
	private static final File MASSFILE = new File(HOME,"MassExcesses.obj");

	/**
	 * array of element symbols keyed to element numbers
	 */
	private String[] symbolTable = new String[120];

	/**
	 * Default constructor, necessary for loading from <code>ObjectInputStream</code>.
	 */
	public MassExcessTable() {
	}

	/**
	 * Get the binding energy in MeV of the specified nucleus.
	 */
	public UncertainNumber getMassExcess(Nucleus n) {
		return (UncertainNumber) get(n);
	}

	/**
	 * Get the element symbol for the specified element number.
	 * 
	 * @return element symbol if Z is valid, empty string otherwise
	 */
	public String getSymbol(int Z) {
		String rval="";
		if (Z >= 0 && Z<symbolTable.length){
			rval=symbolTable[Z];
		}
		return rval;
	}

	/**
	 * Get the element number for the specified element symbol, ignoring
	 * case. Because of the ambiguity between "n" for neutron
	 * and "N" for nitrogen, this only returns the 7, the
	 * element number of Nitrogen, for "n" and "N".
	 */
	public int getElementNumber(String s) throws NuclearException {
		for (int i = 1; i < symbolTable.length; i++) {
			if (s.equalsIgnoreCase(symbolTable[i]))
				return i;
		}
		throw new NuclearException("Couldn't find element number for \""
		+s+"\".");
	}

	/**
	 * Return a <code>Vector</code> of <code>Nucleus</code> objects 
	 * representing the isotopes of the given element.
	 */
	public List getIsotopes(int Z) {
		Nucleus temp;
		List rval = new ArrayList();
		for (Enumeration e = keys(); e.hasMoreElements();) {
			temp = (Nucleus) e.nextElement();
			if (temp.Z == Z)
				rval.add(temp);
		}
		return rval;
	}
	
	boolean massExists(Nucleus n){
		return (get(n) != null);
	}
	
	/**
	 * Associate the given element number and symbol.
	 */
	private void storeSymbol(int Z, String symbol) {
		symbolTable[Z] = symbol;
	}

	static public MassExcessTable load(TableText which) {
		MassExcessTable bet=null;
		boolean buildIt=false;
		try {
			final FileInputStream fis = new FileInputStream(MASSFILE);
			final ObjectInputStream ois = new ObjectInputStream(fis);
			bet = (MassExcessTable) ois.readObject();
			ois.close();
			buildIt=(bet.tableUsed==null) || 
			(!bet.tableUsed.equals(which.getName())); 
		} catch (Exception e) {
			buildIt=true;
		}
		if (buildIt){
			bet = build(which);
		}
		return bet;
	}
		
	private static void errorDialog(Exception e){
		JOptionPane.showMessageDialog(null,e.getMessage(),e.getClass().
		getName(),JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * The workhorse for build(). Attempts to construct a table from the ASCII file <code>
	 * mass_rmd.mas95</code> residing in the <code>dwvisser.nuclear</code>
	 * package.
	 * 
	 * @return the table
	 */
	static private MassExcessTable build(TableText which) {
		String s = "";
		StringReader sr;
		int Z, A;
		final MassExcessTable bet = new MassExcessTable();
		bet.setTableText(which);
		try {
			InputStreamReader isr =
				new InputStreamReader(
					bet.getClass().getResourceAsStream(which.getName()));
			LineNumberReader lnr = new LineNumberReader(isr);
			for (int i = 0; i < 40; i++) {
				s = lnr.readLine();
			}
			do {
				sr = new StringReader(s);
				sr.skip(9); // 1st col. & (N-Z) +& N
				Z = readInt(5, sr);
				A = readInt(5, sr);
				sr.skip(1);
				String symbol = readString(3, sr).trim();
				sr.skip(which.getColsToSkip());
				/* Text file in keV, I want MeV. */
				UncertainNumber m_excess =
					new UncertainNumber(
						readDouble(which.getColsMassExcess(), sr) / 1000.0,
						readDouble(which.getColsUncertainty(), sr) / 1000.0);
				bet.put(new Nucleus(Z, A,true), m_excess);
				bet.storeSymbol(Z, symbol);
				sr.close();
				s = lnr.readLine();
			} while (s != null);
			lnr.close();
			s = null;
			sr = null;
			lnr = null;
			isr = null;
			FileOutputStream fos =
				new FileOutputStream(MassExcessTable.MASSFILE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(bet);
			oos.close();
			oos = null;
			fos = null;
		} catch (IOException e) {
			errorDialog(e);
			return null;
		}
		return bet;
	}
	
	void setTableText(TableText which){
		tableUsed=which;
	}

	/** 
	 * private worker methods for reading in strings, ints, and doubles 
	 * from a Reader. 
	 */
	static private String readString(int len, Reader r) throws IOException {
		char[] temp;
		temp = new char[len];
		r.read(temp);
		String s = new String(temp);
		return s.replace('#', ' ');
	}

	static private int readInt(int len, Reader r) throws IOException {
		return Integer.parseInt(readString(len, r).trim());
	}

	static private double readDouble(int len, Reader r) throws IOException {
		return Double.parseDouble(readString(len, r).trim());
	}

	/**
	 * for testing purposes only
	 */
	public static void main(String args[]) {
		MassExcessTable.load(TableText.TABLE_1995);
	}

}
