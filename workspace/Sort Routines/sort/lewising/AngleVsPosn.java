/*
 */
package sort.lewising;
import java.io.*;
import jam.data.*;
import jam.sort.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;
import sort.coinc.offline.ResidualKinematics;
import sort.coinc.offline.SurfaceAlphaEnergyLoss;

/** Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>, which
 * was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right way; also 
 * added gate to cathAnde
 *
 * @author Dale Visser
 * @since 26 July 2001
 */
public class AngleVsPosn extends SortRoutine { 
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 128;//ADC lower threshold in channels
    static final int LAST_ADC_BIN = 3840;
    static final int D_WIRES = 100;  // distance between wires in millimeters

    //names
//    static final String DEAD_TIME="Dead Time %";
 //   static final String TRUE_DEAD_TIME = "True Dead Time %";

    //histogramming constants
    final int ADC_CHANNELS = 4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 1024;//number of channels in compressed histogram
    final int TWO_D_CHANNELS = 256; //number of channels per dimension in 2-d histograms
    //bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    
    // ungated spectra
    Histogram hCthd,hAnde,hSntr1,hSntr2,hSntrSum, hFrntPsn, hRearPsn, hTheta, hThetaPID, hPhi, hFC, hFCPID;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;     // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hSntrCthd, hFrntCthd, hFrntAnde, hFrntSntr, hFrntPRearP;
    Histogram hSntrAnde;
    Histogram hFrntY, hRearY, hYvsPsn, hYvsPsnGPID;
    Histogram hSntr1Sntr2;
    Histogram hFrntYRearY;
    Histogram hFrntTheta,hFrntThetaPID,hFCThetaPID,hFrntPhiPID,hFCPhiPID,hRearTheta;
    Histogram hThetaPhi,hThetaPhiPID;

    //gated spectra
/*    Histogram hFrntSntrGSC, hFrntCthdGSC;//gate by scintillator cathode
    Histogram hSntrCthdGFC, hFrntSntrGFC;//gate by Front wire Cathode
    Histogram hSntrCthdGFS, hFrntCthdGFS;//gate by Front wire Scintillator
*/    Histogram /*hFrntGCSF, hRearGCSF,*/ hFrntRearGCSF, hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed & time
/*    Histogram hcFrntGTime, hFrntGTime;//front and rear wire gated on All compressed & time
    Histogram hCthdAndeGFS, hCthdAndeGFC, hCthdAndeGSC;
    Histogram hFrntSntrGCA, hFrntCthdGCA, hSntrCthdGCA;
    Histogram hSntr1Sntr2GSC, hSntr1Sntr2GFC, hSntr1Sntr2GFS, hSntr1Sntr2GCA;
    Histogram hSntrCthdGS12, hFrntCthdGS12, hFrntSntrGS12, hCthdAndeGS12;*/
    
    /*
     * 1D gates
     */
 //   Gate  gCthd, gPeak; 
    /*
     * 2D gates
     */
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear, gCthdAnde, gXY, gSntr1Sntr2, gFYRY;
     Gate    gFrntTheta;
   
    //parameters
    DataParameter   pRhoSlope, pRhoOffset;
    DataParameter pXSlope,pXOffset;
    DataParameter pXTheta,pThetaX,pThetaX3, pXTheta2,pXTheta3,pXTheta4,pXTheta5,pThetaSlope,pThetaOffset; 
    DataParameter pPhiSlope, pPhiOffset, pXPhi2, pXPhi4;
    DataParameter pXThetaDelta,pXTheta2Delta,pXTheta3Delta;

