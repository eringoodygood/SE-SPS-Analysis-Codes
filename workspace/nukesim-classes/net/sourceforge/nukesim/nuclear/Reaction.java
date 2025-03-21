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
package net.sourceforge.nukesim.nuclear;

import jade.JADE;
import jade.math.Operable;
import jade.physics.Angle;
import jade.physics.Constants;
import jade.physics.Energy;
import jade.physics.Mass;
import jade.physics.Quantity;
import jade.physics.Scalar;
import jade.physics.models.RelativisticModel;

/**
 * Given experimental parameters beam energy, beam, target, projectile, can give
 * useful quantities via it's getter methods. Core of code is from Kazim
 * Yildiz's relkin code written Fortran.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser </a>
 * @version 1.0
 */
public class Reaction implements NukeUnits {

	/**
	 * target(beam,projectile)residual
	 */
	protected Nucleus target, beam, projectile, residual;

	private Energy residualExcitation;

	private Energy qValue;

	private Angle tmax; //The maximum kinematically allowed angle in the lab.

	private Angle t3l;//Angle of particle 3 in lab

	private Quantity xx; //units of mass^2

	private Scalar aa, bb, cc;

	private Angle[] t3c; //COM angle(s) of 3

	private Mass m1, m2, m3, m4; //nuclei masses

	private Energy eb; //beam kinetic energy

	private Energy w2l;

	private Quantity p2l;

	private Scalar b2l;//speed parameter beta for particle 2 in the lab

	//total energy, momentum, gamma, beta of 2 in lab
	private Energy w1l;//total energy of 1 in lab

	private Energy wtl; //total lab energy

	private Energy w3c;

	private Quantity p3c;

	private Scalar g3c, b3c, k3c; //total

	// energy,momentum,gamma,beta, ?
	// of 3

	//in COM frame
	private Energy[] w3l, e3l; //total, kinetic energies of Particle 3 in lab

	private Energy w4c; // total energy of Particle 4 in CM

	private Quantity p4c;// momentum of Particle 4 in CM

	private Scalar b4c, k4c;//beta and ? of Particle 4 in CM

	private Angle[] t4c, t4l; //COM, lab angles of particle 4

	private Energy[] w4l, e4l; //total, kinetic energies of Particle 4 in lab

	private Scalar b, g;//cms beta, gamma

	private Energy wtc; //total cms energy

	private Scalar[] jac3; //Jacobian for particle 3

	private Scalar[] kp; //1/p dp/dtheta,lab for particle 3, used for Enge

	// focusing

	private Quantity[] p3l;

	private Quantity[] qbrho; //momentum & rigidity, used for magnetic

	// spectrometers

	double ex2, ex3, ex4; //excitations of the target, projectile, and residual

	boolean overMax = false;

	/**
	 * Constructor.
	 * 
	 * @param thetaLab
	 *            of projectile in degrees
	 * @param target
	 *            nuclear species in target
	 * @param beam
	 *            nuclear species of beam
	 * @param projectile
	 *            nuclear species of projectile into spectrometer
	 * @param Tbeam
	 *            beam energy
	 * @param residualExcitation
	 *            excitation energy of the residual nucleus
	 * @throws KinematicsException
	 *             if a calculation error occurs
	 */
	public Reaction(Nucleus target, Nucleus beam, Nucleus projectile,
			Energy Tbeam, Angle thetaLab, Energy residualExcitation)
			throws NuclearException, KinematicsException {
		this.target = target;
		this.beam = beam;
		this.projectile = projectile;
		this.residualExcitation = residualExcitation;
		try {
			residual = getResidual(target, beam, projectile, residualExcitation);
		} catch (Exception e) {
			throw new NuclearException(e.getMessage());
		}
		eb = Tbeam;
		t3l = thetaLab;
		setQ();
		calculate();
	}

	public Reaction(Nucleus target, Nucleus beam, Nucleus projectile,
			Energy Tbeam, Angle thetaLab, double residualExcitation)
			throws NuclearException, KinematicsException {
		this(target, beam, projectile, Tbeam, thetaLab, Energy
				.energyOf(Quantity.valueOf(residualExcitation, MeV)));
	}

