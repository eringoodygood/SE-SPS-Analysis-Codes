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

public class MiscMath {
    
    static final int ITMAX=100;
    static final double EPS=3.0e-7;
    static final double FPMIN=1.0e-30;
    static final int NTRY=50;
    static final double FACTOR=1.6;

	public MiscMath(){
	}
	
	static public double ave(double [] data) {
		double sum;
        int j;
		
        for (sum=0.0, j=0;j<data.length;j++){
            sum = sum + data[j];
        }
        return sum / data.length;
    }
        
     
    static public double var(double [] data){
        double s;
        
        double ave = ave(data);
        double var = 0.0;
        double ep = 0.0;
        for (int j=0;j<data.length;j++){
            s = data[j]-ave;
            ep = ep + s;
            var = var + s*s;
        }
        return (var-ep*ep/data.length)/(data.length-1);
	}
    
    /**
     * Returns the incomplete gamma function Q(a,x) == 1 - P(a,x).
     * P(x) == 1/gamma(a)*Intgegrate[exp[-t]t**(a-1),{t,0,x}] ; (a>0)
     */
    static public double gammq(double a, double x) throws StatisticsException {
	    if (x < 0.0 || a <= 0.0) {
            throw new StatisticsException("Invalid arguments in routine gammq");
        }
	    if (x < (a+1.0)) {
		    double gamser=gser(a,x);
		    return 1.0-gamser;
	    } else {
		    return gcf(a,x);
	    }   
    }
    
    /**
     *
     */
    static public double gser(double a, double x) throws StatisticsException {
	    int n;
	    double sum,del,ap;
        double gln;  // log gamma(a)
	    
        gln=gammln(a);
	    if (x <= 0.0) {
		    if (x < 0.0) {
                throw new StatisticsException("x less than 0 in routine gser");
            }
		    //*gamser=0.0;
		    return 0.0;
	    } else {
		    ap=a;
		    del=sum=1.0/a;
		    for (n=1;n<=ITMAX;n++) {
			    ++ap;
			    del *= x/ap;
			    sum += del;
			    if (Math.abs(del) < Math.abs(sum)*EPS) {
				    //*gamser=sum*exp(-x+a*log(x)-(*gln));
				    return sum*Math.exp(-x+a*Math.log(x)-(gln));
			    }
		    }
		    throw new StatisticsException("a too large, ITMAX too small in routine gser");
            //return null;
	    }
    }
    
    /**
     * Returns Incomplete gamma P(a,x) evaluated by series.
     */
    static public double gammln(double xx){
	    
        double x;
        double y,tmp,ser;
        
	    double [] cof={76.18009172947146,-86.50532032941677,
		    24.01409824083091,-1.231739572450155,
		    0.1208650973866179e-2,-0.5395239384953e-5};

	    y=x=xx;
	    tmp=x+5.5;
	    tmp -= (x+0.5)*Math.log(tmp);
	    ser=1.000000000190015;
	    for (int j=0;j<=5;j++) ser += cof[j]/++y;
	    return -tmp+Math.log(2.5066282746310005*ser/x);
    } 
    
    /**
     * Returns Incomplete gamma Q(a,x) evaluated by series.
     */
    static public double gcf(double a, double x) throws StatisticsException{
	
        int i;
	    double an,b,c,d,del,h;
        double gln;  // log gamma(a)

	    gln=gammln(a);
	    b=x+1.0-a;
	    c=1.0/FPMIN;
	    d=1.0/b;
	    h=d;
	    for (i=1;i<=ITMAX;i++) {
		    an = -i*(i-a);
		    b += 2.0;
		    d=an*d+b;
		    if (Math.abs(d) < FPMIN) d=FPMIN;
		    c=b+an/c;
		    if (Math.abs(c) < FPMIN) c=FPMIN;
		    d=1.0/d;
		    del=d*c;
		    h *= del;
		    if (Math.abs(del-1.0) < EPS) break;
	    }
	    if (i > ITMAX) {
            throw new StatisticsException("a too large, ITMAX too small in gcf");
        }
	    //*gammcf=exp(-x+a*log(x)-(*gln))*h;
        return Math.exp(-x+a*Math.log(x)-(gln))*h;
    }
    
