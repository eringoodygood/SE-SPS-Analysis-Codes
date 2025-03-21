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

public class S2Det extends Object implements WeightingFunction{
	
	private double z0;
	boolean hit; //whether a strip was hit
	private double cosThetaInc;//cosine angle of "residual", i.e. particle incident on DSSD
	private int ring; //if detector hit, contains strip that was hit
	boolean interring; //if interstrip event then true
	double distance; //distance to detector plane, in mm
	
	/*Detector geometry for Micron S2 detector- annular double sided strip detector
	 * active inner radius = 11 mm; active outer radius = 35 mm
	 * 48 rings which we group into 3's in electronics 
	 * (i.e. 16 rings each group 1.5 mm "thick") 
	 */
	
	public S2Det(double z0){
		
		this.z0 = z0; //in mm
	
	}
		public boolean isHit(Direction dir) {
			hit = false;
			
			//Matrix detDirMat = dir.getVector();//this and cosThetaInc from MictronDetctor class; is this right???
			
			//cosThetaInc = detDirMat.element[1][0]; //y (norm guaranteed to be 1)
	
			double residTheta = dir.getTheta();
			
			//cosThetaInc = Math.cos(residTheta);
			
			double XYresid = z0 * Math.abs(Math.tan(residTheta));
			
			double r = Math.sqrt(z0*z0 + XYresid*XYresid);
			 
			distance = r;
			
			if(XYresid <= 35 && XYresid >= 11) {
				ring = (int) Math.floor((XYresid - 11.0) / 1.5); //possible ring
				hit = true;
				//double truncR = XYresid - 11.0 - 1.5 * ring;
				/* excess radius from inner strip radius 
				if (ring < 16 && ring > -1) {
					hit =true; //this assumes no inactive area between rings!!!!
					/*if (truncR <= ??? whatever the active radius of the strip is 
					  e.g. 4.9 mm for the 5 mm strips in YLSA
					 
						&& cosThetaInc > 0.0) {
						hit = true;
						/* cTI > 0 eliminates forward hemisphere solutions 
					} else { //hit false...possibly interstrip though
						interring = (ring < 15); //false if not
					}*/
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
			
	}
	
