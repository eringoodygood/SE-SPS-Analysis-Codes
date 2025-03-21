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

import jade.physics.Constants;
import jade.physics.Length;
import jade.physics.Mass;
import jade.physics.Pressure;
import jade.physics.Quantity;
import jade.physics.VolumetricDensity;
import jade.units.Unit;

/**
 * Implementation of <code>Absorber</code> for gasses.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser </a>
 */
public class Gas extends Absorber {

	static final Quantity MOLAR_VOLUME = Quantity.valueOf(22414.10, Unit
			.valueOf("cm^3/mol"));

	static final Pressure ATM = Pressure.STANDARD;//1 atm

	private Length length;//length in cm;

	private VolumetricDensity density;
	
	private Pressure pressure;

	/**
	 * Given thickness in millimeters, pressure in torr, element components, and
	 * the numbers of atoms for each element in the gas molecule, creates an
	 * instance of absorber.
	 * 
	 * @param length
	 *            in cm
	 * @param pressure
	 *            in tor
	 * @param components
	 *            elements symbols of molecular components
	 * @param atoms
	 *            number of atoms of each element per molecule in same order as
	 *            <code>components</code>
	 */
	public Gas(Length length, Pressure pressure, String[] components,
			int[] atoms) throws NuclearException {
		this.length = length;
		this.pressure = pressure;
		initialize(components, atoms);
	}
	
	private final void initialize(String [] components, int [] atoms) 
	throws NuclearException {
		Z = new int[components.length];//elements
		/* natural weights in amu == g/mol */
		Quantity moleWeight = Mass.ZERO.multiply(Constants.N);
		int numAtoms = 0;
		final Quantity[] atomicWeight = new Quantity[components.length];
		for (int i = 0; i < components.length; i++) {
			Z[i] = data.getElement(components[i]);
			atomicWeight[i] = data.getNaturalWeight(Z[i]);
			moleWeight = moleWeight.add(atomicWeight[i].multiply(atoms[i]));
			numAtoms += atoms[i];
		}
		fractions = new double[components.length];
		for (int i = 0; i < components.length; i++) {
			fractions[i] = (double) atoms[i] / (double) numAtoms;
		}
		/* [amu==g/mol]/[cm^3==cm^3/mol](mass density at 1 ATM) * [p/atm] */
		density = VolumetricDensity.volumetricDensityOf(moleWeight.divide(
				MOLAR_VOLUME).multiply(pressure.divide(ATM)));
		thickness = density.multiply(length);		
	}

	public Gas(Length length, Pressure pressure, String component,
			int atoms) throws NuclearException {
		this();
		this.length=length;
		this.pressure=pressure;
		final String[] passC = new String[1];
		final int[] passA = new int[1];
		passC[0] = component;
		passA[0] = atoms;
		initialize(passC, passA);
	}

	private Gas() {
	}

	public VolumetricDensity getDensity() {
		return density;
	}

	/**
	 * Static factory method for isobutane gas absorber.
	 * 
	 * @param length
	 *            in cm
	 * @param pressure
	 *            in torr
	 * @return isobutane gas absorber object
	 * @throws NuclearException
	 *             if something goes wrong running <code>Gas</code>
	 *             constructor
	 */
	static public Gas isobutane(Length length, Pressure pressure)
			throws NuclearException {
		String[] elements = { "C", "H" };
		int[] numbers = { 4, 10 };
		return new Gas(length, pressure, elements, numbers);
	}

	/**
	 * Static factory method for tetraflouromethane gas absorber.
	 * 
	 * @param length
	 *            in cm
	 * @param pressure
	 *            in torr
	 * @return isobutane gas absorber object
	 * @throws NuclearException
	 *             if something goes wrong running <code>Gas</code>
	 *             constructor
	 */
	static public Gas cf4(Length length, Pressure pressure)
			throws NuclearException {
		String[] elements = { "C", "F" };
		int[] numbers = { 1, 4 };
		return new Gas(length, pressure, elements, numbers);
	}

	public Absorber getNewInstance(double factor) {
		Absorber rval = (Absorber) copy();
		rval.setThickness(getThickness().multiply(factor));
		return rval;
	}

	private Gas copy() {
		Gas rval = new Gas();
		rval.Z = new int[Z.length];
		rval.fractions = new double[Z.length];
		rval.thickness = thickness;
		rval.length = length;
		System.arraycopy(Z, 0, rval.Z, 0, Z.length);
		System.arraycopy(fractions, 0, rval.fractions, 0, fractions.length);
		return rval;
	}
}

