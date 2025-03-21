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
import javax.swing.table.*;
import net.sourceforge.nukesim.analysis.spanc.*;

/**
 * Data model for <code>CoefficientTable</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class CoefficientTableModel extends DefaultTableModel {
    
    static String [] headers={"Coefficient","Value","Covar 0",
    "Covar 1"};    
	private static final java.text.DecimalFormat df = new java.text.DecimalFormat("0.000");
	private static final java.text.DecimalFormat df_sci = new java.text.DecimalFormat("0.000E0");
    
    public CoefficientTableModel() {
        super(headers,0);
    }    
    
    private String format(double number){
        if (Math.abs(number)>=0.01) return df.format(number);
        return df_sci.format(number);
    }
       
    synchronized void updateCoefficients() {
       this.setColumnCount(2);
       while (getRowCount()>0) {
            removeRow(0);
       }
       final CalibrationFit calFit=CalibrationFit.getInstance();
        if (calFit.hasFit()){
            int covarDim = calFit.getOrder()+1;
            for (int i=0; i<covarDim; i++) this.addColumn("Covar "+i);
            for (int i=0; i<covarDim; i++) {
                Object [] rowData = new Object[2+covarDim];
                rowData[0] = "a"+i;
                rowData[1] = calFit.getCoefficient(i);
                for (int j=0; j<covarDim; j++){
                    rowData[2+j] = format(calFit.getCovariance(i,j));
                }
                this.addRow(rowData);
            }
        }
    }
           
        
}