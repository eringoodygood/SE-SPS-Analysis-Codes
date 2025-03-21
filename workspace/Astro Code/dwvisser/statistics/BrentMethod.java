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

/**
 * This class is based on the routine <code>brent</code> in Numerical Recipes
 * in C, 2nd ed.  It is for finding a function minimum given values known
 * to bracket it.
 */
public class BrentMethod {
    
    /**
     * Maximum number of iterations.
     */
    static final int ITMAX=100;
    
    /**
     * The so-called golden ratio, by which successive intervals are magnified.
     */
    static final double CGOLD=0.3819660;
    
    /**
     * A small number that protects against trying to achieve fractional
     * accuracy for a minimum that happens to be exactly zero.
     */
    static final double ZEPS=1.0e-10;
    
    /**
     * The function to minimize.
     */
    private Function f;
    
    /**
     * Creates an instance of BrentMethod with the function f.
     *
     * @param f the function to minimize
     */
    public BrentMethod(Function f){
        this.f=f;
    }
    
    /**
     * Find minimum of the function in the interval given to a certain
     * tolerance.
     *
     * @param ax one endpoint of initial bracket
     * @param bx initial guess between bracket limits, f(bx)<f(ax) and f(bx)<f(cx)
     * @param cx other endpoint of initial bracket
     * @param tol fractional precision to which minimum is to be found
     * @return value at which function is minimized
     */
    public double xmin(double ax, double bx, double cx,double tol)
    throws StatisticsException{
        
        int iter;
        double a,b,d,etemp,fu,fv,fw,fx,p,q,r,tol1,tol2,u,v,w,x,xm;
        //double xmin;
        double e=0.0;
        
        d=0.0;
        a=(ax < cx ? ax : cx);
        b=(ax > cx ? ax : cx);
        x=w=v=bx;
        fw=fv=fx=f.valueAt(x);
        for (iter=1;iter<=ITMAX;iter++) {
            xm=0.5*(a+b);
            tol2=2.0*(tol1=tol*Math.abs(x)+ZEPS);
            if (Math.abs(x-xm) <= (tol2-0.5*(b-a))) return x;
            if (Math.abs(e) > tol1) {
                r=(x-w)*(fx-fv);
                q=(x-v)*(fx-fw);
                p=(x-v)*q-(x-w)*r;
                q=2.0*(q-r);
                if (q > 0.0) p = -p;
                q=Math.abs(q);
                etemp=e;
                e=d;
                if (Math.abs(p) >= Math.abs(0.5*q*etemp) || p <= q*(a-x) || p >= q*(b-x)){
                    d=CGOLD*(e=(x >= xm ? a-x : b-x));
                } else {
                    d=p/q;
                    u=x+d;
                    if (u-a < tol2 || b-u < tol2) d=sign(tol1,xm-x);
                }
            } else {
                d=CGOLD*(e=(x >= xm ? a-x : b-x));
            }
            u=(Math.abs(d) >= tol1 ? x+d : x+sign(tol1,d));
            
            fu=f.valueAt(u);
            if (fu <= fx) {
                if (u >= x) a=x; else b=x;
                v=w;w=x;x=u;
                //SHFT(v,w,x,u)
                fv=fw;fw=fx;fx=fu;
                //SHFT(fv,fw,fx,fu)
            } else {
                if (u < x) a=u; else b=u;
                if (fu <= fw || w == x) {
                    v=w;
                    w=u;
                    fv=fw;
                    fw=fu;
                } else if (fu <= fv || v == x || v == w) {
                    v=u;
                    fv=fu;
                }
            }
        }
        throw new StatisticsException("Too many iterations in BrentMethod.");
    }
    
    private double sign(double a, double b) {
        return (b>0.0 ? Math.abs(a) : -Math.abs(a));
    }
    
}

