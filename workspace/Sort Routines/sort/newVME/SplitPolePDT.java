/*
 */
package sort.newVME;
import java.io.*;
import jam.data.*;
import jam.sort.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;

/** Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>, which
 * was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right way; also 
 * added gate to cathAnde
 *
 * @author Dale Visser
 * @since 26 July 2001
 */
public class SplitPolePDT extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 128;//ADC lower threshold in channels
    static final int LAST_ADC_BIN = 3840;

    //names
    static final String DEAD_TIME="Dead Time %";
    static final String TRUE_DEAD_TIME = "True Dead Time %";

    //histogramming constants
    final int ADC_CHANNELS = 4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed histogram
    final int TWO_D_CHANNELS = 256; //number of channels per dimension in 2-d histograms
    //bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    
    // ungated spectra
    Histogram hCthd,hAnde,hSntr1,hSntr2,hSntrSum, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;     // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hSntrCthd, hFrntCthd, hFrntAnde, hFrntSntr, hFrntPRearP;
    Histogram hSntrAnde;
    Histogram hFrntY, hRearY, hYvsPsn, hYvsPsnGPID;
    Histogram hSntr1Sntr2;
    Histogram hFrntYRearY;

    //gated spectra
    Histogram hFrntSntrGSC, hFrntCthdGSC;//gate by scintillator cathode
    Histogram hSntrCthdGFC, hFrntSntrGFC;//gate by Front wire Cathode
    Histogram hSntrCthdGFS, hFrntCthdGFS;//gate by Front wire Scintillator
    Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hFrntGAll_p, hFrntGAll_d, hFrntGAll_t;
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed & time
    Histogram hcFrntGTime, hFrntGTime;//front and rear wire gated on All compressed & time
    Histogram hCthdAndeGFS, hCthdAndeGFC, hCthdAndeGSC;
    Histogram hFrntSntrGCA, hFrntCthdGCA, hSntrCthdGCA;
    Histogram hSntr1Sntr2GSC, hSntr1Sntr2GFC, hSntr1Sntr2GFS, hSntr1Sntr2GCA;
    Histogram hSntrCthdGS12, hFrntCthdGS12, hFrntSntrGS12, hCthdAndeGS12;
    
    /*
     * 1D gates
     */
    Gate  gCthd, gPeak; 
    /*
     * 2D gates
     */
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear, gCthdAnde, gXY, gSntr1Sntr2, gFYRY;
    Gate gSntrCthd_p, gSntrCthd_d, gSntrCthd_t;
    Gate gFrntSntr_p, gFrntSntr_d, gFrntSntr_t;
    Gate gFrntCthd_p, gFrntCthd_d, gFrntCthd_t;
    
    /*
     * Scalers and monitors
     */
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Scaler sFCLR;//number of FCLR's that went to ADC's
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors
    Monitor mFCLR;

	
    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh,
    idRearHgh, idYFrnt, idYRear;
    //Histogram hDebug;
    
    int idFWbias, idRWbias, idBCIRange;
    Histogram hFWbias, hRWbias, hBCIRange;

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
	idFWbias = vmeMap.eventParameter(2, ADC_BASE[0], 12, THRESHOLDS);
	idRWbias = vmeMap.eventParameter(2, ADC_BASE[0], 13, THRESHOLDS);
	idBCIRange = vmeMap.eventParameter(2, ADC_BASE[0], 14, 16);
        //idAndeADC2=vmeMap.eventParameter(3,ADC_BASE[2], 16, 0);	
        hCthd      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS,
        "Cathode Raw ");
        hAnde      =new Histogram("Anode       ", HIST_1D_INT, ADC_CHANNELS,
        "Anode Raw");
        hSntr1      =new Histogram("Scint1      ", HIST_1D_INT, ADC_CHANNELS,
        "Scintillator PMT 1");
        hSntr2      =new Histogram("Scint2      ", HIST_1D_INT, ADC_CHANNELS,
        "Scintillator PMT 2");
        hSntrSum    =new Histogram("ScintSum    ", HIST_1D_INT, ADC_CHANNELS,
        "Scintillator Sum");
        hFrntPsn    =new Histogram("FrontPosn    ", HIST_1D_INT, ADC_CHANNELS,
        "Front Wire Position");
        hRearPsn    =new Histogram("RearPosn     ", HIST_1D_INT, ADC_CHANNELS,
        "Rear Wire Position");
        hFrntHgh    =new Histogram("FrontHeight   ", HIST_1D_INT, ADC_CHANNELS,
        "Front Wire Pulse Height");
        hRearHgh    =new Histogram("RearHeight    ", HIST_1D_INT, ADC_CHANNELS,
        "Rear Wire Pulse Height");
        hRWbias    =new Histogram("RearBias    ", HIST_1D_INT, ADC_CHANNELS,
        "Rear Wire Bias");
        hFWbias    =new Histogram("FrontBias    ", HIST_1D_INT, ADC_CHANNELS,
        "Front Wire Bias");
        hBCIRange    =new Histogram("BCIRange", HIST_1D_INT, ADC_CHANNELS,
        "BCI Full Scale Range - about 330 mV/position");
        hFrntY = new Histogram("Front Y", HIST_1D_INT, ADC_CHANNELS,
        "Y (vertical) Position at Front Wire");
        hRearY = new Histogram("Rear Y", HIST_1D_INT, ADC_CHANNELS,
        "Y (vertical) Position at Rear Wire");
        hFrntPH      =new Histogram("FrontPvsHeight", HIST_2D_INT,
        TWO_D_CHANNELS, "Pulse Height vs Front Position","Front Position",
        "Pulse Height");
        hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,
        TWO_D_CHANNELS, "Pulse Height vs Rear Position","Rear Position",
        "Pulse Height");
        hYvsPsn = new Histogram("Y vs Position", HIST_2D_INT, TWO_D_CHANNELS,
        "Front Y vs. Front Wire Position (X)", "Position", "Y");
        hYvsPsnGPID = new Histogram("YvsPosnPID", HIST_2D_INT, TWO_D_CHANNELS,
        "Front Y vs. Front Wire Position (X) Gated on PID", "Position", "Y");
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
        hSntrCthdGFC=new Histogram("ScintCathodeGFC", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Scintillator - FwCa gate", "Scintillator","Cathode");
        hSntrCthdGFS=new Histogram("ScintCathodeGFS", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Scintillator - FwSc gate","Scintillator","Cathode");
        hSntrCthdGCA=new Histogram("ScintCathodeGCA", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Scintillator - CaAn gate", "Scintillator","Cathode");
        hSntrCthdGS12=new Histogram("ScintCathodeGS12", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Scintillator - Sc1Sc2 gate", "Scintillator","Cathode");
        //FrontCathode Gated on other
        hFrntCthdGSC=new Histogram("FrontCathodeGSC", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Front Position - ScCa gate","Front Position","Cathode");
        hFrntCthdGFS=new Histogram("FrontCathodeGFS ", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Front Position - FwSc gate ","Front Position","Cathode");
        hFrntCthdGCA=new Histogram("FrontCathodeGCA", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Front Position - CaAn gate","Front Position","Cathode");  
        hFrntCthdGS12=new Histogram("FrontCathodeGS12", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Front Position - Sc1Sc2 gate","Front Position","Cathode");  
        //FrontScint Gated on other
        hFrntSntrGSC=new Histogram("FrontScintGSC ", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Scintillator vs Front Position - ScCa gate","Front Position", "Scintillator");
        hFrntSntrGFC=new Histogram("FrontScintGFC", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Scintillator vs Front Position - FwCa gate","Front Position", "Scintillator");
        hFrntSntrGCA=new Histogram("FrontScintGCA ", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Scintillator vs Front Position - CaAn gate","Front Position", "Scintillator");    
        hFrntSntrGS12=new Histogram("FrontScintGS12", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Scintillator vs Front Position - Sc1Sc2 gate","Front Position", "Scintillator");    
        //CathodeAnode gated on other
        hCthdAndeGSC=new Histogram("CathodeAnodeGSC", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Anode vs Cathode - ScCa Gate ",
        "Cathode","Anode");
        hCthdAndeGFC=new Histogram("CathodeAnodeGFC", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Anode vs Cathode - FwCa Gate ",
        "Cathode","Anode");
        hCthdAndeGFS=new Histogram("CathodeAnodeGFS", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Anode vs Cathode - FwSc Gate ",
        "Cathode","Anode");        
        hCthdAndeGS12=new Histogram("CathodeAnodeGS12", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Anode vs Cathode - Sc1Sc2 Gate ",
        "Cathode","Anode");        
        //Scint1Scint2 Gated on other
        hSntr1Sntr2GSC =new Histogram("Sntr1Sntr2GSC", HIST_2D_INT, TWO_D_CHANNELS, 
        "Scint 2 vs Scint 1--Gated on ScCa", "Scint  1", "Scint 2");
        hSntr1Sntr2GFC =new Histogram("Sntr1Sntr2GFC", HIST_2D_INT, TWO_D_CHANNELS, 
         "Scint 2 vs Scint 1--Gated on FwCa", "Scint  1", "Scint 2");
        hSntr1Sntr2GFS =new Histogram("Sntr1Sntr2GFS", HIST_2D_INT, TWO_D_CHANNELS, 
         "Scint 2 vs Scint 1--Gated on FwSc", "Scint  1", "Scint 2");
        hSntr1Sntr2GCA =new Histogram("Sntr1Sntr2GCA", HIST_2D_INT, TWO_D_CHANNELS, 
         "Scint 2 vs Scint 1--Gated on CaAn", "Scint  1", "Scint 2");
        //gated on 3 gates
        hFrntGCSF   =new Histogram("FrontGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc gates");
        hRearGCSF   =new Histogram(   "RearGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc gates");
        hFrntRearGCSF=new Histogram("FRGateCSF  ",HIST_2D_INT, TWO_D_CHANNELS,"Front vs. Rear - ScCa, FwCa, FwSc gates");
        //gated on 4 gates
        hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
        hFrntGAll_p   =new Histogram("FrontGAll_p    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - Proton gates");
        hFrntGAll_d   =new Histogram("FrontGAll_d    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - Deuteron gates");
        hFrntGAll_t   =new Histogram("FrontGAll_t    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - Triton gates");
        hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
        hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");

        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gPeak   =new Gate("Peak", hFrntGAll);
        
        //gates  2d
        gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode        
        gSntrCthd_p   =new Gate("Ca-Sc p", hSntrCthd);      //gate on Scintillator Cathode
        gSntrCthd_d   =new Gate("Ca-Sc d", hSntrCthd);      //gate on Scintillator Cathode
        gSntrCthd_t   =new Gate("Ca-Sc t", hSntrCthd);      //gate on Scintillator Cathode
        gCthdAnde   =new Gate("Ca-An", hCthdAnde);      //gate on Anode Cathode
        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
        gFrntSntr_p   =new Gate("Fw-Sc p", hFrntSntr);          //gate on Front Scintillator
        gFrntSntr_d   =new Gate("Fw-Sc d", hFrntSntr);          //gate on Front Scintillator
        gFrntSntr_t   =new Gate("Fw-Sc t", hFrntSntr);          //gate on Front Scintillator
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
        gFrntCthd_p   =new Gate("Fw-Ca p", hFrntCthd);      //gate on Front Cathode
        gFrntCthd_d   =new Gate("Fw-Ca d", hFrntCthd);      //gate on Front Cathode
        gFrntCthd_t   =new Gate("Fw-Ca t", hFrntCthd);      //gate on Front Cathode
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        gXY         =new Gate("XY", hYvsPsn);           // gate on x and y acceptance
        hYvsPsnGPID.addGate(gXY);
        gSntr1Sntr2 =new Gate("S1-S2", hSntr1Sntr2);    // gate on Scint1 vs Scint2
        gFYRY       =new Gate("FY-RY", hFrntYRearY);    // gate on y1 vs y2
        hFrntSntrGSC.addGate(gFrntSntr);
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
        hCthdAndeGS12.addGate(gCthdAnde);
        //scalers
        sBic      =new Scaler("BIC",0);
        sClck      =new Scaler("Clock",1);
        sEvntRaw    =new Scaler("Event Raw", 2);
        sEvntAccpt  =new Scaler("Event Accept",3);
        sScint    =new Scaler("Scintillator", 4);
        sCathode  =new Scaler("Cathode",5);
        sFCLR = new Scaler("FCLR",6);
        
       //Parameters
              
        //monitors
        mBeam=new Monitor("Beam ",sBic);
        mClck=new Monitor("Clock",sClck);
        mEvntRaw=new Monitor("Raw Events",sEvntRaw);
        mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
        mScint=new Monitor("Scintillator",sScint);
        mCathode=new Monitor("Cathode",sCathode);
        mFCLR = new Monitor("FCLR",sFCLR);
        Monitor mDeadTime=new Monitor(DEAD_TIME, this);
        Monitor mTrueDeadTime = new Monitor(TRUE_DEAD_TIME, this);
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
        int iTheta = eRPsn-eFPsn-140;
        double Theta = iTheta;
        double Front=eFPsn;
        int iFCp,iFCd,iFCt,iFC;

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
        
        // singles spectra
        hCthd.inc(eCthd);
        hAnde.inc(eAnde);
        hSntr1.inc(eSntr1);
        hSntr2.inc(eSntr2);
        hSntrSum.inc(eSntr);
        hFrntPsn.inc(eFPsn);
        hRearPsn.inc(eRPsn);
        hFrntHgh.inc(eFHgh);
        hRearHgh.inc(eRHgh);
        hFrntY.inc(eYF);
        hRearY.inc(eYR);
        hYvsPsn.inc(ecFPsn,eYF>>(TWO_D_FACTOR));
        hSntrCthd.inc(ecSntr,ecCthd);
        hFrntCthd.inc(ecFPsn,ecCthd);
        hFrntAnde.inc(ecFPsn,ecAnde);
        hFrntSntr.inc(ecFPsn,ecSntr);
        hFrntPRearP.inc(ecFPsn,ecRPsn);
        hCthdAnde.inc(ecCthd,ecAnde);
        hSntrAnde.inc(ecSntr,eAnde>>TWO_D_FACTOR);
        hSntr1Sntr2.inc(ecSntr1, ecSntr2);

	hRWbias.inc(dataEvent[idRWbias]);
	hFWbias.inc(dataEvent[idFWbias]);
	hBCIRange.inc(dataEvent[idBCIRange]);	        

        boolean bSC = gSntrCthd.inGate(ecSntr,ecCthd);
        boolean bCA = gCthdAnde.inGate(ecCthd,ecAnde);
        boolean bFC = gFrntCthd.inGate(ecFPsn,ecCthd);
        boolean bFS = gFrntSntr.inGate(ecFPsn,ecSntr);
        boolean bS1S2 = gSntr1Sntr2.inGate(eSntr1>>TWO_D_FACTOR,eSntr2>>TWO_D_FACTOR);
        boolean bXY = gXY.inGate(ecFPsn,eYF>>(TWO_D_FACTOR));
        boolean bPID = bSC && bFC && bFS && bCA && bS1S2;
		boolean bAcceptance = bXY && gFrntRear.inGate(ecFPsn,ecRPsn) && 
        gFYRY.inGate(eYF>>TWO_D_FACTOR,eYR>>TWO_D_FACTOR);
        boolean bGood = bPID && bAcceptance;
        boolean bState = bGood && gPeak.inGate(eFPsn);
		boolean bProtons = gSntrCthd_p.inGate(ecSntr, ecCthd) 
						 && gFrntSntr_p.inGate(ecFPsn,ecSntr) 
						 && gFrntCthd_p.inGate(ecFPsn,ecCthd) && bCA && bS1S2 && bAcceptance;
		boolean bDeuterons = gSntrCthd_d.inGate(ecSntr, ecCthd) && gFrntSntr_d.inGate(ecFPsn,ecSntr) 
															   && gFrntCthd_d.inGate(ecFPsn,ecCthd)
															   && bCA && bS1S2 && bAcceptance;
		boolean bTritons = gSntrCthd_t.inGate(ecSntr, ecCthd) && gFrntSntr_t.inGate(ecFPsn,ecSntr) 
															   && gFrntCthd_t.inGate(ecFPsn,ecCthd)
															   && bCA && bS1S2 && bAcceptance;

        if (bSC) {// gate on Scintillator vs Cathode
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
        }
        if (bPID) {// gated on all 3 gate above
            //writeEvent(dataEvent);
            hFrntGCSF.inc(eFPsn);
            hRearGCSF.inc(eRPsn);
            hFrntRearGCSF.inc(ecFPsn,ecRPsn);
            hFrntYRearY.inc(eYF>>TWO_D_FACTOR, eYR>>TWO_D_FACTOR);
            hYvsPsnGPID.inc(ecFPsn, eYF>>(TWO_D_FACTOR));
            if(bGood) {
                writeEvent(dataEvent);
                hFrntGAll.inc(eFPsn);
                hRearGAll.inc(eRPsn);
                hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
                hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
           }
        }
         if (bProtons) {// gated on proton gates plus acceptance gates
         	hFrntGAll_p.inc(eFPsn);
         }
         if (bDeuterons) {// gated on deuteron gates plus acceptance gates
         	hFrntGAll_d.inc(eFPsn);
         }
         if (bTritons) {// gated on proton gates plus acceptance gates
         	hFrntGAll_t.inc(eFPsn);
         }
    }
}
