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
package dwvisser.statistics;
import dwvisser.math.Matrix;
import dwvisser.analysis.GJENice;
import dwvisser.math.UncertainNumber;

/**
 * Given a data set, performs a linear regression, then can be queried for the results.
 * Based on code in Numerical Recipes in C
 *
 * @author Dale Visser
 * @version 1.0
 */
public final class PolynomialFitErrY {

	private final double[] a; // fit coefficients y=a[0]+a[1]*x+a[2]*x^2+...
	private final double[] siga; //sqrt(diagonal elements of covar)
	private Matrix covar; // covariance matrix for coefficients
	private double chi2; // chi-sqared statistic
	private double q; // p-value
	private final int dof; //degrees of freedom
	private final int degree; //order of polynomial of fit
	private final int ndata;
	private final boolean weighted;
	private double[] x, y, sig;
	private double[] residual;
	private final double meanx;
	private final boolean centered;
	private double MSE, SSX;

	/**
	 * Creates and performs polynomial regression on weighted data set.
	 *
	 * @param x the x coordinates of the points
	 * @param y the y coordinates of the points
	 * @param sig the error bars on the y coordinates
	 * @param order the order of the polynomial to use
	 */
	public PolynomialFitErrY(
		int order,
		double[] x,
		double[] y,
		double[] sig,
		boolean centered)
		throws StatisticsException {
		if (x.length != y.length) {
			throw new StatisticsException(
				getClass().getName()
					+ ": Array lengths not equal x["
					+ x.length
					+ "] and y["
					+ y.length
					+ "].");
		}
		if (x.length != sig.length) {
			throw new StatisticsException(
				getClass().getName()
					+ ": Array lengths not equal x,y["
					+ x.length
					+ "] and sig["
					+ sig.length
					+ "].");
		}
		if (order < 1) {
			throw new StatisticsException(
				getClass().getName()
					+ ": Order of fit polynomial must be 1 or greater.");
		}
		if (x.length <= (order + 1)) {
			throw new StatisticsException(
				getClass().getName()
					+ ": Data set must be at least size "
					+ (order + 2)
					+ " to fit with degree "
					+ order
					+ " polynomial.");
		}
		ndata = x.length;
		degree = order;
		dof = ndata - degree - 1;
		a = new double[order + 1];
		siga = new double[order + 1];
		weighted = true;
		this.x = new double[x.length];
		System.arraycopy(x, 0, this.x, 0, x.length);
		this.centered = centered;
		meanx=calculateMeanX();
		if (centered){
			for (int i = 0; i < ndata; i++) {
				setX(x[i] - meanx,i);
			}
		}		
		this.y = new double[y.length];
		System.arraycopy(y, 0, this.y, 0, y.length);
		this.sig = new double[sig.length];
		System.arraycopy(sig, 0, this.sig, 0, sig.length);
		fit();
	}
	
	public int getDataSize(){
		return x.length;
	}

	public PolynomialFitErrY(int order, double[] x, double[] y, double[] sig)
		throws StatisticsException {
		this(order, x, y, sig, false);
	}

	private final double calculateMeanX() {
		double sx = 0.0;
		for (int i = 0; i < ndata; i++) {
			sx += x[i];
		}
		return sx/ndata;
	}
	
	private void setX(double val, int i){
		synchronized (this) {
			x[i]=val;
		}
	}

	/**
	 * @return the arithmetic mean of the input x-values 
	 */
	public double getMeanX() {
		return meanx;
	}

	/**
	 * @return the fit coefficients
	 */
	public double[] getFitCoefficients() {
		return a;
	}

	/**
	 * @return the errors in the coefficients
	 */
	public double[] getCoefficientErrors() {
		return siga;
	}

