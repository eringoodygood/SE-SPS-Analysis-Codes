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
import jade.physics.Angle;
import jade.physics.Constants;
import jade.physics.Energy;
import jade.physics.Mass;
import jade.physics.Quantity;
import jade.physics.models.RelativisticModel;
import jade.realtime.PoolContext;
import jade.units.Unit;

import java.io.Serializable;
import java.util.Arrays;

import javax.swing.JOptionPane;

import net.sourceforge.nukesim.math.CashKarpRungeKutta;
import net.sourceforge.nukesim.math.Differentiable;
import net.sourceforge.nukesim.math.QuantityUtilities;

/**
 * Class for calculating energy losses of ions in given absorber.
 * 
 * @author Dale Visser
 * @version 1.2
 */
public final class EnergyLoss implements Serializable, NukeUnits {

	/**
	 * 10 <sup>-24 </sup> times Avagodro's number, used for conversions.
	 */
	private final double AVAGADRO = Constants.N.multiply(1e-24).doubleValue(
			Unit.valueOf("1/mol"));//0.60221367;

	private static final double MAX_STEP_FRAC = 0.25;

	private final EnergyLossData data = EnergyLossData.instance();

	/**
	 * The fractional amount of a chemical element in an absorber.
	 */
	private double[] fractions;

	/**
	 * The current absorber object to be used in calculations.
	 */
	private Absorber absorber;

	/**
	 * The atomic numbers of the components of the absorber.
	 */
	private int[] Z;

	/**
	 * The thickness of the absorber in micrograms/cm^2.
	 */
	private Quantity thickness;

	private Nucleus projectile;

	/**
	 * Create an energy loss calculator associated with the given absorber.
	 * 
	 * @param a
	 *            material that energy losses will be calculated in
	 */
	public EnergyLoss(Absorber a) {
		setAbsorber(a);
	}

	/**
	 * Called whenever one wants to change the absorber in this object.
	 * 
	 * @param a
	 *            the new absorber
	 */
	public void setAbsorber(Absorber a) {
		synchronized (this) {
			absorber = a;
			Z = absorber.getElements();
			fractions = absorber.getFractions();
			thickness = absorber.getThickness();
		}
	}

	/**
	 * Returns the absorber object used by this instance.
	 * 
	 * @return the absorbing material specification
	 */
	public Absorber getAbsorber() {
		return absorber;
	}

	/**
	 * Returns the total stopping power, using the absorber information already
	 * set in the current instance of <code>EnergyLoss</code>. By definition,
	 * the stopping power given for non-positive energies is zero.
	 * 
	 * @param p
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy in MeV
	 * @throws NuclearException
	 *             if the energy is greter than 100 MeV/u
	 * @return total stopping power in keV/[&#181;g/cm&#178;]
	 */
	public Quantity getStoppingPower(Nucleus p, Energy energy)
			throws NuclearException {
		Quantity rval;

		setProjectile(p);
		PoolContext.enter();
		try {
			if (QuantityUtilities.isPositive(energy)) {
				rval = getElectronicStoppingPower(p, energy).add(
						getNuclearStoppingPower(p, energy));
			} else {
				rval = Quantity.valueOf(0.0, keV_per_µg);
			}
			rval.export();
		} finally {
			PoolContext.exit();
		}
		return rval;
	}

	/**
	 * Gets dx/dE, the inverse of the stopping power, which is used for
	 * calculating the range.
	 * 
	 * @param energy
	 *            initial energy in MeV
	 * @param x
	 *            any array, since it isn't used here
	 * @return dx/dE in [&#181;g/cm&#178;]/MeV
	 * @see net.sourceforge.nukesim.math.Differentiable#dydx(double,double[])
	 */
	private Differentiable dEdx = new Differentiable() {
		public Quantity[] dydx(Quantity x, Quantity[] E) {
			final int n = E.length;
			final Quantity[] rval = new Quantity[n];
			try {
				for (int i = 0; i < n; i++) {
					rval[i] = getStoppingPower(projectile,
							Energy.energyOf(E[i])).negate();
				}
			} catch (NuclearException ne) {
				JOptionPane.showMessageDialog(null, ne.getMessage(), getClass()
						.getName(), JOptionPane.ERROR_MESSAGE);
			}
			return rval;
		}
	};

