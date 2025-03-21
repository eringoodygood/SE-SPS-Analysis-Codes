/*
 */
package sort.coinc.offline.may;
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
 * @author Dale Visser
 * @since 24 April 2001
 */
public class YLSAandEnge22Mg_p extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
static final int [] TDC_BASE = {0x30000000,0x30010000,0x30020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 300;//ADC lower threshold in channels
    static final int TIME_THRESHOLDS = 30;//TDC lower threshold in channels
    static final int TIME_RANGE = 600;//ns
    static final int LAST_ADC_BIN = 3840;

    //LEDA Detectors
    static final int NUM_DETECTORS = 5;
    static final int STRIPS_PER_DETECTOR = 16;
    static final int SAMPLE_STRIP = 4;

    //QBr calibration
    static final double FPX0 = 217.206;
    static final double FPA0 = 2222.1673492326267;
    static final double FPA1 = 1.971278992256618;
    static final double FPA2 = 5.0619683634844925E-5;

    //names
    static final String DEAD_TIME="Dead Time %";

    //histogramming constants
    final int ADC_CHANNELS = 4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed histogram
    final int TWO_D_CHANNELS = 256; //number of channels per dimension in 2-d histograms
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
    Histogram hCthdAndeGCSF, hFrntCthdGCA; Gate gCA;
    Histogram hFrntCthdGFA, hFrntAndeGCSF; Gate gFA;
    Histogram hTimeGA;
    
    Gate gSilicon, gCthd, gPeak;//gates 1 d
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear;//gates 2 d
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors

    Gate gTime;
    Histogram hCthdFrntGT;
    Histogram hHits, hEvsStrip, hTvsStrip, hTimeHits, hTvsEhits, hInterHits;
    Histogram hMultiplicity;
    Histogram hEvsStripGA, hTvsStripGA; //gain adjusted spectra
    //Histogram hTvsStripPID;
    Histogram hSilEvsFront;//to see that we satisfy total energy conditions
    Histogram hFinalState, hStateVsFP;
    Histogram hAngDist; //for getting angular distribution of alphas
    Histogram hFrntGgs; //focal plane gated on g.s. of 16O
    Histogram hEvsStrip_gs, hTvsStrip_gs; //YLSA plots gated on 16O g.s.
    Gate gGS; //for tagging ground state of 16O on 2D plot

    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh, idRearHgh;

    //nuclear properties for physics analysis
    //objects representing carbon-12, proton, He6, and 22Mg
    Nucleus C12,proton,He6;
    //masses of proton and 21Na g.s.
    double Mproton,M21;
    double Tcent;//beam energy middle of target
    
    //for calculating energy losses
    EnergyLoss deadLayerLoss, targetLoss;
    
    //calibration of gain adjusted energy channels and time channels
    static final double C = 299.792458; //speed of light in mm/nsec

    //int multiplicity;//number of real hits (in TDC window)
    int [] detHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] stripHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] bin = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int numInterHits;//number of real interStrip hits (in TDC window)
    int [] interDetHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] interStripHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
    int [] interBin = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];

    ResidualKinematics rk;//for calculating Residual trajectory

    ArrayCalibration ac;
    /** Sets up objects, called when Jam loads the sort routine.
     * @throws Exception necessary for Jam to handle exceptions
     */
    public void initialize() throws Exception {
        new ResidualKinematics(); new EnergyLoss(); //set up kinematics/ nuclear / eloss stuff
        proton = new Nucleus(1,1);
        He6 = new Nucleus(2,6);
        C12 = new Nucleus(6,12);
        Nucleus O16 = new Nucleus(8,16);
        Mproton = proton.getMass().value;
        Nucleus Na21=new Nucleus(11,21);
        M21=Na21.getMass().value;
        Solid deadLayer = new Solid(0.2*1.0e-4, Absorber.CM, "Al");//0.2 um
        deadLayerLoss = new EnergyLoss(deadLayer);
        targetLoss = new EnergyLoss(new Solid(19.0/2, Absorber.MICROGRAM_CM2, "C"));
        Tcent = 90.0 - 0.001*targetLoss.getThinEnergyLoss(O16,90.0);
        rk = new ResidualKinematics(Tcent, 5.0, O16, C12, He6);
        retrieveCalibration();
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
        //gate on front/anode
        hFrntCthdGFA=new Histogram("FrontCathGFA", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Front Position - AnFr gate","Front Position","Cathode");
        //gated on cathode/anode
        hFrntCthdGCA = new Histogram("FrontCathGCA", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Front Position - AnCa gate","Front Position","Cathode");
        //gated on 3 gates
        hFrntGCSF   =new Histogram("FrontGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc gates");
        hRearGCSF   =new Histogram(   "RearGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc gates");
        hFrntRearGCSF=new Histogram("FRGateCSF  ",HIST_2D_INT, TWO_D_CHANNELS,"Front vs. Rear - ScCa, FwCa, FwSc gates");
        hCthdAndeGCSF   =new Histogram("CathAndeGCSF", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Anode, gated on FrCaSc ","Cathode","Anode");
        hFrntAndeGCSF = new Histogram("FrntAndeGCSF  ", HIST_2D_INT,
        TWO_D_CHANNELS, "Anode vs Front Position, gated on CaFrSc","Front Position","Anode");
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
                idEnergies[i][j]=vmeMap.eventParameter(Eparam, whichADC(i), whichChannel(i,j), THRESHOLDS);
                hEnergies[i][j]=new Histogram("E_D"+i+"_S"+j, HIST_1D_INT, ADC_CHANNELS,
                "Detector "+i+", Strip "+j);
            }
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int Tparam = 0x110 + i*STRIPS_PER_DETECTOR + j;
                idTimes[i][j]=vmeMap.eventParameter(Tparam, whichTDC(i), whichTDCchannel(i,j), TIME_THRESHOLDS);
                hTimes[i][j]=new Histogram("T_D"+i+"_S"+j, HIST_1D_INT, ADC_CHANNELS, "Detector "+i+", Strip "+j+" time");
            }
        }

        System.err.println("# Parameters: "+getEventSize());
        System.err.println("ADC channels: "+ADC_CHANNELS);
        System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
        System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);

        hMultiplicity=new Histogram("Multiplicity",HIST_1D_INT,NUM_DETECTORS*STRIPS_PER_DETECTOR,
        "Multiplicity of Energy and Time Hits");
        hHits = new Histogram("Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over ADC threshold",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hAngDist = new Histogram("DecayAngDist", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, 
        "Angular Distribution of Decays of gated State",STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hInterHits = new Histogram("InterHits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Inter-Strip hits",
        STRIPS_PER_DETECTOR+"*Det+Strip","Counts");
        hTimeHits = new Histogram("Time Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over TDC threshold",
        "Strip","Counts");
        /*hTvsEhits = new Histogram("T vs E hits", HIST_2D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Time hits vs Energy hits",
        "E hits", "T hits");*/
        hEvsStrip = new Histogram("EvsStrip",HIST_2D_INT, TWO_D_CHANNELS, "Energy vs. Strip, All Detectors", "Energy",
        STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStrip = new Histogram("TvsStrip",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        /*hTvsStripPID = new Histogram("TvsStripPID",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, multiplicity one,PID gates", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");*/
        hEvsStripGA = new Histogram("EvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,
        "Energy vs. Strip, All Detectors, Gain Adjusted", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStripGA = new Histogram("TvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, Gain Adjusted", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTimeGA = new Histogram("TimeGA",HIST_1D_INT, TWO_D_CHANNELS, 
        "Time, Gain Adjusted, all strips");
        hCthdFrntGT=new Histogram("CthdFrntGT", HIST_2D_INT, TWO_D_CHANNELS,
        "Cathode vs. Position, gated on: "+gTime,"Position", "Cathode");
        hSilEvsFront = new Histogram("SilEvsFront",HIST_2D_INT, TWO_D_CHANNELS,
        "Silicon Energy vs. FW Position, multiplicity one, PID gate, time gate",
        "Position","E in Si");
        hFinalState = new Histogram("FinalState", HIST_1D_INT, 400,"Final State in 21Na");
        hStateVsFP=new Histogram("StateVsFP",HIST_2D_INT,512,"Final State in 21Na vs. FP Position",
        "Focal Plane", "F-19 state");
        hFrntGgs = new Histogram("FrntGgs", HIST_1D_INT, ADC_CHANNELS, 
        "Front Gated on F-19 ground state");
        hEvsStrip_gs = new Histogram("EvsStrip_gs",HIST_2D_INT, TWO_D_CHANNELS,
        "Energy vs. Strip, All Detectors, Gain Adjusted, F-19 g.s.", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");
        hTvsStrip_gs = new Histogram("TvsStrip_gs",HIST_2D_INT, TWO_D_CHANNELS,
        "Time vs. Strip, All Detectors, Gain Adjusted, F-19 g.s.", "Energy", STRIPS_PER_DETECTOR+"*Det+Strip");

        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gPeak   =new Gate("Peak", hFrntGAll);
        hFrntGgs.addGate(gPeak);
        hFrntGTime.addGate(gPeak);
        //gates  2d
        gTime=new Gate("Time", hTvsStripGA);//gates on selected TDC channels
        hTvsStrip_gs.addGate(gTime);
        gGS  =new Gate("GroundState",hStateVsFP);
        gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        gCA = new Gate("Ca-An", hCthdAnde);
        gFA = new Gate("Fr-An", hFrntAnde);
        hCthdAndeGCSF.addGate(gCA);
        hFrntAndeGCSF.addGate(gFA);
        hFrntSntrGSC.addGate(gFrntSntr);
        hFrntCthdGSC.addGate(gFrntCthd);
        hSntrCthdGFC.addGate(gSntrCthd);
        hFrntSntrGFC.addGate(gFrntSntr);
        hSntrCthdGFS.addGate(gSntrCthd);
        hFrntCthdGFS.addGate(gFrntCthd);
        hFrntCthdGCA.addGate(gFrntCthd);

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
        Monitor mDeadTime=new Monitor(DEAD_TIME, this);
        
        System.out.println("YLSA Energy GA = "+ac.getChannelsPerMeV()+" ch/MeV");
        System.out.println("YLSA Time GA = "+ac.getChannelsPerNsec()+" ch/nsec");
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

    private void retrieveCalibration() throws IOException,
    ClassNotFoundException{
        File data = new File("/data/may01/analysis/ArrayCalibration.obj");
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(data));
        ac = (ArrayCalibration)ois.readObject();
        ois.close();
    }

    //private int event=1;
    /** Called every time Jam has an event it wants to sort.
     * @param dataEvent contains all parameters for one event
     * @throws Exception so Jam can handle exceptions
     */
    public void sort(int [] dataEvent) throws Exception {
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

        //System.out.println("Declared silicon data storage: event "+event);
        int multiplicity=0;
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int stripBin = i*STRIPS_PER_DETECTOR+j;
                eEnergies[i][j]=dataEvent[idEnergies[i][j]];
                hEnergies[i][j].inc(eEnergies[i][j]);
                hEvsStrip.inc(eEnergies[i][j]>>TWO_D_FACTOR,stripBin);
                eTimes[i][j]=dataEvent[idTimes[i][j]];
                hTimes[i][j].inc(eTimes[i][j]);
                boolean energy = eEnergies[i][j] >0  &&
                eEnergies[i][j] <= LAST_ADC_BIN;
                boolean time = eTimes[i][j] > 0;
                if (time && energy) {
                    detHit[multiplicity]=i;
                    stripHit[multiplicity]=j;
                    bin[multiplicity]=STRIPS_PER_DETECTOR*i+j;
                    multiplicity++;
                    eEnergiesGA[i][j] = ac.getCalibratedEnergyChannel(i,j,
                    eEnergies[i][j]);
                    double eDeposit = ac.getEnergyDeposited(i,j,eEnergies[i][j]);//deposited in det [MeV]
                    //kinetic energy for flight:
                    double eFlight = eDeposit + 0.001* //keV to MeV
                    deadLayerLoss.getThinEnergyLoss(proton,eDeposit,
                    Math.acos(1/ac.getIncidence(j)));
                    hEvsStripGA.inc(eEnergiesGA[i][j]>>TWO_D_FACTOR,stripBin);
                    if (eTimes[i][j]==0) {
                        eTimesGA[i][j]=0;
                    } else {
                        eTimesGA[i][j] = ac.getCalibratedTimeChannel(i,j,
                        eTimes[i][j]);
                        double TplusMsquare = Math.pow(eFlight+Mproton,2.0);
                        //velocity in mm/nsec
                        double velocity = C*Math.sqrt((TplusMsquare-Mproton*Mproton)/
                        TplusMsquare);
                        double timeDiff = ac.getDistance(j) / velocity;//nsec
                        double chDiff = timeDiff * ac.getChannelsPerNsec();
                        eTimesGA[i][j] -= chDiff;
                    }
                    hTvsStrip.inc(eTimes[i][j]>>TWO_D_FACTOR,stripBin);
                    int timeGA2d=eTimesGA[i][j]>>TWO_D_FACTOR;
                    hTvsStripGA.inc(timeGA2d,stripBin);
                    hTimeGA.inc(timeGA2d);
                    if (eTimes[i][j] >0){
                        hTimeHits.inc(stripBin);
                    }
                    int ecTime=eTimes[i][j]>>TWO_D_FACTOR;
                    if (gTime.inGate(ecTime,stripBin)) {
                        hCthdFrntGT.inc(ecFPsn,ecCthd);
                    }
                }
            }
        }
        //System.out.println("mult = "+multiplicity);
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

        for (int i=0; i<multiplicity; i++) hHits.inc(bin[i]);
        for (int i=0; i< numInterHits; i++) hInterHits.inc(interBin[i]);
        hMultiplicity.inc(multiplicity);

        boolean bSC = gSntrCthd.inGate(ecSntr,ecCthd);
        boolean bFC = gFrntCthd.inGate(ecFPsn,ecCthd);
        boolean bFS = gFrntSntr.inGate(ecFPsn,ecSntr);
        boolean bCA = gCA.inGate(ecCthd,ecAnde);
        boolean bFA = gFA.inGate(ecFPsn,ecAnde);
        boolean bCSF = bSC && bFC && bFS;//cathode, scint, posn only
        boolean bPID = bCSF && bCA && bFA;
        boolean bGood = bPID && gFrntRear.inGate(ecFPsn,ecRPsn);

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
        if (bCA) hFrntCthdGCA.inc(ecFPsn,ecCthd);
        if (bFA) hFrntCthdGFA.inc(ecFPsn,ecCthd);
        if (bCSF) {// gated on all 3 gate above
            writeEvent(dataEvent);
            hFrntGCSF.inc(eFPsn);
            hRearGCSF.inc(eRPsn);
            hFrntRearGCSF.inc(ecFPsn,ecRPsn);
            hCthdAndeGCSF.inc(ecCthd,ecAnde);
            hFrntAndeGCSF.inc(ecFPsn,ecAnde);
            if(bGood) {
                hFrntGAll.inc(eFPsn);
                hRearGAll.inc(eRPsn);
                hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
                hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
            }
        }
        //produce hit pattern histograms, not necessary in offline, so
        //commenting out
        ///int [] energy=new int[STRIPS_PER_DETECTOR];
        //int [] time = new int[STRIPS_PER_DETECTOR];
        /*for (int i=0;i<NUM_DETECTORS;i++) {
            for  (int eStrip = 0; eStrip<STRIPS_PER_DETECTOR; eStrip++){
                energy[eStrip]=eEnergies[i][eStrip];
                for (int tStrip=0; tStrip<STRIPS_PER_DETECTOR; tStrip++){
                    time[tStrip] = eTimes[i][tStrip];
                    boolean tHit = time[tStrip] >= TIME_THRESHOLDS;
                    boolean eHit = energy[eStrip] >= THRESHOLDS && energy[eStrip] <= LAST_ADC_BIN;
                    if (tHit && eHit)
                    hTvsEhits.inc(eStrip+STRIPS_PER_DETECTOR*i,
                    tStrip+STRIPS_PER_DETECTOR*i);
                }
            }
        }*/
        if (bGood && numInterHits == 0) {
            for (int hit = 0; hit<multiplicity; hit++){
                int det=detHit[hit]; int strip=stripHit[hit]; int binHit=bin[hit];
                if (gTime.inGate(eTimesGA[det][strip]>>
                TWO_D_FACTOR,bin[hit])){
                    hFrntGTime.inc(eFPsn);
                    hcFrntGTime.inc(eFPsn>>COMPRESS_FACTOR);
                    hSilEvsFront.inc(ecFPsn,
                    eEnergiesGA[det][strip]>>TWO_D_FACTOR);
                    //coincidence=true;
                    double eDiff = (eFPsn>>COMPRESS_FACTOR)-FPX0;
                    double qbr = FPA0+FPA1*eDiff+FPA2*eDiff*eDiff;
                    //System.out.println("-----");
                    //System.out.println("Position Ch = "+eFPsn);
                    //System.out.println("qbr = "+qbr);
                    double spectKE = Reaction.getKE(He6,qbr);
                    //System.out.println("Spectrometer KE = "+spectKE);
                    double reactKE = spectKE+0.001*targetLoss.getThinEnergyLoss(
                    He6,spectKE,Math.toRadians(5));
                    rk.setSpectrometerKE(reactKE);
                    double Ex=rk.getEx4();
                    //lab motion of alpha, angles in radians
                    double thetaP = ac.getTheta(strip);//lab theta of proton
                    double phiP = ac.getPhi(det);//lab phi of proton
                    double eDeposit = ac.getEnergyDeposited(det,strip,eEnergies[det][strip]);//deposited in det [MeV]
                    //System.out.println("Energy in Detector = "+eDeposit+" MeV");
                    //kinetic energy for flight:
                    double eFlight = eDeposit + 0.001* //keV to MeV
                    deadLayerLoss.getThinEnergyLoss(proton,eDeposit,Math.acos(1/ac.getIncidence(strip)));
                    //System.out.println("Energy in Flight = "+eFlight+" MeV");
                    double Tproton = eFlight + 0.001*targetLoss.getThinEnergyLoss(proton,eFlight,
                    Math.toRadians(180.0)-thetaP);//eloss in keV
                    //System.out.println("Decay Kinetic Energy = "+Talpha+" MeV");
                    double Eproton = Tproton+Mproton;
                    double temp = Tproton/Mproton;
                    double Pproton = Mproton*Math.sqrt(temp*(temp+2));
                    //lab motion of residual, angles in radians
                    double zeta = rk.getResidualAngle();//lab angle of 22Mg*
                    double E4=rk.getResidualTotalEnergy();//total energy of 22Mg*
                    double P4=rk.getResidualMomentum();
                    //lab motion, mass of 22Mg*
                    //cos(angle between 22Mg* and proton)
                    double cosThetaRelative = Math.sin(zeta)*Math.sin(thetaP)*
                    Math.cos(phiP)+Math.cos(zeta)*Math.cos(thetaP);
                    //momentum^2 of 21Na
                    double P21sq = P4*P4+Pproton*Pproton-2.0*P4*Pproton*
                    cosThetaRelative;
                    double E21=E4-Eproton;//total energy of 21Na
                    double M21x = Math.sqrt(E21*E21-P21sq);
                    double Ex21 = M21x-M21;//state populated in 21Na
                    int plotBin = (int)Math.round(Ex21*10.0)+100;
                    //System.out.println("Channel for Ex = "+plotBin);
                    hFinalState.inc(plotBin);
                    int front = eFPsn >>3;
                    hStateVsFP.inc(front,plotBin);
                    if (gGS.inGate(front,plotBin)) {
                        hFrntGgs.inc(eFPsn);
                        hEvsStrip_gs.inc(eEnergiesGA[det][strip]>>TWO_D_FACTOR,binHit);
                        hTvsStrip_gs.inc(eTimesGA[det][strip]>>TWO_D_FACTOR,binHit);
                        if (gPeak.inGate(eFPsn)) hAngDist.inc(binHit);
                    }                        
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
