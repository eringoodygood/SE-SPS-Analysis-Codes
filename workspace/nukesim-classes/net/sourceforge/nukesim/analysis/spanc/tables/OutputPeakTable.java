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
import net.sourceforge.nukesim.analysis.spanc.OutputPeak;

/** Table for displaying reactions in Spanc.
 *
 * @author Dale Visser.
 * @version 1.0
 */
public class OutputPeakTable extends javax.swing.JTable {

    /** Constructor.
     */
    public OutputPeakTable()  {
        super(new OutputPeakTableModel());
        setOpaque(true);
    }
    
    public void addRow(OutputPeak op)  {
         ((OutputPeakTableModel)getModel()).addRow(op);
    }
    
    public boolean isCellEditable(int row, int column){
    	return false;
    }
    public void removeRow(int row){
        ((javax.swing.table.DefaultTableModel)getModel()).removeRow(row);
    }
    public void refreshData(){
        ((OutputPeakTableModel)getModel()).refreshData();
    }
    synchronized public void adjustErrors(boolean adjust) {
        ((OutputPeakTableModel)getModel()).adjustErrors(adjust);
    }
    
    public String toString(){
    	return ((OutputPeakTableModel)getModel()).toString();
    }
    
    public String getExportTableText(){
    	return ((OutputPeakTableModel)getModel()).getExportTableText();
    }
    	
}
