package dwvisser.math;
import java.lang.Math;
import java.io.Serializable;
import java.text.NumberFormat;

public class UncertainNumber implements Serializable {
    
    /**
     * The best estimate of the value.
     */
    public double value;
    
    /**
     * The uncertainty.
     */
    public double error;
    
    /**
     * Constructor.
     */
    public UncertainNumber(double value, double error) {
        this.value=value;
        this.error=error;
    }
    
    public UncertainNumber(double value) {
        this(value,0.0);
    }
    
    public UncertainNumber plus(UncertainNumber x){
        return new UncertainNumber(this.value+x.value,
        Math.sqrt(this.error*this.error+x.error*x.error));
    }
    
    public UncertainNumber minus(UncertainNumber x){
        return new UncertainNumber(this.value-x.value,
        Math.sqrt(this.error*this.error+x.error*x.error));
    }
    
    public UncertainNumber minus(double x){
        return minus(new UncertainNumber(x));
    }
    
    public UncertainNumber times(UncertainNumber x){
        return new UncertainNumber(value*x.value,
        Math.sqrt(error*error*x.value*x.value+
        x.error*x.error*value*value));
    }
    
    public UncertainNumber times(double x) {
        return times(new UncertainNumber(x,0.0));
    }
    
    public UncertainNumber divide(UncertainNumber x) {
        return times(invert(x));
    }
    public UncertainNumber divide(double x) {
        return divide(new UncertainNumber(x,0));
    }
    
    private UncertainNumber invert(UncertainNumber x) {
        return new UncertainNumber(1.0/x.value,x.error/(x.value*x.value));
    }
    
    public String toString(){
        return format();
    }
    
    public String plusMinusString(){
    	return value+" +- "+error;
    }
    
    public boolean equals(Object o){
        if (!this.getClass().isInstance(o)) return false;
        UncertainNumber un = (UncertainNumber)o;
        if (value != un.value) return false;
        if (error != un.error) return false;
        return true; //all conditions passed if this line reached
    }
    
    private String format(){
        NumberFormat fval,ferr;
        int temp;
        
        fval=NumberFormat.getInstance();
        fval.setGroupingUsed(false);
        ferr=NumberFormat.getInstance();
        ferr.setGroupingUsed(false);
        if (error > 0.0) {
            temp=fractionDigits();
            //ferr.setMinimumFractionDigits(temp);
            //ferr.setMaximumFractionDigits(temp);
            fval.setMinimumFractionDigits(temp);
            fval.setMaximumFractionDigits(temp);
            temp=integerDigits();
            ferr.setMinimumIntegerDigits(temp);
            //ferr.setMaximumIntegerDigits(temp);
            fval.setMinimumIntegerDigits(1);
            return fval.format(value)+"("+abbreviatedError()+")";
        } else {//=0.0
            return String.valueOf(value);
        }
        //out[0] = fval.format(value);
        //out[1] = ferr.format(error);
    }
    
    private double log10(double x){
        return Math.log(x)/Math.log(10.0);
    }
    
    /**
     * Given an error, determines the appropriat number of fraction digits to show.
     */
    private int fractionDigits() {
        int out;
        
        //if (err == 0.0) throw new FitException("fractionDigits called with 0.0");
        if (error >= 3.0) {
            out = 0;
        } else if (error >= 1.0) {
            out = 1;
        } else if (firstSigFig(error) <= 2) {
            out = decimalPlaces(error,2);
        } else { // firstSigFig > 2
            out = decimalPlaces(error,1);
        }
        return out;
    }
    
    private int abbreviatedError() {
        long temp=Math.round(Math.floor(log10(error)));//where is error decimal place?
        if (temp>=1) {
            return (int)Math.round(error);
        } else {
            if (firstSigFig(error)<=2) temp -= 1;
            return (int)Math.round(Math.pow(10.0,-temp)*error);
        }
    }
    
    /**
     * Given an error term determine the appropriate number of integer digits
     * to display.
     */
    private int integerDigits(){
        int out;
        
        if (error >= 1.0) {
            out = (int)Math.ceil(log10(error));
        } else  {
            out = 1;
        }
        return out;
    }
    
    /**
     * Given a double, returns the value of the first significant decimal digit.
     */
    private int firstSigFig(double x) {
        char first;
        int rval=0;
        int index=0;
        String sval=""+x;
        boolean notDone=true;
        do {
            first = sval.charAt(index);
            index++;
            if (Character.isDigit(first)){
                rval=Character.digit(first,10);
                notDone = (rval==0); //sets to false if non-zero digit
            }
        } while (notDone);
        //System.out.println("FirstSigFig("+x+") gives "+rval);
        return rval;
    }
    
    /**
     * Given a double between zero and 1, and number of significant figures desired, return
     * number of decimal fraction digits to display.
     */
    private int decimalPlaces(double x,int sigfig) {
        int out;
        int pos;//position of firstSigFig
        
        //if (x <= 0.0 || x >= 1.0) throw new FitException("Must call decimalPlaces() with x in (0,1).");
        //if (sigfig<1) throw new FitException("Can't have zero significant figures.");
        pos = (int)Math.abs(Math.floor(log10(x)));
        out = pos + sigfig - 1;
        return out;
    }
    
    public static void main(String [] args){
        System.out.println(new UncertainNumber(709.8,2));
        System.out.println(new UncertainNumber(709.8,3));
        System.out.println(new UncertainNumber(709.8,.2));
        System.out.println(new UncertainNumber(709.8,.3));
        System.out.println(0.3*1.0);
    }
}

