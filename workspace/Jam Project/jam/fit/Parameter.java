/**
 */
package jam.fit;

/**
 * <p>Parameters used to fit. </p>
 * <dl>
 * <dt>INT</dt>	<dd>integer, such as Number of Peaks, or Minimum Channel</dd>    
 *	<dt>DOUBLE</dt>	<dd>standard variable fit parameter, includes a "fix value" checkbox </dd>    
 *	<dt>TEXT</dt>	<dd>field showing fit function and/or brief instructions </dd>    
 *	<dt>BOOLEAN</dt>	<dd>true/false option, such as Include Background, or Display Output (how this would be
 *			implemented, I'm not sure)</dd>
 * </dl>
 * <p>Options. You can use as many options as you want.</p>
 * <dl>
 *	<dt>(NO_)OUTPUT</dt>	<dd>is calculated and has no associated error bars (e.g. Chi-Squared)</dd>
 *	<dt>(NO_)MOUSE</dt>	<dd> value can be obtained with mouse from screen</dd>
 *  <dt>(NO_)ESTIMATE</dt>	<dd>can be estimated</dd>
 * <dt>(NO_)FIX</dt><dd>value is fixed..do not vary during fit</dd>
 * <dt>(NO_)KNOWN</dt><dd>input only, not varied by routine</dd>
 * </dl>
 */
public final class Parameter {

	/* used to determine the type */ 
	private final static int TYPE_MASK = 7;
	
	/**  
	 * Default paramter type, a floating point number.
	 */
	public final static int DOUBLE = 0;

	/**
		 * Parameter is an integer number, e.g., a histogram channel number.
		 */
	public final static int INT = 1;

	/**
		 * Parameter is a boolean value...displayed as a checkbox.
		 */
	public final static int BOOLEAN = 2;

	/**
	 * Parameter is simply a text box.
	 */
	public final static int TEXT = 4;

	/* Options */
	public final static int FIX = 16;
	public final static int NO_FIX = 0; //default    
	public final static int ESTIMATE = 32;
	public final static int NO_ESTIMATE = 0; //default            
	public final static int MOUSE = 64;
	public final static int NO_MOUSE = 0; //default
	public final static int OUTPUT = 128;
	public final static int NO_OUTPUT = 0; //default
	public final static int KNOWN = 256;
	public final static int NO_KNOWN = 0; //default

	/* *
	 * type contains the parameter type.  Its value is available via getType().
	 * The types are: boolean,double,text, or int
	 *	input/output or output only
	 *	clickable
	 *	estimable
	 */
	private int options;
	private int type;
	//    int index;			    //not currently used
	
	/* the storage for the options and their default values */
	
	/* whether there is an error bar */
	private boolean errorOption = true;
	/* whether an estimation routine is provided */
	private boolean estimateOption = false;
	/* whether the parameter has been fixed */
	private boolean fixOption = false;
	/* whether the parameter can be set by clicking */
	private boolean mouseOption = false;
	/* whether the parameter is output only */
	private boolean outputOption = false;
	/* not sure */
	private boolean knownOption = false;
	//parameter name
	private String name;
	//double fields
	private double valueDbl;
	// error field
	private double errorDbl;
	//int fields
	private int valueInt;
	//boolean fields
	private boolean valueBln;
	//TEXT fields
	private String valueTxt;

	/**
	 * Whether or not the parameter is currently fixed.
	 */
	private boolean fix;

	/**
	 * Whether or not this parameter should be estimated automatically before doing fit.
	 */
	protected boolean estimate;

	public Parameter(
		String name,
		int options) { //default variable parameter instance
		this.name = name;
		this.options = options;
		type = options & TYPE_MASK;
		if (type == Parameter.TEXT){
			options |= KNOWN; //change default for TEXT
		}
		valueInt = 0;
		valueDbl = 0.0;
		errorDbl = 0.0;
		fix = false;
		estimate = false;

		if (type == BOOLEAN) {
			errorOption = false;
		}
		if ((options & Parameter.FIX) != 0) {
			fixOption = true;
		}
		if ((options & Parameter.ESTIMATE) != 0) {
			estimateOption = true;
		}
		if ((options & Parameter.MOUSE) != 0) {
			mouseOption = true;
		}
		if ((options & Parameter.OUTPUT) != 0) {
			outputOption = true;
		}
		if ((options & Parameter.KNOWN) != 0) {
			knownOption = true;
			errorOption = false;//default=true
		}

	}

