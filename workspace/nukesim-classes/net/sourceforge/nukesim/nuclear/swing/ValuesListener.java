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
package net.sourceforge.nukesim.nuclear.swing;

/**
* Class to listen for changes in values.
*
* @author Dale Visser
* @see net.sourceforge.nukesim.nuclear.swing.ValuesChooser
*/
public interface ValuesListener {

	/**
	 * Receives values with confirmation of whether they are 
	 * acceptable.
	 * 
	 * @param chooser the <code>ValuesChooser</code>sending the values
	 * @param values the sent numbers
	 * @return true if accepted, false if not
	 */
	public boolean receiveValues(ValuesChooser chooser, double [] values);

}
