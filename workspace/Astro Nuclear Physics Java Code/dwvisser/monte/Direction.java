/*
 * Direction.java
 *
 * Created on March 7, 2001, 11:47 AM
 */
package dwvisser.monte;
import dwvisser.math.Matrix;

/**
 * Class which provides an abstraction for a direction
 * in 3-dimensional space.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public class Direction extends Object implements Cloneable {

    private double theta,phi;//in radians
    private double x,y,z;//direction components
    private static RandomWill random;

    /**
     * Creates new direction, given theta (angle from z-axis) and phi
     * (azimuthal angle from x axis) in radians.
     */
    public Direction(double theta, double phi) {
        z=Math.cos(theta);
        x=Math.sin(theta)*Math.cos(phi);
        y=Math.sin(theta)*Math.sin(phi);
        setAngles();
        if (random==null) initRandom();
    }

	/**
	 *  Create a new direction by specifying its x, y, and z components.
	 *  These will be renormailized.
	 */
    public Direction(double _x, double _y, double _z){
        double norm = Math.sqrt(_x*_x+_y*_y+_z*_z);
        this.x = _x/norm;
        this.y = _y/norm;
        this.z = _z/norm;
        setAngles();
        if (random==null) initRandom();
    }
    
    /**
     * called by constructors, assuming x,y, and z have been set with a norm of 1.
     * i.e. x^2+y^2+z^2=1
     */
    private void setAngles(){
        phi = normPhi(Math.atan2(y,x));
        theta=Math.acos(z);
    }
    
    protected Object clone(){
        return new Direction(theta,phi);
    }

    //initializes random number generator
    private void initRandom() {
        try {
            random = new RandomWill();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    /**
     * return a new Direction object resulting from this object 
     * being rotated by angRad about the y-axis
     */
    public Direction rotateY(double angRad){
        if (angRad == 0.0) return (Direction)this.clone();
        double c=Math.cos(angRad); double s=Math.sin(angRad);
        Matrix rotate = new Matrix(3,3);
        rotate.element[0][0]=c; rotate.element[0][1]=0; rotate.element[0][2]=s;
        rotate.element[1][0]=0; rotate.element[1][1]=1; rotate.element[1][2]=0;
        rotate.element[2][0]=-s; rotate.element[2][1]=0; rotate.element[2][2]=c;
        Matrix mNewDir = new Matrix(rotate, getVector(), '*');
        return new Direction(mNewDir.element[0][0], mNewDir.element[1][0],
        mNewDir.element[2][0]);
    }
    
    
    /**
     * Return a new Direction object resulting from this object's 
     * reference frame being rotated by angRad about the y-axis.
     */
    public Direction rotateFrameY(double angRad){
        if (angRad == 0.0) return (Direction)this.clone();
        double c=Math.cos(angRad); double s=Math.sin(angRad);
        return new Direction(c*x-s*z,y,s*x+c*z);        
    }
    
    /**
     * Return a new Direction object resulting from this object's 
     * reference frame being rotated by angRad about the z-axis.
     */
    public Direction rotateFrameZ(double angRad){
        if (angRad == 0.0) return (Direction)this.clone();
        double c=Math.cos(angRad); double s=Math.sin(angRad);
        return new Direction(c*x+s*y,-s*x+c*y,z);        
    }

    static public Direction getDirection(Matrix m){
        if (m.rows==3 && m.columns==1) {
            return new Direction(m.element[0][0],m.element[1][0],m.element[2][0]);
        } else {
            return null;
        }
    }

    public double [] get3vector(double amplitude) {
        double [] rval = new double[3];
        rval[0] = amplitude*x;
        rval[1] = amplitude*y;
        rval[2] = amplitude*z;
        return rval;
    }

    /**
     * @return phi in radians, guaranteed between -pi and pi
     */
    public double getPhi(){
        return phi;
    }

    public double getTheta(){
        return theta;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public double getZ(){
        return z;
    }

    public Matrix getVector(){
        Matrix rval=new Matrix(3,1);
        rval.element[0][0]=x;
        rval.element[1][0]=y;
        rval.element[2][0]=z;
        return rval;
    }


    static public Direction getBackwardRandomDirection(){
        Direction rval=null;
        try {
            rval = new Direction(Math.PI-Math.acos(1.0-random.next()),
            2.0*Math.PI*random.next());
        } catch (Exception e) {
            System.err.println(e);
        }
        return rval;
    }

    public String toString(){
        String rval="Direction: theta = "+getThetaDegrees()+" deg, ";
        rval += " phi = "+getPhiDegrees()+" deg, ";
        rval += "\nx\ty\tz\n"+x+"\t"+y+"\t"+z+"\n";
        return rval;
    }

    static public double normPhi(double _phi){
        double twoPi = 2*Math.PI;
        if (_phi >= 0 && _phi < twoPi) return _phi;
        if (_phi >= twoPi) return Math.IEEEremainder(_phi,twoPi);
        //phi < 0, make recursive call which will eventually go positive
        return normPhi(_phi+twoPi);
    }

    public double getPhiDegrees(){
        return Math.toDegrees(phi);
    }

    public double getThetaDegrees() {
        return Math.toDegrees(theta);
    }
    
    /**
     * Taken from plgndr, section 6.8 in Numerical Recipes in C.
     * Decays with angular momentum 'l' are distributed as the square
     * of evaluateLegengre(l,0,cos theta).  (Modulated of course by
     * the sin(theta) factor of the phase space available.)
     *
     * @param l orbital angular momentum quantum number, 0 or positive
     * @param m substate, can be from 0 to l
     * @param x where to evaluate, from -1 to 1
     */
    static public double evaluateLegendre(int _l, int _m, double _x) throws 
    IllegalArgumentException{
        double fact, pll, pmm, pmmp1, somx2;
        
        if (_m < 0 || _m > _l || Math.abs(_x) > 1.0) throw new 
        IllegalArgumentException("Invalid argument for Legendre evaluation: l="
            +_l+", m="+_m+", x="+_x);
        pmm=1.0;//compute Pmm
        if (_m>0) {
            somx2=Math.sqrt((1-_x)*(1+_x));
            fact=1.0;
            for (int i=0; i<_m; i++){
                pmm *= -fact*somx2;
                fact += 2;
            }
        }
        if (_l==_m) {
            return pmm;
        } else {    //compute Pm,m+1
            pmmp1 = _x*(2*_m+1)*pmm;
            if (_l == (_m+1)) {
                return pmmp1;
            } else { //Compute  Pl,m where l>m+1
                pll=0.0;
                for (int ll=_m+2; ll<=_l; ll++){
                    pll=(_x*(2*ll-1)*pmmp1-(ll+_m-1)*pmm)/(ll-_m);
                    pmm=pmmp1;
                    pmmp1=pll;
                }
                return pll;
            }
        }
    }
    
    /**
     * Generate a random direction using a Spherical Harmonic 
     * distribution (attenuated by a sin theta solid angle factor).
     */
    static public Direction getRandomDirection(int l, int m){
        double _x,test,leg;
        do {
            _x = 1-2*random.next();//-1..1, x=cos(theta)
            test = random.next();//0..1
            leg = evaluateLegendre(l,m,_x);
        } while (test > (leg*leg));
        return new Direction(Math.acos(_x),2*Math.PI*random.next());
    }
    
    /**
     * Generate a random direction for m=0 using a Legendre polynomial
     * distribution (attenuated by a sin theta solid angle factor).
     */
    static public Direction getRandomDirection(int l){
    	return getRandomDirection(l,0);
    }
    
    static public Direction getRandomDirection(){
        Direction rval=null;
        try {
            rval = new Direction(Math.acos(1.0-2.0*random.next()),
            2.0*Math.PI*random.next());
        } catch (Exception e) {
            System.err.println(e);
        }
        return rval;
    }
    
    /**
     * get Random direction between given theta limits
     */
    static public Direction getRandomDirection(double minThetaRad,
    double maxThetaRad){
        Direction rval=null;
        double maxRandom = 0.5*(1-Math.cos(maxThetaRad));
        double minRandom = 0.5*(1-Math.cos(minThetaRad));
        double delimitedRandom = minRandom+(maxRandom-minRandom)*random.next();
        try {
            rval = new Direction(Math.acos(1.0-2.0*delimitedRandom),2.0*Math.PI*random.next());
        } catch (Exception e) {
            System.err.println(e);
        }
        return rval;
    }
    
    static final Direction Z_AXIS = new Direction(0,0,1);        
    /**
     * Generate a random direction using a Legendre polynomial 
     * distribution (attenuated by a sin theta solid angle factor),
     * relative to a z-axis defined by the given direction.
     */
    static public Direction getRandomDirection(int l, Direction d){
    	Direction rval = getRandomDirection(l);
    	rval = rval.rotateFrameZ(d.getTheta()).rotateFrameY(d.getPhi());
    	return rval;
    }
     
    public static void main(String [] args) {
        //double thetaD = 30;
        //int [] bins = new int[18];
        Direction d=new Direction(0,0);
        for (int i=0; i< 1; i++) {
            Direction r=Direction.getRandomDirection(0.0,Math.toRadians(5.0));
            System.out.println("init: "+r);
            System.out.println("final: "+r.rotateY(Math.toRadians(30.0)));
            //bins[(int)Math.floor(Math.toDegrees(r.getTheta())/10)]++;
            //System.out.println(d.getRandomDirection(1));
        }
        /*for (int i=0;i<18; i++){
            System.out.println((i*10)+"..."+((i+1)*10)+"\t"+bins[i]);
        }*/
    }
}