/*
 */
package sort.coinc;
import jam.data.*;
import jam.sort.*;

/*
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified October 2000 by Dale Visser
 */
public class YLSAtest extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
static final int [] TDC_BASE = {0x30000000,0x30010000,0x30020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int DEFAULT_THRESHOLD = 256;    //ADC lower threshold in channels
    static final int TIME_THRESHOLD = 25;   //TDC lower threshold in channels
    static final int SCINT_THRESHOLD = 25;     //lower than for silicon
    static final int TIME_RANGE = 600;//ns
    static final int LAST_GOOD_BIN = 3840;

    //LEDA Detectors
    static final int NUM_DETECTORS = 5;
    static final int STRIPS_PER_DETECTOR = 16;
    static final int SAMPLE_STRIP = 4;
    final double AM241ENERGY = 5412.0;//keV

    //histogramming constants
    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed histogram
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    Histogram [][] hStrips = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    Histogram [][] hTimes = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] idStrips = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] idTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

    double [][] Am241E = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] Am241T = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];

    // ungated spectra
    Histogram hCthd,hAnde,hSntr1,hSntr2,hSntrSum, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hSntrCthd, hFrntCthd, hFrntAnde, hFrntSntr, hFrntPRearP;

    Histogram hFrntSntrGSC, hFrntCthdGSC;//gate by scintillator cathode
    Histogram hSntrCthdGFC, hFrntSntrGFC;//gate by Front wire Cathode
    Histogram hSntrCthdGFS, hFrntCthdGFS;//gate by Front wire Scintillator
    Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed & time
    Histogram hcFrntGTime, hFrntGTime;//front and rear wire gated on All compressed & time

    Gate gSilicon, gCthd, gPeak, gGood;//gates 1 d
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear;//gates 2 d
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors

    Gate gTime;
    Histogram hCthdFrntGT;
    ///Histogram [] hTimeGPID = new Histogram[NUM_DETECTORS];
    Histogram hHits, hEvsStrip, hTvsStrip, hTimeHits, hTvsEhits, hInterHits;
    Histogram hEvsStripGA, hTvsStripGA; //gain adjusted spectra
    Histogram hEvsStripGTime;
    Histogram hTvsStripPID, hTvsStripAll;
    Histogram hSilEvsFront;
    
    Histogram hDebug; Gate gDebug;
    
    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh, idRearHgh, idSilicon, idNaI1, idNaI2;
    int idFrontR, idFrontL, idRearR, idRearL,idDummy;
    //int NUM_PARAMETERS;

    int lastEvntAccpt;
    boolean firstTimeThru=true;
    double [][] energyGain = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] timeOffset = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];


    public void initialize() throws Exception {
        vmeMap.setScalerInterval(2);
        for (int i=0; i < TDC_BASE.length; i++){
            vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
        }
        //Focal Plane Detector
        idCthd=vmeMap.eventParameter(0, ADC_BASE[0], 0, DEFAULT_THRESHOLD);
        idAnde=vmeMap.eventParameter(1, ADC_BASE[0], 1, DEFAULT_THRESHOLD);
        idScintR=vmeMap.eventParameter(2, ADC_BASE[0], 2, SCINT_THRESHOLD);
        idScintL=vmeMap.eventParameter(3, ADC_BASE[0], 3, SCINT_THRESHOLD);
        idFrntPsn=vmeMap.eventParameter(4, ADC_BASE[0], 4, DEFAULT_THRESHOLD);
        idRearPsn=vmeMap.eventParameter(5, ADC_BASE[0], 5, DEFAULT_THRESHOLD);
        idFrntHgh=vmeMap.eventParameter(6, ADC_BASE[0], 6, DEFAULT_THRESHOLD);
        idRearHgh=vmeMap.eventParameter(7, ADC_BASE[0], 7, DEFAULT_THRESHOLD);
        idDummy=vmeMap.eventParameter(8, ADC_BASE[0], 8, DEFAULT_THRESHOLD);
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
        hFrntGTime  = new Histogram("FrontGTime    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw & time gates");
        hcFrntGTime  =new Histogram("FrontGTimecmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw & time gates");

        Am241E[0][0]=270.36*8;
        Am241E[0][1]=260.79*8;
        Am241E[0][2]=293.54*8;
        Am241E[0][3]=291.65*8;
        Am241E[0][4]=266.34*8;
        Am241E[0][5]=256.37*8;
        Am241E[0][6]=270.20*8;
        Am241E[0][7]=271.09*8;
        Am241E[0][8]=260.92*8;
        Am241E[0][9]=260*8;
        Am241E[0][10]=273.88*8;
        Am241E[0][11]=276.25*8;
        Am241E[0][12]=254.99*8;
        Am241E[0][13]=264.5*8;
        Am241E[0][14]=264.68*8;
        Am241E[0][15]=272.39*8;
        Am241T[0][0]=205*8;
        Am241T[0][1]=202*8;
        Am241T[0][2]=205*8;
        Am241T[0][3]=199*5;
        Am241T[0][4]=203*8;
        Am241T[0][5]=197*8;
        Am241T[0][6]=198*8;
        Am241T[0][7]=202*8;
        Am241T[0][8]=199.6*8;
        Am241T[0][9]=200*8;
        Am241T[0][10]=197.8*8;
        Am241T[0][11]=205*8;
        Am241T[0][12]=207*8;
        Am241T[0][13]=101.1*8;
        Am241T[0][14]=197*8;
        Am241T[0][15]=204*8;

        Am241E[1][0]=270*8;
        Am241E[1][1]=270*8;
        Am241E[1][2]=287.17*8;
        Am241E[1][3]=278.74*8;
        Am241E[1][4]=271.99*8;
        Am241E[1][5]=304.07*8;
        Am241E[1][6]=273.69*8;
        Am241E[1][7]=282.88*8;
        Am241E[1][8]=268.27*8;
        Am241E[1][9]=282.24*8;
        Am241E[1][10]=287.52*8;
        Am241E[1][11]=275.31*8;
        Am241E[1][12]=271.35*8;
        Am241E[1][13]=275.38*8;
        Am241E[1][14]=265.68*8;
        Am241E[1][15]=281.30*8;
        Am241T[1][0]=202*8;
        Am241T[1][1]=202*8;
        Am241T[1][2]=202*8;
        Am241T[1][3]=198*5;
        Am241T[1][4]=184.5*8;
        Am241T[1][5]=202.7*8;
        Am241T[1][6]=194*8;
        Am241T[1][7]=198*8;
        Am241T[1][8]=192.9*8;
        Am241T[1][9]=198.2*8;
        Am241T[1][10]=194*8;
        Am241T[1][11]=203*8;
        Am241T[1][12]=189.3*8;
        Am241T[1][13]=168*8;
        Am241T[1][14]=192.4*8;
        Am241T[1][15]=198*8;

        Am241E[2][0]=271.84*8;
        Am241E[2][1]=242.49*8;
        Am241E[2][2]=271.43*8;
        Am241E[2][3]=279.65*8;
        Am241E[2][4]=275.25*8;
        Am241E[2][5]=278.01*8;
        Am241E[2][6]=265.36*8;
        Am241E[2][7]=284.9*8;
        Am241E[2][8]=267.08*8;
        Am241E[2][9]=273.23*8;
        Am241E[2][10]=275.26*8;
        Am241E[2][11]=267.99*8;
        Am241E[2][12]=272.1*8;
        Am241E[2][13]=271.3*8;
        Am241E[2][14]=266.5*8;
        Am241E[2][15]=282.58*8;
        Am241T[2][0]=207.1*8;
        Am241T[2][1]=197*8;
        Am241T[2][2]=208*8;
        Am241T[2][3]=207*5;
        Am241T[2][4]=195*8;
        Am241T[2][5]=202*8;
        Am241T[2][6]=194*8;
        Am241T[2][7]=206*8;
        Am241T[2][8]=197*8;
        Am241T[2][9]=191*8;
        Am241T[2][10]=194.8*8;
        Am241T[2][11]=197*8;
        Am241T[2][12]=198*8;
        Am241T[2][13]=179*8;
        Am241T[2][14]=190*8;
        Am241T[2][15]=198*8;

        //LEDA Array
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int Eparam = 0x10 + i*STRIPS_PER_DETECTOR + j;
                idStrips[i][j]=vmeMap.eventParameter(Eparam, whichADC(i), whichChannel(i,j), DEFAULT_THRESHOLD);
                hStrips[i][j]=new Histogram("E_D"+i+"_S"+j, HIST_1D_INT, COMPRESSED_CHANNELS,
                "Detector "+i+", Strip "+j);
                if (Am241E[i][j] != 0.0) {//values are zero unless initialized
                    energyGain[i][j] = (AM241ENERGY/3.0)/Am241E[i][j];
                } else {
                    energyGain[i][j] = 1.0;
                }
                if (Am241T[i][j] != 0.0) {//values are zero unless initialized
                    timeOffset[i][j] = Am241T[i][j];
                } else{
                    timeOffset[i][j] = 0.0;
                }
                //System.err.println("Det "+i+" Strip "+j+" energyGain = "+energyGain[i][j]+ "timeGain = "+timeGain[i][j]);
            }
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int Tparam = 0x110 + i*STRIPS_PER_DETECTOR + j;
                idTimes[i][j]=vmeMap.eventParameter(Tparam, whichTDC(i), whichTDCchannel(i,j), TIME_THRESHOLD);
                hTimes[i][j]=new Histogram("T_D"+i+"_S"+j, HIST_1D_INT, COMPRESSED_CHANNELS, "Detector "+i+", Strip "+j+" time");
            }
        }

        hHits = new Histogram("Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over ADC threshold",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hInterHits = new Histogram("InterHits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Inter-Strip hits",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hTimeHits = new Histogram("Time Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over TDC threshold",
        "Strip","Counts");
        hTvsEhits = new Histogram("T vs E hits", HIST_2D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Time hits vs Energy hits",
        "E hits", "T hits");
        hEvsStrip = new Histogram("EvsStrip",HIST_2D_INT, TWO_D_CHANNELS, "Energy vs. Strip, All Detectors", "Energy",
        STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStrip = new Histogram("TvsStrip",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStripPID = new Histogram("TvsStripPID",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one,PID gates", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStripAll = new Histogram("TvsStripAll",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, any multiplicity", "Time", 
        STRIPS_PER_DETECTOR+"*Det+Strip");
        hEvsStripGA = new Histogram("EvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,
        "Energy vs. Strip, All Detectors, Gain Adjusted", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hEvsStripGTime = new Histogram("EvsStripGt",HIST_2D_INT, TWO_D_CHANNELS,
        "Energy vs. Strip, All Detectors, gated on time", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStripGA = new Histogram("TvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one, Gain Adjusted", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        gTime=new Gate("Time", hTvsStripGA);//gates on selected TDC channels
        hCthdFrntGT=new Histogram("CthdFrntGT", HIST_2D_INT, TWO_D_CHANNELS, "Cathode vs. Position, gated on: "+gTime,
        "Position", "Cathode");
        hSilEvsFront = new Histogram("SilEvsFront",HIST_2D_INT, TWO_D_CHANNELS,
        "Silicon Energy vs. FW Position, multiplicity one, PID gate, time gate",
        "Position","E in Si");
        hDebug = new Histogram("Debug", HIST_1D_INT, 512, "Debug Word");
        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gPeak   =new Gate("Peak", hFrntGCSF);
        gGood  =new Gate("GoodEvent",hFrntGAll);
        gDebug = new Gate("Debug",hDebug);
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
        Monitor mDebug=new Monitor("Debug", gDebug);
    }

    //Utility methods for mapping strips to ADC channels

    /**
     * Returns which adc unit given which detector.
     */
    private int whichADC(int detector){
        if (detector == 0) {//Detector 0
            return ADC_BASE[0];
        } else if (detector == 1 || detector == 2) {//Detectors 1 and 2
            return ADC_BASE[1];
        } else {//detectors 3 and 4
            return ADC_BASE[2];
        }
    }
    /**
     * Returns which tdc unit given which detector.
     */
    private int whichTDC(int detector){
        if (detector == 0) {//Detector 0
            return TDC_BASE[0];
        } else if (detector == 1 || detector == 2){//Detectors 1  and 2
            return TDC_BASE[1];
        } else {//detectors 3 and 4
            return TDC_BASE[2];
        }
    }

    /**
     * Returns which channel in the adc given which detector and strip.
     */
    private int whichChannel(int detector, int strip){
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

    public void sort(int [] dataEvent) throws Exception {
        boolean bPID=false;  //whether particle id has been satisfied
        int [][] eStrips = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eStripsGA = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eTimesGA= new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

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
        int debugWord = dataEvent[0x200];
        if (debugWord >0) {
            hDebug.inc(debugWord);//debug word
        }

        int eSntr=(int)Math.round(Math.sqrt(eSntr1*eSntr2));

        int ecFPsn=eFPsn>>TWO_D_FACTOR;
        int ecRPsn=eRPsn>>TWO_D_FACTOR;
        int ecFHgh=eFHgh>>TWO_D_FACTOR;
        int ecRHgh=eRHgh>>TWO_D_FACTOR;
        int ecSntr=eSntr>>TWO_D_FACTOR;
        int ecCthd=eCthd>>TWO_D_FACTOR;
        int ecAnde=eAnde>>TWO_D_FACTOR;

        boolean multiplicityOne=false;
        boolean higherMultiplicity=false;
        boolean interStripEvent=false;
        int firstStripHit=-1;
        int firstDetHit=-1;
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                eStrips[i][j]=dataEvent[idStrips[i][j]];
                eTimes[i][j]=dataEvent[idTimes[i][j]];
                eStripsGA[i][j]=(int)Math.round(eStrips[i][j]*energyGain[i][j]);
                eTimesGA[i][j]=(int)Math.round(eTimes[i][j]+2048.0-timeOffset[i][j]);
                hStrips[i][j].inc(eStrips[i][j]>>COMPRESS_FACTOR);
                hTimes[i][j].inc(eTimes[i][j]>>COMPRESS_FACTOR);
                int stripBin = i*STRIPS_PER_DETECTOR+j;
                if (eStrips[i][j] > 0) {
                    hHits.inc(stripBin);
                    if (eTimes[i][j] >0){
                        if (multiplicityOne) {//2nd time a time/E coinc seen
                            multiplicityOne=false;
                            higherMultiplicity=true;
                            if (i==firstDetHit && j==firstStripHit+1) interStripEvent = true;
                        } else {//first time a time/E coinc seen
                            multiplicityOne=true;
                            firstDetHit=i;
                            firstStripHit=j;
                        }

                    }
                }
                int ecTime=eTimes[i][j]>>TWO_D_FACTOR;
                if (eTimes[i][j] >0){
                    hTimeHits.inc(stripBin);
                    hTvsStripAll.inc(ecTime,stripBin);
                }
                hEvsStrip.inc(eStrips[i][j]>>TWO_D_FACTOR,stripBin);
                hEvsStripGA.inc(eStripsGA[i][j]>>TWO_D_FACTOR,stripBin);
                if (gTime.inGate(ecTime,stripBin)) {
                    hCthdFrntGT.inc(ecFPsn,ecCthd);
                    hEvsStripGTime.inc(eStrips[i][j]>>TWO_D_FACTOR,stripBin);
                }

            }
        }
        if (multiplicityOne) {
            int ecTime=eTimes[firstDetHit][firstStripHit]>>TWO_D_FACTOR;
            int stripBin=firstDetHit*STRIPS_PER_DETECTOR+firstStripHit;
            hTvsStrip.inc(ecTime,stripBin);
            hTvsStripGA.inc(eTimesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR,stripBin);
        } else if (interStripEvent){
            int stripBin=firstDetHit*STRIPS_PER_DETECTOR+firstStripHit;
            hInterHits.inc(stripBin);
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
            bPID=true;
            hFrntGAll.inc(eFPsn);
            hRearGAll.inc(eRPsn);
            hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
            hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
            int stripBin=firstDetHit*STRIPS_PER_DETECTOR+firstStripHit;
            if (multiplicityOne &&gTime.inGate(eTimesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR,stripBin)){
                hFrntGTime.inc(eFPsn);
                hcFrntGTime.inc(eFPsn>>COMPRESS_FACTOR);
                if (firstDetHit==0 || firstDetHit==1 || firstDetHit ==4){
                    hSilEvsFront.inc(ecFPsn,eStripsGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR);
                }
            }
        }

        int [] energy=new int[STRIPS_PER_DETECTOR];
        int [] time = new int[STRIPS_PER_DETECTOR];
        for (int i=0;i<NUM_DETECTORS;i++) {
            for  (int eStrip = 0; eStrip<STRIPS_PER_DETECTOR; eStrip++){
                if (bPID) {
                    hTvsStripPID.inc(eTimes[i][eStrip]>>COMPRESS_FACTOR,16*i+eStrip);
                }
                energy[eStrip]=eStrips[i][eStrip];
                for (int tStrip=0; tStrip<STRIPS_PER_DETECTOR; tStrip++){
                    time[tStrip] = eTimes[i][tStrip];
                    boolean tHit = time[tStrip] >= TIME_THRESHOLD && time[tStrip] <= LAST_GOOD_BIN;
                    boolean eHit = energy[eStrip] >= DEFAULT_THRESHOLD && energy[eStrip] <= LAST_GOOD_BIN;
                    if (tHit && eHit) hTvsEhits.inc(eStrip+STRIPS_PER_DETECTOR*i,tStrip+STRIPS_PER_DETECTOR*i);
                }
            }
        }
    }

    /**
     * monitor method
     * calculate the live time
     */
    public double monitor(String name){
        if (name.equals("Live Time")){
            if (((double)(mEvntRaw.getValue()))>0.0){
                return 100.0*mEvntAccept.getValue()/mEvntRaw.getValue();
            } else {
                return 0.0;
            }
        } else {
            return 50.0;
        }
    }
}
