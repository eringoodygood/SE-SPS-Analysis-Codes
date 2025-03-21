/*
 */
package sort.coinc.V7x5Format;
import java.io.*;
import jam.data.*;
import jam.sort.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;
import sort.coinc.offline.ResidualKinematics;
import sort.coinc.offline.SurfaceAlphaEnergyLoss;
import dwvisser.analysis.ArrayCalibration;
import dwvisser.analysis.regression.MathException;

/** Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>, which
 * was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right way; also 
 * added gate to cathAnde. Now the data shipped across
 * is in the format as produced by the V7x5's.  The EventInputStream this code
 * works with is YaleCAEN_InputStream.
 *
 * @author Dale Visser
 * @since 26 July 2001
 */
public class Mg22 extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
static final int [] TDC_BASE = {0x30000000,0x30010000,0x30020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 200;  //ADC lower threshold in channels
    static final int TIME_THRESHOLDS = 30;//TDC lower threshold in channels
    static final int TIME_RANGE = 600;//ns
    static final int LAST_ADC_BIN = 3840;

    //LEDA Detectors
    static final int NUM_DETECTORS = 5;
    static final int STRIPS_PER_DETECTOR = 16;

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

    //histograms and parameter ID's for YLSA channels
    Histogram [][] hEnergies = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    Histogram [][] hTimes = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] idEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] idTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

    // ungated spectra
    Histogram hCthd,hAnde,hSntr1,hSntr2,hSntrSum, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hSntrCthd, hFrntCthd, hFrntAnde, hFrntSntr, hFrntPRearP;
    Histogram hFrntY, hRearY, hYvsPsn, hYvsPsnGPID;

    Histogram hFrntSntrGSC, hFrntCthdGSC;//gate by scintillator cathode
    Histogram hSntrCthdGFC, hFrntSntrGFC;//gate by Front wire Cathode
    Histogram hSntrCthdGFS, hFrntCthdGFS;//gate by Front wire Scintillator
    Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed & time
    Histogram hcFrntGTime, hFrntGTime;//front and rear wire gated on All compressed & time

    Gate /*gSilicon*/ gCthd;//gates 1 d
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear, gCthdAnde;//gates 2 d
    Gate gFRexclude;
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Scaler sFCLR;//number of FCLR's that went to ADC's
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors
    Monitor mFCLR;

    Histogram hHits, hTvsStrip, hTimeHits, hTvsEhits, hInterHits,hEnergyHits;
    Histogram hMultiplicity;
    Histogram hEvsStripGA, hTvsStripGA; //gain adjusted spectra
    Histogram hAngDist; //for getting angular distribution of alphas

    //coincidence histograms & gates
    Gate gPeak; //added to frontGAll, front gTime
    Histogram hTimeGA,hTimeGAstate, hTimeGAdecay;//all strips time gain adjusted (TOF adjustment for alpha)
    Gate gTimeBroad,gTimeState,gTimeDecay;
    Histogram hEvsStripBroad, hEvsStripState, hEvsStripDecay;
    Gate gEvsS;
    Histogram hEvsChBroad, hEvsChState, hEvsChDecay;
    Histogram hEsilVsFP;
    Histogram hRYvsFY;
    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh,
    idRearHgh, idYFrnt, idYRear;

    /**
     * Containers of information about any strips with both an energy and time
     * signal for this event.
     */
    int TOTAL_STRIPS = STRIPS_PER_DETECTOR*NUM_DETECTORS;
    int [] detHit = new int[TOTAL_STRIPS];
    int [] stripHit = new int[TOTAL_STRIPS];
    int [] bin = new int[TOTAL_STRIPS];

    int numInterHits;//number of real interStrip hits (in TDC window)
    int [] interDetHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] interStripHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] interBin = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];

    ResidualKinematics rk;//for calculating Residual trajectory
    //double Mproton;
    double mAlpha;

    ArrayCalibration ac;
    Histogram hEcmVsStripBroad,hEcmVsFP;
    /** Sets up objects, called when Jam loads the sort routine.
     * @throws Exception necessary for Jam to handle exceptions
     */
    public void initialize() throws Exception {
        retrieveCalibration();
        new Nucleus();//initialize nuclear data
        mAlpha = (new Nucleus(2,4)).getMass().value;
        rk=new ResidualKinematics(92,0,new Nucleus(8,16), new Nucleus(6,12),
        new Nucleus(2,4));
        vmeMap.setScalerInterval(3);
        for (int i=0; i < TDC_BASE.length; i++){
            vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
        }
        //Focal Plane Detector      (slot, base address, channel, threshold channel)
        idCthd=vmeMap.eventParameter(2, ADC_BASE[0], 10, 0);
        idAnde=vmeMap.eventParameter(2, ADC_BASE[0], 1, 0);
        idScintR=vmeMap.eventParameter(2, ADC_BASE[0], 2, 0);
        idScintL=vmeMap.eventParameter(2, ADC_BASE[0], 3, 0);
        idFrntPsn=vmeMap.eventParameter(2, ADC_BASE[0], 4, 0);
        idRearPsn=vmeMap.eventParameter(2, ADC_BASE[0], 5, 0);
        idFrntHgh=vmeMap.eventParameter(2, ADC_BASE[0], 6, 0);
        idRearHgh=vmeMap.eventParameter(2, ADC_BASE[0], 7, 0);
        idYFrnt=vmeMap.eventParameter(2, ADC_BASE[0], 8, 0);
        idYRear=vmeMap.eventParameter(2, ADC_BASE[0], 9, 0);
        int idDummy=vmeMap.eventParameter(2, ADC_BASE[0], 0, 0);
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
        hRYvsFY = new Histogram("RearYvsFrntY", HIST_2D_INT, TWO_D_CHANNELS,
        "Rear Y vs. Front Y", "Front Y", "Rear Y");
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
        hFrntGTime  = new Histogram("FrontGTime    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw & time gates");
        hcFrntGTime  =new Histogram("FrontGTimecmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw & time gates");

        //YLSA
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                //eventParameter(slot, base address, channel, threshold channel)
                idEnergies[i][j]=vmeMap.eventParameter(whichADCslot(i), 
                whichADCaddress(i), whichADCchannel(i,j), THRESHOLDS);
                hEnergies[i][j]=new Histogram("E_D"+i+"_S"+j, HIST_1D_INT, ADC_CHANNELS,
                "Detector "+i+", Strip "+j);
            }
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                //eventParameter(slot, base address, channel, threshold channel)
                idTimes[i][j]=vmeMap.eventParameter(whichTDCslot(i), 
                whichTDCaddress(i), whichTDCchannel(i,j), TIME_THRESHOLDS);
                hTimes[i][j]=new Histogram("T_D"+i+"_S"+j, HIST_1D_INT, ADC_CHANNELS, "Detector "+i+", Strip "+j+" time");
            }
        }

        System.err.println("# Parameters: "+getEventSize());
        System.err.println("ADC channels: "+ADC_CHANNELS);
        System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
        System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);

        hMultiplicity=new Histogram("Multiplicity",HIST_1D_INT,NUM_DETECTORS*STRIPS_PER_DETECTOR,
        "Multiplicity of Energy and Time Hits");
        hHits = new Histogram("Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over ADC and TDC thresholds",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hAngDist = new Histogram("DecayAngDist", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,
        "Angular Distribution of Decays of gated State",STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hInterHits = new Histogram("InterHits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Inter-Strip hits",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hTimeHits = new Histogram("Time Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over TDC threshold",
        "Strip","Counts");
        hEnergyHits = new Histogram("Energy Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over ADC threshold",
        "Strip","Counts");
        hTvsEhits = new Histogram("T vs E hits", HIST_2D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Time hits vs Energy hits",
        "E hits", "T hits");
        //hEvsStrip = new Histogram("EvsStrip",HIST_2D_INT, TWO_D_CHANNELS, "Energy vs. Strip, All Detectors", "Energy",
        //STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStrip = new Histogram("TvsStrip",HIST_2D_INT, TWO_D_CHANNELS,NUM_DETECTORS*STRIPS_PER_DETECTOR,
        "Time vs. Strip, All Detectors, multiplicity one", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        /*hTvsStripPID = new Histogram("TvsStripPID",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one,PID gates", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");*/
        hEvsStripGA = new Histogram("EvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,NUM_DETECTORS*STRIPS_PER_DETECTOR,
        "Energy vs. Strip, All Detectors, Gain Adjusted", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStripGA = new Histogram("TvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,NUM_DETECTORS*STRIPS_PER_DETECTOR,
        "Time vs. Strip, All Detectors, Gain Adjusted", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        hEvsStripBroad = new Histogram("EvsSbroad",HIST_2D_INT
        ,TWO_D_CHANNELS,STRIPS_PER_DETECTOR+1,
        "Strip vs. Energy Deposited","Energy","Strip");
        hEcmVsStripBroad = new Histogram("EcmVsSbroad",HIST_2D_INT
        ,1000,STRIPS_PER_DETECTOR+1,
        "Strip vs. Ecm(proton)","Energy[Mev]*10","Strip");
        hEcmVsFP = new Histogram("EcmVsFP", HIST_2D_INT,TWO_D_CHANNELS,100,
        "Ecm(proton) vs. Focal Plane Position","QBrho [channels]","Ecm [MeV] * 10");
        hEsilVsFP = new Histogram("EsilVsFP", HIST_2D_INT,TWO_D_CHANNELS,100,
        "E (YLSA) vs. Focal Plane Position","QBrho [channels]","E [MeV] * 10");
        hEvsStripState = new Histogram("EvsSstate",HIST_2D_INT
        ,TWO_D_CHANNELS,STRIPS_PER_DETECTOR+1,
        "Strip vs. Energy Deposited","Energy","Strip");
        hEvsStripDecay = new Histogram("EvsSdecay",HIST_2D_INT
        ,TWO_D_CHANNELS,STRIPS_PER_DETECTOR+1,
        "Strip vs. Energy Deposited","Energy","Strip");
        hEvsChBroad = new Histogram("EvsChBroad", HIST_2D_INT,TWO_D_CHANNELS,
        NUM_DETECTORS*STRIPS_PER_DETECTOR,"Channel vs. Energy", "Energy",
        "Channel");
        hEvsChState = new Histogram("EvsChState", HIST_2D_INT,TWO_D_CHANNELS,
        NUM_DETECTORS*STRIPS_PER_DETECTOR,"Channel vs. Energy", "Energy",
        "Channel");
        hEvsChDecay = new Histogram("EvsChDecay", HIST_2D_INT,TWO_D_CHANNELS,
        NUM_DETECTORS*STRIPS_PER_DETECTOR,"Channel vs. Energy", "Energy",
        "Channel");

        gEvsS = new Gate("EvsS", hEvsStripBroad);
        hEvsStripState.addGate(gEvsS);
        hEvsStripDecay.addGate(gEvsS);
        hTimeGA = new Histogram("TimeGA",HIST_1D_INT, TWO_D_CHANNELS,
        "Time, Gain Adjusted with alpha TOF subtracted");
        hTimeGAstate= new Histogram("TimeGAstate",HIST_1D_INT, TWO_D_CHANNELS,
        "Time, Gain Adjusted with alpha TOF subtracted");
        hTimeGAdecay= new Histogram("TimeGAdecay",HIST_1D_INT, TWO_D_CHANNELS,
        "Time, Gain Adjusted with alpha TOF subtracted");
        gTimeBroad=new Gate("TimeBroad", hTimeGA);//gates on selected TDC channels
        gTimeState=new Gate("TimeState", hTimeGA);//gates on selected TDC channels
        gTimeDecay=new Gate("TimeDecay", hTimeGA);//gates on selected TDC channels
        hTimeGAstate.addGate(gTimeBroad);
        hTimeGAstate.addGate(gTimeState);
        hTimeGAstate.addGate(gTimeDecay);
        hTimeGAdecay.addGate(gTimeBroad);
        hTimeGAdecay.addGate(gTimeState);
        hTimeGAdecay.addGate(gTimeDecay);

        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gPeak   =new Gate("Peak", hFrntGAll);
        hFrntGTime.addGate(gPeak);
        //gates  2d
        gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
        gCthdAnde   =new Gate("Ca-An", hCthdAnde);      //gate on Anode Cathode
        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        gFRexclude = new Gate("FRexclude", hFrntPRearP);
        Gate gDataEvent = new Gate("DataEvent",hSntrCthd);
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
        

        //monitors
        mBeam=new Monitor("Beam ",sBic);
        mClck=new Monitor("Clock",sClck);
        mEvntRaw=new Monitor("Raw Events",sEvntRaw);
        mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
        mScint=new Monitor("Scintillator",sScint);
        mCathode=new Monitor("Cathode",sCathode);
        new Monitor("DataEvent", gDataEvent);
        mFCLR = new Monitor("FCLR",sFCLR);
        Monitor mDeadTime=new Monitor(DEAD_TIME, this);
        Monitor mTrueDeadTime = new Monitor(TRUE_DEAD_TIME, this);
    }
    //Utility methods for mapping strips to ADC channels
    /**
     * Returns which adc unit given which detector.
     */
    private int whichADCaddress(int detector){
        if (detector == 0) {//Detector 0
            return ADC_BASE[0];
        } else if (detector == 1 || detector == 2) {//Detectors 1 and 2
            return ADC_BASE[1];
        } else {//detectors 3 and 4
            return ADC_BASE[2];
        }
    }
    /**
     * @return which slot in the VME crate contains the ADC for this detector
     */
    private int whichADCslot(int detector){
        if (detector==0){
            return 2;
        } else if (detector == 1 || detector == 2) {
            return 3;
        } else { //detectors 3 and 4
            return 4;
        }
    }
    
    /**
     * Returns which tdc unit given which detector.
     */
    private int whichTDCaddress(int detector){
        if (detector == 0) {//Detector 0
            return TDC_BASE[0];
        } else if (detector == 1 || detector == 2){//Detectors 1  and 2
            return TDC_BASE[1];
        } else {//detectors 3 and 4
            return TDC_BASE[2];
        }
    }
    /**
     * @return which slot in the VME crate contains the TDC for this detector
     */
    private int whichTDCslot(int detector){
        if (detector==0){
            return 5;
        } else if (detector == 1 || detector == 2) {
            return 6;
        } else { //detectors 3 and 4
            return 7;
        }
    }
    /**
     * Returns which channel in the adc given which detector and strip.
     */
    private int whichADCchannel(int detector, int strip){
        if (detector == 0 || detector==1 || detector==3) {//top half
            return strip+16;
        } else {//detectors 2 and 4, bottom half
            return strip;
        }
    }
    /**
     * Returns which channel in the tdc given which detector and strip.
     */
    private int whichTDCchannel(int detector, int strip){
        if (detector == 0 || detector==1 || detector==3) {//top half
            return strip+16;
        } else {//detetors 2 and 4, bottom half
            return strip;
        }
    }
    private void retrieveCalibration() throws IOException,
    ClassNotFoundException{
        String calibrationFile = "ArrayCalibration.obj";
        System.out.println("Attempting to retrieve calibration from: "+
        calibrationFile);
        ObjectInputStream ois = new ObjectInputStream(
        getClass().getResourceAsStream(calibrationFile));
        ac = (ArrayCalibration)ois.readObject();
        System.out.println("Calibration retrieved.  String representation = "+ac);
        ois.close();        
    }
    
    /** Called every time Jam has an event it wants to sort.
     * @param dataEvent contains all parameters for one event
     * @throws Exception so Jam can handle exceptions
     */
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
        hCthd.inc(eCthd);
        hAnde.inc(eAnde);
        hSntr1.inc(eSntr1);
        hSntr2.inc(eSntr2);
        hSntrSum.inc(eSntr);
        hFrntPsn.inc(eFPsn);
        hRearPsn.inc(eRPsn);
        hFrntHgh.inc(eFHgh);
        hRearHgh.inc(eRHgh);
        int ecYF=eYF>>TWO_D_FACTOR;
        hFrntY.inc(eYF);
        hRearY.inc(eYR);
        hRYvsFY.inc(ecYF,eYR>>TWO_D_FACTOR);
        hYvsPsn.inc(ecFPsn,ecYF);
        hFrntPH.inc(ecFPsn,ecFHgh);
        hRearPH.inc(ecRPsn,ecRHgh);
        hCthdAnde.inc(ecCthd,ecAnde);
        hSntrCthd.inc(ecSntr,ecCthd);
        hFrntCthd.inc(ecFPsn,ecCthd);
        hFrntAnde.inc(ecFPsn,ecAnde);
        hFrntSntr.inc(ecFPsn,ecSntr);
        hFrntPRearP.inc(ecFPsn,ecRPsn);


        int [][] eEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eEnergiesGA = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eTimesGA= new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

        boolean bSC = gSntrCthd.inGate(ecSntr,ecCthd);
        boolean bCA = gCthdAnde.inGate(ecCthd,ecAnde);
        boolean bFC = gFrntCthd.inGate(ecFPsn,ecCthd);
        boolean bFS = gFrntSntr.inGate(ecFPsn,ecSntr);
        boolean bPID = bSC && bFC && bFS && bCA;
        boolean bFR = gFrntRear.inGate(ecFPsn,ecRPsn) && !gFRexclude.inGate(ecFPsn,ecRPsn);
        boolean bGood = bPID && bFR;
        boolean bState = bGood && gPeak.inGate(eFPsn);

        //System.out.println("Declared silicon data storage: event "+event);
        int multiplicity=0;
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int stripBin = i*STRIPS_PER_DETECTOR+j;
                eEnergies[i][j]=dataEvent[idEnergies[i][j]];
                hEnergies[i][j].inc(eEnergies[i][j]);
                //hEvsStrip.inc(eEnergies[i][j]>>TWO_D_FACTOR,stripBin);
                eTimes[i][j]=dataEvent[idTimes[i][j]];
                hTimes[i][j].inc(eTimes[i][j]);
                boolean energy = eEnergies[i][j] >0  &&
                eEnergies[i][j] <= LAST_ADC_BIN;
                boolean time = eTimes[i][j] > 0;
                if (energy) {
                    hEnergyHits.inc(stripBin);
                    eEnergiesGA[i][j] = ac.getCalibratedEnergyChannel(i,j,
                    eEnergies[i][j]);
                    hEvsStripGA.inc(eEnergiesGA[i][j]>>TWO_D_FACTOR,stripBin);
                }
                if (time) hTimeHits.inc(stripBin);
                if (time && energy) {
                    hHits.inc(stripBin);
                    detHit[multiplicity]=i;
                    stripHit[multiplicity]=j;
                    bin[multiplicity]=stripBin;
                    multiplicity++;
                    double eDeposit = ac.getEnergyDeposited(i,j,eEnergies[i][j]);//deposited in det [MeV]
                    eTimesGA[i][j] = ac.getCalibratedTimeChannel(i,j,
                    eTimes[i][j]);
                    hTvsStrip.inc(eTimes[i][j] >> TWO_D_FACTOR,stripBin);
                    int ecTimeGA = eTimesGA[i][j] >> TWO_D_FACTOR;
                    int eChannel = (int)Math.round(eDeposit*20);
                    boolean bTimeBroad = bPID && gTimeBroad.inGate(ecTimeGA);
                    boolean bTimeState = bState && gTimeState.inGate(ecTimeGA);
                    boolean bEvsS= gEvsS.inGate(eChannel,j);
                    boolean bTimeDecay = bState && bEvsS &&
                    gTimeDecay.inGate(ecTimeGA);
                    if (bPID) hTimeGA.inc(ecTimeGA);
                    if (bTimeBroad) {
                        hEvsChBroad.inc(eChannel,stripBin);
                        hEvsStripBroad.inc(eChannel,j);
                        try {
                            int eCMchannel= getEcmChannel(eEnergies[i][j],i,j,eFPsn);
                            hEcmVsStripBroad.inc(eCMchannel,j);
                            hEcmVsFP.inc(ecFPsn,eCMchannel/10);
                            hEsilVsFP.inc(ecFPsn,(int)Math.round(ac.getEnergyDeposited(i,j,eEnergies[i][j])*10));
                        } catch (MathException me){
                            System.err.println(me);
                        }
                        hFrntGTime.inc(eFPsn);
                        hcFrntGTime.inc(eFPsn>>COMPRESS_FACTOR);
                    }
                    if (bState) {
                        hTimeGAstate.inc(ecTimeGA);
                    }
                    if (bTimeState) {
                        hEvsChState.inc(eChannel,stripBin);
                        hEvsStripState.inc(eChannel,j);
                    }
                    if (bState && bEvsS){
                        hTimeGAdecay.inc(ecTimeGA);
                    }
                    if (bTimeDecay) {
                        hEvsChDecay.inc(eChannel,stripBin);
                        hEvsStripDecay.inc(eChannel,j);
                        hAngDist.inc(stripBin);
                    }
                    hTvsStripGA.inc(ecTimeGA,stripBin);
                }
            }
        }
        /************
        Processing of T vs. E hits diagnostic plot
         ************/
        for (int det=0; det<NUM_DETECTORS; det++){
            for (int strip=0; strip<STRIPS_PER_DETECTOR; strip++){
                if (eTimes[det][strip]>0) {
                    for (int Estrip=0; Estrip<STRIPS_PER_DETECTOR; Estrip++) {
                        if (eEnergies[det][Estrip] > 0  &&
                        eEnergies[det][Estrip] < LAST_ADC_BIN) {
                            hTvsEhits.inc(STRIPS_PER_DETECTOR*det+Estrip,
                            STRIPS_PER_DETECTOR*det+strip);
                        }
                    }
                }
            }
        }
        numInterHits=0;
        if (multiplicity > 1){
            for (int m=0; m< multiplicity; m++){
                for (int n=m+1; n<multiplicity; n++){
                    if (detHit[m]==detHit[n]){
                        int diff = Math.abs(stripHit[m]-stripHit[n]);
                        if (diff==1){
                            interDetHit[numInterHits]=detHit[m];
                            interStripHit[numInterHits]=stripHit[m];
                            interBin[numInterHits]=bin[m];
                            numInterHits++;
                        }
                    }
                }
            }
        }

        for (int i=0; i< numInterHits; i++) hInterHits.inc(interBin[i]);
        hMultiplicity.inc(multiplicity);


        if (bSC && bFR) {// gate on Scintillator vs Cathode
            hFrntSntrGSC.inc(ecFPsn,ecSntr);
            hFrntCthdGSC.inc(ecFPsn,ecCthd);
        }
        if (bFC && bFR) {// gate on Front Wire Position vs Cathode
            hSntrCthdGFC.inc(ecSntr,ecCthd);
            hFrntSntrGFC.inc(ecFPsn,ecSntr);
        }
        if (bFS && bFR){// gate on Front Wire Position vs Scintillator
            hSntrCthdGFS.inc(ecSntr,ecCthd);
            hFrntCthdGFS.inc(ecFPsn,ecCthd);
        }
        if (bPID) {// gated on all 3 gate above
            //writeEvent(dataEvent);
            hFrntGCSF.inc(eFPsn);
            hRearGCSF.inc(eRPsn);
            hFrntRearGCSF.inc(ecFPsn,ecRPsn);
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
    
    /**
     * Returns channel = Ecm(keV)/10
     */
    private int getEcmChannel(int eChannelRaw, int det, int strip, int FPchannel) 
    throws MathException {
        double qbr=13.898/13.8864*(1828.13+0.25226*FPchannel);
        rk.setQBr(qbr); double beta=rk.getResidualBeta();
        double [] beta4={beta,0,0,beta};
        Boost boost=new Boost(beta4);
        double Tlab = ac.getEnergyDeposited(det,strip,eChannelRaw);
        double [] plab = Boost.make4Momentum(Tlab,mAlpha,ac.getTheta(strip),ac.getPhi(det));
        double [] pcm = boost.transformVector(plab);
        double Tcm = pcm[0]-mAlpha;
        return (int)Math.round(Tcm*100);
    }
    
}
