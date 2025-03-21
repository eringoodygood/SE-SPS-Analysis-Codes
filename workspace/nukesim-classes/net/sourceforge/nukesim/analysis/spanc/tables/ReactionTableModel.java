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
package net.sourceforge.nukesim.analysis.spanc.tables;
import jade.physics.Mass;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import net.sourceforge.nukesim.analysis.spanc.SpancReaction;
import net.sourceforge.nukesim.analysis.spanc.Target;
import net.sourceforge.nukesim.math.QuantityUtilities;
import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.Nucleus;
import net.sourceforge.nukesim.nuclear.NukeUnits;

/**
 * Data model for <code>ReactionTable</code>.
 *
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class ReactionTableModel extends DefaultTableModel implements NukeUnits {
	static String[] headers =
		{
			"Reaction",
			"Beam",
			"Energy [MeV]",
			"B [kG]",
			"Target",
			"Interaction Layer",
			"Target Nuclide",
			"Projectile",
			"Residual",
			"Q [e]",
			"Theta [\u00b0]" };
	Class[] columnClasses =
		{
			Integer.class,
			String.class,
			Double.class,
			Double.class,
			Target.class,
			Integer.class,
			String.class,
			String.class,
			String.class,
			Boolean.class,
			Integer.class,
			Double.class };

	public ReactionTableModel() throws KinematicsException {
		super(headers, 0);
	}

	static java.text.DecimalFormat df = new java.text.DecimalFormat("0.#");
	public void addRow(SpancReaction sr) throws NuclearException {
		Vector temp = new Vector(headers.length);
		temp.addElement(new Integer(getRowCount()));
		String beamString = sr.getBeam().toString();
		if (sr.getBeamUncertain()) {
			beamString += " "
				+ (char) 0xb1
				+ " "
				+ nukeMassUnc(sr.getBeam());
		}
		temp.addElement(beamString);
		temp.addElement(QuantityUtilities.noUnits(sr.getBeamEnergy(),MeV));
		temp.addElement(QuantityUtilities.noUnits(sr.getMagneticField(),tesla));
		temp.addElement(sr.getTarget().getName());
		temp.addElement(new Integer(sr.getInteractionLayer()));
		String targetString = sr.getTargetNuclide().toString();
		if (sr.getTargetUncertain()) {
			targetString += " "
				+ (char) 0xb1
				+ " " + nukeMassUnc(sr.getTargetNuclide());
		}
		temp.addElement(targetString);
		String projectileString = sr.getProjectile().toString();
		if (sr.getProjectileUncertain()) {
			projectileString += " "
				+ (char) 0xb1
				+ " "
				+ nukeMassUnc(sr.getProjectile());
		}
		temp.addElement(projectileString);
		String residualString = sr.getResidual().toString();
		if (sr.getResidualUncertain()) {
			residualString += " "
				+ (char) 0xb1
				+ " "
				+ nukeMassUnc(sr.getResidual());
		}
		temp.addElement(residualString);
		temp.addElement(new Integer(sr.getQ()));
		temp.addElement(QuantityUtilities.noUnits(sr.getTheta(),deg));
		addRow(temp);
	}
	
	private String nukeMassUnc(Nucleus nucleus){
		final Mass mass=nucleus.getMass();
		final double errkeV=mass.multiply(mass.getRelativeError()).doubleValue(keV);
		return df.format(errkeV)+" keV";
	}

	void refreshData() {
		while (getRowCount() > 0) {
			removeRow(0);
		}
		final SpancReaction [] rxns = SpancReaction.getAllReactions();
		try {
			for (int i = 0; i < rxns.length; i++) {
				addRow(rxns[i]);
			}
		} catch (NuclearException ne){
			System.err.println("Shouldn't be here.");
			ne.printStackTrace();
		}
	}
}