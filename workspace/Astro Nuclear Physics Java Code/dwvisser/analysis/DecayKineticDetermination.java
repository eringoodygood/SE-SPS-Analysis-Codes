/*
 * DecayKineticDetermination.java
 *
 * Created on June 12, 2001, 3:37 PM
 */

package dwvisser.analysis;
import dwvisser.nuclear.*;
import dwvisser.math.UncertainNumber;
import java.io.*;
import java.text.DecimalFormat; 

/**
 *
 * @author  <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version
 */
public class DecayKineticDetermination extends dwvisser.analysis.TextOutputter {
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
     
     private static double [] incidence = {1.2789, 1.2428, 1.2099, 1.1784, 1.1500, 1.1241,
     1.1016, 1.0818, 1.0643, 1.0508, 1.0408, 1.0358, 1.0336, 1.0265,
     1.0214, 1.0189};
     
     //these angles stored in radians
     private double [][] labAngles = new double[5][16];
     private double [][] Kinetic = new double[5][16];
     private double [][] Edeposit = new double[5][16];
     
     Reaction reaction;
     
     EnergyLoss targetLoss, deadLayerLoss;
     
     private static boolean SETUP=false;
     
     double targetX;//1/2 target thickness in ug/cm^2
     
     /** Creates new KineticDetermination
      * @param target nucleus
      * @param beam nucleus
      * @param projectile nucleus
      * @param beamEnergy in MeV
      * @param thetaSpec in degrees
      * @param decay emitted nucleus
      * @param residualExcitation state populated in spectrometer
      * @param lastExcitation state populated by decay detected in array
      * @param targetThickness in ug/cm^2
      * @param outfile where text goes
      * @param forward whether array is placed forward
      * @param milsAl mils of aluminum placed in front of array
      * @throws FileNotFoundException if file can't be created
      * @throws NuclearException if problem creating Nuceus objects
      */
     public DecayKineticDetermination(Nucleus target,Nucleus beam,
     Nucleus projectile,double beamEnergy,double thetaSpec,Nucleus decay,
     double residualExcitation, double lastExcitation, double targetThickness,
     String outfile, boolean forward, double milsAl) throws FileNotFoundException, NuclearException {
         super(outfile);
         try {
             setup(forward);
             if (setTargetThickness(targetThickness,target))
                 beamEnergy -= targetLoss.getThinEnergyLoss(beam,beamEnergy)/1000;
             reaction = new Reaction(target, beam, projectile,beamEnergy, 
             thetaSpec, new UncertainNumber(residualExcitation));
         } catch (KinematicsException ke) {
             System.err.println(ke);
         }
         //it is assumed that there is a single solution here
         //reaction.printStatus();
         Mdecay = decay.getMass().value;
         Nucleus ultimate = new Nucleus(reaction.getResidual().Z-decay.Z,
         reaction.getResidual().A-decay.A,lastExcitation);
         Mfinalstate = ultimate.getMass().value;
         double Mresid = reaction.getResidual().getMass().value;
         Msquare = Mresid*Mresid+Mdecay*Mdecay-Mfinalstate*Mfinalstate;
         Presid = reaction.getLabMomentumResidual(0);
         Eresid = reaction.getTotalEnergyResidual(0);
         thetaResid = Math.toRadians(reaction.getLabAngleResidual(0));
         gamma = reaction.getLabGammaResidual(0);
         beta = reaction.getLabBetaResidual(0);
         Solid deadLayer = new Solid(getThicknessAl(milsAl), Absorber.CM, "Al");//0.2 um+foil thickness
         deadLayerLoss = new EnergyLoss(deadLayer);
         System.out.println("strip\tD0\tD1\tD2\tD3\tD4");
         for (int strip = 0; strip < theta.length; strip++){
             System.out.print(strip);
             for (int det = 0; det < phi.length; det++) {
                 labAngles[det][strip] = getLabAngle(Math.toRadians(theta[strip]),
                 Math.toRadians(phi[det]));
                 Kinetic[det][strip] = getKinetic(labAngles[det][strip]);
                 if (Kinetic[det][strip] > 0) {
                     double Eflight = Kinetic[det][strip] - 0.001 *
                     targetLoss.getThinEnergyLoss(decay,Kinetic[det][strip],
                     Math.toRadians(180.0-theta[strip]));
                     Edeposit[det][strip] = Eflight - 0.001 *
                     deadLayerLoss.getEnergyLoss(decay,Kinetic[det][strip],
                     Math.acos(1/incidence[strip]));
                     System.out.print("\t"+round(Edeposit[det][strip]));//new
                 }
             }
             System.out.println();
         }
         try {
             closeOutput();
         } catch (IOException ioe){
             System.err.println(ioe);
         }
         revertToDefaultOutput();
         System.out.println("Done.");
     }
     
