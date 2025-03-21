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
package net.sourceforge.nukesim.analysis.spanc;
import jade.JADE;
import jade.physics.Angle;
import jade.physics.Energy;
import jade.physics.Quantity;
import jade.physics.models.RelativisticModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;

import net.sourceforge.nukesim.nuclear.Absorber;
import net.sourceforge.nukesim.nuclear.EnergyLoss;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.Nucleus;
import net.sourceforge.nukesim.nuclear.NukeUnits;
import net.sourceforge.nukesim.nuclear.Solid;

/**
 * This class represents a target in a splitpole experiment, possibly
 * containing more than one layer. It handles target energy loss 
 * calculations. Each target is uniquely identified by a name.
 *
 * @author  Dale Visser
 * @version 1.2
 * @since 1.0 (15 Dec 2001)
 */
public class Target implements Serializable, NukeUnits {

	private List layers = new Vector(1, 1);
	private List fullLosses = new Vector(1, 1);
	private List halfLosses = new Vector(1, 1);

	private String name;

	private static Hashtable targets = new Hashtable();
	private static DefaultListModel dlm_targets = new DefaultListModel();
	private static DefaultComboBoxModel dcbm_targets =
		new DefaultComboBoxModel();

	/** Creates new Target */
	public Target(String name) {
		this.name = name;
		addTargetToLists();
	}

	private void addTargetToLists() {
		targets.put(name, this);
		dlm_targets.addElement(this.name);
		dcbm_targets.addElement(this.name);
	}

	static public void removeTarget(Target t) {
		targets.remove(t.getName());
		dlm_targets.removeElement(t.name);
		dcbm_targets.removeElement(t.name);
	}

	static public void refreshData(Collection retrievedTargets) {
		Iterator iter = retrievedTargets.iterator();
		while (iter.hasNext()) {
			Target targ = (Target) iter.next();
			targets.put(targ.getName(), targ);
			dlm_targets.addElement(targ.name);
			dcbm_targets.addElement(targ.name);
		}
	}

	static public void removeAllTargets() {
		Iterator it_targ = targets.values().iterator();
		while (it_targ.hasNext()) {
			it_targ.next();
			it_targ.remove();
		}
		dlm_targets.removeAllElements();
		dcbm_targets.removeAllElements();
	}

	static public Target getTarget(String name) {
		return (Target) targets.get(name);
	}

	public void addLayer(Solid layer) {
		layers.add(layer);
		fullLosses.add(new EnergyLoss(layer));
		Absorber half = layer.getNewInstance(0.5);
		halfLosses.add(new EnergyLoss(half));
	}

	void removeLayer(int index) {
		layers.remove(index);
	}

	Energy calculateInteractionEnergy(
		int interaction_layer,
		Nucleus beam,
		Energy Ebeam) {
		Energy rval = Ebeam;
		for (int i = 0; i < interaction_layer; i++) {
			rval = Energy.energyOf(
					rval.subtract(getFullLoss(i).getEnergyLoss(beam, rval)));
		}
		rval = Energy.energyOf(
				rval.subtract(getHalfLoss(interaction_layer).getEnergyLoss(beam, rval)));
		return rval;
	}

	private EnergyLoss getFullLoss(int layer) {
		return (EnergyLoss) fullLosses.get(layer);
	}

	private EnergyLoss getHalfLoss(int layer) {
		return (EnergyLoss) halfLosses.get(layer);
	}

	public int getNumberOfLayers() {
		return layers.size();
	}

	public Solid getLayer(int index) {
		return (Solid) layers.get(index);
	}

	static public DefaultListModel getTargetList() {
		return dlm_targets;
	}

	static public DefaultComboBoxModel getComboModel() {
		return dcbm_targets;
	}

	Energy calculateProjectileEnergy(
		int interaction_layer,
		Nucleus projectile,
		Energy Einit,
		Angle thetaRadians) {
		Energy rval = Einit;
		rval = Energy.energyOf(rval.subtract(getHalfLoss(interaction_layer).getEnergyLoss(
				projectile,
				Einit,
				thetaRadians)));
		for (int i = interaction_layer + 1; i < layers.size(); i++) {
			rval = Energy.energyOf(rval.subtract(
					getFullLoss(i).getEnergyLoss(projectile, rval, thetaRadians)));
		}
		return rval;
	}

	Energy calculateInitialProjectileEnergy(
		int interaction_layer,
		Nucleus projectile,
		Energy Efinal,
		Angle thetaRadians) {
		Energy rval = Efinal;
		for (int i = layers.size() - 1; i > interaction_layer; i--) {
			rval =
				getFullLoss(i).reverseEnergyLoss(
					projectile,
					rval,
					thetaRadians);
		}
		rval =
			getHalfLoss(interaction_layer).reverseEnergyLoss(
				projectile,
				rval,
				thetaRadians);
		return rval;
	}

	public String getName() {
		return name;
	}

	public DefaultComboBoxModel getLayerNumberComboModel() {
		DefaultComboBoxModel rval = new DefaultComboBoxModel();
		for (int i = 0; i < layers.size(); i++) {
			rval.addElement(new Integer(i));
		}
		return rval;
	}

	public DefaultComboBoxModel getLayerNuclideComboModel(int layerIndex) {
		DefaultComboBoxModel rval = new DefaultComboBoxModel();
		int[] Z = getLayer(layerIndex).getElements();
		for (int i = 0; i < Z.length; i++) {
			List possible = Nucleus.getIsotopes(Z[i]);
			for (int j = 0; j < possible.size(); j++) {
				rval.addElement(possible.get(j));
			}
		}
		return rval;
	}

	static public Collection getTargetCollection() {
		return targets.values();
	}

	public String toString() {
		String rval = "Target: " + name + "\n";
		for (int i = 0; i < layers.size(); i++) {
			rval += "Layer " + i + ": Specification '";
			Solid l = getLayer(i);
			rval += l.getText() + "' " + l.getThickness() + " ug/cm^2\n";
		}
		return rval;
	}

	static public void main(String[] args) {
		JADE.initialize();
		RelativisticModel.select();
		Target t = new Target("test");
		try {
			t.addLayer(new Solid("C 1", Quantity.valueOf(100,µg_per_cmsq)));
			//t.addLayer(new Solid("Si 1 O 2",170));
			Nucleus proj = new Nucleus(2, 6);
			int layer = 0;
			Energy Einit = Energy.energyOf(Quantity.valueOf(31.6,MeV));
			Angle thetaRad = Angle.angleOf(Quantity.valueOf(7.5,deg));
			Energy Efinal =
				t.calculateProjectileEnergy(layer, proj, Einit, thetaRad);
			System.out.println("Einit = " + Einit.toText(MeV) + 
					" -> Efinal = " + Efinal.toText(MeV));
			Einit =
				t.calculateInitialProjectileEnergy(
					layer,
					proj,
					Efinal,
					thetaRad);
			System.out.println("Efinal = " + Efinal.toText(MeV) + " -> Einit = " + 
					Einit.toText(MeV));
		} catch (NuclearException ne) {
			System.err.println(ne);
		}
	}

}
