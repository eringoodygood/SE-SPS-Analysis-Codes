/*
 * Created on Jul 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.nukesim.math;

import jade.physics.Quantity;
import jade.realtime.PoolContext;
import jade.units.Unit;

/**
 * @author Administrator
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class QuantityUtilities {

	public static String reportQuantity(Quantity value, Unit unit,
			boolean showUnit) {
		final double dval = value.doubleValue(unit);
		final double derr = dval * value.getRelativeError();
		final UncertainNumber unc = new UncertainNumber(dval, derr);
		final StringBuffer rval = new StringBuffer(unc.toString());
		if (showUnit) {
			rval.append(' ');
			rval.append(unit.toString());
		}
		return rval.toString();
	}

	public static String noUnits(Quantity value, Unit unit) {
		final StringBuffer rval = new StringBuffer();
		if (unit != null) {
			final int unitLength = unit.toString().length();
			if (value.isPossiblyZero()) {
				rval.append(0);
			} else {
				rval.append(value.toText(unit));
				rval.delete(rval.length() - unitLength, rval.length());
			}
		} else {
			rval.append(value.toString());
		}
		return Double.toString(Double.parseDouble(rval.toString()));
	}

	public static double errorValue(Quantity value, Unit unit) {
		return value.doubleValue(unit) * value.getRelativeError();
	}

	public static Quantity quantity(UncertainNumber value, Unit unit) {
		return Quantity.valueOf(value.value, value.error, unit);
	}

	public static Quantity scaleError(Quantity value, double factor) {
		return Quantity.valueOf(value.doubleValue(), value.getAbsoluteError()
				* factor, value.getSystemUnit());
	}

	public static Quantity fakeExact(Quantity value) {
		return Quantity.valueOf(value.doubleValue(), value.getSystemUnit());
	}

	public static String plusMinusQuantity(Quantity value, Unit unit,
			boolean showUnit) {
		final double dval = value.doubleValue(unit);
		final double derr = errorValue(value, unit);
		final UncertainNumber unc = new UncertainNumber(dval, derr);
		final StringBuffer rval = new StringBuffer(unc.toPlusMinusString());
		if (showUnit) {
			rval.append(' ');
			rval.append(unit.toString());
		}
		return rval.toString();
	}

	public static boolean isNonNegative(Quantity x){
		boolean rval;
		PoolContext.enter();
		try {
			rval = x.compareTo(x.multiply(0))>=0 || x.isPossiblyZero();
		} finally {
			PoolContext.exit();
		}
		return rval;
	}

	public static boolean isPositive(Quantity x){
		boolean rval;
		PoolContext.enter();
		try {
			rval = x.compareTo(x.multiply(0))>0;
		} finally {
			PoolContext.exit();
		}
		return rval;
	}

	public static Comparable max(Comparable a, Comparable b) {
		return a.compareTo(b) >= 0 ? a : b;
	}

	public static Comparable min(Comparable a, Comparable b) {
		return a.compareTo(b) < 0 ? a : b;
	}

	public static Quantity sign(Quantity a, Quantity b) {
		Quantity rval;
		PoolContext.enter();
		try {
			final Quantity absA = a.abs();
			rval = b.compareTo(b.multiply(0)) >= 0 ? (Quantity) absA.export()
					: (Quantity) absA.negate().export();
		} finally {
			PoolContext.exit();
		}
		return rval;
	}
}