    /**
     * From zbrac.c in "Numerical Recipes".
     * Given a Function and initial guessed range x1 to x2, the method 
     * expands the range geometrically until a root is bracketed by the
     * values in the returned array.  If the range becomes unacceptably
     * large, and exception is thrown, and no bracketing values ever
     * get returned.
     */
    public double [] zbrac(Function func, double x1, double x2) throws 
            StatisticsException {
        double out[]=new double[2];
        out[0]=x1;
        out[1]=x2;
        if (x1==x2) throw new StatisticsException("zbrac(): can't have x1==x2");
        double f1=func.valueAt(x1);
        double f2=func.valueAt(x2);
        for(int j=0;j<NTRY;j++){
            if (f1*f2 < 0.0) return out;
            if (Math.abs(f1) < Math.abs(f2)) {
                out[0] += FACTOR*(out[0]-out[1]);
                f1=func.valueAt(out[0]);
            } else {
                out[1] += FACTOR*(out[1]-out[0]);
                f2=func.valueAt(out[1]);
            }
        }
        throw new StatisticsException("xbrac(): Failed in "+NTRY+
                " tries to find interval.");
    }
    
    /**
     * From zbrent.c in "Numerical Recipes".
     * Using Brent's method, finds the root of Function func known to lie 
     * between x1 and x2.  The root is returned, and will be refined until
     * it's accuracy is tol.
     */
    static public double zbrent(Function func, double x1, double x2, 
            double tol) throws StatisticsException {
	
        int iter;
	    double a=x1,b=x2,c=x2,d=0.0,e=0.0,min1,min2;
	    double fa=func.valueAt(a),fb=func.valueAt(b),fc,p,q,r,s,tol1,xm;
        double EPSILON=3.0e-8;

	    if ((fa > 0.0 && fb > 0.0) || (fa < 0.0 && fb < 0.0)){
		    throw new StatisticsException("Root must be bracketed in zbrent");
        }
	    fc=fb;
	    for (iter=1;iter<=ITMAX;iter++) {
		    if ((fb > 0.0 && fc > 0.0) || (fb < 0.0 && fc < 0.0)) {
			    c=a;
			    fc=fa;
			    e=d=b-a;
		    }
		    if (Math.abs(fc) < Math.abs(fb)) {
			    a=b;
			    b=c;
			    c=a;
			    fa=fb;
			    fb=fc;
                fc=fa;
		    }
		    tol1=2.0*EPSILON*Math.abs(b)+0.5*tol;
		    xm=0.5*(c-b);
		    if (Math.abs(xm) <= tol1 || fb == 0.0) return b;
		    if (Math.abs(e) >= tol1 && Math.abs(fa) > Math.abs(fb)) {
			    s=fb/fa;
			    if (a == c) {
				    p=2.0*xm*s;
				    q=1.0-s;
			    } else {
				    q=fa/fc;
				    r=fb/fc;
				    p=s*(2.0*xm*q*(q-r)-(b-a)*(r-1.0));
				    q=(q-1.0)*(r-1.0)*(s-1.0);
			    }
			    if (p > 0.0) q = -q;
			    p=Math.abs(p);
			    min1=3.0*xm*q-Math.abs(tol1*q);
			    min2=Math.abs(e*q);
			    if (2.0*p < (min1 < min2 ? min1 : min2)) {
                    e=d;
				    d=p/q;
			    } else {
				    d=xm;
				    e=d;
			    }
		    } else {
			    d=xm;
			    e=d;
		    }
		    a=b;
		    fa=fb;
		    if (Math.abs(d) > tol1)
			    b += d;
		    else
			    b += SIGN(tol1,xm);
		    fb=func.valueAt(b);
	    }
	    throw new StatisticsException("Maximum number of iterations exceeded in zbrent");
	    //return 0.0;
    }
    
