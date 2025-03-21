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
package dwvisser.nuclear.swing;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;

/**
* A special JComboBox for calling up several related dialogues.  If valid
* entries are recieved in the dialogues, an array of double values specified
* by the user is returned.
*
* @author <a href="mailto:dale@visser.name">Dale W Visser</a>
* @version 1.1
*/
public class ValuesChooser extends JPanel implements ActionListener {

	private static final String RANGE = "Min Max Delta";
	private static final String MULT = "List of values";

	private static final String[] LIST = {MULT, RANGE};
	private JComboBox choice = new JComboBox(LIST);
	private String quantity, units;
	private JTextField entry;
	
	/**
	 *
	 */
	private double[] values = null;
	private double min,max,del;
	private double temp_min,temp_max,temp_del;
	private ValuesListener vl;

	/**
	 * Create a new values chooser.
	 * 
	 * @param vl listener for changed values
	 * @param quantity name of category
	 * @param units units of category if appropriate, leave null if dimensionless
	 * @param values initial values to display
	 */
	public ValuesChooser(
		ValuesListener vl,
		String quantity,
		String units,
		double[] values) {
			entry = new JTextField("" + values[0]);
		this.vl = vl;
		this.quantity = quantity;
		this.units = units;
		setValues(values);
		setLayout(new GridLayout(3, 1));
		if (units != null){
			add(new Label(quantity + " [" + units + "]"));
		} else {
			add(new Label(quantity));
		}
		add(choice);
		entry.addActionListener(this);
		add(entry);
		setBorder(new EtchedBorder());
		choice.setToolTipText("Chooses method of entering values.");
	}

	/**
	 * Called whenever enter is pressed in the text area. It builds the array
	 * of values and calls the code which sends notification to the 
	 * <code>ValuesListener</code>.
	 * 
	 * @param ae events produced by the text area
	 */
	public void actionPerformed(ActionEvent ae) {
		String sval = entry.getText();
		String value = (String) choice.getSelectedItem();
 		if (value == RANGE) {
			if (sval != null) {
				try {
					StreamTokenizer st =
						new StreamTokenizer(new StringReader(sval));
					st.nextToken();
					temp_min = st.nval;
					st.nextToken();
					temp_max = st.nval;
					st.nextToken();
					temp_del = st.nval;
					int numVal = (int) Math.floor((temp_max - temp_min) / temp_del);
					double[] temp = new double[numVal + 1];
					double val = temp_min;
					int counter = 0;
					for (counter = 0; val <= temp_max; val += temp_del, counter++) {
						temp[counter] = val;
					}
					if (counter == numVal + 1) {
						setValues(temp);
					} else if (counter == numVal) {
						double[] temp2 = new double[numVal];
						System.arraycopy(temp, 0, temp2, 0, numVal);
						setValues(temp2);
					}
				} catch (IOException ioe) {
					System.err.println(ioe);
				}
			}
		} else if (value == MULT) {
			if (sval != null) {
				try {
					Vector tempV = new Vector();
					StreamTokenizer st =
						new StreamTokenizer(new StringReader(sval));
					while (st.nextToken() == StreamTokenizer.TT_NUMBER) {
						tempV.add(new Double(st.nval));
					}
					int numVal = tempV.size();
					double[] temp = new double[numVal];
					for (int counter = 0; counter < numVal; counter++) {
						temp[counter] =
							((Double) (tempV.get(counter))).doubleValue();
					}
					setValues(temp);
				} catch (IOException ioe) {
					System.err.println(ioe);
				}
			}
		}
	}

	final private void setValues(double[] da) {
		if (sendValues(da)){
			values = da;
			if (choice.getSelectedItem().equals(RANGE)){	
				min=temp_min;
				max=temp_max;
				del=temp_del;
			}
		}
		setTextToValues();
	}

	/**
	 * Returns the array of numbers represented by what the user
	 * has entered.
	 */
	public double[] getValues() {
		return values;
	}

	/**
	 * Notify the <code>ValuesListener</code> of any changes.
	 * 
	 * @see ValuesListener
	 */
	public final boolean sendValues(double [] sendvals) {
		return vl.receiveValues(this, sendvals);
	}
	
	private void setTextToValues(){
		String val="";
		if (choice.getSelectedItem().equals(MULT)){
			for (int i=0; i<values.length; i++){
				val += values[i]+" ";
			}
		} else {//RANGE
			val = min+" "+max+" "+del;
		}
		entry.setText(val);
	}
}
