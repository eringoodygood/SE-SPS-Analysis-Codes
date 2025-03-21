/*
 * CalibrationPeak.java
 *
 * Created on December 19, 2001, 11:12 AM
 */

package dwvisser.analysis.spanc;
import dwvisser.math.MathException;
import dwvisser.math.UncertainNumber;
import dwvisser.nuclear.KinematicsException;
import java.util.Vector;
import java.util.Collection;
import dwvisser.statistics.StatisticsException;

/**
 *
 * @author  dwvisser
 * @version 
 */
public class OutputPeak implements java.io.Serializable {

    SpancReaction reaction;
    static CalibrationFit fit;
    
    double ExProjectile = 0;
    
    UncertainNumber ExResidual = new UncertainNumber(0);
    
    UncertainNumber channel;
    
    UncertainNumber rho;
    
    static Vector peaks = new Vector(1,1);
    
    /** Creates new OutputPeak */
    public OutputPeak(SpancReaction reaction, double ExProjectile, 
    UncertainNumber channel, CalibrationFit cf) throws KinematicsException,
    StatisticsException, MathException {
        fit=cf;
    	setValues(reaction,ExProjectile,channel);
        peaks.addElement(this);
    }
    
    public void setValues(SpancReaction reaction, double ExProjectile, 
    UncertainNumber channel) throws KinematicsException,
    MathException,StatisticsException {
        this.reaction=reaction;
        this.ExProjectile=ExProjectile;
        this.channel=channel;
        calculate();
    }    	
    
    void calculate() throws KinematicsException, StatisticsException, 
    MathException {
        rho = fit.getRho(channel);
        ExResidual = reaction.getExResid(ExProjectile,rho);     
    }
    
    static public void setCalibration(CalibrationFit cf){
        fit=cf;
    }
    
    static public void removePeak(int which){
        peaks.removeElementAt(which);
    }
    
    static public OutputPeak getPeak(int which){
    	return (OutputPeak)peaks.elementAt(which);
    }
    
    static public void removeAllPeaks(){
        peaks.removeAllElements();
    }
    
    static public Collection getPeakCollection(){
        return peaks;
    }
    
    static public void refreshData(Collection retrievedPeaks)  {
        peaks.addAll(retrievedPeaks);
    }
    
    static public void recalculate() throws KinematicsException,
    StatisticsException, MathException {
        java.util.Iterator iter = peaks.iterator();
        while (iter.hasNext()) ((OutputPeak)iter.next()).calculate();
    }
    
    public UncertainNumber getRho(boolean adjustError) throws KinematicsException {
        //UncertainNumber temp = reaction.getRho(ExProjectile,ExResidual);
        if (adjustError){
            return new UncertainNumber(rho.value, rho.error*Math.sqrt(Math.max(1,
            fit.getReducedChiSq())));
        } else {
            return rho;
        }
    }
    
    public int getReactionIndex(){
        return SpancReaction.getReactionIndex(reaction);
    }
    
    public UncertainNumber getExResidual(boolean adjustError){
        if (adjustError) {
            return new UncertainNumber(ExResidual.value, ExResidual.error*
            Math.sqrt(Math.max(1,fit.getReducedChiSq())));
        } else {
            return ExResidual;
        }
    }
    
    public double getExProjectile(){
        return ExProjectile;
    }
    
    public UncertainNumber getChannel(){
        return channel;
    }
    
    public String toString(){
        String rval = "Output Peak for Reaction #"+getReactionIndex()+"\n";
        rval += "Ex projectile [MeV] = "+ExProjectile+" MeV\n";
        rval += "Centroid Channel = "+channel+"\n";
        try {
            rval += "rho from calibration = "+getRho(false)+
            " cm, adjusted error = "+getRho(true).error+" cm\n";
        } catch (KinematicsException ke){
            System.err.println(ke);
            rval += "ERROR: Problem calculating Brho\n";
        }
        rval += "Ex residual from calibration = "+getExResidual(false)+
        " MeV, adjusted error = "+getExResidual(true).error*1000+" keV\n";
        return rval;
    }
        
}