    /**
     * Incomplete beta function.  Used to calculate Student's T.
     */
    static public double betai(double a, double b, double x) throws StatisticsException {
	double bt;
	if (x < 0.0 || x > 1.0) throw new StatisticsException("Bad x="+x+
        " in routine betai, should in interval [0,1].");
        if (x == 0.0 || x == 1.0) {
            bt=0.0;
        } else {
		bt=Math.exp(gammln(a+b)-gammln(a)-gammln(b)+a*Math.log(x)+
                b*Math.log(1.0-x));
        }
	if (x < (a+1.0)/(a+b+2.0)) {
            return bt*betacf(a,b,x)/a;
        } else {
            return 1.0-bt*betacf(b,a,1.0-x)/b;
        }
    }
    
    /**
     * Continued fractions used by betai.
     */
    static private double betacf(double a, double b, double x) throws StatisticsException{
	int m,m2;
	double aa,c,d,del,h,qab,qam,qap;
	qab=a+b;
	qap=a+1.0;
	qam=a-1.0;
	c=1.0;
	d=1.0-qab*x/qap;
	if (Math.abs(d) < FPMIN) d=FPMIN;
	d=1.0/d;
	h=d;
	for (m=1;m<=ITMAX;m++) {
		m2=2*m;
		aa=m*(b-m)*x/((qam+m2)*(a+m2));
		d=1.0+aa*d;
		if (Math.abs(d) < FPMIN) d=FPMIN;
		c=1.0+aa/c;
		if (Math.abs(c) < FPMIN) c=FPMIN;
		d=1.0/d;
		h *= d*c;
		aa = -(a+m)*(qab+m)*x/((a+m2)*(qap+m2));
		d=1.0+aa*d;
		if (Math.abs(d) < FPMIN) d=FPMIN;
		c=1.0+aa/c;
		if (Math.abs(c) < FPMIN) c=FPMIN;
		d=1.0/d;
		del=d*c;
		h *= del;
		if (Math.abs(del-1.0) < EPS) break;
	}
	if (m > ITMAX) throw new StatisticsException("a or b too big, or MAXIT too small in betacf");
	return h;
    }    
    
    /**
     * Returns the probability that the absolute value of a t-statistic would be smaller than 
     * <code>tObserved</code>, for a t-distribution with <code>dof</code>
     * degrees of freedom and a mean of zero.
     */
    static public double calculateStudentsT(double tObserved, double dof) throws
    StatisticsException {
        return 1.0-betai(0.5*dof,0.5,dof/(dof+tObserved*tObserved));
    }
    
    /**
     * Calculates t-statistic such that 
     * calculateStudentsT(findOneSigmaT(dof),dof) returns 0.683, the one-sigma
     * probability for the normal distribution.
     */
    static public double findOneSigmaT(int dof) throws StatisticsException {
        class tFunc implements Function {
            int dof;
            public tFunc(int dof) {this.dof=dof;}
            public double valueAt(double x){
                try {
                    return Math.abs(0.683 - calculateStudentsT(x,dof));
                } catch (StatisticsException se) {
                    System.err.println(se);
                }
                return 0;
            }
        }
        BrentMethod brent = new BrentMethod(new tFunc(dof));
        return brent.xmin(0.9,1.9,1.5,0.001);
    }
    
    private static double SIGN(double a,double b){
        return (b >= 0.0 ? Math.abs(a) : -Math.abs(a));
    }
    
    static public void main(String [] args){
        try{
         System.out.println(MiscMath.findOneSigmaT(1));
        } catch (StatisticsException se) {
            System.err.println(se);
        }
    }
}