	private Differentiable dxdE = new Differentiable() {
		public Quantity[] dydx(Quantity E, Quantity[] x) {
			final int n = x.length;
			final Quantity[] rval = new Quantity[n];
			try {
				final Quantity deriv = getStoppingPower(projectile,
						Energy.energyOf(E)).inverse().negate();
				Arrays.fill(rval, deriv);
			} catch (NuclearException ne) {
				JOptionPane.showMessageDialog(null, ne.getMessage(), getClass()
						.getName(), JOptionPane.ERROR_MESSAGE);
			}
			return rval;
		}
	};

	/**
	 * Returns the range in the absorber material specified in this instance of
	 * <code>EnergyLoss</code>.
	 * 
	 * @param p
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy
	 * @return the range in &#181;g/cm&#178;
	 */
	public Quantity getRange(Nucleus p, Energy energy) {
		final Quantity rval;
		setProjectile(p);
		PoolContext.enter();
		try {
			final Quantity[] ystart = { zeroThickness };
			final CashKarpRungeKutta adder = new CashKarpRungeKutta(dxdE);
			final Quantity stepSize = Quantity.valueOf(1, keV);
			final Quantity minStepSize = Quantity.valueOf(1e-4, keV);
			adder.integrate(energy, Energy.ZERO, ystart, 0.005, stepSize,
					minStepSize);
			rval = (Quantity) adder.getIntegral()[0].export();
		} finally {
			PoolContext.exit();
		}
		return rval;
	}

	public Quantity getRangeUnc(Nucleus p, Energy energy) {
		final Quantity rval;
		PoolContext.enter();
		try {
			final Quantity value = getRange(p, energy);
			final double dval = value.doubleValue();
			final double error = getFractionError(p, energy) * dval;
			rval = (Quantity) Quantity.valueOf(dval, error, value
					.getSystemUnit());
		} finally {
			PoolContext.exit();
		}
		return rval;
	}

	public final Quantity zeroThickness = Quantity.valueOf(0, µg_per_cmsq);

	/**
	 * Returns the total energy loss in keV. Uses the absorber information
	 * already set in the current instance of <code>EnergyLoss</code>. See p.
	 * 16 of v.3 of "Stopping and Ranges of Ions in Matter" by Ziegler
	 * 
	 * @param p
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy in MeV
	 * @param theta
	 *            angle of incidence (from normal in radians)
	 * @return energy loss in keV
	 */
	public Energy getEnergyLoss(Nucleus p, final Energy energy, Angle theta) {
		final Energy eFinal = getFinalEnergy(p, energy, theta);
		return energy.equals(eFinal) || energy.approxEquals(eFinal) ? Energy.ZERO
				: Energy.energyOf(energy.subtract(eFinal));
	}

	public Energy getFinalEnergy(Nucleus p, final Energy energy) {
		return getFinalEnergy(p, energy, Angle.ZERO);
	}

	public Energy getFinalEnergy(Nucleus p, final Energy energy,
			final Angle theta) {
		final Energy rval;

		setProjectile(p);
		/* Take care of case where given absorber has no thickness. */
		if (energy.isPossiblyZero()) {
			rval = Energy.ZERO;
		} else if (absorber.getThickness().isPossiblyZero()) {
			rval = energy;
		} else {
			PoolContext.enter();
			try {
				final Quantity x = thickness.divide(theta.cos());
				final Quantity range = getRange(p, energy);
				if (range.compareTo(x) <= 0) {
					rval = Energy.ZERO;
				} else {
					final CashKarpRungeKutta loser = new CashKarpRungeKutta(
							dEdx);
					final Energy[] initE = { energy };
					/* 0.5% accuracy is more than sufficient */
					final double eps = 0.005;
					final Quantity delXmin = x.multiply(0.001);
					loser.integrate(x.multiply(0), x, initE, eps, x, delXmin);
					rval = (Energy) Energy.energyOf(loser.getIntegral()[0])
							.export();
				}
			} finally {
				PoolContext.exit();
			}
		}
		return rval;
	}

