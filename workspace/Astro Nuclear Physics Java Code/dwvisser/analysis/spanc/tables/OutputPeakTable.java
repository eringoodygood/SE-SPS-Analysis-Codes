package dwvisser.analysis.spanc.tables;
import dwvisser.analysis.spanc.OutputPeak;

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
