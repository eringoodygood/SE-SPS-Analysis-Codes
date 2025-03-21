/*
 */
package dwvisser.monte;
import dwvisser.nuclear.*;
import java.io.*;
import java.util.Random;
import dwvisser.math.UncertainNumber;
import java.text.DecimalFormat;

/**
 * This class will produce a Monte Carlo simulation of the in-flight
 * decay of a nucleus in an excited state produced and (presumably)
 * tagged by the Enge SplitPole.  The code also determines if and where
 * the decay product interacts with two silicon strip detectors.
 * April 04: modified to take into account the position sensitivity:
 * criterion for a good event is now energy above threshold deposited 
 * in each end of the strip.
 * Sept 04: modified to do boost right 
 * and to handle angles (absolute range of detectors and conversion from lab to cm) right (corrected oct 11)
 * Jan 25 2005: geometry for Brady is now up to date, according to Rachel!
 */
public class EngeYLSA_Simulation90 extends Object {
    static final double C_MM_PER_NSEC = 299.792458;
    static final double DEAD_LAYER_THICKNESS = 0.2; //um
    static final int STRIPS_PER_DET = 16;
    static final int NUM_DET = 2;

    //Fields specified in constructor
    Nucleus target, beam, projectile, decay;
    double xtarg; //thickness in ug/cm^2
    double thetaTarget;//angle of target, in degrees, measured between target's normal and incoming beam
    double Ebeam; //beam kinetic energy in MeV
    double ExResid; //in MeV
    double ExUltimate; //in MeV
    double theta; //for spectrometer, in degrees
    String outFile;

    //Fields set by calculation
    Nucleus residual, ultimate;
    double thetaR; //for spectrometer, in radians
    FileWriter outDescription,outAngles,outEvn;//,outAngles,outCounts;/*, */
//    FileWriter[] outEvn=new FileWriter[NUM_DET*STRIPS_PER_DET];
//  FileWriter[] outAngles=new FileWriter[NUM_DET*STRIPS_PER_DET];

