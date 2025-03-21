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
package dwvisser.nuclear;
import dwvisser.math.UncertainNumber;

/**
 * Class for calculating rutherford scattering cross sections
 * in barns.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 */
public class Rutherford {

	public static final double FM2_TO_BARNS = 0.01;
	
	//constants from July 2002 Particle Physics Booklet; Particle Data Group
	public static final UncertainNumber ALPHA=new UncertainNumber(7.297352533e-3,2.7e-11);
	public static final UncertainNumber HBAR_C=new UncertainNumber(197.3269602,7.7e-6);//MeV-fm
	public static final UncertainNumber E2 = ALPHA.times(HBAR_C); //MeV-fm

	private Nucleus beam, target;
	private double ebeam, labangle;
	private double xsec;

	/**
	 * Define a rutherford scattering scenario.
	 * 
	 * @param beam nuclear species
	 * @param target nuclear species
	 * @param ebeam in MeV
	 * @param labangle in degrees
	 * @throws KinematicsException for unphysical angles
	 */
	public Rutherford (
		Nucleus beam,
		Nucleus target,
		double ebeam,
		double labangle) throws KinematicsException {
		this.beam = beam;
		this.target = target;
		this.ebeam = ebeam;
		this.labangle = labangle;
		calculate();
	}

	private void calculate() throws KinematicsException {
		double mbeam = beam.getMass().value; //MeV
		double mtarget = target.getMass().value;
		try {
			Reaction r = new Reaction(target, beam, beam, ebeam, labangle, 0.0);
			double ebeam_cm =
				Math.sqrt(
					mbeam * mbeam
						+ mtarget * mtarget
						+ 2 * mtarget * (ebeam + mbeam))
					- (mbeam + mtarget);
			double cmangle=r.getCMAngleProjectile(0);
			xsec =Math.pow(beam.Z * target.Z * E2.value / (4.0 * ebeam_cm),2.0)
					* Math.pow(Math.sin(Math.toRadians(0.5*cmangle)), -4.0);
			xsec *= FM2_TO_BARNS;
		} catch (NuclearException e){
			System.err.println("Shouldn't be here.");
			e.printStackTrace();
		}
	}
	
	/**
	 * @return Rutherford differential cross-section in barns/sr
	 */
	public double getXsection(){
		return xsec;
	}
	
	/**
	 * Change the beam energy.
	 * 
	 * @param ebeam in MeV
	 * @throws KinematicsException if stored lab angle become unphysical
	 */
	public void setEbeam(double ebeam) throws KinematicsException  {
		this.ebeam=ebeam;
		calculate();
	}
	
	/**
	 * Change the lab angle.
	 * 
	 * @param angle in degrees
	 * @throws KinematicsException if the angle is unphysical
	 */
	public void setLabAngle(double angle) throws KinematicsException  {
		this.labangle=angle;
		calculate();
	}
}
