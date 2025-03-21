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
package cmdeibel.monte;
import dwvisser.math.Matrix;
import java.io.FileWriter;
import java.io.IOException;


/**
 * This class represents the geometry of a Micron S2 detector, based 
 * on the class for the LEDA-type detector written by D. W. Visser
 * 
 * This class represents the geometry of a Micron LEDA-type detector,
 * to use for deciding if an virtual vector will hit and which strip.
 *
 * @author  Dale Visser
 * @version 1.0 (7 March 2001)
 * updated by C. M. Deibel for micron S2 DSSD at ANL March 2010
 */

public class S2Detector extends Object implements WeightingFunction{
	
	private double z0;
	private Matrix xpd; //lab origin in detector frame
	boolean hit; //whether a strip was hit
	private double cosThetaInc; //cosine of incidence angle if detector hit
	private int ring; //if detector hit, contains strip that was hit
	boolean interring; //if interstrip event then true
	double distance; //distance to detector plane, in mm
	
	/*Detector geometry for Micron S2 detector- annular double sided strip detector
	 * active inner radius = 11 mm; active outer radius = 35 mm
	 * 48 rings which we group into 3's in electronics 
	 * (i.e. 16 rings each group 1.5 mm "thick") 
	 */
	
	public S2Detector(double z0){
		
		this.z0 = z0; //in mm
		
		xpd = new Matrix("0; 0; " + z0 + ";"); /*origin before rotating (e.g. YLSA); 
												since S2 is not at an angle
												no rotatation is necessary*/
	}
		public boolean isHit(Direction dir) {
			hit = false;
			
			Matrix detDirMat = dir.getVector();//this and cosThetaInc from MictronDetctor class; is this right???
			
			cosThetaInc = detDirMat.element[1][0]; //y (norm guaranteed to be 1)

			
			double residTheta = dir.getTheta();
			
			double XYresid = z0 * Math.abs(Math.sin(residTheta));
			
			double r = Math.sqrt(z0*z0 + XYresid*XYresid);
			 
			distance = r;
			
			hit = false; 
			
			if(XYresid <= 35 && XYresid >= 11) {
				ring = (int) Math.floor((XYresid - 11.0) / 1.5); //possible ring
				double truncR = XYresid - 11.0 - 1.5 * ring;
				/* excess radius from inner strip radius */
				if (ring < 16 && ring > -1) {
					hit =true; //this assumes no inactive area between rings!!!!
					/*if (truncR <= ??? whatever the active radius of the strip is 
					  e.g. 4.9 mm for the 5 mm strips in YLSA
					 
						&& cosThetaInc > 0.0) {
						hit = true;
						/* cTI > 0 eliminates forward hemisphere solutions 
					} else { //hit false...possibly interstrip though
						interring = (ring < 15); //false if not*/
					}
				}
			return hit;
			}
			
		/**
		 * If interstrip is true, then the event was between the returned strip and the returned strip + 1.
		 */
		public int getRing() {
			return ring;
		}
		
		/**
		 * Returns 1/cos(incidence angle).
		 */
		public double getIncidence() {
			return 1.0 / cosThetaInc;
		}

		/**
		 * Returns distance to detector in mm.
		 */
		public double getDistance() {
			return distance;
		}

/*	not worrying about interstrip for now	
 * public boolean getInterStrip() {
			return interstrip;*/
		
		/**
		 * Weighting for isotropic thetas in degrees.
		 */
		public double weight(double x) {
			return Math.sin(Math.toRadians(x));
		}
		
