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
package cmdeibel.nuclear;
import dwvisser.math.*;
import java.util.Vector;
import java.util.Iterator;

/**
 * Given experimental parameters beam energy, beam, target, projectile, can give
 * useful quantities via it's getter methods. Core of code is from Kazim Yildiz's
 * relkin code written Fortran.
 *
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public class Reaction {

	/**
	 * speed of light, in m/s
	 */
	public static final double C = 299792458;

	/**
	 * Multiply p in MeV/c by this and get qbrho in kG cm
	 */
	public static final double P_TO_QBRHO = 1.0e9 / C;

	/**
	 * Multiply qbrho in kG cm by this and get p in Mev/c
	 */
	public static final double QBRHO_TO_P = C * 1.0e-9;

	/**
	 * target(beam,projectile)residual
	 */
	protected Nucleus target, beam, projectile, residual;

	//double Tbeam;
	double thetaLab;
	UncertainNumber residualExcitation;
	UncertainNumber Q;

	//boolean calculated = false; //Set to true if a good kinematics calculation
	//has been done.

	double xa; // lab angle, particle 3, in degrees
	double tmax; //The maximum kinematically allowed angle in the lab.
	double t3l, xx, aa, bb, cc;
	double[] t3c; //COM angle(s) of 3
	double m1, m2, m3, m4; //nuclei masses
	double eb; //beam kinetic energy
	double w2l, p2l, g2l, b2l;
	//total energy, momentum, gamma, beta of 2 in lab
	double w1l, wtl; //total energy of 1 in lab, total lab energy
	double w3c, p3c, g3c, b3c, k3c; //total energy,momentum,gamma,beta, ? of 3
	//in COM frame
	double[] w3l, e3l; //total, kinetic energies of Particle 3 in lab
	double w4c, p4c, g4c, b4c, k4c;
	double[] t4c, t4l; //COM, lab angles of particle 4
	double[] w4l, e4l; //total, kinetic energies of Particle 4 in lab
	double b, g, wtc; //cms beta, gamma, total cms energy
	double[] jac3; //Jacobian for particle 3
	double[] kp; //1/p dp/dtheta,lab for particle 3, used for Enge focusing
	double[] p3l, qbrho; //momentum & rigidity, used for magnetic spectrometers
	double[] xb; //COM angle, particle 3, in degrees
	double[] xc; //lab angle, particle 4, in degrees
	double ex2, ex3, ex4; //excitations of the target, projectile, and residual

	boolean overMax = false;

	/** Constructor.
	 * @param thetaLab of projectile in degrees
	 * @param target nuclear species in target
	 * @param beam nuclear species of beam
	 * @param projectile nuclear species of projectile into spectrometer
	 * @param Tbeam beam energy in MeV
	 * @param residualExcitation excitation in MeV of the residual nucleus
	 * @throws KinematicsException if a cclculation error occurs
	 */
	public Reaction(
		Nucleus target,
		Nucleus beam,
		Nucleus projectile,
		double Tbeam,
		double thetaLab,
		UncertainNumber residualExcitation)
		throws NuclearException, KinematicsException {
		this.target = target;
		this.beam = beam;
		this.projectile = projectile;
		this.residualExcitation = residualExcitation;
		this.residual =
			new Nucleus(
				target.Z + beam.Z - projectile.Z,
				target.A + beam.A - projectile.A,
				residualExcitation);
		this.eb = Tbeam;
		this.thetaLab = thetaLab;
		setQ();
		setKinematicQuantities();
		setThetaCOM();
		COMtoLab();
	}

	public Reaction(
		Nucleus target,
		Nucleus beam,
		Nucleus projectile,
		double Tbeam,
		double thetaLab,
		double residualExcitation)
		throws NuclearException, KinematicsException {
		this(
			target,
			beam,
			projectile,
			Tbeam,
			thetaLab,
			new UncertainNumber(residualExcitation));
	}

	/** Simplified constructor for no excitation in the residual, forward direction
	 * for the projectile.
	 * @param target nuclear species of the target
	 * @param beam nuclear species of the beam
	 * @param projectile neuclear species of the projectile into the spectrometer
	 * @throws KinematicsException if a calculation problem occurs
	 */
	/*public Reaction(Nucleus target, Nucleus beam, Nucleus projectile)
		throws NuclearException {
		this(target, beam, projectile, 0.0, 0.0, new UncertainNumber(0.0));
	}*/

	/** Constructor to make some changes to the parameters of a  previous instance.
	 * @param r previous <CODE>Reaction</CODE> to copy species info from
	 * @param eBeam new beam energy in MeV
	 * @param thetaLab new lab angle in degrees
	 * @param residExcite new excitation in MeV for the residual nucleus
	 * @throws KinematicsException if a calculation problem occurs
	 */
	public Reaction(
		Reaction r,
		double eBeam,
		double thetaLab,
		double residExcite)
		throws NuclearException, KinematicsException {
		this(
			r.getTarget(),
			r.getBeam(),
			r.getProjectile(),
			eBeam,
			thetaLab,
			new UncertainNumber(residExcite));
	}

	public Reaction(Reaction r, double residExcite)
		throws NuclearException, KinematicsException {
		this(
			r.getTarget(),
			r.getBeam(),
			r.getProjectile(),
			r.getBeamEnergy(),
			r.getThetaLab(),
			new UncertainNumber(residExcite));
	}

	public double getThetaLab() {
		return thetaLab;
	}

	/**
	 *options (can be or'ed together) for requesting QBrho
	 */
	static public final int EXACT_OPTION = 0;
	static public final int UNCERTAIN_BEAM_MASS_OPTION = 1;
	static public final int UNCERTAIN_TARGET_MASS_OPTION = 2;
	static public final int UNCERTAIN_PROJECTILE_MASS_OPTION = 4;
	static public final int UNCERTAIN_RESIDUAL_MASS_OPTION = 8;
	int calculateOption = Reaction.EXACT_OPTION;
	public UncertainNumber getQBrho(int which, int options)
		throws KinematicsException {
		double value = getQBrho(which);
		Vector deviations = new Vector(4);
		if ((options & Reaction.UNCERTAIN_BEAM_MASS_OPTION) > 0) {
			calculateOption = Reaction.UNCERTAIN_BEAM_MASS_OPTION;
			recalculate();
			deviations.add(new Double(getQBrho(which) - value));
		}
		if ((options & Reaction.UNCERTAIN_TARGET_MASS_OPTION) > 0) {
			calculateOption = Reaction.UNCERTAIN_TARGET_MASS_OPTION;
			recalculate();
			deviations.add(new Double(getQBrho(which) - value));
		}
		if ((options & Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION) > 0) {
			calculateOption = Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION;
			recalculate();
			deviations.add(new Double(getQBrho(which) - value));
		}
		if ((options & Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION) > 0) {
			calculateOption = Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION;
			recalculate();
			deviations.add(new Double(getQBrho(which) - value));
		}
		calculateOption = Reaction.EXACT_OPTION;
		recalculate();
		double error = 0;
		Iterator iter = deviations.iterator();
		while (iter.hasNext()) {
			double temp = ((Double) iter.next()).doubleValue();
			error += temp * temp;
		}
		error = Math.sqrt(error);
		return new UncertainNumber(value, error);
	}
	private void recalculate() throws KinematicsException {
		setKinematicQuantities();
		setThetaCOM();
		//if (calculated) {
		COMtoLab();
		//}
	}

	/** Using the given momentum of the projectile in the lab,
	 * calculates the necessary excitation of the residual nucleus.
	 * This is useful for interpreting spectrometer measurements.
	 * @param labMomentum3 the lab frame momentum of the projectile, in MeV/c
	 * @return excitation energy of residual nucleus
	 */
	public UncertainNumber getEx4(UncertainNumber labMomentum3) {
		//Nucleus groundState;
		double devUpper, devLower, bestEstimate, estimatedError;
		double p4lSq, p3l, w4l, w3l;
		//local p3l,w3l,w4l are double, not double []
		double p3lUpper, p3lLower;

		p3l = labMomentum3.value;
		p3lUpper = labMomentum3.value + labMomentum3.error;
		p3lLower = labMomentum3.value - labMomentum3.error;
		p4lSq = p2l * p2l - 2 * p2l * p3l * Math.cos(t3l) + p3l * p3l;
		w3l = Math.sqrt(p3l * p3l + m3 * m3);
		w4l = wtl - w3l;
		//groundState=new Nucleus(residual.Z,residual.A); //implicitly g.s.
		double mresid_gs = residual.getGroundStateMass().value;
		bestEstimate = Math.sqrt(w4l * w4l - p4lSq) - mresid_gs;
		p3l = p3lUpper;
		p4lSq = p2l * p2l - 2 * p2l * p3l * Math.cos(t3l) + p3l * p3l;
		w3l = Math.sqrt(p3l * p3l + m3 * m3);
		w4l = wtl - w3l;
		//groundState=new Nucleus(residual.Z,residual.A); //implicitly g.s.
		devUpper = Math.sqrt(w4l * w4l - p4lSq) - mresid_gs;
		devUpper = Math.abs(devUpper - bestEstimate);
		p3l = p3lLower;
		p4lSq = p2l * p2l - 2 * p2l * p3l * Math.cos(t3l) + p3l * p3l;
		w3l = Math.sqrt(p3l * p3l + m3 * m3);
		w4l = wtl - w3l;
		//groundState=new Nucleus(residual.Z,residual.A); //implicitly g.s.
		devLower = Math.sqrt(w4l * w4l - p4lSq) - mresid_gs;
		devLower = Math.abs(devLower - bestEstimate);
		estimatedError = (devUpper + devLower) * 0.5;
		return new UncertainNumber(bestEstimate, estimatedError);
	}

	/** Gives some information on the state of this object.
	 */
	public void printStatus() {
		System.out.println("Q-value: " + Q + " MeV");
		for (int i = 0; i < t3c.length; i++) {
			System.out.println("Theta 3, cms[" + i + "] = " + xb[i]);
			System.out.println("Theta 4, lab[" + i + "] = " + xc[i]);
			System.out.println("QBrho[" + i + "] = " + qbrho[i]);
			System.out.println(
				"Momentum of 3, lab[" + i + "] = " + p3l[i] + " MeV/c");
		}
	}

	/**
	 * The workhorse.
	 */
	private void setKinematicQuantities() throws KinematicsException {
		m1 = target.getMass().value;
		m2 = beam.getMass().value;
		m3 = projectile.getMass().value;
		m4 = residual.getMass().value;
		if (calculateOption == Reaction.UNCERTAIN_BEAM_MASS_OPTION) {
			m2 += beam.getMass().error;
		} else if (calculateOption == Reaction.UNCERTAIN_TARGET_MASS_OPTION) {
			m1 += target.getMass().error;
		} else if (
			calculateOption == Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION) {
			m3 += projectile.getMass().error;
		} else if (
			calculateOption == Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION) {
			m4 += residual.getMass().error;
		}

		//eb = Tbeam;
		t3l = Math.toRadians(thetaLab);
		double ct3l = Math.cos(t3l);
		double st3l = Math.sin(t3l);

		w2l = eb + m2;
		g2l = w2l / m2;
		b2l = Math.sqrt(1.0 - 1.0 / (g2l * g2l));
		p2l = g2l * b2l * m2;

		w1l = m1;
		wtl = w1l + w2l;

		b = p2l / wtl;
		g = 1.0 / Math.sqrt(1.0 - b * b);

		wtc = wtl / g;

		if (wtc > (m3 + m4)) {
			/* Total energy greater than rest mass of products. */
			w3c = (wtc * wtc + m3 * m3 - m4 * m4) / (2 * wtc);
			p3c = Math.sqrt(w3c * w3c - m3 * m3);
			g3c = w3c / m3;
			b3c = Math.sqrt(1.0 - 1.0 / (g3c * g3c));
			k3c = b / b3c;

			w4c = (wtc * wtc + m4 * m4 - m3 * m3) / (2 * wtc);
			p4c = Math.sqrt(w4c * w4c - m4 * m4);
			g4c = w4c / m4;
			b4c = Math.sqrt(1.0 - 1.0 / (g4c * g4c));
			k4c = b / b4c;

			xx =
				(m1 - m4) * (m1 + m4)
					+ (m2 - m3) * (2.0 * m1 + m2 - m3)
					+ 2.0 * eb * (m1 - m3);
			aa = ct3l * ct3l + g * g * st3l * st3l;
			bb = g * g * st3l * st3l * 2.0 * k3c;
			cc =
				4.0
					* ct3l
					* ct3l
					* (ct3l * ct3l
						+ g
							* g
							* st3l
							* st3l
							* xx
							* (g3c / g + 1.0)
							/ ((g3c * g3c - 1.0) * (2.0 * m3 * wtl)));
		} else { //not enough energy for exit channel
			/* calculate minimum ennergy for channel */
			double def = m3 + m4 - wtc;
			double mi = m1 + m2;
			double ebcm = -Q.value;
			double t2est = (0.5 * (ebcm + 2 * mi) / m1) * ebcm;
			throw new KinematicsException(
				this
					+ ": beam energy = "
					+ eb
					+ " MeV.\nNeed "
					+ def
					+ " MeV more in CM frame for reaction."
					+ "\nMinimum beam energy \u2248 "
					+ t2est
					+ " MeV.");
		}
	}

	private void setQ() {
		if (beam.equals(projectile) || beam.equals(residual)) { //elastic
			Q = new UncertainNumber(0.0);
		} else if (beam.equals(target)) { //identical incoming
			Q =
				target.getMass().times(2.0).minus(projectile.getMass()).minus(
					residual.getMass());
		} else if (projectile.equals(residual)) { //identical outgoing
			Q =
				target.getMass().plus(beam.getMass()).minus(
					projectile.getMass().times(2.0));
		} else {//all unique
			Q =
				target.getMass().plus(beam.getMass()).minus(
					projectile.getMass()).minus(
					residual.getMass());
		}
	}

	private void setThetaCOM() throws KinematicsException {
		if (xx > 0.0) { //no thetaMax-->single valued
			t3c = new double[1];
			if (Math.cos(t3l) >= 0) {
				t3c[0] = Math.acos((-bb + Math.sqrt(cc)) / (2.0 * aa));
			} else { // t3l <0
				t3c[0] = Math.acos((-bb - Math.sqrt(cc)) / (2.0 * aa));
			}
		} else { //thetaMax exists

			xa = -g * xx * (g3c + g) / (2.0 * m3 * wtl * (g3c * g3c - 1.0));
			tmax = Math.acos(Math.sqrt(xa / (xa + 1.0)));
			if (t3l > tmax) {
				overMax = true;
			}
			if (t3l==100){
				
				throw new KinematicsException(
					"Lab angle: "
						+ thetaLab
						+ " > max angle: "
						+ Math.toDegrees(tmax));
			} else { //valid angle
				if (xx == 0.0) {
					t3c = new double[1];
					t3c[0] = Math.acos((-bb + Math.sqrt(cc)) / (2.0 * aa));
				} else { //double valued
					t3c = new double[2];
					t3c[0] = Math.acos((-bb + Math.sqrt(cc)) / (2.0 * aa));
					t3c[1] = Math.acos((-bb - Math.sqrt(cc)) / (2.0 * aa));
				}
			}
		}
	}

	private void COMtoLab() {
		double ct3c, st3c;

		t4c = new double[t3c.length];
		t4l = new double[t3c.length];
		w3l = new double[t3c.length];
		e3l = new double[t3c.length];
		w4l = new double[t3c.length];
		e4l = new double[t3c.length];
		jac3 = new double[t3c.length];
		kp = new double[t3c.length];
		xb = new double[t3c.length];
		xc = new double[t3c.length];
		p3l = new double[t3c.length];
		qbrho = new double[t3c.length];
		for (int i = 0; i < t3c.length; i++) {
			ct3c = Math.cos(t3c[i]);
			st3c = Math.sin(t3c[i]);
			t4c[i] = t3c[i] - Math.PI;
			t4l[i] = Math.atan(Math.sin(t4c[i]) / (g * Math.cos(t4c[i]) + k4c));
			if (t4c[i] * t4l[i] < 0.0)
				t4l[i] = t4l[i] - Math.PI;
			w3l[i] = w3c * g * (1.0 + b * b3c * ct3c);
			e3l[i] = w3l[i] - m3;
			w4l[i] = w4c * g * (1.0 + b * b4c * Math.cos(t4c[i]));
			e4l[i] = w4l[i] - m4;
			if (1.0 + k3c * ct3c > 0.0) {
				jac3[i] =
					Math.pow(
						st3c * st3c + g * g * Math.pow(k3c + ct3c, 2.0),
						1.5);
				jac3[i] /= g * (1.0 + k3c * ct3c);
			} else { //<=0
				jac3[i] = 0.0;
			}
			kp[i] = g * g * (ct3c + k3c) * (ct3c + k3c) + st3c * st3c;
			kp[i] /= g * (1.0 + k3c * ct3c);
			kp[i] = -w3c * g * b * b3c * st3c * kp[i];
			kp[i] = kp[i] * w3l[i] / (w3l[i] * w3l[i] - m3 * m3);
			p3l[i] = Math.sqrt(e3l[i] * (2.0 * m3 + e3l[i]));
			qbrho[i] = P_TO_QBRHO * p3l[i];
			xa = Math.toDegrees(t3l);
			xb[i] = Math.toDegrees(t3c[i]);
			xc[i] = Math.toDegrees(t4l[i]);
		}
	}

	
	/**Returns maximum lab angle of reaction*/
	public double getTMax(){
		return tmax;
	}
	
	/** Returns energy of beam in MeV.
	 * @return beam energy in MeV
	 */
	public double getBeamEnergy() {
		return eb;
	}

	/**
	 * @return nuclear species of residual (including excitation energy)
	 */
	public Nucleus getResidual() {
		return residual;
	}

	/**
	 * @return nuclear species of target
	 */
	public Nucleus getTarget() {
		return target;
	}

	/**
	 * @return nuclear species of beam
	 */
	public Nucleus getBeam() {
		return beam;
	}

	/**
	 * @return nuclear species of projectile into spectrometer
	 */
	public Nucleus getProjectile() {
		return projectile;
	}

	/** Returns the Q-value, which is the net energy released in the reaction.
	 * @return the Q-value for this reaction
	 */
	public UncertainNumber getQValue() {
		return Q;
	}

	/**
	 * @param _target target nuclide
	 * @param _beam beam nuclide
	 * @param _projectile projectile nuclide
	 * @param _Ex excitation in MeV of residual nucleus
	 * @return Q-value for reaction to specified state of residual nucleus
	 */
	static public UncertainNumber getQValue(
		Nucleus _target,
		Nucleus _beam,
		Nucleus _projectile,
		UncertainNumber _Ex)
		throws NuclearException {
		return (new Reaction(_target,_beam,_projectile,1000.0,0.0,_Ex.value)).getQValue();
	}

	/**
	 * 
	 * @param _target target nuclide
	 * @param _beam beam nuclide 
	 * @param _projectile projectile nuclide
	 * @param _Ex excitation in MeV of residual nucleus
	 * @return residual nucleus for hypothetical reaction
	 * @throws NuclearException
	 */
	static public Nucleus getResidual(
		Nucleus _target,
		Nucleus _beam,
		Nucleus _projectile,
		UncertainNumber _Ex)
		throws NuclearException {
		return new Nucleus(
			_target.Z + _beam.Z - _projectile.Z,
			_target.A + _beam.A - _projectile.A,
			_Ex);
	}

	/** Returns 0,1, or 2, for the number of CM angles corresponding to the lab angle
	 * in this reaction.  Zero is returned in the case of the lab angle requested
	 * being over a maximum limiting angle.
	 * @return number of solutions
	 */
	public int getAngleDegeneracy() {
		if (overMax /*|| !calculated*/
			)
			return 0;
		return t3c.length;
	}
	
	
	/**this is new-- to determine if random thetaCM for projectile is over the maximum kinetically
	 * allowed angle; to be used to reject incorrect random thetaCM's
	 * @return overMax
	 */
	public boolean getOverMax() {
		return overMax;
	}

	/** Returns the angle in degrees in the CM system of the projectile.
	 * @param which which of the solutions to return
	 * @return CM angle in degrees
	 */
	
	
	public double getCMAngleProjectile(int which) {
		return Math.toDegrees(t3c[which]);
	}

	public double getLabEnergyProjectile(int which) {
		return e3l[which];
	}

	public UncertainNumber getLabEnergyProjectile(int which, int options)
		throws KinematicsException {
		double value = getLabEnergyProjectile(which);
		Vector deviations = new Vector(4);
		if ((options & Reaction.UNCERTAIN_BEAM_MASS_OPTION) > 0) {
			calculateOption = Reaction.UNCERTAIN_BEAM_MASS_OPTION;
			recalculate();
			deviations.add(new Double(getLabEnergyProjectile(which) - value));
		}
		if ((options & Reaction.UNCERTAIN_TARGET_MASS_OPTION) > 0) {
			calculateOption = Reaction.UNCERTAIN_TARGET_MASS_OPTION;
			recalculate();
			deviations.add(new Double(getLabEnergyProjectile(which) - value));
		}
		if ((options & Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION) > 0) {
			calculateOption = Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION;
			recalculate();
			deviations.add(new Double(getLabEnergyProjectile(which) - value));
		}
		if ((options & Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION) > 0) {
			calculateOption = Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION;
			recalculate();
			deviations.add(new Double(getLabEnergyProjectile(which) - value));
		}
		calculateOption = Reaction.EXACT_OPTION;
		recalculate();
		double error = 0;
		Iterator iter = deviations.iterator();
		while (iter.hasNext()) {
			double temp = ((Double) iter.next()).doubleValue();
			error += temp * temp;
		}
		error = Math.sqrt(error);
		return new UncertainNumber(value, error);
	}

	public double getTotalEnergyProjectile(int which) {
		return w3l[which];
	}

	public double getTotalEnergyResidual(int which) {
		return w4l[which];
	}
	
	
	/*public double getCMmomentumResidual(int which){
		return p4c[which];
	}*/

	public double getLabAngleResidual(int which) {
		return Math.toDegrees(t4l[which]);
	}
	
	public double getCMAngleResidual(int which) {
		return Math.toDegrees(t4c[which]);
	}

	public double getLabEnergyResidual(int which) {
		return e4l[which];
	}

	public double getLabGammaResidual(int which) {
		return w4l[which] / m4;
	}

	public double getLabBetaResidual(int which) {
		return Math.sqrt(1 - Math.pow(getLabGammaResidual(which), -2));
	}

	public double getLabMomentumResidual(int which) {
		double gamma = getLabGammaResidual(which);
		return m4 * Math.sqrt(gamma * gamma - 1);
	}

	public double getJacobianProjectile(int which) {
		return jac3[which];
	}
	public double getFocusParameter(int which) {
		return kp[which];
	}
	public double getQBrho(int which) {
		return qbrho[which];
	}

	static public double getQBrho(Nucleus nuke, double KE) {
		double m = nuke.getMass().value;
		double zeta = KE / m;
		double p = m * Math.sqrt(zeta * (zeta + 2));
		return P_TO_QBRHO * p;
	}
	static public UncertainNumber getQBrho(
		Nucleus nuke,
		UncertainNumber KE,
		boolean varyMass) {
		Vector deviations = new Vector(2);
		double value = Reaction.getQBrho(nuke, KE.value);
		deviations.add(
			new Double(Reaction.getQBrho(nuke, KE.value + KE.error) - value));
		if (varyMass) {
			double m = nuke.getMass().value + nuke.getMass().error;
			double zeta = KE.value / m;
			double p = m * Math.sqrt(zeta * (zeta + 2));
			deviations.add(new Double(P_TO_QBRHO * p - value));
		}
		Iterator iter = deviations.iterator();
		double error = 0;
		while (iter.hasNext()) {
			double temp = ((Double) iter.next()).doubleValue();
			error += temp * temp;
		}
		return new UncertainNumber(value, Math.sqrt(error));
	}

	static public double getKE(Nucleus nuke, double qbr) {
		double p = QBRHO_TO_P * qbr;
		double m = nuke.getMass().value;
		double eta2 = Math.pow(p / m, 2); //eta^2, parameter for calculating T
		return m * eta2 / (1 + Math.sqrt(1 + eta2));
	}

	static public double getBeta(Particle p, double KE) {
		double gamma = KE / p.getMass().value + 1;
		return Math.sqrt(1 - Math.pow(gamma, -2));
	}

	public String toString() {
		return target.toString()
			+ "("
			+ beam.toString()
			+ ","
			+ projectile.toString()
			+ ")"
			+ residual.toString();
	}

	static public void main(String[] args) {
		try {
			Nucleus beam = new Nucleus(2, 3);
			Nucleus target = new Nucleus(14, 29);
			Nucleus projectile = new Nucleus(2, 6);
			double energy = 45;
			double theta = 10;
			double ex = 0;
			Reaction r =
				new Reaction(target, beam, projectile, energy, theta, ex);
			System.out.println(
				"Exact QBr : " + r.getQBrho(0, Reaction.EXACT_OPTION));
			System.out.println(
				"Variable beam mass = "
					+ beam.getMass()
					+ " : "
					+ r.getQBrho(0, Reaction.UNCERTAIN_BEAM_MASS_OPTION));
			System.out.println(
				"Variable target mass = "
					+ target.getMass()
					+ " : "
					+ r.getQBrho(0, Reaction.UNCERTAIN_TARGET_MASS_OPTION));
			System.out.println(
				"Variable projectile mass = "
					+ projectile.getMass()
					+ " : "
					+ r.getQBrho(0, Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION));
			System.out.println(
				"Variable residual mass = "
					+ r.getResidual().getMass()
					+ " : "
					+ r.getQBrho(0, Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION));
			System.out.println(
				"Vary all masses: "
					+ r.getQBrho(
						0,
						Reaction.UNCERTAIN_BEAM_MASS_OPTION
							| Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION
							| Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION
							| Reaction.UNCERTAIN_TARGET_MASS_OPTION));
		} catch (KinematicsException ke) {
			System.err.println(ke);
		} catch (NuclearException ke) {
			System.err.println(ke);
		}
	}
}
