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
package net.sourceforge.nukesim;
import jade.JADE;
import jade.physics.Angle;
import jade.physics.ElectricCharge;
import jade.physics.Energy;
import jade.physics.Length;
import jade.physics.MagneticFluxDensity;
import jade.physics.Quantity;
import jade.physics.models.RelativisticModel;
import jade.units.Unit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.help.CSH;
import javax.help.HelpSet;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.Nucleus;
import net.sourceforge.nukesim.nuclear.NukeUnits;
import net.sourceforge.nukesim.nuclear.Reaction;
import net.sourceforge.nukesim.nuclear.graphics.RadiusRange;
import net.sourceforge.nukesim.nuclear.graphics.ScaleCanvas;
import net.sourceforge.nukesim.nuclear.graphics.SpectrumCanvas;
import net.sourceforge.nukesim.nuclear.swing.ComponentPrintable;

/**
 * This class will execute a process to simulate the focal plane 
 * detector. It is based on the code written by Kazim Yildiz for  
 * Vax machines.
 *
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public class SpecPlot implements RadiusRange,FileOpener,NukeUnits {
	private ActionListener getHelpListener() {
		final HelpSet hs;
		final String helpsetName = "help/specplot/specplot.hs";
		try {
			final URL hsURL =
				getClass().getClassLoader().getResource(helpsetName);
			hs = new HelpSet(null, hsURL);
		} catch (Exception ee) {
			return null;
		}
		return new CSH.DisplayHelpFromSource(hs.createHelpBroker());
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
	
	public void openFile(File in){
		if (in != null) {
			simSpecFile(in);
		}		
	}
	
	private class FileReloadAction extends AbstractAction {
		public FileReloadAction() {
			super("Reload");
		}
		public void actionPerformed(ActionEvent ae) {
			if (lastFile != null) {
				simSpecFile(lastFile);
			}
		}
	}

	private static final int MAX_RXNS = 50;
	private File lastFile = null;
	private PageFormat mPageFormat;
	private final Container contents;
	private Nucleus beam;
	private Energy eBeam;
	private MagneticFluxDensity bField;
	private Angle angle;
	private Length rhoMin, rhoMax;
	private java.util.List reactions = new ArrayList();
	private ElectricCharge [] charge = new ElectricCharge[MAX_RXNS];
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
	private JMenuItem mReload=new JMenuItem();
	private final JMenu file = new JMenu("File", true);
	
	{
		contents = window.getContentPane();
		contents.setLayout(new BorderLayout());
		ClassLoader cl = getClass().getClassLoader();
		window.setIconImage(
			new ImageIcon(cl.getResource("net/sourceforge/nukesim/plotter96.png")).getImage());
		final JMenuBar mb = new JMenuBar();
		file.add(new FileOpenAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
		mReload.setAction(new FileReloadAction());
		mReload.setEnabled(false);
		file.add(mReload);
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
		file.addSeparator();
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
		
	private final PreviousFileMenuItems prevFiles;

	/**
	 * Creates new PIDplotYaleOld.
	 */
	public SpecPlot() {
		new SplashWindow(
			null,
			10000,
			"net/sourceforge/nukesim/plotter96.png",
			"SpecPlot",
			"v 1.2");
		createLicenseDialog();
		prevFiles=new PreviousFileMenuItems(getClass(), this, file, 4);
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
		Energy Tbeam,
		MagneticFluxDensity Bfield,
		Angle angle,
		Length rMin,
		Length rMax) {
		setBeam(beam);
		setTbeam(Tbeam);
		setBfield(Bfield);
		setAngle(angle);
		setRange(rMin, rMax);
		P_input.repaint();
	}
	
	private String valueText(Quantity value, Unit units) {
		final StringBuffer rval=new StringBuffer();
		if (units != null) {
			final int unitLength = units.toString().length();
			if (value.approxEquals(value.multiply(0.0))){
				rval.append(0);
			} else {
				rval.append(value.toText(units));
				rval.delete(rval.length() - unitLength,rval.length());			
			}
		} else {
			rval.append(value.toString());
		}
		return Double.toString(Double.parseDouble(rval.toString()));
	}

	private synchronized void setRange(Length r1, Length r2) {
		if (r1.doubleValue(cm)<r2.doubleValue(cm)){
			rhoMin = r1;
			rhoMax = r2;
		} else {
			rhoMin = r2;
			rhoMax = r1;
		}
		t_rhoMin.setText(valueText(rhoMin,cm));
		t_rhoMax.setText(valueText(rhoMax,cm));
	}

	private synchronized void setAngle(Angle a) {
		angle = a;
		t_angle.setText(valueText(a,deg));
	}

	private synchronized void setBeam(Nucleus b) {
		beam = b;
	}

	private synchronized void setTbeam(Energy tb) {
		eBeam = tb;
		t_Tbeam.setText(valueText(tb,MeV));
	}

	private synchronized void setBfield(MagneticFluxDensity b) {
		bField = b;
		this.t_Bfield.setText(valueText(b,kgauss));
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
					new JLabel(r.getProjectile() + "[" + (int)Math.round(charge[i].doubleValue(e)) + "+]");
				ion.setForeground(Color.red);
				jp.add(ion);
				final Nucleus residual=r.getResidual();
				jp.add(new JLabel(")" + residual + " "));
				P_labels.add(jp);
				try {
					canvas[i] = new SpectrumCanvas(this);
					canvas[i].setRadii(getRadii(i));
					final boolean fake=excitationsTable.get(residual.toString())==null;
					canvas[i].setEx(getEx(i),fake);
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
		Quantity temp = Quantity.valueOf(Double.parseDouble(t_rhoMin.getText()),cm);
		if (!temp.approxEquals(rhoMin)) {
			rhoMin = Length.lengthOf(temp);
			redraw = true;
		}
		temp = Quantity.valueOf(Double.parseDouble(t_rhoMax.getText()),cm);
		if (temp != rhoMax) {
			rhoMax = Length.lengthOf(temp);
			redraw = true;
		}
		temp = Quantity.valueOf(Double.parseDouble(t_Tbeam.getText()),MeV);
		if (!eBeam.approxEquals(temp)) {
			eBeam = Energy.energyOf(temp);
			redraw = true;
			newReactions = true;
			recalculate = true;
		}
		temp = Quantity.valueOf(Double.parseDouble(t_angle.getText()),deg);
		if (angle != temp) {
			angle = Angle.angleOf(temp);
			redraw = true;
			newReactions = true;
			recalculate = true;
		}
		temp = Quantity.valueOf(Double.parseDouble(t_Bfield.getText()),kgauss);
		if (bField != temp) {
			bField = MagneticFluxDensity.magneticFluxDensityOf(temp);
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
			final Reaction react = (Reaction) reactions.get(i);
			try {
				reactions.set(i, new Reaction(react, eBeam, angle, 0));
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

	private Length[] getRadii(int rxnNum)
		throws NuclearException, KinematicsException {
		final Reaction react = (Reaction) reactions.get(rxnNum);
		final Energy [] excite=getEx(rxnNum);
		int size=excite.length;
		final Length [] temp=new Length[size];
		for (int i = 0; i < size; i++) {
			final Energy eExcite =excite[i];
			try {
				final Reaction rEx = new Reaction(react, eExcite);
				if (rEx.getAngleDegeneracy() > 0) {
					temp[i] = Length.lengthOf(rEx.getQBrho(0).divide(charge[rxnNum]).divide(bField));
				} else {
					warnings.append(react.toString()).append(": Ex=").append(eExcite);
					warnings.append(" had no solutions.\n");
					size--;
				}
			} catch (KinematicsException ke) {
				warnings.append(react.toString()).append(": Ex=").append(eExcite);
				warnings.append(" failed: ").append(ke.getMessage()).append('\n');
				size--;
			}
		}
		Length [] rval=new Length[size];
		System.arraycopy(temp,0,rval,0,size);
		return rval;
	}

	private Energy[] getEx(int rxnNum) {
		final Reaction react = (Reaction) reactions.get(rxnNum);
		final String nucleus = react.getResidual().toString();
		final List list = (List) excitationsTable.get(nucleus);
		final Energy[] rval =
			(list != null) ? new Energy[list.size()] : new Energy[20];
		if (list != null) {
			//final Energy[] temp = new Energy[list.size()];
			list.toArray(rval);
			/*for (int i = 0; i < rval.length; i++) {
				rval[i] = temp[i].doubleValue();
			}*/
		} else {
			for (int i=0; i<rval.length; i++){
				rval[i]=Energy.energyOf(Quantity.valueOf(i,MeV));
			}
		}
		return rval;
	}

	private void addReaction(
		Nucleus target,
		Nucleus projectile,
		ElectricCharge Qprojectile)
		throws NuclearException {
		final int reactionCount = reactions.size();
		if (reactionCount < MAX_RXNS) {
			charge[reactionCount] = Qprojectile;
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
							tempVector.add(Energy.energyOf(
									Quantity.valueOf(readDouble(st),keV)));
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
				final MagneticFluxDensity _Bfield = MagneticFluxDensity.magneticFluxDensityOf(
						Quantity.valueOf(readDouble(st),kgauss));
				st.nextToken();
				final Angle _angle = Angle.angleOf(Quantity.valueOf(readDouble(st),deg));
				st.nextToken();
				final Length _rhoMin = Length.lengthOf(Quantity.valueOf(readDouble(st),cm));
				st.nextToken();
				final Length _rhoMax = Length.lengthOf(Quantity.valueOf(readDouble(st),cm));
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
				final Energy _energy = Energy.energyOf(Quantity.valueOf(readDouble(st),MeV));
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
					final ElectricCharge _qproj = ElectricCharge.electricChargeOf(ElectricCharge.ELEMENTARY.multiply(readInteger(st)));
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
				lr.close();
				mReload.setEnabled(true);
				prevFiles.addPrevFile(in);
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
	public Length getRhoMin() {
		return rhoMin;
	}

	/**
	 * @return the maximumc radius in cm to plot
	 */
	public Length getRhoMax() {
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
		if (!VERSION.equals(prefs.get(key,defaultVal))){
			showLicense();
			prefs.put(key,VERSION);
		}
	}
	private final Preferences prefs=Preferences.userNodeForPackage(getClass());
	
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
		JADE.initialize();
		RelativisticModel.select();
		final SpecPlot fpp = new SpecPlot();
		fpp.simSpecFile(in);
	}
}
