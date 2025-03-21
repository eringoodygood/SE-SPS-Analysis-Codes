/*
 */

package dwvisser.monte;
import dwvisser.math.Matrix;
//import java.io.FileWriter;
//import java.io.IOException;

/**
 * This class represents the geometry of two Micron silicon strip detectors, to use for
 * deciding if and where a virtual vector will hit.
 *Modified sept 04: do everything in the lab frame: don't rotate to detector frame
 */
public class MicronDetector90 extends Object implements WeightingFunction{

    private double x0, y0, z0, theta0, x1, y1, z1, theta1;
    private Matrix temp0; //lab origin in detector frame
    private Matrix temp1; //lab origin in detector frame
    boolean hit; //whether a strip was hit
    private double cosThetaInc;//cosine of incidence angle if detector hit
    private int xstrip; //if detector hit, contains strip that was hit
    private int zstrip; //if detector hit, contains strip that was hit
    private int detector; //if detector hit, contains detector that was hit
    boolean interstrip; //if interstrip event then true
    double distance0,distance1;//distance to detector plane along particle trajectory, in mm
    private static final double Lz=40;//length of detector active area, in mm
    private static final double Lx=50;//length of detector active area, in mm
    private static final double w=3;//width of strip, in mm
    private int nxStrip;//number of strips in the x direction
    private int nzStrip;//number of strips in the z direction
    private int NUM_DET,STRIPS_PER_DET;
    private double xHit,zHit,zMin;

    public MicronDetector90(double x0, double y0, double z0, double theta0,
    double x1,double y1, double z1, double theta1, int NUM_DET, int STRIPS_PER_DET) {
        this.x0=x0;//in mm
        this.y0=y0;//in mm
        this.z0=z0;//in mm
        this.theta0=theta0;
        double theta0P = (theta0-Math.PI/2.0);
        this.x1=x1;//in mm
        this.y1=y1;//in mm
        this.z1=z1;//in mm
        this.theta1=theta1;
        double theta1P = (theta1-Math.PI/2.0);
        this.NUM_DET =NUM_DET;
        this.STRIPS_PER_DET=STRIPS_PER_DET;
        nxStrip=STRIPS_PER_DET;
        nzStrip=STRIPS_PER_DET;//creating 16 bins along the length of each strip
        
        System.out.println("MicronDetector90: x0,y0,z0,theta0: "+x0+","+y0+","+z0+","
            +Math.round(Math.toDegrees(theta0)));
        System.out.println("MicronDetector90: x1,y1,z1,theta1: "+x1+","+y1+","+z1+","
            +Math.round(Math.toDegrees(theta1)));
        System.out.println("MicronDetector90: theta0P,theta1P: "
            +Math.round(Math.toDegrees(theta0P))+","+Math.round(Math.toDegrees(theta1P)));
        double test0 = theta0P/(Math.PI/2.0);
        double sinTheta0P= Math.sin(theta0P);
        double cosTheta0P= Math.cos(theta0P);
        if (test0==1){cosTheta0P=0;}
        if ((test0==-2)||(test0==2)){sinTheta0P=0;}
        double test1 = theta1P/(Math.PI/2.0);
        double sinTheta1P= Math.sin(theta1P);
        double cosTheta1P= Math.cos(theta1P);
        if (test1==1){cosTheta1P=0;}
        if ((test1==-2)||(test1==2)){sinTheta1P=0;}
        temp0=new Matrix(x0+";"+ y0+";"+ z0+";");//origin of det 0 before rotating
        temp1=new Matrix(x1+";"+ y1+";"+ z1+";");//origin of det 1 before rotating
       }

