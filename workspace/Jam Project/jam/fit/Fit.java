package jam.fit;
import jam.data.Histogram;
import jam.global.MessageHandler;
import jam.plot.Display;
import jam.plot.PlotMouseListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

/**
 * This defines the necessary methods that need to be defined in order for a 
 * fit routine to work with <code>FitControl</code>.  It also contains the dialog box
 * that serves as the user interface.
 *
 * @author  Dale Visser
 * @author Ken Swartz
 *
 * @see	    NonLinearFit
 * @see	    GaussianFit
 */
public abstract class Fit implements ItemListener, PlotMouseListener {

	/*
	 * Displayed name of <code>Fit</code> routine.
	 */
	protected String NAME;

	/**
	 * Controlling frame for this dialog box.
	 */
	protected Frame frame;

	/**
	 * Jam main display window.
	 */
	protected Display display;

	/**
	 * Class to send output messages to.
	 */
	protected MessageHandler msgHandler;

	/**
	 * The ordered list of all <code>Parameter</code> objects..
	 *
	 * @see Parameter
	 */
	protected java.util.List parameters = new Vector();

	/**
	 * The list of <code>Parameter</code> objects accessible by name.
	 *
	 * @see Parameter
	 */
	protected Hashtable parameterTable = new Hashtable();

	/**
	 * <code>Enumeration</code> of parameters
	 */
	protected Iterator parameterEnum;

	/**
	 * The histogram to be fitted.
	 */
	protected double[] counts;

	/**
	 * The errors associatied with <code>counts</code>.
	 */
	protected double[] errors;

	/**
	 * If these are set, they are used for display.
	 */
	int lowerLimit;

	/**
	 * 
	 */
	int upperLimit;

	/**
	 * Counter for mouse input.
	 */
	private int mouseClickCount;

	protected boolean residualOption = true;

	/**
	 * Dialog box
	 */
	private JDialog dfit;

	/**
	 * panel containing parameters
	 */
	private JPanel panelParam;

	/**
	 * Layout for <code>dfit</dfit>
	 */
	//private GridBagLayout gridBag;

	/**
	 * Checkboxes for parameter fixing.
	 */
	private JCheckBox[] cFixValue;

	/**
	 * Checkboxes for finding initial estimates.
	 */
	private JCheckBox[] cEstimate;

	/**
	 * Checkboxes for miscellaneous options.
	 */
	private JCheckBox[] cOption;

	/**
	 * Text printed in dialog box.
	 */
	//private JTextField[] textKnown;

	/**
	 * Data field values.
	 */
	private JTextField[] textData;

	/**
	 * Fit error field values.
	 */
	private JLabel[] textError;

	/**
	 * Parameter labels
	 */
	private JLabel[] text;

	/**
	 * Status message
	 */
	private JLabel status;

	/**
	 * Histogram name field
	 */
	private JLabel textHistName;

	/**
	 * Button to get mouse input.
	 */
	private JButton bMouseGet;

	private Parameter[] parameterArray;
	
	protected FitConsole textInfo;
	

	/**
	 * Class constructor.
	 *
	 * @param	name	name for dialog box and menu item
	 */
	public Fit(String name) {
		this.NAME = name;
	}

	//-------------------------
	// List of abstract methods 
	//-------------------------

	/**
	 * Changes parameter values to calculated estimates.  Useful for <code>NonLinearFit</code>, which requires 
	 * reasonably close guesses in order to converge on a good chi-squared minimum.
	 *
	 * @exception   FitException    thrown if unrecoverable error occurs during estimation
	 * @see NonLinearFit
	 * @see GaussianFit#estimate
	 */
	public abstract void estimate() throws FitException;

	/**
	 * Sets the contents of <code>counts</code>.
	 * 
	 * @param	counts the new array of channel values
	 * @see	#counts 
	 */

	/**
	 * Performs calulations neccessary to find a best fit to the data.
	 *
	 * @return	<code>String</code> containing information about the fit.
	 * @exception   FitException	    thrown if unrecoverable error occurs during fit
	 */
	public abstract String doFit() throws FitException;

