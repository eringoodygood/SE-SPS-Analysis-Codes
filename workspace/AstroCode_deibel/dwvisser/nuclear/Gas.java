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

/**
 * Implementation of <code>Absorber</code> for gasses. 
 * 
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class Gas extends Absorber {

    static final double MOLAR_VOLUME = 22414.10;//cm^3 per mole
    static final double ATM = 760;//torr
    double length;//length in cm;
    
    /**
     * Given thickness in millimeters, pressure in torr, element components, 
     * and the numbers of atoms for each element in the gas molecule, creates 
     * an instance of absorber. 
     * 
     * @param length in cm
     * @param pressure in tor
     * @param components elements symbols of molecular components
     * @param atomsInMolecule number of atoms of each element per molecule in same order as <code>components</code>
     */
    public Gas(double length, double pressure,
    String [] components, int [] atomsInMolecule) throws NuclearException {
        Z = new int[components.length];//elements
        double [] A = new double[components.length];//natural weights in AMU
        fractions = new double[components.length];
        double moleWeight=0.0;
        int numAtoms=0;
        for (int i=0;i<components.length;i++) {
            Z[i] = data.getElement(components[i]);
            A[i] = data.getNaturalWeight(Z[i]);
            moleWeight += A[i]*atomsInMolecule[i];
            numAtoms += atomsInMolecule[i];
        }
        for (int i=0;i<components.length;i++){
            fractions[i]=(double)atomsInMolecule[i]/(double)numAtoms;
        }
        //             [g/mol]/[cm^3/mol](1 ATM)   [atm/atm]      [cm]
        this.thickness=(moleWeight/MOLAR_VOLUME)*(pressure/ATM)*(length);// g/cm^2
        this.thickness *= 1.0E6;// ug/cm^2
        this.length = length;
    }
    
    private Gas(){
    }
    
    
    public double getDensity(){
        return thickness*1.0e-6 /*g/cm^2*/ / length /*length in cm*/;
    }
    
    /**
     * Static factory method for isobutane gas absorber. 
     *
     * @param length in cm
     * @param pressure in torr
     * @return isobutane gas absorber object
     * @throws NuclearException if something goes wrong running <code>Gas</code> constructor
     */
    static public Gas Isobutane(double length, double pressure) throws NuclearException {
        String [] elements = {"C","H"};
        int [] numbers = {4,10};
        return new Gas(length, pressure,elements,numbers);
    }
    
	/**
	  * Static factory method for tetraflouromethane gas absorber. 
	  *
	  * @param length in cm
	  * @param pressure in torr
	  * @return isobutane gas absorber object
	  * @throws NuclearException if something goes wrong running <code>Gas</code> constructor
	  */
	 static public Gas CF4(double length, double pressure) throws NuclearException {
		 String [] elements = {"C","F"};
		 int [] numbers = {1,4};
		 return new Gas(length, pressure,elements,numbers);
	 }

    public Gas(double length,  double pressure,
    String component, int atomsInMolecule) throws NuclearException {
        Z = new int[1];//elements
        double [] A = new double[1];//natural weights in AMU
        fractions = new double[1];
        double moleWeight;
        Z[0] = data.getElement(component);
        A[0] = data.getNaturalWeight(Z[0]);
        moleWeight = A[0]*atomsInMolecule;
        fractions[0]=1.0;
        this.thickness=moleWeight/MOLAR_VOLUME/ATM*pressure*(length);// g/cm^2
        this.thickness *= 1.0E6;// ug/cm^2
        this.length=length;
    }

    /**
     * Returns thickness in micrograms/cm^2.
     */
    public  double getThickness(){
        return thickness;
    }
    
    public Absorber getNewInstance(double factor) {
        Absorber rval = (Absorber)clone();
        rval.setThickness(getThickness()*factor);
        return rval;
    }    

    protected Object clone(){
        Gas rval = new Gas();
        rval.Z = new int[Z.length];
        rval.fractions = new double[Z.length];
        rval.thickness = thickness;
        rval.length=length;
        System.arraycopy(Z,0,rval.Z,0,Z.length);
        System.arraycopy(fractions,0,rval.fractions,0,fractions.length);
        return rval;
    }
}