    /*
     * Scalers and monitors
     
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Scaler sFCLR;//number of FCLR's that went to ADC's
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors
    Monitor mFCLR;
*/
    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh, idRearHgh, idYFrnt, idYRear;
    //Histogram hDebug;
/*    
    int idFWbias, idRWbias, idCurrent;
    Histogram hFWbias, hRWbias, hCurrent;
*/
    public void initialize() throws Exception {
        vmeMap.setScalerInterval(3);
        //Focal Plane Detector      (slot, base address, channel, threshold channel)
        idCthd=vmeMap.eventParameter(2, ADC_BASE[0], 10, THRESHOLDS);
        idAnde=vmeMap.eventParameter(2, ADC_BASE[0], 1, THRESHOLDS);
        idScintR=vmeMap.eventParameter(2, ADC_BASE[0], 2, THRESHOLDS);
        idScintL=vmeMap.eventParameter(2, ADC_BASE[0], 3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(2, ADC_BASE[0], 4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(2, ADC_BASE[0], 5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(2, ADC_BASE[0], 6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(2, ADC_BASE[0], 7, THRESHOLDS);
        idYFrnt=vmeMap.eventParameter(2, ADC_BASE[0], 8, THRESHOLDS);
        idYRear=vmeMap.eventParameter(2, ADC_BASE[0], 9, THRESHOLDS);
        int idDummy=vmeMap.eventParameter(2, ADC_BASE[0], 0, 0);
	/*idFWbias = vmeMap.eventParameter(2, ADC_BASE[0], 12, THRESHOLDS);
	idRWbias = vmeMap.eventParameter(2, ADC_BASE[0], 13, THRESHOLDS);
	idCurrent = vmeMap.eventParameter(2, ADC_BASE[0], 14, THRESHOLDS);*/
        //idAndeADC2=vmeMap.eventParameter(3,ADC_BASE[2], 16, 0);	
        hCthd      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS,       "Cathode Raw ");
        hAnde      =new Histogram("Anode       ", HIST_1D_INT, ADC_CHANNELS,        "Anode Raw");
        hSntr1      =new Histogram("Scint1      ", HIST_1D_INT, ADC_CHANNELS,        "Scintillator PMT 1");
        hSntr2      =new Histogram("Scint2      ", HIST_1D_INT, ADC_CHANNELS,        "Scintillator PMT 2");
        hSntrSum    =new Histogram("ScintSum    ", HIST_1D_INT, ADC_CHANNELS,        "Scintillator Sum");
        hFrntPsn    =new Histogram("FrontPosn    ", HIST_1D_INT, ADC_CHANNELS,        "Front Wire Position");
         hFC        =new Histogram("FrontPosnCorr", HIST_1D_INT, ADC_CHANNELS,            "Front Wire Position - Corrected");
        hFCPID     =new Histogram("FrontPosnCorrPID", HIST_1D_INT, ADC_CHANNELS,            "Front Wire Position - Corrected, PID-gated");
       hRearPsn    =new Histogram("RearPosn     ", HIST_1D_INT, ADC_CHANNELS,        "Rear Wire Position");
        hFrntHgh    =new Histogram("FrontHeight   ", HIST_1D_INT, ADC_CHANNELS,        "Front Wire Pulse Height");
        hRearHgh    =new Histogram("RearHeight    ", HIST_1D_INT, ADC_CHANNELS,        "Rear Wire Pulse Height");
 /*       hRWbias    =new Histogram("RearBias    ", HIST_1D_INT, ADC_CHANNELS,        "Rear Wire Bias");
        hFWbias    =new Histogram("FrontBias    ", HIST_1D_INT, ADC_CHANNELS,        "Front Wire Bias");
        hCurrent    =new Histogram("Det Current", HIST_1D_INT, ADC_CHANNELS,
        "Detector Current");*/
        hFrntY = new Histogram("Front Y", HIST_1D_INT, ADC_CHANNELS,        "Y (vertical) Position at Front Wire");
        hRearY = new Histogram("Rear Y", HIST_1D_INT, ADC_CHANNELS,        "Y (vertical) Position at Rear Wire");
        hFrntPH      =new Histogram("FrontPvsHeight", HIST_2D_INT,        TWO_D_CHANNELS, "Pulse Height vs Front Position","Front Position",        "Pulse Height");
        hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,        TWO_D_CHANNELS, "Pulse Height vs Rear Position","Rear Position",        "Pulse Height");
        hTheta     =new Histogram("Theta     ", HIST_1D_INT, ADC_CHANNELS,            "Theta Raw ");
        hThetaPID  =new Histogram("ThetaPID  ", HIST_1D_INT, ADC_CHANNELS,            "Theta - PID gated ");
        hPhi       =new Histogram("Phi       ", HIST_1D_INT, ADC_CHANNELS,            "Phi Raw ");
        hFrntTheta   =new Histogram("FrntvsTheta   ", HIST_2D_INT, TWO_D_CHANNELS, "Theta vs Front Position",             "Front Position","Theta");
        hFrntThetaPID=new Histogram("FrntvsThetaPID", HIST_2D_INT, 1024, "Theta vs Front Position",             "Front Position","Theta");
        hFrntPhiPID=new Histogram("FrntvsPhiPID", HIST_2D_INT, TWO_D_CHANNELS, "Phi vs Front Position - PID gated",             "Front Position","Phi");
        hFCThetaPID=new Histogram("FCvsThetaPID", HIST_2D_INT, 1024, "Theta vs Corrected Front Position, PID gates",             "Corrected Front Position","Theta");
        hFCPhiPID=new Histogram("FCvsPhiPID", HIST_2D_INT, TWO_D_CHANNELS, "Phi vs Corrected Front Position, PID gates",             "Corrected Front Position","Phi");
        hThetaPhi    =new Histogram("ThetavsPhi   ", HIST_2D_INT, TWO_D_CHANNELS, "Phi vs Theta",             "Theta","Phi");
        hThetaPhiPID=new Histogram("ThetavsPhiPID ", HIST_2D_INT, TWO_D_CHANNELS, "Phi vs Theta - PID gated",             "Theta","Phi");
        hRearTheta   =new Histogram("RearvsTheta   ", HIST_2D_INT, TWO_D_CHANNELS, "Theta vs Rear Position",             "Rear Position","Theta");
        hYvsPsn = new Histogram("Y vs Position", HIST_2D_INT, TWO_D_CHANNELS,        "Front Y vs. Front Wire Position (X)", "Position", "Y");
        hYvsPsnGPID = new Histogram("YvsPosnPID", HIST_2D_INT, TWO_D_CHANNELS,        "Front Y vs. Front Wire Position (X) Gated on PID", "Position", "Y");
        hCthdAnde   =new Histogram("CathodeAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Cathode","Cathode","Anode");
        hSntrCthd   =new Histogram("ScintCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator","Scintillator","Cathode");
        hSntrAnde   =new Histogram("ScintAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Scintillator","Scintillator","Anode");
        hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position","Front Position","Cathode");
        hFrntAnde   =new Histogram("FrontAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Front Position","Front Position","Anode");
        hFrntSntr   =new Histogram("FrontScint ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position","Front Position","Scintillator");
        hFrntPRearP =new Histogram("FrontRear  ", HIST_2D_INT,  TWO_D_CHANNELS, "Rear Position vs Front Position","Front Position","Rear Position");
        hFrntYRearY =new Histogram("FrontY_RearY  ", HIST_2D_INT,  TWO_D_CHANNELS, "Rear Y Position vs Y Front Position","Front Y Position","Rear Y Position");
        hSntr1Sntr2 =new Histogram("Sntr1Sntr2  ", HIST_2D_INT, TWO_D_CHANNELS, "Scint 1 vs Scint 2", "Scint  1", "Scint 2");
        //ScintCathode Gated on other
 /*       hSntrCthdGFC=new Histogram("ScintCathodeGFC", HIST_2D_INT,  TWO_D_CHANNELS,         "Cathode vs Scintillator - FwCa gate", "Scintillator","Cathode");
        hSntrCthdGFS=new Histogram("ScintCathodeGFS", HIST_2D_INT,  TWO_D_CHANNELS,         "Cathode vs Scintillator - FwSc gate","Scintillator","Cathode");
        hSntrCthdGCA=new Histogram("ScintCathodeGCA", HIST_2D_INT,  TWO_D_CHANNELS,         "Cathode vs Scintillator - CaAn gate", "Scintillator","Cathode");
        hSntrCthdGS12=new Histogram("ScintCathodeGS12", HIST_2D_INT,  TWO_D_CHANNELS,         "Cathode vs Scintillator - Sc1Sc2 gate", "Scintillator","Cathode");
        //FrontCathode Gated on other
        hFrntCthdGSC=new Histogram("FrontCathodeGSC", HIST_2D_INT,  TWO_D_CHANNELS,         "Cathode vs Front Position - ScCa gate","Front Position","Cathode");
        hFrntCthdGFS=new Histogram("FrontCathodeGFS ", HIST_2D_INT,  TWO_D_CHANNELS,         "Cathode vs Front Position - FwSc gate ","Front Position","Cathode");
        hFrntCthdGCA=new Histogram("FrontCathodeGCA", HIST_2D_INT,  TWO_D_CHANNELS,         "Cathode vs Front Position - CaAn gate","Front Position","Cathode");  
        hFrntCthdGS12=new Histogram("FrontCathodeGS12", HIST_2D_INT,  TWO_D_CHANNELS,         "Cathode vs Front Position - Sc1Sc2 gate","Front Position","Cathode");  
        //FrontScint Gated on other
        hFrntSntrGSC=new Histogram("FrontScintGSC ", HIST_2D_INT,  TWO_D_CHANNELS,         "Scintillator vs Front Position - ScCa gate","Front Position", "Scintillator");
        hFrntSntrGFC=new Histogram("FrontScintGFC", HIST_2D_INT,  TWO_D_CHANNELS,         "Scintillator vs Front Position - FwCa gate","Front Position", "Scintillator");
        hFrntSntrGCA=new Histogram("FrontScintGCA ", HIST_2D_INT,  TWO_D_CHANNELS,         "Scintillator vs Front Position - CaAn gate","Front Position", "Scintillator");    
        hFrntSntrGS12=new Histogram("FrontScintGS12", HIST_2D_INT,  TWO_D_CHANNELS,         "Scintillator vs Front Position - Sc1Sc2 gate","Front Position", "Scintillator");    
        //CathodeAnode gated on other
        hCthdAndeGSC=new Histogram("CathodeAnodeGSC", HIST_2D_INT,  TWO_D_CHANNELS,         "Anode vs Cathode - ScCa Gate ",        "Cathode","Anode");
        hCthdAndeGFC=new Histogram("CathodeAnodeGFC", HIST_2D_INT,  TWO_D_CHANNELS,         "Anode vs Cathode - FwCa Gate ",        "Cathode","Anode");
        hCthdAndeGFS=new Histogram("CathodeAnodeGFS", HIST_2D_INT,  TWO_D_CHANNELS,         "Anode vs Cathode - FwSc Gate ",        "Cathode","Anode");        
        hCthdAndeGS12=new Histogram("CathodeAnodeGS12", HIST_2D_INT,  TWO_D_CHANNELS,         "Anode vs Cathode - Sc1Sc2 Gate ",        "Cathode","Anode");        
        //Scint1Scint2 Gated on other
        hSntr1Sntr2GSC =new Histogram("Sntr1Sntr2GSC", HIST_2D_INT, TWO_D_CHANNELS,         "Scint 2 vs Scint 1--Gated on ScCa", "Scint  1", "Scint 2");
        hSntr1Sntr2GFC =new Histogram("Sntr1Sntr2GFC", HIST_2D_INT, TWO_D_CHANNELS,          "Scint 2 vs Scint 1--Gated on FwCa", "Scint  1", "Scint 2");
        hSntr1Sntr2GFS =new Histogram("Sntr1Sntr2GFS", HIST_2D_INT, TWO_D_CHANNELS,          "Scint 2 vs Scint 1--Gated on FwSc", "Scint  1", "Scint 2");
        hSntr1Sntr2GCA =new Histogram("Sntr1Sntr2GCA", HIST_2D_INT, TWO_D_CHANNELS,          "Scint 2 vs Scint 1--Gated on CaAn", "Scint  1", "Scint 2");
 */       //gated on 3 gates
 //       hFrntGCSF   =new Histogram("FrontGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc gates");
 //       hRearGCSF   =new Histogram(   "RearGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc gates");
        hFrntRearGCSF=new Histogram("FRGateCSF  ",HIST_2D_INT, TWO_D_CHANNELS,"Front vs. Rear - ScCa, FwCa, FwSc gates");
        //gated on 4 gates
        hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
        hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
        hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");
 //new       hFrntRearGCSF=new Histogram("FRGateCSF  ",HIST_2D_INT, TWO_D_CHANNELS,"Front vs. Rear - ScCa, FwCa, FwSc gates");

        // gates 1d
//        gCthd   =new Gate("Counts", hCthd);
//        gPeak   =new Gate("Peak", hFrntGAll);
        
        //gates  2d
        gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
        gCthdAnde   =new Gate("Ca-An", hCthdAnde);      //gate on Anode Cathode
        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        gXY         =new Gate("XY", hYvsPsn);           // gate on x and y acceptance
        hYvsPsnGPID.addGate(gXY);
        gSntr1Sntr2 =new Gate("S1-S2", hSntr1Sntr2);    // gate on Scint1 vs Scint2
        gFYRY       =new Gate("FY-RY", hFrntYRearY);    // gate on y1 vs y2
         gFrntTheta  =new Gate("Front-Theta", hFrntThetaPID);
/*       hFrntSntrGSC.addGate(gFrntSntr);
        hFrntCthdGSC.addGate(gFrntCthd);
        hSntrCthdGFC.addGate(gSntrCthd);
        hFrntSntrGFC.addGate(gFrntSntr);
        hSntrCthdGFS.addGate(gSntrCthd);
        hFrntCthdGFS.addGate(gFrntCthd);
        hFrntRearGCSF.addGate(gFrntRear);
        hCthdAndeGFC.addGate(gCthdAnde);
        hCthdAndeGFS.addGate(gCthdAnde);
        hCthdAndeGSC.addGate(gCthdAnde);
        hFrntSntrGCA.addGate(gFrntSntr);
        hFrntCthdGCA.addGate(gFrntCthd);
        hSntrCthdGCA.addGate(gSntrCthd);
        hSntr1Sntr2GSC.addGate(gSntr1Sntr2);
        hSntr1Sntr2GFC.addGate(gSntr1Sntr2);
        hSntr1Sntr2GFS.addGate(gSntr1Sntr2);
        hSntr1Sntr2GCA.addGate(gSntr1Sntr2);
        hFrntSntrGS12.addGate(gFrntSntr);
        hFrntCthdGS12.addGate(gFrntCthd);
        hSntrCthdGS12.addGate(gSntrCthd);
        hCthdAndeGS12.addGate(gCthdAnde);*/
        //scalers
/*        sBic      =new Scaler("BIC",0);
        sClck      =new Scaler("Clock",1);
        sEvntRaw    =new Scaler("Event Raw", 2);
        sEvntAccpt  =new Scaler("Event Accept",3);
        sScint    =new Scaler("Scintillator", 4);
        sCathode  =new Scaler("Cathode",5);
        sFCLR = new Scaler("FCLR",6);
        
        //monitors
        mBeam=new Monitor("Beam ",sBic);
        mClck=new Monitor("Clock",sClck);
        mEvntRaw=new Monitor("Raw Events",sEvntRaw);
        mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
        mScint=new Monitor("Scintillator",sScint);
        mCathode=new Monitor("Cathode",sCathode);
        mFCLR = new Monitor("FCLR",sFCLR);
        Monitor mDeadTime=new Monitor(DEAD_TIME, this);
        Monitor mTrueDeadTime = new Monitor(TRUE_DEAD_TIME, this);*/
       //Parameters
        pXSlope=new DataParameter("XSlope      ");
        pXOffset=new DataParameter("XOffset    ");
        pThetaSlope=new DataParameter("ThetaSlope");
        pThetaOffset=new DataParameter("ThetaOffset");
        pPhiSlope=new DataParameter("PhiSlope");
        pPhiOffset=new DataParameter("PhiOffset");
        pRhoSlope=new DataParameter("RhoSlope");
        pRhoOffset=new DataParameter("RhoOffset");
        pXTheta=new DataParameter("(X|Theta)    ");
        pThetaX=new DataParameter("(Theta|X)    ");
        pThetaX3=new DataParameter("(Theta|X^3)    ");
        pXTheta2=new DataParameter("(X|Theta^2)  ");
        pXTheta3=new DataParameter("(X|Theta^3) ");
        pXTheta4=new DataParameter("(X|Theta^4) ");
        pXTheta5=new DataParameter("(X|Theta^5) ");
        pXPhi2=new DataParameter("(X|Phi^2)    ");
        pXPhi4=new DataParameter("(X|Phi^4)   ");
        pXThetaDelta=new DataParameter("(X|ThetaDelta)");
        pXTheta2Delta=new DataParameter("(X|Theta2Delta)");
        pXTheta3Delta=new DataParameter("(X|Theta3Delta)");
    }

    public void sort(int [] dataEvent) throws Exception {
        //int eTimeRawGA;//temp holder for non-TOF adjusted gain-matched time
        //System.out.println("Sorting "+(event++));
        //unpack data into convenient names
        int eCthd   =dataEvent[idCthd];
        int eAnde   =dataEvent[idAnde];
        int eSntr1  =dataEvent[idScintR];
        int eSntr2  =dataEvent[idScintL];
        int eFPsn   =dataEvent[idFrntPsn];
        int eRPsn   =dataEvent[idRearPsn];
        int eFHgh   =dataEvent[idFrntHgh];
        int eRHgh   =dataEvent[idRearHgh];
        int eYF = dataEvent[idYFrnt];
        int eYR = dataEvent[idYRear];
        //int eCthdAnde = (eCthd + eAnde)/2;

        //int eAndeADC2 = dataEvent[idAndeADC2];
        //proper way to add for 2 phototubes at the ends of scintillating rod
        //see Knoll
        int eSntr=(int)Math.round(Math.sqrt(eSntr1*eSntr2));
        //        int eSntr = (eSntr1+eSntr2)/2;
        // for this expt. left scint PM connection was dead:
        //int eSntr = eSntr1;//scint=right scint
        int ecFPsn=eFPsn>>TWO_D_FACTOR;
        int ecRPsn=eRPsn>>TWO_D_FACTOR;
        int ecFHgh=eFHgh>>TWO_D_FACTOR;
        int ecRHgh=eRHgh>>TWO_D_FACTOR;
        int ecSntr=eSntr>>TWO_D_FACTOR;
        int ecCthd=eCthd>>TWO_D_FACTOR;
        int ecAnde=eAnde>>TWO_D_FACTOR;
        int ecSntr1 = eSntr1>>TWO_D_FACTOR;
        int ecSntr2 = eSntr2>>TWO_D_FACTOR;
 //new       int eTheta = eFPsn-eRPsn;
 //new       int ecTheta = eTheta>>TWO_D_FACTOR;
         //  BEGIN PseudoParameter Section...
        
        //  Define calibration parameters first...
        
        //get DataParameter values
        double XSlope=pXSlope.getValue();           // in mm/channel at focal plane
        double XOffset=pXOffset.getValue();         // centroid of x distribution (in channels) - 2048
        double ThetaSlope=pThetaSlope.getValue();   // in mrad/channel at entrance
        double ThetaOffset=pThetaOffset.getValue(); // centroid of theta distribution (in channels) - 2048
        double PhiSlope=pPhiSlope.getValue();       // in mrad/channel at entrance
        double PhiOffset=pPhiOffset.getValue();     // centroid of phi distribution (in channels) - 2048
        double RhoSlope=pRhoSlope.getValue();       // slope of rho (cm/channel)
        double RhoOffset=pRhoOffset.getValue();     // offset of rho (channel)
        double XTheta=pXTheta.getValue();       //use this to correct the focus for different particle groups with different kinematics
        double ThetaX=pThetaX.getValue();
        double ThetaX3=pThetaX3.getValue();
        double XTheta2=pXTheta2.getValue();     //use this to correct the  (x|theta^2) split-pole aberration
        double XTheta3=pXTheta3.getValue();     //use this to correct the dominant (x|theta^3) split-pole aberration
        double XTheta4=pXTheta4.getValue();     //use this to correct the  (x|theta^4) split-pole aberration
        double XTheta5=pXTheta5.getValue();     //use this to correct the  (x|theta^5) split-pole aberration
        double XPhi2 = pXPhi2.getValue();           // dominant phi aberration
        double XPhi4 = pXPhi4.getValue();
        double XThetaDelta =  pXThetaDelta.getValue();       // corrects different first-order focus conditions along focal plane
        double XTheta2Delta = pXTheta2Delta.getValue();     // corrects momentum-dependence (x|theta2) term
        double XTheta3Delta = pXTheta3Delta.getValue();     // corrects momentum-dependence of (x|theta3) term
        
        //  create pseudo parameters and assign histograms

        int     eCthdAnde = (eCthd + eAnde)/2;
        int     iTheta=0, iPhi=0;
        int     iFC=0;
        double  Theta=0,Phi=0,FC=0,Xfp;
        double  Delta=0;                 // delta = (p-p0)/p0; Enge claims constant dispersion (dx/drho=1.96)
        double  Rho=0;
        double  RHO0 = 80.0;             // central rho in cm
        double  fp0 = 610,  xt10 = -0.139, xt20=-0.00156,xt30=-1.83e-6,xt40=2.62e-8,xt50=1.6e-12;
        double  fp1 = 1184, xt11 = 0.0507, xt21=-0.001188,xt31=-1.108e-5,xt41=1.672e-8,xt51=8.183e-11;
        double  fp2 = 1787, xt12 = 0.00505, xt22=-0.00102,xt32=-6.037e-6,xt42=1.62e-8,xt52=-7.04e-11;
        double  fp3 = 2119, xt13 = 0.0361, xt23=-0.000593,xt33=-4.16e-6,xt43=6.038e-9,xt53=-1.36e-10;
        double  fp4 = 2670, xt14=0.002488,xt24=9.13E-4,xt34=-5.014e-6,xt44=-1.254e-7,xt54=-9.09e-10;
        
        if (eRPsn > 0 && eFPsn > 0) {   //check for valid position signals
            iTheta = eRPsn - eFPsn + 2048 - (int)ThetaOffset;  // add 2048 to put 0 in middle of the spectrum
            Theta = (eRPsn - eFPsn - ThetaOffset)*ThetaSlope;
            Xfp = (eFPsn - XOffset)*XSlope;
            Theta = Theta + ThetaX*Xfp + ThetaX3*Xfp*Xfp*Xfp;
            Rho=(eFPsn - RhoOffset) * RhoSlope;
            Delta = (Rho - RHO0)/RHO0;
// New section to correct all of focal plane, based on 5.91, 4.446, 2.784, and 1.796 MeV states.
// These are located at x=610, x=?, x=1787, x=2119.
// I will do it this way:  I have the coefficients from each of these states, and I will do a linear
// interpolation between these states, using the eFPsn and Theta parameters.
// First decide where on the focal plane we are...
// if we are less than position 620, then use the coefficients for the 5.910 MeV state.
            if (eFPsn < fp0) {
                XTheta  = xt10;
                XTheta2 = xt20;
                XTheta3 = xt30;
                XTheta4 = xt40;
                XTheta5 = xt50;
            }
            if (eFPsn > fp0-1 && eFPsn < fp1) {
                XTheta  = xt10 + (xt11-xt10)/(fp1-fp0)*(eFPsn-fp0);
                XTheta2 = xt20 + (xt21-xt20)/(fp1-fp0)*(eFPsn-fp0);
                XTheta3 = xt30 + (xt31-xt30)/(fp1-fp0)*(eFPsn-fp0);
                XTheta4 = xt40 + (xt41-xt40)/(fp1-fp0)*(eFPsn-fp0);
                XTheta5 = xt50 + (xt51-xt50)/(fp1-fp0)*(eFPsn-fp0);
            }
            if (eFPsn > fp1-1 && eFPsn < fp2) {
                XTheta  = xt11 + (xt12-xt11)/(fp2-fp1)*(eFPsn-fp1);
                XTheta2 = xt21 + (xt22-xt21)/(fp2-fp1)*(eFPsn-fp1);
                XTheta3 = xt31 + (xt32-xt31)/(fp2-fp1)*(eFPsn-fp1);
                XTheta4 = xt41 + (xt42-xt41)/(fp2-fp1)*(eFPsn-fp1);
                XTheta5 = xt51 + (xt52-xt51)/(fp2-fp1)*(eFPsn-fp1);
            }
            if (eFPsn > fp2-1 && eFPsn < fp3) {
                XTheta  = xt12 + (xt13-xt12)/(fp3-fp2)*(eFPsn-fp2);
                XTheta2 = xt22 + (xt23-xt22)/(fp3-fp2)*(eFPsn-fp2);
                XTheta3 = xt32 + (xt33-xt32)/(fp3-fp2)*(eFPsn-fp2);
                XTheta4 = xt42 + (xt43-xt42)/(fp3-fp2)*(eFPsn-fp2);
                XTheta5 = xt52 + (xt53-xt52)/(fp3-fp2)*(eFPsn-fp2);            
            }
            if (eFPsn > fp3-1 && eFPsn < fp4) {
                XTheta  = xt13 + (xt14-xt13)/(fp4-fp3)*(eFPsn-fp3);
                XTheta2 = xt23 + (xt24-xt23)/(fp4-fp3)*(eFPsn-fp3);
                XTheta3 = xt33 + (xt34-xt33)/(fp4-fp3)*(eFPsn-fp3);
                XTheta4 = xt43 + (xt44-xt43)/(fp4-fp3)*(eFPsn-fp3);
                XTheta5 = xt53 + (xt54-xt53)/(fp4-fp3)*(eFPsn-fp3);            
            }
            if (eFPsn > fp4-1) {
                XTheta  = xt14;
                XTheta2 = xt24;
                XTheta3 = xt34;
                XTheta4 = xt44;
                XTheta5 = xt54;            
            }
            FC = eFPsn + (XThetaDelta*Theta + XTheta2Delta*Theta*Theta + XTheta3Delta*Theta*Theta*Theta)*Delta;
            FC = FC + XTheta*Theta + XTheta2*Theta*Theta + XTheta3*(Theta*Theta*Theta)+XTheta4*(Theta*Theta*Theta*Theta) + XTheta5*(Theta*Theta*Theta*Theta*Theta);
            }       
        if (eYF > 0 && eYR > 0) {       //check for valid position signals
            iPhi = eYR - eYF + 2048;        // add 2048 to put 0 in middle of the spectrum
            Phi = (eYR - eYF - PhiOffset)*PhiSlope;
//            FC = FC + XPhi2*Phi*Phi + XPhi4*Phi*Phi*Phi*Phi;      
//            iFC = (int)Math.round(FC);
            }     
        iFC = (int)Math.round(FC);
        //  END PseudoParameter Section...
       
        // singles spectra
        hCthd.inc(eCthd);
        hAnde.inc(eAnde);
 //       hSntr1.inc(eSntr1);
  //      hSntr2.inc(eSntr2);
        hSntrSum.inc(eSntr);
        hFrntPsn.inc(eFPsn);
        hRearPsn.inc(eRPsn);
        hFrntHgh.inc(eFHgh);
        hRearHgh.inc(eRHgh);
        hFrntY.inc(eYF);
        hRearY.inc(eYR);
        hTheta.inc(iTheta);
        hPhi.inc(iPhi);
        hFC.inc(iFC);
        hYvsPsn.inc(ecFPsn,eYF>>TWO_D_FACTOR);
        hFrntPH.inc(ecFPsn,ecFHgh);
        hRearPH.inc(ecRPsn,ecRHgh);
        hSntrCthd.inc(ecSntr,ecCthd);
        hFrntCthd.inc(ecFPsn,ecCthd);
        hFrntTheta.inc(ecFPsn,iTheta>>TWO_D_FACTOR);
        hRearTheta.inc(ecRPsn,iTheta>>TWO_D_FACTOR);
        hFrntAnde.inc(ecFPsn,ecAnde);
        hFrntSntr.inc(ecFPsn,ecSntr);
        hFrntPRearP.inc(ecFPsn,ecRPsn);
        hCthdAnde.inc(ecCthd,ecAnde);
        hSntrAnde.inc(ecSntr,eAnde>>TWO_D_FACTOR);
        hSntr1Sntr2.inc(ecSntr1, ecSntr2);
        hThetaPhi.inc(iTheta>>TWO_D_FACTOR,iPhi>>TWO_D_FACTOR);
/*
	hRWbias.inc(dataEvent[idRWbias]);
	hFWbias.inc(dataEvent[idFWbias]);
	hCurrent.inc(dataEvent[idCurrent]);	   */     

        boolean bSC = gSntrCthd.inGate(ecSntr,ecCthd);
        boolean bCA = gCthdAnde.inGate(ecCthd,ecAnde);
        boolean bFC = gFrntCthd.inGate(ecFPsn,ecCthd);
        boolean bFS = gFrntSntr.inGate(ecFPsn,ecSntr);
        boolean bS1S2 = gSntr1Sntr2.inGate(eSntr1>>TWO_D_FACTOR,eSntr2>>TWO_D_FACTOR);
        boolean bXY = gXY.inGate(ecFPsn,eYF>>TWO_D_FACTOR);
        boolean bPID = bSC && bFC && bFS && bCA && bS1S2;
//        boolean bGood = bPID && bXY && gFrntRear.inGate(ecFPsn,ecRPsn) && 
        gFYRY.inGate(eYF>>TWO_D_FACTOR,eYR>>TWO_D_FACTOR);
        boolean bXacc  = gFrntRear.inGate(ecFPsn,ecRPsn);
        boolean bYacc  = gFYRY.inGate(eYF>>TWO_D_FACTOR,eYR>>TWO_D_FACTOR);
        boolean bGoodX = bPID && gFrntTheta.inGate(eFPsn>>2,iTheta>>2);
  //      boolean bState = bGood && gPeak.inGate(eFPsn);

 /*       if (bSC) {// gate on Scintillator vs Cathode
            hFrntSntrGSC.inc(ecFPsn,ecSntr);
            hFrntCthdGSC.inc(ecFPsn,ecCthd);
            hCthdAndeGSC.inc(ecCthd,ecAnde);
            hSntr1Sntr2GSC.inc(ecSntr1, ecSntr2);
        }
        if (bFC) {// gate on Front Wire Position vs Cathode
            hSntrCthdGFC.inc(ecSntr,ecCthd);
            hFrntSntrGFC.inc(ecFPsn,ecSntr);
            hCthdAndeGFC.inc(ecCthd,ecAnde);
            hSntr1Sntr2GFC.inc(ecSntr1, ecSntr2);
        }
        if (bFS){// gate on Front Wire Position vs Scintillator
            hSntrCthdGFS.inc(ecSntr,ecCthd);
            hFrntCthdGFS.inc(ecFPsn,ecCthd);
            hCthdAndeGFS.inc(ecCthd,ecAnde);
            hSntr1Sntr2GFS.inc(ecSntr1, ecSntr2);
        }
        if (bCA){//gated on Anode vs. Cathode
            hFrntSntrGCA.inc(ecFPsn,ecSntr);
            hFrntCthdGCA.inc(ecFPsn,ecCthd);
            hSntrCthdGCA.inc(ecSntr,ecCthd);
            hSntr1Sntr2GCA.inc(ecSntr1, ecSntr2);
        }
        if (bS1S2) {//gated on Scint 2 vs. Scint 1
            hFrntSntrGS12.inc(ecFPsn,ecSntr);
            hFrntCthdGS12.inc(ecFPsn,ecCthd);
            hCthdAndeGS12.inc(ecCthd,ecAnde);
            hSntrCthdGS12.inc(ecSntr,ecCthd);
        }*/
        if (bPID) {// gated on all 3 gate above
            //writeEvent(dataEvent);
//            hFrntGCSF.inc(eFPsn);
 //           hRearGCSF.inc(eRPsn);
            hFrntRearGCSF.inc(ecFPsn,ecRPsn);
            hFrntYRearY.inc(eYF>>TWO_D_FACTOR, eYR>>TWO_D_FACTOR);
            hYvsPsnGPID.inc(ecFPsn, eYF>>TWO_D_FACTOR);
            hThetaPID.inc(iTheta);
            hFrntThetaPID.inc(eFPsn>>2,iTheta>>2);
            hFCThetaPID.inc(iFC>>2,iTheta>>2);
             hFrntPhiPID.inc(ecFPsn, iPhi>>TWO_D_FACTOR);
            hFCPhiPID.inc(iFC>>TWO_D_FACTOR, iPhi>>TWO_D_FACTOR);
            hThetaPhiPID.inc(iPhi>>TWO_D_FACTOR,iTheta>>TWO_D_FACTOR);
           if(bGoodX) {
                writeEvent(dataEvent);
                hFrntGAll.inc(eFPsn);
                hRearGAll.inc(eRPsn);
                 hFCPID.inc(iFC);
               hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
                hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
 //new           	hFrntTheta.inc(ecFPsn,ecTheta);
            }
        }
    }
    /** Called so the dead time can be calculated.
     * @param name name of monitor to calculate
     * @return floating point value of monitor
     
    public double monitor(String name){
        if (name.equals(DEAD_TIME)){
            double acceptRate = mEvntAccept.getValue();
            double rawRate = mEvntRaw.getValue();
            if (acceptRate > 0.0  && acceptRate <= rawRate){
                return 100.0 * (1.0 - acceptRate/rawRate);
            } else {
                return 0.0;
            }
        } /*else if (name.equals(ACQUIRED_RATE)) {
            double fclrRate = mFCLR.getValue();
            double acceptRate = mEvntAccept.getValue();
            double rawRate = mEvntRaw.getValue();
            double acq = (1-fclrRate/rawRate)*
            if (acceptRate > 0.0  && acceptRate <= rawRate){
                return 100.0 * (1.0 - acceptRate/rawRate);
            } else {
                return 0.0;
            }
         } else {
            return 50.0;
        }
    }*/
}
