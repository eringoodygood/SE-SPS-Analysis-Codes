package dwvisser.analysis.spanc.tables;
import javax.swing.table.*;
import dwvisser.analysis.spanc.*;

/**
 * Data model for <code>CoefficientTable</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class CoefficientTableModel extends DefaultTableModel {
    static String [] headers={"Coefficient","Value","Covar 0",
    "Covar 1"};
    
    CalibrationFit calFit;
    
    public CoefficientTableModel(CalibrationFit cf) {
        super(headers,0);
        calFit = cf;
    }    
    
    static java.text.DecimalFormat df = new java.text.DecimalFormat("0.000");
    static java.text.DecimalFormat df_sci = new java.text.DecimalFormat("0.000E0");
    private String format(double number){
        if (Math.abs(number)>=0.01) return df.format(number);
        return df_sci.format(number);
    }
    
    
    synchronized void updateCoefficients() {
       this.setColumnCount(2);
       while (getRowCount()>0) {
            removeRow(0);
       }
        if (calFit.hasFit()){
            int covarDim = calFit.getOrder()+1;
            for (int i=0; i<covarDim; i++) this.addColumn("Covar "+i);
            for (int i=0; i<covarDim; i++) {
                Object [] rowData = new Object[2+covarDim];
                rowData[0] = "a"+i;
                rowData[1] = calFit.getCoefficient(i);
                for (int j=0; j<covarDim; j++){
                    rowData[2+j] = format(calFit.getCovariance(i,j));
                }
                this.addRow(rowData);
            }
        }
    }
           
        
}