	public Parameter(String name, int option1, int option2) {
		this(name, option1 | option2);
	}

	public Parameter(String name, int option1, int option2, int option3) {
		this(name, option1 | option2 | option3);
	}

	public Parameter(
		String name,
		int option1,
		int option2,
		int option3,
		int option4) {
		this(name, option1 | option2 | option3 | option4);
	}

	public Parameter(
		String name,
		int option1,
		int option2,
		int option3,
		int option4,
		int option5) {
		this(name, option1 | option2 | option3 | option4 | option5);
	}

	public Parameter(
		String name,
		int option1,
		int option2,
		int option3,
		int option4,
		int option5,
		int option6) {
		this(name, option1 | option2 | option3 | option4 | option5 | option6);
	}

	//END OF CONSTRUCTORS

	public String getName() {
		return name;
	}
	
	public int getType() {
		return type;
	}

	public synchronized void setFixed(boolean state) {
		this.fix = state;
	}

	/**
	 * Tells whether the parameter is currently fixed.
	 */
	public synchronized boolean isFixed() {
		return fix;
	}
	
	public boolean canBeFixed(){
		return this.fixOption;
	}
	
	public boolean hasErrorBar(){
		return this.errorOption;
	}

	public synchronized void setEstimate(boolean state) {
		estimate = state;
	}

	public synchronized boolean isEstimate() {
		return (fix ? false : estimate);
	}

	/**
	 * Set the floating point value.
	 *
	 * @throws UnsupportedOperationException if this is not a DOUBLE parameter
	 */
	public synchronized void setValue(double value) {
		if (type == DOUBLE) {
			valueDbl = value;
		} else {
			throw new UnsupportedOperationException(
				"Parameter '" + name + "' can't set a double value.");
		}
	}

	/**
	 * Set the floating point value.
	 *
	 * @throws UnsupportedOperationException if this is not a DOUBLE parameter with
	 * error bar
	 */
	public synchronized void setValue(double value, double error) {
		if (errorOption){
			valueDbl = value;
			errorDbl = error;
		} else {
			throw new UnsupportedOperationException(
				"Parameter '" + name + "' can't set a double value and error bar.");
		}
	}

	/**
	 * Set the floating point value.
	 *
	 * @throws UnsupportedOperationException if this is not an INT parameter
	 */
	public synchronized void setValue(int value) {
	if (type == INT){
		valueInt = value;
	} else {
		throw new UnsupportedOperationException(
			"Parameter '" + name + "' can't set an integer value.");
	}
	}

	public synchronized void setValue(String text) {
		valueTxt = text;
	}
	
	public synchronized void setValue(boolean flag) {
		valueBln = flag;
	}

	public synchronized void setError(double err) {
		errorDbl = err;
	}

	public synchronized double getDoubleValue() {
		return valueDbl;
	}

	public synchronized int getIntValue() {
		return valueInt;
	}

	public synchronized boolean getBooleanValue() {
		return valueBln;
	}

	public synchronized double getDoubleError() {
		return errorDbl;
	}

	public boolean isBoolean() {
		return (type == BOOLEAN);
	}

	public boolean isNumberField() {
		return (type == INT || type == DOUBLE);
	}
	
	public boolean isDouble(){
		return type==DOUBLE;
	}
	public boolean isInteger(){
		return type==INT;
	}
	
	public synchronized String getText(){
		return valueTxt;
	}

	/**
	 * Returns true if the parameter is represented by a text field in the dialog box.
	 */
	public boolean isText() {
		return (type == TEXT);
	}

	public boolean canBeEstimated() {
		return ((options & ESTIMATE) != 0);
	}
	
	public boolean isKnown(){
		return knownOption;
	}
	
	public boolean isOutputOnly(){
		return outputOption;
	}
	
	public boolean isMouseClickable(){
		return mouseOption;
	}
}