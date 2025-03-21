/*
 * TargteDefinitionTable.java
 *
 * Created on December 17, 2001, 2:30 PM
 */

package dwvisser.analysis.spanc.tables;
import dwvisser.analysis.spanc.Target;
import dwvisser.nuclear.NuclearException;

/**
 *
 * @author  Dale
 * @version 
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