	/**
	 * @return the errors in the coefficients adjusted up for chi^2
	 */
	public double[] getAdjustedCoeffErrors() {
		final double [] rval;
		if (getReducedChiSq() > 1.0) {
			rval = new double[siga.length];
			final double fac = Math.sqrt(getReducedChiSq());
			for (int i = 0; i < siga.length; i++) {
				rval[i] = siga[i] * fac;
			}
		} else {
			rval=siga;
		}
		return rval;
	}

	/**
	 * @return chi^2
	 */
	public double getChiSq() {
		return chi2;
	}
	
	/**
	 * @return chi^2 / d.o.f.
	 */
	public double getReducedChiSq() {
		return chi2 / dof;
	}

	/**
	 * @return the number of degrees of freedom in the fit.
	 */
	public int getDegreesOfFreedom() {
		return dof;
	}
	
	/**
	 * @return the P-value for the fit
	 */
	public double get_p_value() {
		return q;
	}

	/**
	 * Calculate the fit curve at the given x-value.
	 *
	 * @return the fit curve at the given x-axis value
	 * @param xp the value to calculate at
	 */
	public double calculateY(double xp) {
		double rval = 0.0;
		for (int i = 0; i <= degree; i++) {
			rval += a[i] * Math.pow(xp - meanx, i);
		}
		return rval;
	}

	/**
	 * Calculate the Y value with uncertainty for an uncertain x.
	 *
	 * @return uncertain y-value on fit curve
	 * @throws dwvisser.statistics.StatisticsException if there's a 
	 * problem
	 */
	public UncertainNumber calculateY(UncertainNumber ux)
		throws dwvisser.statistics.StatisticsException {
		final double curveError =
			dwvisser.statistics.MiscMath.findOneSigmaT(dof)
				* Math.sqrt(
					MSE
						* (1.0 / x.length
							+ Math.pow(ux.value - meanx, 2) / SSX));
		final double value = calculateY(ux.value);
		final double xError = Math.abs(calculateY(ux.value + ux.error) - value);
		return new UncertainNumber(
			value,
			Math.sqrt(curveError * curveError + xError * xError));
	}

	/**
	 * Get the slope of the fit at the given point.
	 *
	 * @param xp place to calculate the slope
	 */
	public double getSlope(double xp) {
		double rval = 0.0;
		for (int i = 1; i <= degree; i++) {
			rval += i * a[i] * Math.pow(xp - meanx, i - 1);
		}
		return rval;
	}
	
	/**
	 * Calculate the fit curve at one of the input points.
	 *
	 * @param index of the point to calculate for
	 */
	public double calculateFitValue(int index) {
		return calculateY(x[index] + meanx);
	}

