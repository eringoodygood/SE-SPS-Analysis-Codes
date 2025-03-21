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

/**
 * Abstraction of an accelerator beam.
 * 
 * @author  Dale Visser
 * @version 1.1
 * @since 1.0 (19 Oct 2001)
 */
public class Beam   {

    private Nucleus nucleus;
    private double energy;
    
    /** Creates new Beam */
    public Beam(Nucleus nuke, double e) {
        nucleus=nuke;
        energy=e;
    }
    
    public Nucleus getNucleus(){
    	return nucleus;
    }
    
    public double getEnergy(){
    	return energy;
    }
}
