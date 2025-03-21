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
 * Boost.java
 *
 * Created on April 4, 2001, 9:51 AM
 */

package net.sourceforge.nukesim.nuclear;

import jade.JADE;
import jade.math.Matrix;
import jade.math.Vector;
import jade.physics.Angle;
import jade.physics.Energy;
import jade.physics.Mass;
import jade.physics.Quantity;
import jade.physics.Scalar;
import jade.physics.Velocity;
import jade.physics.models.RelativisticModel;
import net.sourceforge.nukesim.math.MathException;
import net.sourceforge.nukesim.monte.Direction;

/**
 * This class handles general boosts from one lorentz frame to another.
 * Instances are created by specifying the relative velocity of the frame to be
 * boosted to.
 * 
 * @author Dale Visser
 */
public final class Boost implements NukeUnits {

	private Scalar gamma;

	/**
	 * zeroth element has beta, 1st-3rd have x,y,z components
	 */
	private Scalar[] beta = new Scalar[4];

	private Matrix boost;

	/**
	 * Creates new Boost
	 * 
	 * @param _beta
	 *            velocity of frame (in units of c)
	 * @param theta
	 *            angle from z axis (in radians)
	 * @param phi
	 *            azimuthal angle (in radians)
	 */
	public Boost(Scalar _beta, Angle theta, Angle phi) {
		beta[0] = _beta;
		gamma = calculateGamma(beta[0]);
		final Scalar stheta = theta.sine();
		//beta[1]=beta[0]*Math.sin(theta)*Math.cos(phi);
		beta[1] = Scalar.scalarOf(stheta.multiply(phi.cos()).multiply(beta[0]));
		//beta[2]=beta[0]*Math.sin(theta)*Math.sin(phi);
		beta[2] = Scalar
				.scalarOf(stheta.multiply(phi.sine()).multiply(beta[0]));
		//beta[3]=beta[0]*Math.cos(theta);
		beta[3] = Scalar.scalarOf(theta.cos().multiply(beta[0]));
		makeBoostMatrix();
	}

	/**
	 * Creates a boost given the beta "4-vector" that a boost creates. This
	 * "4-vector" has beta as its 0th element, and the x, y, and z components as
	 * the 1, 2, and 3 elements, respectively.
	 * 
	 * @param _beta
	 *            the array described above
	 */
	public Boost(Scalar[] _beta) {
		System.arraycopy(_beta, 0, beta, 0, 4);
		gamma = calculateGamma(beta[0]);
		makeBoostMatrix();
	}

	public Boost(Scalar _beta, Direction d) {
		this(_beta, d.getTheta(), d.getPhi());
	}

	Scalar[] getBeta() {
		return beta;
	}

	Angle getTheta() {
		//return Math.acos(beta[3]/beta[0]);
		return Scalar.scalarOf(beta[3].divide(beta[0])).acos();
	}

	Angle getPhi() {
		//return Math.acos(beta[1]/(beta[0]*Math.sin(getTheta())));
		return Scalar.scalarOf(
				beta[1].divide(beta[0].multiply(getTheta().sine()))).acos();
	}

	/**
	 * Creates the boost for a velocity equal in magnitude in the opposite
	 * direction from the original boost. This is useful when you want to go
	 * back and forth between 2 frames.
	 * 
	 * @param boost
	 *            the boost to be inverted
	 * @return boost for the inverse transformation
	 */
	static public Boost inverseBoost(Boost boost) {
		Scalar[] temp = new Scalar[4];
		Scalar[] orig = boost.getBeta();
		temp[0] = orig[0];
		temp[1] = Scalar.scalarOf(orig[1].negate());
		temp[2] = Scalar.scalarOf(orig[2].negate());
		temp[3] = Scalar.scalarOf(orig[3].negate());
		return new Boost(temp);
	}

	public Boost getInverse() {
		return Boost.inverseBoost(this);
	}

	/**
	 * Given a velocity (_beta) in units of c, calculate gamma.
	 * 
	 * <PRE>
	 * 
	 * gamma=[sqrt(1-_beta^2)]^(-1)
	 * 
	 * </PRE>
	 * 
	 * @param _beta
	 *            velocity over c, the speed of light
	 * @return the standard gamma parameter from special relativity
	 */
	public Scalar calculateGamma(Scalar _beta) {
		//return 1/Math.sqrt(1-_beta*_beta);
		return Scalar.scalarOf(Scalar.ONE.subtract((Quantity) _beta.pow(2))
				.root(2).inverse());
	}