	/**
	 * Returns initial ion energy in MeV.
	 * 
	 * @param p
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy in MeV
	 * @param theta
	 *            angle of incidence (from normal in radians)
	 * @return initial energy in MeV the ion would have had to emerge with the
	 *         given energy
	 */
	public Energy reverseEnergyLoss(Nucleus p, final Energy energy, Angle theta) {
		setProjectile(p);
		final Quantity x = thickness.divide(theta.cos());
		Quantity xc = zeroThickness;
		Energy ei = energy;
		boolean stop = false;
		Quantity h = x.divide(4);
		try {
			Energy deltaE = Energy.energyOf(getStoppingPower(projectile, ei)
					.multiply(h));
			while (!stop) {
				if (deltaE.divide(ei).abs().doubleValue() < MAX_STEP_FRAC) {//step
					// not
					// too
					// big
					if (h.add(xc).subtract(x).doubleValue(µg_per_cmsq) > 0) {//last
						// step
						stop = true;
						h = x.subtract(xc);//remaining thickness
					} else {
						xc = xc.add(h);
					}
					deltaE = Energy.energyOf(getStoppingPower(projectile, ei)
							.multiply(h));
					ei = Energy.energyOf(ei.add(deltaE));
				} else {//step size too big, adjust lower
					h = h.divide(2);
					deltaE = Energy.energyOf(getStoppingPower(projectile, ei)
							.multiply(h));
				}
			}
		} catch (NuclearException ne) {
			ne.printStackTrace();
			ei = Energy.energyOf(energy.negate());
		}
		return ei;
	}

	/**
	 * Returns the light output produced in a plastic scintillator by an ion
	 * losing energy in it. The units are arbitrary, normalized to L.O.=30 for
	 * an 8.78 MeV alpha. The formula for this comes from NIM 138 (1976) 93-104,
	 * table 3, row I. The formula is implemented in the private method, getL.
	 * The formula is technically only good for 0.5 MeV/u to 15 MeV/u and
	 * assumes complete stopping, so if there is partial energy loss, we take
	 * the difference of the light output for the initial and final energies.
	 * The accuracy is +/- 20%.
	 * 
	 * @return light output scaled to 30 for an 8.78 MeV &#x3b1;
	 * @param p
	 *            incident projectile
	 * @param energy
	 *            in MeV
	 * @param theta
	 *            incidence angle in radians
	 */
	public double getPlasticLightOutput(Nucleus p, Energy energy, Angle theta) {
		/* the next line sets the projectile */
		final Energy efinal = Energy.energyOf(energy.subtract(getEnergyLoss(p,
				energy, theta)));
		final boolean stopped = efinal.compareTo(Energy.ZERO) <= 0;
		final double topL = getL(p, energy);
		final double rval = stopped ? topL : topL - getL(p, efinal);
		return rval;
	}

	/**
	 * 
	 * @param p
	 *            projectile
	 * @param energy
	 *            of projectile
	 * @return total light output signal strength
	 */
	private double getL(Nucleus p, Energy energy) {
		final double eMeV = energy.doubleValue(MeV);
		return 4.0 * Math.pow(p.getChargeNumber() * p.getMassNumber(), -0.63)
				* Math.pow(eMeV, 1.62);
	}

	/**
	 * Returns the total energy loss in keV at normal incidence. Uses the
	 * absorber information already set in the current instance of
	 * <code>EnergyLoss</code>. See p. 16 of v.3 of "Stopping and Ranges of
	 * Ions in Matter" by Ziegler.
	 * 
	 * @param p
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy in MeV
	 * @return energy loss in keV
	 */
	public Energy getEnergyLoss(Nucleus p, Energy energy) {
		return getEnergyLoss(p, energy, Angle.ZERO);//sets projectile
	}

	/**
	 * Returns the total energy loss in keV. Uses the absorber information
	 * already set in the current instance of <code>EnergyLoss</code>. See p.
	 * 16 of v.3 of "Stopping and Ranges of Ions in Matter" by Ziegler
	 * 
	 * @param ion
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy in MeV
	 * @return energy loss in keV
	 */
	public Energy getEnergyLossUnc(Nucleus ion, Energy energy) {
		/* The next line sets the projectile */
		final Energy eloss = getEnergyLoss(ion, energy);//in keV
		final boolean notStopped = !eloss.approxEquals(energy);
		final double eMeV = eloss.doubleValue(MeV);
		return notStopped ? Energy.energyOf(Quantity.valueOf(eMeV,
				eMeV
						* getFractionError(ion, Energy.energyOf(energy
								.subtract(eloss))), MeV)) : eloss;
	}

	/**
	 * Returns the fractional error in energy loss. Uses the absorber
	 * information already set in the current instance of
	 * <code>EnergyLoss</code>. See p. 16 of v.3 of "Stopping and Ranges of
	 * Ions in Matter" by Ziegler
	 * 
	 * @param p
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy in MeV
	 * @return the relative error bar for the stopping power
	 */
	private double getFractionError(Nucleus p, Energy energy) {
		/* no need to set projectile */
		final Mass mass = p.getMass();
		final double ea = energy.divide(mass).doubleValue(MeVperAmu);
		double rval = 0.025;
		if (ea <= 2.0) {
			if (absorber instanceof Gas) {
				rval = 0.1;
			} else {//solid
				rval = 0.05;
			}
		}
		return rval;
	}

