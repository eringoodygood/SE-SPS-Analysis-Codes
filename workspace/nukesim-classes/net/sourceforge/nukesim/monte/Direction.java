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
 * Direction.java
 *
 * Created on March 7, 2001, 11:47 AM
 */
package net.sourceforge.nukesim.monte;

import jade.JADE;
import jade.math.Matrix;
import jade.math.Vector;
import jade.physics.Angle;
import jade.physics.Quantity;
import jade.physics.Scalar;
import jade.physics.models.RelativisticModel;
import net.sourceforge.nukesim.nuclear.NukeUnits;

/**
 * Class which provides an abstraction for a direction in 3-dimensional space.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser </a>
 */
public class Direction extends Object implements Cloneable, NukeUnits {

	private Angle theta, phi;//in radians

	private Scalar x, y, z;//direction components

	private static RandomNumbers random;

	private static final Angle ANGLE_PI;

	private static final Angle ANGLE_2PI;

	static {
		JADE.initialize();
		RelativisticModel.select();
		ANGLE_PI = Angle.angleOf(Quantity.valueOf(180, deg));
		ANGLE_2PI = Angle.angleOf(Quantity.valueOf(360, deg));
	}

	/**
	 * Creates new direction, given theta (angle from z-axis) and phi (azimuthal
	 * angle from x axis) in radians.
	 */
	public Direction(Angle theta, Angle phi) {
		z = theta.cos();
		final Scalar sine = theta.sine();
		x = Scalar.scalarOf(sine.multiply(phi.cos()));
		y = Scalar.scalarOf(sine.multiply(phi.sine()));
		setAngles();
		if (random == null) {
			initRandom();
		}
	}

	/**
	 * Create a new direction by specifying its x, y, and z components. These
	 * will be renormailized.
	 */
	public Direction(Quantity _x, Quantity _y, Quantity _z) {
		if (!_x.getSystemUnit().isCompatible(_y.getSystemUnit())
				|| !_x.getSystemUnit().isCompatible(_z.getSystemUnit())) {
			throw new IllegalArgumentException(
					"All three quantities must possess compatible dimensions.");
		}
		final Quantity norm = ((Quantity)_x.pow(2).plus(_y.pow(2)).plus(_z.pow(2))).root(2);
		this.x = Scalar.scalarOf(_x.divide(norm));
		this.y = Scalar.scalarOf(_y.divide(norm));
		this.z = Scalar.scalarOf(_z.divide(norm));
		setAngles();
		if (random == null) {
			initRandom();
		}
	}

	/**
	 * called by constructors, assuming x,y, and z have been set with a norm of
	 * 1. i.e. x^2+y^2+z^2=1
	 */
	private void setAngles() {
		phi = normPhi(Angle.atan2(y, x));
		theta = z.acos();
	}

	private Direction copy() {
		return new Direction(theta, phi);
	}

