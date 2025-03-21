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
import jade.physics.Length;
import jade.physics.Quantity;
import jade.physics.VolumetricDensity;

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Vector;

/**
 * Implementation of <code>Absorber</code> for a solid.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class Solid extends Absorber implements Serializable{
    
    public Solid(Quantity x, String [] components,
    double [] fractions) throws NuclearException {
        if (components.length!=fractions.length) {
        	throw new IllegalArgumentException(
        			"Solid constructor: Arrays not equal size!");
        }
        this.fractions=setFractions(fractions);
        Z = new int[components.length];
        for (int i=0;i<components.length;i++) {
            Z[i] = data.getElement(components[i]);
        }
        setDensity();
        setThickness(x);
    }
    
    public Solid(Quantity x, String component)
    throws NuclearException {
        fractions = new double[1];
        fractions[0] = 1.0;
        Z=new int[1];
        Z[0] = data.getElement(component);
        setDensity();
        setThickness(x);
        if (getThickness().isPossiblyZero()) {
        	throw new NuclearException("No thickness!");
        }
    }
    
    public Solid(Quantity x, Nucleus component)
    throws NuclearException {
        fractions = new double[1];
        fractions[0] = 1.0;
        Z=new int[1];
        Z[0] = component.getChargeNumber();
        setDensity();
        setThickness(x);
        if (getThickness().isPossiblyZero()) {
        	throw new NuclearException("No thickness!");
        }
    }
    
    public Solid(String spec, Quantity x) throws NuclearException {
        final Vector v_elements=new Vector(1,1);
        final Vector v_amounts=new Vector(1,1);
        try {
            StreamTokenizer parser=new StreamTokenizer(new StringReader(spec));
            int type = parser.nextToken();
            while (type != StreamTokenizer.TT_EOF && type != StreamTokenizer.TT_EOL){
                if (type==StreamTokenizer.TT_WORD) {
                    v_elements.addElement(parser.sval);
                    type = parser.nextToken(); 
                    if (type==StreamTokenizer.TT_NUMBER) {
                        v_amounts.addElement(new Double(parser.nval));
                    }
                }                
                type = parser.nextToken();
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
        if (components.length!=fractions.length){
        	throw new IllegalArgumentException(
        			"Solid constructor: Arrays not equal size!");
        }
        
        this.fractions=setFractions(fractions);
        Z = new int[components.length];
        for (int i=0;i<components.length;i++) {
            Z[i] = data.getElement(components[i]);
        }
        setDensity();
        setThickness(x);
    }
    
    
    
    private Solid(){
    }
    
    static public final Solid mylar(Quantity x) throws
    NuclearException{
        final String [] elements = {"C","H","O"};
        final double [] fractions = {10, 8, 4};
        final Solid rval = new Solid(x,elements,fractions);
        final VolumetricDensity actualDensity=VolumetricDensity.volumetricDensityOf(
        		Quantity.valueOf(1.397,g_per_cm3));
        rval.modifyDensity(actualDensity,x);
        return rval;
    }
    
    static public final Solid icru216(Length x) throws NuclearException{
        final String [] elements = {"C","H"};
        final double [] fractions = {10, 11};
        final Solid rval = new Solid(x,elements,fractions);
        final VolumetricDensity actualDensity=VolumetricDensity.volumetricDensityOf(
        		Quantity.valueOf(1.032,g_per_cm3));
        rval.modifyDensity(actualDensity,x);
        return rval;
    }
    
    /**
     * only used for specific material private methods, so 
     * @param newDensity
     */
    private void modifyDensity(VolumetricDensity newDensity, Quantity x){
        final Quantity factor=newDensity.divide(getDensity());
        for (int i=0; i<Z.length; i++){
        	density[i]=VolumetricDensity.volumetricDensityOf(
        			density[i].multiply(factor));
        }
        setThickness(x);
    }
    
    static public final Solid kapton(Length x) 
    throws NuclearException{
    	final String [] elements={"H","C","N","O"};
    	final double [] fractions={10,22,2,5};
    	final Solid rval = new Solid(x,elements,fractions);
        final VolumetricDensity actualDensity=VolumetricDensity.volumetricDensityOf(
        		Quantity.valueOf(1.42,g_per_cm3));
        rval.modifyDensity(actualDensity,x);
    	return rval;
    }
    
    private void setDensity(){
        density = new VolumetricDensity[Z.length];
        for (int i=0; i<Z.length; i++){
            density[i]=VolumetricDensity.volumetricDensityOf(
            		data.getDensity(Z[i]).multiply(fractions[i]));
        }
    }
    
    /**
     * Estimated density of solid in g/cm^3.
     */
    public VolumetricDensity getDensity(){
        Quantity rval = VolumetricDensity.ZERO;
        for (int i=0; i<Z.length; i++){
            rval = rval.add(density[i]);
        }
        return VolumetricDensity.volumetricDensityOf(rval);
    }
    
    public Absorber getNewInstance(double factor) {
        Solid rval=(Solid)copy();
        rval.setThickness(getThickness().multiply(factor));
        return rval;
    }
    
    protected Solid copy() {
        Solid rval = new Solid();
        rval.Z = new int[Z.length];
        rval.fractions = new double[Z.length];
        rval.density = new VolumetricDensity[Z.length];
        rval.thickness = thickness;
        System.arraycopy(Z,0,rval.Z,0,Z.length);
        System.arraycopy(fractions,0,rval.fractions,0,fractions.length);
        System.arraycopy(density,0,rval.density,0,density.length);
        return rval;
    }
    
    static java.text.DecimalFormat format = new java.text.DecimalFormat("0.000#");
    public String getText(){
        String rval="";
        for (int i=0; i< Z.length; i++){
            rval += Nucleus.getElementSymbol(Z[i])+" ";
            rval += format.format(fractions[i])+" ";
        }
        return rval;
    }
}

