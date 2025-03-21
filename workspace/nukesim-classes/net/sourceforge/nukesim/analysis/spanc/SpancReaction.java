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
 * SpancReaction.java
 *
 * Created on December 16, 2001, 3:27 PM
 */

package net.sourceforge.nukesim.analysis.spanc;

import jade.physics.Angle;
import jade.physics.ElectricCharge;
import jade.physics.Energy;
import jade.physics.Length;
import jade.physics.MagneticFluxDensity;
import jade.physics.Quantity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.Nucleus;
import net.sourceforge.nukesim.nuclear.NukeUnits;
import net.sourceforge.nukesim.nuclear.Reaction;

/**
 * Class for holding modifiable specifications of different scattering
 * measurements on a magnetic spectrograph.
 * 
 * @author Dale Visser
 * @version 1.2
 * @since 1.0
 */
public final class SpancReaction implements Serializable, NukeUnits {

	private Nucleus beam;

	private boolean beamUncertain = false;

	private Nucleus targetNuclide;

	private boolean targetUncertain = false;

	private Nucleus projectile;

	private boolean projectileUncertain = false;

	private boolean residualUncertain = false;

	private Energy beamEnergy;

	private MagneticFluxDensity magneticField;

	private Target target;

	private int interactionLayer;

	private ElectricCharge projectileCharge;

	private Angle thetaDegrees;

	private static final List REACTIONS = Collections
			.synchronizedList(new ArrayList());

	/**
	 * Creates new SpancReaction.
	 * 
	 * @param beamN
	 *            beam nuclear species
	 * @param tn
	 *            target nuclear species
	 * @param p
	 *            projectile nuclear species
	 * @param e
	 *            beam kinetic energy in MeV
	 * @param b
	 *            spectrograph field in kG
	 * @param t
	 *            target specification
	 * @param il
	 *            layer in target for nuclear interaction
	 * @param q
	 *            charge of the projectile ion
	 * @param angle
	 *            of the spectrograph in degrees
	 */
	public SpancReaction(Nucleus beamN, Nucleus tn, Nucleus p, Energy e,
			MagneticFluxDensity b, Target t, int il, int q, Angle angle) {
		setValues(beamN, tn, p, e, b, t, il, q, angle);
		REACTIONS.add(this);
	}

	/**
	 * Set the parameters of this reaction.
	 * 
	 * @param beamN
	 *            beam nuclear species
	 * @param tn
	 *            target nuclear species
	 * @param p
	 *            projectile nuclear species
	 * @param e
	 *            beam kinetic energy in MeV
	 * @param b
	 *            spectrograph field in kG
	 * @param t
	 *            target specification
	 * @param il
	 *            layer in target for nuclear interaction
	 * @param q
	 *            charge of the projectile ion
	 * @param angle
	 *            of the spectrograph in degrees
	 */
	public void setValues(Nucleus beamN, Nucleus tn, Nucleus p, Energy e,
			MagneticFluxDensity b, Target t, int il, int q, Angle angle) {
		beam = beamN;
		targetNuclide = tn;
		projectile = p;
		beamEnergy = e;
		magneticField = b;
		target = t;
		interactionLayer = il;
		projectileCharge = ElectricCharge
				.electricChargeOf(ElectricCharge.ELEMENTARY.multiply(q));
		thetaDegrees = angle;
	}

	/**
	 * Set whether to consider the beam mass uncertain when determining
	 * statistical error bars. Should be true when multiple beams are used for
	 * calibration.
	 * 
	 * @param state
	 *            true if the beam mass is uncertain
	 */
	public void setBeamUncertain(boolean state) {
		beamUncertain = state;
	}

	public boolean getBeamUncertain() {
		return beamUncertain;
	}

	public void setTargetUncertain(boolean state) {
		targetUncertain = state;
	}

	public boolean getTargetUncertain() {
		return targetUncertain;
	}

	public void setProjectileUncertain(boolean state) {
		projectileUncertain = state;
	}

	public boolean getProjectileUncertain() {
		return projectileUncertain;
	}

	public void setResidualUncertain(boolean state) {
		residualUncertain = state;
	}

	public boolean getResidualUncertain() {
		return residualUncertain;
	}

	public java.lang.String toString() {
		String rval = "Reaction " + REACTIONS.indexOf(this) + "\n";
		rval += description() + "\n";
		rval += "Target Name: " + target.getName() + ", interaction layer = "
				+ interactionLayer + "\n";
		rval += "Projectile:  Q = +" + projectileCharge + ", Theta = "
				+ thetaDegrees + " deg\n";
		rval += "B-field = " + magneticField + " kG\n";
		return rval;
	}

	public String description() {
		return targetNuclide + "(" + beamEnergy + " MeV " + beam + ","
				+ projectile + ")";
	}

	static public void removeReaction(int index) {
		REACTIONS.remove(index);
	}

	static public void removeAllReactions() {
		REACTIONS.clear();
	}

	static public int getReactionIndex(SpancReaction reaction) {
		return REACTIONS.indexOf(reaction);
	}

	static public SpancReaction[] getAllReactions() {
		SpancReaction[] rval = new SpancReaction[REACTIONS.size()];
		REACTIONS.toArray(rval);
		return rval;
	}

	static public Collection getReactionCollection() {
		return REACTIONS;
	}

