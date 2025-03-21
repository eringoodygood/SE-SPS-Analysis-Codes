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
package net.sourceforge.nukesim.nuclear.swing;
import jade.physics.Quantity;
import jade.units.Unit;

import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

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
	private final transient JComboBox choice = new JComboBox(LIST);
	private final transient String name;
	private final transient Unit units;
	private final transient JTextField entry;
	
	/**
	 *
	 */
	private Quantity [] values = null;
	private double min,max,del;
	private double tempMin,tempMax,tempDel;
	private final ValuesListener valuesListener;

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
		Unit units,
		double[] values) {
			entry = new JTextField("" + values[0]);
		this.valuesListener = vl;
		this.name = quantity;
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
					StreamTokenizer parser =
						new StreamTokenizer(new StringReader(sval));
					parser.nextToken();
					tempMin = parser.nval;
					parser.nextToken();
					tempMax = parser.nval;
					parser.nextToken();
					tempDel = parser.nval;
					int numVal = (int) Math.floor((tempMax - tempMin) / tempDel);
					double[] temp = new double[numVal + 1];
					double val = tempMin;
					int counter = 0;
					for (counter = 0; val <= tempMax; val += tempDel, counter++) {
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
					List tempV = Collections.synchronizedList(new ArrayList());
					StreamTokenizer parser =
						new StreamTokenizer(new StringReader(sval));
					while (parser.nextToken() == StreamTokenizer.TT_NUMBER) {
						tempV.add(new Double(parser.nval));
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

	final private void setValues(double[] _values) {
		if (sendValues(_values)){
			final int len=_values.length;
			values = new Quantity[len];
			for (int i=0; i< len; i++){
				values[i] = Quantity.valueOf(_values[i],units);
			}
			if (choice.getSelectedItem().equals(RANGE)){	
				min=tempMin;
				max=tempMax;
				del=tempDel;
			}
		}
		setTextToValues();
	}

	/**
	 * Returns the array of numbers represented by what the user
	 * has entered.
	 */
	public Quantity [] getValues() {
		return values;
	}

	/**
	 * Notify the <code>ValuesListener</code> of any changes.
	 * 
	 * @see ValuesListener
	 */
	public final boolean sendValues(double [] sendvals) {
		return valuesListener.receiveValues(this, sendvals);
	}
	
	private void setTextToValues(){
		final StringBuffer val=new StringBuffer();
		if (choice.getSelectedItem().equals(MULT)){
			for (int i=0; i<values.length; i++){
				val.append(valueText(values[i])).append(' ');
			}
		} else {//RANGE
			val.append(min).append(' ').append(max).append(' ').append(del);
		}
		entry.setText(val.toString());
	}
	
	public Unit getUnits(){
		return units;
	}
	
	private String valueText(Quantity value) {
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
}
