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
package net.sourceforge.nukesim.nuclear.table;
import jade.physics.Energy;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

import net.sourceforge.nukesim.math.QuantityUtilities;
import net.sourceforge.nukesim.math.UncertainNumber;
import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.Nucleus;
import net.sourceforge.nukesim.nuclear.NukeUnits;
import net.sourceforge.nukesim.nuclear.Reaction;

/**
 *
 * @author Dale Visser
 * @version 1.0
 */
public final class ReactionTableModel extends AbstractTableModel implements NukeUnits {
    private ReactionTableClient rtc;
    
    /**
     * Parent component for warning dialogs.
     */
    private transient Component parent;
	private static final String [] HEADERS={"","A,Z","Mass"};
	/* used Object.class instead of Nucleus.class to get editing working */
	private final static Class []  CLASSES={String.class,Object.class,
		UncertainNumber.class};
    private transient final Object [][] data=new Object[5][3]; 

    public ReactionTableModel(Component parent){
        super();
        data[0][0]="Target(1)";
        data[1][0]="Beam(2)";
        data[2][0]="Projectile(3)";
        data[3][0]="Residual(4)";
        data[4][0]="Q0";
        	data[0][1]=new Nucleus(6,12);
        	data[1][1]=data[0][1];
        	data[2][1]=new Nucleus(2,4);
        	data[3][1]= new Nucleus(10,20);
        for (int i=0; i<data.length;i++){
        	data[i][2]=new UncertainNumber(0.0);
        }
        this.parent=parent;
        setValueAt(data[0][0],0,0);//force calculation of table
    }

    public int getRowCount(){
        return data.length;
    }

    public int getColumnCount() {
        return data[0].length;
    }

    public Class getColumnClass(int column) {
        return CLASSES[column];
    }

    public String getColumnName(int column) {
        return HEADERS[column];
    }

    public boolean isCellEditable(int row, int column){
        return (row<3 && column == 1);
    }

    public Object getValueAt(int row, int column){
        return data[row][column];
    }

	private Nucleus target, beam, projectile, lastTarget, lastBeam, lastProj;
	
    public void setValueAt(Object value, int row, int column){
        if (column==1) {
            if (value instanceof String) {
            	Object temp=data[row][column];
            	try {
					data[row][column]=Nucleus.parseNucleus((String)value);
            	} catch (NuclearException ne) {
					JOptionPane.showMessageDialog(
						parent, 
						ne.getMessage(),
						"Nucleus name error",
						JOptionPane.ERROR_MESSAGE);
					data[row][column]=temp;
            	}
            } else if (value instanceof Nucleus) {
                data[row][column]=value;
            }
        } else {
            data[row][column]=value;
        }
        target=(Nucleus)data[0][1];
        beam=(Nucleus)data[1][1];
        projectile=(Nucleus)data[2][1];
        try {
            if (rtc != null) {
                rtc.setReaction(target,beam,projectile);
            }
			data[3][1]=Reaction.getResidual(target,beam,projectile,Energy.ZERO);
			data[4][2]=QuantityUtilities.reportQuantity(Reaction.getQValue(target,beam,projectile,Energy.ZERO),MeV,true);
            /* if successful store these locally */
            lastTarget=target;
            lastBeam=beam;
            lastProj=projectile;
        } catch (KinematicsException ke) {
            System.err.println(ke);
			JOptionPane.showMessageDialog(
				parent,
				ke.getMessage(),
				"Kinematics Exception",
				JOptionPane.WARNING_MESSAGE);
			/* revert */
			data[0][1]=lastTarget;
			data[1][1]=lastBeam;
			data[2][1]=lastProj;
		} catch (NuclearException ke) {
			System.err.println(ke);
			JOptionPane.showMessageDialog(
				parent,
				ke.getMessage(),
				"Kinematics Exception",
				JOptionPane.WARNING_MESSAGE);
			/* revert */
			data[0][1]=lastTarget;
			data[1][1]=lastBeam;
			data[2][1]=lastProj;
		}
        for (int i=0;i<4;i++){
			data[i][2]=QuantityUtilities.reportQuantity(((Nucleus)data[i][1]).getMass(),amu,true);
        }
        fireTableDataChanged();
    }
    

    public void setReactionTableClient(ReactionTableClient rtc)
    throws KinematicsException, NuclearException{
        this.rtc=rtc;
        if (rtc == null) {
            System.err.println("RTM.setRTC():should have rxn at this point");
        } else {
            rtc.setReaction(target,beam,projectile);
        }
    }
}
