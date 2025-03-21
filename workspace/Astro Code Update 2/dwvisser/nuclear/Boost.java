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
 * Boost.java
 *
 * Created on April 4, 2001, 9:51 AM
 */

package dwvisser.nuclear;
import dwvisser.math.MathException;
import dwvisser.math.Matrix;
import dwvisser.monte.Direction;

/** This class handles general boosts from one lorentz frame to another.
 * Instances are created by specifying the relative velocity of the frame to be
 * boosted to.
 * @author dwvisser
 */
public final class Boost {

    private double gamma;

    /**
     * zeroth element has beta, 1st-3rd have x,y,z components
     */
    private double [] beta = new double[4];

    private Matrix boost;

    /** Creates new Boost
     *
     * @param _beta velocity of frame (in units of c)
     * @param theta angle from z axis (in radians)
     * @param phi azimuthal angle (in radians)
     */
    public Boost(double _beta, double theta, double phi) {
        beta[0]=_beta;
        gamma=calculateGamma(beta[0]);
        beta[1]=beta[0]*Math.sin(theta)*Math.cos(phi);
        beta[2]=beta[0]*Math.sin(theta)*Math.sin(phi);
        beta[3]=beta[0]*Math.cos(theta);
        makeBoostMatrix();
    }
    
    /** Creates a boost given the beta "4-vector" that a boost creates.
     * This "4-vector" has beta as its 0th element, and the x, y, and z
     * components as the 1, 2, and 3 elements, respectively.
     * @param _beta the array described above
     */
    public Boost(double [] _beta) {
        System.arraycopy(_beta,0,beta,0,4);
        gamma=calculateGamma(beta[0]);
        makeBoostMatrix();
    }
    
    public Boost(double _beta, Direction d){
    	this(_beta,d.getTheta(),d.getPhi());
    }
    
    double [] getBeta(){
        return beta;
    }
    
    double getTheta(){
        return Math.acos(beta[3]/beta[0]);
    }
    
    double getPhi(){
        return Math.acos(beta[1]/(beta[0]*Math.sin(getTheta())));
    }
    
    /** Creates the boost for a velocity equal in magnitude in the opposite direction
     * from the original boost.  This is useful when you want to go back and forth
     * between 2 frames.
     * @param boost the boost to be inverted
     * @return boost for the inverse transformation
     */
    static public Boost inverseBoost(Boost boost){
        double [] temp = new double[4];
        double [] orig_beta = boost.getBeta();
        temp[0] = orig_beta[0];
        temp[1] = -orig_beta[1];
        temp[2] = -orig_beta[2];
        temp[3] = -orig_beta[3];
        return new Boost(temp);
    }
    
    public Boost getInverse(){
    	return Boost.inverseBoost(this);
    }

    /** 
     * Given a velocity (_beta) in units of c, calculate gamma.
     * <PRE>
     * gamma=[sqrt(1-_beta^2)]^(-1)</PRE>
     * @param _beta velocity over c, the speed of light
     * @return the standard gamma parameter from special relativity
     */
    public double calculateGamma(double _beta){
        return 1/Math.sqrt(1-_beta*_beta);
    }

    /** 
     * Given a proper 4-vector, boost it to the frame indicated at 
     * the creation of this object.
     *
     * @param fourVector proper special relativistic 4-vector
     * @throws MathException if there's a computation problem
     * @return 4-vector in the new frame
     * @see Boost
     */
    public double [] transformVector(double [] fourVector) throws MathException {
        Matrix init=makeFourVector(fourVector);
        Matrix result=new Matrix(boost,init,'*');//matrix product
        return makeArray(result);
    }

    private Matrix makeFourVector(double [] in) throws MathException {
        if (in.length != 4) throw new MathException("Can't make 4-vector.");
        Matrix rval=new Matrix(4,1);
        for (int i=0; i<4; i++) rval.element[i][0]=in[i];
        return rval;
    }
    
    static public double [] make4Momentum(double KE, double mass, double theta, double phi){
        double [] rval=new double[4];
        rval[0]=KE+mass;
        double p=Math.sqrt(KE*(2*mass+KE));
        rval[1]=p*Math.sin(theta)*Math.cos(phi);
        rval[2]=p*Math.sin(theta)*Math.sin(phi);
        rval[3]=p*Math.cos(theta);
        return rval;
    }
        


    private double [] makeArray(Matrix in) throws MathException {
        if (in.rows !=4 || in.columns != 1) throw new MathException(
        "Not a 4-vector");
        double [] rval = new double[4];
        for (int i=0; i<4; i++) rval[i]=in.element[i][0];
        return rval;
    }


    /**
     * Makes boost matrix from p. 541 of Jackson v.2.
     */
    private void makeBoostMatrix(){
        boost=new Matrix(4,4);
        boost.element[0][0]=gamma;
        for (int i=1; i<=3; i++){
            boost.element[0][i]=-gamma*beta[i];
            boost.element[i][0]=-gamma*beta[i];
            for (int j=1; j<=3; j++){
                boost.element[i][j]=(gamma-1.0)*beta[i]*beta[j]/(beta[0]*beta[0]);
                if (i==j) boost.element[i][j] += 1.0;
            }
        }
    }
    
    public String toString(){
        String rval = "Frame to boost to: "+beta[0]*100+"% c, Beta: ";
        rval += "x = "+beta[1]+", y = "+beta[2]+", z = "+beta[3]+"\n";
        return rval;
    }
        

    /** Test code.
     * @param args ignored
     */
    public static void main(String [] args){
        double T=5.0; double m0=3727.3802;
        double theta = Math.toRadians(140.0);
        System.out.println(T+" MeV alpha in CM");
    double [] phiD = {-90.0, -18.0, 54.0, 126.0, 198.0};
        double [] phi = new double[phiD.length];
        for (int i=0; i< phiD.length; i++) phi[i]=Math.toRadians(phiD[i]);
        double [] p= new double[4];
        double [] pb;//after boost
        Boost b=Boost.inverseBoost(new Boost(0.001,Math.toRadians(5.0),0.0));
        System.out.println(b);
        p[0] = T+m0;
        double p3=Math.sqrt(T*T+2*m0*T);
        for (int i=0; i< phi.length; i++){
            p[1] = p3*Math.sin(theta)*Math.cos(phi[i]);
            p[2] = p3*Math.sin(theta)*Math.sin(phi[i]);
            p[3] = p3*Math.cos(theta);
            try {
                pb = b.transformVector(p);
                System.out.println("Detector at "+phiD[i]+" degrees:\n"+
                "\tT[alpha][lab] = "+(pb[0]-m0)+" MeV.");
            } catch (MathException e) {
                System.err.println(e);
            }
        }
    }
}