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
package dwvisser.nuclear;
import dwvisser.math.*;
import java.io.*;
import java.util.*;

/**
 * Class representing atomic nuclei for the purposes of
 * kinematics calculations.
 * 
 * @author <a href="dale@visser.name">Dale W Visser</a>
 */
public class Nucleus extends Particle implements Serializable {

	/**
	 * Unified mass unit using the V90 standard. See Audi and Wapstra 2003 evaluation.
	 */
	static public UncertainNumber U_V90=new UncertainNumber(931.4940090,0.0000071);

	/**
	 * Stores binding energy data for all <code>Nucleus</code> objects
	 * to access.
	 */
	static MassExcessTable bet = null;

	/**
	 * Mass number.
	 */
	public int A;

	/**
	 * Element Number.
	 */
	public int Z;

	/**
	 * Excitation of nucleus in MeV.
	 */
	public UncertainNumber Ex;

	public UncertainNumber massExcess = null;
	
	private static TableText tableToUse=TableText.TABLE_2003;

	/**
	 * Calling this constructor causes the Binding energies to be read in.
	 */
	Nucleus(int Z, int A, boolean buildingTable) {
		if (buildingTable) {
			this.Z = Z;
			this.A = A;
			this.Ex = new UncertainNumber(0.0, 0.0);
		} else {
			System.err.println(
				"This is a special constructor that only MassExcessTable should ever call.");
		}
	}

	/**
	 * Default constructor, returns an object representing a particular nucleus.
	 * 
	 * @param Z element number
	 * @param A mass number
	 * @param Ex excitation energy in MeV
	 */
	public Nucleus(int Z, int A, UncertainNumber Ex) throws NuclearException {
		setup(); //make sure tables exist
		this.Z = Z;
		this.A = A;
		this.Ex = Ex;
		if (!bet.massExists(this)){
			throw new NuclearException(this+" was not found in the mass table.");
		}
	}

	/**
	 * Returns an object representing a particular nucleus.
	 * 
	 * @param Z element number
	 * @param A mass number
	 * @param Ex excitation energy in MeV
	 */
	public Nucleus(int Z, int A, double Ex) throws NuclearException {
		this(Z, A, new UncertainNumber(Ex));
	}

	/**
	 * Returns an object representing a particular nucleus in its ground state.
	 * 
	 * @param Z element number
	 * @param A mass number
	 */
	public Nucleus(int Z, int A) throws NuclearException {
		this(Z, A, new UncertainNumber(0.0, 0.0));
	}

	/**
	 * Sets up lookup tables.
	 */
	static void setup() {
		if (bet == null) {
			bet = MassExcessTable.load(tableToUse);
		}
	}
	
	public static void setMassTable(TableText tt){
		if (tt != tableToUse){
			bet=MassExcessTable.load(tt);
			tableToUse=tt;
		}
	}

	/**
	 * Returns element symbol for the specified element.
	 * 
	 * @param z_ element number
	 */
	static public String getElementSymbol(int z_) {
		setup(); //make sure table exists
		return bet.getSymbol(z_);
	}

	/**
	 * Returns element number for the given symbol.
	 * 
	 * @param symbol the 2 or 3 letter symbol for the element
	 */
	static public int getElementNumber(String symbol) throws NuclearException {
		setup(); //make sure tables exist
		if (symbol.equals("g")) {
			return 0;
		}
		return bet.getElementNumber(symbol);
	}

	/**
	 * Parses a string like "197Au" into a Nucleus object. 
	 * 
	 * @param s element symbol (case insensitive) and mass number in any orders
	 */
	static public Nucleus parseNucleus(String s) throws NuclearException {
		setup(); //make sure tables exist
		int _A=0; //mass number
		String _el=null; //element symbol
		try {
			if (s.equals("g")) {
				return new Nucleus(0, 0);
			}
			StreamTokenizer st = new StreamTokenizer(new StringReader(s));
			st.nextToken();
			if (st.ttype == StreamTokenizer.TT_NUMBER) {
				_A = (int) st.nval;
			} else if (st.ttype == StreamTokenizer.TT_WORD) {
				_el = st.sval;
				testloop: for (int i=1;i<_el.length();i++){//first char can't be #
					if (Character.isDigit(_el.charAt(i))){
						_A=Integer.parseInt(_el.substring(i));
						_el=_el.substring(0,i);
						break testloop;
					}
				}
			} else {
				throw new NuclearException("Can't parse an empty string as a nucleus.");
			}
			st.nextToken();
			if (st.ttype == StreamTokenizer.TT_NUMBER) {
				_A = (int) st.nval;
			} else if (st.ttype == StreamTokenizer.TT_WORD) {
				_el = st.sval;
			} //else forget it
			if (_el == null) {
				throw new NuclearException("Can't parse a Nucleus without an element symbol or \"n\" or \"g\".");
			} else {
				if (_el.equalsIgnoreCase("n") && _A == 1) {
					return new Nucleus(0, 1);
				} else {
					int _Z = getElementNumber(_el);
					return new Nucleus(_Z, _A);
				}
			}
		} catch (IOException ioe) {
			throw new NuclearException(
				"An error occured while parsing the nucleus: "
					+ ioe.getMessage());
		}
	}

	/**
	 * Pass through call to return all stable isotopes for this element.
	 */
	static public List getIsotopes(int Z) {
		setup(); //make sure tables exist
		return bet.getIsotopes(Z);
	}

	/**
	 * Mass in MeV/c^2.
	 */
	public UncertainNumber getMass()  {
		setup(); //make sure tables exist
		return getGroundStateMass().plus(Ex);
	}

	/**
	 * Returns ground state mass in MeV/c^2.
	 */
	public UncertainNumber getGroundStateMass() {
		setup(); //make sure tables exist
		if (massExcess == null){
			massExcess = bet.getMassExcess(this);
		}
		/*if (Z == 1 && A == 1) {
			return PROTON_MASS;
		} else 
		if (Z==0 && A>1){
			return new Nucleus(0,1).getMass().times(A);
		} else*/ 
		if (A == 0) {
			return new UncertainNumber(0);
		} else {
			return (U_V90.times(A)).plus(massExcess);
		}
	}

	/**
	 * Returns element symbol for this nucleus.
	 */
	public String getElementSymbol() {
		setup(); //make sure tables exist
		if (A == 0) {
			return "g";
		}
		//try {
			return bet.getSymbol(Z);
		/*} catch (NuclearException ne){
			System.err.println("We shouldn't be here:\n"+ne);
		}*/
	}

	/**
	 * Checks if object represents the same isotope.
	 */
	public boolean equals(Object o) {
		if (!this.getClass().isInstance(o)) {
			return false;
		}
		Nucleus n = (Nucleus) o;
		if (n.hashCode() != hashCode()) {
			return false;
		}
		return true;
	}

	/**
	 * Inherited from <code>java.lang.Object</code> for storing in a HashTable.
	 */
	public int hashCode() {
		int out = Z * 10000 + A;
		return out;
	}

	/**
	 * Override of <code>java.lang.Object</code> for printing.
	 */
	public String toString() {
		return A + getElementSymbol();
	}

	static public void main(String[] args) {
		try {
			Nucleus alpha = new Nucleus(2, 4);
			System.out.println(alpha + " has mass " + alpha.getMass() + " MeV/c^2");
			System.out.println(parseNucleus("c13"));
		} catch (NuclearException ne){
			ne.printStackTrace();
		}
	}
}
