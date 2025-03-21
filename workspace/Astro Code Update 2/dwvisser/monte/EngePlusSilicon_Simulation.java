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
 * EngeYLSA_Simulation.java
 *
 * Created on July 2, 2001, 12:30 PM
 */

package dwvisser.monte;
import dwvisser.nuclear.*;
import java.io.*;
import java.util.Random;
import dwvisser.math.UncertainNumber;
import java.text.DecimalFormat;

/**
 * This class will produce a Monte Carlo simulation of the in-flight
 * decay of a nucleus in an excited state produced and (presumably)
 * tagged by the Enge SplitPole.  The code also determines if and where
 * the decay product interacts with YLSA.
 *
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class EngePlusSilicon_Simulation extends Object {
	static final double C_MM_PER_NSEC = 299.792458;
	static final double DEAD_LAYER_THICKNESS = 0.2; //um

	//Fields specified in constructor
	Nucleus target, beam, projectile, decay;
	double xtarg; //thickness in ug/cm^2
	double Ebeam; //beam kinetic energy in MeV
	double ExResid; //in MeV
	double ExUltimate; //in MeV
	double theta; //for spectrometer, in degrees
	String outFile;

	//Fields set by calculation
	Nucleus residual, ultimate;
	double thetaR; //for spectrometer, in radians
	FileWriter outEvn, outCounts, outDescription, outAngles;

	/** Creates new EngeYLSA_Simulation */
	public EngePlusSilicon_Simulation() {
		double[] ExResidValues = { 5.673 };
		/*** Initial values put here ***/
		int hitsWanted = 100 * 16;
		//hits wanted ~ (fractional error wanted per strip)^-2
		/**
		 * numEvents will be expanded as needed so that hitsWanted
		 * will be exceeded by approximatley 20%
		 */
		int numEvents = (int) Math.round(hitsWanted * 1.2);
		//total events to simulate
		double z0 = 167.1; //distance in mm to vertex of array
		double thetaYLSA = 55; //angle of array detectors
		try {
			target = new Nucleus(9, 19);
			beam = new Nucleus(2, 3);
			projectile = new Nucleus(2, 4);
			decay = new Nucleus(1, 1);
		} catch (NuclearException ne) {
			System.err.println(ne);
		}
		Ebeam = 5; //beam kinetic energy in MeV
		xtarg = 80; //thickness in ug/cm^2
		//40 ug 12C + 4 ug Ne
		String starg = "Ca 1 F 2"; //material specification for target
		theta = 0; //for spectrometer, in degrees
		double thetaAcceptance = 0.080;
		//choose "theta" of projectile <= +/- this
		double phiAcceptance = 0.040; //choose "phi" of projectile <= +/- this
		double randomAcceptanceMax =
			Math.sqrt(
				Math.pow(thetaAcceptance, 2) + Math.pow(phiAcceptance, 2));
		ExUltimate = 0; //in MeV
		int angularMomentum = 0;
		//assumed orbital angular momentum for the decay
		double AlThickness = 0;
		//thickness of degrading foil in front of detector in mils
		//at this time, simply added to dead layer thickness, assuming we would
		//put the foils parallel to the detectors
		double hit_threshold = 0.0;
		//energy deposited in MeV to be considered a hit
		int[] counts = new int[90];
		double[] Emin = new double[90];
		double[] Emax = new double[90];
		boolean arrayForward = false;

		/*** Calculation of some fields based on initial values ***/
		for (int rEx = 0; rEx < ExResidValues.length; rEx++) {
			ExResid = ExResidValues[rEx]; //in MeV
			try {
				residual =
					new Nucleus(
						target.Z + beam.Z - projectile.Z,
						target.A + beam.A - projectile.A,
						ExResid);
				ultimate =
					new Nucleus(
						residual.Z - decay.Z,
						residual.A - decay.A,
						ExUltimate);
			} catch (NuclearException ne) {
				System.err.println(ne);
			}

			String whetherForward = "_back";
			if (arrayForward)
				whetherForward = "_front";
			String outFileRoot =
				"d:/simulations/"
					+ (int) Math.round(ExResid * 1000)
					+ "_"
					+ residual
					+ "_"
					+ (int) Math.round(ExUltimate * 1000)
					+ "_"
					+ ultimate
					+ "_L"
					+ angularMomentum
					+ whetherForward
					+ "_"
					+ AlThickness
					+ "mil"
					+ (int) Math.round(Ebeam)
					+ "MeV"
					+ "_"
					+ theta
					+ "deg";
			thetaR = Math.toRadians(theta);
			System.out.println(
				target
					+ "("
					+ beam
					+ ","
					+ projectile
					+ ") -> "
					+ ultimate
					+ "+"
					+ decay);
			System.out.println("File root: " + outFileRoot);
			try {
				outDescription = new FileWriter(outFileRoot + ".txt");
				outEvn = new FileWriter(outFileRoot + ".evn");
				outCounts = new FileWriter(outFileRoot + ".sum");
				outAngles = new FileWriter(outFileRoot + ".ang");
			} catch (IOException ioe) {
				System.err.println(ioe);
			}

			Random random = new Random();
			double Mresid = residual.getMass().value;
			double Mdecay = decay.getMass().value;
			double Mdecay2 = Mdecay * Mdecay;
			double Multimate = ultimate.getMass().value;
			double Multimate2 = Multimate * Multimate;
			double PcmDecay =
				Math.sqrt(
					Mresid * Mresid
						- 2 * (Mdecay2 + Multimate2)
						+ Math.pow((Mdecay2 - Multimate2) / Mresid, 2))
					/ 2;
			//MeV/c
			double EcmDecay = Math.sqrt(PcmDecay * PcmDecay + Mdecay * Mdecay);
			System.out.println(
				"Ex = "
					+ ExResid
					+ " MeV, CM K.E. for detected decay product = "
					+ (EcmDecay - Mdecay)
					+ " MeV");
			MicronDetector md =
				new MicronDetector(z0, Math.toRadians(thetaYLSA));
			Solid deadLayer;
			EnergyLoss deadLayerLoss;
			try {
				outDescription.write(
					target
						+ "("
						+ beam
						+ ","
						+ projectile
						+ ")"
						+ residual
						+ "("
						+ decay
						+ ")"
						+ ultimate
						+ "\n");
				outDescription.write(
					"Ex(residual " + residual + ") = " + ExResid + " MeV\n");
				outDescription.write(
					"Ex(final " + ultimate + ") = " + ExUltimate + " MeV\n");
				outDescription.write(
					"Theta(projectile "
						+ projectile
						+ ") = "
						+ theta
						+ " degrees\n");
				outDescription.write(
					"Target Thickness = " + xtarg + " ug/cm^2\n");
				outDescription.write("l = " + angularMomentum + " decay\n");
				deadLayer =
					new Solid(getThicknessAl(AlThickness), Absorber.CM, "Al");
				deadLayerLoss = new EnergyLoss(deadLayer);

				double[] p_CM = new double[4];
				double[] p_lab = new double[4];
				/*** work begins here ***/
				Reaction reaction;
				Boost labBoost;
				Direction directionCM;
				int angleBinning = 60;
				double divFactor = 180.0 / angleBinning;
				int[] thetaCM = new int[angleBinning];
				int[] thetaLab = new int[angleBinning];
				int hits = 0;
				outEvn.write(
					"hit\tprojTheta\tprojPhi\tCMtheta\tCMphi\tlabTheta\tlabPhi\tinc.\tEdep\ttof\tdet\tstrip\n");
				int _i = 0;
				/*for (int i=0; i < numEvents; i++)*/
				do {
					_i++;
					double depth = random.nextDouble() * xtarg;
					Solid targetMatter = new Solid(starg, depth);
					EnergyLoss targetLossCalc = new EnergyLoss(targetMatter);
					double targetLoss =
						0.001 * targetLossCalc.getEnergyLoss(beam, Ebeam);
					double Tbeam = Ebeam - targetLoss;

					//Direction _randomProjDir = new Direction(0,0);

					//randomly select a direction into the spectrometer
					//assuming its slits are centered on the beam axis
					Direction projDir = null;
					boolean directionAccepted = false;
					while (!directionAccepted) {
						Direction _randomProjDir =
							Direction.getRandomDirection(
								0,
								randomAcceptanceMax);
						//"theta" for theta acceptance actually arcTan(x/z) or tan("theta")=
						//tan(theta)cos(phi)
						double tempTanTheta =
							Math.abs(
								Math.tan(_randomProjDir.getTheta())
									* Math.cos(_randomProjDir.getPhi()));
						double tempTanPhi =
							Math.abs(
								Math.tan(_randomProjDir.getTheta())
									* Math.sin(_randomProjDir.getPhi()));
						directionAccepted =
							(tempTanTheta <= Math.tan(thetaAcceptance))
								&& (tempTanPhi <= Math.tan(phiAcceptance));
						if (directionAccepted) {
							//rotate random direction to be about actual spectrometer location
							//theta = angle the spectrometer is set at
							projDir =
								_randomProjDir.rotateY(Math.toRadians(theta));
						}
					}

					//create reaction for theta from projDir
					reaction =
						new Reaction(
							target,
							beam,
							projectile,
							Tbeam,
							projDir.getThetaDegrees(),
							ExResid);
					//determine direction residual nucleus is moving
					//theta comes from reaction object, (taking absolute value, as the minus
					//sign usually returned causes the new phi to be calculated wrong)
					//phi is opposite the phi of the projectile
					Direction residDir =
						new Direction(
							Math.toRadians(
								Math.abs(reaction.getLabAngleResidual(0))),
							Math.PI + projDir.getPhi());
					//create boost object to convert CM 4-momentum to lab 4-momentum
					labBoost =
						new Boost(reaction.getLabBetaResidual(0), residDir);
					if (!arrayForward)
						labBoost = Boost.inverseBoost(labBoost);
					//consider l=angularMomentum case:
					//The assumption is theta = 0 degrees exactly for the projectile
					//(not exactly true, of course) so that the correlation function
					//is just a Legendre Polynomial (corresponding to only M=0
					//being populated.
					directionCM = Direction.getRandomDirection(angularMomentum);
					//copy components of momentum into 4-vector
					p_CM[0] = EcmDecay;
					System.arraycopy(
						directionCM.get3vector(PcmDecay),
						0,
						p_CM,
						1,
						3);
					p_lab = labBoost.transformVector(p_CM);
					//extract direction of decay product in lab
					Direction directionLab =
						new Direction(p_lab[1], p_lab[2], p_lab[3]);
					//???rotating on this next line was the wrong thing to do, I think
					/*directionLab = directionLab.rotateY(
					Math.toRadians(reaction.getLabAngleResidual(0)));*/
					Direction queryDir = MicronDetector.changePhi(directionLab);
					boolean hit = md.isHit(queryDir);
					int strip, det;
					double Edep, tof;
					if (arrayForward) {
						directionLab =
							new Direction(
								Math.PI - directionLab.getTheta(),
								directionLab.getPhi());
						directionCM =
							new Direction(
								Math.PI - directionCM.getTheta(),
								directionCM.getPhi());
					}
					double thetaDlab = directionLab.getThetaDegrees();
					double thetaDcm = directionCM.getThetaDegrees();
					thetaCM[(int) Math.floor(thetaDcm / divFactor)]++;
					thetaLab[(int) Math.floor(thetaDlab / divFactor)]++;
					if (hit) {
						strip = md.getStrip();
						det = MicronDetector.getDetector(directionLab);
						int bin = strip + det * 16;
						double incidence = md.getIncidence();
						double Tinit = p_lab[0] - Mdecay;
						double Tflight =
							Tinit
								- 0.001
									* targetLossCalc.getEnergyLoss(
										decay,
										Tinit,
										Math.PI - directionLab.getTheta());
						tof =
							md.getDistance()
								/ (Reaction.getBeta(decay, Tflight)
									* C_MM_PER_NSEC);
						Edep =
							Tflight
								- 0.001
									* deadLayerLoss.getEnergyLoss(
										decay,
										Tflight,
										Math.acos(1 / incidence));
						boolean energyHit = Edep >= hit_threshold;
						if (energyHit) {
							hits++;
							counts[bin]++;
							if (Emin[bin] == 0.0 || Emin[bin] > Edep)
								Emin[bin] = Edep;
							if (Emax[bin] == 0.0 || Emax[bin] < Edep)
								Emax[bin] = Edep;
							//time-of-flight in nsec
							outEvn.write(
								"1\t"
									+ round(projDir.getThetaDegrees())
									+ "\t"
									+ round(projDir.getPhiDegrees())
									+ "\t"
									+ round(thetaDcm)
									+ "\t"
									+ round(directionCM.getPhiDegrees())
									+ "\t"
									+ round(thetaDlab)
									+ "\t"
									+ round(directionLab.getPhiDegrees())
									+ "\t"
									+ round(incidence)
									+ "\t"
									+ round(Edep)
									+ "\t"
									+ round(tof)
									+ "\t"
									+ det
									+ "\t"
									+ strip
									+ "\n");
						} else {
							outEvn.write(
								"0\t"
									+ round(projDir.getThetaDegrees())
									+ "\t"
									+ round(projDir.getPhiDegrees())
									+ "\t"
									+ round(thetaDcm)
									+ "\t"
									+ round(directionCM.getPhiDegrees())
									+ "\t"
									+ round(thetaDlab)
									+ "\t"
									+ round(directionLab.getPhiDegrees())
									+ "\t"
									+ round(incidence)
									+ "\t"
									+ round(Edep)
									+ "\t"
									+ round(tof)
									+ "\t"
									+ det
									+ "\t"
									+ strip
									+ "\n");
						}
					} else {
						outEvn.write(
							"0\t"
								+ round(projDir.getThetaDegrees())
								+ "\t"
								+ round(projDir.getPhiDegrees())
								+ "\t"
								+ round(thetaDcm)
								+ "\t"
								+ round(directionCM.getPhiDegrees())
								+ "\t"
								+ round(thetaDlab)
								+ "\t"
								+ round(directionLab.getPhiDegrees())
								+ "\n");
					}
					if (_i % 1000 == 0) {
						System.out.println(_i + ":" + hits);
					}
					if (_i == numEvents && hits < hitsWanted) {
						numEvents =
							(int) Math.round(
								1.2 * (double) _i / (double) hits * hitsWanted);
						System.out.println(
							"Events so far = " + _i + ", " + hits + " hits.");
						System.out.println(
							"Changing total events to simulate to "
								+ numEvents
								+ ".");
					}
				}
				while (_i < numEvents);
				outEvn.flush();
				outEvn.close();
				outCounts.write("bin\tdet\tstrip\tcounts\tEmin\tEmax\n");
				for (int i = 0; i < 5; i++) {
					for (int j = 0; j < 16; j++) {
						int bin = i * 16 + j;
						outCounts.write(
							bin
								+ "\t"
								+ i
								+ "\t"
								+ j
								+ "\t"
								+ counts[bin]
								+ "\t"
								+ round(Emin[bin])
								+ "\t"
								+ round(Emax[bin])
								+ "\n");
					}
				}
				outAngles.write("Theta\tCMcounts\tlabCounts\n");
				for (int i = 0; i < angleBinning; i++) {
					outAngles.write(
						(i * 180 / angleBinning)
							+ "\t"
							+ round(thetaCM[i])
							+ "\t"
							+ round(thetaLab[i])
							+ "\n");
				}
				outAngles.flush();
				outAngles.close();
				outCounts.flush();
				outCounts.close();
				System.out.println(
					"Done. "
						+ hits
						+ " detector hits for "
						+ _i
						+ " simulated decays.");
				outDescription.write(_i + " simulated events\n");
				outDescription.write(hits + " hits\n");
				double efficiency = (double) hits / (double) _i;
				double delEff = Math.sqrt((double) hits) / (double) _i;
				UncertainNumber uncEff =
					new UncertainNumber(efficiency, delEff);
				outDescription.write("total efficiency: " + uncEff + "\n");
				outDescription.flush();
				outDescription.close();
			} catch (KinematicsException ke) {
				System.err.println(ke);
			} catch (NuclearException ke) {
				System.err.println(ke);
			} catch (dwvisser.math.MathException me) {
				System.err.println(me);
			} catch (IOException ioe) {
				System.err.println(ioe);
			}
		}
	}

	static private String round(double number) {
		DecimalFormat dm = new DecimalFormat("##.###");
		return dm.format(number);
	}

	/**
	 * Returns thickness of Al in front of active region in cm (foil plus dead layer).
	 * @param foilThickness in mils
	 */
	private double getThicknessAl(double foilThickness) {
		double detectorDeadLayer = DEAD_LAYER_THICKNESS * 1.0e-4;
		//in cm, i.e. 0.2 um
		double thicknessInCM = foilThickness / 1000 * 2.54;
		return thicknessInCM + detectorDeadLayer;
	}

	static public void main(String[] args) {
		new EngeYLSA_Simulation();
		//System.out.println(Math.toDegrees(0.160));
		//System.out.println(Math.toDegrees(0.080));
	}
}
