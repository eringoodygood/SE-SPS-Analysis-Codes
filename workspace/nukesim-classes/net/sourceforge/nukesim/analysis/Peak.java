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
 * Peak.java
 *
 * Created on February 14, 2001, 1:21 PM
 */

package net.sourceforge.nukesim.analysis;

/** This class represents a gaussian peak, in terms of it's properties.  Fields are
 * also provided for the error bars on these properties.
 * @author Dale
 */
public class Peak extends Object implements Comparable {

    private double position, area, width;
    private double perr, aerr, werr;

    /** Creates new Peak  assuming no uncertainty in values.
     * @param position position of the peak centroid
     * @param area total peak area
     * @param width Full width at half max of the peak
     */
    public Peak(double position, double area, double width) {
        this(position, 0.0, area, 0.0, width, 0.0);
    }

    /** Generates a peak with error bars on its parameters.
     * @param p position of peak centroid
     * @param pe error on position
     * @param a area of peak
     * @param ae uncertainty in area
     * @param w FWHM of peak
     * @param we uncertainty in FWHM
     */
    public Peak(double p,double pe, double a, double ae,  double w, double we) {
        setPosition(p,pe);
        setArea(a,ae);
        setWidth(w,we);
    }

    /**
     * @return position of the peak centroid
     */
    public double getPosition() {
        return position;
    }

    public double getArea() {
        return area;
    }

    public double getWidth() {
        return width;
    }

    public double getPositionError() {
        return perr;
    }

    public double getAreaError() {
        return aerr;
    }

    public double getWidthError() {
        return werr;
    }

    public void setPosition(double p) {
        setPosition(p,0.0);
    }

    public final void setPosition(double p, double e){
        position=p;
        perr=e;
    }

    public void setArea(double a){
        setArea(a,0.0);
    }

    public final void setArea(double a, double e){
        area=a;
        aerr=e;
    }

    public void setWidth(double w){
        setWidth(w,0.0);
    }

    public final void setWidth(double w, double e){
        width=w;
        werr=e;
    }

    public String toString() {
        String rval = "Peak\n";
        rval += "  Position = "+position+" +/- "+perr+"\n";
        rval += "  Area = "+area+" +/- "+aerr+"\n";
        rval += "  FWHM = "+width+" +/- "+werr+"\n";
        return rval;
    }

    public int compareTo(Object p1) {
        if (getPosition() < ((Peak)p1).getPosition()){
            return -1;
        } else if (getPosition() > ((Peak)p1).getPosition()){
            return 1;
        } else {
            return 0;
        }
    }
            
}