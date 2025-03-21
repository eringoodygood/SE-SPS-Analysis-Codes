package dwvisser.analysis.spanc.tables;
import javax.swing.*;
import dwvisser.analysis.spanc.CalibrationFit;

/** 
 * Table for fit residuals in Spanc.
 *
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class ResidualTable extends JTable {

    /** 
     * Constructor.
     * 
     * @param cf object containing the particular fit
     */
    public ResidualTable(CalibrationFit cf)  {
        super(new ResidualTableModel(cf));
        setOpaque(true);
    }
    
    public boolean isCellEditable(int row, int column){
    	return false;
    }
    
    synchronized public void updateResiduals()  {
        ((ResidualTableModel)getModel()).updateResiduals();
    }
}