	/**
	 * Constructor to make some changes to the parameters of a previous
	 * instance.
	 * 
	 * @param r
	 *            previous <CODE>Reaction</CODE> to copy species info from
	 * @param eBeam
	 *            new beam energy in MeV
	 * @param thetaLab
	 *            new lab angle in degrees
	 * @param residExcite
	 *            new excitation in MeV for the residual nucleus
	 * @throws KinematicsException
	 *             if a calculation problem occurs
	 */
	public Reaction(Reaction r, Energy eBeam, Angle thetaLab, double residExcite)
			throws NuclearException, KinematicsException {
		this(r.getTarget(), r.getBeam(), r.getProjectile(), eBeam, thetaLab,
				Energy.energyOf(Quantity.valueOf(residExcite, MeV)));
	}

	public Reaction(Reaction r, Energy residExcite) throws NuclearException,
			KinematicsException {
		this(r.getTarget(), r.getBeam(), r.getProjectile(), r.getBeamEnergy(),
				r.getThetaLab(), residExcite);
	}

	public Angle getThetaLab() {
		return t3l;
	}

	static public class VaryOption {
		private final int option;

		private VaryOption(int opt) {
			option = opt;
		}

		private int getOption() {
			return option;
		}

		public VaryOption add(VaryOption opt) {
			return new VaryOption(option | opt.getOption());
		}

		public boolean hasOption(VaryOption opt) {
			boolean rval = false;
			final int nopt = opt.getOption();
			if (opt.getOption() == 0) {
				rval = option == 0;
			} else {
				rval = (nopt & option) > 0;
			}
			return rval;
		}

		static public final VaryOption EXACT_OPTION = new VaryOption(0);

		static public final VaryOption UNCERTAIN_BEAM_MASS_OPTION = new VaryOption(
				1);

		static public final VaryOption UNCERTAIN_TARGET_MASS_OPTION = new VaryOption(
				2);

		static public final VaryOption UNCERTAIN_PROJECTILE_MASS_OPTION = new VaryOption(
				4);

		static public final VaryOption UNCERTAIN_RESIDUAL_MASS_OPTION = new VaryOption(
				8);

		static public final VaryOption ALL = EXACT_OPTION.add(
				UNCERTAIN_BEAM_MASS_OPTION).add(UNCERTAIN_TARGET_MASS_OPTION)
				.add(UNCERTAIN_PROJECTILE_MASS_OPTION).add(
						UNCERTAIN_RESIDUAL_MASS_OPTION);
	}

	/**
	 * options (can be or'ed together) for requesting QBrho
	 */
	private VaryOption calculateOption = VaryOption.EXACT_OPTION;

	private void calculate() throws KinematicsException {
		setKinematicQuantities();
		setThetaCOM();
		comToLab();
	}

	/**
	 * Using the given momentum of the projectile in the lab, calculates the
	 * necessary excitation of the residual nucleus. This is useful for
	 * interpreting spectrometer measurements.
	 * 
	 * @param labMomentum3
	 *            the lab frame momentum of the projectile, in MeV/c
	 * @return excitation energy of residual nucleus
	 */
	public Energy getEx4(Quantity labMomentum3) {
		final Quantity p3l = labMomentum3;
		final Operable p4lSq = ((Quantity) p2l.pow(2)).subtract(
				p2l.multiply(2).multiply(p3l).multiply(t3l.cos())).plus(
				p3l.pow(2));
		final Energy w3l = (Energy) ((Quantity) (p3l.pow(2)).plus(m3.pow(2)))
				.root(2);
		final Energy w4l = (Energy) wtl.subtract(w3l);
		final Mass mresid_gs = residual.getGroundStateMass();
		final Energy bestEstimate = (Energy) ((((Quantity) w4l.pow(2))
				.subtract((Quantity) p4lSq)).root(2)).subtract(mresid_gs);
		return bestEstimate;
	}