    /** Creates new EngeYLSA_Simulation */
    public EngeYLSA_Simulation90() {
/**we want to keep track of statistics in each z bin in each strip*/
        int[][] counts = new int[NUM_DET*STRIPS_PER_DET][STRIPS_PER_DET];
/**this is hard-wiring in the first index to be the x bin (i.e. strip number plus 16* the detector number)
 * and the second index to be the z strip (i.e. the position along the strip, subdivided into 16 chunks)*/
        double[][] Emin = new double[NUM_DET*STRIPS_PER_DET][STRIPS_PER_DET];
        double[][] Emax = new double[NUM_DET*STRIPS_PER_DET][STRIPS_PER_DET];
        DataSet [][] stripLabtheta = new DataSet[NUM_DET*STRIPS_PER_DET][STRIPS_PER_DET];
        DataSet [][] stripEnergy = new DataSet[NUM_DET*STRIPS_PER_DET][STRIPS_PER_DET];
        DataSet [][] stripLabphi = new DataSet[NUM_DET*STRIPS_PER_DET][STRIPS_PER_DET];
        DataSet [][] stripInc = new DataSet[NUM_DET*STRIPS_PER_DET][STRIPS_PER_DET];
//      double fractionalErrorPerStrip = .1;
//      int hitsWanted = NUM_DET*STRIPS_PER_DET *STRIPS_PER_DET* (int) Math.pow(fractionalErrorPerStrip, -2);
//      System.err.println("Total hits wanted: "+hitsWanted+" to get "+(100*fractionalErrorPerStrip)
//          +"% error each strip");
/** numEvents will be expanded as needed so that hitsWanted will be exceeded by approximately 20% */
//      int numEvents = (int) Math.round(hitsWanted * 1.2);//total events to simulate
        int numEvents=40000;

        double x0 = -5; //
        double y0 = 26; //distance in mm to (x,y,z) centre of one detector--passed to "micron detector"
        double z0 = 4; 
        double theta0 = 90; //angle of array detectors: between dector's normal and beam line 
        double x1 = 3; // information for second detector
        double y1 = -28;
        double z1 = 3; 
        double theta1 = -90; 
//		double L=50;
		double Lz=40;

        System.err.println("EngeYLSA_Simulation90: y0,z0,theta0,y1,z1,theta1: "+y0+","+z0+","+theta0+","
            +y1+","+z1+","+theta1);

        target = new Nucleus(13,26);//!!!6,12)13,27
        beam = new Nucleus(2, 3);//!!!
        projectile = new Nucleus(1, 3);//!!!
        decay = new Nucleus(1, 1);//!!!
        Ebeam = 25; //beam kinetic energy in MeV !!!
        xtarg = 150;// //thickness in ug/cm^2 !!!40;100;
        String starg = "Al 1";////material specification for target, by mass"Al 1";"C 168 H 6 N 90 O 8";
//"C 36 H 6 N 90" is 15N melamine (chemical formula) 
//"C 168 H 6 N 90 O 8" is the 15N melamine tgt
//"Al 1"
//"O 8" "C 3 Ca 80 F 80"
        thetaTarget=60;//angle of target, in degrees, measured between target's normal and incoming beam
        theta = 0; //for spectrometer, in degrees !!!
        double thetaAcceptance = 0.080; //choose "theta" of projectile <= +/- this
        double phiAcceptance = 0.040; //choose "phi" of projectile <= +/- this
        double randomAcceptanceMax =Math.sqrt(Math.pow(thetaAcceptance, 2) + Math.pow(phiAcceptance, 2));
        double[] ExResidValues = {5.970};//2.365,3.55,6.36 for n13
        //8.65,8.289,8.037,7.741,8.206,7.832,7.971 for 27Si;
        //16,15.49 , 12.742 ,12.663,14.36for 28Si
        int[] angularMomentumValues = {0};//0,1,2,3assumed orbital angular momentum for the decay !!!
//2 for 3,4x in 13N; 0 for 1x
        double[] ExUltimateValues = {0}; 
//in MeV; ,0.2280 .416852 1.058,1.759 for 26Al; 
//0.0, 0.844, 1.1014,2.212, 2.735 2.982 3.004 3.680 3.957 4.055 for 27Al
        double AlThickness = 0;
        double hit_threshold = 0.15;    //energy deposited in MeV to be considered a hit !!!

        /*** Calculation of some fields based on initial values ***/
        for (int rU = 0; rU < ExUltimateValues.length; rU++) {
        for (int rEx = 0; rEx < ExResidValues.length; rEx++) {
            for (int rL = 0; rL < angularMomentumValues.length; rL++) {
            for (int i=0;i<NUM_DET*STRIPS_PER_DET; i++){/**this is hard-wiring the sizes of these arrays*/
                for (int j=0;j<STRIPS_PER_DET; j++){
                    stripLabtheta[i][j]=new DataSet();  
                    stripLabphi[i][j]=new DataSet();    
                    stripInc[i][j]=new DataSet();   
                    stripEnergy[i][j]=new DataSet();    
                    counts[i][j]=0;
                    Emin[i][j]=0;
                    Emax[i][j]=0;//need to set these arrays to zero every time we do a new reaction! 
                }
            }
            ExUltimate=ExUltimateValues[rU];
            ExResid = ExResidValues[rEx]; //in MeV
            int angularMomentum=angularMomentumValues[rL];
            residual =
                new Nucleus(
                    target.Z + beam.Z - projectile.Z,
                    target.A + beam.A - projectile.A,
                    ExResid);
//          System.out.println("residual "+residual.Z+residual.A);
            ultimate =
                new Nucleus(
                    residual.Z - decay.Z,
                    residual.A - decay.A,
                    ExUltimate);
//          System.out.println("ultimate "+ultimate.Z+ultimate.A);

            String outFileRoot =
			"C:/Documents and Settings/Owner/My Documents/simulations/brady/prospectus/"
                + (int) Math.round(ExResid * 1000)+ "_"+ residual+ "_"
                + (int) Math.round(ExUltimate * 1000)+ "_"+ ultimate
                + "_L"+ angularMomentum
                + "_" + (int) Math.round(Ebeam)+ "MeV"+"_test";
            thetaR = Math.toRadians(theta);
            System.out.println(target+ "("+ beam+ ","+ projectile+ ") -> "+ ultimate+ "+"+ decay);
            System.out.println("File root: " + outFileRoot);
            try {
                outDescription = new FileWriter(outFileRoot + ".txt");
              outEvn = new FileWriter(outFileRoot + ".csv");
//                for (int i=0;i<NUM_DET*STRIPS_PER_DET; i++){
  //                  outEvn[i] = new FileWriter(outFileRoot +"_"+i+ ".csv");
//                  outAngles[i] = new FileWriter(outFileRoot +"_"+i+ ".ang");
    //            }
//              outCounts = new FileWriter(outFileRoot + ".sum");
                outAngles = new FileWriter(outFileRoot + ".ang");
            } catch (IOException ioe) {
                System.err.println(ioe);
            }

            Random random = new Random();
            double Mresid = residual.getMass().value;
//          System.out.println("Mresid "+Mresid);
            double Mdecay = decay.getMass().value;
//          System.out.println("Mdecay "+Mdecay);
            double Mdecay2 = Mdecay * Mdecay;
            double Multimate = ultimate.getMass().value;
//          System.out.println("Multimate "+Multimate);
            double excess = Mresid - Multimate - Mdecay;
            try {
                if (excess < 0) {
                    throw new NuclearException("Mass of final system is "+ (-excess)+ " MeV above the state.");
                }
                double Multimate2 = Multimate * Multimate;
                double PcmDecay =Math.sqrt(Mresid * Mresid- 2 * (Mdecay2 + Multimate2)
                        + Math.pow((Mdecay2 - Multimate2) / Mresid, 2))/ 2;             //MeV/c
                double EcmDecay =Math.sqrt(PcmDecay * PcmDecay + Mdecay * Mdecay);
                System.out.println(
                    "Ex = " + ExResid+ " MeV; CM K.E. for detected decay product = "+ (EcmDecay - Mdecay)+ " MeV");
                MicronDetector90 md =new MicronDetector90(x0,y0,z0, Math.toRadians(theta0),x1,y1,z1, 
                    Math.toRadians(theta1),NUM_DET,STRIPS_PER_DET);
                Solid deadLayer;
                EnergyLoss deadLayerLoss;
                outDescription.write(target+ "("+ beam+ ","+ projectile+ ")"+ residual+ "("+ decay+ ")"+ ultimate+ "\n");
                outDescription.write("Ex(residual " + residual + ") = " + ExResid + " MeV\n");
                outDescription.write("Ex(final " + ultimate + ") = " + ExUltimate + " MeV\n");
                outDescription.write("Theta(projectile "+ projectile+ ") = "+ theta+ " degrees\n");
                outDescription.write("Target Thickness = " + xtarg + " ug/cm^2\n");
                outDescription.write("l = " + angularMomentum + " decay\n");
                deadLayer = new Solid(getThicknessAl(AlThickness), Absorber.CM, "Al");
                deadLayerLoss = new EnergyLoss(deadLayer);

                double[] p_CM = new double[4];
                double[] p_lab = new double[4];
                /*** work begins here ***/
                Reaction reaction;
                Boost labBoost;
                Direction directionCM;
                int angleBinning = 60;
                double divFactor = 180.0 / angleBinning;
                int[] thetaCM = new int[angleBinning];
                int[] thetaLab = new int[angleBinning];
                int[] thetaCMall = new int[angleBinning];
                int[] thetaLaball = new int[angleBinning];
                int hits = 0;
//                for (int i=0;i<NUM_DET*STRIPS_PER_DET; i++){/**this is hard-wiring the sizes of these arrays*/
  //                  outEvn[i].write("theta,Edep,EL,ER\n");//,
//                  outAngles[i].write("theta,hits");
//                  outEvn[i].write("\n");
    //            }
				outEvn.write("theta,Edep,EL,ER\n");//,
//              outEvn.write("hit,projTheta,projPhi,CMtheta,CMphi,labTheta,labPhi,x,z,inc.,Edep,EL,ER" +
//                  ",det,xstrip,zstrip,xbin,zbin\n");//header for .csv file    
//              outEvn.write("cmtheta,labtheta,Edep\n");            
                int _i = 0;
                do {
                    _i++;
                    double depth = random.nextDouble() * xtarg;
                    Solid targetMatter = new Solid(starg, depth);
                    EnergyLoss targetLossCalc = new EnergyLoss(targetMatter);
                    double targetLoss =0.001 * targetLossCalc.getThinEnergyLoss(beam, 
                        Ebeam, Math.toRadians(thetaTarget));//this takes into account angle of target
                    double Tbeam = Ebeam - targetLoss;
//randomly select a direction into the spectrometer assuming its slits are centered on the beam axis
                    Direction projDir = null;
                    boolean directionAccepted = false;
                    while (!directionAccepted) {
                        Direction _randomProjDir =Direction.getRandomDirection(0,randomAcceptanceMax);
//"theta" for theta acceptance actually arcTan(x/z) or tan("theta")=tan(theta)cos(phi)
                        double tempTanTheta =Math.abs(
                                Math.tan(_randomProjDir.getTheta())* Math.cos(_randomProjDir.getPhi()));
                        double tempTanPhi =Math.abs(
                                Math.tan(_randomProjDir.getTheta())* Math.sin(_randomProjDir.getPhi()));
                        directionAccepted =
                            (tempTanTheta <= Math.tan(thetaAcceptance))&& (tempTanPhi <= Math.tan(phiAcceptance));
                        if (directionAccepted) {
//rotate random direction to be about actual spectrometer location
//theta = angle the spectrometer is set at
                            projDir =_randomProjDir.rotateY(Math.toRadians(theta));
                        }
                    }
//create reaction for theta from projDir
                    reaction =new Reaction(target,beam,projectile,Tbeam,projDir.getThetaDegrees(),ExResid);
//determine direction residual nucleus is moving
//theta comes from reaction object, (taking absolute value, as the minus sign usually returned causes 
//the new phi to be calculated wrong)
//phi is opposite the phi of the projectile
                    Direction residDir =new Direction(Math.toRadians(Math.abs(reaction.getLabAngleResidual(0))),
                        Math.PI + projDir.getPhi());
//create boost object to convert CM 4-momentum to lab 4-momentum
                    labBoost =new Boost(reaction.getLabBetaResidual(0), residDir);
                    labBoost = Boost.inverseBoost(labBoost);
//consider l=angularMomentum case:
//The assumption is theta = 0 degrees exactly for the projectile (not exactly true, of course) 
//so that the correlation function is just a Legendre Polynomial (corresponding to only M=0 being populated).
                    directionCM = Direction.getRandomDirection(angularMomentum);
//                  directionCM = new Direction(Math.toRadians(120),Math.toRadians(260));
//                  System.out.println("cmdir: theta "+ Math.round(Math.toDegrees(directionCM.getTheta()))
//                      +" phi "+ Math.round(Math.toDegrees(directionCM.getPhi())));
//copy components of momentum into 4-vector
                    p_CM[0] = EcmDecay;
                    System.arraycopy(directionCM.get3vector(PcmDecay),0,p_CM,1,3);
                    p_lab = labBoost.transformVector(p_CM);
//extract direction of decay product in lab
                    Direction directionLab =new Direction(p_lab[1], p_lab[2], p_lab[3]);
//                  Direction directionLab =new Direction(Math.toRadians(70),Math.toRadians(90));
//                  Direction directionLab =new Direction(0, 1, 0);
//                  System.out.println("labdir: theta "+ Math.round(Math.toDegrees(directionLab.getTheta()))
//                      +" phi "+ Math.round(Math.toDegrees(directionLab.getPhi())));
                    boolean hit = md.isHit(directionLab); 
                    int xstrip, zstrip, det;
                    double Edep, tof;
                    double Y = directionLab.getY();
                    double thetaDlab = directionLab.getThetaDegrees();
                    double phiDlab = directionLab.getPhiDegrees();
                    double thetaDcm = directionCM.getThetaDegrees();
                    double phiDcm = directionCM.getPhiDegrees();
                    thetaCMall[(int) Math.floor(thetaDcm / divFactor)]++;
                    thetaLaball[(int) Math.floor(thetaDlab / divFactor)]++;
                    if (hit) {
                        xstrip = md.getXStrip();
                        zstrip = md.getZStrip();
                        det = md.getDetector();
                        int xbin = xstrip + det * STRIPS_PER_DET;
                        int zbin = zstrip + det * STRIPS_PER_DET;
                        double incidence = md.getIncidence();
                        double Tinit = p_lab[0] - Mdecay;
                        double angleOut = Math.abs(180 -thetaDlab - thetaTarget);
                        if (Y<=0){
                            angleOut = Math.abs(thetaDlab - thetaTarget);
                        }
                        double angleOutR=Math.toRadians(angleOut);
                        double Tflight =
                            Tinit- 0.001* targetLossCalc.getThinEnergyLoss(
                                        projectile,Tinit,angleOutR);
                        Edep =Tflight- 0.001* deadLayerLoss.getEnergyLoss(
                                        projectile,
                                        Tflight,
                                        Math.acos(1 / Math.abs(incidence)));
                        double xHit = md.getXHit();
                        double zHit = md.getZHit();
                        double zMin = md.getZMin();
                        double ER=Edep*(zHit-zMin)/Lz;
                        double EL=Edep-ER;
                        boolean energyHit = (EL >= hit_threshold)&&(ER >= hit_threshold);
                        if (energyHit) {
                            thetaCM[(int) Math.floor(thetaDcm / divFactor)]++;
                            thetaLab[(int) Math.floor(thetaDlab / divFactor)]++;
                            stripLabtheta[xbin][zstrip].add(thetaDlab);
                            stripLabphi[xbin][zstrip].add(phiDlab);
                            stripEnergy[xbin][zstrip].add(Edep);
                            stripInc[xbin][zstrip].add(incidence);
                            hits++;
                            counts[xbin][zstrip]++;
                            if (Emin[xbin][zstrip] == 0.0 || Emin[xbin][zstrip] > Edep)
                                Emin[xbin][zstrip] = Edep;
                            if (Emax[xbin][zstrip]== 0.0 || Emax[xbin][zstrip] < Edep)
                                Emax[xbin][zstrip] = Edep;
							outEvn.write(round(directionLab.getThetaDegrees())
							+","+round(Edep)+","+round(EL)+ ","+ round(ER)+"\n");
 //                           for (int i=0;i<NUM_DET*STRIPS_PER_DET; i++){
   //                             if (xbin==i) outEvn[i].write(round(directionLab.getThetaDegrees())
     //                           +","+round(Edep)+","+round(EL)+ ","+ round(ER)+"\n");
       //                     }
//                          outEvn.write(round(directionCM.getThetaDegrees())
//                          +","+round(directionLab.getThetaDegrees())+","+round(Edep)+"\n");
/*                          outEvn.write("1,"
                                    + round(projDir.getThetaDegrees())+ ","
                                    + round(projDir.getPhiDegrees())+ ","
                                    + round(thetaDcm)+ ","
                                    + round(directionCM.getPhiDegrees())+ ","
                                    + round(thetaDlab)+ ","
                                    + round(directionLab.getPhiDegrees())+ ","
                                    + round(xHit)+ ","
                                    + round(zHit)+ ","
                                    + round(incidence)+ ","
                                    + round(Edep)+ ","
//                                  + round(tof)+ ","
                                    + round(EL)+ ","
                                    + round(ER)+ ","
                                    + det+ ","
                                    + xstrip+ ","
                                    + zstrip+ ","//!!!
                                    + xbin+ ","
                                    + zbin+ "\n");*/
                        } /*else {
                            outEvn.write("0,"
                                    + round(projDir.getThetaDegrees())+ ","
                                    + round(projDir.getPhiDegrees())+ ","
                                    + round(thetaDcm)+ ","
                                    + round(directionCM.getPhiDegrees())+ ","
                                    + round(thetaDlab)+ ","
                                    + round(directionLab.getPhiDegrees())+ ","
                                    + round(incidence)+ ","
                                    + round(Edep)+ ","
//                                  + round(tof)+ ","
                                    + det+ ","
                                    + xstrip
                                    + zstrip+ ","//!!!
                                    + xbin+ ","
                                    + zbin+ "\n");
                        }
                */  } /*else {
                        outEvn.write("0,"
                                + round(projDir.getThetaDegrees())+ ","
                                + round(projDir.getPhiDegrees())+ ","
                                + round(thetaDcm)+ ","
                                + round(directionCM.getPhiDegrees())+ ","
                                + round(thetaDlab)+ ","
                                + round(directionLab.getPhiDegrees())+ ",,,,,"+ "\n");
                    }
*/                  if (_i % 1000 == 0) {
                        System.out.println(_i + ":" + hits);
                    }
/*                  if (_i == numEvents && hits < hitsWanted) {
                        numEvents =
                            (int) Math.round(
                                1.2 * (double) _i / (double) hits * hitsWanted);
                        System.err.println(
                            "Events so far = " + _i + ", " + hits + " hits.");
                        System.err.println(
                            "Changing total events to simulate to "+ numEvents+ ".");
                    }*/
                }
                while (_i < numEvents);
//                for (int i=0;i<NUM_DET*STRIPS_PER_DET; i++){
  //                  outEvn[i].flush();
    //                outEvn[i].close();
      //          }
			  	outEvn.flush();
	   			outEvn.close();
            /*              outEvn.flush();
                outEvn.close();
                            outCounts.write("xbin\tdet\tzpixel\tcounts\tEmin\tEmax\n");
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 16; j++) {
                        int bin = i * 16 + j;
                        outCounts.write(bin+ "\t"
                                + i+ "\t"
                                + j+ "\t"
                                + counts[bin][j]+ "\t"
                                + round(Emin[bin][j])+ "\t"
                                + round(Emax[bin][j])+ "\n");
                    }
                }
*/                  outAngles.write("Theta\tCMhits\tlabHits\tallCMcounts\tallLabCounts\n");
                for (int i = 0; i < angleBinning; i++) {
                    outAngles.write(
                            (i * 180 / angleBinning)+ "\t"
                            + round(thetaCM[i])+ "\t"
                            + round(thetaLab[i])+ "\t"
                    + round(thetaCMall[i])+ "\t"
                    + round(thetaLaball[i])+ "\n");
                }
                outAngles.flush();
                outAngles.close();
/*              outCounts.flush();
                outCounts.close();*/
                System.err.println("Done. "+ hits+ " detector hits for "+ _i + " simulated decays.");
                outDescription.write(_i + " simulated events\n");
                outDescription.write(hits + " hits\n");
                double efficiency = (double) hits / (double) _i;
                double delEff = Math.sqrt((double) hits) / (double) _i;
                UncertainNumber uncEff =new UncertainNumber(efficiency, delEff);
                outDescription.write("total efficiency: " + uncEff + "\n");
                outDescription.write("\nhits\tLabTheta\tE\tEmin\tEmax\tsdE\tsdTheta\tLabPhi\tsdPhi" +
                    "\tEff\tdelEff\txbin\tzstrip\tInc\tdelInc\n");
                for (int i=0;i<NUM_DET*STRIPS_PER_DET; i++){/**this is hard-wiring the sizes of these arrays*/
                    for (int j=0;j<STRIPS_PER_DET; j++){
                        int _hits = stripEnergy[i][j].getSize();
                        double _eff = (double)_hits/_i;
                        double _deleff = Math.sqrt(_hits)/_i;
                        outDescription.write(_hits+"\t"
                            +round(stripLabtheta[i][j].getMean())+"\t"
                            +round(stripEnergy[i][j].getMean())+"\t"
                            + round(Emin[i][j])+ "\t"
                            + round(Emax[i][j])+ "\t"
                            +round(stripEnergy[i][j].getSD())+"\t"
                            +round(stripLabtheta[i][j].getSD())+"\t"
                            +round(stripLabphi[i][j].getMean())+"\t"
                            +round(stripLabphi[i][j].getSD())+"\t"
                            +_eff+"\t"+_deleff+"\t"
                            +i+"\t"+j+"\t"
                            +round(stripInc[i][j].getMean())+ "\t"
                            +round(stripInc[i][j].getSD())+ "\n");
                    }
                }
                outDescription.flush();
                outDescription.close();
            } catch (KinematicsException ke) {
                System.err.println(ke);
            } catch (NuclearException ke) {
                System.err.println(ke);
            } catch (dwvisser.math.MathException me) {
                System.err.println(me);
            } catch (IOException ioe) {
                System.err.println(ioe);
            }
        }
    }
    }
    }
    static private String round(double number) {
        DecimalFormat dm = new DecimalFormat("##.###");
        return dm.format(number);
    } 
    /** Returns thickness of Al in front of active region in cm (foil plus dead layer).
     * @param foilThickness in mils  */ 
    private double getThicknessAl(double foilThickness) {
        double detectorDeadLayer = DEAD_LAYER_THICKNESS * 1.0e-4;//in cm, i.e. 0.2 um
        double thicknessInCM = foilThickness / 1000 * 2.54;
        return thicknessInCM + detectorDeadLayer;
    }
    static public void main(String[] args) {
        new EngeYLSA_Simulation90();
    }
}
