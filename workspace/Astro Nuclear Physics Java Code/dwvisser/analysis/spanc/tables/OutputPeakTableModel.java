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
public class OutputPeakTableModel extends DefaultTableModel {
    static String [] headers={"Reaction","Ex Projectile [MeV]","Channel",
    "rho [cm]","Ex Residual [MeV]"};
    
    private boolean adjustError = false;
    
    public OutputPeakTableModel() {
        super(headers,0);
    }    
           
    static java.text.DecimalFormat df = new java.text.DecimalFormat("0.000#");
    public void addRow(OutputPeak op)  {
        Vector temp = new Vector(5);
        temp.addElement(new Integer(op.getReactionIndex()));
        temp.addElement(new Double(op.getExProjectile()));
        temp.addElement(op.getChannel());
        try {
            temp.addElement(op.getRho(adjustError));
        } catch (KinematicsException ke) {
            System.err.println(ke);
        }
        temp.addElement(op.getExResidual(adjustError));
        addRow(temp);
    }
    
    void refreshData(){
       while (getRowCount()>0) {
            removeRow(0);
       }
       Iterator peaks = OutputPeak.getPeakCollection().iterator();
       while (peaks.hasNext()) addRow((OutputPeak)peaks.next());
    }
    
    synchronized void adjustErrors(boolean adjust) {
        this.adjustError = adjust;
        refreshData();
    }
    
    public String toString(){
    	String rval="";
    	for (int i=0; i<headers.length; i++){
    		rval += headers[i];
    		if (i < headers.length -1){
    			rval += "\t";
    		} else {
    			rval += "\n";
    		}
    	}    	
    	for (int i=0, n=getRowCount(); i<n;i++){
    		OutputPeak op=OutputPeak.getPeak(i);   
    		rval += op.getReactionIndex()+"\t";
    		rval += op.getExProjectile()+"\t";
    		rval += op.getChannel().plusMinusString()+"\t";
    		try {
    			rval += op.getRho(false).plusMinusString()+"\t";
    		} catch (KinematicsException ke){
    			System.err.println(ke);
    		}
    		rval += op.getExResidual(false).plusMinusString()+"\n";
    	}
    	return rval;
    }
    public String getExportTableText(){
    	String rval="";
    	for (int i=0, n=getRowCount(); i<n;i++){
    		OutputPeak op=OutputPeak.getPeak(i); 
    		int rxnNum = op.getReactionIndex();
    		SpancReaction sr = SpancReaction.getReaction(rxnNum);  
    		rval += sr.description()+"\t";
    		rval += sr.getBfield()+"\t";
    		rval += op.getExProjectile()+"\t";
    		UncertainNumber exRes = op.getExResidual(false);
    		rval += exRes.value+"\t"+exRes.error+"\t";
    		rval += op.getChannel().value+"\t"+op.getChannel().error+"\t";
			rval +="\t\t";
    		try {
    			UncertainNumber rho = op.getRho(false);    		
    			rval += rho.value+"\t"+rho.error+"\t";
    		} catch (KinematicsException ke){
    			System.err.println(ke);
    			rval += "Problem calculating calibration rho";
    		}
    		rval+="\n";
    	}
    	return rval;
    }
}