	/**
	 * Given a proper 4-vector, boost it to the frame indicated at the creation
	 * of this object.
	 * 
	 * @param fourVector
	 *            proper special relativistic 4-vector
	 * @throws MathException
	 *             if there's a computation problem
	 * @return 4-vector in the new frame
	 * @see Boost
	 */
	public Vector transformVector(Quantity[] fourVector) throws MathException {
		final Vector init = Vector.valueOf(fourVector);
		final Matrix result = boost.multiply(init);//matrix product
		return getVector(result);
	}

	private Vector getVector(Matrix m) {
		if (m.getColumnDimension() != 1) {
			throw new IllegalArgumentException("Need single column matrix.");
		}
		final int len = m.getRowDimension();
		final Vector rval = Vector.newInstance(len);
		for (int i = 0; i < len; i++) {
			rval.set(i, m.get(i, 0));
		}
		return rval;
	}

	static public double[] make4Momentum(double KE, double mass, double theta,
			double phi) {
		double[] rval = new double[4];
		rval[0] = KE + mass;
		double p = Math.sqrt(KE * (2 * mass + KE));
		rval[1] = p * Math.sin(theta) * Math.cos(phi);
		rval[2] = p * Math.sin(theta) * Math.sin(phi);
		rval[3] = p * Math.cos(theta);
		return rval;
	}

	private Quantity[] makeArray(Vector in) throws MathException {
		if (in.getRowDimension() != 4) {
			throw new MathException("Not a 4-vector");
		}
		final Quantity[] rval = new Quantity[4];
		for (int i = 0; i < 4; i++) {
			rval[i] = (Quantity) in.get(i);
		}
		return rval;
	}

	/**
	 * Makes boost matrix from p. 541 of Jackson v.2.
	 */
	private void makeBoostMatrix() {
		boost = Matrix.newInstance(4, 4);
		boost.set(0, 0, gamma);
		for (int i = 1; i <= 3; i++) {
			final Quantity edges = gamma.negate().multiply(beta[i]);
			boost.set(0, i, edges);
			boost.set(i, 0, edges);
			for (int j = 1; j <= 3; j++) {
				boost.set(i, j, gamma.subtract(Scalar.ONE).multiply(beta[i])
						.multiply(beta[j]).divide(((Quantity) beta[0].pow(2))));
				if (i == j) {
					boost.set(i, j, boost.get(i, j).plus(Scalar.ONE));
				}
			}
		}
	}

	public Velocity getFrameVelocity() {
		return Velocity.velocityOf(Velocity.SPEED_OF_LIGHT.multiply(beta[0]));
	}

	public String toString() {
		String rval = "Frame to boost to: " + beta[0].toText(light_speed)
				+ ", Beta: ";
		rval += "x = " + beta[1] + ", y = " + beta[2] + ", z = " + beta[3]
				+ "\n";
		return rval;
	}

	/**
	 * Test code.
	 * 
	 * @param args
	 *            ignored
	 */
	public static void main(String[] args) {
		JADE.initialize();
		RelativisticModel.select();
		final Energy kinetic = Energy.energyOf(Quantity.valueOf(5.0, MeV));
		Mass m0 = Mass.massOf(Quantity.valueOf(3727.3802, MeV));
		Angle theta = Angle.angleOf(Quantity.valueOf(140.0, deg));
		System.out.println(kinetic.toText(MeV) + " alpha in CM");
		double[] phiD = { -90.0, -18.0, 54.0, 126.0, 198.0 };
		Angle[] phi = new Angle[phiD.length];
		for (int i = 0; i < phiD.length; i++) {
			phi[i] = Angle.angleOf(Quantity.valueOf(phiD[i], deg));
		}
		Quantity[] p = new Quantity[4];
		Boost b = Boost.inverseBoost(new Boost(Scalar.valueOf(0.001, 0.000001),
				Angle.angleOf(Quantity.valueOf(5, deg)), Angle.ZERO));
		System.out.println(b);
		p[0] = kinetic.add(m0);
		Quantity p3 = ((Quantity) kinetic.pow(2)).add(
				m0.multiply(2).multiply(kinetic)).root(2);
		for (int i = 0; i < phi.length; i++) {
			p[1] = p3.multiply(theta.sine()).multiply(phi[i].cos());
			p[2] = p3.multiply(theta.sine()).multiply(phi[i].sine());
			p[3] = p3.multiply(theta.cos());
			try {
				Vector pb = b.transformVector(p);
				Quantity pb0 = (Quantity) pb.get(0);
				System.out.println("Detector at " + phi[i].toText(deg) + ":\n"
						+ "\tT[alpha][lab] = " + pb0.subtract(m0).toText(MeV)
						+ '.');
			} catch (MathException e) {
				System.err.println(e);
			}
		}
	}
}