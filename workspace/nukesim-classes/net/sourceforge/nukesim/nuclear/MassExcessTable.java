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
package net.sourceforge.nukesim.nuclear;
import jade.physics.Mass;
import jade.physics.Quantity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

/**
 * Class for storing binding energies. Has a hashtable storing binding energies
 * (as <code>UncertainNumber</code> objects) with the <code>
 * Nucleus</code> objects as keys. It also has a table mapping element 
 * symbols to element numbers.
 */
public class MassExcessTable implements Serializable, NukeUnits{
	
	TableText tableUsed=TableText.TABLE_2003;//default value
	
	private final Map massTable=Collections.synchronizedMap(new HashMap());
	
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
	public Mass getMassExcess(Nucleus nucleus) {
		return (Mass)massTable.get(nucleus);
	}

	/**
	 * Get the element symbol for the specified element number.
	 * 
	 * @return element symbol if Z is valid, empty string otherwise
	 */
	public String getSymbol(int charge) {
		String rval="";
		if (charge >= 0 && charge<symbolTable.length){
			rval=symbolTable[charge];
		}
		return rval;
	}

	/**
	 * Get the element number for the specified element symbol, ignoring
	 * case. Because of the ambiguity between "n" for neutron
	 * and "N" for nitrogen, this only returns the 7, the
	 * element number of Nitrogen, for "n" and "N".
	 */
	public int getElementNumber(String symbol) throws NuclearException {
		for (int i = 1; i < symbolTable.length; i++) {
			if (symbol.equalsIgnoreCase(symbolTable[i])){
				return i;
			}
		}
		throw new NuclearException("Couldn't find element number for \""
		+symbol+"\".");
	}

	/**
	 * Return a <code>List</code> of <code>Nucleus</code> objects 
	 * representing the isotopes of the given element.
	 */
	public List getIsotopes(int charge) {
		final List rval = new ArrayList();
		for (Iterator e = massTable.keySet().iterator(); e.hasNext();) {
			final Nucleus temp = (Nucleus) e.next();
			if (temp.getChargeNumber() == charge){
				rval.add(temp);
			}
		}
		return rval;
	}
	
	boolean massExists(Nucleus nuke){
		return (massTable.get(nuke) != null);
	}
	
	/**
	 * Associate the given element number and symbol.
	 */
	private void storeSymbol(int charge, String symbol) {
		symbolTable[charge] = symbol;
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
		
	private static void errorDialog(Exception exception){
		JOptionPane.showMessageDialog(null,exception.getMessage(),exception.getClass().
		getName(),JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * The workhorse for build(). Attempts to construct a table from the ASCII file <code>
	 * mass_rmd.mas95</code> residing in the <code>net.sourceforge.nukesim.nuclear</code>
	 * package.
	 * 
	 * @return the table
	 */
	static private MassExcessTable build(TableText which) {
		String s = "";
		StringReader sr;
		int charge, mass;
		final MassExcessTable bet = new MassExcessTable();
		bet.setTableText(which);
		try {
			InputStreamReader isr =
				new InputStreamReader(
					MassExcessTable.class.getResourceAsStream(which.getName()));
			LineNumberReader lnr = new LineNumberReader(isr);
			for (int i = 0; i < 40; i++) {
				s = lnr.readLine();
			}
			do {
				sr = new StringReader(s);
				sr.skip(9); // 1st col. & (N-Z) +& N
				charge = readInt(5, sr);
				mass = readInt(5, sr);
				sr.skip(1);
				String symbol = readString(3, sr).trim();
				sr.skip(which.getColsToSkip());
				/* Text file in keV, I want MeV. */
				Mass massExcess = Mass.massOf(Quantity.valueOf(
				readDouble(which.getColsMassExcess(), sr) ,
				readDouble(which.getColsUncertainty(), sr), keV));
				bet.put(new Nucleus(charge, mass,true), massExcess);
				bet.storeSymbol(charge, symbol);
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
		bet.put(new Nucleus(0,0,true),Mass.ZERO);//gamma
		return bet;
	}
	
	private void put(Nucleus nucleus, Mass excess){
		massTable.put(nucleus,excess);
	}
	
	void setTableText(TableText which){
		tableUsed=which;
	}

	/** 
	 * private worker methods for reading in strings, ints, and doubles 
	 * from a Reader. 
	 */
	static private String readString(int len, Reader reader) throws IOException {
		final char[] temp = new char[len];
		reader.read(temp);
		final StringBuffer sbuff=new StringBuffer();
		return sbuff.append(temp).toString().replace('#', ' ');
	}

	static private int readInt(int len, Reader reader) throws IOException {
		return Integer.parseInt(readString(len, reader).trim());
	}

	static private double readDouble(int len, Reader reader) throws IOException {
		return Double.parseDouble(readString(len, reader).trim());
	}

	/**
	 * for testing purposes only
	 */
	public static void main(String args[]) {
		MassExcessTable.load(TableText.TABLE_1995);
	}

}