	static public void refreshData(Collection retrievedReactions) {
		REACTIONS.addAll(retrievedReactions);
	}

	static public SpancReaction getReaction(int index) {
		return (SpancReaction) REACTIONS.get(index);
	}

	Length getRho(Energy ExProjectile, Energy ExResidual)
			throws KinematicsException, NuclearException {
		Nucleus tempProjectile = new Nucleus(projectile.getChargeNumber(),
				projectile.getMassNumber(), ExProjectile);
		Reaction.VaryOption calculateOption = Reaction.VaryOption.EXACT_OPTION;
		final Length[] rho = new Length[2];
		final Energy exResErr = Energy.energyOf(ExResidual.multiply(ExResidual
				.getRelativeError()));
		for (int i = 0; i < 2; i++) {
			final Energy exResid = Energy.energyOf(ExResidual.add(exResErr
					.multiply(i)));
			Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile,
					target.calculateInteractionEnergy(interactionLayer, beam,
							beamEnergy), thetaDegrees, exResid);
			Energy KEinit = rxn.getLabEnergyProjectile(0);
			Energy KEfinal = target.calculateProjectileEnergy(interactionLayer,
					tempProjectile, KEinit, thetaDegrees);
			rho[i] = Length.lengthOf(Reaction.getQBrho(tempProjectile, KEfinal)
					.divide(projectileCharge).divide(magneticField));
		}
		Length error_from_ex = Length.lengthOf(rho[0].subtract(rho[1]).abs());
		if (beamUncertain) {
			calculateOption.add(Reaction.VaryOption.UNCERTAIN_BEAM_MASS_OPTION);
		}
		if (targetUncertain) {
			calculateOption
					.add(Reaction.VaryOption.UNCERTAIN_TARGET_MASS_OPTION);
		}
		if (projectileUncertain) {
			calculateOption
					.add(Reaction.VaryOption.UNCERTAIN_PROJECTILE_MASS_OPTION);
		}
		if (residualUncertain) {
			calculateOption
					.add(Reaction.VaryOption.UNCERTAIN_RESIDUAL_MASS_OPTION);
		}
		Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile,
				target.calculateInteractionEnergy(interactionLayer, beam,
						beamEnergy), thetaDegrees, ExResidual);
		rxn.setVaryOption(calculateOption);
		Energy KEinit = rxn.getLabEnergyProjectile(0);
		Energy KEfinal = target.calculateProjectileEnergy(interactionLayer,
				tempProjectile, KEinit, thetaDegrees);
		Quantity qbr_unc_masses = Reaction.getQBrho(tempProjectile, Energy
				.energyOf(Quantity.valueOf(KEfinal.doubleValue(MeV), KEinit
						.multiply(KEinit.getRelativeError()).doubleValue(MeV),
						MeV)), projectileUncertain);
		Length rho_unc_masses = Length.lengthOf(qbr_unc_masses.divide(
				projectileCharge).divide(magneticField));
		return Length.lengthOf(Quantity.valueOf(rho[0].doubleValue(cm),
				((Quantity) error_from_ex.pow(2)).add(
						(Quantity) rho_unc_masses.pow(2)).root(2).doubleValue(
						cm), cm));
	}

	Energy getExResid(Energy ExProjectile, Length rho)
			throws KinematicsException, NuclearException {
		Nucleus tempProjectile = new Nucleus(projectile.getChargeNumber(),
				projectile.getMassNumber(), ExProjectile);
		Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile,
				target.calculateInteractionEnergy(interactionLayer, beam,
						beamEnergy), thetaDegrees, 0.0);
		final Quantity[] p = new Quantity[2];
		final Length rhoErr = Length.lengthOf(rho.multiply(rho
				.getRelativeError()));
		//need to add target energy loss back to brho for accurate value
		for (int i = 0; i < 2; i++) {
			final Quantity qbr = rho.add(rhoErr.multiply(i)).multiply(
					projectileCharge).multiply(magneticField);
			Energy KE = Reaction.getKE(tempProjectile, qbr);
			KE = target.calculateInitialProjectileEnergy(interactionLayer,
					tempProjectile, KE, thetaDegrees);
			p[i] = Reaction.rigidityToMomentum(Reaction.getQBrho(
					tempProjectile, KE));
		}
		Quantity momentum = Quantity.valueOf(p[0].doubleValue(), p[1].subtract(
				p[0]).doubleValue(), p[0].getSystemUnit());
		return rxn.getEx4(momentum);
	}

	public Nucleus getBeam() {
		return beam;
	}

	public Energy getBeamEnergy() {
		return beamEnergy;
	}

	public Target getTarget() {
		return target;
	}

	public int getInteractionLayer() {
		return interactionLayer;
	}

	public Nucleus getTargetNuclide() {
		return targetNuclide;
	}

	public Nucleus getProjectile() {
		return projectile;
	}

	public Nucleus getResidual() throws NuclearException {
		return Reaction.getResidual(targetNuclide, beam, projectile,
				Energy.ZERO);
	}

	public int getQ() {
		return (int) Math.round(projectileCharge.divide(
				ElectricCharge.ELEMENTARY).doubleValue());
	}

	public Angle getTheta() {
		return thetaDegrees;
	}

	public MagneticFluxDensity getMagneticField() {
		return magneticField;
	}

}