	/*	static public void main(String[] args) {
			String fileRoot = "d:\\simulations\\labEfficiency\\";
			Direction d;
			boolean hit;
			double z0 = 167.1;
			
			String input =
				"z"
					+ (int) Math.round(z0 * 10);
			DataSet[] theta = new DataSet[16];
			DataSet[] inc = new DataSet[16];
			DataSet[] dist = new DataSet[16];
			S2Detector s2d = new S2Detector(z0);
			S2DetectorFrame df = new S2DetectorFrame(z0);
			for (int i = 0; i < 16; i++) {
				theta[i] = new DataSet();
				inc[i] = new DataSet();
				dist[i] = new DataSet();
			}
			
			int numberDone = 0;
			int countsToBeDone = 900;
			int eventCount = 0;
			int hitCount = 0;
			int updateInterval = 500;
			int numEventsToSimulate = 100000;
			
			while (eventCount < numEventsToSimulate) {
				d = Direction.getRandomDirection();
				/*if (d.getTheta() < deg112 || d.getTheta() > deg166) {
				    hit=false;
				} else {*/
				/*hit = s2d.isHit(d);
				//}
				eventCount++;
				if (hit) {
					hitCount++;
					int ring = s2d.getRing();
					theta[ring].add(Math.toDegrees(d.getTheta()));
					//inc[ring].add(s2d.getIncidence());
					//dist[ring].add(s2d.getDistance());
					if (theta[ring].getSize() == countsToBeDone)
						numberDone++;
				}
				if (eventCount % updateInterval == 0
					|| eventCount == numEventsToSimulate) {
					for (int ring = 0; ring < 16; ring++) {
						df.updateRing(
							ring,
							theta[ring].getSize(),
							theta[ring].getMean(),
							theta[ring].getSD(),
							inc[ring].getMean(),
							inc[ring].getSD(),
							dist[ring].getMean(),
							dist[ring].getSD());
					}
				}
				df.updateEventCount(eventCount, hitCount);
			}
			
			int[][] thetaHists = new int[16][122];
			int[][] incHists = new int[16][82];
			int[][] distHists = new int[16][(110 - 75 + 1) * 5];
			for (int ring = 0; ring < 16; ring++) {
				thetaHists[ring] = theta[ring].getHistogram(110, 170, 0.5);
				incHists[ring] = inc[ring].getHistogram(0.8, 1.6, 0.02);
				distHists[ring] = dist[ring].getHistogram(75, 110, 0.2);
			}
			
			try {
				FileWriter incHist =
					new FileWriter(fileRoot + "\\incHist_" + input + ".dat");
				FileWriter thetaHist =
					new FileWriter(fileRoot + "\\thetaHist_" + input + ".dat");
				FileWriter distHist =
					new FileWriter(fileRoot + "\\dist_" + input + ".dat");
				FileWriter summary =
					new FileWriter(fileRoot + "\\monte_" + input + ".txt");
				summary.write(
					"z0 = " + z0 + " mm, " + " degrees\n");
				summary.write(
					"Done. All rings have at least "
						+ countsToBeDone
						+ " counts.\n");
				double p = (double) hitCount / (double) eventCount;
				summary.write(
					"Array Geometric Efficiency = "
						+ 2.5 * p
						+ " +/- "
						+ 2.5 * Math.sqrt(p * (1.0 - p) / (double) eventCount)
						+ "\n");

				for (int ring = 0; ring < 16; ring++) {
					summary.write(
						ring
							+ "\t"
							+ theta[ring].getSize()
							+ "\t"
							+ theta[ring].getMean()
							+ "\t"
							+ theta[ring].getSD()
							+ "\t"
							+ inc[ring].getMean()
							+ "\t"
							+ inc[ring].getSD()
							+ "\t"
							+ dist[ring].getMean()
							+ "\t"
							+ dist[ring].getSD()
							+ "\n");
					FileWriter fw =
						new FileWriter(fileRoot + "\\monte_" + ring + ".dat");
					double[] angles = theta[ring].getData();
					double[] incid = inc[ring].getData();
					double[] distances = dist[ring].getData();
					for (int event = 0; event < angles.length; event++) {
						fw.write(
							angles[event]
								+ "\t"
								+ incid[event]
								+ "\t"
								+ distances[event]
								+ "\n");
					}
					fw.flush();
					fw.close();
				}
				for (int bin = 0; bin < thetaHists[0].length; bin++) {
					thetaHist.write((110 + bin * 0.5) + "");
					for (int ring = 0; ring < 16; ring++) {
						thetaHist.write("\t" + thetaHists[ring][bin]);
					}
					thetaHist.write("\n");
				}
				for (int bin = 0; bin < incHists[0].length; bin++) {
					incHist.write((0.8 + bin * 0.02) + "");
					for (int ring = 0; ring < 16; ring++) {
						incHist.write("\t" + incHists[ring][bin]);
					}
					incHist.write("\n");
				}
				for (int bin = 0; bin < distHists[0].length; bin++) {
					distHist.write((75 + bin * 0.2) + "");
					for (int ring = 0; ring < 16; ring++) {
						distHist.write("\t" + distHists[ring][bin]);
					}
					distHist.write("\n");
				}
				thetaHist.flush();
				thetaHist.close();
				incHist.flush();
				incHist.close();
				distHist.flush();
				distHist.close();
				summary.flush();
				summary.close();
			}
			
			catch (IOException e) {
				System.err.println(e);
			}
			//Direction d=new Direction(Math.toRadians(41.7),Math.toRadians(96.5));
			//md.isHit(d);
			
			
		}*/

	
	}
	