	/**
	 * Gives some information on the state of this object.
	 */
	public void printStatus() {
		System.out.println("Q-value: " + qValue.toText(MeV));
		for (int i = 0; i < t3c.length; i++) {
			System.out.println("Beam Energy = " + getBeamEnergy().toText(MeV));
			System.out.println("Residual Excitation = "
					+ residualExcitation.toText(MeV));
			System.out.println("Projectile lab angle = " + t3l.toText(deg));
			System.out.println("Theta 3, cms[" + i + "] = "
					+ t3c[i].toText(deg));
			System.out.println("Projectile lab energy = "
					+ getLabEnergyProjectile(i).toText(MeV));
			System.out.println("Theta 4, lab[" + i + "] = "
					+ t4l[i].toText(deg));
			System.out.println("Residual Lab Energy = "
					+ getLabEnergyResidual(i).toText(MeV));
			System.out.println("Projectile Jacobian = "
					+ getJacobianProjectile(i));
			System.out.println("QBrho[" + i + "] = " + qbrho[i].toText(eTm));
			System.out.println("Momentum of 3, lab[" + i + "] = "
					+ p3l[i].toText(MeV));
		}
	}

	private static Mass stripError(Mass m) {
		return Mass.massOf(Quantity.valueOf(m.doubleValue(MeV), 0, MeV));
	}

	private void setMasses() throws KinematicsException {
		m1 = target.getMass();
		m2 = beam.getMass();
		m3 = projectile.getMass();
		m4 = residual.getMass();
		if (!calculateOption.hasOption(VaryOption.UNCERTAIN_BEAM_MASS_OPTION)) {
			m2 = stripError(m2);
		}
		if (!calculateOption.hasOption(VaryOption.UNCERTAIN_TARGET_MASS_OPTION)) {
			m1 = stripError(m1);
		}
		if (!calculateOption
				.hasOption(VaryOption.UNCERTAIN_PROJECTILE_MASS_OPTION)) {
			m3 = stripError(m3);
		}
		if (!calculateOption
				.hasOption(VaryOption.UNCERTAIN_RESIDUAL_MASS_OPTION)) {
			m4 = stripError(m4);
		}
		if (m1.approxEquals(Mass.ZERO) || m3.approxEquals(Mass.ZERO)) {
			throw new KinematicsException(
					"Only beam or residual may be massless.");
		}
	}

