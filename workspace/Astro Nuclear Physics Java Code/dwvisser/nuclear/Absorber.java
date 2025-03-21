package dwvisser.nuclear;
import java.io.*;

/**
 * An abstraction of a thickness of material acting
 * as a stopper of energetic ions.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public abstract class Absorber implements Serializable {

    /** 
     * Units of thickness, ug/cm^2, mg/cm^2, cm
     */
    public final static int MICROGRAM_CM2 = 1;
    public final static int MILLIGRAM_CM2 = 2;
    public final static int CM            = 3;
    public final static int MIL           = 4;

    protected int [] Z;
    
    /**
     * Densities in g/cm^3.  (E.g. water would be ~ 1.0).
     */
    protected double [] density;
    
    /**
     * Fraction of nuclei that are this species.
     */
    protected double [] fractions;
    
    protected double thickness;

    /**
     * Returns thickness in micrograms/cm^2.
     */
    public abstract double getThickness();
    
    protected double [] setFractions(double [] fin){
        double [] fout = new double[fin.length];
        double sum=getSum(fin);
        for (int i=0;i<fin.length;i++) fout[i]=fin[i]/sum;
        return fout;
    }
    
    private double getSum(double [] fin){
        double sum = 0.0;
        for (int i=0;i<fin.length;i++) sum += fin[i];
        return sum;
    }
    
    public int [] getElements(){
        return Z;
    }
    
    public double [] getFractions(){
        return fractions;
    }
    
    public void setThickness(double value, int units){
        if (units==MICROGRAM_CM2){
            thickness=value;
        } else if (units==MILLIGRAM_CM2) {
            thickness=value*1000.0;
        } else if (units==CM) {
            thickness=getDensity() /*g/cm^3*/ *1000000.0 /*ug/cm^3*/*value;
        }
    }
    
    public void setThickness(double value){
        setThickness(value,MICROGRAM_CM2);
    }
    
    /**
     * Returns mass density of absorber in g/cm^3.
     */
    public abstract double getDensity();
    
    /**
     * Returns new absorber identical to this one, with thickness multiplied
     * by <code>factor</code>.
     */
    public abstract Absorber getNewInstance(double factor);
}

