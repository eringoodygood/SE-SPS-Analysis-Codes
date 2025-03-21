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
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** 
 * Table for displaying calibration coefficients in 
 * Spanc.
 *
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class CoefficientTable extends JTable {

    /** 
     * Constructor.
     */
    public CoefficientTable()  {
        super(new CoefficientTableModel());
        setOpaque(true);
    }
    
    public boolean isCellEditable(int row, int column){
    	return false;
    }
    
    synchronized public void updateCoefficients()  {
        ((CoefficientTableModel)getModel()).updateCoefficients();
    }
    
    public void removeRow(int row){
        ((DefaultTableModel)getModel()).removeRow(row);
    }
}
