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
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

//import javax.help.CSH;
//import javax.help.HelpSet;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dwvisser.analysis.spanc.AddCalibrationPeakDialog;
import dwvisser.analysis.spanc.AddOutputPeakDialog;
import dwvisser.analysis.spanc.AddReactionDialog;
import dwvisser.analysis.spanc.CalibrationFit;
import dwvisser.analysis.spanc.CalibrationPeak;
import dwvisser.analysis.spanc.ChangeCalibrationPeakDialog;
import dwvisser.analysis.spanc.ChangeOutputPeakDialog;
import dwvisser.analysis.spanc.ChangeReactionDialog;
import dwvisser.analysis.spanc.ExVsChOutputDialog;
import dwvisser.analysis.spanc.OutputPeak;
import dwvisser.analysis.spanc.SpancReaction;
import dwvisser.analysis.spanc.Target;
import dwvisser.analysis.spanc.TargetListDialog;
import dwvisser.analysis.spanc.tables.CalibrationPeakTable;
import dwvisser.analysis.spanc.tables.CoefficientTable;
import dwvisser.analysis.spanc.tables.OutputPeakTable;
import dwvisser.analysis.spanc.tables.ReactionTable;
import dwvisser.analysis.spanc.tables.ResidualTable;
import dwvisser.math.MathException;
import dwvisser.nuclear.KinematicsException;
import dwvisser.nuclear.NuclearException;
import dwvisser.nuclear.swing.MassTableChooserMenuItems;
import dwvisser.statistics.StatisticsException;

/**
 * SPlitpole ANalysis Code. Application for calibrating magnetic 
 * spectrometer data,
 * especially for the Enge we use at Yale.  The user inputs
 * target description data and reaction description data, then
 * they input calibration peaks from one or more reactions. 
 * Energy losses in the target are automatically accounted for, 
 * and polynomial fits of user-selectable order are performed
 * between channel and radius.  Then the user may enter
 * other peaks, and extract excitation energies for them.
 * All data may be saved for use in multiple sessions.
 * 
 * @author Dale Visser
 * @version 1.1
 */
