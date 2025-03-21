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

import jade.physics.Energy;
import jade.physics.Mass;
import jade.physics.Quantity;
import jade.physics.Scalar;

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.List;

/**
 * Class representing atomic nuclei for the purposes of kinematics calculations.
 * 
 * @author <a href="dale@visser.name">Dale W Visser </a>
 */
public class Nucleus extends Particle implements Serializable, NukeUnits {

	/**
	 * Unified mass unit using the V90 standard. See Audi and Wapstra 2003
	 * evaluation.
	 */
	static public final Mass U_V90 = Mass.massOf(Quantity.valueOf(931.4940090,
			0.0000071, MeV));

	/**
	 * Stores binding energy data for all <code>Nucleus</code> objects to
	 * access.
	 */
	static MassExcessTable bet = null;

	/**
	 * Mass number.
	 */
	private final int massNumber;

	/**
	 * Element Number.
	 */
	private final int protonNumber;

	/**
	 * Excitation of nucleus in MeV.
	 */
	private final Energy excitation;

	private Mass massExcess = null;

	private static TableText tableToUse = TableText.TABLE_2003;

	/**
	 * Calling this constructor causes the Binding energies to be read in.
	 */
	Nucleus(int charge, int mass, boolean buildTable) {
		if (!buildTable) {
			throw new IllegalArgumentException(
					"Only MassExcessTable may call this, using true.");
		}
		protonNumber = charge;
		massNumber = mass;
		excitation = Energy.ZERO;
	}

	/**
	 * Default constructor, returns an object representing a particular nucleus.
	 * 
	 * @param charge
	 *            element number
	 * @param mass
	 *            mass number
	 * @param excitation
	 *            excitation energy in MeV
	 * @throws IllegalArgumentException
	 *             if the asked-for nucleus is not in the mass table
	 */
	public Nucleus(int charge, int mass, Energy excitation) {
		initialize(); //make sure tables exist
		this.protonNumber = charge;
		this.massNumber = mass;
		this.excitation = excitation;
		if (!bet.massExists(this)) {
			throw new IllegalArgumentException(this
					+ " was not found in the mass table.");
		}
	}

	/**
	 * Returns an object representing a particular nucleus.
	 * 
	 * @param charge
	 *            element number
	 * @param mass
	 *            mass number
	 * @param exMeV
	 *            excitation energy in MeV
	 */
	public Nucleus(int charge, int mass, double exMeV) {
		this(charge, mass, (Energy) Quantity.valueOf(exMeV, MeV));
	}

	/**
	 * Returns an object representing a particular nucleus in its ground state.
	 * 
	 * @param Z
	 *            element number
	 * @param A
	 *            mass number
	 */
	public Nucleus(int charge, int mass) {
		this(charge, mass, Energy.ZERO);
	}
	
	public int getMassNumber(){
		return massNumber;
	}

	/**
	 * Sets up lookup tables.
	 */
	private static void initialize() {
		if (bet == null) {
			bet = MassExcessTable.load(tableToUse);
		}
	}

	public static void setMassTable(TableText textTable) {
		if (textTable != tableToUse) {
			bet = MassExcessTable.load(textTable);
			tableToUse = textTable;
		}
	}

	/**
	 * Returns element symbol for the specified element.
	 * 
	 * @param z_
	 *            element number
	 */
	static public String getElementSymbol(int element) {
		initialize(); //make sure table exists
		return bet.getSymbol(element);
	}

	/**
	 * Returns element number for the given symbol.
	 * 
	 * @param symbol
	 *            the 2 or 3 letter symbol for the element
	 */
	static public int getElementNumber(String symbol) throws NuclearException {
		initialize(); //make sure tables exist
		return symbol.equals("g") ? 0 : bet.getElementNumber(symbol);
	}

