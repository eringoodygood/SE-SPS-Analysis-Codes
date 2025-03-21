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
package net.sourceforge.nukesim.statistics;

/**
 * Code for fitting a line to data with error bars in
 * both x and y.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class LinearFitErrXY implements Function {

    static final double POTN=1.571000;
    static final double BIG=1.0e30;
    static final double ACC=1.0e-3;

    int nn;
    double [] xx,yy,sx,sy,ww;
    double aa,offs;
    /**
     * y=a+b*x
     * chi2=full chi2 not reduced
     * q=probability of good fit based on chi2 and dof
     */
    public double a, b, siga, sigb, chi2, q;

    /**
     * Degrees of freedom = num data points - 2
     */
    public double dof;

    public LinearFitErrXY(){
    }

    public void doFit(double x[], double y[], double sigx[], double sigy[])
    throws StatisticsException{

        int j,ndat;
        double swap,amx,amn,varx,vary;
        double [] ang=new double[7];
        double [] ch=new double[7];
        double scale,bmn,bmx,d1,d2,r2;
        LinearFitErrY lfy=new LinearFitErrY();

        ndat = x.length;
        xx=new double[ndat];
        yy=new double[ndat];
        sx=new double[ndat];
        sy=new double[ndat];
        ww=new double[ndat];

        varx=MiscMath.var(x);
        vary=MiscMath.var(y);
        scale=Math.sqrt(varx/vary);
        nn=ndat;
        dof=nn-2;
        lfy.fit(x,y,sigy);
        for (j=0;j<ndat;j++) {
            xx[j]=x[j];
            yy[j]=y[j]*scale;
            sx[j]=sigx[j];
            sy[j]=sigy[j]*scale;
            ww[j]=Math.sqrt(Math.pow(sx[j],2.0)+Math.pow(sy[j],2.0));
        }
        lfy.fit(xx,yy,ww);
        b=lfy.getSlope();
        offs=ang[1]=0.0;
        ang[2]=Math.atan(b);
        ang[4]=0.0;
        ang[5]=ang[2];
        ang[6]=POTN;
        for (j=4;j<=6;j++) ch[j]=chixy(ang[j]);
        BracketMinimum bracketer = new BracketMinimum(this);
        bracketer.bracket(ang[1],ang[2]);
        ang[1]=bracketer.ax;
        ang[2]=bracketer.bx;
        ang[3]=bracketer.cx;
        ch[1]=bracketer.fa;
        ch[2]=bracketer.fb;
        ch[3]=bracketer.fc;
        BrentMethod brent=new BrentMethod(this);
        b=brent.xmin(ang[1],ang[2],ang[3],ACC);
        chi2=valueAt(b);
        a=aa;
        q=MiscMath.gammq(0.5*(nn-2),chi2*0.5);
        for (r2=0.0,j=0;j<nn;j++) r2 += ww[j];
        r2=1.0/r2;
        bmx=BIG;
        bmn=BIG;
        offs=(chi2)+1.0;
        for (j=1;j<=6;j++) {
            if (ch[j] > offs) {
                d1=Math.abs(ang[j]-(b));
                while (d1 >= Math.PI) d1 -= Math.PI;
                d2=Math.PI-d1;
                if (ang[j] < b) {
                    swap=d1;
                    d1=d2;
                    d2=swap;
                }
                if (d1 < bmx) bmx=d1;
                if (d2 < bmn) bmn=d2;
            }

        }
        if (bmx < BIG) {
            bmx=MiscMath.zbrent(this,b,b+bmx,ACC)-(b);
            amx=aa-(a);
            bmn=MiscMath.zbrent(this,b,b-bmn,ACC)-(b);
            amn=aa-(a);
            sigb=Math.sqrt(0.5*(bmx*bmx+bmn*bmn))/(scale*SQR(Math.cos(b)));
            siga=Math.sqrt(0.5*(amx*amx+amn*amn)+r2)/scale;
        } else sigb=siga=BIG;
        a /= scale;
        b=Math.tan(b)/scale;
    }

    public double valueAt(double x){
        return chixy(x);
    }

    private double chixy(double bang){
        int j;
        double ans,avex=0.0,avey=0.0,sumw=0.0,b;

        b=Math.tan(bang);
        for (j=0;j<nn;j++) {
            ww[j] = SQR(b*sx[j])+SQR(sy[j]);
            sumw += (ww[j] = (ww[j] < 1.0/BIG ? BIG : 1.0/ww[j]));
            avex += ww[j]*xx[j];
            avey += ww[j]*yy[j];
        }
        avex /= sumw;
        avey /= sumw;
        aa=avey-b*avex;
        for (ans = -offs,j=0;j<nn;j++) ans += ww[j]*SQR(yy[j]-aa-b*xx[j]);
        return ans;
    }

    private double SQR(double x){
        return Math.pow(x,2.0);
    }

    /**
     * Calculates a value to translate the x's by to diagonalize
     * the coefficient covariance matrix.
     */
    public double getTranslation(double [] x, double [] dx){
        double sigmasum=0.0;
        double xsum=0.0;
        //double sqxsum=0.0;
        for (int i=0;i<x.length;i++){
            sigmasum += 1/(dx[i]*dx[i]);
            xsum += x[i]/(dx[i]*dx[i]);
            //sqxsum += x[i]*x[i]/(dx[i]*dx[i]);
        }
        return xsum/sigmasum;
    }

    /**
     * Return an array equal to x with its elements reduced the mean of x.
     */
    public double [] translate(double [] x,double [] dx){
        double [] out;
        double delx;

        delx = getTranslation(x,dx);
        out=new double[x.length];
        for (int i=0;i<x.length;i++){
            out[i] = x[i]-delx;
        }
        return out;
    }
}
