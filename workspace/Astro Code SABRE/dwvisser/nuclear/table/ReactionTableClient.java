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
package dwvisser.nuclear.table;
import dwvisser.nuclear.*;

/**
* There is one reaction client per reaction table, which receives the reaction
* object from it.
*
* @author <a href="mailto:dale@visser.name">Dale Visser</a>
*/
public interface ReactionTableClient {

  public void setReaction(Nucleus target, Nucleus beam, Nucleus projectile) throws KinematicsException, NuclearException;

}

