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
package dwvisser;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.prefs.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
//import javax.help.*;
//import java.net.URL;
import javax.swing.*;

import dwvisser.nuclear.KinematicsException;
import dwvisser.nuclear.NuclearException;
import dwvisser.nuclear.Nucleus;
import dwvisser.nuclear.Reaction;
import dwvisser.nuclear.graphics.RadiusRange;
import dwvisser.nuclear.graphics.SpectrumCanvas;
import dwvisser.nuclear.graphics.ScaleCanvas;
import dwvisser.nuclear.swing.ComponentPrintable;

/**
 * This class will execute a process to simulate the focal plane 
 * detector. It is based on the code written by Kazim Yildiz for  
 * Vax machines.
 *
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public class SpecPlot implements RadiusRange {
	private ActionListener getHelpListener() {
		//final HelpSet hs;
		//final String helpsetName = "help/specplot/specplot.hs";
		/*try {
			final URL hsURL =
				//getClass().getClassLoader().getResource(helpsetName);
		//	hs = new HelpSet(null, hsURL);
		} catch (Exception ee) */{
			return null;
		}
		//return new CSH.DisplayHelpFromSource(hs.createHelpBroker());
	}

	private class Redraw implements ActionListener {
		/**
		 * Redraws the window when this listener gets an event.
		 */
		public void actionPerformed(ActionEvent ae) {
			if (getTextFromFields()) {
				window.repaint();
			}
		}
	}
	
	private class FilePrintAction extends AbstractAction {

		/** Creates new FilePrintAction */
		FilePrintAction() {
			super("Print");
		}

		public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
			final PrinterJob pj = PrinterJob.getPrinterJob();
			final ComponentPrintable cp = new ComponentPrintable(contents);
			pj.setPrintable(cp, mPageFormat);
			if (pj.printDialog()) {
				try {
					pj.print();
				} catch (PrinterException e) {
					error(e);
				}
			}
		}

	}

	private class FilePageSetupAction extends AbstractAction {
		FilePageSetupAction() {
			super("Page setup...");
		}
		public void actionPerformed(ActionEvent ae) {
			final PrinterJob pj = PrinterJob.getPrinterJob();
			mPageFormat = pj.pageDialog(mPageFormat);
		}
	}

	private class FileQuitAction extends AbstractAction {
		FileQuitAction() {
			super("Quit");
		}
		public void actionPerformed(ActionEvent ae) {
			System.exit(0);
		}
	}

	private class FileOpenAction extends AbstractAction {
		public FileOpenAction() {
			super("Open...");
		}
		public void actionPerformed(ActionEvent ae) {
			final File in = getFile();
			if (in != null) {
				simSpecFile(in);
			}
		}
	}

	private static final int MAX_RXNS = 50;
	private File lastFile = null;
	private PageFormat mPageFormat;
	private final Container contents;
	private Nucleus beam;
	private double eBeam, bField, angle;
	private double rhoMin, rhoMax;
	private java.util.List reactions = new ArrayList();
	private int[] Q = new int[MAX_RXNS];
	private final static String TITLE = "SpecPlot -- Spectrograph Plotter";
	private final JFrame window = new JFrame(TITLE);
	private final JTextField t_rhoMin, t_rhoMax, t_Tbeam, t_angle, t_Bfield;
	private SpectrumCanvas[] canvas;
	private ScaleCanvas scale=new ScaleCanvas(this);
	private StringBuffer warnings=new StringBuffer();
	private JDialog licenseD;
	private static final int VERSION_MAJOR=1;
	private static final int VERSION_MINOR=2;
	private static final String VERSION=VERSION_MAJOR+"."+VERSION_MINOR;
	
	{
		contents = window.getContentPane();
		contents.setLayout(new BorderLayout());
		ClassLoader cl = getClass().getClassLoader();
		window.setIconImage(
			new ImageIcon(cl.getResource("dwvisser/plotter96.png")).getImage());
		final JMenuBar mb = new JMenuBar();
		final JMenu file = new JMenu("File", true);
		file.add(new FileOpenAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
		file.addSeparator();
		file.add(new FilePrintAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
		file.add(new FilePageSetupAction()).setAccelerator(
			KeyStroke.getKeyStroke(
				KeyEvent.VK_P,
				Event.CTRL_MASK | Event.SHIFT_MASK));
		file.addSeparator();
		file.add(new FileQuitAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
		mb.add(file);
		final JMenu help=new JMenu("Help");
		JMenuItem toc=new JMenuItem("Table of Contents...");
		toc.addActionListener(getHelpListener());
		help.add(toc);
		mb.add(help);
		window.setJMenuBar(mb);
	}
	private final Container P_reactions = Box.createVerticalBox();
	private final Container P_labels = Box.createVerticalBox();
	private final JPanel P_input = new JPanel(new FlowLayout());

	{
		//new BoxLayout(P_reactions,BoxLayout.Y_AXIS);
		//new BoxLayout(P_labels,BoxLayout.Y_AXIS);
		t_rhoMin = new JTextField(rhoMin + " ");
		t_rhoMin.addActionListener(new Redraw());
		t_rhoMax = new JTextField(rhoMax + " ");
		t_rhoMax.addActionListener(new Redraw());
		t_Tbeam = new JTextField(eBeam + " ");
		t_Tbeam.addActionListener(new Redraw());
		t_angle = new JTextField(angle + " ");
		t_angle.addActionListener(new Redraw());
		t_Bfield = new JTextField(bField + " ");
		t_Bfield.addActionListener(new Redraw());
		P_input.add(new JLabel("Min. \u03c1 [cm]"));
		P_input.add(t_rhoMin);
		P_input.add(new JLabel("Max. \u03c1 [cm]"));
		P_input.add(t_rhoMax);
		P_input.add(new JLabel("Beam Energy [MeV]"));
		P_input.add(t_Tbeam);
		P_input.add(new JLabel("Theta [deg]"));
		P_input.add(t_angle);
		P_input.add(new JLabel("B-field [kG]"));
		P_input.add(t_Bfield);
	}

	/* stores nucleus excitations keyed by AAZZ string where ZZ is 
	 * the element symbol */
	private final Map excitationsTable =
		Collections.synchronizedMap(new HashMap());

	/**
	 * Creates new PIDplotYaleOld.
	 */
	public SpecPlot() {
		new SplashWindow(
			null,
			10000,
			"dwvisser/plotter96.png",
			"SpecPlot",
			"v 1.2");
		createLicenseDialog();
	}

	private void error(Exception e) {
		JOptionPane.showMessageDialog(
			window,
			e.getMessage(),
			e.getClass().getName(),
			JOptionPane.ERROR_MESSAGE);
	}
	
	private void error(String e) {
		JOptionPane.showMessageDialog(
			window,
			e,
			"Kinematics Warning",
			JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Sets initial values. Assumes isobutane gas in the detector.
	 *
	 * @param beam species of beam
	 * @param Tbeam kinetic energy in MeV
	 * @param Bfield of spectrometer in kG
	 * @param angle of spectrometer in degrees
	 * @param rMin lower limit of detector radius
	 * @param rMax upper limit of detector radius
	 */
	private void initialize(
		Nucleus beam,
		double Tbeam,
		double Bfield,
		double angle,
		double rMin,
		double rMax) {
		setBeam(beam);
		setTbeam(Tbeam);
		setBfield(Bfield);
		setAngle(angle);
		setRange(rMin, rMax);
		P_input.repaint();
	}

	private synchronized void setRange(double r1, double r2) {
		rhoMin = Math.min(r1, r2);
		rhoMax = Math.max(r1, r2);
		t_rhoMin.setText(String.valueOf(rhoMin));
		t_rhoMax.setText(String.valueOf(rhoMax));
	}

	private synchronized void setAngle(double a) {
		angle = a;
		t_angle.setText(String.valueOf(a));
	}

	private synchronized void setBeam(Nucleus b) {
		beam = b;
	}

	private synchronized void setTbeam(double tb) {
		eBeam = tb;
		t_Tbeam.setText(String.valueOf(tb));
	}

	private synchronized void setBfield(double b) {
		bField = b;
		this.t_Bfield.setText(String.valueOf(b));
	}

	private void drawWindow() {
		contents.removeAll();
		P_labels.removeAll();
		P_reactions.removeAll();
		final int reactionCount = reactions.size();
		if (reactionCount > 0) {
			contents.add(P_labels, BorderLayout.WEST);
			contents.add(P_reactions, BorderLayout.CENTER);
			contents.add(P_input, BorderLayout.SOUTH);
			canvas = new SpectrumCanvas[reactionCount];
			for (int i = 0; i < reactionCount; i++) {
				final JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
				final Reaction r = (Reaction) reactions.get(i);
				jp.add(
					new JLabel(" " + r.getTarget() + "(" + r.getBeam() + ","));
				final JLabel ion =
					new JLabel(r.getProjectile() + "[" + Q[i] + "+]");
				ion.setForeground(Color.red);
				jp.add(ion);
				jp.add(new JLabel(")" + r.getResidual() + " "));
				P_labels.add(jp);
				try {
					canvas[i] = new SpectrumCanvas(this);
					canvas[i].setRadii(getRadii(i));
					canvas[i].setEx(getEx(i));
					P_reactions.add(canvas[i]);
				} catch (KinematicsException ke) {
					error(ke);
				} catch (NuclearException ke) {
					error(ke);
				}
			}
			printAnyWarnings();
			P_labels.add(scale.getScalePanel());
			P_reactions.add(scale);			
		}
		window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				window.dispose();
				System.exit(0);
			}
		});
		final PrinterJob pj = PrinterJob.getPrinterJob();
		mPageFormat = pj.defaultPage();
		mPageFormat.setOrientation(PageFormat.LANDSCAPE);
		final Runnable startGui = new Runnable() {
			public void run() {
				window.pack();
				if (window.getWidth()<300){
					window.setSize(300,window.getHeight());
				}
				if (!window.isShowing()) {
					window.show();
				}
			}
		};
		SwingUtilities.invokeLater(startGui);
		window.repaint();
	}

	/**
	 * Returns whether to redraw.
	 *
	 * @return whether it is necessary to redraw
	 */
	private boolean getTextFromFields() {
		boolean recalculate = false;
		boolean redraw = false;
		boolean newReactions = false;
		double temp = Double.parseDouble(t_rhoMin.getText());
		if (temp != rhoMin) {
			rhoMin = temp;
			redraw = true;
		}
		temp = Double.parseDouble(t_rhoMax.getText());
		if (temp != rhoMax) {
			rhoMax = temp;
			redraw = true;
		}
		temp = Double.parseDouble(t_Tbeam.getText());
		if (eBeam != temp) {
			eBeam = temp;
			redraw = true;
			newReactions = true;
			recalculate = true;
		}
		temp = Double.parseDouble(t_angle.getText());
		if (angle != temp) {
			angle = temp;
			redraw = true;
			newReactions = true;
			recalculate = true;
		}
		temp = Double.parseDouble(t_Bfield.getText());
		if (bField != temp) {
			bField = temp;
			redraw = true;
			recalculate = true;
		}
		if (newReactions) {
			changeReactions();
		}
		if (recalculate) {
			sendRadii();
		}
		return redraw;
	}

	private void changeReactions() {
		for (int i = 0; i < reactions.size(); i++) {
			final Reaction r = (Reaction) reactions.get(i);
			try {
				reactions.set(i, new Reaction(r, eBeam, angle, 0));
			} catch (KinematicsException ke) {
				error(ke);
			} catch (NuclearException ke) {
				error(ke);
			}
		}
	}

	private void sendRadii() {
		for (int i = 0; i < reactions.size(); i++) {
			try {
				canvas[i].setRadii(getRadii(i));
			} catch (KinematicsException ke) {
				error(ke);
			} catch (NuclearException ke) {
				error(ke);
			}
		}
		printAnyWarnings();
	}
	
	private void printAnyWarnings(){
		if (warnings.length()>0){
			error(warnings.toString());
			warnings.setLength(0);
		}		
	}

	private double[] getRadii(int reactionNumber)
		throws NuclearException, KinematicsException {
		final Reaction react = (Reaction) reactions.get(reactionNumber);
		final String nucleus = react.getResidual().toString();
		final List v = (List) excitationsTable.get(nucleus);
		final double[] temp =
			(v != null) ? new double[v.size()] : new double[1];
		int size=temp.length;
		for (int i = 0; i < temp.length; i++) {
			final double Ex =
				(v != null) ? ((Double) v.get(i)).doubleValue() : 0.0;
			try {
				final Reaction rEx = new Reaction(react, Ex);
				if (rEx.getAngleDegeneracy() > 0) {
					temp[i] = rEx.getQBrho(0) / Q[reactionNumber] / bField;
				} else {
					warnings.append(react.toString()).append(": Ex=").append(Ex);
					warnings.append(" had no solutions.\n");
					size--;
				}
			} catch (KinematicsException ke) {
				warnings.append(react.toString()).append(": Ex=").append(Ex);
				warnings.append(" failed: ").append(ke.getMessage()).append('\n');
				size--;
			}
		}
		double [] rval=new double[size];
		System.arraycopy(temp,0,rval,0,size);
		return rval;
	}

	private double[] getEx(int reactionNumber) {
		final Reaction react = (Reaction) reactions.get(reactionNumber);
		final String nucleus = react.getResidual().toString();
		final List v = (List) excitationsTable.get(nucleus);
		final double[] rval =
			(v != null) ? new double[v.size()] : new double[1];
		if (v != null) {
			final Double[] temp = new Double[v.size()];
			v.toArray(temp);
			for (int i = 0; i < rval.length; i++) {
				rval[i] = temp[i].doubleValue();
			}
		}
		return rval;
	}

	private void addReaction(
		Nucleus target,
		Nucleus projectile,
		int Qprojectile)
		throws NuclearException {
		final int reactionCount = reactions.size();
		if (reactionCount < MAX_RXNS) {
			Q[reactionCount] = Qprojectile;
		} else {
			throw new NuclearException(
				"No more than " + MAX_RXNS + " reactions, please.");
		}
		reactions.add(new Reaction(target, beam, projectile, eBeam, angle, 0));
	}

	private void readExcitations(File in) {
		try {
			final LineNumberReader lr =
				new LineNumberReader(new FileReader(in));
			final StreamTokenizer st =
				new StreamTokenizer(new BufferedReader(lr));
			st.eolIsSignificant(false); //treat end of line as white space
			st.commentChar('#'); //ignore end of line comments after '#'
			st.wordChars('/', '/'); //slash can be part of words
			st.wordChars('_', '_'); //underscore can be part of words
			st.nextToken();
			if (st.ttype == StreamTokenizer.TT_NUMBER) {
				do {
					final List tempVector = new ArrayList();
					final int a = readInteger(st);
					st.nextToken();
					final String element = readString(st);
					excitationsTable.put(a + element, tempVector);
					do {
						st.nextToken();
						if (st.ttype == StreamTokenizer.TT_NUMBER) {
							tempVector.add(new Double(readDouble(st)));
						}
					} while (st.ttype == StreamTokenizer.TT_NUMBER);
					if (st.ttype == StreamTokenizer.TT_WORD) {
						st.nextToken();
						//a word, usually 'end' after all excitations for a nucleus
					}
				}
				while (st.ttype != StreamTokenizer.TT_EOF);
				//if not EOF, assumed to be next A
			}
		} catch (IOException e) {
			error(e);
		} catch (Exception e) {
			error(e);
		}
	}

	private void simSpecFile(File in) {
		if (in == null || !in.exists()) {
			drawWindow();
		} else {
			try {
				reactions.clear();
				final LineNumberReader lr =
					new LineNumberReader(new FileReader(in));
				final StreamTokenizer st =
					new StreamTokenizer(new BufferedReader(lr));
				st.eolIsSignificant(false); //treat end of line as white space
				st.commentChar('#'); //ignore end of line comments after '#'
				st.wordChars('/', '/'); //slash can be part of words
				st.wordChars('_', '_'); //underscore can be part of words
				st.nextToken();
				final double _Bfield = readDouble(st);
				st.nextToken();
				final double _angle = readDouble(st);
				st.nextToken();
				final double _rhoMin = readDouble(st);
				st.nextToken();
				final double _rhoMax = readDouble(st);
				st.nextToken(); //final double _pressure = readDouble(st);
				st.nextToken(); //final double _blockerMils = readDouble(st);
				st.nextToken();
				//final String _blockerElement = readString(st);
				st.nextToken(); //final double _inMils = readDouble(st);
				st.nextToken(); //final double _outMils = readDouble(st);
				st.nextToken(); //final double _scintFoilMils = readDouble(st);
				st.nextToken();
				final int _Z = readInteger(st);
				st.nextToken();
				final int _A = readInteger(st);
				st.nextToken();
				final double _energy = readDouble(st);
				boolean firstReaction = true;
				int count = 0;
				st.nextToken();
				do {
					count++;
					final int _ztarg = readInteger(st);
					st.nextToken();
					final int _atarg = readInteger(st);
					st.nextToken();
					final int _zproj = readInteger(st);
					st.nextToken();
					final int _aproj = readInteger(st);
					st.nextToken();
					final int _qproj = readInteger(st);
					final Nucleus target = new Nucleus(_ztarg, _atarg);
					final Nucleus projectile = new Nucleus(_zproj, _aproj);
					if (firstReaction) {
						final Nucleus b = new Nucleus(_Z, _A); //beam
						initialize(
							b,
							_energy,
							_Bfield,
							_angle,
							_rhoMin,
							_rhoMax);
					}
					firstReaction = false;
					addReaction(target, projectile, _qproj);
					st.nextToken();
				} while (st.ttype != StreamTokenizer.TT_EOF);
				readExcitations(
					new File(in.getParentFile(), "excitations.dat"));
				drawWindow();
			} catch (IOException e) {
				error(e);
			} catch (NuclearException e) {
				error(e);
			}
		}
	}

	private static int readInteger(StreamTokenizer st) throws IOException {
		if (st.ttype != StreamTokenizer.TT_NUMBER) {
			throw new IOException(
				".readInteger(): Wrong token type: " + st.ttype);
		}
		return (int) st.nval;
	}

	private static double readDouble(StreamTokenizer st) throws IOException {
		if (st.ttype != StreamTokenizer.TT_NUMBER) {
			throw new IOException(
				".readInteger(): Wrong token type: " + st.ttype);
		}
		return st.nval;
	}

	private static String readString(StreamTokenizer st) throws IOException {
		if (st.ttype != StreamTokenizer.TT_WORD) {
			throw new IOException(
				".readString(): Wrong token type: " + st.ttype);
		}
		return st.sval;
	}

	/**
	 * @return the minimum radius in cm to plot
	 */
	public double getRhoMin() {
		return rhoMin;
	}

	/**
	 * @return the maximumc radius in cm to plot
	 */
	public double getRhoMax() {
		return rhoMax;
	}

	/**
	 * Read in an unspecified file by opening up a dialog box.
	 *
	 * @return  <code>true</code> if successful, 
	 * <code>false</code> if not
	 */
	private File getFile() {
		final JFileChooser jfile = new JFileChooser(lastFile);
		final int option = jfile.showOpenDialog(window);
		// dont do anything if it was cancel
		if (option == JFileChooser.APPROVE_OPTION
			&& jfile.getSelectedFile() != null) {
			lastFile = jfile.getSelectedFile();
			return lastFile;
		}
		return null;
	}
	
	private final void createLicenseDialog() {
		licenseD =
			new JDialog(
				window,
				"University of Illinois/NCSA Open Source License",
				false);
		final Container contents = licenseD.getContentPane();
		licenseD.setResizable(true);
		licenseD.setLocation(20, 50);
		contents.setLayout(new BorderLayout());
		final JPanel center = new JPanel(new GridLayout(0, 1));
		final InputStream license_in =
			getClass().getClassLoader().getResourceAsStream("license.txt");
		final Reader reader = new InputStreamReader(license_in);
		String text = "";
		int length = 0;
		final char[] textarray = new char[2000];
		try {
			length = reader.read(textarray);
		} catch (IOException e) {
			System.err.println(e);
		}
		text = new String(textarray, 0, length);
		final JTextArea textarea = new JTextArea(text);
		center.add(new JScrollPane(textarea));
		contents.add(center, BorderLayout.CENTER);
		final JPanel south = new JPanel(new GridLayout(1, 0));
		contents.add(south, BorderLayout.SOUTH);
		final JButton bok = new JButton("OK");
		bok.setActionCommand("l_ok");
		bok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				licenseD.hide();
			}
		});
		south.add(bok);
		licenseD.pack();
		final Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
		licenseD.setSize(licenseD.getWidth(),screen.height/2);
		final int posx=20;
		licenseD.setLocation(posx,screen.height/4);
		/* Recieves events for closing the dialog box and closes it. */
		licenseD.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				licenseD.dispose();
			}
		});
		final String defaultVal="notseen";
		final String key="SpecPlotLicense";
		final Preferences helpnode=Preferences.userNodeForPackage(getClass());
		if (!VERSION.equals(helpnode.get(key,defaultVal))){
			showLicense();
			helpnode.put(key,VERSION);
		}
	}
	
	/**
	 * Show Jam's open source license text.
	 */
	private void showLicense() {
		licenseD.show();
	}




	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		File in = null;
		final String linux = "Linux";
		final String kunststoff =
			"com.incors.plaf.kunststoff.KunststoffLookAndFeel";
		boolean useKunststoff = linux.equals(System.getProperty("os.name"));
		if (useKunststoff) {
			try {
				UIManager.setLookAndFeel(kunststoff);
			} catch (ClassNotFoundException e) {
				useKunststoff = false;
			} catch (Exception e) { //all other exceptions
				final String title = "Error setting GUI appearance";
				JOptionPane.showMessageDialog(
					null,
					e.getMessage(),
					title,
					JOptionPane.WARNING_MESSAGE);
			}
		}
		if (!useKunststoff) {
			try {
				UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				final String title = "Error setting GUI appearance";
				JOptionPane.showMessageDialog(
					null,
					e.getMessage(),
					title,
					JOptionPane.WARNING_MESSAGE);
			}
		}
		if (args.length > 0) {
			in = new File(args[0]);
		}
		final SpecPlot fpp = new SpecPlot();
		fpp.simSpecFile(in);
	}
}
