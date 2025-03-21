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
import javax.swing.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;

/** 
 * Table at top of JRelKin window showing the reaction.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public class ReactionTable extends JTable {

    /** data model
     */
    ReactionTableModel rtm;

    /** Creates the table.
     * @param rtm data model
     */
    public ReactionTable(ReactionTableModel rtm){
        super(rtm);
        this.rtm=rtm;
        setOpaque(true);
    }
    
    public Nucleus getBeam(){
    	return (Nucleus)rtm.getValueAt(1,1);
    }

	public Nucleus getTarget(){
		return (Nucleus)rtm.getValueAt(0,1);
	}

	public Nucleus getProjectile(){
		return (Nucleus)rtm.getValueAt(2,1);
	}

	public Nucleus getResidual(){
		return (Nucleus)rtm.getValueAt(3,1);
	}
	
	public UncertainNumber getQ0(){
		return (UncertainNumber)rtm.getValueAt(4,2);
	}

    /** Sets the client for data.
     * @param rtc receiver
     * @throws KinematicsException if something goes wrong
     */
    public void setReactionTableClient(ReactionTableClient rtc) throws
    KinematicsException, NuclearException {
        rtm.setReactionTableClient(rtc);
    }

}
