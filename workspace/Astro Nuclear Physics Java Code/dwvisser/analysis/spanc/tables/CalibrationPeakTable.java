package dwvisser.analysis.spanc.tables;
import javax.swing.*;
import dwvisser.analysis.spanc.CalibrationPeak;
import dwvisser.analysis.spanc.CalibrationFit;
import javax.swing.table.DefaultTableModel;

/** 
 * Table for displaying reactions in Spanc.
 *
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class CalibrationPeakTable extends JTable {

    /** 
     * Constructor.
     */
    public CalibrationPeakTable()  {
        super(new CalibrationPeakTableModel());
        setOpaque(true);
    }
    
    public boolean isCellEditable(int row, int column){
    	return false;
    }
    
    public void addRow(CalibrationPeak cp)  {
         ((CalibrationPeakTableModel)getModel()).addRow(cp);
    }
    
    public void removeRow(int row){
        ((DefaultTableModel)getModel()).removeRow(row);
    }
    
    public void refreshData(){
        ((CalibrationPeakTableModel)getModel()).refreshData();
    }
    
    public String getExportTableText(CalibrationFit calfit){
    	return ((CalibrationPeakTableModel)getModel()).getExportTableText(calfit);
    }
}