	/**
	 * The workhorse.
	 */
	private void setKinematicQuantities() throws KinematicsException {
		setMasses();
		final Scalar ct3l = t3l.cos();
		final Scalar st3l = t3l.sine();
		w2l = Energy.energyOf(eb.add(m2));
		if (m2.approxEquals(Mass.ZERO)) {
			b2l = Scalar.ONE;
			p2l = eb;
		} else {
			final Scalar g2l = Scalar.scalarOf(w2l.divide(m2));
			b2l = beta(g2l);
			p2l = g2l.multiply(b2l).multiply(m2);
		}
		w1l = Energy.energyOf(m1);
		wtl = Energy.energyOf(w1l.add(w2l));
		b = Scalar.scalarOf(p2l.divide(wtl));
		g = Scalar.scalarOf((Scalar.ONE.subtract((Quantity) b.pow(2)))
				.inverse().root(2));
		wtc = Energy.energyOf(wtl.divide(g));
		if (wtc.doubleValue(MeV) > (m3.add(m4)).doubleValue(MeV)) {
			final Quantity m3sq = (Quantity) m3.pow(2);
			final Quantity m4sq = (Quantity) m4.pow(2);
			final Scalar ct3lsq = Scalar.scalarOf((Quantity) ct3l.pow(2));
			final Scalar gsq = Scalar.scalarOf((Quantity) g.pow(2));
			final Quantity st3lsq = (Quantity) st3l.pow(2);
			final Quantity wtcsq = (Quantity) wtc.pow(2);
			/* Total energy greater than rest mass of products. */
			w3c = Energy.energyOf(wtcsq.add(m3sq).subtract(m4sq).divide(
					wtc.multiply(2)));
			p3c = ((Quantity) w3c.pow(2)).subtract(m3sq).root(2);
			g3c = Scalar.scalarOf(w3c.divide(m3));
			b3c = beta(Scalar.scalarOf(g3c));
			k3c = Scalar.scalarOf(b.divide(b3c));
			w4c = Energy.energyOf(wtcsq.add(m4sq).subtract(m3sq).divide(
					wtc.multiply(2)));
			p4c = (((Quantity) w4c.pow(2)).subtract(m4sq)).root(2);
			if (m4.approxEquals(Mass.ZERO)) {
				b4c = Scalar.ONE;
			} else {
				final Scalar g4c = Scalar.scalarOf(w4c.divide(m4));
				b4c = beta(g4c);
			}
			k4c = Scalar.scalarOf(b.divide(b4c));
			final Quantity xxTerm1 = m1.subtract(m4).multiply(m1.add(m4));
			final Mass m23diff = Mass.massOf(m2.subtract(m3));
			final Quantity xxTerm2 = m23diff.multiply(m1.multiply(2).add(
					m23diff));
			final Quantity xxTerm3 = eb.multiply(2).multiply(m1.subtract(m3));
			xx = xxTerm1.add(xxTerm2).add(xxTerm3);
			aa = Scalar.scalarOf(ct3lsq.add(gsq.multiply(st3lsq)));
			bb = Scalar
					.scalarOf(gsq.multiply(st3lsq).multiply(2).multiply(k3c));
			final Quantity ccParTerm1 = gsq.multiply(st3lsq).multiply(xx);
			final Scalar ccParTerm2 = Scalar.scalarOf((g3c.divide(g))
					.add(Scalar.ONE));
			final Scalar ccParTerm3 = Scalar.scalarOf(((Quantity) g3c.pow(2))
					.subtract(Scalar.ONE));
			final Quantity ccParTerm4 = m3.multiply(2).multiply(wtl);
			cc = Scalar.scalarOf(ct3lsq.multiply(4).multiply(
					ct3lsq.add(ccParTerm1.multiply(ccParTerm2).divide(
							ccParTerm3.multiply(ccParTerm4)))));
		} else { //not enough energy for exit channel
			/* calculate minimum ennergy for channel */
			final Energy def = Energy.energyOf(m3.add(m4).subtract(wtc));
			final Mass mi = Mass.massOf(m1.add(m2));
			final Energy ebcm = Energy.energyOf(qValue.negate());
			final Scalar scaleFactor = Scalar.scalarOf(((ebcm.add(mi
					.multiply(2))).divide(m1)).divide(2));
			final Energy t2est = Energy.energyOf(ebcm.multiply(scaleFactor));
			throw new KinematicsException(this + ": beam energy = "
					+ eb.toText(MeV) + "\nNeed " + def.toText(MeV)
					+ " more in CM frame for reaction."
					+ "\nMinimum beam energy \u2248 " + t2est.toText(MeV) + ".");
		}
	}

	private static Scalar beta(Scalar gamma) {
		final Quantity rval = (Scalar.ONE.subtract((Quantity) gamma.pow(2)
				.reciprocal())).root(2);
		return Scalar.scalarOf(rval);
	}

	private void setQ() {
		if (beam.equals(projectile) || beam.equals(residual)) { //elastic
			qValue = Energy.ZERO;
		} else if (beam.equals(target)) { //identical incoming
			qValue = Energy.energyOf(target.getMass().multiply(2.0).subtract(
					projectile.getMass()).subtract(residual.getMass()));
		} else if (projectile.equals(residual)) { //identical outgoing
			qValue = Energy.energyOf(target.getMass().add(beam.getMass())
					.subtract(projectile.getMass().multiply(2.0)));
		} else {//all unique
			qValue = Energy.energyOf(target.getMass().add(beam.getMass())
					.subtract(projectile.getMass())
					.subtract(residual.getMass()));
		}
	}