	private double[] getLowEnergyES(double ea) {
		final double[] stopH = new double[Z.length];
		final double sea = Math.sqrt(ea);
		for (int i = 0; i < Z.length; i++) {
			stopH[i] = data.COEFFS[0][Z[i]] * sea;
		}
		return stopH;
	}

	private double[] getMedEnergyES(double ea) {
		final double[] stopH = new double[Z.length];
		for (int i = 0; i < Z.length; i++) {
			final double sLo = data.COEFFS[1][Z[i]] * Math.pow(ea, 0.45);
			final double sHi = data.COEFFS[2][Z[i]]
					/ ea
					* Math.log(1.0 + data.COEFFS[3][Z[i]] / ea
							+ data.COEFFS[4][Z[i]] * ea);
			stopH[i] = sLo * sHi / (sLo + sHi);
		}
		return stopH;
	}

	private double[] getHiEnergyES(double mass, double energy, double ea) {
		final double[] stopH = new double[Z.length];
		/* beta squared */
		final double beta2 = (energy * (energy + 2. * mass))
				/ Math.pow(energy + mass, 2.0);
		for (int i = 0; i < Z.length; i++) {
			stopH[i] = data.COEFFS[5][Z[i]] / beta2;
			double logTerm = data.COEFFS[6][Z[i]] * beta2 / (1.0 - beta2)
					- beta2 - data.COEFFS[7][Z[i]];
			for (int j = 1; j <= 4; j++) {
				logTerm += data.COEFFS[7 + j][Z[i]] * Math.pow(Math.log(ea), j);
			}
			stopH[i] *= Math.log(logTerm);
		}
		return stopH;
	}

	private double[] getHydrogenEs(double mass, double energy, double ea) {
		final double[] stopH;
		if (ea <= 10.0) {
			stopH = getLowEnergyES(ea);
		} else if (ea > 10.0 && ea <= 1000.0) {
			stopH = getMedEnergyES(ea);
		} else {// ea > 1000.0 && ea <= 100000.0
			stopH = getHiEnergyES(mass, energy, ea);
		}
		return stopH;
	}

	/**
	 * Returns the stopping power due to collisions with electrons. Uses the
	 * absorber information already set in <code>EnergyLoss</code>. See p. 16
	 * in Andersen & Ziegler, "The Stopping and Ranges of Ions in Matter",
	 * volume 3.
	 * 
	 * @param p
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy in MeV
	 * @throws NuclearException
	 *             if the ion energy is greater than 100 AMeV
	 * @return stopping power in keV/[&#181;g/cm&#178]
	 */
	public Quantity getElectronicStoppingPower(Nucleus p, Energy energy)
			throws NuclearException {
		setProjectile(p);
		/*
		 * First part - calculate stopping power for 1H at this energy Find
		 * Energy in keV per amu of projectile
		 */
		final Mass mass = projectile.getMass();
		final double ea = energy.divide(mass).doubleValue(keVperAmu);//keV/amu
		/* Find contributions of components */
		final double[] stopH;
		if (ea > 100000.0) {
			final String s1 = ".getElectronicStoppingPower(): E/A in keV/amu  > 100000: ";
			final StringBuffer message = new StringBuffer(getClass().getName())
					.append(s1).append(ea);
			throw new NuclearException(message.toString());
		} else {
			stopH = getHydrogenEs(mass.doubleValue(MeV), energy
					.doubleValue(MeV), ea);
		}
		if (projectile.getChargeNumber() > 1) {//adjust if not hydrogen
			adjustForZratio(stopH, ea);
		}
		return Quantity.valueOf(sumComponents(stopH), keV_per_µg);
	}

	private double sumComponents(double[] stopH) {
		double stotal = 0.0;
		double conversion = 0;
		for (int i = 0; i < Z.length; i++) {
			conversion += fractions[i]
					* data.NATURALWEIGHT[Z[i]].doubleValue(g_per_mol);
			stotal += fractions[i] * stopH[i];
		}
		stotal *= AVAGADRO / conversion;
		return stotal;
	}

