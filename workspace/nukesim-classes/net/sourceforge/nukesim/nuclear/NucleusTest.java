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
import jade.JADE;
import jade.physics.Energy;
import jade.physics.Quantity;
import jade.physics.models.RelativisticModel;
import junit.framework.TestCase;

/**
 * JUnit test of <code>Nucleus</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class NucleusTest extends TestCase {

	/**
	 * Constructor for NucleusTest.
	 * @param arg0
	 */
	public NucleusTest(String arg0) {
		super(arg0);
		JADE.initialize();
	}

	/*
	 * Test for boolean equals(Object)
	 */
	public void testEqualsObject() {
		Energy ex=Energy.energyOf(Quantity.valueOf(20.1,1.0,Nucleus.MeV));
		try {
			Nucleus a1=new Nucleus(2,4);
			Nucleus a2=new Nucleus(2,4,0.0);
			Nucleus a3=new Nucleus(2,4,ex);
			Nucleus a4=new Nucleus(2,4,ex);
			assertEquals(a1,a2);
			assertEquals(a2,a3);
			assertEquals(a3,a4);
			assertEquals(a3.getExcitation().doubleValue(),
					a4.getExcitation().doubleValue(),
					a3.getExcitation().getAbsoluteError());
			a2=Nucleus.parseNucleus("he4");
			assertEquals(a1,a2);
		} catch (NuclearException ne) {
			ne.printStackTrace();
		}		
	}
	
	static public void main(String[] args) {
		JADE.initialize();
		RelativisticModel.select();
		Nucleus alpha = new Nucleus(2, 4);
		System.out.println(alpha + " has mass " + alpha.getMass().toText(Nucleus.MeV));
		try{
			System.out.println(Nucleus.parseNucleus("c13"));
		} catch (NuclearException ne){
			System.err.println(ne.getMessage());
		}
	}
}