	private void setThetaCOM() throws KinematicsException {
		final Scalar[] ct3c;
		final Scalar rootcc = Scalar.scalarOf(cc.root(2));
		final Scalar twoaa = Scalar.scalarOf(aa.multiply(2));
		final Scalar minusbb = Scalar.scalarOf(bb.negate());
		final Scalar cosPlus = Scalar.scalarOf(minusbb.add(rootcc)
				.divide(twoaa));
		final Scalar cosMinus = Scalar.scalarOf(minusbb.subtract(rootcc)
				.divide(twoaa));
		if (xx.doubleValue() > 0.0) { //no thetaMax-->single valued
			ct3c = new Scalar[1];
			ct3c[0] = t3l.cos().doubleValue() >= 0 ? cosPlus : cosMinus;
		} else { //thetaMax exists
			final Quantity xaNum = g.negate().multiply(xx).multiply(g3c.add(g));
			final Quantity xaDen = m3.multiply(2).multiply(wtl).multiply(
					((Quantity) g3c.pow(2)).subtract(Scalar.ONE));
			final Quantity xa = xaNum.divide(xaDen);
			tmax = Scalar.scalarOf((xa.divide(xa.add(Scalar.ONE))).root(2))
					.acos();
			if (t3l.doubleValue() > tmax.doubleValue()) {
				overMax = true;
				throw new KinematicsException("Lab angle: " + t3l.toText(deg)
						+ " > max angle: " + tmax.toText(deg));
			} else { //valid angle
				if (xx.doubleValue() == 0.0) {
					ct3c = new Scalar[1];
				} else { //double valued
					ct3c = new Scalar[2];
					ct3c[1] = cosMinus;
				}
				ct3c[0] = cosPlus;
			}
		}
		final int len = ct3c.length;
		t3c = new Angle[len];
		for (int i = 0; i < len; i++) {
			t3c[i] = ct3c[i].acos();
		}
	}

	private void comToLab() {
		final int len = t3c.length;
		t4c = new Angle[len];
		t4l = new Angle[len];
		w3l = new Energy[len];
		e3l = new Energy[len];
		w4l = new Energy[len];
		e4l = new Energy[len];
		jac3 = new Scalar[len];
		kp = new Scalar[len];
		p3l = new Quantity[len];
		qbrho = new Quantity[len];
		for (int i = 0; i < len; i++) {
			final Scalar ct3c = t3c[i].cos();
			final Quantity ct3cSq = (Quantity) ct3c.pow(2);
			final Scalar st3c = t3c[i].sine();
			final Quantity st3cSq = (Quantity) st3c.pow(2);
			final Quantity gSq = (Quantity) g.pow(2);
			final Quantity tempSum1 = k3c.add(ct3c);
			final Quantity tempSum2 = tempSum1.add(Scalar.ONE);
			final Angle deg180 = Angle.angleOf(Quantity.valueOf(180, deg));
			t4c[i] = Angle.angleOf(t3c[i].subtract(deg180));
			t4l[i] = Scalar.scalarOf(
					t4c[i].sine().divide(g.multiply(t4c[i].cos()).add(k4c)))
					.atan();
			if (t4l[i].multiply(t4c[i]).doubleValue() < 0.0) {
				t4l[i] = Angle.angleOf(t4l[i].subtract(deg180));
			}
			w3l[i] = Energy.energyOf(w3c.multiply(g).multiply(
					Scalar.ONE.add(b.multiply(b3c).multiply(ct3c))));
			e3l[i] = Energy.energyOf(w3l[i].subtract(m3));
			w4l[i] = Energy.energyOf(w4c.multiply(g).multiply(
					Scalar.ONE.add(b.multiply(b4c).multiply(t4c[i].cos()))));
			e4l[i] = Energy.energyOf(w4l[i].subtract(m4));
			final Quantity tempSum1sq = (Quantity) tempSum1.pow(2);
			if (k3c.multiply(ct3c).add(Scalar.ONE).doubleValue() > 0.0) {
				jac3[i] = Scalar.scalarOf(st3cSq.add(gSq.multiply(tempSum1sq))
						.pow(1.5).divide(g.multiply(tempSum2)));
			} else {
				jac3[i] = Scalar.ZERO;
			}
			kp[i] = Scalar.scalarOf(gSq.multiply(tempSum1sq).add(st3cSq)
					.divide(g.multiply(tempSum2)).multiply(
							w3c.negate().multiply(g).multiply(
									b.multiply(b3c).multiply(st3c))).multiply(
							w3l[i]).divide(
							((Quantity) w3l[i].pow(2)).subtract((Quantity) m3
									.pow(2))));
			p3l[i] = e3l[i].multiply(m3.multiply(2).add(e3l[i])).root(2);
			qbrho[i] = momentumToRigidity(p3l[i]);
		}
	}