	/**
	 * Inverts x and y data sets.  X error bars are determined using
	 * sigy/y ratios.  Useful for inverting linear calibrations of
	 * positive sets (e.g., silicon detector channel vs. energy).
	 *
	 * @param cen whether to expand the fit around the middle of
	 * the data
	 * @return the reverse fit
	 * @throws StatisticsException if the fit can't work
	 */
	public PolynomialFitErrY invertFit(boolean cen)
		throws StatisticsException {
		final double[] newsigy = new double[x.length];
		final double[] newx = new double[x.length];
		final double[] newy = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			newsigy[i] = sig[i] / y[i] * x[i];
			newx[i] = y[i];
			newy[i] = x[i];
		}
		return new PolynomialFitErrY(degree, newx, newy, newsigy, cen);
	}

	private void fit() throws StatisticsException {
		final Matrix A = new Matrix(x.length, degree + 1); //A[ij] = x[j]/sig[i]
		final Matrix b = new Matrix(x.length, 1); //b[i]=y[i]/sig[i]
		setChi2(0.0);
		/* assumed weighted fit */
		for (int r = 0; r < A.element.length; r++) {
			b.element[r][0] = y[r] / sig[r];
			for (int c = 0; c < A.element[0].length; c++) {
				A.element[r][c] = Math.pow(x[r], c) / sig[r];
			}
		}
		final Matrix alpha = new Matrix(A.transpose(), A, '*');
		final Matrix beta = new Matrix(A.transpose(), b, '*');
		try {
			final GJENice gj = new GJENice(alpha, beta);
			setCovar(gj.getInverse());
			for (int i = 0; i <= degree; i++) {
				setA(gj.getSolution().element[i][0],i);
			}
		} catch (dwvisser.math.MathException me) {
			throw new StatisticsException(me.getMessage());
		}
		computeSigA();
		computeResiduals();
		final double half=0.5;
		final int two=2;
		setQ(MiscMath.gammq(half * (ndata - two), half * (chi2)));
	}
	
	private void setA(double x, int i){
		synchronized(this){
			a[i]=x;
		}
	}
	
	private void setChi2(double x){
		synchronized (this){
			chi2=x;
		}
	}
	
	private void setCovar(Matrix m){
		synchronized (this){
			covar=m;
		}
	}
	
	private void setQ(double x){
		synchronized (this){
			q=x;
		}
	}

	private void computeSigA() {
		synchronized (this){
			for (int i = 0; i < siga.length; i++) {
				siga[i] = Math.sqrt(covar.element[i][i]);
			}
		}
	}

	private void computeResiduals() {
		synchronized (this){
		residual = new double[x.length];
		double weightSum = 0.0;
		double SSE = 0.0;
		SSX = 0.0;
		chi2 = 0.0;
		for (int i = 0; i < x.length; i++) {
			residual[i] = y[i] - calculateFitValue(i);
			chi2 += Math.pow(residual[i] / sig[i], 2.0);
			final double weight = Math.pow(sig[i], -2.0);
			weightSum += weight;
			SSX += x[i] * x[i];
		}
		SSE = chi2 / weightSum;
		MSE = SSE / dof;
		}
	}

	/**
	 * @return the covariance matrix of the fit
	 */
	public Matrix getCovarianceMatrix() {
		return covar;
	}

	/**
	 * @return the covariance matrix adjusted upward by sqrt(chi^2/dof) if > 1
	 */
	public Matrix getAdjustedCovarMatrix() {
		final Matrix rval;
		if (getReducedChiSq() > 1.0) {
			rval=new Matrix(Math.sqrt(getReducedChiSq()), covar);
		} else {
			rval=covar;
		}
		return rval;
	}

	/**
	 * Returns the string representation of this fit.
	 */
	public String toString() {
		final char cr='\n';
		final StringBuffer rval = new StringBuffer();
		if (weighted) {
			rval.append("Weighted ");
		} else {
			rval.append("Unweighted ");
		}
		rval.append("polynomial (order ").append(degree);
		rval.append(") regression of ").append(ndata).append(" points:\n");
		rval.append("  Reduced Chi-Squared Statistic = ");
		rval.append(getReducedChiSq()).append(cr).append("  P-Value = ");
		rval.append(q).append(cr);
		if (centered){
			rval.append("x-offset used: ").append(meanx).append(cr);
		}
		rval.append("\nCoefficients:\n");
		for (int i = 0; i < a.length; i++) {
			rval.append("\t a").append(i).append(": ").append(a[i]);
			rval.append(" +/- ").append(siga[i]).append(cr);
		}
		rval.append("Covariance Matrix:\n").append(covar.toString(5));
		rval.append(cr);
		if (getReducedChiSq() > 1.0) {
			rval.append("Adjusted for chisq value: \nCoefficients:\n");
			final double[] smod = getAdjustedCoeffErrors();
			for (int i = 0; i < a.length; i++) {
				rval.append("\t a").append(i).append(": ").append(a[i]);
				rval.append(" +/- ").append(smod[i]).append(cr);
			}
			rval.append("Covariance Matrix:\n");
			rval.append(getAdjustedCovarMatrix().toString(5)).append(cr);
		}
		return rval.toString();
	}

	public double getNormalizedResidual(int index) {
		return residual[index] / sig[index];
	}
	
	public double getResidual(int index){
		return residual[index];
	}

}
