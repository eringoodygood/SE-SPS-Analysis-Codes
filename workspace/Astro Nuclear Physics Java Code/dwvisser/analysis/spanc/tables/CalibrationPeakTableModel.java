package dwvisser.analysis.spanc.tables;
import javax.swing.table.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;
import dwvisser.analysis.spanc.*;
import java.util.Vector;
import java.util.Iterator;

/**
 *
 * @author Dale Visser
 * @version 1.0
 */
public class CalibrationPeakTableModel extends DefaultTableModel {
    static String [] headers={"Reaction","Ex Projectile [MeV]","Ex Residual [MeV]",
    "rho [cm]","Channel"};
    
    public CalibrationPeakTableModel() {
        super(headers,0);
    }    
           
    static java.text.DecimalFormat df = new java.text.DecimalFormat("0.000#");
    public void addRow(CalibrationPeak cp)  {
        Vector temp = new Vector(5);
        temp.addElement(new Integer(cp.getReactionIndex()));
        temp.addElement(new Double(cp.getExProjectile()));
        temp.addElement(cp.getExResidual());
        try {
            temp.addElement(cp.getRho());
        } catch (KinematicsException ke) {
            System.err.println(ke);
        }
        temp.addElement(cp.getChannel());
        addRow(temp);
    }
    void refreshData(){
       while (getRowCount()>0) {
            removeRow(0);
       }
       Iterator peaks = CalibrationPeak.getPeakCollection().iterator();
       while (peaks.hasNext()) addRow((CalibrationPeak)peaks.next());
    }
    
    public String getExportTableText(CalibrationFit calfit){
    	String rval="";
    	for (int i=0, n=getRowCount(); i<n;i++){
    		CalibrationPeak cp=CalibrationPeak.getPeak(i); 
    		int rxnNum = cp.getReactionIndex();
    		SpancReaction sr = SpancReaction.getReaction(rxnNum);  
    		rval += sr.description()+"\t";
    		rval += sr.getBfield()+"\t";
    		rval += cp.getExProjectile()+"\t";
    		UncertainNumber exRes = cp.getExResidual();
    		rval += exRes.value+"\t"+exRes.error+"\t";
    		UncertainNumber channel = cp.getChannel();
    		rval += channel.value+"\t"+channel.error+"\t";
    		try {
    			UncertainNumber rho = cp.getRho();//input
    			rval += rho.value+"\t"+rho.error+"\t";
    			rho = calfit.getRho(channel);//from calibration
    			rval += rho.value+"\t"+rho.error+"\t";
    		} catch (Exception e){
    			System.err.println(e);
    		}
    		rval += calfit.getResidual(i)+"\n";
    	}
    	return rval;
    }
    
}