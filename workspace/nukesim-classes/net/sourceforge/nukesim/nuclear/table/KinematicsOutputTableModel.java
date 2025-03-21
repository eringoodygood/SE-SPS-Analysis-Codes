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
package net.sourceforge.nukesim.nuclear.table;

import jade.physics.Angle;
import jade.physics.Energy;
import jade.physics.Quantity;
import jade.physics.Scalar;
import jade.units.Unit;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.nukesim.nuclear.EnergyLoss;
import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.Nucleus;
import net.sourceforge.nukesim.nuclear.NukeUnits;
import net.sourceforge.nukesim.nuclear.Reaction;
import net.sourceforge.nukesim.nuclear.Solid;

/**
 * Data model for <code>KinematicsOutputTable</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser </a>
 * @version 1.0
 */
public final class KinematicsOutputTableModel extends AbstractTableModel
		implements ReactionTableClient, NukeUnits {
	private final String[] headers = { "T(1)", "Ex(4)", "Lab \u03b8(3)",
			"CM \u03b8(3)", "T(3)", "Lab \u03b8(4)", "T(4)", "Jac(3)", "k(3)",
			"z", "QB\u03c1(3)", };

	private final Class[] columnClasses = { String.class, String.class,
			String.class, String.class, String.class, String.class,
			String.class, String.class, String.class, String.class,
			String.class };

	private final Unit[] columnUnits = { MeV, keV, deg, deg, MeV, deg, MeV,
			null, null, null, eTm };

	private Object[][] data = new Object[1][headers.length];

	private Energy[] beamEnergies, residExcite;

	private Angle[] labAngles;

	private Nucleus target, beam, projectile;

	private EnergyLoss energyLoss;

	public KinematicsOutputTableModel(ReactionTable rt, Energy[] beamEnergies,
			Energy[] residExcite, Angle[] labAngles)
			throws KinematicsException, NuclearException {
		super();
		this.beamEnergies = beamEnergies;
		this.residExcite = residExcite;
		this.labAngles = labAngles;
		//        new EnergyLoss();//called to initialize energy loss routines
		rt.setReactionTableClient(this);
		iterateTable();
		setValueAt(data[0][0], 0, 0);
	}

	/**
	 * Sets reaction along with target thickness in ug/cm^2.
	 */
	public void setReaction(Nucleus target, Nucleus beam, Nucleus projectile)
			throws KinematicsException, NuclearException {
		this.target = target;
		this.beam = beam;
		this.projectile = projectile;
		if (energyLoss != null) {
			setTargetThickness(energyLoss.getAbsorber().getThickness());
		}
		iterateTable();
	}

	public void setTargetThickness(Quantity thickness)
			throws KinematicsException, NuclearException {
		try {
			if (!thickness.isPossiblyZero()) {
				energyLoss = new EnergyLoss(new Solid(thickness,
						target.getElementSymbol()));
			} else {
				energyLoss = null;
			}
		} catch (NuclearException ne) {
			System.err.println(getClass().getName() + ".setReaction(): " + ne);
		}
		iterateTable();
	}

	public void setBeamEnergies(Energy[] be) throws KinematicsException,
			NuclearException {
		beamEnergies = be;
		iterateTable();
	}

	public void setResidualExcitations(Energy[] re) throws KinematicsException,
			NuclearException {
		residExcite = re;
		iterateTable();
	}

	public void setLabAngles(Angle[] la) throws KinematicsException,
			NuclearException {
		labAngles = la;
		iterateTable();
	}

	public int getRowCount() {
		return data.length;
	}

	public int getColumnCount() {
		return data[0].length;
	}

	public Class getColumnClass(int c) {
		return columnClasses[c];
	}

	public String getColumnName(int c) {
		final Unit units=columnUnits[c];
		return (units == null) ? 
		headers[c] : headers[c]+" ["+units.toString()+"]";
	}

	public boolean isCellEditable(int r, int c) {
		return false;
	}

	public Object getValueAt(int r, int c) {
		return data[r][c];
	}

	public void setValueAt(Object value, int r, int c) {
		data[r][c] = (value==null || !(value instanceof Quantity)) ? 
				value : cellText((Quantity) value, c);
	}

	void iterateTable() throws KinematicsException, NuclearException {
		Energy[] backBeamEnergy = new Energy[beamEnergies.length];
		Quantity qbr_front, qbr_back;
		/* reactions[0][i] = front of target; reactions[1][i]=back of target */
		Reaction[][] reactions = new Reaction[2][beamEnergies.length
				* residExcite.length * labAngles.length];
		int numRows = 0;
		int counter = 0;
		for (int i = 0; i < beamEnergies.length; i++) {
			final Energy bEnergy = Energy.energyOf(beamEnergies[i]);
			if (energyLoss != null) {
				backBeamEnergy[i] = Energy.energyOf(beamEnergies[i]
						.subtract(energyLoss.getEnergyLoss(beam, bEnergy)));
			} else {
				backBeamEnergy[i] = bEnergy;
			}
			for (int j = 0; j < residExcite.length; j++) {
				for (int k = 0; k < labAngles.length; k++) {
					reactions[0][counter] = new Reaction(target, beam,
							projectile, Energy.energyOf(beamEnergies[i]), Angle
									.angleOf(labAngles[k]), Energy
									.energyOf(residExcite[j]));
					reactions[0][counter].setVaryOption(Reaction.VaryOption.ALL);
					numRows += reactions[0][counter].getAngleDegeneracy();
					if (energyLoss != null) {
						reactions[1][counter] = new Reaction(target, beam,
								projectile, Energy.energyOf(backBeamEnergy[i]),
								Angle.angleOf(labAngles[k]), Energy
										.energyOf(residExcite[j]));
						numRows += reactions[0][counter].getAngleDegeneracy();
						reactions[1][counter].setVaryOption(Reaction.VaryOption.ALL);
					}
					counter++;
				}
			}
		}
		data = new Object[numRows][headers.length];
		counter = 0;
		int row = 0;
		for (int i = 0; i < beamEnergies.length; i++) {
			for (int j = 0; j < residExcite.length; j++) {
				for (int k = 0; k < labAngles.length; k++) {
					for (int l = 0; l < reactions[0][counter]
							.getAngleDegeneracy(); l++) {
						setValueAt(beamEnergies[i], row, 0);
						setValueAt(residExcite[j], row, 1);
						setValueAt(labAngles[k], row, 2);
						setValueAt(reactions[0][counter]
								.getCMAngleProjectile(l), row, 3);
						Energy Tproj = reactions[0][counter]
								.getLabEnergyProjectile(l);
						if (energyLoss != null) {
							Tproj = Energy.energyOf(Tproj.subtract(energyLoss
									.getEnergyLoss(projectile, Tproj, Angle
											.angleOf(labAngles[k]))));
						}
						setValueAt(Tproj, row, 4);
						setValueAt(
								reactions[0][counter].getLabAngleResidual(l),
								row, 5);
						setValueAt(reactions[0][counter]
								.getLabEnergyResidual(l), row, 6);
						setValueAt(reactions[0][counter]
								.getJacobianProjectile(l), row, 7);
						Scalar kP = reactions[0][counter].getFocusParameter(l);
						setValueAt(kP, row, 8);
						setValueAt(kP.abs().multiply(57.01).negate().add(
								Scalar.valueOf(50.12)), row, 9);
						if (energyLoss != null) {
							qbr_front = Reaction.getQBrho(projectile, Tproj);
						} else {
							qbr_front = reactions[0][counter].getQBrho(l);
						}
						setValueAt(qbr_front, row, 10);
						row++;
						if (energyLoss != null) {
							setValueAt(backBeamEnergy[i], row, 0);
							setValueAt(residExcite[j], row, 1);
							setValueAt(labAngles[k], row, 2);
							setValueAt(reactions[1][counter]
									.getCMAngleProjectile(l), row, 3);
							Tproj = reactions[1][counter]
									.getLabEnergyProjectile(l);
							setValueAt(Tproj, row, 4);
							setValueAt(reactions[1][counter]
									.getLabAngleResidual(l), row, 5);
							setValueAt(reactions[1][counter]
									.getLabEnergyResidual(l), row, 6);
							setValueAt(reactions[1][counter]
									.getJacobianProjectile(l), row, 7);
							kP = reactions[1][counter].getFocusParameter(l);
							setValueAt(kP, row, 8);
							setValueAt(kP.abs().multiply(57.01).negate().add(
									Scalar.valueOf(50.12)),
							//new Double(50.12 - 57.01 * Math.abs(kP)),
									row, 9);
							qbr_back = reactions[1][counter].getQBrho(l);
							setValueAt(qbr_back, row, 10);
							row++;
						}
					}
					counter++;
				}
			}
		}
		fireTableDataChanged();
	}

	private String cellText(Quantity value, int column) {
		final Unit unit = columnUnits[column];
		final StringBuffer rval=new StringBuffer();
		if (unit != null) {
			final int unitLength = unit.toString().length();
			if (value.approxEquals(value.multiply(0.0))){
				rval.append(0);
			} else {
				rval.append(value.toText(unit));
				rval.delete(rval.length() - unitLength,rval.length());			
			}
		} else {
			rval.append(value.toString());
		}
		return Double.toString(Double.parseDouble(rval.toString()));
	}
}