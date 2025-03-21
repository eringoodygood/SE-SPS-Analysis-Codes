	/***************************************************************
 * Author: CMDeibel
 * 
 * My own attempt at a simulation from scratch based on the YLSA simulation
 * 
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

package cmdeibel.monte;
import cmdeibel.nuclear.*;

import java.io.*;
import java.util.Random;
import dwvisser.math.UncertainNumber;
import java.text.DecimalFormat;


public class ANL_DSSD_Enge_sim_HE_branch_v2 extends Object{
	static final double C_MM_PER_NSEC = 299.792458;
	static final double DEAD_LAYER_THICKNESS = 0.2; //um

	//Fields specified in constructor
	Nucleus target, beam, projectile; //, decay;
	double xtarg; //thickness in ug/cm^2
	double Ebeam; //beam kinetic energy in MeV
	double ExResid; //in MeV
	double Q;
	double theta; //for spectrometer, in degrees
	String outFile;
	
	
	//Fields set by calculation
	Nucleus residual;
	double thetaR; //for spectrometer, in radians
	FileWriter outEvn, outCounts, outDescription, outAngles;

	/** creates new simulation for beam + target => residual + projectile 
	 * 	in inverse kinematics such that the heavy residual particle is 
	 * 	detected in the spectrograph and the lighter projectile is detected 
	 * 	in the DSSD.  However, for now we will keep the naming convention that
	 * projectile = particle detected in spectrograph and residual is detected in 
	 * DSSD.  This will only work for final nuclei in g.s. which is no good, but 
	 * no time to fix this now. 
	 * */
	public ANL_DSSD_Enge_sim_HE_branch_v2() {
		DataSet[] ringCMtheta = new DataSet[16];

		double[] ExResidValues = {0};
		
		double fractionalErrorPerRing = 0.1;
		int hitsWanted = 16 * (int) Math.pow(fractionalErrorPerRing, -2);
		//hits wanted ~ (fractional error wanted per strip)^-2
		System.err.println(
			"Total hits wanted: "
				+ hitsWanted
				+ " to get "
				+ (100 * fractionalErrorPerRing)
				+ "% error each ring");
		/**
		 * numEvents will be expanded as needed so that hitsWanted
		 * will be exceeded by approximatley 20%
		 */
		//int numEvents = (int) Math.round(hitsWanted * 1.2);
		int numEvents = 4000;
		
		double z0 = 78; //distance in mm to vertex of array
		//double theta = 55; 
		
		try {
			target = new Nucleus(1, 1);
			beam = new Nucleus(17, 33);
			projectile = new Nucleus(16, 30);
		} catch (NuclearException ne) {
			System.err.println(ne);
		}
		
		Ebeam = 250; //beam kinetic energy in MeV
		Q = -2.07684;
		xtarg = 648; //thickness in ug/cm^2
		String starg = "C 1 H 2"; //material specification for target
		theta = 0; //for spectrometer, in degrees
		
		double AlThickness = 0;
		//thickness of degrading foil in front of detector in mils
		//at this time, simply added to dead layer thickness, assuming we would
		//put the foils parallel to the detectors
		double hit_threshold = 1.0;
		//energy deposited in MeV to be considered a hit
		int[] counts = new int[90];
		double[] Emin = new double[90];
		double[] Emax = new double[90];  //are counts Emin and Emax necessary?????
		
		/*** Calculation of some fields based on initial values ***/
		for (int rEx = 0; rEx < ExResidValues.length; rEx++) {
			for (int i = 0; i < ringCMtheta.length; i++) {
				ringCMtheta[i] = new DataSet();
			}
			ExResid = ExResidValues[rEx]; //in MeV
			try {
				residual =
					new Nucleus(
						target.Z + beam.Z - projectile.Z,
						target.A + beam.A - projectile.A,
						ExResid);
			
			} catch (NuclearException ne) {
				System.err.println(ne);
			}
			
			String outFileRoot =
			      //System.getProperty("user.home")+File.separator+
			      "/Users/catherinedeibel/Documents/Monte Carlo/S2_DSSD/"
				    + "HE_branch_v2"
					+ "_"
					+ residual
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
				     "("
					+ beam
					+ "+"
					+ target
					+") -> ("
					+ projectile
					+"+"
					+ residual
					+")"
					);
			System.out.println("File root: " + outFileRoot);
			
			try {
				outDescription = new FileWriter(outFileRoot + ".txt");
				outEvn = new FileWriter(outFileRoot + ".csv");
				outCounts = new FileWriter(outFileRoot + ".sum");
				outAngles = new FileWriter(outFileRoot + ".ang");
			} catch (IOException ioe) {
				System.err.println(ioe);
			}

			Random random = new Random();
			double Mresid = residual.getMass().value;
			double Mbeam = beam.getMass().value;
			double Mtarget = target.getMass().value;
			double Mproj = projectile.getMass().value;
			
			try{
			
			S2Det s2d = new S2Det(z0);
			//MicronDetector md = new MicronDetector(z0,theta);
				
				
			Solid deadLayer;
			EnergyLoss deadLayerLoss;
			outDescription.write(
				target
					+ "("
					+ beam
					+ ","
					+ projectile
					+ ")"
					+ residual
					+ "\n");
			outDescription.write(
				"Ex(residual " + residual + ") = " + ExResid + " MeV\n");
			outDescription.write(
				"Theta(residual "
					+ residual
					+ ") = "
					+ theta
					+ " degrees\n");
			outDescription.write(
				"Target Thickness = " + xtarg + " ug/cm^2\n");
			deadLayer =
				new Solid(getThicknessAl(AlThickness), Absorber.CM, "Al");
			deadLayerLoss = new EnergyLoss(deadLayer);


			double[] p_CM = new double[4];// is this necessary???
			double[] p_lab = new double[4];
			/*** work begins here ***/
			Reaction reaction;
			
			int angleBinning = 60;
			double divFactor = 180.0 / angleBinning;
			
			int[] thetaCM = new int[angleBinning];
			int[] thetaLab = new int[angleBinning];
			int[] thetaProjLab = new int[angleBinning];
			int[] thetaProjCM = new int[angleBinning];
			int hits = 0;
			outEvn.write(
				"hit,projTheta,projPhi,CMtheta,CMphi,labTheta,labPhi,Edep,tof,ring\n");
			int _i = 0;
			
			do{
				_i++;
				double depth = random.nextDouble() * xtarg;
				int branch = random.nextInt(2);//should return random value of 1 or 0
				//System.out.println(branch);
				Solid targetMatter = new Solid(starg, depth);
				EnergyLoss targetLossCalc = new EnergyLoss(targetMatter);
				double targetLoss =
					0.001 * targetLossCalc.getEnergyLoss(beam, Ebeam);
				double Tbeam = Ebeam - targetLoss;
				

				/* choosing random direction for projectile in CM frame, then going to lab frame*/
				Direction projDir = null;
				Direction projDirCM = null;
				boolean directionAccepted = false;
				while (!directionAccepted) {
					Direction temp_projDirCM = Direction.getRandomDirection();
					
					double gamma = Math.sqrt((Mbeam*Mproj*Tbeam)
									/(Mresid*(Mproj+Mresid)*Q+Mresid*(Mresid+Mproj-Mbeam)*Tbeam));
					
					double CosThetaLab = (gamma + Math.cos(temp_projDirCM.getTheta()))
											/Math.sqrt(1+Math.pow(gamma, 2) +2*gamma*Math.cos(temp_projDirCM.getTheta()));
					
					Direction test_projDir = new Direction(Math.acos(CosThetaLab),temp_projDirCM.getPhi());
					
					Reaction temp_reaction =
						new Reaction(
							target,
							beam,
							projectile,
							Tbeam,
							test_projDir.getThetaDegrees(),
							ExResid);
					
					boolean overMax_result = temp_reaction.getOverMax();
					
					if(overMax_result==false){
						directionAccepted = true;
					}
					
					if(directionAccepted){
						projDir = test_projDir;
						projDirCM = temp_projDirCM;
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
				//determine direction residual nucleus is moving; i.e. the nucleus detected in S2
				//theta comes from reaction object, (taking absolute value, as the minus
				//sign usually returned causes the new phi to be calculated wrong)
				//phi is opposite the phi of the projectile
				
				
				Direction residDir =
					new Direction(
						Math.toRadians(
							Math.abs(reaction.getLabAngleResidual(1))),
						Math.PI + projDir.getPhi());
				
				Direction CMresidDir =new Direction(
						Math.toRadians(
								Math.abs(reaction.getCMAngleResidual(1))),
							Math.PI + projDir.getPhi());
				
				Direction directionCM = CMresidDir;
				
				double ELabResid = reaction.getLabEnergyResidual(1);
				
				p_lab[0] = ELabResid;
				System.arraycopy(
						residDir.get3vector(reaction.getLabMomentumResidual(1)),
						0,
						p_lab,
						1,
						3);
				Direction directionLab =
					new Direction(p_lab[1], p_lab[2], p_lab[3]);
				Direction queryDir = directionLab;
				//Direction queryDir = new Direction(Math.toRadians(15),Math.toRadians(90));
				

				
				boolean hit = s2d.isHit(queryDir);
				//boolean hit = md.isHit(queryDir);
				int ring;
				double Edep, tof;
				
				double thetaDlab = directionLab.getThetaDegrees();
				double thetaDcm = directionCM.getThetaDegrees();
				
				double thetaDProjLab = projDir.getThetaDegrees();
				double thetaDProjCM = projDirCM.getThetaDegrees();
				
				
				thetaLab[(int) Math.floor(thetaDlab / divFactor)]++;
				thetaCM[(int) Math.floor(thetaDcm / divFactor)]++;
				thetaProjLab[(int) Math.floor(thetaDProjLab/ divFactor)]++;
				thetaProjCM[(int) Math.floor(thetaDProjCM/ divFactor)]++;
				
				if (hit) {
					//System.out.println("got here");
					ring = s2d.getRing();
					//ring = md.getRing();
					int bin = ring;
					
					double Tinit = p_lab[0];
					
					//double Tinit = p_lab[0] - Mresid;
					double Tflight =
						Tinit
							- 0.001
								* targetLossCalc.getEnergyLoss(
									residual,
									Tinit,
									Math.PI - directionLab.getTheta());
					
					//double Tflight = Tinit;  in original code p_lab[0] came from boost to p_cm
					
					tof =
						s2d.getDistance()
							/ (Reaction.getBeta(residual, Tflight)
								* C_MM_PER_NSEC);
					
					
					/*Edep =
						Tflight
							- 0.001
								* deadLayerLoss.getEnergyLoss(
									residual,
									Tflight,
									Math.acos(1 / incidence));*/
					
					Edep = Tflight;
					
					
					boolean energyHit = Edep >= hit_threshold;
					//boolean energyHit = true;	
					
					if(energyHit){
						//System.out.println("got here");
						ringCMtheta[ring].add(thetaDcm);
						hits++;
						counts[bin]++;
						if (Emin[bin] == 0.0 || Emin[bin] > Edep)
							Emin[bin] = Edep;
						if (Emax[bin] == 0.0 || Emax[bin] < Edep)
							Emax[bin] = Edep;
						//time-of-flight in nsec
						outEvn.write(
							"1,"
								+ round(projDir.getThetaDegrees())
								+ ","
								+ round(projDir.getPhiDegrees())
								+ ","
								+ round(thetaDcm)
								+ ","
								+ round(directionCM.getPhiDegrees())
								+ ","
								+ round(thetaDlab)
								+ ","
								+ round(directionLab.getPhiDegrees())
								+ ","
								+ round(Edep)
								+ ","
								+ round(tof)
								+ ","								
								+ ring
								+ "\n");
					
					}
					
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
			outCounts.write("ring\tcounts\tEmin\tEmax\n");
				for (int j = 0; j < 16; j++) {
				int bin = j;
				outCounts.write(
					bin
						+ "\t"
						+ counts[bin]
						+ "\t"
						+ round(Emin[bin])
						+ "\t"
						+ round(Emax[bin])
						+ "\n");
			}
			
				outAngles.write("Theta\tCMcounts\tlabCounts\tProjCMCounts\tProjLabCounts\n");
				for (int i = 0; i < angleBinning; i++) {
					outAngles.write(
						(i * 180 / angleBinning)
							+ "\t"
							+ round(thetaCM[i])
							+ "\t"
							+ round(thetaLab[i])
							+ "\t"
							+ round(thetaProjCM[i])
							+"\t"
							+ round(thetaProjLab[i])
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
				outDescription.write("\nRing\tCMtheta\thits\tEff\tdelEff\n");
				for (int i = 0; i < ringCMtheta.length; i++) {
					int _hits = ringCMtheta[i].getSize();
					double _eff = (double) _hits / _i;
					double _deleff = Math.sqrt(_hits) / _i;
					outDescription.write(
						i
							+ "\t"
							+ round(ringCMtheta[i].getMean())
							+ "\t"
							+ _hits
							+ "\t"
							+ _eff
							+ "\t"
							+ _deleff
							+ "\n");
				}
				
			outDescription.flush();
			outDescription.close();
			}
		
			catch (KinematicsException ke) {
				System.err.println(ke);
			} catch (NuclearException ke) {
				System.err.println(ke);
			} /*catch (dwvisser.math.MathException me) {
				System.err.println(me);
			}*/ catch (IOException ioe) {
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
		new ANL_DSSD_Enge_sim_HE_branch_v2();
		//System.out.println(Math.toDegrees(0.160));
		//System.out.println(Math.toDegrees(0.080));
	}
}