	/**
	 * Parses a string like "197Au" into a Nucleus object.
	 * 
	 * @param nukeString
	 *            element symbol (case insensitive) and mass number in any
	 *            orders
	 */
	static public Nucleus parseNucleus(String nukeString) throws NuclearException {
		initialize(); //make sure tables exist
		final Nucleus rval;
		try {
			if (nukeString.equals("g") || nukeString.equals("0γ")) {
				rval = new Nucleus(0, 0);
			} else {
				StreamTokenizer parser = new StreamTokenizer(new StringReader(nukeString));
				parser.nextToken();
				String elSymbol = null; //element symbol
				int localA = 0; //mass number
				if (parser.ttype == StreamTokenizer.TT_NUMBER) {
					localA = (int) parser.nval;
				} else if (parser.ttype == StreamTokenizer.TT_WORD) {
					elSymbol = parser.sval;
					testloop: for (int i = 1; i < elSymbol.length(); i++) {
						/* first char can't be # */
						if (Character.isDigit(elSymbol.charAt(i))) {
							localA = Integer.parseInt(elSymbol.substring(i));
							elSymbol = elSymbol.substring(0, i);
							break testloop;
						}
					}
				} else {
					throw new NuclearException(
							"Can't parse an empty string as a nucleus.");
				}
				parser.nextToken();
				if (parser.ttype == StreamTokenizer.TT_NUMBER) {
					localA = (int) parser.nval;
				} else if (parser.ttype == StreamTokenizer.TT_WORD) {
					elSymbol = parser.sval;
				} //else forget it
				if (elSymbol == null) {
					throw new NuclearException(
							"Can't parse a Nucleus without an element symbol or \"n\" or \"g\".");
				} else {
					if (elSymbol.equalsIgnoreCase("n") && localA == 1) {
						rval = new Nucleus(0, 1);
					} else {
						final int elNumber = getElementNumber(elSymbol);
						rval = new Nucleus(elNumber, localA);
					}
				}
			}
		} catch (IOException ioe) {
			throw new NuclearException(
					"An error occured while parsing the nucleus: "
							+ ioe.getMessage());
		}
		return rval;
	}

	/**
	 * Pass through call to return all stable isotopes for this element.
	 */
	static public List getIsotopes(int element) {
		initialize(); //make sure tables exist
		return bet.getIsotopes(element);
	}

	/**
	 * Mass in MeV/c^2.
	 */
	public Mass getMass() {
		initialize(); //make sure tables exist
		return (Mass) getGroundStateMass().plus(excitation);
	}
	
	public Energy getExcitation(){
		return excitation;
	}

	/**
	 * Returns ground state mass in MeV/c^2.
	 */
	public Mass getGroundStateMass() {
		initialize(); //make sure tables exist
		if (massExcess == null) {
			massExcess = bet.getMassExcess(this);
		}
		return (massNumber == 0) ? Mass.ZERO :
		Mass.massOf(U_V90.multiply(Scalar.valueOf(massNumber)).add(
					massExcess));
	}

	public int getChargeNumber() {
		return protonNumber;
	}

	/**
	 * Returns element symbol for this nucleus.
	 */
	public String getElementSymbol() {
		initialize(); //make sure tables exist
		return (massNumber == 0) ?
			"γ" :
		    bet.getSymbol(protonNumber);
	}

	/**
	 * Checks if object represents the same isotope.
	 */
	public boolean equals(Object object) {
		final boolean rval;
		if (!getClass().isInstance(object)) {
			rval = false;
		} else {
			Nucleus nuke = (Nucleus) object;
			rval = (nuke.hashCode() == hashCode());
		}
		return rval;
	}

	/**
	 * Inherited from <code>java.lang.Object</code> for storing in a
	 * HashTable.
	 */
	public int hashCode() {
		int out = protonNumber * 10000 + massNumber;
		return out;
	}

	/**
	 * Override of <code>java.lang.Object</code> for printing.
	 */
	public String toString() {
		return massNumber + getElementSymbol();
	}

}