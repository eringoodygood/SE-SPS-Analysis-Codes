package dwvisser.analysis.spanc.tables;
import javax.swing.*;
import dwvisser.analysis.spanc.SpancReaction;
import dwvisser.nuclear.KinematicsException;
import javax.swing.table.DefaultTableModel;

/** 
 * Table for displaying reactions in Spanc.
 *
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class ReactionTable extends JTable {

    /** 
     * Constructor.
     */
    public ReactionTable() throws KinematicsException {
        super(new ReactionTableModel());
        setOpaque(true);
    }
    
    public boolean isCellEditable(int row, int column){
    	return false;
    }
    
    public void addRow(SpancReaction sr){
         ((ReactionTableModel)getModel()).addRow(sr);
    }
    
    public void removeRow(int row){
        ((DefaultTableModel)getModel()).removeRow(row);
    }
    
    public void refreshData(){
        ((ReactionTableModel)getModel()).refreshData();
    }
}