public final class Spanc
	extends JFrame
	implements ActionListener, ChangeListener {

	private class ShowFrameAction extends AbstractAction {

		private final JFrame frame;

		ShowFrameAction(JFrame f, String name) {
			super("Show " + name);
			frame = f;
		}

		public void actionPerformed(ActionEvent ae) {
			frame.show();
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
		FileOpenAction() {
			super("Load Data...");
		}
		public void actionPerformed(ActionEvent ae) {
			final File in = getFileOpen();
			if (in != null) {
				try {
					loadData(in);
				} catch (Exception e) {
					popupException(e);
				}
			}
		}
	}

	private class FileSaveAction extends AbstractAction {
		FileSaveAction() {
			super("Save Data...");
		}

		public void actionPerformed(ActionEvent ae) {
			final File out = getFileSave();
			if (out != null) {
				try {
					saveData(out);
				} catch (FileNotFoundException fnf) {
					popupException(fnf);
				} catch (IOException ioe) {
					popupException(ioe);
				}
			}
		}
	}

	private class TextExportAction extends AbstractAction {
		TextExportAction() {
			super("Text Export...");
		}

		/**
		 * Export a text file.
		 * 
		 * @param ae the signal
		 */
		public void actionPerformed(ActionEvent ae) {
			final File out = getFileSave();
			if (out != null) {
				try {
					exportText(out);
				} catch (FileNotFoundException fnf) {
					popupException(fnf);
				} catch (IOException ioe) {
					popupException(ioe);
				}
			}
		}
	}

/*	private ActionListener getHelpListener() {
		final HelpSet hs;
		final String helpsetName = "help/spanc/spanc.hs";
		try {
			final URL hsURL =
				getClass().getClassLoader().getResource(helpsetName);
			hs = new HelpSet(null, hsURL);
		} catch (Exception ee) {
			return null;
		}
		return new CSH.DisplayHelpFromSource(hs.createHelpBroker());
	}
*/
	/**
	 * Action for exporting Spanc's info to a tabular text file.
	 *
	 * @author Dale Visser
	 * @version 1.0
	 */
	private class TableExportAction extends AbstractAction {

		/**
		 * Create an instance of this action.
		 */
		TableExportAction() {
			super("Table Export...");
		}

		/**
		 * Export a table containing information on the present
		 * calibration.
		 *
		 * @param ae the signal 
		 */
		public void actionPerformed(ActionEvent ae) {
			final File out = getFileSave();
			if (out != null) {
				try {
					exportFitTable(out);
				} catch (FileNotFoundException fnf) {
					popupException(fnf);
				} catch (IOException ioe) {
					popupException(ioe);
				}
			}
		}
	}

	/**
	 * Action for exporting calibration to text file.
	 *
	 * @author Dale Visser
	 * @version 1.0
	 */
	private class ExVsChExportAction extends AbstractAction {

		private static final String TITLE = "Ex Vs. Channel Export...";

		/**
		 * Create a new instance of this action.
		 */
		ExVsChExportAction() {
			super(TITLE);
		}

		/**
		 * Create a new export dialog.
		 *
		 * @param ae signal event
		 */
		public void actionPerformed(ActionEvent ae) {
			new ExVsChOutputDialog();
		}
	}

	/**
	 * Action which shows a dialog with the list of targets.
	 *
	 * @author Dale Visser
	 * @version 1.0
	 */
	private class TargetListAction extends AbstractAction {

		private final TargetListDialog tld = new TargetListDialog(Spanc.this);

		/**
		 * Creates an instance of this action.
		 */
		TargetListAction() {
			super("List Targets...");
		}

		/**
		 * Show the frame listing the targets.
		 *
		 * @param ae the signal
		 */
		public void actionPerformed(ActionEvent ae) {
			tld.show();
		}
	}

	private static final int VERSION_MAJOR=1;
	private static final int VERSION_MINOR=2;
	private static final String VERSION=VERSION_MAJOR+"."+VERSION_MINOR;
	private File lastFile;
	private ReactionTable rtable;
	private CalibrationPeakTable cpTable;
	private final JButton b_addReaction = new JButton("Add Reaction");
	private final JButton b_removeReaction = new JButton("Remove Reaction");
	private final JButton b_changeReaction = new JButton("Change Reaction");
	private final JButton b_addCalPeak = new JButton("Add Peak");
	private final JButton b_removeCalPeak = new JButton("Remove Peak");
	private final JButton b_changeCalPeak = new JButton("Change Peak");
	private final JSlider _order = new JSlider(JSlider.HORIZONTAL, 1, 8, 1);
	private final CalibrationFit calFit = CalibrationFit.getInstance();
	private final CoefficientTable coeffTable = new CoefficientTable();
	private final ResidualTable resTable = new ResidualTable();
	private final JTextField _chisq = new JTextField(8);
	private final JTextField _dof = new JTextField(2);
	private final JTextField _channel0 = new JTextField(8);
	private final JTextField _pvalue = new JTextField(8);
	{
		_chisq.setEditable(false);
		_dof.setEditable(false);
		_order.setMinorTickSpacing(1);
		_order.setMajorTickSpacing(1);
		_order.setPaintTicks(true);
		_order.setPaintLabels(true);
		_order.setSnapToTicks(true);
		_order.addChangeListener(this);
		_channel0.setEditable(false);
		_pvalue.setEditable(false);
	}
	private final OutputPeakTable opTable = new OutputPeakTable();
	private final JButton b_addOutPeak = new JButton("Add Peak");
	private final JButton b_removeOutPeak = new JButton("Remove Peak");
	private final JButton b_changeOutPeak = new JButton("Change Peak");
	private final JCheckBox _adjustError = new JCheckBox("Adjust Error");
	private final JFrame calPeaks = new JFrame();

	private final JFrame fit = new JFrame();
	private final JFrame outFrame = new JFrame();
	private static final String title = "SPANC--Spectrograph Analysis Code";

	/**
	 * Constructor.
	 */
	public Spanc() {
		super();
		new SplashWindow(this, 10000, "dwvisser/spanc96.png", title, "v 1.2");
		createLicenseDialog();
		makeWindow();
	}
	
	private final class MassTableChangeListener implements ItemListener{
		public void itemStateChanged(ItemEvent ie){
			cpTable.refreshData();
			calculateFit();
			recalculateOutputTable();
		}
	}

	private void makeWindow() {
		final ClassLoader cl = getClass().getClassLoader();
		final Image icon =
			(new ImageIcon(cl.getResource("dwvisser/spanc96.png")).getImage());
		this.setIconImage(icon);
		final Container window = this.getContentPane();
		setTitle(title);
		window.setLayout(new BorderLayout());
		final JMenuBar mb = new JMenuBar();
		final JMenu file = new JMenu("File", true);
		file.add(new FileOpenAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
		file.add(new FileSaveAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
		file.addSeparator();
		file.add(new TextExportAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
		file.add(new TableExportAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK));
		file.add(new ExVsChExportAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK));
		file.addSeparator();
		file.add(new FileQuitAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
		mb.add(file);
		final JMenu targets = new JMenu("Targets", true);
		targets.add(new TargetListAction());
		mb.add(targets);
		final JMenu windows = new JMenu("Windows");
		mb.add(windows);
		final JMenu help = new JMenu("Help");
		mb.add(help);
		JMenuItem toc=new JMenuItem("Table of Contents...");
//		toc.addActionListener(getHelpListener());
		help.add(toc);
		setJMenuBar(mb);
		final JPanel center = new JPanel(new GridLayout(0, 1));
		final JPanel calReactions = new JPanel(new BorderLayout());
		final JPanel calRnorth = new JPanel(new FlowLayout());
		calRnorth.add(new JLabel("Calibration Reactions"));
		calRnorth.add(b_addReaction);
		b_addReaction.addActionListener(this);
		calRnorth.add(b_removeReaction);
		b_removeReaction.addActionListener(this);
		calRnorth.add(b_changeReaction);
		b_changeReaction.addActionListener(this);
		calReactions.add(calRnorth, BorderLayout.NORTH);
		try {
			setReactionTable(new ReactionTable());
		} catch (KinematicsException ke) {
			popupException(ke);
		}
		calReactions.add(new JScrollPane(rtable), BorderLayout.CENTER);
		//reactionsPlusPeaks.add(calReactions);
		windows.add(new ShowFrameAction(calPeaks, "Calibration Peaks"));
		calPeaks.setTitle("Spanc--Calibration Peaks");
		calPeaks.setIconImage(icon);
		final Container cpc = calPeaks.getContentPane();
		cpc.setLayout(new BorderLayout());
		final JPanel calPnorth = new JPanel(new FlowLayout());
		calPnorth.add(new JLabel("Calibration Peaks"));
		calPnorth.add(b_addCalPeak);
		b_addCalPeak.addActionListener(this);
		calPnorth.add(b_removeCalPeak);
		b_removeCalPeak.addActionListener(this);
		calPnorth.add(b_changeCalPeak);
		b_changeCalPeak.addActionListener(this);
		cpc.add(calPnorth, BorderLayout.NORTH);
		setCalibrationPeakTable(new CalibrationPeakTable());
		cpc.add(new JScrollPane(cpTable), BorderLayout.CENTER);
		//reactionsPlusPeaks.add(calPeaks);
		center.add(calReactions);
		windows.add(new ShowFrameAction(fit, "Fit"));
		fit.setTitle("Spanc--Polynomial Fit of \u03c1 vs. Channel");
		fit.setIconImage(icon);
		final Container fitContents = fit.getContentPane();
		fitContents.setLayout(new BorderLayout());
		final JPanel fitNorth = new JPanel(new BorderLayout());
		final JPanel fitNtitle = new JPanel(new FlowLayout(FlowLayout.CENTER));
		fitNtitle.add(
			new JLabel("Fit:  \u03c1 = a0 + a1\u00b7(channel - channel[0]) + ..."));
		fitNorth.add(fitNtitle, BorderLayout.NORTH);
		final JPanel fitNfields = new JPanel(new GridLayout(2, 5));
		final JLabel lFitOrder = new JLabel("Polynomial Order");
		fitNfields.add(lFitOrder);
		lFitOrder.setLabelFor(_order);
		final JLabel ldof = new JLabel("d.o.f. (\u03bd)");
		fitNfields.add(ldof);
		ldof.setLabelFor(_dof);
		final JLabel lRedChSq = new JLabel("\u03c7\u00b2/\u03bd");
		fitNfields.add(lRedChSq);
		lRedChSq.setLabelFor(_chisq);
		final JLabel lPvalue = new JLabel("p-value");
		fitNfields.add(lPvalue);
		lPvalue.setLabelFor(_pvalue);
		final JLabel lChannel0 = new JLabel("channel[0]");
		fitNfields.add(lChannel0);
		lChannel0.setLabelFor(_channel0);
		fitNfields.add(_order);
		fitNfields.add(_dof);
		fitNfields.add(_chisq);
		fitNfields.add(_pvalue);
		fitNfields.add(_channel0);
		fitNorth.add(fitNfields, BorderLayout.CENTER);
		fitContents.add(fitNorth, BorderLayout.NORTH);
		final JPanel fitCenter = new JPanel(new GridLayout(2, 1));
		fitCenter.add(new JScrollPane(coeffTable));
		fitCenter.add(new JScrollPane(resTable));
		fitContents.add(fitCenter, BorderLayout.CENTER);
		//center.add(fit);
		windows.add(new ShowFrameAction(outFrame, "Output Peaks"));
		outFrame.setTitle("Spanc--Output Peaks");
		outFrame.setIconImage(icon);
		final Container outContents = outFrame.getContentPane();
		outContents.setLayout(new BorderLayout());
		final JPanel outNorth = new JPanel(new FlowLayout());
		outNorth.add(new JLabel("Output Peaks"));
		outNorth.add(b_addOutPeak);
		outNorth.add(b_removeOutPeak);
		outNorth.add(b_changeOutPeak);
		outNorth.add(_adjustError);
		_adjustError.addChangeListener(this);
		b_addOutPeak.addActionListener(this);
		b_removeOutPeak.addActionListener(this);
		b_changeOutPeak.addActionListener(this);
		outContents.add(outNorth, BorderLayout.NORTH);
		outContents.add(new JScrollPane(opTable), BorderLayout.CENTER);
		window.add(center, BorderLayout.CENTER);
		final JMenu masses=new JMenu("Masses");
		new MassTableChooserMenuItems(masses,new MassTableChangeListener());
		mb.add(masses);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});
		final Runnable startGui = new Runnable() {
			public void run() {
				pack();
				show();
				calPeaks.pack();
				fit.pack();
				outFrame.pack();
				setButtonStates();
			}
		};
		SwingUtilities.invokeLater(startGui);
	}

	private void setReactionTable(ReactionTable rt) {
		synchronized (this) {
			rtable = rt;
		}
	}

	private void setCalibrationPeakTable(CalibrationPeakTable cpt) {
		synchronized (this) {
			cpTable = cpt;
		}
	}

	void saveData(File file) throws FileNotFoundException, IOException {
		final ObjectOutputStream os =
			new ObjectOutputStream(new FileOutputStream(file));
		final List data = new ArrayList();
		//data.add(calFit);
		data.addAll(Target.getTargetCollection());
		data.addAll(SpancReaction.getReactionCollection());
		data.addAll(CalibrationPeak.getPeakCollection());
		data.addAll(OutputPeak.getPeakCollection());
		os.writeObject(data);
		os.close();
	}

	void exportText(File file) throws FileNotFoundException, IOException {
		final PrintWriter pw = new PrintWriter(new FileOutputStream(file));
		final List data = new ArrayList();
		data.addAll(Target.getTargetCollection());
		data.addAll(SpancReaction.getReactionCollection());
		data.addAll(CalibrationPeak.getPeakCollection());
		data.add(calFit);
		data.addAll(OutputPeak.getPeakCollection());
		for (final Iterator iter = data.iterator(); iter.hasNext();) {
			pw.print(iter.next());
			pw.print("--------\n");
		}
		pw.close();
	}

	void exportFitTable(File file) throws FileNotFoundException, IOException {
		final String tableHead =
			"Reaction\tB-field\tEx(Projectile)\t"
				+ "input Ex(Residual)\tdelEx(Residual)\tChannel\tdelChannel\t"
				+ "input rho\tdelRho\trho from fit\tdelRho\tresidual\n";
		final PrintWriter pw = new PrintWriter(new FileOutputStream(file));
		pw.print(tableHead);
		pw.print(cpTable.getExportTableText());
		pw.print("\t\t\tfit Ex(Residual)\n");
		pw.print(opTable.getExportTableText());
		pw.close();
	}

	void loadData(File file)
		throws
			FileNotFoundException,
			IOException,
			ClassNotFoundException,
			MathException {
		CalibrationPeak.removeAllPeaks();
		SpancReaction.removeAllReactions();
		Target.removeAllTargets();
		_order.setValue(1);
		final ObjectInputStream is =
			new ObjectInputStream(new FileInputStream(file));
		final List data = (List) is.readObject();
		is.close();
		final List targets = new ArrayList();
		final List reactions = new ArrayList();
		final List c_peaks = new ArrayList();
		final List o_peaks = new ArrayList();
		for (final Iterator iter = data.iterator(); iter.hasNext();) {
			final Object o = iter.next();
			if (o instanceof Target) {
				targets.add(o);
			} else if (o instanceof SpancReaction) {
				reactions.add(o);
			} else if (o instanceof CalibrationPeak) {
				c_peaks.add(o);
			} else if (o instanceof OutputPeak) {
				o_peaks.add(o);
			}
		}
		Target.refreshData(targets);
		SpancReaction.refreshData(reactions);
		rtable.refreshData();
		CalibrationPeak.refreshData(c_peaks);
		cpTable.refreshData();
		OutputPeak.refreshData(o_peaks);
		calculateFit();
		setButtonStates();
	}

	/**
	 * Read in an unspecified file by opening up a dialog box.
	 *
	 * @return  <code>true</code> if successful, <code>false</code> if 
	 * not
	 */
	File getFileOpen() {
		final JFileChooser jfile = new JFileChooser(lastFile);
		File rval = null;
		final int option = jfile.showOpenDialog(this);
		/* Don't do anything if it was cancelled. */
		if (option == JFileChooser.APPROVE_OPTION
			&& jfile.getSelectedFile() != null) {
			setLastFile(jfile.getSelectedFile());
			rval = lastFile;
		}
		return rval;
	}

	/**
	 * Read in an unspecified file by opening up a dialog box.
	 *
	 * @return  <code>true</code> if successful, <code>false</code> if 
	 * not
	 */
	File getFileSave() {
		final JFileChooser jfile = new JFileChooser(lastFile);
		File rval = null;
		final int option = jfile.showSaveDialog(this);
		// dont do anything if it was cancel
		if (option == JFileChooser.APPROVE_OPTION
			&& jfile.getSelectedFile() != null) {
			setLastFile(jfile.getSelectedFile());
			rval = lastFile;
		}
		return rval;
	}

	private void setLastFile(File f) {
		synchronized (this) {
			lastFile = f;
		}
	}

	/**
	 * Launches Spanc application.
	 * @param args
	 */
	public static void main(String args[]) {
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
		new Spanc();
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 *
	 * @param actionEvent the event to handle
	 */
	public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
		final Object source = actionEvent.getSource();
		if (source.equals(b_addReaction)) {
			new AddReactionDialog(rtable, this);
		} else if (source.equals(b_removeReaction)) {
			final int row = rtable.getSelectedRow();
			SpancReaction.removeReaction(row);
			rtable.removeRow(row);
			setButtonStates();
		} else if (source.equals(b_changeReaction)) {
			if (rtable.getSelectedRow() != -1) { //only if a row is selected
				new ChangeReactionDialog(rtable, cpTable, this);
			}
		} else if (source.equals(b_addCalPeak)) {
			new AddCalibrationPeakDialog(cpTable, this);
		} else if (source.equals(b_removeCalPeak)) {
			final int row = cpTable.getSelectedRow();
			CalibrationPeak.removePeak(row);
			cpTable.removeRow(row);
			setButtonStates();
		} else if (source.equals(b_changeCalPeak)) {
			if (cpTable.getSelectedRow() != -1) { //only if a row is selected
				new ChangeCalibrationPeakDialog(cpTable, this);
			}
		} else if (source.equals(b_addOutPeak)) {
			new AddOutputPeakDialog(opTable, this);
		} else if (source.equals(b_removeOutPeak)) {
			final int row = opTable.getSelectedRow();
			OutputPeak.removePeak(row);
			opTable.removeRow(row);
			setButtonStates();
		} else if (source.equals(b_changeOutPeak)) {
			if (opTable.getSelectedRow() != -1) { //only if a row is selected
				new ChangeOutputPeakDialog(opTable, this);
			}
		}
	}

	/**
	 * Handles changes in main window sliders.
	 *
	 * @param changeEvent the event created when the user adjusts a
	 * slider
	 */
	public void stateChanged(ChangeEvent changeEvent) {
		final Object source = changeEvent.getSource();
		if (source.equals(_order)) {
			calculateFit();
		}
		if (source.equals(_adjustError)) {
			opTable.adjustErrors(_adjustError.isSelected());
		}
	}

	/**
	 * Calculates calibration fit.
	 */
	public void calculateFit() {
		try {
			calFit.setOrder(_order.getModel().getValue());
		} catch (Exception me) {
			popupException(me);
		}
		coeffTable.updateCoefficients();
		resTable.updateResiduals();
		_dof.setText(String.valueOf(calFit.getDOF()));
		if (calFit.getDOF() > 0) {
			final java.text.DecimalFormat df =
				new java.text.DecimalFormat("0.000#");
			_chisq.setText(df.format(calFit.getReducedChiSq()));
			_channel0.setText(df.format(calFit.getChannel0()));
			_pvalue.setText(df.format(calFit.getPvalue()));
			recalculateOutputTable();
		}
	}

	/**
	 * Used to enable/disable buttons based on state of data.
	 */
	public void setButtonStates() {
		final boolean noTargets = Target.getTargetCollection().isEmpty();
		if (noTargets) {
			b_addReaction.setEnabled(false);
		} else {
			b_addReaction.setEnabled(true);
		}
		final boolean noReactions =
			SpancReaction.getReactionCollection().isEmpty();
		if (noTargets || noReactions) {
			b_addCalPeak.setEnabled(false);
			calPeaks.hide();
		} else {
			b_addCalPeak.setEnabled(true);
			calPeaks.show();
		}
		final Collection peaks = CalibrationPeak.getPeakCollection();
		final int numCalPeaks = peaks.size();
		final boolean noCalPeaks = peaks.isEmpty();
		if (noTargets || noReactions || noCalPeaks) {
			b_addOutPeak.setEnabled(false);
			_order.setEnabled(false);
			fit.hide();
		} else {
			b_addOutPeak.setEnabled(true);
			_order.setEnabled(true);
			if (numCalPeaks > 2) {
				fit.show();
			} else {
				fit.hide();
			}
		}
		final boolean enableReactionChange=(!noReactions);
		b_removeReaction.setEnabled(enableReactionChange);
		b_changeReaction.setEnabled(enableReactionChange);
		final boolean enableCalPeakChange=(!noCalPeaks);
		b_removeCalPeak.setEnabled(enableCalPeakChange);
		b_changeCalPeak.setEnabled(enableCalPeakChange);
		final boolean enableOutPeakChange=
		!OutputPeak.getPeakCollection().isEmpty();
		b_removeOutPeak.setEnabled(enableOutPeakChange);
	}

	void recalculateOutputTable() {
		try {
			OutputPeak.recalculate();
		} catch (KinematicsException ke) {
			popupException(ke);
		} catch (StatisticsException se) {
			popupException(se);
		} catch (MathException me) {
			popupException(me);
		} catch (NuclearException me) {
			popupException(me);
		}
		opTable.refreshData();
	}

	private static void popupException(Exception e) {
		final String title = "Exception in Spanc";
		JOptionPane.showMessageDialog(
			null,
			e.getMessage(),
			title,
			JOptionPane.ERROR_MESSAGE);
	}
	
	private JDialog licenseD;
	private final void createLicenseDialog() {
		licenseD =
			new JDialog(
				this,
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
		bok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				licenseD.dispose();
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
		final String key="SpancLicense";
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



}