/*
 * CMangleDetermination.java
 *
 * Created on June 8, 2001, 3:37 PM
 */
package dwvisser.analysis;
import dwvisser.nuclear.*;
import dwvisser.math.UncertainNumber;

/**
 * A simple code for finding out what the CM angles for various YLSA
 * strips would be for a given branching ratio experiment.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class CMangleDetermination extends Object {
    /**
     * Angle of residual, in radians.
     */
    double thetaResid;
    
    /**
     * Relativistic gamma parameter for residual velocity in lab.
     */
    double gamma;
    
    /**
     * Mass of decay nucleus from residual.
     */
    double Mdecay;
    
    /**
     * Mass of ultimate decay product.
     */
    double Mfinalstate;
    
    /**
     * mass parameter for determining decay energy
     */
    double Msquare;
    
    /**
     * momentum of residual, MeV/c
     */
    double Presid;
    
    /**
     * mass-energy of residual
     */
    double Eresid;
    
    /**
     * lab velocity of residual (CM system)
     */
    double beta;
    
    /**
     * Phi of detectors in degrees.
     */
    private static double [] phi = {198.0, 126.0, 54.0, -18.0, -90.0};
    
    /**
     * theta of strips in degrees.
     */
    private static double [] theta = 
    {165.95, 164.2, 162.3, 160.4, 158.4, 156.2, 154.0, 151.7,
        149.3, 146.85, 144.3, 141.6, 138.9, 136.2, 133.4, 130.6};
    
    /* these angles stored in radians */
    private double [][] labAngles = new double[5][16];
    private double [][] CMangles = new double[5][16];
    
    Reaction reaction;

    /** Creates new CMangleDetermination */
    public CMangleDetermination(Nucleus target, Nucleus beam, 
    Nucleus projectile, double beamEnergy, double thetaSpec, Nucleus decay,
    double residualExcitation) {
        try {
            reaction = new Reaction(target, beam, projectile,beamEnergy, thetaSpec,
            new UncertainNumber(residualExcitation));
        } catch (KinematicsException ke) {
            System.err.println(ke);
        }
        /* it is assumed that there is a single solution here */
        Mdecay = decay.getMass().value;
        Nucleus ultimate = new Nucleus(reaction.getResidual().Z-decay.Z,
        reaction.getResidual().A-decay.A, residualExcitation);
        Mfinalstate = ultimate.getMass().value;
        double Mresid = reaction.getResidual().getMass().value;
        Msquare = Mresid*Mresid+Mdecay*Mdecay-Mfinalstate*Mfinalstate;
        Presid = reaction.getLabMomentumResidual(0);
        Eresid = reaction.getTotalEnergyResidual(0);
        thetaResid = Math.toRadians(reaction.getLabAngleResidual(0));
        gamma = reaction.getLabGammaResidual(0);
        beta = reaction.getLabBetaResidual(0);
        System.out.println("Gamma: "+gamma+", Beta: "+beta);
        System.out.println("det\tstrip\tlab\tCM");
        for (int det = 0; det < phi.length; det++){ 
            for (int strip = 0; strip < theta.length; strip++) {
                labAngles[det][strip] = getLabAngle(Math.toRadians(theta[strip]), 
                Math.toRadians(phi[det]));
                CMangles[det][strip] = getCMangle(labAngles[det][strip]);
                System.out.println(det+"\t"+strip+"\t"+
                Math.toDegrees(labAngles[det][strip])+"\t"+
                Math.toDegrees(CMangles[det][strip]));
            }
        }       
    }

    private double getLabAngle(double thetaDet, double phiDet){
        double cosThetaRelative = Math.sin(thetaResid)*Math.sin(thetaDet)*
                    Math.cos(phiDet)+Math.cos(thetaResid)*Math.cos(thetaDet);
        return Math.acos(cosThetaRelative);
    }

    private double getCMangle(double thetaLab){
        double denom = 2*(Eresid*Eresid-
        Presid*Presid*Math.pow(Math.cos(thetaLab),2));
        double Edecay = (Eresid*Msquare+Presid*Math.cos(thetaLab)*
        Math.sqrt(Msquare*Msquare-Mdecay*Mdecay*2*
        denom))/denom;
        double gDecay = Edecay/Mdecay;
        double bDecay = Math.sqrt(1-Math.pow(gamma,-2));
        double bDx = bDecay*Math.cos(thetaLab);
        double bDy = bDecay*Math.sin(thetaLab);
        /* next 2 lines would have to be divided by 1-beta*bDx to be 
         * true velocities, but we only care about their ratio */
        double bDxCM = bDx-beta;
        double bDyCM = bDy/gamma;
        return Math.atan2(bDyCM,bDxCM);
    }
        
    /**
    * 
    * @param args the command line arguments
    */
    public static void main (String args[]) {
        Nucleus target = new Nucleus(6,12);
        Nucleus beam = new Nucleus(6,12);
        Nucleus projectile = new Nucleus(2,4);
        Nucleus decay = projectile;
        double beamEnergy = 79.821;
        double theta = 5.0; //degrees
        double Ex = 15.0;
        new CMangleDetermination(target, beam, projectile, beamEnergy, theta, 
        decay, Ex);        
    }
}