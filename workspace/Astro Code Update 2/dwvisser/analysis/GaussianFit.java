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
package dwvisser.analysis;

/**
 * This abstract class uses <code>NonLinearFit</code> to fit a single gaussian peak with a background..
 * The background is a polynomial up to a quadradic term if desired. (Channel - Centroid) is the term
 * the polynomial is expanded in.
 *
 * @author  Dale Visser
 * @version 0.5, 20 February 2001
 *
 * @see	    NonLinearFit
 */
public class GaussianFit extends NonLinearFit {

    /**
     * magic number for calculating
     */
    static private final double a=0.93911;

    /**
     * magic number for calculating
     */
    static private final double b=2.77066;

    /**
     * magic number for calculating
     */
    static private final double c=5.20391;

    /**
     * magic number for calculating
     */
    static final double SIGMA_TO_FWHM=2.354;

    /**
     * name of <code>Parameter</code>--centroid of peak
     */
    public static final String CENTROID="Cent";

    /**
     * name of <code>Parameter</code>--width of peak
     */
    public static final String WIDTH="Wide";

    /**
     * name of <code>Parameter</code>--area of peak
     */
    public static final String AREA="Area";

    /**
     * function <code>Parameter</code>--area of peak
     */
    private Parameter [] area;

    /**
     * function <code>Parameter</code>--centroid of peak
     */
    private Parameter [] centroid;

    /**
     * function <code>Parameter</code>--wodth of peak
     */
    private Parameter [] width;

    /**
     * used for calculations
     */
    private double diff;

    /**
     * used for calculations
     */
    private double exp;

    private Multiplet multiplet;

    /**
     * Class constructor.
     */
    public GaussianFit(double [] spectrum, double [] errors, int begin, int end, Multiplet m) 
                        throws FitException {
        super(spectrum, errors, begin, end);
        area = new Parameter[m.size()];
        centroid = new Parameter[m.size()];
        width = new Parameter[m.size()];
        multiplet=m;
        for (int i=0; i< m.size(); i++) {
            area[i]=new Parameter(AREA+i, Parameter.DOUBLE);
            area[i].setValue(m.getPeak(i).getArea());
            centroid[i]=new Parameter(CENTROID+i, Parameter.DOUBLE);
            centroid[i].setValue(m.getPeak(i).getPosition());
            width[i]=new Parameter(WIDTH+i, Parameter.DOUBLE);
            width[i].setValue(m.getPeak(i).getWidth());
            addParameter(area[i]);
            addParameter(centroid[i]);
            addParameter(width[i]);
        }
    }

    /**
     * If so requested, estimates A, Area, and Width.
     *
     * @exception   FitException	    thrown if unrecoverable error occurs during estimation
     */
    public void estimate() throws FitException{
    }

    /**
     * Calculates the gaussian with background at a given x.
     *
     * @param	x   value to calculate at
     * @return	    value of function at x
     */
    public double valueAt(double x){
        double temp = 0.0;
        for (int i=0; i<multiplet.size(); i++){
            diff=x-p(CENTROID+i);
            exp=Math.exp(-b*diff*diff/(p(WIDTH+i)*p(WIDTH+i)));
            temp += p(AREA+i)/p(WIDTH+i)*a*exp;
        }
        return temp;
    }

    /**
     * Evaluates derivative with respect to 
     * <code>parameterName</code> at <code>x</code>.
     *
     * @param parName the name of the parameter to differentiate with
     * respect to
     * @param x value to evalueate at
     * @return df(<code>x</code>)/d(<code>parameterName</code>) at x
     */
    public double derivative(double x, String parName){
        double temp=0.0;
        int whichPeakNum = Integer.parseInt(parName.substring(4));
        String whichParamType = parName.substring(0,4);
        double amp=p(AREA+whichPeakNum);
        double cen=p(CENTROID+whichPeakNum);
        double wid=p(WIDTH+whichPeakNum);
        diff=x-cen;
        exp=Math.exp(-b*diff*diff/(wid*wid));

        if (whichParamType.equals(AREA)){
            temp=a/wid*exp;
        } else if (whichParamType.equals(CENTROID)){
            temp=c*amp*exp*diff/(wid*wid*wid);
        } else if (whichParamType.equals(WIDTH)) {
            temp=-a*amp*exp/(wid*wid);
            temp=temp + c*amp*exp*diff*diff/(wid*wid*wid*wid);
        } else {//not valid
            temp=0.0;
            System.err.println("Invalid derivative argument: "+parName);
        }
        return temp;
    }

    public Multiplet getFitResult(){
        Multiplet rval = new Multiplet();
        for (int p=0; p<area.length; p++) {
            rval.addPeak(new Peak(centroid[p].getDoubleValue(), centroid[p].getDoubleError(), 
                                    area[p].getDoubleValue(), area[p].getDoubleError(),
                                    width[p].getDoubleValue(), width[p].getDoubleError()));
        }
        return rval;
    }
        
}