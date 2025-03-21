package dwvisser.analysis.spanc.tables;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import dwvisser.analysis.spanc.CalibrationFit;


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
     * 
     * @param cf the object containing a calibration fit
     */
    public CoefficientTable(CalibrationFit cf)  {
        super(new CoefficientTableModel(cf));
        setOpaque(true);
    }
    
    public boolean isCellEditable(int row, int column){
    	return false;
    }
    
    synchronized public void updateCoefficients()  {
        //if (getColumnCount() > 2) removeColumnSelectionInterval(2, getColumnCount()-1);
        ((CoefficientTableModel)getModel()).updateCoefficients();
    }
    public void removeRow(int row){
        ((DefaultTableModel)getModel()).removeRow(row);
    }
}
