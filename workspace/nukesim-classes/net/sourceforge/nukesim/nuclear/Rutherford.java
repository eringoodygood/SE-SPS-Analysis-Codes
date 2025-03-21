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

import jade.physics.Angle;
import jade.physics.Area;
import jade.physics.Constants;
import jade.physics.Energy;
import jade.physics.Mass;
import jade.physics.Quantity;
import jade.physics.Scalar;

/**
 * Class for calculating rutherford scattering cross sections in barns.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser </a>
 */
public class Rutherford {

	public static final Quantity HBAR_C = Constants.hBar.multiply(Constants.c);

	public static final Quantity E2 = (Quantity) Constants.ePlus.pow(2);

	private Nucleus beam, target;

	private Energy ebeam;

	private Angle labangle;

	private Area xsec;

	/**
	 * Define a rutherford scattering scenario.
	 * 
	 * @param beam
	 *            nuclear species
	 * @param target
	 *            nuclear species
	 * @param ebeam
	 *            in MeV
	 * @param labangle
	 *            in degrees
	 * @throws KinematicsException
	 *             for unphysical angles
	 */
	public Rutherford(Nucleus beam, Nucleus target, Energy ebeam, Angle labangle)
			throws KinematicsException {
		this.beam = beam;
		this.target = target;
		this.ebeam = ebeam;
		this.labangle = labangle;
		calculate();
	}

	private void calculate() throws KinematicsException {
		Mass mbeam = beam.getMass(); //MeV
		Mass mtarget = target.getMass();
		try {
			Reaction reaction = new Reaction(target, beam, beam, ebeam,
					labangle, 0.0);
			final Quantity mbeamsq = (Quantity) mbeam.pow(2);
			final Quantity mtargsq = (Quantity) mtarget.pow(2);
			final Quantity relEbeam = ebeam.add(mbeam);
			final Quantity mTotal = mbeam.add(mtarget);
			final Quantity plusTerm = mtarget.multiply(2).multiply(relEbeam);
			final Quantity eBeamCMsq = mbeamsq.add(mtargsq).add(plusTerm)
					.subtract(mTotal);
			final Angle cmangle = reaction.getCMAngleProjectile(0);
			final int zProduct = beam.getChargeNumber()
					* target.getChargeNumber();
			final Area xsecFactor1 = Area.areaOf(((Quantity) (E2
					.multiply(zProduct).divide(4)).pow(2)).divide(eBeamCMsq));
			final Scalar xsecFactor2 = Scalar.scalarOf((Quantity) Angle
					.angleOf(cmangle.divide(2)).sine().pow(-4));
			xsec = Area.areaOf(xsecFactor1.multiply(xsecFactor2));
		} catch (NuclearException e) {
			System.err.println("Shouldn't be here.");
			e.printStackTrace();
		}
	}

	/**
	 * @return Rutherford differential cross-section in barns/sr
	 */
	public Area getXsection() {
		return xsec;
	}

	/**
	 * Change the beam energy.
	 * 
	 * @param ebeam
	 *            in MeV
	 * @throws KinematicsException
	 *             if stored lab angle become unphysical
	 */
	public void setEbeam(Energy energy) throws KinematicsException {
		ebeam = energy;
		calculate();
	}

	/**
	 * Change the lab angle.
	 * 
	 * @param angle
	 *            in degrees
	 * @throws KinematicsException
	 *             if the angle is unphysical
	 */
	public void setLabAngle(Angle angle) throws KinematicsException {
		labangle = angle;
		calculate();
	}
}