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
package dwvisser.monte;
import dwvisser.math.Matrix;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class represents the geometry of a Micron LEDA-type detector,
 * to use for deciding if an virtual vector will hit and which strip.
 *
 * @author  Dale Visser
 * @version 1.0 (7 March 2001)
 */
public class MicronDetector extends Object implements WeightingFunction {

	private double z0, theta;
	private Matrix rotate; //rotation matrix between lab and detector
	private Matrix xpd; //lab origin in detector frame
	boolean hit; //whether a strip was hit
	private double cosThetaInc; //cosine of incidence angle if detector hit
	private int strip; //if detector hit, contains strip that was hit
	boolean interstrip; //if interstrip event then true
	double distance; //distance to detector plane, in mm

	//Detector geometry from T. Davinson et.al. NIM A 454 (2000) 350-358
	//Inner radius for strip is 50.00 mm + 5.00 mm * strip number (0 thru 15)
	//Outer radius for strip is inner radius + 4.90 mm
	//phi extents in degrees are as follows
	//private static final double [] PHI = {39.82, 40.00, 40.17, 40.30, 40.42, 
	//40.52, 40.61, 40.69, 40.76, 40.82, 40.88, 40.93, 40.98, 35.89, 28.75, 18.91};
	private static final double phiLimit = Math.toRadians(21.0);
	private static final double[] rayDisplacement =
		{
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			1.0,
			6.266,
			6.266 + 7.961,
			6.266 + 7.961 + 11.807 };
	private static double[] zDiff = new double[rayDisplacement.length];

	/** It is assumed that the detector is centered about phi=90 degrees.
	 * Unfortunately, the real array has detectors at -90 degrees, etc,
	 * so some care is needed in using this. See the changePhi and getDetector
	 * methods. This
	 * constructor specified the location along the z-axis of the virtual center
	 * of the detector.  Zero would be the target, and positive numbers mean upstream
	 * of the target.
	 * Theta is the angle the normal vector of the detector plane makes with the
	 * beam axis (positive by definition).  Alternatively, it is the angle that
	 * an initally flat array gets rotated forward to be in the lampshade
	 * configuration.
	 * @param z0 in mm
	 * @param theta in radians
	 */
	public MicronDetector(double z0, double theta) {
		this.z0 = z0; //in mm
		this.theta = theta;
		double thetaP = (theta - Math.PI / 2.0);
		rotate =
			new Matrix(
				"1 0 0\n"
					+ "0 "
					+ Math.cos(thetaP)
					+ " "
					+ Math.sin(thetaP)
					+ "\n"
					+ "0 "
					+ (-Math.sin(thetaP))
					+ " "
					+ Math.cos(thetaP)
					+ "\n");
		Matrix temp = new Matrix("0; 0; " + z0 + ";"); //origin before rotating
		xpd = new Matrix(rotate, temp, '*'); //rotated to detector frame
		for (int i = 0; i < rayDisplacement.length; i++)
			zDiff[i] = rayDisplacement[i] / (Math.sin(phiLimit));
		System.out.println(
			"Initialized "
				+ getClass().getName()
				+ "("
				+ z0
				+ ","
				+ theta
				+ ")");
	}

	public boolean isHit(Direction dir) {
		hit = false;
		Matrix detDirMat = new Matrix(rotate, dir.getVector(), '*');
		//Direction detDir = Direction.getDirection(detDirMat);
		cosThetaInc = detDirMat.element[1][0]; //y (norm guaranteed to be 1)
		double s = -xpd.element[1][0] / cosThetaInc;
		//"distance" to detector plane
		distance = s; //distance in mm to detector plane
		double px = xpd.element[0][0] + s * detDirMat.element[0][0];
		//x position of hit
		double pz = xpd.element[2][0] + s * detDirMat.element[2][0];
		//z position of hit
		double pr = Math.sqrt(px * px + pz * pz); //radial coordinate of hit
		//angular coordinate, zero equals center line of detector strips
		//1.5 added to account for the fact that the 42 degree extent of the wafer is to one side of the pcb
		//above theta is not the one we want...see below
		hit = false;
		if (pr <= 129.90 && pr >= 50.00) {
			strip = (int) Math.floor((pr - 50.0) / 5.0); //possible strip
			double truncR = pr - 50.0 - 5.0 * strip;
			/* excess radius from inner strip radius */
			if (strip < 16 && strip > -1) {
				double pzprime = pz - zDiff[strip];
				double pthprime = Math.atan2(px, pzprime);
				if (Math.abs(pthprime) <= phiLimit
					&& truncR <= 4.90
					&& cosThetaInc > 0.0) {
					hit = true;
					/* cTI > 0 eliminates forward hemisphere solutions */
				} else { //hit false...possibly interstrip though
					interstrip = (strip < 15); //false if not
				}
			}
		}
		return hit;
	}

