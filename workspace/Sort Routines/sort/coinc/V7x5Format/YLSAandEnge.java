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

/** 
 * Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.YLSAandEnge</CODE>, which
 * was used prior to the changes in the VME code.  Now the data shipped across
 * is in the format as produced by the V7x5's.  The EventInputStream this code
 * works with is YaleCAEN_InputStream.
 *
 * @author Dale Visser
 * @since 16 Feb 2001
 */
public class YLSAandEnge extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
static final int [] TDC_BASE = {0x30000000,0x30010000,0x30020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 500;//ADC lower threshold in channels
    static final int TIME_THRESHOLDS = 30;//TDC lower threshold in channels
    static final int TIME_RANGE = 600;//ns
    static final int LAST_GOOD_BIN = 3840;

    //LEDA Detectors
    static final int NUM_DETECTORS = 5;
    static final int STRIPS_PER_DETECTOR = 16;
    static final int SAMPLE_STRIP = 4;
    //final double AM241ENERGY = 5412.0;//keV

    //names
    static final String DEAD_TIME="Dead Time %";
    
    //histogramming constants
    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed histogram
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
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
    Histogram hMultiplicity;
    Histogram hEvsStripGA, hTvsStripGA; //gain adjusted spectra
    Histogram hTvsStripPID;
    Histogram hSilEvsFront;//to see that we satisfy total energy conditions
    
    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh, idRearHgh;
    //int idFrontR, idFrontL, idRearR, idRearL,idDummy;

    boolean firstTimeThru=true;

    //nuclear properties for physics analysis
    //objects representing carbon-12 nucleus and alpha
    //Nucleus C12,proton, He6;
    //Nucleus proton;
    //masses of alpha particle and oxygen-16 ground states
    double Mproton, M16;
    //object for calculating losses in YLSA dead layer
    //EnergyLoss deadLayerLoss;
    //calibration of gain adjusted energy channels and time channels
    double CHANNELS_PER_MEV = 500.0;//dummy value
    static final double C = 299.792458; //speed of light in mm/nsec
    static final double CHANNELS_PER_NS = 6.434;
    //gains and offsets for calibrations
    double [][] energyGain = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] energyOffset = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] timeGain = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] timeOffset = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    //geometry of YLSA array
    double [] theta=new double[STRIPS_PER_DETECTOR];//lab angle
    double [] incidence=new double[STRIPS_PER_DETECTOR];//1/cos(inc angle)
    double [] distance=new double[STRIPS_PER_DETECTOR];//distance in mm

    int multiplicity;//number of real hits (in TDC window)
    int [] detHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] stripHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] bin = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int numInterHits;//number of real interStrip hits (in TDC window)
    int [] interDetHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] interStripHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] interBin = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    
    /** Sets up objects, called when Jam loads the sort routine.
     * @throws Exception necessary for Jam to handle exceptions
     */
    public void initialize() throws Exception {        
        /*new ResidualKinematics(); new EnergyLoss(); //set up kinematics/ nuclear / eloss stuff
        proton = new Nucleus(1,1);
        Mproton = proton.getMass().value;
        Solid deadLayer = new Solid(0.2*1.0e-4, Absorber.CM, "Al");//0.2 um
        deadLayerLoss = new EnergyLoss(deadLayer);*/
        try{//read in energy calibration
            LineNumberReader lr=new LineNumberReader(new FileReader("/data/may01/YLSAenergies.dat"));
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
            LineNumberReader lr=new LineNumberReader(new FileReader("/data/may01/YLSAtimes.dat"));
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
        try{
            LineNumberReader lr=new LineNumberReader(new FileReader("/data/jan01/calibration/monte.txt"));
            lr.readLine(); lr.readLine();//skip first 2 lines
            StreamTokenizer st = new StreamTokenizer(new BufferedReader(lr));
            st.eolIsSignificant(false); //treat end of line as white space
            System.out.println("----\nAngles\n----");
            do {
                st.nextToken();//should be strip
                if (st.ttype != StreamTokenizer.TT_EOF){
                    int str=(int)Math.round(st.nval);
                    st.nextToken();//skip counts
                    st.nextToken(); theta[str]=st.nval;
                    st.nextToken();//skip theta uncertainty
                    st.nextToken(); incidence[str]=st.nval;
                    st.nextToken(); //skip incidence uncertainty
                    st.nextToken(); distance[str]=st.nval;//distance in mm
                    System.out.println("Strip "+str+", theta = "
                    +theta[str]+", incidence = "+incidence[str]+", distance = "
                    +distance[str]);
                    st.nextToken(); //skip distance uncertainty
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
        } catch (IOException e) {
            System.err.println(e);
        }
        vmeMap.setScalerInterval(3);
        for (int i=0; i < TDC_BASE.length; i++){
            vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
        }
        //Focal Plane Detector      (slot, base address, channel, threshold channel)
        idCthd=vmeMap.eventParameter(2, ADC_BASE[0], 10, THRESHOLDS);
        idAnde=vmeMap.eventParameter(2, ADC_BASE[0], 1, THRESHOLDS);
        idScintR=vmeMap.eventParameter(2, ADC_BASE[0], 2, THRESHOLDS);
        idScintL=vmeMap.eventParameter(2, ADC_BASE[0], 3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(2, ADC_BASE[0], 4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(2, ADC_BASE[0], 5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(2, ADC_BASE[0], 6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(2, ADC_BASE[0], 7, THRESHOLDS);
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
                //eventParameter(slot, base address, channel, threshold channel)
                idEnergies[i][j]=vmeMap.eventParameter(whichADCslot(i), 
                whichADCaddress(i), whichChannel(i,j), THRESHOLDS);
                hEnergies[i][j]=new Histogram("E_D"+i+"_S"+j, HIST_1D_INT, COMPRESSED_CHANNELS,
                "Detector "+i+", Strip "+j);
            }
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                //eventParameter(slot, base address, channel, threshold channel)
                idTimes[i][j]=vmeMap.eventParameter(whichTDCslot(i), 
                whichTDCaddress(i), whichTDCchannel(i,j), TIME_THRESHOLDS);
                hTimes[i][j]=new Histogram("T_D"+i+"_S"+j, HIST_1D_INT, COMPRESSED_CHANNELS, "Detector "+i+", Strip "+j+" time");
            }
        }
        hMultiplicity=new Histogram("Multiplicity",HIST_1D_INT,NUM_DETECTORS*STRIPS_PER_DETECTOR,
        "Multiplicity of Energy and Time Hits");
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
        hEvsStripGA = new Histogram("EvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,
        "Energy vs. Strip, All Detectors, Gain Adjusted", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStripGA = new Histogram("TvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one, Gain Adjusted", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        gTime=new Gate("Time", hTvsStrip);//gates on selected TDC channels
        hCthdFrntGT=new Histogram("CthdFrntGT", HIST_2D_INT, TWO_D_CHANNELS, 
        "Cathode vs. Position, gated on: "+gTime,"Position", "Cathode");
        hSilEvsFront = new Histogram("SilEvsFront",HIST_2D_INT, TWO_D_CHANNELS,
        "Silicon Energy vs. FW Position, multiplicity one, PID gate, time gate",
        "Position","E in Si");

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

        //scalers
        sBic      =new Scaler("BIC",0);
        sClck      =new Scaler("Clock",1);
        sEvntRaw    =new Scaler("Event Raw", 2);
        sEvntAccpt  =new Scaler("Event Accept",3);
        sScint    =new Scaler("Scintillator", 4);
        sCathode  =new Scaler("Cathode",5);

        //monitors
        mBeam=new Monitor("Beam ",sBic);
        mClck=new Monitor("Clock",sClck);
        mEvntRaw=new Monitor("Raw Events",sEvntRaw);
        mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
        mScint=new Monitor("Scintillator",sScint);
        mCathode=new Monitor("Cathode",sCathode);
        Monitor mDeadTime=new Monitor(DEAD_TIME, this);
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

    /** Called every time Jam has an event it wants to sort.
     * @param dataEvent contains all parameters for one event
     * @throws Exception so Jam can handle exceptions
     */
    public void sort(int [] dataEvent) throws Exception {
        int [][] eEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
        int [][] eEnergiesGA = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
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

        //proper way to add for 2 phototubes at the ends of scintillating rod
        //see Knoll
        int eSntr=(int)Math.round(Math.sqrt(eSntr1*eSntr2));

        int ecFPsn=eFPsn>>TWO_D_FACTOR;
        int ecRPsn=eRPsn>>TWO_D_FACTOR;
        int ecFHgh=eFHgh>>TWO_D_FACTOR;
        int ecRHgh=eRHgh>>TWO_D_FACTOR;
        int ecSntr=eSntr>>TWO_D_FACTOR;
        int ecCthd=eCthd>>TWO_D_FACTOR;
        int ecAnde=eAnde>>TWO_D_FACTOR;

        //boolean multiplicityOne=false;
        //boolean higherMultiplicity=false;
        //boolean interStripEvent=false;
        //int firstStripHit=-1;
        //int firstDetHit=-1;
        //int firstStripBin = 0;
        multiplicity=0;
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int stripBin = i*STRIPS_PER_DETECTOR+j;
                eEnergies[i][j]=dataEvent[idEnergies[i][j]];
                hEnergies[i][j].inc(eEnergies[i][j]>>COMPRESS_FACTOR);
                hEvsStrip.inc(eEnergies[i][j]>>TWO_D_FACTOR,stripBin);
                eTimes[i][j]=dataEvent[idTimes[i][j]];
                if (eTimes[i][j] > 0 && eEnergies[i][j] >0) {
                    detHit[multiplicity]=i;
                    stripHit[multiplicity]=j;
                    bin[multiplicity]=STRIPS_PER_DETECTOR*i+j;
                    multiplicity++;
                }
                hTimes[i][j].inc(eTimes[i][j]>>COMPRESS_FACTOR);
                double dEnergyCh = (eEnergies[i][j]-energyOffset[i][j])*energyGain[i][j];
                double eDeposit = dEnergyCh/CHANNELS_PER_MEV;//deposited in det [MeV]
                //kinetic energy for flight:
                /*double energy = eDeposit + 0.001* //keV to MeV
                deadLayerLoss.getEnergyLoss(proton,eDeposit,incidence[j]);     */    
                //double energyInCh = energy*CHANNELS_PER_MEV;
                //eEnergiesGA[i][j]=Math.max(0,(int)Math.round(energyInCh));
                //hEvsStripGA.inc(eEnergiesGA[i][j]>>TWO_D_FACTOR,stripBin);
                if (eTimes[i][j]==0) {
                    eTimesGA[i][j]=0;
                } else {
                    double timeCh = timeOffset[i][j]+timeGain[i][j]*eTimes[i][j];
                    //double TplusMsquare = Math.pow(energy+Mproton,2.0);
                    //velocity in mm/nsec
                    //double velocity = C*Math.sqrt((TplusMsquare-Mproton*Mproton)/TplusMsquare);
                    //double timeDiff = distance[j] / velocity;//nsec
                    //double chDiff = timeDiff * CHANNELS_PER_NS;
                    //timeCh -= chDiff;
                    //eTimesGA[i][j]=Math.max(0,(int)Math.round(timeCh));                    
                }
                if (eTimes[i][j] >0){
                    hTimeHits.inc(stripBin);
                }
                int ecTime=eTimes[i][j]>>TWO_D_FACTOR;
                if (gTime.inGate(ecTime,stripBin)) {
                    hCthdFrntGT.inc(ecFPsn,ecCthd);
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
                            interBin[numInterHits]=detHit[m]*STRIPS_PER_DETECTOR+stripHit[m];
                            numInterHits++;
                        }
                    }
                }
            }
        }
        if (multiplicity == 1) {
            int ecTime=eTimes[detHit[0]][stripHit[0]]>>TWO_D_FACTOR;
            hTvsStrip.inc(ecTime,bin[0]);
            hTvsStripGA.inc(eTimesGA[detHit[0]][stripHit[0]]>>TWO_D_FACTOR,
            bin[0]);
        }
        for (int i=0; i<multiplicity; i++) hHits.inc(bin[i]);
        for (int i=0; i< numInterHits; i++) hInterHits.inc(interBin[i]);
        hMultiplicity.inc(multiplicity);

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
        
        boolean bSC = gSntrCthd.inGate(ecSntr,ecCthd);
        boolean bFC = gFrntCthd.inGate(ecFPsn,ecCthd);
        boolean bFS = gFrntSntr.inGate(ecFPsn,ecSntr);
        boolean bPID = bSC && bFC && bFS;

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
        }
        if (bPID) {// gated on all 3 gate above
            writeEvent(dataEvent);
            hFrntGCSF.inc(eFPsn);
            hRearGCSF.inc(eRPsn);
            hFrntRearGCSF.inc(ecFPsn,ecRPsn);
            if(gFrntRear.inGate(ecFPsn,ecRPsn)) {
                for (int i=0; i<multiplicity;i++){
                    hTvsStripPID.inc(eTimes[detHit[i]][stripHit[i]]>>TWO_D_FACTOR,
                    bin[i]);
                }
                hFrntGAll.inc(eFPsn);
                hRearGAll.inc(eRPsn);
                hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
                hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
                if (multiplicity==1 &&
                gTime.inGate(eTimes[detHit[0]][stripHit[0]]>>TWO_D_FACTOR,
                bin[0])){
                    hFrntGTime.inc(eFPsn);
                    hcFrntGTime.inc(eFPsn>>COMPRESS_FACTOR);
                    hSilEvsFront.inc(ecFPsn,eEnergies[detHit[0]][stripHit[0]]
                    >>TWO_D_FACTOR);
                }
            }
        }
        //produce hit pattern histograms
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
        } else {
            return 50.0;
        }
    }
}
