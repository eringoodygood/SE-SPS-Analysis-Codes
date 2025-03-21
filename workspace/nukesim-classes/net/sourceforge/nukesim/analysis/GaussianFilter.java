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
package net.sourceforge.nukesim.analysis;
import java.io.File;

public class GaussianFilter {
	
    /**
     * magic number for calculating
     */
    static private final double a=0.93911;

    /**
     * magic number for calculating
     */
    static private final double b=2.77066;

    /**
     * magic number for calculating
     */
    static final double SIGMA_TO_FWHM=2.354;
	
	private double [] inSpectrum, outSpectrum; 
	private double width;

	public GaussianFilter(double [] inSpectrum, double width){
		this.inSpectrum=inSpectrum;
		this.width=width;
		processSpectrum();
	}
	
	private void processSpectrum(){
		int length = inSpectrum.length;
		outSpectrum=new double[length];
		for (int newChannel=0; newChannel < length; newChannel++){
			double newCounts = 0.0;
			double scaleFactor = a*inSpectrum[newChannel]/width;
			int minCh = (int)Math.round(Math.max(0,newChannel-3*width));
			int maxCh = (int)Math.round(Math.min(length-1,newChannel+3*width));
			for (int oldChannel = minCh; oldChannel <= maxCh; oldChannel++){
				newCounts += Math.exp(-b*
				Math.pow((oldChannel-newChannel)/width,2));
			}
			newCounts *= scaleFactor;
			outSpectrum[newChannel] = newCounts;
		}
	}
	

	
	public double [] getFilteredSpectrum(){
		return outSpectrum;
	}
	
	/**
	 * arg1 = width, arg2= infile.dat, arg3=outfile.dat
	 */
	public static void main(String[] args) {
		double width = Double.parseDouble(args[0]);
		File infile = new File(args[1]);
		File outfile = new File(args[2]);
		DatFile inDat = new DatFile(infile);
		GaussianFilter gf = new GaussianFilter(inDat.getData(), width);
		new DatFile(outfile, gf.getFilteredSpectrum());
	}
}

