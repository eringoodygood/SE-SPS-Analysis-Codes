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
import java.io.*;
import java.util.Vector;

/**
 * Implementation of <code>Absorber</code> for a solid.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class Solid extends Absorber implements Serializable{
    
    public Solid(double thickness,int units, String [] components,
    double [] fractions) throws NuclearException {
        if (components.length!=fractions.length) throw new
        NuclearException("Solid constructor: Arrays not equal size!");
        this.fractions=setFractions(fractions);
        Z = new int[components.length];
        for (int i=0;i<components.length;i++) {
            Z[i] = data.getElement(components[i]);
        }
        setDensity();
        setThickness(thickness,units);
    }
    
    public Solid(double thickness, int units, String component)
    throws NuclearException {
        fractions = new double[1];
        fractions[0] = 1.0;
        Z=new int[1];
        Z[0] = data.getElement(component);
        setDensity();
        setThickness(thickness,units);
        if (getThickness() == 0.0) throw new NuclearException("No thickness!");
    }
    
    public Solid(double thickness, int units, Nucleus component)
    throws NuclearException {
        fractions = new double[1];
        fractions[0] = 1.0;
        Z=new int[1];
        Z[0] = component.Z;
        setDensity();
        setThickness(thickness,units);
        if (getThickness() == 0.0) throw new NuclearException("No thickness!");
    }
    
    public Solid(String spec, double thickness) throws NuclearException {
        Vector v_elements=new Vector(1,1);
        Vector v_amounts=new Vector(1,1);
        try {
            StreamTokenizer st=new StreamTokenizer(new StringReader(spec));
            int type = st.nextToken();
            while (type != StreamTokenizer.TT_EOF && type != StreamTokenizer.TT_EOL){
                if (type==StreamTokenizer.TT_WORD) {
                    v_elements.addElement(st.sval);
                    type = st.nextToken(); 
                    if (type==StreamTokenizer.TT_NUMBER) {
                        v_amounts.addElement(new Double(st.nval));
                    }
                }                
                type = st.nextToken();
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
        String [] components = new String[v_elements.size()];
        double [] fractions = new double[v_amounts.size()];
        for (int i=0; i<v_amounts.size(); i++){
            components[i]=(String)v_elements.elementAt(i);
            fractions[i]=((Double)v_amounts.elementAt(i)).doubleValue();
        }
        if (components.length!=fractions.length) throw new
        NuclearException("Solid constructor: Arrays not equal size!");
        this.fractions=setFractions(fractions);
        Z = new int[components.length];
        for (int i=0;i<components.length;i++) {
            Z[i] = data.getElement(components[i]);
        }
        setDensity();
        setThickness(thickness,Solid.MICROGRAM_CM2);
    }
    
    
    
    private Solid(){
    }
    
    static public Solid Mylar(double thickness, int units) throws
    NuclearException{
        String [] mylarElements = {"C","H","O"};
        double [] mylarFractions = {10, 8, 4};
        return new Solid(thickness,units,mylarElements,mylarFractions);
    }
    
    static public Solid Kapton(double thickness, int units) 
    throws NuclearException{
    	String [] kaptonElements={"H","C","N","O"};
    	double [] kaptonFractions={10,22,2,5};
    	return new Solid(thickness,units,kaptonElements,kaptonFractions);
    }
    
    private void setDensity(){
        density = new double[Z.length];
        for (int i=0; i<Z.length; i++){
            density[i]=fractions[i] * data.getDensity(Z[i]);
        }
    }
    
    /**
     * Estimated density of solid in g/cm^3.
     */
    public double getDensity(){
        double rval = 0.0;
        for (int i=0; i<Z.length; i++){
            rval += this.density[i];
        }
        return rval;
    }
    
    /**
     * Returns thickness in micrograms/cm^2.
     */
    final public double getThickness(){
        return thickness;
    }
    
    public Absorber getNewInstance(double factor) {
        Solid rval=(Solid)clone();
        rval.setThickness(getThickness()*factor);
        return rval;
    }
    
    protected Object clone(){
        Solid rval = new Solid();
        rval.Z = new int[Z.length];
        rval.fractions = new double[Z.length];
        rval.density = new double[Z.length];
        rval.thickness = thickness;
        System.arraycopy(Z,0,rval.Z,0,Z.length);
        System.arraycopy(fractions,0,rval.fractions,0,fractions.length);
        System.arraycopy(density,0,rval.density,0,density.length);
        return rval;
    }
    
    static java.text.DecimalFormat df = new java.text.DecimalFormat("0.000#");
    public String getText(){
        String rval="";
        for (int i=0; i< Z.length; i++){
            rval += Nucleus.getElementSymbol(Z[i])+" ";
            rval += df.format(fractions[i])+" ";
        }
        return rval;
    }
}

