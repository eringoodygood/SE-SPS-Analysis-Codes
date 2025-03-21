package dwvisser;
import dwvisser.nuclear.*;
import dwvisser.math.*;
import dwvisser.statistics.*;
import java.io.*;
import dwvisser.analysis.spanc.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import dwvisser.analysis.spanc.tables.*;
import java.util.Vector;
import java.util.Iterator;

/**
 * SPlitpole ANalysis Code. Application for calibrating magnetic spectrometer data,
 * especially for the Enge we use at Yale.  The user inputs
 * target description data and reaction description data, then
 * they input calibration peaks from one or more reactions. 
 * Energy losses in the target are automatically accounted for, 
 * and polynomial fits of user-selectable order are performed
 * between channel and radius.  Then the user may enter
 * other peaks, and extract excitation energies for them.
 * All data may be saved for use in multiple sessions.
 */
public class Spanc extends JFrame implements ActionListener, ChangeListener {

	/**
	 * Constructor.
	 */
	public Spanc() {
//		new Nucleus(); //initialize nuclear data
//		new EnergyLoss(); //initialize stopping data
		makeWindow();
	}

	Container window = this.getContentPane();
	ReactionTable rtable;
	CalibrationPeakTable cpTable;
	JButton b_addReaction = new JButton("Add Reaction");
	JButton b_removeReaction = new JButton("Remove Reaction");
	JButton b_changeReaction = new JButton("Change Reaction");
	JButton b_addCalPeak = new JButton("Add Peak");
	JButton b_removeCalPeak = new JButton("Remove Peak");
	JButton b_changeCalPeak = new JButton("Change Peak");
	JSlider _order;
	CalibrationFit calFit;
	CoefficientTable coeffTable;
	ResidualTable resTable;
	JTextField _dof, _chisq, _channel0, _pvalue;
	OutputPeakTable opTable = new OutputPeakTable();
	JButton b_addOutPeak = new JButton("Add Peak");
	JButton b_removeOutPeak = new JButton("Remove Peak");
	JButton b_changeOutPeak = new JButton("Change Peak");
	JCheckBox _adjustError = new JCheckBox("Adjust Error");
	private void makeWindow() {
		setTitle("SPANC--SPlitpole ANalysis Code");
		window.setLayout(new BorderLayout());
		JMenuBar mb = new JMenuBar();
		JMenu file = new JMenu("File", true);
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
		JMenu targets = new JMenu("Targets", true);
		targets.add(new TargetListAction());
		mb.add(targets);
		setJMenuBar(mb);
		JPanel center = new JPanel(new GridLayout(0, 1));
		JPanel reactionsPlusPeaks = new JPanel(new GridLayout(2, 1));
		JPanel calReactions = new JPanel(new BorderLayout());
		JPanel calRnorth = new JPanel(new FlowLayout());
		calRnorth.add(new JLabel("Calibration Reactions"));
		calRnorth.add(b_addReaction);
		b_addReaction.addActionListener(this);
		calRnorth.add(b_removeReaction);
		b_removeReaction.addActionListener(this);
		calRnorth.add(b_changeReaction);
		b_changeReaction.addActionListener(this);
		calReactions.add(calRnorth, BorderLayout.NORTH);
		try {
			rtable = new ReactionTable();
		} catch (KinematicsException ke) {
			System.err.println(ke);
		}
		calReactions.add(new JScrollPane(rtable), BorderLayout.CENTER);
		reactionsPlusPeaks.add(calReactions);
		//center.add(calReactions);
		JPanel calPeaks = new JPanel(new BorderLayout());
		JPanel calPnorth = new JPanel(new FlowLayout());
		calPnorth.add(new JLabel("Calibration Peaks"));
		calPnorth.add(b_addCalPeak);
		b_addCalPeak.addActionListener(this);
		calPnorth.add(b_removeCalPeak);
		b_removeCalPeak.addActionListener(this);
		calPnorth.add(b_changeCalPeak);
		b_changeCalPeak.addActionListener(this);
		calPeaks.add(calPnorth, BorderLayout.NORTH);
		cpTable = new CalibrationPeakTable();
		calPeaks.add(new JScrollPane(cpTable), BorderLayout.CENTER);
		reactionsPlusPeaks.add(calPeaks);
		center.add(reactionsPlusPeaks);
		JPanel fit = new JPanel(new BorderLayout());
		JPanel fitNorth = new JPanel(new BorderLayout());
		JPanel fitNtitle = new JPanel(new FlowLayout(FlowLayout.CENTER));
		fitNtitle.add(
			new JLabel("Fit:  rho = a0 + a1*(channel - channel[0]) + ..."));
		fitNorth.add(fitNtitle, BorderLayout.NORTH);
		JPanel fitNfields = new JPanel(new GridLayout(2, 5));
		fitNfields.add(new JLabel("Fit Order"));
		fitNfields.add(new JLabel("d.o.f."));
		fitNfields.add(new JLabel("ChiSq/d.o.f."));
		fitNfields.add(new JLabel("p-value"));
		fitNfields.add(new JLabel("channel[0]"));
		_order = new JSlider(JSlider.HORIZONTAL, 1, 8, 1);
		_order.setMinorTickSpacing(1);
		_order.setMajorTickSpacing(1);
		_order.setPaintTicks(true);
		_order.setPaintLabels(true);
		_order.setSnapToTicks(true);
		_order.addChangeListener(this);
		fitNfields.add(_order);
		_dof = new JTextField(2);
		_dof.setEditable(false);
		fitNfields.add(_dof);
		_chisq = new JTextField(8);
		_chisq.setEditable(false);
		fitNfields.add(_chisq);
		_pvalue = new JTextField(8);
		_pvalue.setEditable(false);
		fitNfields.add(_pvalue);
		_channel0 = new JTextField(8);
		_channel0.setEditable(false);
		fitNfields.add(_channel0);
		fitNorth.add(fitNfields, BorderLayout.CENTER);
		fit.add(fitNorth, BorderLayout.NORTH);
		JPanel fitCenter = new JPanel(new GridLayout(2, 1));
		calFit = new CalibrationFit();
		coeffTable = new CoefficientTable(calFit);
		resTable = new ResidualTable(calFit);
		fitCenter.add(new JScrollPane(coeffTable));
		fitCenter.add(new JScrollPane(resTable));
		fit.add(fitCenter, BorderLayout.CENTER);
		center.add(fit);
		JPanel outPanel = new JPanel(new BorderLayout());
		JPanel outNorth = new JPanel(new FlowLayout());
		outNorth.add(new JLabel("Output Peaks"));
		outNorth.add(b_addOutPeak);
		outNorth.add(b_removeOutPeak);
		outNorth.add(b_changeOutPeak);
		outNorth.add(_adjustError);
		_adjustError.addChangeListener(this);
		b_addOutPeak.addActionListener(this);
		b_removeOutPeak.addActionListener(this);
		b_changeOutPeak.addActionListener(this);
		outPanel.add(outNorth, BorderLayout.NORTH);
		outPanel.add(new JScrollPane(opTable), BorderLayout.CENTER);
		center.add(outPanel);
		window.add(center, BorderLayout.CENTER);
		setSize(600, 800);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});
		show();
		setButtonStates();
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
			super("Load Data...");
		}
		public void actionPerformed(ActionEvent ae) {
			File in = getFileOpen();
			if (in != null) {
				try {
					loadData(in);
				} catch (FileNotFoundException fnf) {
					System.err.println(
						"File not found while trying to load: " + fnf);
				} catch (IOException ioe) {
					System.err.println(
						"I/O problem while trying to load: " + ioe);
				} catch (ClassNotFoundException cnf) {
					System.err.println(
						"Class not found while trying to load: " + cnf);
				} catch (MathException me) {
					System.err.println(
						"Math error while trying to load: " + me);
				}
			}
		}
	}

	public class FileSaveAction extends AbstractAction {
		public FileSaveAction() {
			super("Save Data...");
		}

		public void actionPerformed(ActionEvent ae) {
			File out = getFileSave();
			if (out != null) {
				try {
					saveData(out);
				} catch (FileNotFoundException fnf) {
					System.err.println(
						"File not found while trying to save: " + fnf);
				} catch (IOException ioe) {
					System.err.println(
						"I/O problem while trying to save: " + ioe);
				}
			}
		}
	}
	public class TextExportAction extends AbstractAction {
		public TextExportAction() {
			super("Text Export...");
		}

		public void actionPerformed(ActionEvent ae) {
			File out = getFileSave();
			if (out != null) {
				try {
					exportText(out);
				} catch (FileNotFoundException fnf) {
					System.err.println(
						"File not found while trying to save: " + fnf);
				} catch (IOException ioe) {
					System.err.println(
						"I/O problem while trying to save: " + ioe);
				}
			}
		}
	}
	public class TableExportAction extends AbstractAction {
		public TableExportAction() {
			super("Table Export...");
		}

		public void actionPerformed(ActionEvent ae) {
			File out = getFileSave();
			if (out != null) {
				try {
					exportFitTable(out);
				} catch (FileNotFoundException fnf) {
					System.err.println(
						"File not found while trying to save: " + fnf);
				} catch (IOException ioe) {
					System.err.println(
						"I/O problem while trying to save: " + ioe);
				}
			}
		}
	}

	public class ExVsChExportAction extends AbstractAction {
		public ExVsChExportAction() {
			super("Ex Vs. Channel Export...");
		}

		public void actionPerformed(ActionEvent ae) {
			new ExVsChOutputDialog(calFit);
		}
	}

	void saveData(File file) throws FileNotFoundException, IOException {
		ObjectOutputStream os =
			new ObjectOutputStream(new FileOutputStream(file));
		Vector data = new Vector();
		data.add(calFit);
		data.addAll(Target.getTargetCollection());
		data.addAll(SpancReaction.getReactionCollection());
		data.addAll(CalibrationPeak.getPeakCollection());
		data.addAll(OutputPeak.getPeakCollection());
		os.writeObject(data);
		os.close();
	}

	void exportText(File file) throws FileNotFoundException, IOException {
		PrintWriter pw = new PrintWriter(new FileOutputStream(file));
		Vector data = new Vector();
		data.addAll(Target.getTargetCollection());
		data.addAll(SpancReaction.getReactionCollection());
		data.addAll(CalibrationPeak.getPeakCollection());
		data.add(calFit);
		data.addAll(OutputPeak.getPeakCollection());
		Iterator iter = data.iterator();
		while (iter.hasNext()) {
			pw.print(iter.next());
			pw.print("--------\n");
		}
		pw.close();
	}

	void exportFitTable(File file) throws FileNotFoundException, IOException {
		String tableHead =
			"Reaction\tB-field\tEx(Projectile)\t"
				+ "input Ex(Residual)\tdelEx(Residual)\tChannel\tdelChannel\t"
				+ "input rho\tdelRho\trho from fit\tdelRho\tresidual\n";
		PrintWriter pw = new PrintWriter(new FileOutputStream(file));
		pw.print(tableHead);
		pw.print(cpTable.getExportTableText(calFit));
		pw.print("\t\t\tfit Ex(Residual)\n");
		pw.print(opTable.getExportTableText());
		pw.close();
	}

	void loadData(File file)
		throws
			FileNotFoundException,
			IOException,
			OptionalDataException,
			ClassNotFoundException,
			MathException {
		CalibrationPeak.removeAllPeaks();
		SpancReaction.removeAllReactions();
		Target.removeAllTargets();
		_order.setValue(1);
		ObjectInputStream is = new ObjectInputStream(new FileInputStream(file));
		Vector data = (Vector) is.readObject();
		is.close();
		Vector targets = new Vector();
		Vector reactions = new Vector();
		Vector c_peaks = new Vector();
		Vector o_peaks = new Vector();
		for (Iterator iter = data.iterator(); iter.hasNext();) {
			Object o = iter.next();
			if (o instanceof Target) {
				targets.add(o);
			} else if (o instanceof SpancReaction) {
				reactions.add(o);
			} else if (o instanceof CalibrationPeak) {
				c_peaks.add(o);
			} else if (o instanceof OutputPeak) {
				o_peaks.add(o);
			} else if (o instanceof CalibrationFit) {
				calFit = (CalibrationFit) calFit;
				calFit.setOrder(_order.getValue());
			}
		}
		Target.refreshData(targets);
		SpancReaction.refreshData(reactions);
		rtable.refreshData();
		CalibrationPeak.refreshData(c_peaks);
		cpTable.refreshData();
		OutputPeak.refreshData(o_peaks);
		OutputPeak.setCalibration(calFit);
		if (calFit != null) {
			calculateFit();
		}
		setButtonStates();
	}

	TargetListDialog tld = new TargetListDialog(this);
	public class TargetListAction extends AbstractAction {
		public TargetListAction() {
			super("List Targets...");
		}
		public void actionPerformed(ActionEvent ae) {
			tld.show();
		}
	}

	File lastFile;
	/**
	 * Read in an unspecified file by opening up a dialog box.
	 *
	 * @param  mode  whether to open or reload
	 * @return  <code>true</code> if successful, <code>false</code> if not
	 */
	File getFileOpen() {
		JFileChooser jfile;
		File rval;
		
		if (lastFile == null) {
			jfile = new JFileChooser();
		} else {
			jfile = new JFileChooser(lastFile);
		}
		int option = jfile.showOpenDialog(this);
		// dont do anything if it was cancel
		if (option == JFileChooser.APPROVE_OPTION
			&& jfile.getSelectedFile() != null) {
			lastFile = jfile.getSelectedFile();
			rval=lastFile;
//			return lastFile;
		} else {
			rval=null;
		}
		return rval;
	}
	
	/**
	 * Read in an unspecified file by opening up a dialog box.
	 *
	 * @param  mode  whether to open or reload
	 * @return  <code>true</code> if successful, <code>false</code> if not
	 */
	File getFileSave() {
		JFileChooser jfile;
		File rval;
		
		if (lastFile == null) {
			jfile = new JFileChooser();
		} else {
			jfile = new JFileChooser(lastFile);
		}
		int option = jfile.showSaveDialog(this);
		// dont do anything if it was cancel
		if (option == JFileChooser.APPROVE_OPTION
			&& jfile.getSelectedFile() != null) {
			lastFile = jfile.getSelectedFile();
			rval=lastFile;
//			return lastFile;
		} else {
			rval=null;
		}
		return rval;
	}

	/**
	 * Launches Spanc application.
	 * @param args
	 */
	public static void main(String args[]) {
		new Spanc();
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
		Object source = actionEvent.getSource();
		if (source == b_addReaction) {
			new AddReactionDialog(rtable, this);
		} else if (source == b_removeReaction) {
			int row = rtable.getSelectedRow();
			SpancReaction.removeReaction(row);
			rtable.removeRow(row);
			setButtonStates();
		} else if (source == b_changeReaction) {
			if (rtable.getSelectedRow() != -1) { //only if a row is selected
				new ChangeReactionDialog(rtable, cpTable, this);
			}
		} else if (source == b_addCalPeak) {
			new AddCalibrationPeakDialog(cpTable, this);
		} else if (source == b_removeCalPeak) {
			int row = cpTable.getSelectedRow();
			CalibrationPeak.removePeak(row);
			cpTable.removeRow(row);
			setButtonStates();
		} else if (source == b_changeCalPeak) {
			if (cpTable.getSelectedRow() != -1) { //only if a row is selected
				new ChangeCalibrationPeakDialog(cpTable, this);
			}
		} else if (source == b_addOutPeak) {
			new AddOutputPeakDialog(opTable, calFit, this);
		} else if (source == b_removeOutPeak) {
			int row = opTable.getSelectedRow();
			OutputPeak.removePeak(row);
			opTable.removeRow(row);
			setButtonStates();
		} else if (source == b_changeOutPeak) {
			if (opTable.getSelectedRow() != -1) { //only if a row is selected
				new ChangeOutputPeakDialog(opTable, calFit, this);
			}
		}
	}

	static java.text.DecimalFormat df = new java.text.DecimalFormat("0.000#");
	public void stateChanged(ChangeEvent changeEvent) {
		Object source = changeEvent.getSource();
		if (source == _order) {
			calculateFit();
		} else if (source == _adjustError) {
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
			System.err.println(me);
		}
		//_order.setValue(calFit.getOrder());
		coeffTable.updateCoefficients();
		resTable.updateResiduals();
		_dof.setText("" + calFit.getDOF());
		if (calFit.getDOF() > 0) {
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
		boolean noTargets = Target.getTargetCollection().isEmpty();
		if (noTargets) {
			b_addReaction.setEnabled(false);
		} else {
			b_addReaction.setEnabled(true);
		}
		boolean noReactions = SpancReaction.getReactionCollection().isEmpty();
		if (noTargets || noReactions) {
			b_addCalPeak.setEnabled(false);
		} else {
			b_addCalPeak.setEnabled(true);
		}
		boolean noCalPeaks = CalibrationPeak.getPeakCollection().isEmpty();
		if (noTargets || noReactions || noCalPeaks) {
			b_addOutPeak.setEnabled(false);
			_order.setEnabled(false);
		} else {
			b_addOutPeak.setEnabled(true);
			_order.setEnabled(true);
		}
		if (noReactions) {
			b_removeReaction.setEnabled(false);
			b_changeReaction.setEnabled(false);
		} else {
			b_removeReaction.setEnabled(true);
			b_changeReaction.setEnabled(true);
		}
		if (noCalPeaks) {
			b_removeCalPeak.setEnabled(false);
			b_changeCalPeak.setEnabled(false);

		} else {
			b_removeCalPeak.setEnabled(true);
			b_changeCalPeak.setEnabled(true);
		}
		if (OutputPeak.getPeakCollection().isEmpty()) {
			b_removeOutPeak.setEnabled(false);
		} else {
			b_removeOutPeak.setEnabled(true);
		}
	}

	void recalculateOutputTable() {
		try {
			OutputPeak.recalculate();
		} catch (KinematicsException ke) {
			System.err.println(ke);
		} catch (StatisticsException se) {
			System.err.println(se);
		} catch (MathException me) {
			System.err.println(me);
		}
		opTable.refreshData();
	}

}