	//initializes random number generator
	private void initRandom() {
		try {
			random = new RandomNumbers();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	/**
	 * return a new Direction object resulting from this object being rotated by
	 * angRad about the y-axis
	 */
	public Direction rotateY(Angle angRad) {
		final Direction rval;
		if (angRad.approxEquals(Angle.ZERO)) {
			rval = copy();
		} else {
			final Scalar c = angRad.cos();
			final Scalar s = angRad.sine();
			final Matrix rotate = Matrix.newInstance(3, 3);
			rotate.set(0, 0, c);
			rotate.set(0, 1, Scalar.ZERO);
			rotate.set(0, 2, s);
			rotate.set(1, 0, Scalar.ZERO);
			rotate.set(1, 1, Scalar.ONE);
			rotate.set(1, 2, Scalar.ZERO);
			rotate.set(2, 0, s.negate());
			rotate.set(2, 1, Scalar.ZERO);
			rotate.set(2, 2, c);
			rval = getDirection(rotate.multiply(getVector()));
		}
		return rval;
	}

	/**
	 * Return a new Direction object resulting from this object's reference
	 * frame being rotated by angRad about the y-axis.
	 */
	public Direction rotateFrameY(Angle angRad) {
		final Direction rval;
		if (angRad.approxEquals(Angle.ZERO)) {
			rval = copy();
		} else {
			Scalar c = angRad.cos();
			Scalar s = angRad.sine();
			rval = new Direction(c.multiply(x).subtract(s.multiply(z)), y, s
					.multiply(x).add(c.multiply(z)));
		}
		return rval;
	}

	/**
	 * Return a new Direction object resulting from this object's reference
	 * frame being rotated by angRad about the z-axis.
	 */
	public Direction rotateFrameZ(Angle angRad) {
		Scalar c = angRad.cos();
		Scalar s = angRad.sine();
		return new Direction(c.multiply(x).add(s.multiply(y)), s.negate()
				.multiply(x).add(c.multiply(y)), z);
	}

	static public Direction getDirection(Matrix m) {
		if (m.getRowDimension() == 3 && m.getColumnDimension() == 1) {
			return new Direction((Quantity) m.get(0, 0),
					(Quantity) m.get(1, 0), (Quantity) m.get(2, 0));
		} else {
			throw new IllegalArgumentException(
					"Expected a single-column, 3 element matrix.");
		}
	}

	public Quantity[] get3vector(Quantity amplitude) {
		final Quantity[] rval = new Scalar[3];
		rval[0] = x.multiply(amplitude);
		rval[1] = y.multiply(amplitude);
		rval[2] = z.multiply(amplitude);
		return rval;
	}

	/**
	 * @return phi in radians, guaranteed between -pi and pi
	 */
	public Angle getPhi() {
		return phi;
	}

	public Angle getTheta() {
		return theta;
	}

	public Scalar getX() {
		return x;
	}

	public Scalar getY() {
		return y;
	}

	public Scalar getZ() {
		return z;
	}

	public Vector getVector() {
		final Vector rval = Vector.newInstance(3);
		rval.set(0, x);
		rval.set(1, y);
		rval.set(2, z);
		return rval;
	}

	static public Direction getBackwardRandomDirection() {
		Direction rval = null;
		try {
			final Scalar randNum = Scalar.scalarOf(Scalar.ONE.subtract(Scalar
					.valueOf(random.next())));
			rval = new Direction(Angle.angleOf(ANGLE_PI
					.subtract(randNum.acos())), Angle.angleOf(ANGLE_2PI
					.multiply(random.next())));
		} catch (Exception e) {
			System.err.println(e);
		}
		return rval;
	}

	public String toString() {
		StringBuffer rval = new StringBuffer("Direction: theta = ");
		rval.append(theta.toText(deg)).append(", ").append("phi = ");
		rval.append(phi.toText(deg)).append(", \nx\ty\tz\n");
		rval.append(x).append('\t').append(y).append('\t').append(z);
		rval.append('\n');
		return rval.toString();
	}

	static public Angle normPhi(Angle _phi) {
		Angle rval = _phi.bounded();//-pi to pi
		if (rval.doubleValue() < 0) {
			rval = Angle.angleOf(rval.add(ANGLE_PI));
		}
		return rval;
	}

	/**
	 * Taken from plgndr, section 6.8 in Numerical Recipes in C. Decays with
	 * angular momentum 'l' are distributed as the square of
	 * evaluateLegengre(l,0,cos theta). (Modulated of course by the sin(theta)
	 * factor of the phase space available.)
	 * 
	 * @param _l
	 *            orbital angular momentum quantum number, 0 or positive
	 * @param _m
	 *            substate, can be from 0 to l
	 * @param _x
	 *            where to evaluate, from -1 to 1
	 */
	static public double evaluateLegendre(int _l, int _m, double _x)
			throws IllegalArgumentException {
		double fact, pll, pmm, pmmp1, somx2;

		if (_m < 0 || _m > _l || Math.abs(_x) > 1.0)
			throw new IllegalArgumentException(
					"Invalid argument for Legendre evaluation: l=" + _l
							+ ", m=" + _m + ", x=" + _x);
		pmm = 1.0;//compute Pmm
		if (_m > 0) {
			somx2 = Math.sqrt((1 - _x) * (1 + _x));
			fact = 1.0;
			for (int i = 0; i < _m; i++) {
				pmm *= -fact * somx2;
				fact += 2;
			}
		}
		if (_l == _m) {
			return pmm;
		} else { //compute Pm,m+1
			pmmp1 = _x * (2 * _m + 1) * pmm;
			if (_l == (_m + 1)) {
				return pmmp1;
			} else { //Compute Pl,m where l>m+1
				pll = 0.0;
				for (int ll = _m + 2; ll <= _l; ll++) {
					pll = (_x * (2 * ll - 1) * pmmp1 - (ll + _m - 1) * pmm)
							/ (ll - _m);
					pmm = pmmp1;
					pmmp1 = pll;
				}
				return pll;
			}
		}
	}

	/**
	 * Generate a random direction using a Spherical Harmonic distribution
	 * (attenuated by a sin theta solid angle factor).
	 */
	static public Direction getRandomDirection(int l, int m) {
		double _x, test, leg;
		do {
			_x = 1 - 2 * random.next();//-1..1, x=cos(theta)
			test = random.next();//0..1
			leg = evaluateLegendre(l, m, _x);
		} while (test > (leg * leg));
		return new Direction(Scalar.valueOf(_x).acos(), Angle.angleOf(ANGLE_2PI
				.multiply(random.next())));
	}

	/**
	 * Generate a random direction for m=0 using a Legendre polynomial
	 * distribution (attenuated by a sin theta solid angle factor).
	 */
	static public Direction getRandomDirection(int l) {
		return getRandomDirection(l, 0);
	}

	static public Direction getRandomDirection() {
		Direction rval = null;
		try {
			final Scalar cosTheta = Scalar.valueOf(.0 - 2.0 * random.next());
			rval = new Direction(cosTheta.acos(), randomPhi());
		} catch (Exception e) {
			System.err.println(e);
		}
		return rval;
	}

	static public Angle randomPhi() {
		return Angle.angleOf(Quantity.valueOf(360 * random.next(), deg));
	}

	/**
	 * get Random direction between given theta limits
	 */
	static public Direction getRandomDirection(Angle minThetaRad,
			Angle maxThetaRad) {
		Direction rval = null;
		final Quantity maxRandom = Scalar.ONE.subtract(maxThetaRad.cos())
				.multiply(0.5);
		final Quantity minRandom = Scalar.ONE.subtract(minThetaRad.cos())
				.multiply(0.5);
		final Quantity delimitedRandom = minRandom.add(maxRandom.subtract(
				minRandom).multiply(random.next()));
		try {
			final Scalar cosTheta = Scalar.scalarOf(Scalar.ONE
					.subtract(delimitedRandom.multiply(2)));
			final Angle thetaLocal = cosTheta.acos();
			final Angle phiLocal = Angle.angleOf(ANGLE_2PI.multiply(random
					.next()));
			rval = new Direction(thetaLocal, phiLocal);
		} catch (Exception e) {
			System.err.println(e);
		}
		return rval;
	}

	static final Direction Z_AXIS = new Direction(Scalar.ZERO, Scalar.ZERO,
			Scalar.ONE);

	/**
	 * Generate a random direction using a Legendre polynomial distribution
	 * (attenuated by a sin theta solid angle factor), relative to a z-axis
	 * defined by the given direction.
	 */
	static public Direction getRandomDirection(int l, Direction d) {
		Direction rval = getRandomDirection(l);
		rval = rval.rotateFrameZ(d.getTheta()).rotateFrameY(d.getPhi());
		return rval;
	}

	public static void main(String[] args) {
		JADE.initialize();
		for (int i = 0; i < 1; i++) {
			Direction r = Direction.getRandomDirection(Angle.ZERO, Angle
					.angleOf(Quantity.valueOf(5, deg)));
			System.out.println("init: " + r);
			final Angle angleY = Angle.angleOf(Quantity.valueOf(30, deg));
			System.out.println("final: " + r.rotateY(angleY));
		}
	}
}