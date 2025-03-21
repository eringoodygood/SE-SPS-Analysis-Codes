/*******************************************************************************
 * Nuclear Simulation Java Class Libraries Copyright (C) 2003 Yale University
 * 
 * Original Developer Dale Visser (dale@visser.name)
 * 
 * OSI Certified Open Source Software
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the University of Illinois/NCSA Open Source License.
 * 
 * This program is distributed in the hope that it will be useful, but without
 * any warranty; without even the implied warranty of merchantability or fitness
 * for a particular purpose. See the University of Illinois/NCSA Open Source
 * License for more details.
 * 
 * You should have received a copy of the University of Illinois/NCSA Open
 * Source License along with this program; if not, see
 * http://www.opensource.org/
 ******************************************************************************/
package net.sourceforge.nukesim.math;

import jade.physics.Quantity;
import jade.physics.Scalar;
import jade.realtime.PoolContext;

public class RungeKutta4 {

	private Quantity[] y;

	private Quantity x, h;

	private final Differentiable derivs;

	public RungeKutta4(Differentiable de) {
		derivs = de;
	}

	/**
	 * See numerical recipes Section 16.1
	 */
private void setVariables(Quantity evaluateAt, Quantity[] initialValues,
			Quantity interval) /* throws Exception */{
		if (!evaluateAt.getSystemUnit().isCompatible(
				interval.getSystemUnit())) {
			throw new IllegalArgumentException(
					"Step must be in same units as x-axis.");
		} else if (initialValues.length<1){
			throw new IllegalArgumentException(
					"Need at least one inital value.");
		}
		y = initialValues;
		x = evaluateAt;
		h = interval;
		final Quantity [] dydx=derivs.dydx(x,y);
		for (int i=0; i < y.length; i++){
			if (!y[i].getSystemUnit().isCompatible(
				dydx[i].multiply(h).getSystemUnit())){
			throw new IllegalStateException("Incompatible units. [dydx] = "+
					dydx[i].getSystemUnit()+", [y] = "+y[i].getSystemUnit()+
					", [x] = "+x.getSystemUnit());
			}
		}
	}
	/**
	 * Almost verbatim routine rk4 in Numerical Recipes.
	 */
	private Quantity[] step() {
		final Quantity hh = h.multiply(0.5);
		final Quantity h6 = h.divide(6.0);
		final Quantity xh = x.add(hh);
		final Quantity[] yt = new Quantity[y.length];
		final Quantity[] yout = new Quantity[y.length];
		PoolContext.enter();
		try {
			final Quantity [] dydx = derivs.dydx(x, y);
			for (int i = 0; i < y.length; i++) {
				yt[i] = y[i].add(hh.multiply(dydx[i]));
			}
			Quantity[] dyt = derivs.dydx(xh, yt);
			for (int i = 0; i < y.length; i++) {
				yt[i] = y[i].add(hh.multiply(dyt[i]));
			}
			final Quantity[] dym = derivs.dydx(xh, yt);
			for (int i = 0; i < y.length; i++) {
				yt[i] = y[i].add(h.multiply(dym[i]));
				dym[i] = dym[i].add(dyt[i]);
			}
			dyt = derivs.dydx(x.add(h), yt);
			for (int i = 0; i < y.length; i++) {
				final Quantity paren = dydx[i].add(dyt[i]).add(
						dym[i].multiply(2));
				yout[i] = (Quantity) y[i].add(h6.multiply(paren)).export();
			}
		} finally {
			PoolContext.exit();
		}
		return yout;
	}

	public Quantity[] integrate(final Quantity startX, final Quantity endX,
			final Quantity[] initY, final Quantity interval) {
		setVariables(startX, initY, interval);
		if (endX.subtract(startX).divide(h).compareTo(Scalar.ZERO) <= 0 ){
			throw new IllegalArgumentException("Step interval "+h.toString()+
					" is zero or the wrong sign to get from "+startX.toString()+
					" to "+endX.toString());
		}
		PoolContext.enter();
		try {
			while (wholeStepsLeft(endX)) {
				y = step();
				x = x.add(h);
			}
			h=endX.subtract(x);
			y=step();
			for (int i = y.length-1; i >= 0; i--) {
				y[i] = (Quantity) y[i].export();
			}
		} finally {
			PoolContext.exit();
		}
		return y;
	}

	private final boolean wholeStepsLeft(Quantity endX){
		return x.add(h).subtract(endX).divide(h).compareTo(Scalar.ZERO) < 0;
	}
}