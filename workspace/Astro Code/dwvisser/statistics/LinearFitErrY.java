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
package dwvisser.statistics;
import java.io.*;
import java.util.Vector;

/**
 * Given a data set, performs a linear regression, then can be queried for the results.
 * Based on code in Numerical Recipes in C
 *
 * @author Dale Visser
 */
public final class LinearFitErrY {

    private double a; // constant fit coefficient
    private double b; // slope fit coefficient
    private double siga; // error in a
    private double sigb; // error in b
    private double chi2; // chi-sqared statistic
    private double q; // p-value
    private int dof; //degrees of freedom

    private boolean weighted;
    double [] x, y , sig;

    public double [] residual;
    
    
    /**
     * do-nothing initializer to re-use this same code for multiple fits
     */
    public LinearFitErrY(){
    	//do nothing
    }

    /**
     * Creates and performs linear regression on weighted data set.
     *
     * @param x the x coordinates of the points
     * @param y the y coordinates of the points
     * @param sig the error bars on the y coordinates
     */
    public LinearFitErrY(double x[], double y[], double sig[]) throws StatisticsException {
		fit(x,y,sig);
    }

    /**
     * Creates and performs linear regression on unweighted data set.
     *
     * @param x the x coordinates of the points
     * @param y the y coordinates of the points
     */
    public LinearFitErrY(double [] x, double [] y) throws StatisticsException {
    	fit(x,y);
    }
    
    public void fit(double x[], double y[], double sig[]) 
    throws StatisticsException {
        if (x.length != y.length)  {
            throw new StatisticsException(getClass().getName()+": Array lengths not equal x["+x.length+
            "] and y["+y.length+"].");
        }
        if ( x.length != sig.length){
            throw new StatisticsException(getClass().getName()+": Array lengths not equal x,y["+x.length+
            "] and sig["+sig.length+"].");
        }
        weighted=true;
        this.x = x;
        this.y = y;
        this.sig = sig;
        fit();
    }    	

    public void fit(double [] x, double [] y) throws StatisticsException {
        if (x.length != y.length)  {
            throw new StatisticsException(getClass().getName()+": Array lengths not equal x["+x.length+
            "] and y["+y.length+"].");
        }
        weighted=false;
        this.x = x;
        this.y = y;
        this.sig = y;
        fit();
    }