	/**
	 * Creates user interface dialog box.
	 *
	 * @param	f	    controlling frame
	 * @param	d	    Jam main class
	 * @param	mh  object to send error messages to
	 * @exception   FitException	    thrown if unrecoverable error occurs during dialog creation
	 */
	public void createDialog(
		Frame f,
		Display d,
		MessageHandler mh)
		throws FitException {
		frame = f;
		display = d;
		msgHandler = mh;
		parameters = getParameters();
		final int parNumber = parameters.size();
		parameterArray = new Parameter[parNumber];
		parameters.toArray(parameterArray);
		dfit = new JDialog(frame, NAME, false);
		Container contents = dfit.getContentPane();
		dfit.setResizable(false);
		dfit.setLocation(20, 50);
		contents.setLayout(new BorderLayout());
		final JTabbedPane tabs=new JTabbedPane();
		contents.add(tabs,BorderLayout.CENTER);
		final JPanel cp =new JPanel(new BorderLayout());
		tabs.addTab("Fit",null,cp,"Setup parameters and do fit.");
		textInfo=new FitConsole(35*parNumber);
		tabs.addTab("Information",null,textInfo,"Additional information output from the fits.");
		/* top panel with histogram name */
		JPanel pHistName = new JPanel(new BorderLayout());
		pHistName.setBorder(LineBorder.createBlackLineBorder());
		pHistName.add(
			new JLabel("Fit Histogram: ", JLabel.RIGHT),
			BorderLayout.WEST);
		textHistName = new JLabel("     ");
		pHistName.add(textHistName, BorderLayout.CENTER);
		cp.add(pHistName, BorderLayout.NORTH);
		/* bottom panel with status and buttons */
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridLayout(0, 1));
		cp.add(bottomPanel, BorderLayout.SOUTH);
		/* status panel part of bottom panel */
		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout());
		statusPanel.add(
			new JLabel("Status: ", JLabel.RIGHT),
			BorderLayout.WEST);
		status =
			new JLabel("OK                                                          ");
		statusPanel.add(status, BorderLayout.CENTER);
		bottomPanel.add(statusPanel);
		/* button panel part of bottom panel */
		JPanel pbut = new JPanel();
		pbut.setLayout(new GridLayout(1, 0));
		bottomPanel.add(pbut);
		bMouseGet = new JButton("Get Mouse");
		bMouseGet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initializeMouse();
				setMouseActive(true);
			}
		});
		pbut.add(bMouseGet);
		JButton bGo = new JButton("Do Fit");
		bGo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					setMouseActive(false);
					updateParametersFromDialog();
					getCounts();
					status.setText("Estimating...");
					estimate();
					status.setText("Doing Fit...");
					status.setText(doFit());
					updateDisplay();
					drawFit();
				} catch (FitException fe) {
					status.setText(fe.getMessage());
				}
			}
		});
		pbut.add(bGo);
		JButton bDraw = new JButton("Draw Fit");
		bDraw.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					drawFit();
				} catch (FitException fe) {
					status.setText(fe.getMessage());
				}
			}
		});
		pbut.add(bDraw);
		JButton bReset = new JButton("Reset");
		bReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					reset();
				} catch (FitException fe) {
					status.setText(fe.getMessage());
				}
			}
		});
		pbut.add(bReset);
		JButton bCancel = new JButton("Cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setMouseActive(false);
				dfit.dispose();
			}
		});
		pbut.add(bCancel);

		/* Layout of parameter widgets */
		panelParam = new JPanel(new GridLayout(0, 1));
		cp.add(panelParam, BorderLayout.CENTER);
		JPanel west = new JPanel(new GridLayout(0, 1));
		JPanel center = new JPanel(new GridLayout(0, 1));
		JPanel east = new JPanel(new GridLayout(0, 1));
		cp.add(west, BorderLayout.WEST);
		cp.add(east, BorderLayout.EAST);
		cp.add(center, BorderLayout.CENTER);
		cFixValue = new JCheckBox[parNumber];
		cEstimate = new JCheckBox[parNumber];
		cOption = new JCheckBox[parNumber];
		//textKnown = new JTextField[parNumber];
		textData = new JTextField[parNumber];
		textError = new JLabel[parNumber];
		text = new JLabel[parNumber];
		for (int i = 0; i < parameters.size(); i++) {
			JPanel middle = new JPanel(new GridLayout(1, 3));
			center.add(middle);
			final Parameter parameter = (Parameter) parameters.get(i);
			final String parName = parameter.getName();
			if (parameter.isDouble()) {
				textData[i] = new JTextField(formatValue(parameter), 8);
				textData[i].setEnabled(true);
				west.add(new JLabel(parName, JLabel.RIGHT));
				middle.add(textData[i]);
			} else if (parameter.isInteger()) {
				textData[i] = new JTextField(formatValue(parameter), 8);
				textData[i].setEnabled(true);
				west.add(new JLabel(parName, JLabel.RIGHT));
				middle.add(textData[i]);
			} else if (parameter.isText()) {
				text[i] = new JLabel(formatValue(parameter));
				west.add(new JLabel(parName, JLabel.RIGHT));
				middle.add(text[i]);
			} else if (parameter.isBoolean()) {
				cOption[i] =
					new JCheckBox(parName, parameter.getBooleanValue());
				cOption[i].addItemListener(this);
				middle.add(cOption[i]);
				west.add(new JPanel());
			}
			/* Take care of options. */
			JPanel right = new JPanel(new GridLayout(1, 0));
			east.add(right);
			if (parameter.hasErrorBar()) {
				textError[i] = new JLabel(formatError(parameter));
				textError[i].setEnabled(true);
				middle.add(textError[i]);
			}
			if (parameter.canBeFixed()) {
				cFixValue[i] = new JCheckBox("Fixed", parameter.isFixed());
				cFixValue[i].addItemListener(this);
				right.add(cFixValue[i]);
			}
			if (parameter.canBeEstimated()) {
				cEstimate[i] = new JCheckBox("Estimate", parameter.estimate);
				cEstimate[i].addItemListener(this);
				right.add(cEstimate[i]);
			}
			if (parameter.isOutputOnly()) {
				textData[i].setEnabled(false);
			}
		} //loop for all parameters
		dfit.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setMouseActive(false);
				dfit.dispose();
			}
			public void windowActivated(WindowEvent e) {
				updateHist();
			}
		});
		dfit.pack();
	}

	/** 
	 * Show fit dialog box.
	 */
	public void show() {
		setMouseActive(false);
		dfit.show();
	}
	
	MessageHandler getTextInfo(){
		return textInfo;
	}

	/**
	 * Sets whether to receive mouse clicks from display or not.
	 */
	private void setMouseActive(boolean state) {
		if (state) {
			display.addPlotMouseListener(this);
		} else {
			display.removePlotMouseListener(this);
		}
	}

	/**
	 * Takes action when a checkbox is toggled.
	 *
	 * @param	ie  item which changed
	 */
	public void itemStateChanged(ItemEvent ie) {
		try {
			for (int i = 0; i < parameters.size(); i++) {
				Parameter par = (Parameter) (parameters.get(i));
				if (par.canBeFixed()) {
					if (ie.getItemSelectable() == cFixValue[i]) {
						setFixed(par, i);
					}
				}
				if (par.canBeEstimated()) {
					if (ie.getItemSelectable() == cEstimate[i]) {
						setEstimate(par, i);
					}
				}
				if (par.isBoolean()) {
					if (ie.getItemSelectable() == cOption[i]) {
						par.setValue(cOption[i].isSelected());
					}
				}
			}
		} catch (FitException fe) {
			status.setText(fe.getMessage());
		}
	}

	/**
	 *
	 */
	public void plotMousePressed(Point p, Point pPixel) {
		while (parameterEnum.hasNext()) {
			Parameter parameter = (Parameter) parameterEnum.next();
			mouseClickCount++;
			if (parameter.isMouseClickable() && (!parameter.isFixed())) {
				textData[mouseClickCount - 1].setForeground(Color.BLACK);
				textData[mouseClickCount - 1].setText("" + p.x);
				break;
			}
		}
		if (!parameterEnum.hasNext()) {
			setMouseActive(false);
		}
	}

	/**
	 * Set the state to enter values using mouse
	 */
	private void initializeMouse() {
		mouseClickCount = 0;
		ArrayList tempList = new ArrayList();
		for (int i = 0; i < parameters.size(); i++) {
			Parameter parameter = (Parameter) parameters.get(i);
			if (parameter.isMouseClickable() && (!parameter.isFixed())) {
				textData[i].setForeground(Color.RED);
				tempList.add(parameter);
			}
		}
		if (tempList.size() > 0) {
			String temp = "Click spectrum to set: ";
			temp += ((Parameter) tempList.get(0)).getName();
			if (tempList.size() > 1) {
				for (int i = 1; i < (tempList.size() - 1); i++) {
					temp += ", " + ((Parameter) tempList.get(i)).getName();
				}
				temp += " and "
					+ ((Parameter) tempList.get(tempList.size() - 1)).getName();
			}
			status.setText(temp);
		}

		parameterEnum = parameters.iterator();
	}

	/**
	 * Set the parameter values using input in the text fields
	 * just before a fit.
	 */
	private void updateParametersFromDialog() throws FitException {
		Parameter parameter = null;
		for (int i = 0; i < parameters.size(); i++) {
			try {
				parameter = (Parameter) parameters.get(i);
				if (parameter.isDouble()) {
					parameter.setValue(
						Double
							.valueOf(textData[i].getText().trim())
							.doubleValue());
					if (parameter.hasErrorBar()) {
						parameter.setError(
							Double
								.valueOf(
									textError[i].getText().substring(1).trim())
								.doubleValue());
					}
				} else if (parameter.isInteger()) {
					parameter.setValue(
						Integer
							.valueOf(textData[i].getText().trim())
							.intValue());
				} else if (parameter.isBoolean()) {
					parameter.setValue(cOption[i].isSelected());
				}
			} catch (NumberFormatException nfe) {
				clear();
				throw new FitException(
					"Invalid input, parameter: " + parameter.getName());
			}
		}
	}

	/**
	 * Update all fields in the dialog after performing a fit.
	 */
	private void updateDisplay() throws FitException {
		updateHist();
		for (int i = 0; i < parameters.size(); i++) {
			Parameter parameter = (Parameter) parameters.get(i);
			if (parameter.isDouble() || parameter.isInteger()) {
				textData[i].setText(formatValue(parameter));
				if (parameter.canBeFixed()) {
					this.cFixValue[i].setSelected(parameter.isFixed());
					setFixed(parameter,i);
				}
				if (parameter.hasErrorBar()) {
					textError[i].setText(formatError(parameter));
					if (!parameter.isFixed()) {
						textData[i].setBackground(Color.YELLOW);
					}
				}
			}
		}
	}

	/**
	 * Resets all the parameter to default values, which are zero for now.
	 *
	 * @exception   FitException	    thrown if unrecoverable error occurs during reset of dialog
	 */
	public void reset() throws FitException {
		Parameter parameter;

		for (int i = 0; i < parameters.size(); i++) {
			parameter = (Parameter) parameters.get(i);
			if (parameter.isDouble()) {
				parameter.setValue(0.0);
				textData[i].setText(formatValue(parameter));
			} else if (parameter.isInteger()) {
				parameter.setValue(0);
				textData[i].setText(formatValue(parameter));
			}
			if (parameter.hasErrorBar()) {
				parameter.setError(0.0);
				textError[i].setText(formatError(parameter));
			}
		}
		clear();
	}

	private void clear() throws FitException {
		for (int i = 0; i < parameters.size(); i++) {
			Parameter parameter = (Parameter) parameters.get(i);
			if (!parameter.isBoolean()) {
				// text field backgrounds				    	    		    
				if (parameter.isOutputOnly()) {
					textData[i].setEditable(false);
				} else {
					if (!parameter.isText()) {
						textData[i].setBackground(Color.white);
					}
				}
				if (parameter.isFixed()) {
					setFixed(parameter, i);
				}
				if (parameter.canBeEstimated()) {
					setEstimate(parameter, i);
				}
			}
			setMouseActive(false);
		}
	}

	/**
	 * Called when a fix checkbox is toggled.
	 */
	private void setFixed(Parameter param, int index) throws FitException {
		boolean fixed = cFixValue[index].isSelected();
		param.setFixed(fixed);
		if (fixed) {
			if (param.canBeEstimated()) {
				cEstimate[index].setSelected(false);
				param.setEstimate(false);
				cEstimate[index].setEnabled(false);
			}
			if (param.hasErrorBar()) {
				param.setError(0.0);
				textError[index].setText(formatError(param));
				textData[index].setEditable(!fixed);
			}
			//not a fixed value
		} else {
			if (param.canBeEstimated()) {
				cEstimate[index].setEnabled(true);
			}
			if (param.hasErrorBar()) {
				textData[index].setEditable(!fixed);
			}
		}
	}

	/**
	 * a estimate checkbox was toggled
	 */
	private void setEstimate(Parameter param, int index) {
		boolean state;
		state = cEstimate[index].isSelected();
		param.setEstimate(state);
	}

	/**
	 * Makes a <code>parameterTable</code> from <code>parameters</code>.
	 *
	 * @see #parameterTable
	 * @see #parameters
	 */
	protected void addParameter(Parameter newParameter) {
		parameters.add(newParameter);
		parameterTable.put(newParameter.getName(), newParameter);
	}

	/**
	 * Accesses a parameter in <code>parameterTable</code> by name.
	 * 
	 * @param	which contains the name of the parameter (same name displayed in dialog box)
	 * @return	the <code>Parameter</code> object going by that name
	 */
	protected Parameter getParameter(String which) {
		return (Parameter) (parameterTable.get(which));
	}

	/**
	 * Gets the contents of <code>parameters</code>. 
	 *
	 * @return	the contents of <code>parameters</code> 
	 * @see	#parameters
	 */
	java.util.List getParameters() {
		return parameters;
	}

	/**
	 * Update the name of the displayed histogram in the dialog box.
	 */
	private void updateHist() {
		final Histogram h = display.getHistogram();
		if (h != null && h.getDimensionality() == 1) {
			if (h.getType() == Histogram.ONE_DIM_INT) {
				final int [] ia = (int[]) h.getCounts();
				counts = new double[ia.length];
				for (int j = 0; j < ia.length; j++) {
					counts[j] = ia[j];
				}
			} else if (h.getType() == Histogram.ONE_DIM_DOUBLE) {
				counts = (double[]) h.getCounts();
			}
			textHistName.setText(h.getName());
		} else { //2d
			textHistName.setText("Need 1D Hist!");
		}
	}

	/**
	 * Draw the fit with the values in the fields.
	 */
	private void drawFit() throws FitException {
		double [][] signals=null;
		double [] residuals=null;
		double [] background=null;
		updateParametersFromDialog();
		int numChannel = upperLimit-lowerLimit+1;
		//double[] evaluated = new double[numChannel];
		if (getNumberOfSignals()>0){
			signals=new double[getNumberOfSignals()][numChannel];
			for (int sig=0; sig<signals.length; sig++){
				for (int i=0; i<numChannel; i++){
					signals[sig][i]=calculateSignal(sig,i+lowerLimit);
				}
			}
		}
		if (this.residualOption){		
			residuals = new double[numChannel];
			for (int i = 0; i < numChannel; i++) {
				int chan=i+lowerLimit;
				residuals[i] = counts[chan] - calculate(chan);
			}
		}
		if (this.hasBackground()){
			background = new double[numChannel];
			for (int i=0; i<numChannel; i++){
				background[i]=this.calculateBackground(i+lowerLimit);
			}
		}
		display.displayFit(signals,background,residuals,lowerLimit);
	}

	/**
	 * Gets counts from currently displayed <code>Histogram</code>
	 */
	private void getCounts() {
		Histogram h = display.getHistogram();
		if (h.getType() == Histogram.ONE_DIM_INT) {
			int[] ia = (int[]) h.getCounts();
			counts = new double[ia.length];
			for (int j = 0; j < ia.length; j++) {
				counts[j] = ia[j];
			}
		} else if (h.getType() == Histogram.ONE_DIM_DOUBLE) {
			counts = (double[]) h.getCounts();
		}
		this.errors = h.getErrors();
	}

	/**
	 * Formats values in number text fields.
	 */
	private String formatValue(Parameter param) throws FitException {
		String temp = "Invalid Type"; //default return value
		if (param.isDouble()) {
			double value = param.getDoubleValue();
			if (param.hasErrorBar()) {
				double error = param.getDoubleError();
				temp = format(value, error)[0];
			} else {
				int integer = (int) log10(Math.abs(value));
				integer = Math.max(integer, 1);
				int fraction = Math.max(4 - integer, 0);
				temp = format(value, fraction);
			}
		} else if (param.isInteger()) {
			temp = (new Integer(param.getIntValue())).toString().trim();
		} else if (param.isText()) {
			temp = param.getText();
		}
		return temp;
	}

	private String formatError(Parameter param) throws FitException {
		if (!param.hasErrorBar())
			throw new FitException("No error term for this parameter.  Can't formatError().");
		return "\u00b1 "
			+ format(param.getDoubleValue(), param.getDoubleError())[1];
	}

	private String format(double value, int fraction) {
		NumberFormat fval;
		fval = NumberFormat.getInstance();
		fval.setGroupingUsed(false);
		fval.setMinimumFractionDigits(fraction);
		fval.setMinimumFractionDigits(fraction);
		return fval.format(value);
	}

	/**
	 *
	 */
	private String[] format(double value, double err) throws FitException {
		String[] out;
		NumberFormat fval, ferr;
		int temp;

		out = new String[2];
		fval = NumberFormat.getInstance();
		fval.setGroupingUsed(false);
		ferr = NumberFormat.getInstance();
		ferr.setGroupingUsed(false);
		if (err < 0.0)
			throw new FitException("format() can't use negative error.");
		if (err > 0.0) {
			temp = fractionDigits(err);
			ferr.setMinimumFractionDigits(temp);
			ferr.setMaximumFractionDigits(temp);
			fval.setMinimumFractionDigits(temp);
			fval.setMaximumFractionDigits(temp);
			temp = integerDigits(err);
			ferr.setMinimumIntegerDigits(temp);
			//ferr.setMaximumIntegerDigits(temp);
			fval.setMinimumIntegerDigits(1);
		} else {
			ferr.setMinimumFractionDigits(1);
			ferr.setMaximumFractionDigits(1);
			ferr.setMinimumIntegerDigits(1);
			ferr.setMaximumIntegerDigits(1);
			fval.setMinimumFractionDigits(1);
			fval.setMinimumIntegerDigits(1);
		}
		out[0] = fval.format(value);
		out[1] = ferr.format(err);
		return out;
	}

	private double log10(double x) {
		return Math.log(x) / Math.log(10.0);
	}

	/**
	 * Given an error, determines the appropriat number of fraction digits to show.
	 */
	private int fractionDigits(double err) throws FitException {
		int out;

		if (err == 0.0)
			throw new FitException("fractionDigits called with 0.0");
		if (err >= 2.0) {
			out = 0;
		} else if (err >= 1.0) {
			out = 1;
		} else if (firstSigFig(err) == 1) {
			out = decimalPlaces(err, 2);
		} else { // firstSigFig > 1
			out = decimalPlaces(err, 1);
		}
		return out;
	}

	/**
	 * Given an error term determine the appropriate number of integer digits to display.
	 */
	private int integerDigits(double err) throws FitException {
		int out;

		if (err == 0.0)
			throw new FitException("integerDigits called with 0.0");
		if (err >= 1.0) {
			out = (int) Math.ceil(log10(err));
		} else {
			out = 1;
		}
		return out;
	}

	/**
	 * Given a double, returns the value of the first significant decimal digit.
	 */
	private int firstSigFig(double x) throws FitException {
		if (x <= 0.0)
			throw new FitException("Can't call firstSigFig with non-positive number.");
		return (int) Math.floor(x / Math.pow(10.0, Math.floor(log10(x))));
	}

	/**
	 * Given a double between zero and 1, and number of significant figures desired, return
	 * number of decimal fraction digits to display.
	 */
	private int decimalPlaces(double x, int sigfig) throws FitException {
		int out;
		int pos; //position of firstSigFig

		if (x <= 0.0 || x >= 1.0)
			throw new FitException("Must call decimalPlaces() with x in (0,1).");
		if (sigfig < 1)
			throw new FitException("Can't have zero significant figures.");
		pos = (int) Math.abs(Math.floor(log10(x)));
		out = pos + sigfig - 1;
		return out;
	}
	
	/**
	 * Returns the number of signals modelled by the fit.
	 * 
	 * @return the number of signals modelled by the fit
	 */
	abstract int getNumberOfSignals();
	
	/**
	 * Returns the evaluation of a signal at one particular channel.
	 * Should return zero if the given signal doesn't exist.
	 *
	 * @return
	 */
	abstract double calculateSignal(int signal, int channel);
	
	/**
	 * 
	 * @return whether fit function has a background component
	 */
	abstract boolean hasBackground();
	
	abstract double calculateBackground(int channel);
	
	/**
	 * Calculates function value for a specific channel.  Uses whatever the current values are for the parameters in 
	 * <code>parameters</code>.
	 *
	 * @param	channel channel to evaluate fit function at
	 * @return	double containing evaluation 
	 */
	public abstract double calculate(int channel);
	

}