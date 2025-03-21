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
 * @author  dwvisser
 * @version
 */
public class PIDtelescope extends dwvisser.analysis.TextOutputter {
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
     public PIDtelescope(double milsAl, double [] umDetectors, double beamEnergy, Nucleus beam,
     Nucleus target, Nucleus [] projectile, String outfile) throws FileNotFoundException, NuclearException {
         super(outfile);
         try {
             EnergyLoss foilLoss=null;
             if (milsAl>0) foilLoss = new EnergyLoss(new Solid(milsAl,
             Absorber.MIL, "Al"));
             EnergyLoss [] detectorLoss = new EnergyLoss[umDetectors.length];
             System.out.print("Strip\tion\tEx\tEinit\tfoil");
             for (int i=0; i<umDetectors.length; i++){
                 detectorLoss[i]=new EnergyLoss(new Solid(umDetectors[i]*1e-4,
                 Absorber.CM,"Si"));
                 System.out.print("\tSi"+(int)umDetectors[i]);
             }
             System.out.println();
             setup(true);//change theta values to forward array
             for (int strip = 0; strip < theta.length; strip++) {
                 System.err.print("\nStrip "+strip);
                 double theta_inc = Math.acos(1/incidence[strip]);
                 for (int proj=0; proj<projectile.length; proj++) {
                     System.err.print(", "+projectile[proj]);
                     for (int Ex=0; Ex<=60; Ex += 5){
                         Reaction reaction=null;
                         try {
                            reaction = new Reaction(target, beam, projectile[proj],
                            beamEnergy, theta[strip], new UncertainNumber(Ex));
                         } catch (KinematicsException ke) {
                             //only thrown for bad angle, handled below
                         }
                         if (reaction != null && reaction.getAngleDegeneracy()>0){
                             double Einit = reaction.getLabEnergyProjectile(0);
                             double Efoil=0;
                             if (foilLoss != null) Efoil=0.001*foilLoss.getEnergyLoss(
                             projectile[proj],Einit,theta_inc);
                             System.out.print(strip+"\t"+projectile[proj]+"\t"+
                             Ex+"\t"+round(Einit)+"\t"+round(Efoil));
                             double Ecurrent = Einit-Efoil;
                             double [] detDeposit = new double[detectorLoss.length];
                             for (int detector=0; detector<detectorLoss.length; detector++){
                                 if (Ecurrent > 0.1) detDeposit[detector] = 0.001*detectorLoss[detector].getEnergyLoss(
                                 projectile[proj],Ecurrent,theta_inc);
                                 Ecurrent -= detDeposit[detector];
                                 System.out.print("\t"+round(detDeposit[detector]));
                             }
                             System.out.println();
                         }
                     }
                 }
             }
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
      *
      * @param args the command line arguments
      */
     public static void main(String args[]) {
//         new EnergyLoss();
         Nucleus target = new Nucleus(6,12);
         Nucleus beam = new Nucleus(8,16);
         Nucleus [] projectile = {new Nucleus(1,1), new Nucleus(2,4), 
         new Nucleus(6,12), new Nucleus(8,16)};
         double foil = 0;
         double [] thicknesses = {50,500};
         double energy=92;
         String out="c:/simulations/";
         for (int i=0;i<projectile.length;i++) out += "_"+projectile[i];
         for (int i =0; i<thicknesses.length; i++) out += "_"+(int)thicknesses[i];
         out += ".dat";
         try {
             new PIDtelescope(foil, thicknesses, energy, beam, target, projectile,
             out);
         } catch (FileNotFoundException fnfe) {
             System.out.println(fnfe);
         } catch (NuclearException ne) {
             System.out.println(ne);
         }
     }
}