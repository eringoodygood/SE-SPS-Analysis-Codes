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
package dwvisser.analysis.spanc.tables;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import dwvisser.analysis.spanc.OutputPeak;
import dwvisser.analysis.spanc.SpancReaction;
import dwvisser.math.UncertainNumber;

/**
 *
 * @author Dale Visser
 * @version 1.0
 */
public class OutputPeakTableModel extends DefaultTableModel {
    static String [] headers={"Reaction","Ex Projectile [MeV]","Channel",
    "\u03c1 [cm]","Ex Residual [MeV]"};
    
    private boolean adjustError = false;
    
    public OutputPeakTableModel() {
        super(headers,0);
    }    
           
    static java.text.DecimalFormat df = new java.text.DecimalFormat("0.000#");
    public void addRow(OutputPeak op)  {
        final Vector temp = new Vector(5);
        temp.addElement(new Integer(op.getReactionIndex()));
        temp.addElement(new Double(op.getExProjectile()));
        temp.addElement(op.getChannel());
        temp.addElement(op.getRho(adjustError));
        temp.addElement(op.getExResidual(adjustError));
        addRow(temp);
    }
    
    void refreshData(){
       while (getRowCount()>0) {
            removeRow(0);
       }
       Iterator peaks = OutputPeak.getPeakCollection().iterator();
       while (peaks.hasNext()) addRow((OutputPeak)peaks.next());
    }
    
    synchronized void adjustErrors(boolean adjust) {
        adjustError = adjust;
        refreshData();
    }
    
    public String toString(){
    	String rval="";
    	for (int i=0; i<headers.length; i++){
    		rval += headers[i];
    		if (i < headers.length -1){
    			rval += "\t";
    		} else {
    			rval += "\n";
    		}
    	}    	
    	for (int i=0, n=getRowCount(); i<n;i++){
    		OutputPeak op=OutputPeak.getPeak(i);   
    		rval += op.getReactionIndex()+"\t";
    		rval += op.getExProjectile()+"\t";
    		rval += op.getChannel().plusMinusString()+"\t";
    		rval += op.getRho(false).plusMinusString()+"\t";
    		rval += op.getExResidual(false).plusMinusString()+"\n";
    	}
    	return rval;
    }
    public String getExportTableText(){
    	String rval="";
    	for (int i=0, n=getRowCount(); i<n;i++){
    		OutputPeak op=OutputPeak.getPeak(i); 
    		int rxnNum = op.getReactionIndex();
    		SpancReaction sr = SpancReaction.getReaction(rxnNum);  
    		rval += sr.description()+"\t";
    		rval += sr.getMagneticField()+"\t";
    		rval += op.getExProjectile()+"\t";
    		UncertainNumber exRes = op.getExResidual(false);
    		rval += exRes.value+"\t"+exRes.error+"\t";
    		rval += op.getChannel().value+"\t"+op.getChannel().error+"\t";
			rval +="\t\t";
    		UncertainNumber rho = op.getRho(false);    		
    		rval += rho.value+"\t"+rho.error+"\t";
    		rval+="\n";
    	}
    	return rval;
    }
}