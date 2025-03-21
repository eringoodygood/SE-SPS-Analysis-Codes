/*
 */
package sort.coinc.offline;
import jam.data.*;
import jam.sort.*;
import java.io.*;

/*
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified October 2000 by Dale Visser
 */
public class YLSA_etcal extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000};
static final int [] TDC_BASE = {0x30000000,0x30010000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 300;//ADC lower threshold in channels
    static final int TIME_THRESHOLDS = 30;//TDC lower threshold in channels
    static final int TIME_RANGE = 600;//ns
    static final int LAST_GOOD_BIN = 3840;

    //LEDA Detectors
    static final int NUM_DETECTORS = 3;
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

    //double [][] Am241E = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    //double [][] Am241T = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];

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
    Histogram hSilEvsFront;

    Gate gSilicon, gCthd, gPeak, gGood;//gates 1 d
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear;//gates 2 d
    Gate gState1,gState2;//gates on 2 16O "states" in Esil vs. FP spectrum
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors

    Gate gTime;
    Histogram hCthdFrntGT;
    ///Histogram [] hTimeGPID = new Histogram[NUM_DETECTORS];
    Histogram hHits, hEvsStrip, hTvsStrip, hTimeHits, hTvsEhits, hInterHits;
    Histogram hEvsStripGA, hTvsStripGA; //gain adjusted spectra
    Histogram hEvsStripGAT1, hEvsStripGAT2;//gain adjusted with time coincidence with good PID required

    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh, idRearHgh, idSilicon, idNaI1, idNaI2;
    int idFrontR, idFrontL, idRearR, idRearL,idDummy;
    //int NUM_PARAMETERS;

    int lastEvntAccpt;
    boolean firstTimeThru=true;
    double [][] energyGain = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] energyOffset = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] timeOffset = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] timeGain = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];


    public void initialize() throws Exception {
        try{
            LineNumberReader lr=new LineNumberReader(new FileReader("/data/jan01/calibration/calib.out"));
            lr.readLine(); lr.readLine();//skip first 2 lines
            StreamTokenizer st = new StreamTokenizer(new BufferedReader(lr));
            st.eolIsSignificant(false); //treat end of line as white space
                System.out.println("------\nEnergy\n------");
            do {
                st.nextToken();
                if (st.ttype != StreamTokenizer.TT_EOF){
                    int det=(int)Math.round(st.nval);
                    st.nextToken(); int str=(int)Math.round(st.nval);
                    st.nextToken(); double offset=st.nval;
                    st.nextToken(); double gain=st.nval;
                    System.out.println("Det "+det+", Strip "+str+", offset = "+offset+", gain = "+gain);
                    energyGain[det][str]=gain;
                    energyOffset[det][str]=offset;                    
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
        } catch (IOException e) {
            System.err.println(e);
        }
        try{
            LineNumberReader lr=new LineNumberReader(new FileReader("/data/jan01/calibration/timeCal.out"));
            lr.readLine(); lr.readLine();//skip first 2 lines
            StreamTokenizer st = new StreamTokenizer(new BufferedReader(lr));
            st.eolIsSignificant(false); //treat end of line as white space
                System.out.println("----\nTime\n----");
            do {
                st.nextToken();
                if (st.ttype != StreamTokenizer.TT_EOF){
                    int det=(int)Math.round(st.nval);
                    st.nextToken(); int str=(int)Math.round(st.nval);
                    st.nextToken(); double offset=st.nval;
                    st.nextToken(); double gain=st.nval;
                    System.out.println("Det "+det+", Strip "+str+", offset = "
                        +offset+", gain = "+gain);
                    timeOffset[det][str]=offset;
                    timeGain[det][str]=gain;
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
        } catch (IOException e) {
            System.err.println(e);
        }
        vmeMap.setScalerInterval(3);
        for (int i=0; i < TDC_BASE.length; i++){
            vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
        }
        //Focal Plane Detector
        idCthd=vmeMap.eventParameter(0, ADC_BASE[0], 0, THRESHOLDS);
        idAnde=vmeMap.eventParameter(1, ADC_BASE[0], 1, THRESHOLDS);
        idScintR=vmeMap.eventParameter(2, ADC_BASE[0], 2, THRESHOLDS);
        idScintL=vmeMap.eventParameter(3, ADC_BASE[0], 3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(4, ADC_BASE[0], 4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(5, ADC_BASE[0], 5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(6, ADC_BASE[0], 6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(7, ADC_BASE[0], 7, THRESHOLDS);
        idDummy=vmeMap.eventParameter(8, ADC_BASE[0], 8, THRESHOLDS);
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


        //LEDA Array
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int Eparam = 0x10 + i*STRIPS_PER_DETECTOR + j;
                idStrips[i][j]=vmeMap.eventParameter(Eparam, whichADC(i), whichChannel(i,j), THRESHOLDS);
                hStrips[i][j]=new Histogram("E_D"+i+"_S"+j, HIST_1D_INT, ADC_CHANNELS,
                "Detector "+i+", Strip "+j);
            }
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int Tparam = 0x110 + i*STRIPS_PER_DETECTOR + j;
                idTimes[i][j]=vmeMap.eventParameter(Tparam, whichTDC(i), whichTDCchannel(i,j), TIME_THRESHOLDS);
                hTimes[i][j]=new Histogram("T_D"+i+"_S"+j, HIST_1D_INT, ADC_CHANNELS, "Detector "+i+", Strip "+j+" time");
            }
        }

        hHits = new Histogram("Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over ADC threshold",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hInterHits = new Histogram("InterHits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Inter-Strip hits",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hTimeHits = new Histogram("Time Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over TDC threshold",
        "Strip","Counts");
        hTvsEhits = new Histogram("T vs E hits", HIST_2D_INT, 3*STRIPS_PER_DETECTOR, "Time hits vs Energy hits",
        "E hits", "T hits");
        hEvsStrip = new Histogram("EvsStrip",HIST_2D_INT, TWO_D_CHANNELS, "Energy vs. Strip, All Detectors", "Energy",
        STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStrip = new Histogram("TvsStrip",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        hEvsStripGA = new Histogram("EvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,
        "Energy vs. Strip, All Detectors, Gain Adjusted", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hEvsStripGAT1 = new Histogram("EvsStripGAT1",HIST_2D_INT, TWO_D_CHANNELS,
        "Energy vs. Strip, All Detectors, Gain Adjusted, Time Coincidence 1", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hEvsStripGAT2 = new Histogram("EvsStripGAT2",HIST_2D_INT, TWO_D_CHANNELS,
        "Energy vs. Strip, All Detectors, Gain Adjusted, Time Coincidence 2", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStripGA = new Histogram("TvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one, Gain Adjusted", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        gTime=new Gate("Time", hTvsStripGA);//gates on selected TDC channels
        hCthdFrntGT=new Histogram("CthdFrntGT", HIST_2D_INT, TWO_D_CHANNELS, "Cathode vs. Position, gated on: "+gTime,
        "Position", "Cathode");
        hSilEvsFront = new Histogram("SilEvsFront",HIST_2D_INT, TWO_D_CHANNELS,"Silicon Energy vs. FW Position, multiplicity one, PID gate, time gate");

        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gPeak   =new Gate("Peak", hFrntGCSF);
        gGood  =new Gate("GoodEvent",hFrntGAll);
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
        gState1=new Gate("State1",hSilEvsFront);
        gState2=new Gate("State2",hSilEvsFront);
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
        if (detector == 0) {//Detector 2
            return TDC_BASE[0];
        } else {//Detectors 0 and 1
            return TDC_BASE[1];
        }
    }

    /**
     * Returns which channel in the adc given which detector and strip.
     */
    private int whichChannel(int detector, int strip){
        if (detector == 0 || detector==1) {//Detectors 0 and 2
            return strip+16;
        } else {//Detector 1
            return strip;
        }
    }
    /**
     * Returns which channel in the tdc given which detector and strip.
     */
    private int whichTDCchannel(int detector, int strip){
        if (detector == 0 || detector==1) {//Detectors 0 and 2
            return strip+16;
        } else {//Detector 1
            return strip;
        }
    }

    public void sort(int [] dataEvent) throws Exception {
        int [][] eEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eEnergiesGA = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eTimesGA= new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        boolean coincidence=false;//set to true when good PID and time in 2d gate
        boolean PID=false;//set to true when good PID plus FW/RW info
        int goodPIDstripBin=0;//value set when good PID

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

        boolean multiplicityOne=false;
        boolean higherMultiplicity=false;
        boolean interStripEvent=false;
        int firstStripHit=-1;
        int firstDetHit=-1;
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                eEnergies[i][j]=dataEvent[idStrips[i][j]];
                eTimes[i][j]=dataEvent[idTimes[i][j]];
                eEnergiesGA[i][j]=Math.max(0,
                (int)Math.round((eEnergies[i][j]-energyOffset[i][j])*energyGain[i][j]));
                if (eTimes[i][j]==0) {
                    eTimesGA[i][j]=0;
                } else {
                    eTimesGA[i][j]=Math.max(0,(int)Math.round(timeOffset[i][j]+timeGain[i][j]*eTimes[i][j]));
                }
                hStrips[i][j].inc(eEnergies[i][j]);
                hTimes[i][j].inc(eTimes[i][j]);
                int stripBin = i*STRIPS_PER_DETECTOR+j;
                if (eEnergies[i][j] > energyOffset[i][j]) {
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
                if (eTimes[i][j] >0){
                    hTimeHits.inc(stripBin);
                }
                hEvsStrip.inc(eEnergies[i][j]>>TWO_D_FACTOR,stripBin);
                hEvsStripGA.inc(eEnergiesGA[i][j]>>TWO_D_FACTOR,stripBin);
                int ecTime=eTimes[i][j]>>TWO_D_FACTOR;
                if (gTime.inGate(ecTime,stripBin)) {
                    hCthdFrntGT.inc(ecFPsn,ecCthd);
                }
            }
        }
        /*if (multiplicityOne) {
            int ecTime=eTimes[firstDetHit][firstStripHit]>>TWO_D_FACTOR;
            int stripBinT=firstDetHit*STRIPS_PER_DETECTOR+firstStripHit;
            hTvsStrip.inc(ecTime,stripBinT);
            hTvsStripGA.inc(eTimesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR,stripBinT);
        } else if (interStripEvent){
            int stripBin=firstDetHit*STRIPS_PER_DETECTOR+firstStripHit;
            hInterHits.inc(stripBin);
        }*/
        for (int det=0; det<NUM_DETECTORS; det++){
            for (int str=0; str<STRIPS_PER_DETECTOR; str++){
                int ecTime=eTimes[det][str]>>TWO_D_FACTOR;
                int stripBinT=det*STRIPS_PER_DETECTOR+str;
                hTvsStrip.inc(ecTime,stripBinT);
                hTvsStripGA.inc(eTimesGA[det][str]>>TWO_D_FACTOR,stripBinT);
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
            PID=true;
            hFrntGAll.inc(eFPsn);
            hRearGAll.inc(eRPsn);
            hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
            hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
            goodPIDstripBin=firstDetHit*STRIPS_PER_DETECTOR+firstStripHit;
            if (multiplicityOne &&gTime.inGate(eTimesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR,goodPIDstripBin)){
                hFrntGTime.inc(eFPsn);
                hcFrntGTime.inc(eFPsn>>COMPRESS_FACTOR);
                if (firstStripHit==12 && firstDetHit==0){
                    hSilEvsFront.inc(ecFPsn,eEnergiesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR);
                }
                coincidence=true;
            }
        }

        /*if (PID && multiplicityOne){
            writeEvent(dataEvent);
        }*/

        if (coincidence) {
            if (gState1.inGate(ecFPsn,eEnergiesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR)){
                hEvsStripGAT1.inc(eEnergiesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR,goodPIDstripBin);
            } else if (gState2.inGate(ecFPsn,eEnergiesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR)){
                hEvsStripGAT2.inc(eEnergiesGA[firstDetHit][firstStripHit]>>TWO_D_FACTOR,goodPIDstripBin);
            }
        }

        int [] energy=new int[STRIPS_PER_DETECTOR];
        int [] time = new int[STRIPS_PER_DETECTOR];
        for (int i=0;i<NUM_DETECTORS;i++) {
            for  (int eStrip = 0; eStrip<STRIPS_PER_DETECTOR; eStrip++){
                energy[eStrip]=eEnergies[i][eStrip];
                for (int tStrip=0; tStrip<STRIPS_PER_DETECTOR; tStrip++){
                    time[tStrip] = eTimes[i][tStrip];
                    boolean tHit = time[tStrip] >= TIME_THRESHOLDS && time[tStrip] <= LAST_GOOD_BIN;
                    boolean eHit = energy[eStrip] >= THRESHOLDS && energy[eStrip] <= LAST_GOOD_BIN;
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
