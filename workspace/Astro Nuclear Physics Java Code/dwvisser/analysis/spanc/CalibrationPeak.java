/*
 * CalibrationPeak.java
 *
 * Created on December 19, 2001, 11:12 AM
 */

package dwvisser.analysis.spanc;
import dwvisser.math.UncertainNumber;
import dwvisser.nuclear.KinematicsException;
import java.util.Vector;
import java.util.Collection;

/**
 *
 * @author  dwvisser
 * @version
 */
public class CalibrationPeak implements java.io.Serializable {
    
    SpancReaction reaction;
    
    double ExProjectile = 0;
    
    UncertainNumber ExResidual = new UncertainNumber(0);
    
    UncertainNumber channel;
    
    static Vector peaks = new Vector(1,1);
    
    /** Creates new CalibrationPeak */
    public CalibrationPeak(SpancReaction reaction, double ExProjectile,
    UncertainNumber ExResidual, UncertainNumber channel) {
        setValues(reaction,ExProjectile,ExResidual,channel);
        peaks.addElement(this);
    }
        
    public void setValues(SpancReaction reaction, double ExProjectile,
    UncertainNumber ExResidual, UncertainNumber channel) {
        this.reaction=reaction;
        this.ExProjectile=ExProjectile;
        this.ExResidual=ExResidual;
        this.channel=channel;
    }
    	
    static public void removePeak(int which){
        peaks.removeElementAt(which);
    }
    
    static public CalibrationPeak getPeak(int which){
    	return (CalibrationPeak)peaks.elementAt(which);
    }
    
    static public void removeAllPeaks(){
        peaks.removeAllElements();
    }
    
    static public Collection getPeakCollection(){
        return peaks;
    }
    
    static public void refreshData(Collection retrievedPeaks){
        peaks.addAll(retrievedPeaks);
    }
    
    
    public UncertainNumber getRho() throws KinematicsException {
        return reaction.getRho(ExProjectile,ExResidual);
    }
    
    public int getReactionIndex(){
        return SpancReaction.getReactionIndex(reaction);
    }
    
    public UncertainNumber getExResidual(){
        return ExResidual;
    }
    
    public double getExProjectile(){
        return ExProjectile;
    }
    
    public UncertainNumber getChannel(){
        return channel;
    }
    
    static public UncertainNumber [] getY(){
        UncertainNumber [] rval = new UncertainNumber[peaks.size()];
        for (int index=0; index<peaks.size(); index++){
            CalibrationPeak cp = (CalibrationPeak)peaks.elementAt(index);
            try {
                rval[index] = cp.getRho();
            } catch (KinematicsException ke) {
                System.err.println(ke);
            }
        }
        return rval;
    }
    
    static public UncertainNumber [] getX(){
        UncertainNumber [] rval = new UncertainNumber[peaks.size()];
        for (int index=0; index<peaks.size(); index++){
            CalibrationPeak cp = (CalibrationPeak)peaks.elementAt(index);
            rval[index] = cp.channel;
        }
        return rval;
    }
    
    public String toString(){
        String rval = "Calibration Peak "+peaks.indexOf(this);
        try {
            rval += " from reaction #"+SpancReaction.getReactionIndex(reaction)+"\n";
            rval += "Ex of projectile: "+ExProjectile+" MeV\n";
            rval += "Ex of residual: "+ExResidual+" MeV\n";
            rval += "rho of projectile: "+getRho()+" cm\n";
            rval += "Peak Centroid: "+channel+"\n";
        } catch (KinematicsException ke) {
            System.err.println(ke);
        }
        return rval;
    }
    
}