    public boolean isHit(Direction dir){//initialize
        hit=false;
        xstrip=-1;
        zstrip=-1;
        detector=-1;
           double xmin0 = temp0.element[0][0]-Lx/2; //no longer assumes the detector is centred on the beam line
           double xmax0 = temp0.element[0][0]+Lx/2;
           double yd0 = temp0.element[1][0];//in the detector frame, every point on the detector is at the same position, yd
           double zmax0 = temp0.element[2][0]+Lz/2;
           double zmin0 = temp0.element[2][0]-Lz/2;
           Direction direction0A =new Direction(xmax0, yd0, 0); //lower downstream corner of detector
           Direction direction0B =new Direction(xmin0, yd0, 0); //upper upstream corner
           Direction direction0C =new Direction(0, yd0, zmin0); //lower downstream corner of detector
           Direction direction0D =new Direction(0, yd0, zmax0); //upper upstream corner
           double thetaA0 = direction0A.getTheta(); //angular range of detector in detector frame
           double thetaB0 = direction0B.getTheta();
           double thetaC0 = direction0C.getTheta(); //angular range of detector in detector frame
           double thetaD0 = direction0D.getTheta();
           double phiA0 = direction0A.getPhi();
           double phiB0 = direction0B.getPhi();
           double phiC0 = direction0C.getPhi();
           double phiD0 = direction0D.getPhi();
           double thetaMin0 = direction0D.getTheta();
           double thetaMax0 = direction0C.getTheta();
           double phiMin0 = direction0A.getPhi();
           double phiMax0 = direction0B.getPhi();
           if (zmax0 <= zmin0 ) {
               zmin0=zmax0;
           }
           double xmin1 = temp1.element[0][0]-Lx/2; 
           double xmax1 = temp1.element[0][0]+Lx/2;
           double yd1 = temp1.element[1][0];
           double zmax1 = temp1.element[2][0]+Lz/2;
           double zmin1 = temp1.element[2][0]-Lz/2;
           Direction direction1A =new Direction(xmax1, yd1, 0); 
           Direction direction1B =new Direction(xmin1, yd1, 0); 
           Direction direction1C =new Direction(0, yd1, zmin1); 
           Direction direction1D =new Direction(0, yd1, zmax1); 
           double thetaA1 = direction1A.getTheta();
           double thetaB1 = direction1B.getTheta();
           double thetaC1 = direction1C.getTheta(); 
           double thetaD1 = direction1D.getTheta();
           double phiA1 = direction1A.getPhi();
           double phiB1 = direction1B.getPhi();
           double phiC1 = direction1C.getPhi();
           double phiD1 = direction1D.getPhi();
           double thetaMin1 = direction1D.getTheta();
           double thetaMax1 = direction1C.getTheta();
           double phiMin1 = direction1B.getPhi();
           double phiMax1 = direction1A.getPhi();
           if (zmax1 <= zmin1 ) {
               zmin1=zmax1;
           }
        double x0 = dir.getX(); //components
        double y0 = dir.getY();
        double z0 = dir.getZ();
        double distance0 = yd0/y0; //radial distance to hit position
        double xHit0 = x0*distance0; 
        double yHit0 = yd0;
        double zHit0 = z0*distance0;
        double thetaRotated0 = dir.getTheta();//angle of particles in lab frame: "rotated" is now a misnomer
        double phiRotated0 = dir.getPhi();

        double distance1 = yd1/y0; //radial distance to hit position
        double xHit1 = x0*distance1; //components of hit position
        double yHit1 = yd1;
        double zHit1 = z0*distance1;
        if (phiRotated0 <= phiMax0 && phiRotated0 >= phiMin0 
            && thetaRotated0 <= thetaMax0 && thetaRotated0 >= thetaMin0) {
            cosThetaInc = y0;//y (norm guaranteed to be 1)
            xstrip=(int)Math.floor((xHit0-xmin0)*nxStrip/Lx);
            double xmaxstrip=xmin0+w+xstrip*Lx/nxStrip;
            if (xHit0>xmaxstrip){
                xstrip=17;
            }
            zstrip=(int)Math.floor((zHit0-zmin0)*nzStrip/Lz);
            if (xstrip>=0&&xstrip<nxStrip&&zstrip>=0&&zstrip<nzStrip){
                hit=true;
                detector = 0;
                xHit=xHit0;
                zHit=zHit0;
                zMin=zmin0;
            }
        }//...and second...
        else if (phiRotated0 <= phiMax1 && phiRotated0 >= phiMin1 
            && thetaRotated0 <= thetaMax1 && thetaRotated0 >= thetaMin1) {
            cosThetaInc = y0;//y (norm guaranteed to be 1)
            xstrip=(int)Math.floor((xHit1-xmin1)*nxStrip/Lx);
            double xmaxstrip=xmin1+w+xstrip*Lx/nxStrip;
            if (xHit1>xmaxstrip){
                xstrip=17;
            }
            zstrip=(int)Math.floor((zHit1-zmin1)*nzStrip/Lz);
            if (xstrip>=0&&xstrip<nxStrip&&zstrip>=0&&zstrip<nzStrip){
                hit=true;
                xHit=xHit1;
                detector = 1;
                zHit=zHit1;
                zMin=zmin1;
            }
        }
        return hit;
    }
    public int getXStrip(){
        return xstrip;
    }
    public int getZStrip(){
        return zstrip;
    }
    public double getZHit(){
        return zHit;
    }
    public double getXHit(){
        return xHit;
    }
    public double getZMin(){
        return zMin;
    }
    /**Returns 1/cos(incidence angle).*/
    public double getIncidence(){
        return 1.0/cosThetaInc;
    }
    /**Returns distance to detector in mm.*/
    public double getDistance(){
        double x=0; 
        if (detector == 0.0){
             x = distance0;
        }
        else if (detector == 1.0){
             x = distance1;
        }
        return x;
    }
     public int getDetector(){
        return detector;
    }
    /** Weighting for isotropic thetas in degrees.*/
    public double weight(double x){
        return Math.sin(Math.toRadians(x));
    } 
}
