package dwvisser.statistics;
import dwvisser.math.UncertainNumber;

public class LinearFunction implements UncertainFunction {
    
    /**
     * y=a*(x-delx)+b
     */
    UncertainNumber a,b;
    
    /**
     * delx is an offset introduced in x which diagonalizes the covariance
     * matrix of a and b.  It has no error of its own, by assumption.
     */
    double delx;

    public LinearFunction(UncertainNumber a, UncertainNumber b, double delx){
        this.a=a;
        this.b=b;
        this.delx=delx;
    }
    
    public LinearFunction(double a, double siga, double b, double sigb,
            double delx){
        this(new UncertainNumber(a,siga), new UncertainNumber(b,sigb),delx);
    }
    
    public LinearFunction(double a, double b, double delx){
        this(a, 0.0, b, 0.0,delx);
    } 

    /**
     * Given an array of uncertain numbers, return back a function value.
     */
    public UncertainNumber evaluate(UncertainNumber [] x) throws
            StatisticsException{
        UncertainNumber xp;
        
        if (x.length > 1) throw new StatisticsException("LinearFit: too many"
                +" arguments: "+x.length);
                
        //Gives the appropriate number to use in the function. 
        xp = new UncertainNumber(x[0].value-delx,x[0].error);    
        return a.plus(b.times(xp));
    }
    
    public double valueAt(double x){
        return a.value+b.value*x;
    }
}