	/**
	 * If interstrip is true, then the event was between the returned strip and the returned strip + 1.
	 */
	public int getStrip() {
		return strip;
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

	public boolean getInterStrip() {
		return interstrip;
	}

	/**
	 * Given a direction, returns a direction with modified phi so that
	 * associated direction is in the correct 72 degree window to possibly hit
	 * the detector(90-36 to 90+36, or 54 to 126).
	 */
	static public Direction changePhi(Direction dir) {
		double phi = Math.toDegrees(dir.getPhi()); //input phi in degrees
		phi -= 18;
		int n = (int) Math.floor(phi / 72);
		double x = phi - n * 72;
		return new Direction(dir.getTheta(), Math.toRadians(54 + x));
	}

	/** based on our convention of the bottom detector being det 4,
	 * detector numbers going down as one goes counter clockwise
	 * looking at the array from the target
	 */
	static public int getDetector(Direction dir) {
		double phi = Math.toDegrees(dir.getPhi());
		if (phi < 18)
			return 3;
		if (phi < 90)
			return 2;
		if (phi < 162)
			return 1;
		if (phi < 234)
			return 0;
		if (phi < 306)
			return 4;
		return 3; //must be >= 306 and <= 360
	}

	/**
	 * Weighting for isotropic thetas in degrees.
	 */
	public double weight(double x) {
		return Math.sin(Math.toRadians(x));
	}

	static public void main(String[] args) {
		String fileRoot = "/Users/eringood/Desktop/SPS Code/";
		Direction d;
		boolean hit;
		double z0 = 167.1;
		double incline = 0;
		String input =
			"z"
				+ (int) Math.round(z0 * 10)
				+ "th"
				+ (int) Math.round(incline * 10);
		DataSet[] theta = new DataSet[16];
		DataSet[] inc = new DataSet[16];
		DataSet[] dist = new DataSet[16];
		MicronDetector md = new MicronDetector(z0, Math.toRadians(incline));
		DetectorFrame df = new DetectorFrame(z0, incline);
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
			d = changePhi(Direction.getRandomDirection());
			/*if (d.getTheta() < deg112 || d.getTheta() > deg166) {
			    hit=false;
			} else {*/
			hit = md.isHit(d);
			//}
			eventCount++;
			if (hit) {
				hitCount++;
				int strip = md.getStrip();
				theta[strip].add(Math.toDegrees(d.getTheta()));
				inc[strip].add(md.getIncidence());
				dist[strip].add(md.getDistance());
				if (theta[strip].getSize() == countsToBeDone)
					numberDone++;
			}
			if (eventCount % updateInterval == 0
				|| eventCount == numEventsToSimulate) {
				for (int strip = 0; strip < 16; strip++) {
					df.updateStrip(
						strip,
						theta[strip].getSize(),
						theta[strip].getMean(),
						theta[strip].getSD(),
						inc[strip].getMean(),
						inc[strip].getSD(),
						dist[strip].getMean(),
						dist[strip].getSD());
				}
			}
			df.updateEventCount(eventCount, hitCount);
		}
		int[][] thetaHists = new int[16][122];
		int[][] incHists = new int[16][82];
		int[][] distHists = new int[16][(110 - 75 + 1) * 5];
		for (int strip = 0; strip < 16; strip++) {
			thetaHists[strip] = theta[strip].getHistogram(110, 170, 0.5);
			incHists[strip] = inc[strip].getHistogram(0.8, 1.6, 0.02);
			distHists[strip] = dist[strip].getHistogram(75, 110, 0.2);
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
				"z0 = " + z0 + " mm, incline = " + incline + " degrees\n");
			summary.write(
				"Done. All strips have at least "
					+ countsToBeDone
					+ " counts.\n");
			double p = (double) hitCount / (double) eventCount;
			summary.write(
				"Array Geometric Efficiency = "
					+ 2.5 * p
					+ " +/- "
					+ 2.5 * Math.sqrt(p * (1.0 - p) / (double) eventCount)
					+ "\n");

			for (int strip = 0; strip < 16; strip++) {
				summary.write(
					strip
						+ "\t"
						+ theta[strip].getSize()
						+ "\t"
						+ theta[strip].getMean()
						+ "\t"
						+ theta[strip].getSD()
						+ "\t"
						+ inc[strip].getMean()
						+ "\t"
						+ inc[strip].getSD()
						+ "\t"
						+ dist[strip].getMean()
						+ "\t"
						+ dist[strip].getSD()
						+ "\n");
				FileWriter fw =
					new FileWriter(fileRoot + "\\monte_" + strip + ".dat");
				double[] angles = theta[strip].getData();
				double[] incid = inc[strip].getData();
				double[] distances = dist[strip].getData();
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
				for (int strip = 0; strip < 16; strip++) {
					thetaHist.write("\t" + thetaHists[strip][bin]);
				}
				thetaHist.write("\n");
			}
			for (int bin = 0; bin < incHists[0].length; bin++) {
				incHist.write((0.8 + bin * 0.02) + "");
				for (int strip = 0; strip < 16; strip++) {
					incHist.write("\t" + incHists[strip][bin]);
				}
				incHist.write("\n");
			}
			for (int bin = 0; bin < distHists[0].length; bin++) {
				distHist.write((75 + bin * 0.2) + "");
				for (int strip = 0; strip < 16; strip++) {
					distHist.write("\t" + distHists[strip][bin]);
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
		} catch (IOException e) {
			System.err.println(e);
		}
		//Direction d=new Direction(Math.toRadians(41.7),Math.toRadians(96.5));
		//md.isHit(d);
	}
}