     private static void setup(boolean forward){
         if (!SETUP){
//             new EnergyLoss();//called to initialize energy loss routines
             if (forward) for (int i=0; i<theta.length; i++) theta[i]=180-theta[i];
             SETUP=true;
         }
     }
             
     
     /**
      * REturns thickness of Al in front of active region in cm.
      * @param foilThickness in mils
      */
     private double getThicknessAl(double foilThickness){
         double detectorDeadLayer = 0.2*1.0e-4;//in cm, i.e. 0.2 um
         double thicknessInCM=foilThickness/1000*2.54;
         return thicknessInCM+detectorDeadLayer;
     }
     
     
     static private String round(double number) {
         DecimalFormat dm=new DecimalFormat("##.###");
         return dm.format(number);
     }
     
     /**
      * @param thickness
      * @param target
      * @throws KinematicsException
      * @return  */
     public boolean setTargetThickness(double thickness, Nucleus target) throws KinematicsException{
         this.targetX = thickness/2;
         try {
             if (thickness != 0.0){
                 targetLoss = new EnergyLoss(new Solid(thickness,Absorber.MICROGRAM_CM2,
                 target.getElementSymbol()));
             } else {
                 targetLoss =null;
             }
         } catch (NuclearException ne) {
             System.err.println(getClass().getName()+".setReaction(): "+ne);
         }
         return (targetLoss !=null);//returns true if losses are to be accounted for
     }
     
     
     private double getLabAngle(double thetaDet, double phiDet){
         double cosThetaRelative = Math.sin(thetaResid)*Math.sin(thetaDet)*
         Math.cos(phiDet)+Math.cos(thetaResid)*Math.cos(thetaDet);
         return Math.acos(cosThetaRelative);
     }
     
     private double getKinetic(double thetaLab){
         double denom = 2*(Eresid*Eresid-
         Presid*Presid*Math.pow(Math.cos(thetaLab),2));
         double Edecay = (Eresid*Msquare+Presid*Math.cos(thetaLab)*
         Math.sqrt(Msquare*Msquare-Mdecay*Mdecay*2*denom))/denom;
         double Tdecay = Edecay - Mdecay;
         return Tdecay;
     }
     
     /**
      *
      * @param args the command line arguments
      */
     public static void main(String args[]) {
         double [] residualEx = {4.033};
         double [] finalEx = {0};
         Nucleus target = new Nucleus(10,20);
         Nucleus beam = new Nucleus(2,3);
         Nucleus projectile = new Nucleus(2,4);
         Nucleus decay = new Nucleus(2,4);
         Nucleus residual = new Nucleus(target.Z+beam.Z-projectile.Z,
         target.A+beam.A-projectile.A);
         for (int i=0; i<residualEx.length;i++){
             for (int j=0; j<finalEx.length;j++) {
                 double beamEnergy = 25;//MeV
                 double residEx = residualEx[i];//MeV
                 int textEx=(int)Math.round(residEx*1000);//keV
                 //String out = DEFAULT;
                 double theta = 20; //degrees
                 double ultimateEx=finalEx[j];
                 int textUlEx=(int)Math.round(ultimateEx*1000);
                 double targetThickness = 20.0;//thickness in ug/cm^2
                 boolean arrayForward=false; //do calculations for array in forward position
                 double AlFoilThickness=0;//in mils
                 String which="back";
                 if (arrayForward) which="front";
                 String out = "d:/simulations/"+residual+"_"+
                 (int)beamEnergy+"MeV_"+(int)theta+
                 "deg_"+textEx+"_"+decay+"_"+textUlEx+"_"+which+"_"+AlFoilThickness+"mil.dat";
                 try {
                     new DecayKineticDetermination(target, beam, projectile, beamEnergy, theta,
                     decay, residEx,ultimateEx,targetThickness,out,arrayForward,AlFoilThickness);
                 } catch (FileNotFoundException fnfe) {
                     System.out.println(fnfe);
                 } catch (NuclearException ne) {
                     System.out.println(ne);
                 }
             }
         }
     }     
}