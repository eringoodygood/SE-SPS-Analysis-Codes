/***************************************************************
 * Nuclear Simulation Java Class Libraries
 * Copyright (C) 2003 Yale University
 * 
 * Original Developer
 *     Dale Visser (dale@visser.name)
 * 
 * OSI Certified Open Source Software
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the University of Illinois/NCSA 
 * Open Source License.
 * 
 * This program is distributed in the hope that it will be 
 * useful, but without any warranty; without even the implied 
 * warranty of merchantability or fitness for a particular 
 * purpose. See the University of Illinois/NCSA Open Source 
 * License for more details.
 * 
 * You should have received a copy of the University of 
 * Illinois/NCSA Open Source License along with this program; if 
 * not, see http://www.opensource.org/
 **************************************************************/
/*
 * CalibrationFit.java
 *
 * Created on December 20, 2001, 12:30 PM
 */

package net.sourceforge.nukesim.analysis.spanc;
import jade.physics.Length;
import net.sourceforge.nukesim.math.MathException;
import net.sourceforge.nukesim.math.QuantityUtilities;
import net.sourceforge.nukesim.math.UncertainNumber;
import net.sourceforge.nukesim.nuclear.NukeUnits;
import net.sourceforge.nukesim.statistics.PolynomialFitErrY;
import net.sourceforge.nukesim.statistics.StatisticsException;

/**
 * Abstraction class for performing fits of channel versus
 * rho data.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version 1.2
 */
public final class CalibrationFit implements NukeUnits{
    
    private int order = 1;
    private int dof=0;
    private transient PolynomialFitErrY fit=null;
    private boolean canFit=false;
	private static final CalibrationFit instance=new CalibrationFit();

    /** 
     * Creates new CalibrationFit object.
     */
    private CalibrationFit() {
    }
    
    public static CalibrationFit getInstance(){
    	return instance;
    }

	/**
	 * Sets the fit order of the polynomial calibration,
	 * which causes the fit to be recalculated.
	 * 
	 * @param n order of the polynomial used to fit Ch. vs. QBrho
	 * @throws MathException if fit fails
	 */
    public synchronized void setOrder(int n) throws MathException {
        order=n; 
		final UncertainNumber [] x = CalibrationPeak.getX();
		dof = x.length-order-1;
		canFit = dof >= 1;
		if (canFit){
			final Length [] y = CalibrationPeak.getY();
			final double [] yval = new double[y.length];
			final double [] yerr = new double[y.length];
			for (int i=0; i<y.length; i++){
				yval[i]=y[i].doubleValue(cm);
				yerr[i]=yval[i]*y[i].getRelativeError();
			}
			final double [] xval = new double[x.length];
			final double [] xerr = new double[x.length];
			for (int i=0; i<x.length; i++) {
				xval[i]=x[i].value;
				xerr[i]=x[i].error;
			}
			final PolynomialFitErrY tempFit=new PolynomialFitErrY(order,xval,yval,yerr,
			true);
			for (int i=0; i<y.length; i++){
				final double two=2;
				yerr[i]=Math.sqrt(yerr[i]*yerr[i]+
				Math.pow(xerr[i]*tempFit.getSlope(xval[i]),two));
			}
			fit=new PolynomialFitErrY(order,xval,yval,yerr,true);
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
        return fit.getResidual(index);
    }
    
	/**
	 * Returns the number of input peaks fitted to.
	 * @return number of input peaks
	 */
    synchronized public int getDataSize(){
        //return x.length;
        return fit.getDataSize();
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
    public double getChannel0(){
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
    public Length getRho(UncertainNumber channel) throws
    StatisticsException,MathException {
    	synchronized (this){
        	if (fit == null) {
        		setOrder(order);
        	}
        	return Length.lengthOf(QuantityUtilities.quantity(fit.calculateY(channel),cm));
        }
    }
    
	/**
	 * Gives the probability that the calibration actually describes
	 * the input data. In statistical terms, the probability that
	 * the chi-squared statistic would be it's current value or greater
	 * if the calibration curve is correct.
	 * 
	 * @return p-value for the fit
	 */
    public double getPvalue(){
    	synchronized(this){
        	return fit.get_p_value();
        }
    }
    
	/**
	 * Gives more or less complete information about the input
	 * data and calibration curve.
     *
	 * @see java.lang.Object#toString()
	 * @return representation of this fit
	 */
    public String toString(){
    	final String s1="Polynomial fit: order = ";
    	final String s2=", d.o.f. = ";
    	final String s3="ChiSq/d.o.f. = ";
    	final String s4=
    	"p-value (probability that fit describes calibration data): ";
    	final String s5=
    	"rho = a0 + a1*(channel - channel[0]) + ...\nchannel[0] = ";
    	final String s6=" = ";
    	final String s7="Covariance matrix:\n";
    	final String s8="Peak#\tResidual\tResid./sigma\n";
    	final char cr='\n';
        final StringBuffer rval = new StringBuffer(s1).append(order).append(
        s2).append(dof).append(cr).append(s3).append(getReducedChiSq()).append(
        cr).append(s4).append(getPvalue()).append(cr).append(s5).append(
        getChannel0()).append(cr);
        for (int i=0; i<=order; i++){
            rval.append('a').append(i).append(s6).append(
            getCoefficient(i)).append(cr);
        }
        rval.append(s7);
        for (int i=0; i <= order; i++){
            for (int j=0; j<=i; j++){
                rval.append(getCovariance(i,j)).append(' ');
            }
            rval.append(cr);
        }
        rval.append(s8);
        final int size=fit.getDataSize();
        for (int i=0; i< size; i++){
        	final char t='\t';
            rval.append(i).append(t).append(getResidual(i)).append(t).append(
            getNormalizedResidual(i)).append(cr);
        }
        return rval.toString();
    }
    
    
    		

}