    public LinearFitErrY(File batch, File out) throws StatisticsException {
        String temp = "\n";
        Vector fx = new Vector();
        Vector fy = new Vector();
        Vector fsig = new Vector();
        try{
            LineNumberReader lr=new LineNumberReader(new FileReader(batch));
            StreamTokenizer st = new StreamTokenizer(new BufferedReader(lr));
            st.eolIsSignificant(false); //treat end of line as white space
            st.nextToken(); weighted=(st.sval.equals("WEIGHTED"));
            st.nextToken(); String xname=st.sval;
            st.nextToken(); String yname=st.sval;
            do {
                st.nextToken();
                if (st.ttype != StreamTokenizer.TT_EOF){
                    fx.addElement(new Double(st.nval));
                    st.nextToken(); fy.addElement(new Double(st.nval));
                    if (weighted) {
                        st.nextToken(); fsig.addElement(new Double(st.nval));
                        temp= " +/- "+fsig.lastElement()+"\n";
                    }
                    System.out.print(xname+" = "+fx.lastElement()+", "+yname+" = "+fy.lastElement()+temp);
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
            int size=fx.size();
            x=new double[size]; y=new double[size];
            if (weighted) sig=new double[size];
            for (int i=0; i< size; i++){
                x[i]=((Double)fx.elementAt(i)).doubleValue();
                y[i]=((Double)fy.elementAt(i)).doubleValue();
                if (weighted) sig[i]=((Double)fsig.elementAt(i)).doubleValue();
            }
            fit();
            FileWriter fw= new FileWriter(out);
            fw.write(yname+" = a + b * "+xname+"\n");
            fw.write("a\tsiga\tb\tsigb\n");
            fw.write(getOffset()+"\t"+getOffsetErr()+"\t"+getSlope()+
            "\t"+getSlopeErr()+"\n");
            fw.write(xname+" = c + d * "+yname+"\n");
            fw.write("c\tsigc\td\tsigd\n");
            LinearFitErrY inverse=invertFit();
            fw.write(inverse.getOffset()+"\t"+inverse.getOffsetErr()+"\t"+
            inverse.getSlope()+"\t"+inverse.getSlopeErr()+"\n");
            fw.flush();fw.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    public double getSlope(){
        return b;
    }
    public double getSlopeErr(){
        return sigb;
    }
    public double getOffset(){
        return a;
    }
    public double getOffsetErr(){
        return siga;
    }
    public double getChiSq() {
        return chi2;
    }
    public double getReducedChiSq() {
        return chi2/dof;
    }
    public int getDegreesOfFreedom() {
        return dof;
    }
    public double get_p_value() {
        return q;
    }

    public double calculateY(double x) {
        return a+b*x;
    }

    /**
     * Inverts x and y data sets.  X error bars are determined using sigy/y ratios.  Useful for
     * inverting linear calibrations of positive sets (e.g., silicon detector channel vs. energy).
     */
    public LinearFitErrY invertFit() throws StatisticsException {
        if (!weighted){
            return new LinearFitErrY(y,x);
        } else {
            double [] sigx = new double[x.length];
            for (int i=0; i<x.length; i++) {
                sigx[i] = sig[i]/y[i]*x[i];
            }
            return new LinearFitErrY(y,x,sigx);
        }
    }

    private void fit() throws StatisticsException {
        double wt,t,sxoss,sx=0.0,sy=0.0,st2=0.0,ss,sigdat;


        b=0.0;
        int ndata=x.length;
        if (ndata < 3) {
            throw new StatisticsException("Less than 3 data points...no degrees of freedom for linear fit.");
        }
        dof=ndata-2;
        if (weighted) {
            ss=0.0;
            for (int i=0;i<ndata;i++) {
                wt=Math.pow(sig[i],-2.0);
                ss += wt;
                sx += x[i]*wt;
                sy += y[i]*wt;
            }
        } else {
            for (int i=0;i<ndata;i++) {
                sx += x[i];
                sy += y[i];
            }
            ss=ndata;
        }
        sxoss=sx/ss;
        if (weighted) {
            for (int i=0;i<ndata;i++) {
                t=(x[i]-sxoss)/sig[i];
                st2 += t*t;
                b += t*y[i]/sig[i];
            }
        } else {
            for (int i=0;i<ndata;i++) {
                t=x[i]-sxoss;
                st2 += t*t;
                b += t*y[i];
            }
        }
        b /= st2;
        a=(sy-sx*(b))/ss;
        siga=Math.sqrt((1.0+sx*sx/(ss*st2))/ss);
        sigb=Math.sqrt(1.0/st2);
        chi2=0.0;
        if (!weighted) {
            for (int i=0;i<ndata;i++) chi2 += Math.pow(y[i]-(a)-(b)*x[i],2.0);
            q=1.0;
            sigdat=Math.sqrt((chi2)/dof);
            siga *= sigdat;
            sigb *= sigdat;
        } else {
            for (int i=0;i<ndata;i++) {
                chi2 += Math.pow((y[i]-(a)-(b)*x[i])/sig[i],2.0);
            }
            q=MiscMath.gammq(0.5*(ndata-2),0.5*(chi2));
        }
        residual  = new double[ndata];
        for (int i=0; i<ndata; i++){
            residual[i] = calculateY(x[i])-y[i];
        }
    }

    public String toString() {
        String rval = "Unweighted ";
        if (weighted) {
            rval = "Weighted ";
        }
        rval += "linear regression of "+(dof+2)+" points:\n";
        rval += "  Offset = "+getOffset()+" +/- "+getOffsetErr()+"\n";
        rval += "  Slope = "+getSlope()+" +/- "+getSlopeErr()+"\n";
        rval += "  Reduced Chi-Squared Statistic = "+getReducedChiSq()+"\n";
        rval += "  P-Value = "+q+"\n";
        return rval;
    }

    /**
     * @param args the command line arguments
     */
    public static void main (String args[]) {
        try {
            new LinearFitErrY(new File(args[0]), new File(args[1]));
        } catch (StatisticsException me) {
            System.err.println(me);
        }
    }

}
