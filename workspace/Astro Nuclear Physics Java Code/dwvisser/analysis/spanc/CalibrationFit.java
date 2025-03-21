/*
 * CalibrationFit.java
 *
 * Created on December 20, 2001, 12:30 PM
 */

package dwvisser.analysis.spanc;
import dwvisser.math.*;
import dwvisser.statistics.*;

/**
 * Abstraction class for performing fits of channel versus
 * rho data.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale Visser</a>
 */
public class CalibrationFit implements java.io.Serializable {
    
    int order = 1;
    int dof=0;
    
    UncertainNumber [] x;
    UncertainNumber [] y;
    transient PolynomialFitErrY fit;
    private boolean canFit=false;

    /** 
     * Creates new CalibrationFit object.
     */
    public CalibrationFit() {
    }

	/**
	 * Sets the fit order of the polynomial calibration,
	 * which causes the fit to be recalculated.
	 * 
	 * @param n order of the polynomial used to fit Ch. vs. QBrho
	 * @return boolean
	 * @throws MathException if fit fails
	 */
    synchronized public boolean setOrder(int n) throws MathException {
        order=n; 
        refreshData();
        return canFit;
    }
    
    private void refreshData() throws MathException {
        x = CalibrationPeak.getX();
        y = CalibrationPeak.getY();
        double [] yval = new double[y.length];
        double [] yerr = new double[y.length];
        for (int i=0; i<y.length; i++){
            yval[i]=y[i].value;
            yerr[i]=y[i].error;
        }
        dof = x.length-order-1;
        canFit = dof >= 1;
        if (canFit){
            double [] xval = new double[x.length];
            double [] xerr = new double[x.length];
            for (int i=0; i<x.length; i++) {
                xval[i]=x[i].value;
                xerr[i]=x[i].error;
            }
            fit = new PolynomialFitErrY(order,xval,yval,yerr,true);
            for (int i=0; i<y.length; i++){
                yerr[i]=Math.sqrt(yerr[i]*yerr[i]+Math.pow(xerr[i]*fit.getSlope(xval[i]),2));
            }
            fit = new PolynomialFitErrY(order,xval,yval,yerr,true);
        } else {
            order = 1;
        }
    }
    
	/**
	 * Returns the normalized residual for an input peak,
	 * which is the residual divided by its error bar.
	 * 
	 * @param index which input peak to get residual for
	 * @return residual divided by its error bar
	 */
    synchronized public double getNormalizedResidual(int index){
        return fit.getNormalizedResidual(index);
    }
    
	/**
	 * Returns <code>true</code> if input data have been
	 * received, a fit order was set, and it was possible
	 * to do a fit with at least one degree of freedom.
	 * 
	 * @return whether a fit is available
	 */
    synchronized public boolean hasFit(){
        return canFit;
    }
    
	/**
	 * Returns the i'th order coefficient of the calibration fit
	 * and its uncertainty.
	 * @param i which order of coefficient to retrieve
	 * @return the i'th order coefficient
	 */
    synchronized public UncertainNumber getCoefficient(int i){
        return new UncertainNumber(fit.getFitCoefficients()[i], 
        fit.getCoefficientErrors()[i]);
    }
    
	/**
	 * Gets a covariance matrix element.
	 * @param i row index
	 * @param j column index
	 * @return double covariance between i and j coefficients
	 */
    synchronized public double getCovariance(int i, int j){
        return fit.getCovarianceMatrix().element[i][j];
    }
        
	/**
	 * Gets the polynomial order of the calibration fit.
	 * @return the fit order
	 */
    synchronized public int getOrder(){
        return order;
    }
    
	/**
	 * Returns the  residual for an input peak.
	 * 
	 * @param index which input peak to get residual for
	 * @return residual 
	 */
    synchronized public double getResidual(int index){
        return fit.residual[index];
    }
    
	/**
	 * Returns the number of input peaks fitted to.
	 * @return number of input peaks
	 */
    synchronized public int getDataSize(){
        return x.length;
    }
    
	/**
	 * Returns the QBrho value from the fit for the specified 
	 * input peak.
	 * @param index which input peak
	 * @return fit QBrho value
	 */
    synchronized public double calculateFit(int index){
        return fit.calculateFitValue(index);
    }
    
	/**
	 * Returns the number of degrees of freedom in the fit,
	 * which is the number of input peaks minus the fit order
	 * minus 1.  I.e., there are fit order plus one parameters
	 * to a fit.
	 * 
	 * @return degrees of freedom
	 */
    synchronized public int getDOF(){
        return dof;
    }
    
	/**
	 * Returns the calculated chi-square statistic divided
	 * by the degrees of freedom.
	 * 
	 * @return the reduced chi-square statistic from the fit
	 */
    synchronized public double getReducedChiSq(){
        return fit.getReducedChiSq();
    }
    
	/**
	 * Returns the (unweighted) mean of the input peak centroids,
	 * which is used as the offset of the channel axis for the
	 * purposes of the fit.  This decouples the dominant linear
	 * term from the higher order terms as much as possible.
	 * 
	 * @return unweighted mean mean channel
	 */
    synchronized public double getChannel0(){
        return fit.getMeanX();
    }
    
	/**
	 * Returns a rho value for a specified channel and uncertainty.
	 * 
	 * @param channel centroid and uncertainty of a peak
	 * @return rho value and uncertainty from the calibration
	 * @throws StatisticsException if the calculation fails
	 * @throws MathException if the calculation fails
	 */
    synchronized public UncertainNumber getRho(UncertainNumber channel) throws
    StatisticsException,MathException {
        if (fit == null) setOrder(order);
        return fit.calculateY(channel);
    }
    
	/**
	 * Gives the probability that the calibration actually describes
	 * the input data. In statistical terms, the probability that
	 * the chi-squared statistic would be it's current value or greater
	 * if the calibration curve is correct.
	 * 
	 * @return p-value for the fit
	 */
    synchronized public double getPvalue(){
        return fit.get_p_value();
    }
    
	/**
	 * Gives more or less complete information about the input
	 * data and calibration curve.
     *
	 * @see java.lang.Object#toString()
	 */
    public String toString(){
        String rval = "Polynomial fit: order = "+order+", d.o.f. = "+dof+"\n";
        rval += "ChiSq/d.o.f. = "+getReducedChiSq()+"\n";
        rval += "p-value (probability that fit describes calibration data): "+
        getPvalue()+"\n";
        rval += "rho = a0 + a1*(channel - channel[0]) + ...\n";
        rval += "channel[0] = "+getChannel0()+"\n";
        for (int i=0; i<=order; i++){
            rval += "a"+i+" = "+getCoefficient(i)+"\n";
        }
        rval += "Covariance matrix:\n";
        for (int i=0; i <= order; i++){
            for (int j=0; j<=i; j++){
                rval += getCovariance(i,j)+" ";
            }
            rval += "\n";
        }
        rval += "Peak#\tResidual\tResid./sigma\n";
        for (int i=0; i< x.length; i++){
            rval += i+"\t"+getResidual(i)+"\t"+getNormalizedResidual(i)+"\n";
        }
        return rval;
    }
    
    
    		

}
