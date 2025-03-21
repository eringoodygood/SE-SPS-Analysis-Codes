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
import dwvisser.analysis.spanc.Target;
import dwvisser.nuclear.NuclearException;

/**
 *
 * @author  Dale Visser
 * @version 1.2
 * @since 1.0 (17 Dec 2001)
 */
public class TargetDefinitionTable extends javax.swing.JTable {

    /** Creates new TargteDefinitionTable */
    public TargetDefinitionTable(Target target) {
        super(new TargetDefinitionTableModel(target));
    }

    public TargetDefinitionTable(){
        super(new TargetDefinitionTableModel());
    }
    
    /**
     * Returns a Target object constructed from entries in the table.
     */
    public Target makeTarget(String name) throws NuclearException {
        return ((TargetDefinitionTableModel)getModel()).makeTarget(name);
    }
    
    public void addRow(){
        ((TargetDefinitionTableModel)getModel()).addRow();
    }
    
    public void removeRow(int row){
        ((TargetDefinitionTableModel)getModel()).removeLayer(row);
    }
}
