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
import javax.swing.table.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;
import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *
 * @author Dale Visser
 * @version 1.0
 */
public final class ReactionTableModel extends AbstractTableModel {
    Nucleus target,beam,projectile;
    ReactionTableClient rtc;
    
    /**
     * Parent component for warning dialogs.
     */
    private Component parent;
	private static final String [] HEADERS={"","A,Z","Mass [Mev]"};
	/* used Object.class instead of Nucleus.class to get editing working */
	private final static Class []  CLASSES={String.class,Object.class,
		UncertainNumber.class};
    private final Object [][] data=new Object[5][3]; 

    public ReactionTableModel(Component parent){
        super();
        data[0][0]="Target(1)";
        data[1][0]="Beam(2)";
        data[2][0]="Projectile(3)";
        data[3][0]="Residual(4)";
        data[4][0]="Q0";
        try{
        	data[0][1]=new Nucleus(6,12);
        	data[1][1]=data[0][1];
        	data[2][1]=new Nucleus(2,4);
        	data[3][1]= new Nucleus(10,20);
        } catch (NuclearException e){
        	System.err.println(e);
        }
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

    public Class getColumnClass(int c) {
        return CLASSES[c];
    }

    public String getColumnName(int c) {
        return HEADERS[c];
    }

    public boolean isCellEditable(int r, int c){
        return (r<3 && c == 1);
    }

    public Object getValueAt(int r, int c){
        return data[r][c];
    }

	private Nucleus last_target, last_beam, last_projectile;
	
	private static final UncertainNumber gs=new UncertainNumber(0.0);//represents g.s. excitation
	
    public void setValueAt(Object value, int r, int c){
        if (c==1) {
            if (value instanceof String) {
            	Object temp=data[r][c];
            	try {
					data[r][c]=Nucleus.parseNucleus((String)value);
            	} catch (NuclearException ne) {
					JOptionPane.showMessageDialog(
						parent, 
						ne.getMessage(),
						"Nucleus name error",
						JOptionPane.ERROR_MESSAGE);
					data[r][c]=temp;
            	}
            } else if (value instanceof Nucleus) {
                data[r][c]=value;
            }
        } else {
            data[r][c]=value;
        }
        target=(Nucleus)data[0][1];
        beam=(Nucleus)data[1][1];
        projectile=(Nucleus)data[2][1];
        try {
            if (rtc != null) {
                rtc.setReaction(target,beam,projectile);
            }
			data[3][1]=Reaction.getResidual(target,beam,projectile,gs);
			data[4][2]=Reaction.getQValue(target,beam,projectile,gs);
            /* if successful store thes locally */
            last_target=target;
            last_beam=beam;
            last_projectile=projectile;
        } catch (KinematicsException ke) {
            System.err.println(ke);
			JOptionPane.showMessageDialog(
				parent,
				ke.getMessage(),
				"Kinematics Exception",
				JOptionPane.WARNING_MESSAGE);
			/* revert */
			data[0][1]=last_target;
			data[1][1]=last_beam;
			data[2][1]=last_projectile;
		} catch (NuclearException ke) {
			System.err.println(ke);
			JOptionPane.showMessageDialog(
				parent,
				ke.getMessage(),
				"Kinematics Exception",
				JOptionPane.WARNING_MESSAGE);
			/* revert */
			data[0][1]=last_target;
			data[1][1]=last_beam;
			data[2][1]=last_projectile;
		}
        for (int i=0;i<4;i++){
			data[i][2]=((Nucleus)data[i][1]).getMass();
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
