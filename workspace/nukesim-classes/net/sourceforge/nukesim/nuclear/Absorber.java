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
import jade.physics.Quantity;
import jade.physics.VolumetricDensity;
import jade.units.SI;
import jade.units.Unit;

import java.io.Serializable;

/**
 * An abstraction of a thickness of material acting
 * as a stopper of energetic ions.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public abstract class Absorber implements Serializable, NukeUnits {

    /** 
     * The default thickness unit, &#181;g/cm&#178;
     */
    public final static int MICROGRAM_CM2 = 1;
    
    /**
     * mg/cm&#178;
     */
    public final static int MILLIGRAM_CM2 = 2;
    
    /**
     * cm
     */
    public final static int CM            = 3;
    
    /**
     * one-thousandth of an inch
     */
    public final static int MIL           = 4;

    protected int [] Z;
    
    /**
     * Densities in g/cm&#179;.  (E.g., &#x3c1;[H<sub>2</sub>O] 
     * &#x2248; 1.0).
     */
    protected VolumetricDensity [] density;
    
    /**
     * Fraction of nuclei that are this species.
     */
    protected double [] fractions;
    
    /**
     * in keV/(µg/cm^2)
     */
    protected Quantity thickness;
    
    protected EnergyLossData data=EnergyLossData.instance();
    
    protected double [] setFractions(double [] fin){
        final double [] fout = new double[fin.length];
        final double sum=getSum(fin);
        for (int i=0;i<fin.length;i++) {
        	fout[i]=fin[i]/sum;
        } 
        return fout;
    }
    
    private double getSum(double [] fin){
        double sum = 0.0;
        for (int i=0;i<fin.length;i++) {
        	sum += fin[i];
        } 
        return sum;
    }
    
    /**
     * Get the elements in this absorber.
     * 
     * @return ordered list of the atomic numbers of the absorber 
     * elements 
     */
    public int [] getElements(){
        return Z;
    }
    
    /**
     * Get the relative quantity of the elements in this absorber.
     * 
     * @return ordered list of fractions of the elements in this
     * absorber, normalized to sum to 1
     */
    public double [] getFractions(){
        return fractions;
    }
    
    /**
     * Set thickness in some specified units.
     * 
     * @param x the thickness of the absorber
     * @param units the units for the thickness
     * @see #MICROGRAM_CM2
     * @see #MILLIGRAM_CM2
     * @see #CM
     * @see #MIL
     */
    /*public final void setThickness(Quantity x){
		/*switch (units) {
			case MICROGRAM_CM2 : thickness=Quantity.valueOf(x,µg_per_cmsq); break;
			case MILLIGRAM_CM2 : thickness=Quantity.valueOf(x,mg_per_cmsq); break;
			case CM: this.thickness = Quantity.valueOf(x,cm).multiply(getDensity()); break;
			case MIL: this.thickness = Quantity.valueOf(x,mil).multiply(getDensity());
			default: 
				Quantity.valueOf(x,µg_per_cmsq);
				break;
		}*/
    	//if (x)
    //}
    
    /**
     * Set thickness using the default units.
     * @param value thickness in &#181;g/cm&#178;
     */
    /*public void setThickness(double value){
        setThickness(value,MICROGRAM_CM2);
    }*/
    
    public void setThickness(Quantity thick){
    	final Unit unit=thick.getSystemUnit();
    	if (unit.isCompatible(µg_per_cmsq)){
    		thickness=thick;
    	} else if (unit.isCompatible(SI.METER)){
    		thickness=thick.multiply(getDensity());
    	} else {
    		throw new IllegalArgumentException(thick.getSystemUnit()+
				"is not compatible with "+µg_per_cmsq+" or "+SI.METER);
    	}
    }
    
    /**
     * Returns mass density of absorber in g/cm^3.
     */
    public abstract VolumetricDensity getDensity();

    /**
     * Returns thickness in mass/area.
     */
    final public Quantity getThickness(){
        return thickness;
    }
        
    /**
     * Returns new absorber identical to this one, with thickness multiplied
     * by <code>factor</code>.
     */
    public abstract Absorber getNewInstance(double factor);
}