	/**
	 * Returns energy of beam in MeV.
	 * 
	 * @return beam energy in MeV
	 */
	public Energy getBeamEnergy() {
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

	/**
	 * Returns the Q-value, which is the net energy released in the reaction.
	 * 
	 * @return the Q-value for this reaction
	 */
	public Energy getQValue() {
		return qValue;
	}

	/**
	 * @param _target
	 *            target nuclide
	 * @param _beam
	 *            beam nuclide
	 * @param _projectile
	 *            projectile nuclide
	 * @param _Ex
	 *            excitation in MeV of residual nucleus
	 * @return Q-value for reaction to specified state of residual nucleus
	 */
	static public Energy getQValue(Nucleus _target, Nucleus _beam,
			Nucleus _projectile, Energy _Ex) throws NuclearException {
		return (new Reaction(_target, _beam, _projectile, Energy
				.energyOf(Quantity.valueOf(1000.0, MeV)), Angle.ZERO, _Ex))
				.getQValue();
	}

	/**
	 * 
	 * @param _target
	 *            target nuclide
	 * @param _beam
	 *            beam nuclide
	 * @param _projectile
	 *            projectile nuclide
	 * @param _Ex
	 *            excitation in MeV of residual nucleus
	 * @return residual nucleus for hypothetical reaction
	 * @throws NuclearException
	 */
	static public Nucleus getResidual(Nucleus _target, Nucleus _beam,
			Nucleus _projectile, Energy _Ex) throws NuclearException {
		return new Nucleus(_target.getChargeNumber() + _beam.getChargeNumber()
				- _projectile.getChargeNumber(), _target.getMassNumber()
				+ _beam.getMassNumber() - _projectile.getMassNumber(), _Ex);
	}

	/**
	 * Returns 0,1, or 2, for the number of CM angles corresponding to the lab
	 * angle in this reaction. Zero is returned in the case of the lab angle
	 * requested being over a maximum limiting angle.
	 * 
	 * @return number of solutions
	 */
	public int getAngleDegeneracy() {
		return overMax ? 0 : t3c.length;
	}

	/**
	 * Returns the angle in degrees in the CM system of the projectile.
	 * 
	 * @param which
	 *            which of the solutions to return
	 * @return CM angle in degrees
	 */
	public Angle getCMAngleProjectile(int which) {
		return t3c[which];
	}

	public Energy getLabEnergyProjectile(int which) {
		return e3l[which];
	}

	public synchronized void setVaryOption(VaryOption varyOption)
			throws KinematicsException {
		calculateOption = varyOption;
		calculate();
	}

	public Energy getTotalEnergyProjectile(int which) {
		return w3l[which];
	}

	public Energy getTotalEnergyResidual(int which) {
		return w4l[which];
	}

	public Angle getLabAngleResidual(int which) {
		return t4l[which];
	}

	public Energy getLabEnergyResidual(int which) {
		return e4l[which];
	}

	public Scalar getLabGammaResidual(int which) {
		return Scalar.scalarOf(w4l[which].divide(m4));
	}

	public Scalar getLabBetaResidual(int which) {
		return beta(getLabGammaResidual(which));
	}

	public Quantity getLabMomentumResidual(int which) {
		Scalar gamma = getLabGammaResidual(which);
		return m4.multiply((((Quantity) gamma.pow(2)).subtract(Scalar.ONE))
				.root(2));
	}

	public Scalar getJacobianProjectile(int which) {
		return jac3[which];
	}

	public Scalar getFocusParameter(int which) {
		return kp[which];
	}

	public Quantity getQBrho(int which) {
		return qbrho[which];
	}

	static public Quantity getQBrho(Nucleus nuke, Energy energy) {
		final Mass mass = nuke.getMass();
		final Quantity momentum;
		if (mass.approxEquals(Mass.ZERO)) {
			momentum = energy;
		} else {
			final Scalar zeta = Scalar.scalarOf(energy.divide(mass));
			final Quantity two = Scalar.ONE.multiply(2);
			momentum = mass.multiply(zeta.multiply(zeta.add(two)).root(2));
		}
		return momentumToRigidity(momentum);
	}

	static public Quantity momentumToRigidity(Quantity p) {
		return p.multiply(Constants.c);
	}

	static public Quantity rigidityToMomentum(Quantity qbr) {
		return qbr.divide(Constants.c);
	}

	static public Quantity getQBrho(Nucleus nuke, Energy KE, boolean varyMass) {
		final Mass m = varyMass ? nuke.getMass() : stripError(nuke.getMass());
		final Scalar zeta = Scalar.scalarOf(KE.divide(m));
		final Quantity two = Scalar.ONE.multiply(2);
		//double p = m * Math.sqrt(zeta * (zeta + 2));
		final Quantity p = m.multiply(zeta.multiply(zeta.add(two)).root(2));
		return momentumToRigidity(p);
	}

	static public Energy getKE(Nucleus nuke, Quantity qbr) {
		final Quantity p = rigidityToMomentum(qbr);
		final Mass m = nuke.getMass();
		final Quantity eta2 = (Quantity)p.divide(m).pow(2); //eta^2, parameter for
		// calculating T
		return Energy.energyOf(m.multiply(eta2).divide(
				Scalar.ONE.add(Scalar.ONE.add(eta2)).root(2)));
	}

	static public Scalar getBeta(Particle p, Energy KE) {
		final Scalar gamma = Scalar.scalarOf(KE.divide(p.getMass()).add(
				Scalar.ONE));
		return beta(gamma);
	}

	public String toString() {
		return target.toString() + "(" + beam.toString() + ","
				+ projectile.toString() + ")" + residual.toString();
	}

	static public void main(String[] args) {
		try {
			JADE.initialize();
			RelativisticModel.select();
			final Nucleus beam = new Nucleus(2, 3);
			final Nucleus target = new Nucleus(14, 29);
			final Nucleus projectile = new Nucleus(2, 6);
			final Energy energy = Energy.energyOf(Quantity.valueOf(45, MeV));
			final Angle theta = Angle.angleOf(Quantity.valueOf(10, deg));
			final Energy ex = Energy.ZERO;
			Reaction r = new Reaction(target, beam, projectile, energy, theta,
					ex);
			System.out.println(r);
			r.printStatus();
			System.out.println("--");
			r.setVaryOption(VaryOption.EXACT_OPTION);
			System.out.println("Exact QBr : " + r.getQBrho(0).toText(eTm));
			r.setVaryOption(VaryOption.UNCERTAIN_TARGET_MASS_OPTION);
			System.out.println("Variable target mass = "
					+ target.getMass().toText(MeV) + " : "
					+ r.getQBrho(0).toText(eTm));
			r.setVaryOption(VaryOption.UNCERTAIN_BEAM_MASS_OPTION);
			System.out.println("Variable beam mass = "
					+ beam.getMass().toText(MeV) + " : "
					+ r.getQBrho(0).toText(eTm));
			r.setVaryOption(VaryOption.UNCERTAIN_PROJECTILE_MASS_OPTION);
			System.out.println("Variable projectile mass = "
					+ projectile.getMass().toText(MeV) + " : "
					+ r.getQBrho(0).toText(eTm));
			r.setVaryOption(VaryOption.UNCERTAIN_RESIDUAL_MASS_OPTION);
			System.out.println("Variable residual mass = "
					+ r.getResidual().getMass().toText(MeV) + " : "
					+ r.getQBrho(0).toText(eTm));
			r.setVaryOption(VaryOption.ALL);
			System.out.println("Vary all masses: "
					+ r.getQBrho(0).toText(eTm));
		} catch (KinematicsException ke) {
			System.err.println(ke);
		} catch (NuclearException ke) {
			System.err.println(ke);
		}
	}
}