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
import junit.framework.TestCase;

/**
 * JUnit test of <code>Nucleus</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class RutherfordTest extends TestCase {

	/**
	 * Constructor for NucleusTest.
	 * @param arg0
	 */
	public RutherfordTest(String arg0) {
		super(arg0);
	}

	/*
	 * Test for boolean equals(Object)
	 */
	public void testEqualsObject() {
		System.out.println("alpha = "+Rutherford.ALPHA);
		System.out.println("hbar-c = "+Rutherford.HBAR_C);
		System.out.println("e-squared = "+Rutherford.E2);
	}
}
