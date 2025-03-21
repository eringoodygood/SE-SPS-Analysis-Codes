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
package dwvisser.analysis;

import java.util.*;

/**
 * This abstract class uses LevenbergMarquadt to do non-linear parametric function fitting.
 * An actual class should define additional parameters and add them to <code>parameters</code>,
 * It should implement <code>estimate()</code>, <code>valueAt()</code>, and <code>derivative()<code>.
 *
 * @author  Dale Visser
 * @version 0.5, 8/28/98
 *
 * @see      #valueAt
 * @see      #derivative
 * @see      LevenbergMarquadt
 * @see      GaussianFit
 */
public abstract class NonLinearFit {

	/**
	 * The histogram to be fitted.
	 */
	protected double[] counts;

	/**
	 * The errors associatied with <code>counts</code>.
	 */
	protected double[] errors;

	/**
	 * does the actual matrix algebra to find the best fit
	 */
	protected LevenbergMarquadt lm;

	/**
	 * the calculated reduced chi-squared statistic
	 */
	protected Parameter chisq;

	/**
	 * the <code>int</code> value of <code>lo</code>
	 */
	protected int minCH;

	/**
	 * the <code>int</code> value of <code>hi</code>
	 */
	protected int maxCH;

	private List parameters; //all fit parameters
	private Map parameterTable=Collections.synchronizedMap(new HashMap());

	/**
	 * Class constructor.  This is still an abstract class.  Specific subclass constructors
	 * will call this before executing their own constructors.
	 */
	public NonLinearFit(
		double[] spectrum,
		double[] errors,
		int minCh,
		int maxCh) {
		parameters = new Vector();
		parameterTable = new Hashtable();
		chisq =
			new Parameter(
				"ChiSq/dof",
				Parameter.DOUBLE,
				Parameter.NO_ERROR,
				Parameter.OUTPUT);
		addParameter(chisq);
		this.minCH = minCh;
		this.maxCH = maxCh;
		this.counts = spectrum;
		this.errors = errors;
	}

	/**
	 * Adds a parameter to the fit.
	 */
	protected final void addParameter(Parameter newParameter) {
		parameters.add(newParameter);
		parameterTable.put(newParameter.getName(), newParameter);
	}

	/**
	 * Estimate parameters for which the user requests estimation. Unnecessary to implement, but very useful since
	 * good guesses are usually needed to find the fit.
	 *
	 * @exception   FitException      thrown if unrecoverable error occurs during estimation
	 */
	public abstract void estimate() throws FitException;

	/**
	 * Evaluates at x for given parameters.
	 *
	 * @param  x   value at which to evaluate the fit function
	 * @return      value of fit function at <code>x</code>
	 */
	public abstract double valueAt(double x);

	/**
	 * Evaluates derivative with respect to <code>parameterName</code> at <code>x</code>.
	 *
	 * @param   parameterName   the name of the parameter to differentiate with respect to
	 * @param   x    value to evalueate at
	 * @return      df(<code>x</code>)/d(<code>parameterName</code>) at x
	 */
	public abstract double derivative(double x, String parameterName);

	/**
	 * Perform fit calulation and return a status <code>String</code>.
	 * Calls <code>LevenbergMarquadt</code> several times, which determines changes in
	 * parameter values likely to reduce chi-squared.  When reductions are no longer significant,
	 * it stops.  If there is no covergence, it stops after 10 iterations.
	 *
	 * @exception   FitException      thrown if unrecoverable error occurs during fit
	 * @return      message with number of iterations and degrees of freedom to fit
	 */
	public String doFit() throws FitException {
		double chiSq, newChiSq;
		int numIter = 1;
		final int MAX_ITERATIONS = 10;
		final int MAX_SMALL = 2;
		//total non- or marginal improvements before giving up
		boolean close;
		boolean quit;
		int smallCounter = 0;
		String returnVal = "Note: errors not independent.";

		lm = new LevenbergMarquadt(this);
		lm.setup(counts, errors, minCH, maxCH);

		try {
			lm.iterate(LevenbergMarquadt.FIRST_ITERATION);
		} catch (Exception e) {
			returnVal = e.toString();
		}
		chiSq = lm.getChiSq();
		do {
			try {
				lm.iterate(LevenbergMarquadt.NEXT_ITERATION);
			} catch (Exception e) {
				returnVal = e.toString();
			}
			newChiSq = lm.getChiSq();
			numIter++;
			close = (Math.abs(newChiSq - chiSq) < 0.01);
			//didn't improve or improved marginally
			if (close)
				smallCounter++;
			quit = ((smallCounter >= MAX_SMALL) || (numIter >= MAX_ITERATIONS));
			chiSq = newChiSq;
		} while (!quit);
		//do last iteration
		try {
			lm.iterate(LevenbergMarquadt.LAST_ITERATION);
			returnVal =
				(numIter + " iterations, d.o.f. = " + lm.getDegreesOfFreedom());
			returnVal += ", chisq/d.o.f. = " + newChiSq;
			//System.out.println(numIter+" iterations, d.o.f. = "+lm.getDegreesOfFreedom());
		} catch (Exception e) {
			returnVal = e.toString();
		}
		chisq.setValue(newChiSq);
		return returnVal;
	}

	/**
	 * Gets the contents of <code>parameters</code>. 
	 *
	 * @return	the contents of <code>parameters</code> 
	 * @see	#parameters
	 */
	List getParameters() {
		return parameters;
	}

	/**
	 * Returns <code>double</code> value of parameter indicated by name.
	 *
	 * @param  which  the name of the parameter
	 * @return    the current value of the parameter
	 */
	public double p(String which) {
		return getParameter(which).getDoubleValue();
	}

	/**
	 * Set a parameter designated by name to a new value.
	 *
	 * @param  which  the name of the parameter
	 * @param  value  the value to assign
	 * @exception   FitException      thrown if unrecoverable error occurs
	 */
	public void setParameter(String which, double value) throws FitException {
		getParameter(which).setValue(value);
	}

	/**
	 * Allows access to parameters by name for modification.
	 *
	 * @param  which  the name of the parameter
	 * @return    the <code>Parameter</code> object
	 */
	public Parameter getParameter(String which) {
		return (Parameter) (parameterTable.get(which));
	}

	/**
	 * Calculate function value for specific channel in the histogram.
	 *
	 * @param  channel  the channel to evaluate the function at
	 * @return    the value of the function at <code>channel</code>
	 */
	public double calculate(int channel) {
		double temp = 0.0;
		if (channel >= minCH && channel <= maxCH) {
			temp = valueAt((double) channel);
		}
		return temp;
	}
}