	private void adjustForZratio(double[] stopH, final double ea) {
		for (int i = 0; i < Z.length; i++) {
			final int projZ = projectile.getChargeNumber();
			double zratio;//3 diff cases He,Li,Heavy Ion see p.9, v.5
			if (projZ < 4) {
				final double lea = Math.log(ea);
				double expterm = Math.pow(7.6 - lea, 2.0);
				final double gamma = 1. + (.007 + .00005 * Z[i])
						* Math.exp(-expterm);
				if (projZ == 2) {//He ion
					expterm = .7446 + .1429 * lea + .01562 * lea * lea - .00267
							* Math.pow(lea, 3.0) + 1.325e-06
							* Math.pow(lea, 8.0);
					zratio = 2. * gamma * (1. - Math.exp(-expterm));
				} else {//Li ion
					zratio = 3.
							* gamma
							* (1. - Math.exp(-(.7138 + .002797 * ea + 1.348e-06
									* ea * ea)));
				}
			} else {// projectile.Z>= 4
				final double b = .886 * Math.sqrt(ea / 25.)
						/ Math.pow(projZ, 2. / 3.);
				final double a = b + .0378 * Math.sin(Math.PI * b / 2.);
				zratio = 1 - Math.exp(-a)
						* (1.034 - .1777 * Math.exp(-.08114 * projZ));
				zratio *= projZ;
			}
			stopH[i] *= zratio * zratio;
		}
	}

	private synchronized void setProjectile(Nucleus ion) {
		projectile = ion;
	}

	/**
	 * Returns the stopping power due to nuclear collisions. Uses the absorber
	 * information already set in the current instance of
	 * <code>EnergyLoss</code>.
	 * 
	 * @param p
	 *            the ion being stopped
	 * @param energy
	 *            the ion kinetic energy in MeV
	 * @return stopping power in keV/[&#181;g/cm&#178]
	 */
	public Quantity getNuclearStoppingPower(Nucleus p, Energy energy) {
		setProjectile(p);
		final int z1 = projectile.getChargeNumber();
		final double keV = energy.doubleValue(NukeUnits.keV);
		final double m1 = projectile.getMass().doubleValue(amu);
		final double two3 = 2.0 / 3.0;
		double stotal = 0.0;
		for (int i = 0; i < Z.length; i++) {
			final int z2 = Z[i];
			final double m2 = data.NATURALWEIGHT[z2].doubleValue(g_per_mol);
			/* Ziegler v.5 p. 19, eqn. 17 */
			final double x = (m1 + m2)
					* Math.sqrt(Math.pow(z1, two3) + Math.pow(z2, two3));
			final double eps = 32.53 * m2 * keV / (z1 * z2 * x);
			// Ziegler v.5 p. 19, eqn. 15
			final double sn1 = 0.5 * Math.log(1.0 + eps)
					/ (eps + 0.10718 * Math.pow(eps, 0.37544));
			/*
			 * Ziegler v.5 p. 19, eqn. 16, stopping power in eV/1e15 atoms/cm^2
			 */
			final double sn2 = sn1 * 8.462 * z1 * z2 * m1 / x;
			final double conversion = AVAGADRO / m2;
			final double stemp = fractions[i] * sn2 * conversion;
			stotal += stemp;
		}
		return Quantity.valueOf(stotal, keV_per_µg);
	}

	public static void main(String[] args) {
		JADE.initialize();
		RelativisticModel.select();
		try {
			final Quantity length = Quantity.valueOf(0.5, mm);
			final Solid absorber = new Solid(length, "Al");
			final EnergyLoss eLoss = new EnergyLoss(absorber);
			final Nucleus[] projectiles = { new Nucleus(1, 1),
					new Nucleus(2, 3), new Nucleus(2, 4) };
			final Energy energy = Energy.energyOf(Quantity.valueOf(35, MeV));
			for (int i = 0; i < projectiles.length; i++) {
				final String result = QuantityUtilities.reportQuantity(eLoss
						.getRangeUnc(projectiles[i], energy).divide(
								absorber.getDensity()), mm, true);
				System.out.println("A " + energy.toText(MeV) + " "
						+ projectiles[i] + " has a range of " + result + " in "
						+ absorber.getText() + ".");
				System.out.println("\tThe energy loss in "
						+ length.toText(mm)
						+ " is "
						+ QuantityUtilities.reportQuantity(eLoss
								.getEnergyLossUnc(projectiles[i], energy), MeV,
								true));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}