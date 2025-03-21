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
 * PIDplotTUNL.java
 *
 * Based on PIDplotYaleOld, but modified for the FP detector
 * at TUNL.
 * 
 * Created on September 18, 2001, 8:43 PM
 */
package dwvisser;
import java.io.*;
import java.util.*;
import javax.swing.*;

import dwvisser.nuclear.Absorber;
import dwvisser.nuclear.EnergyLoss;
import dwvisser.nuclear.Gas;
import dwvisser.nuclear.NuclearException;
import dwvisser.nuclear.Nucleus;
import dwvisser.nuclear.Reaction;
import dwvisser.nuclear.Solid;

import java.awt.*;
import java.awt.event.*;

/**
 * This class will execute a process to simulate the focal plane detector.
 * It is based on the code written by Kazim Yildiz for the Vax machines,
 * which are now dying.  It was too difficult and time-consuming to figure
 * out why Kazim's code wouldn't run properly on the Solaris machines.
 *
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public final class PIDplotTUNL {

	static final int NUM_ABSORBERS = 18;
	static final int MAX_RXNS = 50;

	//static final int ANODE_INDEX=4;
	static final int SCINT_INDEX = 13;
	static final int DELE_INDEX = 5;
	static final int FW_INDEX = 3;

	static final String SET_COLOR = " grap/set txci ";

	/* These gas thicknesses in cm were taken directly from Kazim's code
	 * He doesn't explicitly account for the 45 degrees incidence on the
	 * detector in his code.  I do, so these have to be divided by cos(45deg)
	 * when I produce the absorber objects below.*/
	static double[] gasThickness = { 1.41, 3.68, 7.07, 34.2, 4.53 };
	//isobutane in cm
	static Absorber[] absorbers = new Absorber[NUM_ABSORBERS];
	static EnergyLoss[] eloss = new EnergyLoss[NUM_ABSORBERS];
	static final String[] SCINT_ELEMENTS = { "C", "H" };
	static final double[] SCINT_FRACTIONS = { 10, 11 };

	/* places to store results */
	static double[] radius = new double[MAX_RXNS]; //start radius(?)
	static int[] maxEnergyIndex = new int[MAX_RXNS];
	static boolean[] firstEnergy = new boolean[MAX_RXNS];
	static double[][] rho = new double[MAX_RXNS][MAX_RXNS];
	static double[][] Eproj = new double[MAX_RXNS][MAX_RXNS];
	static double[][][] losses = new double[MAX_RXNS][MAX_RXNS][NUM_ABSORBERS];

	/* given by user */
	static String title;
	static Nucleus beam;
	static double Tbeam, Bfield, angle, rhoMin, rhoMax, pressure;
	//static Absorber inFoil, scintFoil;
	static java.util.List reactions = new ArrayList();
	static int[] Q = new int[MAX_RXNS];
	static int reactionCount = 0;

	/**
	 * <p>This code calculates PID plots
	 * for the TUNL Enge spectrograph focal plane detector. The
	 * energy loss regions used for calculation are as follows:</p>
	 * 
	 * <dl>
	 * <dt>0</dt><dd>entrance window, 0.5 mil Kapton</dd>
	 * <dt>1</dt><dd>"dead" gas, 0.225"</dd>
	 * <dt>2</dt><dd>cathode foil, 0.125 mil aluminized mylar</dd>
	 * <dt>3</dt><dd>"dead" gas and front position, 0.25"</dd>
	 * <dt>4</dt><dd>ground foil, 0.125 mil aluminized mylar</dd>
	 * <dt>5</dt><dd>delta-E gas, 1.795"</dd>
	 * <dt>6</dt><dd>ground foil, 0.125 mil aluminized mylar</dd>
	 * <dt>7</dt><dd>"dead" gas, 0.225"</dd>
	 * <dt>8</dt><dd>cathode foil, 0.125 mil aluminized mylar</dd>
	 * <dt>9</dt><dd>"dead" gas and rear position, 0.25"</dd>
	 * <dt>10</dt><dd>ground foil, 0.125 mil aluminized mylar</dd>
	 * <dt>11</dt><dd>"dead" gas, 0.67"</dd>
	 * <dt>12</dt><dd>Al foil on scintillator, 1 mil</dd>
	 * <dt>13</dt><dd>BC-404 scintillator, 0.25"</dd>
	 * </dl>
	 * 
	 * @param name the name of the input specification file
	 */
	public PIDplotTUNL(String name) {
		title = name;
		//        new EnergyLoss();//initialize stopping data
		try {
			/* 0.25*25.4 mm, and 1.032 g/cc density */
			setupKapton();
			setupMylarFoils();
			setScintFoil();
			absorbers[SCINT_INDEX] = new Solid(0.25 * 25.4 * 1032 /*970.0*/
			, Absorber.MILLIGRAM_CM2, SCINT_ELEMENTS, SCINT_FRACTIONS);
			//scintillator
		} catch (NuclearException ne) {
			ne.printStackTrace(System.err);
		}
	}

	/**
	 * Sets initial values. Assumes isobutane gas in the detector.
	 *
	 * @param beam species of beam
	 * @param Tbeam kinetic energy in MeV
	 * @param Bfield of spectrometer in kG
	 * @param angle of spectrometer in degrees
	 * @param rhoMin lower limit of detector radius
	 * @param rhoMax upper limit of detector radius
	 * @param pressure gas pressure in torr
	 */
	public void initialize(
		Nucleus beam,
		double Tbeam,
		double Bfield,
		double angle,
		double rhoMin,
		double rhoMax,
		double pressure) {
		PIDplotTUNL.beam = beam;
		PIDplotTUNL.Tbeam = Tbeam;
		PIDplotTUNL.Bfield = Bfield;
		PIDplotTUNL.angle = angle;
		PIDplotTUNL.rhoMin = rhoMin;
		PIDplotTUNL.rhoMax = rhoMax;
		PIDplotTUNL.pressure = pressure;
	}

	private JFrame window;
	private void drawWindow() {
		System.out.println("Drawing Window...");
		window = new JFrame("PIDplotTUNL -- TUNL Enge PID simulator");
		Container contents = window.getContentPane();
		contents.setLayout(new BorderLayout());
		JMenuBar mb = new JMenuBar();
		JMenu file = new JMenu("File", true);
		file.add(new FileOpenAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
		file.add(new FileQuitAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
		mb.add(file);
		window.setJMenuBar(mb);
		window.setSize(320, 80);
		//window.pack();
		window.setResizable(false);
		window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				window.dispose();
				System.exit(0);
			}
		});
		window.show();
	}

	public class FileQuitAction extends AbstractAction {
		public FileQuitAction() {
			super("Quit");
		}
		public void actionPerformed(ActionEvent ae) {
			System.exit(0);
		}
	}

	public class FileOpenAction extends AbstractAction {
		public FileOpenAction() {
			super("Open...");
		}
		public void actionPerformed(ActionEvent ae) {
			File in = getFile();
			if (in != null) {
				window.hide();
				window = null;
				System.gc();
				simSpecFile(in);
			}
		}
	}

	private File lastFile;
	/**
	 * Read in an unspecified file by opening up a dialog box.
	 *
	 * @param  mode  whether to open or reload
	 * @return  <code>true</code> if successful, <code>false</code> if not
	 */
	File getFile() {
		JFileChooser jfile;
		if (lastFile == null) {
			jfile = new JFileChooser();
		} else {
			jfile = new JFileChooser(lastFile);
		}
		int option = jfile.showOpenDialog(window);
		// dont do anything if it was cancel
		if (option == JFileChooser.APPROVE_OPTION
			&& jfile.getSelectedFile() != null) {
			lastFile = jfile.getSelectedFile();
			return lastFile;
		}
		return null;
	}

	/*public void setEntranceFoil(Absorber a){
	    absorbers[0]=a;
	}*/

	static final double SCINT_FOIL_THICK = 1; //in mils
	public void setScintFoil() throws NuclearException {
		absorbers[12] = new Solid(SCINT_FOIL_THICK, Absorber.MIL, "Al");
	}

	static final double INIT_DRIFT_LENGTH = 0.225 * 2.54; //in cm
	//init also works for ground to cathode section
	static final double POSN_DRIFT_LENGTH = 0.25 * 2.54; //in cm
	static final double DELE_DRIFT_LENGTH = 1.795 * 2.54; //in cm
	static final double FINAL_DRIFT_LENGTH = 0.37 * 2.54; //in cm
	public void setupGas() throws NuclearException {
		//double c45 = Math.cos(Math.toRadians(45));
		Gas initialDrift = Gas.Isobutane(INIT_DRIFT_LENGTH, pressure);
		absorbers[1] = initialDrift;
		absorbers[7] = initialDrift;
		Gas posnDrift = Gas.Isobutane(POSN_DRIFT_LENGTH, pressure);
		absorbers[3] = posnDrift;
		absorbers[9] = posnDrift;
		absorbers[DELE_INDEX] = Gas.Isobutane(DELE_DRIFT_LENGTH, pressure);
		absorbers[11] = Gas.Isobutane(FINAL_DRIFT_LENGTH, pressure);
	}

	static final double MYLAR_FOIL_THICKNESS = 0.125; //in mils
	private void setupMylarFoils() throws NuclearException {
		//double c45 = Math.cos(Math.toRadians(45));
		Solid foil = Solid.Mylar(MYLAR_FOIL_THICKNESS, Absorber.MIL);
		absorbers[2] = foil;
		absorbers[4] = foil;
		absorbers[6] = foil;
		absorbers[8] = foil;
		absorbers[10] = foil;
	}

	static final double KAPTON_THICKNESS = 0.5; //in mils
	private void setupKapton() throws NuclearException {
		absorbers[0] = Solid.Kapton(KAPTON_THICKNESS, Absorber.MIL);
	}

	/**
	 * <p>After this is run, absorbers[] will contain 10 physical
	 * regions for energy loss.</p> 
	 */
	private void initializeElossObjects() {
		for (int i = 0; i < NUM_ABSORBERS; i++) {
			if (absorbers[i] != null)
				eloss[i] = new EnergyLoss(absorbers[i]);
		}
	}

	public void addReaction(
		Nucleus target,
		Nucleus projectile,
		int Qprojectile)
		throws Exception {
		if (reactionCount < MAX_RXNS) {
			Q[reactionCount] = Qprojectile;
			reactionCount++;
		} else
			throw new Exception(
				"No more than " + MAX_RXNS + " reactions, please.");
		reactions.add(new Reaction(target, beam, projectile, Tbeam, angle, 0));
	}

	/**
	 * Determine possible sets of rho values for various
	 * reactions and tabulate them, based on kinematics.
	 */
	public void calculateRhoValues() {
		double delRho = (rhoMax - rhoMin) / 20;
		for (int i = 0; i < reactionCount; i++) {
			Reaction rxn = (Reaction) reactions.get(i);
			double qbrho = rxn.getQBrho(0);
			double p0 = qbrho * Reaction.QBRHO_TO_P; // MeV/c
			radius[i] = qbrho / Q[i] / Bfield;
			int energyIndex = 0;
			maxEnergyIndex[i] = 0;
			if (radius[i] > rhoMax) {
				firstEnergy[i] = false;
				double r = radius[i];
				do {
					r -= delRho;
				} while (r > rhoMax);
				energyIndex = 0;
				do {
					rho[i][energyIndex] = r;
					double qbr = Q[i] * Bfield * r;
					double pi = qbr * Reaction.QBRHO_TO_P; // MeV/c
					double m3 = rxn.getProjectile().getMass().value;
					Eproj[i][energyIndex] =
						rxn.getLabEnergyProjectile(0)
							* (Math.sqrt(m3 * m3 + pi * pi) - m3)
							/ (Math.sqrt(m3 * m3 + p0 * p0) - m3);
					energyIndex++;
					r -= delRho;
				} while (r > rhoMin && energyIndex <= MAX_RXNS);
				maxEnergyIndex[i] = energyIndex;
			} else if (radius[i] <= rhoMax && radius[i] >= rhoMin) {
				firstEnergy[i] = true;
				double r = radius[i];
				do {
					r -= delRho;
				} while (r > rhoMax);
				energyIndex = 0;
				do {
					rho[i][energyIndex] = r;
					double qbr = Q[i] * Bfield * r;
					double pi = qbr * Reaction.QBRHO_TO_P; // MeV/c
					double m3 = rxn.getProjectile().getMass().value;
					Eproj[i][energyIndex] =
						rxn.getLabEnergyProjectile(0)
							* (Math.sqrt(m3 * m3 + pi * pi) - m3)
							/ (Math.sqrt(m3 * m3 + p0 * p0) - m3);
					energyIndex++;
					r -= delRho;
				} while (r > rhoMin && energyIndex <= MAX_RXNS);
				maxEnergyIndex[i] = energyIndex;
			} else {
				firstEnergy[i] = false;
				maxEnergyIndex[i] = 0;
			}
		}
	}

	/**
	 * Calculate and tabulate energy losses in various 
	 * detector segments
	 */
	public void calculateElosses() {
		initializeElossObjects();
		double angle = Math.toRadians(45); //45 degrees incidence on detector
		for (int i = 0; i < reactionCount; i++) {
			Reaction rxn = (Reaction) reactions.get(i);
			for (int j = 0;
				j < maxEnergyIndex[i];
				j++) { //skipped if maxEnergyIndex=0
				double energyLeft = Eproj[i][j];
				for (int k = 0; k < NUM_ABSORBERS; k++) {
					if (eloss[k] != null && energyLeft > 0.0) {
						losses[i][j][k] =
							eloss[k].getEnergyLoss(
								rxn.getProjectile(),
								energyLeft,
								angle)
								/ 1000;
						if (k == SCINT_INDEX) {
							double temp = losses[i][j][k];
							losses[i][j][k] =
								eloss[k].getPlasticLightOutput(
									rxn.getProjectile(),
									energyLeft,
									angle);
							energyLeft -= temp;
						} else {
							energyLeft -= losses[i][j][k];
						}
					}
				}
			}
		}
	}

	/**
	 * Produces a text file in the .kumac format, which can be run by the PAW
	 *program from CernLib to produce screen graphics and postscript files.
	 */
	public void outputPaw(File path, String outName)
		throws java.io.IOException {
		FileWriter out = new FileWriter(new File(path, outName + ".kumac"));
		out.write(" macro plot\n");
		out.write(" fortran/file 50 " + outName + ".ps\n");
		out.write(" meta 50 -114\n");
		out.write(" his/del *\n");
		out.write(" gra/set ygti .3\n");
		out.write(" gra/set gsiz .3\n");
		out.write(" hi/crea/title_gl '" + title + "'\n");
		writeHistograms(out);
		out.write(" close 50\n");
		out.write(" return\n");
		out.write(" macro symbol x=4. y=60. num=8 shape=0\n");
		out.write(" sym=\"<[shape]\n");
		out.write(" gra/prim/text [x] [y] [num] 0.2 0. C\n");
		out.write(" gra/prim/text [x] [y] [sym] 0.4 0. C\n");
		out.write(" return\n");
		System.out.println("In order to see the graphics, run PAW.");
		System.out.println("At the 'PAW >' prompt, type: exec " + outName);
		System.out.println(
			"PAW will produce a postscript file: " + outName + ".ps");
		out.flush();
		out.close();
	}

	private int getColor(int Z) {
		int[] colors = { 1, 2, 4, 6, 3, 7 };
		return colors[Z % colors.length];
	}

	private void writeHistograms(FileWriter out) throws java.io.IOException {
		//Anode Vs. Position - ymax is retained from previous plot, posn range is known
		String name = "Delta-E vs. Energy";
		double xmax = 0.0;
		double ymax = 0.0;
		for (int i = 0; i < reactionCount; i++) {
			for (int j = 0; j < maxEnergyIndex[i]; j++) {
				if (losses[i][j][SCINT_INDEX] >= xmax) {
					xmax = losses[i][j][SCINT_INDEX];
				}
				if (losses[i][j][DELE_INDEX] >= ymax) {
					ymax = losses[i][j][DELE_INDEX];
				}
			}
			if (xmax == 0.0)
				xmax = 1.0;
			if (ymax == 0.0)
				ymax = 1.0;
		}
		out.write(" kuip/wait '<CR> to view " + name + "' 0\n");
		out.write(SET_COLOR + " 1\n");
		out.write(
			" his/crea/2dhisto 200 '"
				+ name
				+ "' 10 0. "
				+ (xmax * 1.05)
				+ " 10 0. "
				+ (ymax * 1.05)
				+ "\n");
		out.write(" hi/plot 200\n");
		for (int i = 0; i < reactionCount; i++) {
			Reaction rxn = (Reaction) reactions.get(i);
			int color = getColor(rxn.getProjectile().Z);
			out.write(SET_COLOR + " " + color + "\n");
			for (int j = 0; j < maxEnergyIndex[i]; j++) {
				int shape;
				if (firstEnergy[i] && j == 0) {
					shape = 1;
				} else if (Q[i] == rxn.getProjectile().Z) {
					shape = 0;
				} else {
					shape = 2;
				}
				if (losses[i][j][FW_INDEX] > 0.0) {
					out.write(
						" exec symbol "
							+ losses[i][j][SCINT_INDEX]
							+ " "
							+ losses[i][j][DELE_INDEX]
							+ " "
							+ rxn.getProjectile().A
							+ " "
							+ shape
							+ "\n");
				}
			}
		}
		/* Delta-E Vs. Position - ymax is retained from 
		 * previous plot, posn range is known */
		name = "Delta-E vs. Position";
		out.write(" kuip/wait '<CR> to view " + name + "' 0\n");
		out.write(SET_COLOR + " 1\n");
		out.write(
			" his/crea/2dhisto 500 '"
				+ name
				+ "' 10 "
				+ (rhoMin - 1)
				+ " "
				+ (rhoMax + 1)
				+ " 10 0. "
				+ (ymax * 1.05)
				+ "\n");
		out.write(" hi/plot 500\n");
		for (int i = 0; i < reactionCount; i++) {
			Reaction rxn = (Reaction) reactions.get(i);
			int color = getColor(rxn.getProjectile().Z);
			out.write(SET_COLOR + " " + color + "\n");
			for (int j = 0; j < maxEnergyIndex[i]; j++) {
				int shape;
				if (firstEnergy[i] && j == 0) {
					shape = 1;
				} else if (Q[i] == rxn.getProjectile().Z) {
					shape = 0;
				} else {
					shape = 2;
				}
				if (losses[i][j][FW_INDEX] > 0.0) {
					out.write(
						" exec symbol "
							+ rho[i][j]
							+ " "
							+ losses[i][j][DELE_INDEX]
							+ " "
							+ rxn.getProjectile().A
							+ " "
							+ shape
							+ "\n");
				}
			}
		}
		//Scintillator Vs. Position-- x known since position
		name = "Energy vs. Position";
		for (int i = 0; i < reactionCount; i++) {
			for (int j = 0; j < maxEnergyIndex[i]; j++) {
				if (losses[i][j][SCINT_INDEX] >= ymax)
					ymax = losses[i][j][SCINT_INDEX];
			}
			if (xmax == 0.0)
				xmax = 1.0;
			if (ymax == 0.0)
				ymax = 1.0;
		}
		out.write(" kuip/wait '<CR> to view " + name + "' 0\n");
		out.write(SET_COLOR + " 1\n");
		out.write(
			" his/crea/2dhisto 600 '"
				+ name
				+ "' 10 "
				+ (rhoMin - 1)
				+ " "
				+ (rhoMax + 1)
				+ " 10 0. "
				+ (ymax * 1.05)
				+ "\n");
		out.write(" hi/plot 600\n");
		for (int i = 0; i < reactionCount; i++) {
			Reaction rxn = (Reaction) reactions.get(i);
			int color = getColor(rxn.getProjectile().Z);
			out.write(SET_COLOR + " " + color + "\n");
			for (int j = 0; j < maxEnergyIndex[i]; j++) {
				int shape;
				if (firstEnergy[i] && j == 0) {
					shape = 1;
				} else if (Q[i] == rxn.getProjectile().Z) {
					shape = 0;
				} else {
					shape = 2;
				}
				if (losses[i][j][FW_INDEX] > 0.0) {
					out.write(
						" exec symbol "
							+ rho[i][j]
							+ " "
							+ losses[i][j][SCINT_INDEX]
							+ " "
							+ rxn.getProjectile().A
							+ " "
							+ shape
							+ "\n");
				}
			}
		}
	}

	private static void simSpecFile(File in) {
		PIDplotTUNL det = null;
		if (in == null || !in.exists()) {
			System.out.println("No input file specified. To run, open one.");
			det = new PIDplotTUNL("To run, open an input file.");
			det.drawWindow();
			return; //quits out to give open file a chance
		}
		System.out.println("Processing input file: " + in.getAbsolutePath());
		System.out.println("Positions in cm, most energies in MeV.");
		System.out.println("Scintillator light output in units such that");
		System.out.println("an 8.78 alpha = 30.");
		try {
			LineNumberReader lr = new LineNumberReader(new FileReader(in));
			StreamTokenizer st = new StreamTokenizer(new BufferedReader(lr));
			st.eolIsSignificant(false); //treat end of line as white space
			st.commentChar('#'); //ignore end of line comments after '#'
			st.wordChars('/', '/'); //slash can be part of words
			st.wordChars('_', '_'); //underscore can be part of words
			st.nextToken();
			double _Bfield = readDouble(st);
			st.nextToken();
			double _angle = readDouble(st);
			st.nextToken();
			double _rhoMin = readDouble(st);
			st.nextToken();
			double _rhoMax = readDouble(st);
			st.nextToken();
			double _pressure = readDouble(st);
			st.nextToken();
			int _Z = readInteger(st);
			st.nextToken();
			int _A = readInteger(st);
			st.nextToken();
			double _energy = readDouble(st);
			boolean firstReaction = true;
			st.nextToken();
			do {
				int _ztarg = readInteger(st);
				st.nextToken();
				int _atarg = readInteger(st);
				st.nextToken();
				int _zproj = readInteger(st);
				st.nextToken();
				int _aproj = readInteger(st);
				st.nextToken();
				int _qproj = readInteger(st);
				Nucleus target = new Nucleus(_ztarg, _atarg);
				Nucleus projectile = new Nucleus(_zproj, _aproj);
				if (firstReaction) {
					Nucleus beam = new Nucleus(_Z, _A);
					String title =
						_energy
							+ " MeV "
							+ target
							+ "("
							+ beam
							+ ","
							+ projectile
							+ "), "
							+ _angle
							+ " deg, "
							+ _pressure
							+ " torr, "
							+ _Bfield
							+ " kG";
					det = new PIDplotTUNL(title);
					det.initialize(
						beam,
						_energy,
						_Bfield,
						_angle,
						_rhoMin,
						_rhoMax,
						_pressure);
					det.setupGas();
					det.initializeElossObjects();
				}
				firstReaction = false;
				det.addReaction(target, projectile, _qproj);
				st.nextToken();
			} while (st.ttype != StreamTokenizer.TT_EOF);
			det.calculateRhoValues();
			det.calculateElosses();
			String outName = in.getName();
			outName = outName.substring(0, outName.lastIndexOf('.'));
			det.outputPaw(in.getParentFile(), outName);
		} catch (IOException e) {
			System.err.println(e);
		} catch (NuclearException e) {
			System.err.println(e);
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	private static int readInteger(StreamTokenizer st) throws IOException {
		if (st.ttype != StreamTokenizer.TT_NUMBER)
			throw new IOException(
				".readInteger(): Wrong token type: " + st.ttype);
		return (int) st.nval;
	}

	private static double readDouble(StreamTokenizer st) throws IOException {
		if (st.ttype != StreamTokenizer.TT_NUMBER)
			throw new IOException(
				".readInteger(): Wrong token type: " + st.ttype);
		return st.nval;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println(e);
		}
		File in = null;
		if (args.length > 0) {
			in = new File(args[0]);
		}
		simSpecFile(in); //null flags that a window should be opened
	}

}
