/*
 */
package sort.offline.LEDA;
import jam.data.*;
import jam.sort.*;

/*
 * This sort routine is for sorting our run made on 21 Dec 2000.
 * The physical arrangement was one LEDA detector in coincidence
 * with the FP detector.
 */
public class Coincidence extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000};
static final int [] TDC_BASE = {0x30000000,0x30010000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 300/16;
    static final int TIME_THRESHOLDS = 300/16;

    //LEDA Detectors
    static final int NUM_DETECTORS = 1;
    static final int STRIPS_PER_DETECTOR = 16;
    static final int SAMPLE_STRIP = 4;

    //histogramming constants
    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed position histogram
    final int TIME_CHANNELS = 1024; //number of channels for corrected time spectra (1 ns/ch)
    final int ENERGY_CHANNELS = 2048; //number of channels for corrected energy spectra (10 keV/channel)
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
    final int COMPRESS_FACTOR = Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR = Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));
    final int TWO_D_TIME = Math.round((float)(Math.log(TIME_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));
    final int TWO_D_ENERGY = Math.round((float)(Math.log(ENERGY_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    //Histogram [][] hStrips = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    //Histogram [][] hTimes = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    Histogram hEnergy, hTime;
    int [][] idStrips = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] idTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] eStrips = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] eTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

    // ungated spectra
    Histogram hCthd,hAnde,hSntr1,hSntr2,hSntrSum, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hSntrCthd, hFrntCthd, hFrntAnde, hFrntSntr, hFrntPRearP;
    Histogram hSilicon, hNaI1, hNaI2;
    Histogram hFrntGT, hStripVsE,hStripVsP, hTvsE;
    Histogram [] hEvsP=new Histogram[STRIPS_PER_DETECTOR];
    Histogram hFrntSntrGSC, hFrntCthdGSC;//gate by scintillator cathode
    Histogram hSntrCthdGFC, hFrntSntrGFC;//gate by Front wire Cathode
    Histogram hSntrCthdGFS, hFrntCthdGFS;//gate by Front wire Scintillator
    Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed

    Gate gSilicon, gCthd, gPeak, gPosn;//gates 1 d
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear;//gates 2 d
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors

    Gate gTime;
    Histogram hCthdFrntGT;
    Histogram [] hTimeGPID = new Histogram[NUM_DETECTORS];
    Histogram hHits, hEvsStrip, hTvsStrip;

    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh, idRearHgh, idSilicon, idNaI1, idNaI2;
    int idFrontR, idFrontL, idRearR, idRearL;
    //int NUM_PARAMETERS;

    int lastEvntAccpt;

    double [] timeChannels = {1634.52, 1678.97, 1803.68, 1654.31, 1535.90, 1652.83, 1539.97, 1638.31,
    1636.25,1600.0, 1531.14, 1622.29, 1605.67, 1406.19, 1520.02, 1600.76};
    double [] Am241Channels = {2174.24, 2096.64, 2361.22, 2354.40, 2166.01, 2088.35, 2189.58, 2189.30,
    2111.34, 2128.23, 2225.26, 2235.49, 2061.29, 2135.50, 2139.15, 2203.28};
    double [] timeFactor = new double[16];
    double [] energyFactor = new double[16];
    int DET=0;int STRIP=4;
    DataParameter pStrip;
    
    public void initialize() throws Exception {
        vmeMap.setScalerInterval(3);

        for (int i=0; i<16; i++){
            timeFactor[i]=300.0/timeChannels[i];//multiply by this for corrected channel
            energyFactor[i]=548.0/Am241Channels[i];//multiply by this for corrected channel
        }

        //Focal Plane Detector
        idCthd=vmeMap.eventParameter(1, ADC_BASE[0], 0, THRESHOLDS);
        idAnde=vmeMap.eventParameter(2, ADC_BASE[0], 1, THRESHOLDS);
        idScintR=vmeMap.eventParameter(3, ADC_BASE[0], 2, THRESHOLDS);
        idScintL=vmeMap.eventParameter(4, ADC_BASE[0], 3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(5, ADC_BASE[0], 4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(6, ADC_BASE[0], 5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(7, ADC_BASE[0], 6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(8, ADC_BASE[0], 7, THRESHOLDS);
        for (int i=9; i <=16; i++) {
            int idDummy=vmeMap.eventParameter(i, ADC_BASE[0], i-1, THRESHOLDS);
        }
        //idSilicon=vmeMap.eventParameter(9, ADC_BASE[0], 8, THRESHOLDS);
        //idNaI1=vmeMap.eventParameter(10, ADC_BASE[0], 9, THRESHOLDS);
        //idNaI2=vmeMap.eventParameter(11, ADC_BASE[0], 10, THRESHOLDS);
        //idFrontR=vmeMap.eventParameter(12, TDC_BASE, 0, THRESHOLDS);
        //idFrontL=vmeMap.eventParameter(13, TDC_BASE, 1, THRESHOLDS);
        //idRearR=vmeMap.eventParameter(14, TDC_BASE, 2, THRESHOLDS);
        //idRearL=vmeMap.eventParameter(15, TDC_BASE, 3, THRESHOLDS);
        System.err.println("# Parameters: "+getEventSize());
        System.err.println("ADC channels: "+ADC_CHANNELS);
        System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
        System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);
        hCthd      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS, "Cathode Raw ");
        hAnde      =new Histogram("Anode       ", HIST_1D_INT, ADC_CHANNELS, "Anode Raw");
        hSntr1      =new Histogram("Scint1      ", HIST_1D_INT, ADC_CHANNELS, "Scintillator PMT 1");
        hSntr2      =new Histogram("Scint2      ", HIST_1D_INT, ADC_CHANNELS, "Scintillator PMT 2");
        hSntrSum    =new Histogram("ScintSum    ", HIST_1D_INT, ADC_CHANNELS, "Scintillator Sum");
        hFrntPsn    =new Histogram("FrontPosn    ", HIST_1D_INT, ADC_CHANNELS, "Front Wire Position");
        hRearPsn    =new Histogram("RearPosn     ", HIST_1D_INT, ADC_CHANNELS, "Rear Wire Position");
        hFrntHgh    =new Histogram("FrontHeight   ", HIST_1D_INT, ADC_CHANNELS, "Front Wire Pulse Height");
        hRearHgh    =new Histogram("RearHeight    ", HIST_1D_INT, ADC_CHANNELS, "Rear Wire Pulse Height");
        hSilicon = new Histogram("Silicon     ", HIST_1D_INT, ADC_CHANNELS, "Beam Monitor");
        hNaI1 = new Histogram("NaI 1", HIST_1D_INT, ADC_CHANNELS, "NaI Detector 1");
        hNaI2 = new Histogram("NaI 2", HIST_1D_INT, ADC_CHANNELS, "NaI Detector 2");
        hFrntPH      =new Histogram("FrontPvsHeight", HIST_2D_INT,  TWO_D_CHANNELS, "Pulse Height vs Front Position","Front Position","Pulse Height");
        hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,  TWO_D_CHANNELS, "Pulse Height vs Rear Position","Rear Position", "Pulse Height");
        hCthdAnde   =new Histogram("CathodeAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Anode ","Cathode","Anode");
        hSntrCthd   =new Histogram("ScintCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator","Scintillator","Cathode");
        hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position","Front Position","Cathode");
        hFrntAnde   =new Histogram("FrontAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Front Position","Front Position","Anode");
        hFrntSntr   =new Histogram("FrontScint ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position","Front Position","Scintillator");
        hFrntPRearP =new Histogram("FrontRear  ", HIST_2D_INT,  TWO_D_CHANNELS, "Rear Position vs Front Position","Front Position","Rear Position");
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

        //LEDA Array
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int Eparam=17 + 2*i*STRIPS_PER_DETECTOR + 2*j;
                int Tparam=Eparam+1;
                idStrips[i][j]=vmeMap.eventParameter(Eparam, whichADC(i), whichChannel(i,j), THRESHOLDS);
                idTimes[i][j]=vmeMap.eventParameter(Tparam, whichTDC(i), whichTDCchannel(i,j), TIME_THRESHOLDS);
                if (i==DET) {
                    hEvsP[j]=new Histogram("EvsP "+j,  HIST_2D_INT, TWO_D_CHANNELS, "Silicon Energy vs. FP Posn "+j+
                    ", gated on PID, 2D coinc.","FP Position", "Energy");
                }
            }
        }
        hEnergy=new Histogram("Energy", HIST_1D_INT, ADC_CHANNELS, "Energy");
        hTime=new Histogram("Time", HIST_1D_INT, ADC_CHANNELS, "Time");
        hHits = new Histogram("Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over ADC threshold",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hEvsStrip = new Histogram("EvsStrip",HIST_2D_INT, TWO_D_CHANNELS, "Energy vs. Strip, All Detectors", "Energy",
        STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStrip = new Histogram("TvsStrip",HIST_2D_INT, TWO_D_CHANNELS, "Time vs. Strip, All Detectors", "Time",
        STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsE = new Histogram("TvsE",HIST_2D_INT, TWO_D_CHANNELS, "Time vs. Energy, All Detectors", "Energy", "Time");
        gTime=new Gate("Time", hTime);//gates on selected TDC channels
        hCthdFrntGT=new Histogram("CthdFrntGT", HIST_2D_INT, TWO_D_CHANNELS, "Cathode vs. Position, gated on: "+gTime,
        "Position", "Cathode");
        hFrntGT=new Histogram("FrontGT    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - Coinc, ScCa,FwCa,FwSc,FwRw gates");
        //hEvsP=new Histogram("EvsP",  HIST_2D_INT, TWO_D_CHANNELS, "Silicon Energy vs. FP Posn, gated on PID, 2D coinc.",
        //"FP Position", "Energy");
        //hStripVsE=new Histogram("StripVsE",  HIST_2D_INT, TWO_D_CHANNELS, "Strip vs. Silicon Energy, gated on PID, 2D coinc.",
        //"Energy", "Strip");
        //hStripVsP=new Histogram("StripVsP",  HIST_2D_INT, TWO_D_CHANNELS, "Strip vs. FP Posn, gated on PID, 2D coinc.",
        //"FP Position", "Strip");
        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gPeak   =new Gate("Peak", hFrntGCSF);
        gSilicon    = new Gate("Elastics", hSilicon);
        gPosn  =new Gate("Position",hFrntGAll);
        hFrntGT.addGate(gPosn);
        //gates  2d
        gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        hFrntSntrGSC.addGate(gFrntSntr);
        hFrntCthdGSC.addGate(gFrntCthd);
        hSntrCthdGFC.addGate(gSntrCthd);
        hFrntSntrGFC.addGate(gFrntSntr);
        hSntrCthdGFS.addGate(gSntrCthd);
        hFrntCthdGFS.addGate(gFrntCthd);

        //scalers
        sBic      =new Scaler("BIC",0);
        sClck      =new Scaler("Clock",1);
        sEvntRaw    =new Scaler("Event Raw", 2);
        sEvntAccpt  =new Scaler("Event Accept",3);
        sScint    =new Scaler("Scintillator", 4);
        sCathode  =new Scaler("Cathode",5);
        
        pStrip = new DataParameter("Strip#");

        int SCALER_ADDRESS = 0xf0e00000;
        /* obsolete 
        vmeMap.scalerParameter(2048+sBic.getNumber(), SCALER_ADDRESS, sBic.getNumber(), sBic);
        vmeMap.scalerParameter(2048+sClck.getNumber(), SCALER_ADDRESS, sClck.getNumber(), sClck);
        vmeMap.scalerParameter(2048+sEvntRaw.getNumber(), SCALER_ADDRESS, sEvntRaw.getNumber(), sEvntRaw);
        vmeMap.scalerParameter(2048+sEvntAccpt.getNumber(), SCALER_ADDRESS, sEvntAccpt.getNumber(), sEvntAccpt);
        vmeMap.scalerParameter(2048+sScint.getNumber(), SCALER_ADDRESS, sScint.getNumber(), sScint);
        vmeMap.scalerParameter(2048+sCathode.getNumber(), SCALER_ADDRESS, sCathode.getNumber(), sCathode);
		*/
		
        //monitors
        mBeam=new Monitor("Beam ",sBic);
        mClck=new Monitor("Clock",sClck);
        mEvntRaw=new Monitor("Raw Events",sEvntRaw);
        mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
        mScint=new Monitor("Scintillator",sScint);
        mCathode=new Monitor("Cathode",sCathode);
        Monitor mLiveTime=new Monitor("Live Time", this);
    }

    //Utility methods for mapping strips to ADC channels

    /**
     * Returns which adc unit given which detector.
     */
    private int whichADC(int detector){
        if (detector == 0) {//Detector 0
            return ADC_BASE[0];
        } else {//Detectors 1 and 2
            return ADC_BASE[1];
        }
    }
    /**
     * Returns which tdc unit given which detector.
     */
    private int whichTDC(int detector){
        if (detector == 2) {//Detector 2
            return TDC_BASE[1];
        } else {//Detectors 0 and 1
            return TDC_BASE[1];
        }
    }

    /**
     * Returns which channel in the adc given which detector and strip.
     */
    private int whichChannel(int detector, int strip){
        if (detector == 0 || detector==2) {//Detectors 0 and 2
            return strip+16;
        } else {//Detector 1
            return strip;
        }
    }
    /**
     * Returns which channel in the tdc given which detector and strip.
     */
    private int whichTDCchannel(int detector, int strip){
        if (detector == 0 || detector==2) {//Detectors 0 and 2
            return strip+16;
        } else {//Detector 1
            return strip+16;
        }
    }

    public void sort(int [] dataEvent) throws Exception {
        //unpack data into convenient names
        int eCthd   =dataEvent[idCthd];
        int eAnde   =dataEvent[idAnde];
        int eSntr1  =dataEvent[idScintR];
        int eSntr2  =dataEvent[idScintL];
        int eFPsn   =dataEvent[idFrntPsn];
        int eRPsn   =dataEvent[idRearPsn];
        int eFHgh   =dataEvent[idFrntHgh];
        int eRHgh   =dataEvent[idRearHgh];
        int eSil    = dataEvent[idSilicon];
        int eNaI1 = dataEvent[idNaI1];
        int eNaI2 = dataEvent[idNaI2];

        int eSntr=(int)Math.round(Math.sqrt(eSntr1*eSntr2));

        int ecFPsn=eFPsn>>TWO_D_FACTOR;
        int ecRPsn=eRPsn>>TWO_D_FACTOR;
        int ecFHgh=eFHgh>>TWO_D_FACTOR;
        int ecRHgh=eRHgh>>TWO_D_FACTOR;
        int ecSntr=eSntr>>TWO_D_FACTOR;
        int ecCthd=eCthd>>TWO_D_FACTOR;
        int ecAnde=eAnde>>TWO_D_FACTOR;
        STRIP=(int)Math.round(pStrip.getValue());
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                eStrips[i][j] = dataEvent[idStrips[i][j]];
                //eStrips[i][j] = (int)Math.round(rawEnergy * energyFactor[j]);
                eTimes[i][j] = dataEvent[idTimes[i][j]];
            }
        }

        hEnergy.inc(eStrips[DET][STRIP]);
        hTime.inc(eTimes[DET][STRIP]);
        int stripBin = DET*STRIPS_PER_DETECTOR+STRIP;
        int ecEnergy=eStrips[DET][STRIP]>>TWO_D_FACTOR;
        hEvsStrip.inc(ecEnergy,stripBin);
        int ecTime=eTimes[DET][STRIP]>>TWO_D_FACTOR;
        hTvsStrip.inc(ecTime,stripBin);
        hTvsE.inc(ecEnergy,ecTime);
        if (gTime.inGate(eTimes[DET][STRIP])){
            for (int s=0; s<STRIPS_PER_DETECTOR; s++){
                stripBin = DET*STRIPS_PER_DETECTOR+s;
                if (eStrips[DET][s] > 0) {
                    hHits.inc(stripBin);
                }
            }
        }

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
        hSilicon.inc(eSil);
        hNaI1.inc(eNaI1);
        hNaI2.inc(eNaI2);

        //singles 2d spectra
        hFrntPH.inc(ecFPsn,ecFHgh);
        hRearPH.inc(ecRPsn,ecRHgh);
        hCthdAnde.inc(ecCthd,ecAnde);
        hSntrCthd.inc(ecSntr,ecCthd);
        hFrntCthd.inc(ecFPsn,ecCthd);
        hFrntAnde.inc(ecFPsn,ecAnde);
        hFrntSntr.inc(ecFPsn,ecSntr);
        hFrntPRearP.inc(ecFPsn,ecRPsn);

        if ( gSntrCthd.inGate(ecSntr,ecCthd) ){// gate on Scintillator vs Cathode
            hFrntSntrGSC.inc(ecFPsn,ecSntr);
            hFrntCthdGSC.inc(ecFPsn,ecCthd);
        }
        if ( gFrntCthd.inGate(ecFPsn,ecCthd) ){// gate on Front Wire Position vs Cathode
            hSntrCthdGFC.inc(ecSntr,ecCthd);
            hFrntSntrGFC.inc(ecFPsn,ecSntr);
        }
        if ( gFrntSntr.inGate(ecFPsn,ecSntr) ){// gate on Front Wire Position vs Scintillator
            hSntrCthdGFS.inc(ecSntr,ecCthd);
            hFrntCthdGFS.inc(ecFPsn,ecCthd);
        }

        if (( gSntrCthd.inGate(ecSntr,ecCthd) )&&
        ( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
        ( gFrntSntr.inGate(ecFPsn,ecSntr) )){// gated on all 3 gate above
            hFrntGCSF.inc(eFPsn);
            hRearGCSF.inc(eRPsn);
            hFrntRearGCSF.inc(ecFPsn,ecRPsn);
        }
        // gate on all 3 gates above and the Front wire vs Rear Wire
        if (( gSntrCthd.inGate(ecSntr,ecCthd) )&&
        ( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
        ( gFrntSntr.inGate(ecFPsn,ecSntr) )&&
        ( gFrntRear.inGate(ecFPsn,ecRPsn) )){
            hFrntGAll.inc(eFPsn);
            hRearGAll.inc(eRPsn);
            hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
            hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
            if (gTime.inGate(eTimes[DET][STRIP])){
                hFrntGT.inc(eFPsn);
                //if (gPosn.inGate(eFPsn)){
                for (int strip=0; strip<STRIPS_PER_DETECTOR; strip++){
                    hEvsP[strip].inc(ecFPsn, eStrips[DET][strip]>>TWO_D_FACTOR);
                }
                //}
            }
        }
    }

    /**
     * monitor method
     * calculate the live time
     */
    public double monitor(String name){
        int rateEvntAccpt=sEvntAccpt.getValue()-lastEvntAccpt;
        lastEvntAccpt=sEvntAccpt.getValue();
        if (name.equals("Live Time")){
            if (((double)(mEvntRaw.getValue()))>0.0){
                return 100.0*rateEvntAccpt/mEvntRaw.getValue();
            } else {
                return 0.0;
            }
        } else {
            return 50.0;
        }
    }
}
