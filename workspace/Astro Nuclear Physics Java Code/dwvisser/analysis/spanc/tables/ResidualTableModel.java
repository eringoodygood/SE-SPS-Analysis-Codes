package dwvisser.analysis.spanc.tables;
import javax.swing.table.*;
import dwvisser.analysis.spanc.*;

/**
 * Data model for <code>ResidualTable</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class ResidualTableModel extends DefaultTableModel {
    static String [] headers={"Peak","fitted rho [cm]","Residual [cm]",
    "Resid./Sigma"};
    
    CalibrationFit calFit;
    
    public ResidualTableModel(CalibrationFit cf) {
        super(headers,0);
        calFit = cf;
    }    
    
    static java.text.DecimalFormat df = new java.text.DecimalFormat("0.000#");
    synchronized void updateResiduals() {
        while (getRowCount()>0){
            removeRow(0);
        } 
        if (calFit.hasFit()){
            for (int i=0; i<calFit.getDataSize(); i++){
                Object [] rowData = new Object[4];
                rowData[0] = new Integer(i);
                rowData[1] = df.format(calFit.calculateFit(i));
                rowData[2] = df.format(calFit.getResidual(i));
                rowData[3] = df.format(calFit.getNormalizedResidual(i));
                this.addRow(rowData);
            }
        }
    }
           
        
}