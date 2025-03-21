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
package dwvisser.math;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.DecimalFormat;

public class UncertainNumber implements Serializable {

	/**
	 * The best estimate of the value.
	 */
	public double value;

	/**
	 * The uncertainty.
	 */
	public double error;

	/**
	 * Constructor.
	 * 
	 * @param value value of number
	 * @param error uncertainty in value
	 */
	public UncertainNumber(double value, double error) {
		this.value = value;
		this.error = error;
	}

	/**
	 * Constructor for a fixed value.
	 * 
	 * @param value value of number
	 */
	public UncertainNumber(double value) {
		this(value, 0.0);
	}

	/**
	 * Add this number to the given number, propagating errors.
	 * 
	 * @param x number to add
	 * @return sum
	 */
	public UncertainNumber plus(UncertainNumber x) {
		return new UncertainNumber(
			this.value + x.value,
			Math.sqrt(this.error * this.error + x.error * x.error));
	}

	/**
	 * Subtract the given number from this number, propagating errors.
	 * 
	 * @param x number to subtract
	 * @return difference
	 */
	public UncertainNumber minus(UncertainNumber x) {
		return new UncertainNumber(
			this.value - x.value,
			Math.sqrt(this.error * this.error + x.error * x.error));
	}

	public UncertainNumber minus(double x) {
		return minus(new UncertainNumber(x));
	}

	public UncertainNumber times(UncertainNumber x) {
		return new UncertainNumber(
			value * x.value,
			Math.sqrt(
				error * error * x.value * x.value
					+ x.error * x.error * value * value));
	}

	public UncertainNumber times(double x) {
		return times(new UncertainNumber(x, 0.0));
	}

	public UncertainNumber divide(UncertainNumber x) {
		return times(invert(x));
	}
	public UncertainNumber divide(double x) {
		return divide(new UncertainNumber(x, 0));
	}

	private UncertainNumber invert(UncertainNumber x) {
		return new UncertainNumber(
			1.0 / x.value,
			x.error / (x.value * x.value));
	}

	public String toString() {
		return format();
	}

	public String plusMinusString() {
		return value + " +- " + error;
	}

	/**
	 * @return true only if value and error are equal
	 */
	public boolean equals(Object o) {
		if (!this.getClass().isInstance(o))
			return false;
		UncertainNumber un = (UncertainNumber) o;
		if (value != un.value)
			return false;
		if (error != un.error)
			return false;
		return true; //all conditions passed if this line reached
	}
	
	/**
	 * Override of hashcode to make sure that UncertainNumber's
	 * with the same value and error produce the same hashcode.
	 * 
	 * @return the sum of hashcodes for Double objects containing this.value and this.error
	 */
	public int hashcode(){
		return (new Double(value).hashCode())+(new Double(error).hashCode());
	}

	private String format() {
		NumberFormat fval = NumberFormat.getInstance();
		fval.setGroupingUsed(false);
		if (error > 0.0) {
			int temp = fractionDigits();
			fval.setMinimumFractionDigits(temp);
			fval.setMaximumFractionDigits(temp);
			fval.setMinimumIntegerDigits(1);
			return fval.format(value) + abbreviatedError();
		} else { //=0.0
			return String.valueOf(value);
		}
	}

	private double log10(double x) {
		return Math.log(x) / Math.log(10.0);
	}

	/**
	 * Using the error, determines the appropriate number of fraction digits in 
	 * the <em>value</em> to show.
	 */
	private int fractionDigits() {
		int rval = 0;
		if (error < 1.0) {
			rval = decimalPlaces(error, errSigFigDisplay());
		} else if (error < 3.55){
			rval=1;
		} else {//greater than 3.55
			rval=0; 
		}
		return rval;
	}

	/**
	 * Returns a string containing the parenthetical error term.
	 */
	private String abbreviatedError() {
		int threeDigits = this.firstThreeErrorDigits();
		String rval = "10";
		/* default case where 1st 3 sig. fig. are in set [950,999] */
		if (threeDigits < 355) {
			rval =  Integer.toString((int)Math.round(threeDigits * 0.1));
		} else if (threeDigits < 950) {
			rval = Integer.toString((int)Math.round(threeDigits * 0.01)) ;
		}
		if (minimumIntegerDigits() > rval.length()){
			for (int i=rval.length(); i<minimumIntegerDigits(); i++){
				rval += "0";
			}
		}
		return "("+rval+")";
	}

	/**
	 * Return the minimum number of digits to show in the error term. 
	 */
	private int minimumIntegerDigits() {
		int rval=1;//default value
		if (error >= 1.0) {
			rval = (int) Math.ceil(log10(error));
		}
		if (this.firstThreeErrorDigits()>949){
			rval++;
		}
		return rval;
	}

	/**
	 * Given an error term determine the appropriate number of integer digits
	 * to display in that error term, either 1 or 2. I use PRD v.66, 010001, p. 12 
	 * recommendations.
	 */
	private int errSigFigDisplay() {
		int rval = 1; //if first three sig. figs in error in [355,949]
		int threeDigits = firstThreeErrorDigits();
		if (threeDigits < 355 || threeDigits > 949) {
			rval = 2;
		}
		return rval;
	}

	private int firstThreeErrorDigits() {
		DecimalFormat df = new DecimalFormat("000E0");
		String temp = df.format(this.error);
		return Integer.parseInt(temp.substring(0, 3));
	}

	/**
	 * Given a double between zero and 1, and number of significant figures 
	 * desired, return the number of decimal fraction digits to display.
	 */
	private int decimalPlaces(double x, int sigfig) {
		int pos = (int) Math.abs(Math.floor(log10(x)));//position of firstSigFig
		int rval = pos + sigfig - 1;
		if (this.firstThreeErrorDigits()>949){
			rval--;
		}
		return rval;
	}

	public static void main(String[] args) {
		System.out.println(new UncertainNumber(709.8, 35.4));
		System.out.println(new UncertainNumber(709.8, 35.5));
		System.out.println(new UncertainNumber(709.8, 3.54));
		System.out.println(new UncertainNumber(709.8, 3.55));
		System.out.println(new UncertainNumber(709.8, .354));
		System.out.println(new UncertainNumber(709.8, .355));
		System.out.println(new UncertainNumber(709.8, 94.9));
		System.out.println(new UncertainNumber(709.8, 95.0));
		System.out.println(new UncertainNumber(709.8, 9.49));
		System.out.println(new UncertainNumber(709.8, 9.50));
		System.out.println(new UncertainNumber(709.8, .949));
		System.out.println(new UncertainNumber(709.8, .950));
	}
}
