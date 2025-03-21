/*
 */
package sort.VME;
import java.io.*;
import jam.data.*;
import jam.sort.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;
import sort.coinc.offline.ResidualKinematics;
import sort.coinc.offline.SurfaceAlphaEnergyLoss;
import dwvisser.analysis.ArrayCalibration;

/** Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>, which
 * was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right way; also 
 * added gate to cathAnde
 *
 * @author Dale Visser
 * @since 26 July 2001
 */
public class ElectronicsTesting_30Oct extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 200;//ADC lower threshold in channels
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
    Histogram hDummy,hCthd,hAnde,hSntr1,hSntr2,hSntrSum, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;     // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hSntrCthd, hFrntCthd, hFrntAnde, hFrntSntr, hFrntPRearP;
    Histogram hFrntY, hRearY, hYvsPsn, hYvsPsnGPID;
    Histogram hSntr1Sntr2,hSntrCthdAnde;
    Histogram hFrntYRearY;

    Histogram hFrntSntrGSC, hFrntCthdGSC;//gate by scintillator cathode
    Histogram hSntrCthdGFC, hFrntSntrGFC;//gate by Front wire Cathode
    Histogram hSntrCthdGFS, hFrntCthdGFS;//gate by Front wire Scintillator
    Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed & time
    Histogram hcFrntGTime, hFrntGTime;//front and rear wire gated on All compressed & time

    Gate  gCthd, gPeak;     //gates 1 d
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear, gCthdAnde, gXY, gSntr1Sntr2, gFYRY;//gates 2 d
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Scaler sFCLR;//number of FCLR's that went to ADC's
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors
    Monitor mFCLR;

    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh,
    idRearHgh, idYFrnt, idYRear, idDummy;

    public void initialize() throws Exception {
        vmeMap.setScalerInterval(3);
        //Focal Plane Detector
        idCthd=vmeMap.eventParameter(0, ADC_BASE[0], 10, THRESHOLDS);
        idAnde=vmeMap.eventParameter(1, ADC_BASE[0], 1, THRESHOLDS);
        idScintR=vmeMap.eventParameter(2, ADC_BASE[0], 2, THRESHOLDS);
        idScintL=vmeMap.eventParameter(3, ADC_BASE[0], 3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(4, ADC_BASE[0], 4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(5, ADC_BASE[0], 5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(6, ADC_BASE[0], 6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(7, ADC_BASE[0], 7, THRESHOLDS);
        idYFrnt=vmeMap.eventParameter(8, ADC_BASE[0], 8, THRESHOLDS);
        idYRear=vmeMap.eventParameter(9, ADC_BASE[0], 9, THRESHOLDS);
        int idDummy=vmeMap.eventParameter(0, ADC_BASE[0], 0, 0);
        hCthd      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS,
        "Cathode Raw ");
        hDummy      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS,
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
        hCthdAnde   =new Histogram("CathodeAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Anode ","Cathode","Anode");
        hSntrCthd   =new Histogram("ScintCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator","Scintillator","Cathode");
        hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position","Front Position","Cathode");
        hFrntAnde   =new Histogram("FrontAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Front Position","Front Position","Anode");
        hFrntSntr   =new Histogram("FrontScint ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position","Front Position","Scintillator");
        hFrntPRearP =new Histogram("FrontRear  ", HIST_2D_INT,  TWO_D_CHANNELS, "Rear Position vs Front Position","Front Position","Rear Position");
        hFrntYRearY =new Histogram("FrontY_RearY  ", HIST_2D_INT,  TWO_D_CHANNELS, "Rear Y Position vs Y Front Position","Front Y Position","Rear Y Position");
        hSntr1Sntr2 =new Histogram("Sntr1Sntr2  ", HIST_2D_INT, TWO_D_CHANNELS, "Scint 1 vs Scint 2", "Scint  1", "Scint 2");
        hSntrCthdAnde =new Histogram("SntrCthdAnde  ", HIST_2D_INT, TWO_D_CHANNELS, "Scint vs CathodeplusAnode", "Scint", "Cathode+Anode");
        //gate on Scintillator Cathode
        hFrntSntrGSC=new Histogram("FrontScintGSC ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position - ScCa gate","Front Position", "Scintillator");
        hFrntCthdGSC=new Histogram("FrontCathodeGSC", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - ScCa gate","Front Position","Cathode");
        //gate on Front Wire Cathode
        hSntrCthdGFC=new Histogram("ScintCathodeGFC", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator - FwCa gate", "Scintillator","Cathode");
        hFrntSntrGFC=new Histogram("FrontScintGFC", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position - FwCa gate","Front Position", "Scintillator");
        //gate on Front Wire Scintillator
        hSntrCthdGFS=new Histogram("ScintCathodeGFS", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator - FwSc gate","Scintillator","Cathode");
        hFrntCthdGFS=new Histogram("FrontCathodeGFS ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - FwSc gate ","Front Position","Cathode");
        //gated on 3 gates
        hFrntGCSF   =new Histogram("FrontGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc gates");
        hRearGCSF   =new Histogram(   "RearGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc gates");
        hFrntRearGCSF=new Histogram("FRGateCSF  ",HIST_2D_INT, TWO_D_CHANNELS,"Front vs. Rear - ScCa, FwCa, FwSc gates");
        //gated on 4 gates
        hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
        hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
        hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");

        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gPeak   =new Gate("Peak", hFrntGAll);
        
        //gates  2d
        gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
        gCthdAnde   =new Gate("Ca-An", hCthdAnde);      //gate on Anode Cathode
        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        gXY         =new Gate("XY", hYvsPsn);           // gate on x and y acceptance
        gSntr1Sntr2 =new Gate("S1-S2", hSntr1Sntr2);    // gate on Scint1 vs Scint2
        gFYRY       =new Gate("FY-RY", hFrntYRearY);    // gate on y1 vs y2
        hFrntSntrGSC.addGate(gFrntSntr);
        hFrntCthdGSC.addGate(gFrntCthd);
        hSntrCthdGFC.addGate(gSntrCthd);
        hFrntSntrGFC.addGate(gFrntSntr);
        hSntrCthdGFS.addGate(gSntrCthd);
        hFrntCthdGFS.addGate(gFrntCthd);
        hFrntRearGCSF.addGate(gFrntRear);

        //scalers
        sBic      =new Scaler("BIC",0);
        sClck      =new Scaler("Clock",1);
        sEvntRaw    =new Scaler("Event Raw", 2);
        sEvntAccpt  =new Scaler("Event Accept",3);
        sScint    =new Scaler("Scintillator", 4);
        sCathode  =new Scaler("Cathode",5);
        sFCLR = new Scaler("FCLR",6);
        
        int SCALER_ADDRESS = 0xf0e00000;
        /* obsolete 
        vmeMap.scalerParameter(2048+sBic.getNumber(), SCALER_ADDRESS, sBic.getNumber(), sBic);
        vmeMap.scalerParameter(2048+sClck.getNumber(), SCALER_ADDRESS, sClck.getNumber(), sClck);
        vmeMap.scalerParameter(2048+sEvntRaw.getNumber(), SCALER_ADDRESS, sEvntRaw.getNumber(), sEvntRaw);
        vmeMap.scalerParameter(2048+sEvntAccpt.getNumber(), SCALER_ADDRESS, sEvntAccpt.getNumber(), sEvntAccpt);
        vmeMap.scalerParameter(2048+sScint.getNumber(), SCALER_ADDRESS, sScint.getNumber(), sScint);
        vmeMap.scalerParameter(2048+sCathode.getNumber(), SCALER_ADDRESS, sCathode.getNumber(), sCathode);
        vmeMap.scalerParameter(2048+sFCLR.getNumber(), SCALER_ADDRESS, sFCLR.getNumber(), sFCLR);
		*/
		
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
        int eDummy  =dataEvent[idDummy];
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
        int eCthdAnde = (eCthd + eAnde)/2;

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

        // singles spectra
        hDummy.inc(eDummy);
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
        hYvsPsn.inc(ecFPsn,eYF>>TWO_D_FACTOR);
        hFrntPH.inc(ecFPsn,ecFHgh);
        hRearPH.inc(ecRPsn,ecRHgh);
        hSntrCthd.inc(ecSntr,ecCthd);
        hFrntCthd.inc(ecFPsn,ecCthd);
        hFrntAnde.inc(ecFPsn,ecAnde);
        hFrntSntr.inc(ecFPsn,ecSntr);
        hFrntPRearP.inc(ecFPsn,ecRPsn);
        

        boolean bSC = gSntrCthd.inGate(ecSntr,ecCthd);
        boolean bCA = gCthdAnde.inGate(ecCthd,ecAnde);
        boolean bFC = gFrntCthd.inGate(ecFPsn,ecCthd);
        boolean bFS = gFrntSntr.inGate(ecFPsn,ecSntr);
        boolean bS1S2 = gSntr1Sntr2.inGate(eSntr1>>TWO_D_FACTOR,eSntr2>>TWO_D_FACTOR);
        boolean bXY = gXY.inGate(ecFPsn,eYF>>TWO_D_FACTOR);
        boolean bPID = bSC && bFC && bFS && bCA && bS1S2;
        boolean bGood = bPID && gFrntRear.inGate(ecFPsn,ecRPsn) && gFYRY.inGate(eYF>>TWO_D_FACTOR,eYR>>TWO_D_FACTOR);
        boolean bState = bGood && gPeak.inGate(eFPsn);

        if (bSC) {// gate on Scintillator vs Cathode
            hFrntSntrGSC.inc(ecFPsn,ecSntr);
            hFrntCthdGSC.inc(ecFPsn,ecCthd);
        }
        if (bFC) {// gate on Front Wire Position vs Cathode
            hSntrCthdGFC.inc(ecSntr,ecCthd);
            hFrntSntrGFC.inc(ecFPsn,ecSntr);
        }
        if (bFS){// gate on Front Wire Position vs Scintillator
            hSntrCthdGFS.inc(ecSntr,ecCthd);
            hFrntCthdGFS.inc(ecFPsn,ecCthd);
            
            hSntrCthdAnde.inc(ecSntr,eCthdAnde>>TWO_D_FACTOR);
            hCthdAnde.inc(ecCthd,ecAnde);
        }
        if (bPID) {// gated on all 3 gate above
            //writeEvent(dataEvent);
            hFrntGCSF.inc(eFPsn);
            hRearGCSF.inc(eRPsn);
            hSntr1Sntr2.inc(eSntr1>>TWO_D_FACTOR, eSntr2>>TWO_D_FACTOR);
            hFrntRearGCSF.inc(ecFPsn,ecRPsn);
            hFrntYRearY.inc(eYF>>TWO_D_FACTOR, eYR>>TWO_D_FACTOR);
            if(bGood) {
                writeEvent(dataEvent);
                hFrntGAll.inc(eFPsn);
                hRearGAll.inc(eRPsn);
                hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
                hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
                hYvsPsnGPID.inc(ecFPsn, eYF>>TWO_D_FACTOR);
            }
        }
    }
    /** Called so the dead time can be calculated.
     * @param name name of monitor to calculate
     * @return floating point value of monitor
     */
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
         }*/ else {
            return 50.0;
        }
    }
}
