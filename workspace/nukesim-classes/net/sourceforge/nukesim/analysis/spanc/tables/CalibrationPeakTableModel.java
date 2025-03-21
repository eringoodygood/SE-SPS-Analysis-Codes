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
package net.sourceforge.nukesim.analysis.spanc.tables;
import jade.physics.Energy;
import jade.physics.Quantity;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import net.sourceforge.nukesim.analysis.spanc.CalibrationFit;
import net.sourceforge.nukesim.analysis.spanc.CalibrationPeak;
import net.sourceforge.nukesim.analysis.spanc.SpancReaction;
import net.sourceforge.nukesim.math.QuantityUtilities;
import net.sourceforge.nukesim.math.UncertainNumber;
import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.NukeUnits;

/**
 *
 * @author Dale Visser
 * @version 1.0
 */
public class CalibrationPeakTableModel extends DefaultTableModel implements NukeUnits {
    private final static String [] HEADERS={"Reaction","Ex Projectile [MeV]","Ex Residual [MeV]",
    "\u03c1 [cm]","Channel", "Residual [cm]", "Resid./\u03c3"};
    private final static int NUM_COLUMNS=HEADERS.length;
    
    private final CalibrationFit calFit=CalibrationFit.getInstance();
	private final java.text.DecimalFormat df = new java.text.DecimalFormat("0.000#");
    
    public CalibrationPeakTableModel() {
        super(HEADERS,0);
    }    
           
    public void addRow(CalibrationPeak cp)  {
        final Vector temp = new Vector(NUM_COLUMNS);
        temp.addElement(new Integer(cp.getReactionIndex()));
        temp.addElement(cp.getExProjectile());
        temp.addElement(cp.getExResidual());
        try {
            temp.addElement(cp.getRho());
        } catch (KinematicsException ke) {
            System.err.println(ke);
		} catch (NuclearException ke) {
			System.err.println(ke);
		}
        temp.addElement(cp.getChannel());
        final boolean calibrated=calFit.hasFit();
        final String dfault=" ";
        temp.addElement(calibrated ? df.format(calFit.getResidual(getRowCount()))
        : dfault);
        temp.addElement(calibrated ? 
        df.format(calFit.getNormalizedResidual(getRowCount())) : dfault);
        addRow(temp);
    }
    
    void refreshData(){
       while (getRowCount()>0) {
            removeRow(0);
       }
       Iterator peaks = CalibrationPeak.getPeakCollection().iterator();
       while (peaks.hasNext()) {
       		addRow((CalibrationPeak)peaks.next());
       	} 
    }
    
    public String getExportTableText(){
    	final CalibrationFit calfit=CalibrationFit.getInstance();
    	String rval="";
    	for (int i=0, n=getRowCount(); i<n;i++){
    		CalibrationPeak cp=CalibrationPeak.getPeak(i); 
    		int rxnNum = cp.getReactionIndex();
    		SpancReaction sr = SpancReaction.getReaction(rxnNum);  
    		rval += sr.description()+"\t";
    		rval += sr.getMagneticField()+"\t";
    		rval += cp.getExProjectile()+"\t";
    		final Energy exRes = cp.getExResidual();
    		rval += exRes.doubleValue(keV)+"\t"+QuantityUtilities.errorValue(exRes,keV)+"\t";
    		final UncertainNumber channel = cp.getChannel();
    		rval += channel.value+"\t"+channel.error+"\t";
    		try {
    			Quantity rho = cp.getRho();//input
    			rval += rho.doubleValue(eTm)+"\t"+QuantityUtilities.errorValue(rho,eTm)+"\t";
    			rho = calfit.getRho(channel);//from calibration
    			rval += rho.doubleValue(cm)+"\t"+QuantityUtilities.errorValue(rho,cm)+"\t";
    		} catch (Exception e){
    			System.err.println(e);
    		}
    		rval += calfit.getResidual(i)+"\n";
    	}
    	return rval;
    }
    
}