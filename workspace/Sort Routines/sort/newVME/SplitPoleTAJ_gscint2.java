package sort.newVME;
import jam.data.*;
import jam.sort.*;

/**
 * Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>,
 * which was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right
 * way; also added gate to cathAnde
 *
 * @author     Dale Visser
 * @created    March 24, 2004
 * @since      26 July 2001
 * 
 * Edited to gate on Scint2 and extra histograms deleted(anode and p,d,t,a gates/gated histograms)
 * by Catherine Deibel
 * 14 Feb 2007
 */
public class SplitPoleTAJ_gscint2 extends SortRoutine {

  private final static String DEAD_TIME = "Dead Time %";
  /*
   *  VME properties
   */
  private final static int[] ADC_BASE = {0x20000000};
  private final static int[] TDC_BASE = {0x30000000, 0x30020000};

  /*
   *  ADC lower threshold in channels
   */
  private final static int THRESHOLDS = 128;
  /*
   *  TDC lower threshold in channels
   */
  private final static int TIME_THRESH = 30;
  /*
   *  in nanoseconds
   */
  private final static int TIME_RANGE = 1200;

  /*
   *  num of channels per ADC
   */
  private final static int ADC_CHANNELS = 4096;
  /*
   *  compressed histograms
   */
  private final static int CH_COMPRESS = 512;
  /*
   *  2D histograms
   */
  private final static int CHAN_2D = 256;
  /*
   *  hi-res 2D histograms
   */
  private final static int TWO_D_HIRES = 2 * CHAN_2D;
  /*
   *  compression bits to shift >>
   */
  private final static int COMPRESS_FAC = Math.round((float)
      (Math.log(ADC_CHANNELS / CH_COMPRESS) / Math.log(2.0)));
  /*
   *  2D bits to shift >>
   */
  private final static int TWO_D_FACTOR = Math.round((float)
      (Math.log(ADC_CHANNELS / CHAN_2D) / Math.log(2.0)));
  /*
   *  2D hi-res bits to shift >>
   */
  private final static int HIRES_FACTOR = Math.round((float)
      (Math.log(ADC_CHANNELS / TWO_D_HIRES) / Math.log(2.0)));

  /*
   *  ungated spectra
   */
  private transient Histogram hCthd, hSntr1, hSntr2, hSntrSum, hSntr1Sntr2,
      hFrntPsn, hRearPsn, hMonitor;
  /*
   *  Rear Wire Pulse Height
   */
  private transient Histogram hFrntPH;
  /*
   *  position x height y
   */
  private transient Histogram hRearPH;
  private transient Histogram hSntrCthd, hSntr2Cthd, hFrntCthd, hRearCthd,
	   hFrntSntr, hFrntSntr1, hFrntSntr2, hFrntPRearP, hFrntY, hRearY,
	   hYvsPsn, hYvsPsnGPID, hFrntYRearY, hSntr2Cthd_GFR, hFrntCthd_GFR, hFrntSntr2_GFR;
  
  
  /*
   *  new way to measure front and rear positions, using the wire signal
   *  as the start and the left and right signals as the stop for 4
   *  new TACs
   */
  private transient Histogram hFrontLeft, hFrontRight, hRearLeft, hRearRight,
      hFrontSum, hRearSum, hFrontNew, hRearNew, hFrontLvsR,
      hRearLvsR, hFrontTheta, hRearTheta, hFC,
      hFrNewVsOld;

  /*
   *  status histograms
   */
  private transient Histogram hFWbias, hRWbias, hBCIRange;


  /*
   *  new detector, formerly "TeDector", now TAJ for Ted-Anuj-Jac
   *  more parameters than with old split pole detector
   */
  private transient Histogram hFrFrPH, hFrMidPH, hFrontBackPH,
      hRearFrontPH, hRearMidPH, hRearBackPH;

  /*
   *  gate by scintillator cathode
   */
   private transient Histogram hFrntSntr2GSC, hFrntCthdGSC;
// old   private transient Histogram hFrntSntrGSC, hFrntCthdGSC;
  /*
   *  gate by Front wire Cathode
   */
   private transient Histogram hSntr2CthdGFC, hFrntSntr2GFC;
	private transient Histogram hSntr2CthdGFS, hFrntCthdGFS;
   /* old
  private transient Histogram hSntrCthdGFC, hFrntSntrGFC;
  private transient Histogram hSntrCthdGFS, hFrntCthdGFS;
  /*
   *  gate by Front wire Scintillator
   */
  private transient Histogram hFrntGCSF, hRearGCSF, hFrRightgCSF, hFrntGAll,
      hRearGAll;
  /*
   *  front and rear wire gate on all
   */
  private transient Histogram hcFrntGAll, hcRearGAll;

  /*private transient hCathTheta;*/

  /*
   *  1D gates
   */
  private transient Gate gCthd, gPeak, gFSum, gRSum;
  /*
   *  2D gates
   */
   private transient Gate gSntr2Cthd, gFrntSntr2, gFrntCthd, gFrntRear,
	   gXY, gFrYvsReY, gFrontLvsR, gRearLvsR, gFP;
	   
/* old 	   
  private transient Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear,
      gXY, gFrYvsReY, gFrontLvsR, gRearLvsR,
      gSntrCthd_p, gSntrCthd_d, gSntrCthd_t, gSntrCthd_a,
      gFrntSntr_p, gFrntSntr_d, gFrntSntr_t, gFrntSntr_a,
      gFrntCthd_p, gFrntCthd_d, gFrntCthd_t, gFrntCthd_a;

  /*
   *  Scalers and monitors
   */
  private transient Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode,
      sNMR, sFCLR;
  /*
   *  number of FCLR's that went to ADC's
   */
  private transient Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint,
      mCathode, mNMR, mFCLR;

  /*
   *  id numbers for the signals;
   */
  private transient int idCthd, idScintR, idScintL, idFrntPsn,
      idRearPsn, idFrntHgh, idRearHgh, idYFrnt, idYRear, idFWbias, idRWbias,
      idBCIRange, idFrontLeft, idFrontRight, idRearLeft, idRearRight, idMonitor,
      idFrMidPH, idFrBackPH, idReFrPH, idRearBackPH;
  private transient DataParameter pXTheta, pThOffset, pCTheta;



  /**
   *  Description of the Method
   *
   * @exception  Exception  Description of the Exception
   */
  public void initialize() throws Exception {
    vmeMap.setScalerInterval(3);
    for (int i = 0; i < TDC_BASE.length; i++) {
      vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
    }

    vmeMap.eventParameter(2, ADC_BASE[0], 0, 0);
    idScintR = vmeMap.eventParameter(2, ADC_BASE[0], 2, THRESHOLDS);
    idScintL = vmeMap.eventParameter(2, ADC_BASE[0], 3, THRESHOLDS);
    idFrntPsn = vmeMap.eventParameter(2, ADC_BASE[0], 4,
        THRESHOLDS);
    idRearPsn = vmeMap.eventParameter(2, ADC_BASE[0], 5,
        THRESHOLDS);
    idFrntHgh = vmeMap.eventParameter(2, ADC_BASE[0], 6,
        THRESHOLDS);
    idRearHgh = vmeMap.eventParameter(2, ADC_BASE[0], 7,
        THRESHOLDS);
    idYFrnt = vmeMap.eventParameter(2, ADC_BASE[0], 8, THRESHOLDS);
    idYRear = vmeMap.eventParameter(2, ADC_BASE[0], 9, THRESHOLDS);
    idCthd = vmeMap.eventParameter(2, ADC_BASE[0], 10, THRESHOLDS);

    idFWbias = vmeMap.eventParameter(2, ADC_BASE[0], 12,
        THRESHOLDS);
    idRWbias = vmeMap.eventParameter(2, ADC_BASE[0], 13,
        THRESHOLDS);
        
        
    idBCIRange = vmeMap.eventParameter(2, ADC_BASE[0], 14, 16);
    idFrontLeft = vmeMap.eventParameter(2, ADC_BASE[0], 16 + 3,
        THRESHOLDS);
    idFrontRight = vmeMap.eventParameter(2, ADC_BASE[0], 16 + 4,
        THRESHOLDS);
    idRearLeft = vmeMap.eventParameter(2, ADC_BASE[0], 16 + 6,
        THRESHOLDS);
    idRearRight = vmeMap.eventParameter(2, ADC_BASE[0], 16 + 7,
        THRESHOLDS);

    /*
     *  new pulse height signals from TAJ
     */
    idRearBackPH = vmeMap.eventParameter(2, ADC_BASE[0], 16 + 10,
        THRESHOLDS);
    idReFrPH = vmeMap.eventParameter(2, ADC_BASE[0], 16 + 11,
        THRESHOLDS);
    idFrMidPH = vmeMap.eventParameter(2, ADC_BASE[0], 16 + 12,
        THRESHOLDS);
    idFrBackPH = vmeMap.eventParameter(2, ADC_BASE[0], 16 + 13,
        THRESHOLDS);
	idMonitor = vmeMap.eventParameter(2, ADC_BASE[0], 1,
		  THRESHOLDS);

    hCthd = new Histogram("Cathode     ", HIST_1D_INT,
        ADC_CHANNELS, "Cathode Raw ");
    hSntr1 = new Histogram("Scint1      ", HIST_1D_INT,
        ADC_CHANNELS, "Scintillator PMT 1");
    hSntr2 = new Histogram("Scint2      ", HIST_1D_INT,
        ADC_CHANNELS, "Scintillator PMT 2");
    hSntrSum = new Histogram("ScintSum    ", HIST_1D_INT,
        ADC_CHANNELS, "Scintillator Sum");
	hSntr1Sntr2 = new Histogram("Scint1-Scint2",
					HIST_2D_INT, CHAN_2D,
					"Scintillator PMT 2 versus Scintillator PMT 1   ", "Scint 1", "Scint2");
    hFrntPsn = new Histogram("FrontPosn    ", HIST_1D_INT,
        ADC_CHANNELS, "Front Wire Position");
    hRearPsn = new Histogram("RearPosn     ", HIST_1D_INT,
        ADC_CHANNELS, "Rear Wire Position");
    hFrontLeft = new Histogram("FrontLeft    ", HIST_1D_INT,
        ADC_CHANNELS, "Front Wire Left TAC");
    hFrontRight = new Histogram("FrontRight    ", HIST_1D_INT,
        ADC_CHANNELS, "Front Wire Right TAC");
    hFrontLvsR = new Histogram("FrontLvsR  ", HIST_2D_INT,
        TWO_D_HIRES, "Front Right vs Left", "Left",
        "Right");
    hFrNewVsOld = new Histogram("FrontNewvsOld", HIST_2D_INT,
        TWO_D_HIRES, "Front TDC vs TAC", "TAC", "TDC");
    hFrontSum = new Histogram("FrontPosnSum    ", HIST_1D_INT,
        2 * ADC_CHANNELS,
        "Front Wire Position - sum of left TAC plus right TAC");
    hFrontNew = new Histogram("FrontPosnNew    ", HIST_1D_INT,
        2 * ADC_CHANNELS, "Front Wire Position - two TACs");
    hRearLeft = new Histogram("RearLeft    ", HIST_1D_INT,
        ADC_CHANNELS, "Rear Wire Left TAC");
    hRearRight = new Histogram("RearRight    ", HIST_1D_INT,
        ADC_CHANNELS, "Rear Wire Right TAC");
    hRearLvsR = new Histogram("RearLvsR  ", HIST_2D_INT,
        TWO_D_HIRES, "Rear Right vs Left", "Left", "Right");
    hRearSum = new Histogram("RearPosnSum    ", HIST_1D_INT,
        2 * ADC_CHANNELS,
        "Rear Wire Position - sum of left TAC plus right TAC");
    hRearNew = new Histogram("RearPosnNew    ", HIST_1D_INT,
        2 * ADC_CHANNELS, "Rear Wire Position - two TACs");
    hFrFrPH = new Histogram("FrontFrontPH   ", HIST_1D_INT,
        ADC_CHANNELS,
        "Front wire in the front wire assembly Pulse Height");
    hFrMidPH = new Histogram("FrontMiddlePH    ", HIST_1D_INT,
        ADC_CHANNELS,
        "Middle wire in the front wire assembly Pulse Height");
    hFrontBackPH = new Histogram("FrontBackPH   ", HIST_1D_INT,
        ADC_CHANNELS,
        "Back wire in the rear wire assembly Pulse Height");
    hRearFrontPH = new Histogram("RearFrontPH   ", HIST_1D_INT,
        ADC_CHANNELS,
        "Front wire in the front wire assembly Pulse Height");
    hRearMidPH = new Histogram("RearMiddlePH    ", HIST_1D_INT,
        ADC_CHANNELS,
        "Middle wire in the front wire assembly Pulse Height");
    hRearBackPH = new Histogram("RearBackPH   ", HIST_1D_INT,
        ADC_CHANNELS,
        "Back wire in the rear wire assembly Pulse Height");
    hRWbias = new Histogram("RearBias    ", HIST_1D_INT,
        ADC_CHANNELS, "Rear Wire Bias");
    hFWbias = new Histogram("FrontBias    ", HIST_1D_INT,
        ADC_CHANNELS, "Front Wire Bias");
    hBCIRange = new Histogram("BCIRange", HIST_1D_INT, ADC_CHANNELS,
        "BCI Full Scale Range - about 330 mV/position");
    hFrntY = new Histogram("Front Y", HIST_1D_INT, ADC_CHANNELS,
        "Y (vertical) Position at Front Wire");
    hRearY = new Histogram("Rear Y", HIST_1D_INT, ADC_CHANNELS,
        "Y (vertical) Position at Rear Wire");
    final String FRONT_POS = "Front Position";
    hFrntPH = new Histogram("FrontPvsHeight", HIST_2D_INT,
        CHAN_2D,
        "Pulse Height of FrontFront wire vs Front Position",
        FRONT_POS,
        "Pulse Height");
    hRearPH = new Histogram("RearPvsHeight ", HIST_2D_INT,
        CHAN_2D,
        "Pulse Height of RearMiddle wire vs Rear Position",
        "Rear Position", "Pulse Height");
    final String POS = "Position";
    hYvsPsn = new Histogram("Y vs Position", HIST_2D_INT, CHAN_2D,
        "Front Y vs. Front Wire Position (X)",
        POS,
        "Y");
    hYvsPsnGPID = new Histogram("YvsPosnPID", HIST_2D_INT, CHAN_2D,
        "Front Y vs. Front Wire Position (X) Gated on PID", POS, "Y");
    final String SCINT = "Scintillator";
    final String CATH = "Cathode";
	hSntrCthd = new Histogram("ScintCathode  ", HIST_2D_INT, CHAN_2D,
				"Cathode vs Scintillator PMT", "Scintillator PMT", CATH);
			hSntr2Cthd = new Histogram("Scint2Cathode  ", HIST_2D_INT, CHAN_2D,
				"Cathode vs Scintillator PMT2", "Scintillator PMT 2", CATH);
			hFrntCthd = new Histogram("FrontCathode  ", HIST_2D_INT, CHAN_2D,
				"Cathode vs Front Position", FRONT_POS, CATH);
			hRearCthd = new Histogram("RearCathode  ", HIST_2D_INT, CHAN_2D,
				"Cathode vs Rear Position", "Rear Position", CATH);
			hFrntSntr = new Histogram("FrontScint ", HIST_2D_INT, CHAN_2D,
				"Scintillator vs Front Position", FRONT_POS, SCINT);
			hFrntSntr1 = new Histogram("FrontScint1 ", HIST_2D_INT, CHAN_2D,
				"Scintillator PMT 1 vs Front Position", FRONT_POS, "Scintillator 1");
			hFrntSntr2 = new Histogram("FrontScint2 ", HIST_2D_INT, CHAN_2D,
				"Scintillator PMT 2 vs Front Position", FRONT_POS, "Scintillator 2");
			hFrntPRearP = new Histogram("FrontRear  ", HIST_2D_INT, TWO_D_HIRES,
				"Rear Position vs Front Position", FRONT_POS, "Rear Position");
			hFrntYRearY = new Histogram("FrontY_RearY  ", HIST_2D_INT, CHAN_2D,
				"Rear Y Position vs Y Front Position", "Front Y Position",
				"Rear Y Position");
			hMonitor= new Histogram("Monitor", HIST_1D_INT,ADC_CHANNELS, "Monitor");	
			//ScintCathode Gated on other
			hSntr2CthdGFC = new Histogram("Scint2CathodeGFC", HIST_2D_INT, CHAN_2D,
				"Cathode vs Scintillator2 - FwCa gate", "Scintillator PMT 2", CATH);
			hSntr2CthdGFS = new Histogram("Scint2CathodeGFS", HIST_2D_INT, CHAN_2D,
				"Cathode vs Scintillator 2 - FwSc gate", "Scintillator PMT 2", CATH);
			//FrontCathode Gated on other
			hFrntCthdGSC = new Histogram("FrontCathodeGSC", HIST_2D_INT, CHAN_2D,
				"Cathode vs Front Position - ScCa gate", FRONT_POS, CATH);
			hFrntCthdGFS = new Histogram("FrontCathodeGFS ", HIST_2D_INT, CHAN_2D,
				"Cathode vs Front Position - FwSc gate ", FRONT_POS, CATH);
			//FrontScint Gated on other
			hFrntSntr2GSC = new Histogram("FrontScint2GSC ", HIST_2D_INT, CHAN_2D,
				"Scintillator 2 vs Front Position - ScCa gate", FRONT_POS,
				"Scintillator PMT 2");
			hFrntSntr2GFC = new Histogram("FrontScint2GFC", HIST_2D_INT, CHAN_2D,
				"Scintillator 2 vs Front Position - FwCa gate", FRONT_POS,
				"Scintillator PMT 2");
			//gated on FrontRear
			hSntr2Cthd_GFR = new Histogram("Scint2Cathode_GFR", HIST_2D_INT, CHAN_2D,
					"Cathode vs Scintillator2 - FR gate", "Scintillator PMT 2", CATH);
			hFrntCthd_GFR = new Histogram("FrontCathode_GFR", HIST_2D_INT, CHAN_2D,
					"Cathode vs Front Position - FR gate", FRONT_POS, CATH);
			hFrntSntr2_GFR = new Histogram("FrontScint2_GFR ", HIST_2D_INT, CHAN_2D,
					"Scintillator 2 vs Front Position - FR gate", FRONT_POS,
					"Scintillator PMT 2");
			
			/*
			 *  gated on 3 gates
			 */
			hFrntGCSF = new Histogram("FrontGCSF    ", HIST_1D_INT, ADC_CHANNELS,
				"Front Position - ScCa,FwCa,FwSc gates");
			hRearGCSF = new Histogram("RearGCSF    ", HIST_1D_INT, ADC_CHANNELS,
				"Rear Position - ScCa,FwCa,FwSc gates");
			hFrRightgCSF = new Histogram("FRONT_RIGHTGateCSF  ", HIST_2D_INT, TWO_D_HIRES,
				"Front vs. Rear - ScCa, FwCa, FwSc gates");
    //gated on 4 gates
    hFrntGAll = new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS,
        "Front Position - ScCa,FwCa,FwSc,FwRw gates");
    hRearGAll = new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS,
        "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
    hcFrntGAll = new Histogram("FrontGAllcmp ", HIST_1D_INT,
        CH_COMPRESS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
    hcRearGAll = new Histogram("RearGAllcmp ", HIST_1D_INT, CH_COMPRESS,
        "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");
    hFrontTheta = new Histogram("FrontTheta", HIST_2D_INT, 1024, TWO_D_HIRES,
        "Theta vs. Front Wire Position (X)", POS, "Theta");
    hRearTheta = new Histogram("RearTheta", HIST_2D_INT, 1024, TWO_D_HIRES,
        "Theta vs. RearWire Position (X)", POS, "Theta");
    hFC = new Histogram("FrontCorrected    ", HIST_1D_INT, ADC_CHANNELS,
        "Front Position - refocused");
   /* hCathTheta = new Histogram("CathodeTheta", HIST_2D_INT, 1024,
        TWO_D_HIRES,
        "Theta vs. Front Wire Position (X)", POS, "Theta");
   */
    // gates 1d
    gCthd = new Gate("Counts", hCthd);
    gFSum = new Gate("FrontSum", hFrontSum);
    gRSum = new Gate("RearSum", hRearSum);

    //gates  2d
	gSntr2Cthd   =new Gate("Ca-Sc", hSntr2Cthd);      //gate on Scintillator Cathode
	gFrntSntr2   =new Gate("Fw-Sc", hFrntSntr2);          //gate on Front Scintillator
    
   // gSntrCthd = new Gate("Ca-Sc", hSntrCthd);
    //gate on Scintillator Cathode
   // gFrntSntr = new Gate("Fw-Sc", hFrntSntr);
    //gate on Front Scintillator
    gFrntCthd = new Gate("Fw-Ca", hFrntCthd);
      

    gFrntRear = new Gate("Fw-Rw", hFrntPRearP);
    //gate on Front Rear
    gXY = new Gate("XY", hYvsPsn);
    // gate on x and y acceptance
    hYvsPsnGPID.addGate(gXY);
    gFrYvsReY = new Gate("FRONT_Y-REAR_Y", hFrntYRearY);
    // gate on y1 vs y2
    gFrontLvsR = new Gate("FrontLvsR", hFrontLvsR);
    gRearLvsR = new Gate("RearLvsR", hRearLvsR);
	hSntrCthd.addGate(gSntr2Cthd);
	hFrntSntr2GSC.addGate(gFrntSntr2);
	hFrntCthdGSC.addGate(gFrntCthd);
	hSntr2CthdGFC.addGate(gSntr2Cthd);
	hFrntSntr2GFC.addGate(gFrntSntr2);
	hSntr2CthdGFS.addGate(gSntr2Cthd);
    hFrntCthdGFS.addGate(gFrntCthd);
    hFrRightgCSF.addGate(gFrntRear);
	hFrntSntr2_GFR.addGate(gFrntSntr2);
	hFrntCthd_GFR.addGate(gFrntCthd);
	hSntr2Cthd_GFR.addGate(gSntr2Cthd);
	
    //scalers
    sBic = new Scaler("BIC", 0);
    sClck = new Scaler("Clock", 1);
    sEvntRaw = new Scaler("Event Raw", 2);
    sEvntAccpt = new Scaler("Event Accept", 3);
    sScint = new Scaler(SCINT, 4);
    sCathode = new Scaler(CATH, 5);
    sFCLR = new Scaler("FCLR", 6);
    sNMR = new Scaler("NMR", 14);

    //monitors
    mBeam = new Monitor("Beam ", sBic);
    mClck = new Monitor("Clock", sClck);
    mEvntRaw = new Monitor("Raw Events", sEvntRaw);
    mEvntAccept = new Monitor("Accepted Events", sEvntAccpt);
    mScint = new Monitor(SCINT, sScint);
    mCathode = new Monitor(CATH, sCathode);
    mFCLR = new Monitor("FCLR", sFCLR);
    mNMR = new Monitor("NMR", sNMR);
    new Monitor(DEAD_TIME, this);

    pThOffset = new DataParameter("THETA_OFFSET");
    pXTheta = new DataParameter("(X|Theta)    ");
    pCTheta = new DataParameter("CTheta");

  }


  /**
   *  Description of the Method
   *
   * @param  dataEvent      Description of the Parameter
   * @exception  Exception  Description of the Exception
   */
  public void sort(int[] dataEvent) throws Exception {
    /*
     *  unpack data into convenient names
     */
    int eCthd = dataEvent[idCthd];
	final int eMonitor = dataEvent[idMonitor];
    final int SCINTR = dataEvent[idScintR];
    final int SCINTL = dataEvent[idScintL];
    final int FPOS = dataEvent[idFrntPsn];
    final int RPOS = dataEvent[idRearPsn];
    final int FHEIGHT = dataEvent[idFrntHgh];
    final int RHEIGHT = dataEvent[idRearHgh];
    final int FRONT_Y = dataEvent[idYFrnt];
    final int REAR_Y = dataEvent[idYRear];
    final int FRONT_RIGHT = dataEvent[idFrontRight];
    final int FRONT_LEFT = dataEvent[idFrontLeft];
    final int REAR_RIGHT = dataEvent[idRearRight];
    final int REAR_LEFT = dataEvent[idRearLeft];
    final int FMPH = dataEvent[idFrMidPH];
    final int FBPH = dataEvent[idFrBackPH];
    final int RFPH = dataEvent[idReFrPH];
    final int RBPH = dataEvent[idRearBackPH];
   
    //final int eFFph = FHEIGHT;
    //final int eRMph = RHEIGHT;
    /*
     *  proper way to add for 2 phototubes at the ends of
     *  scintillating rod see Knoll
     */
    final int SCINT = (int) Math.round(Math.sqrt(SCINTR * SCINTL));
	final int SCINT1 = SCINTR >> TWO_D_FACTOR;
	final int SCINT2 = SCINTL >> TWO_D_FACTOR; 
    final int FPOS_COMPR = FPOS >> TWO_D_FACTOR;
    final int RPOS_COMPR = RPOS >> TWO_D_FACTOR;
    final int FY_COMPR = FRONT_Y >> TWO_D_FACTOR;
    final int RY_COMPR = REAR_Y >> TWO_D_FACTOR;
    final int FHEIGHT_COMP = FHEIGHT >> TWO_D_FACTOR;
    final int RHEIGHT_COMP = RHEIGHT >> TWO_D_FACTOR;
    final int SCINT_COMPR = SCINT >> TWO_D_FACTOR;
    final double X_THETA = pXTheta.getValue();
    /*
     *  use this to correct the focus for different particle groups with
     *  different kinematics
     */
    final double THETA_OFFSET = pThOffset.getValue();
    // center channel of Theta distribution
    final double THETA_CENTER = pCTheta.getValue();
    final int THETA_CHANNEL;
    final double DELTA_CATHODE;
    final double THETA_VAL;
    if (FPOS > 0 && RPOS > 0) {
      THETA_CHANNEL = RPOS - FPOS;
      //theta = THETA_CHANNEL;
      THETA_VAL = THETA_CHANNEL - THETA_OFFSET;
      DELTA_CATHODE = (THETA_CENTER * (THETA_VAL));
    } else {
      THETA_CHANNEL = 0;
      DELTA_CATHODE = 0;
      THETA_VAL = 0;
    }
    eCthd += (int) DELTA_CATHODE;
    final int ecCthd = eCthd >> TWO_D_FACTOR;

    // singles spectra
    hCthd.inc(eCthd);
    hSntr1.inc(SCINTR);
    hSntr2.inc(SCINTL);
    hSntrSum.inc(SCINT);
    hFrntPsn.inc(FPOS);
    hRearPsn.inc(RPOS);
    hFrntY.inc(FRONT_Y);
    hRearY.inc(REAR_Y);
    hFrFrPH.inc(FHEIGHT);
    hFrMidPH.inc(FMPH);
    hFrontBackPH.inc(FBPH);
    hRearFrontPH.inc(RFPH);
    hRearMidPH.inc(RHEIGHT);
    hRearBackPH.inc(RBPH);
	hMonitor.inc(eMonitor);
  
    /*
     *  The following 8 histograms are a different way to measure the wire
     *  positions
     *  -Jac, 9/9/2003
     */
    hFrontRight.inc(FRONT_RIGHT);
    hFrontLeft.inc(FRONT_LEFT);
    hRearRight.inc(REAR_RIGHT);
    hRearLeft.inc(REAR_LEFT);
    hFrontSum.inc(FRONT_RIGHT + FRONT_LEFT);
    final int FL_COMPR_HI = FRONT_LEFT >> HIRES_FACTOR;
    final int FR_COMPR_HI = FRONT_RIGHT >> HIRES_FACTOR;
    hFrontLvsR.inc(FL_COMPR_HI, FR_COMPR_HI);
    final int RL_COMPR_HI = REAR_LEFT >> HIRES_FACTOR;
    final int RR_COMPR_HI = REAR_RIGHT >> HIRES_FACTOR;
    hRearLvsR.inc(RL_COMPR_HI, RR_COMPR_HI);
    int eFnew = 0;
    if ((FRONT_RIGHT > 0) && (FRONT_LEFT > 0)) {
      eFnew = FRONT_LEFT - FRONT_RIGHT + 4095;
      hFrontNew.inc(eFnew);
    }
    final int FPOS_COMP_HI = FPOS >> HIRES_FACTOR;
    final int RPOS_COMP_HI = RPOS >> HIRES_FACTOR;
    hRearSum.inc(REAR_RIGHT + REAR_LEFT);
    if ((REAR_RIGHT > 0) && (REAR_LEFT > 0)) {
      final int eRnew = REAR_LEFT - REAR_RIGHT + 4095;
      hRearNew.inc(eRnew);
    }
    int iFC = (int) (FPOS + THETA_VAL * X_THETA);
    hFC.inc(iFC);

	hSntr1Sntr2.inc(SCINT1,SCINT2);
    hYvsPsn.inc(FPOS_COMPR, FY_COMPR);
    hFrntPH.inc(FPOS_COMPR, FHEIGHT_COMP);
    hRearPH.inc(RPOS_COMPR, RHEIGHT_COMP);
	hSntr2Cthd.inc(SCINT2, ecCthd);
    hSntrCthd.inc(SCINT_COMPR, ecCthd);
	hFrntCthd.inc(FPOS_COMPR, ecCthd);
	hRearCthd.inc(RPOS_COMPR, ecCthd);
    hFrntSntr.inc(FPOS_COMPR, SCINT_COMPR);
	hFrntSntr1.inc(FPOS_COMPR, SCINT1);
	hFrntSntr2.inc(FPOS_COMPR, SCINT2);
    hFrntPRearP.inc(FPOS_COMP_HI, RPOS_COMP_HI);

    hRWbias.inc(dataEvent[idRWbias]);
    hFWbias.inc(dataEvent[idFWbias]);
    hBCIRange.inc(dataEvent[idBCIRange]);

   // final boolean SC_INGATE = gSntrCthd.inGate(SCINT_COMPR, ecCthd);
    final boolean SC_INGATE = gSntr2Cthd.inGate(SCINT2,ecCthd);
    final boolean FC_INGATE = gFrntCthd.inGate(FPOS_COMPR, ecCthd);
	final boolean FS_INGATE = gFrntSntr2.inGate(FPOS_COMPR,SCINT2);
    //final boolean FS_INGATE = gFrntSntr.inGate(FPOS_COMPR, SCINT_COMPR);
    final boolean XY_INGATE = gXY.inGate(FPOS_COMPR, FY_COMPR);
    final boolean YY_INGATE = gFrYvsReY.inGate(FY_COMPR, RY_COMPR);
    final boolean IN_PID_GATES = SC_INGATE && FC_INGATE && FS_INGATE;
    final boolean FR_RE_INGATE = gFrntRear.inGate(FPOS_COMP_HI, RPOS_COMP_HI);
    final boolean IN_SUM_GATES = gFSum.inGate(FRONT_RIGHT + FRONT_LEFT) &&
        gRSum.inGate(REAR_RIGHT + REAR_LEFT);
    final boolean GOOD_DIREC = XY_INGATE && FR_RE_INGATE && YY_INGATE;
    final boolean ACCEPT = GOOD_DIREC && IN_SUM_GATES;
    final boolean GOOD = GOOD_DIREC && IN_PID_GATES;

    if (SC_INGATE) {
      // gate on Scintillator vs Cathode
      //hFrntSntrGSC.inc(FPOS_COMPR, SCINT_COMPR);
	  hFrntSntr2GSC.inc(FPOS_COMPR, SCINT2);
      hFrntCthdGSC.inc(FPOS_COMPR, ecCthd);
    }
    if (FC_INGATE) {
      // gate on Front Wire Position vs Cathode
     /* hSntrCthdGFC.inc(SCINT_COMPR, ecCthd);
      hFrntSntrGFC.inc(FPOS_COMPR, SCINT_COMPR);*/
	  hSntr2CthdGFC.inc(SCINT2, ecCthd);
	  hFrntSntr2GFC.inc(FPOS_COMPR, SCINT2);
    }
    if (FS_INGATE) {
      // gate on Front Wire Position vs Scintillator
      //hSntrCthdGFS.inc(SCINT_COMPR, ecCthd);
	    hSntr2CthdGFS.inc(SCINT2, ecCthd);
        hFrntCthdGFS.inc(FPOS_COMPR, ecCthd);
    }
    if(FR_RE_INGATE){
    	//gated on FR spectra
		hSntr2Cthd_GFR.inc(SCINT2, ecCthd);
		hFrntSntr2_GFR.inc(FPOS_COMPR, SCINT2);
		hFrntCthd_GFR.inc(FPOS_COMPR, ecCthd);
    }
    if (IN_PID_GATES) {
      // gated on all 3 gate above
      //writeEvent(dataEvent);
      hFrntGCSF.inc(FPOS);
      hRearGCSF.inc(RPOS);
      hFrRightgCSF.inc(FPOS_COMP_HI, RPOS_COMP_HI);
      hFrntYRearY.inc(FY_COMPR, RY_COMPR);
      hYvsPsnGPID.inc(FPOS_COMPR, FY_COMPR);
      if (GOOD && IN_SUM_GATES) {
        writeEvent(dataEvent);
        hFrntGAll.inc(FPOS);
        hRearGAll.inc(RPOS);
        hcFrntGAll.inc(FPOS >> COMPRESS_FAC);
        hcRearGAll.inc(RPOS >> COMPRESS_FAC);
        hFrontTheta.inc(FPOS >> 2, THETA_CHANNEL);
        hRearTheta.inc(RPOS >> 2, THETA_CHANNEL);
        }
    }

  }


  /**
   * Called so the dead time can be calculated.
   *
   * @param  name  name of monitor to calculate
   * @return       floating point value of monitor
   */
  public double monitor(String name) {
    double rval = 0.0;
    if (name.equals(DEAD_TIME)) {
      final double acceptRate = mEvntAccept.getValue();
      final double rawRate = mEvntRaw.getValue();
      if (acceptRate > 0.0 && acceptRate <= rawRate) {
        rval = 100.0 * (1.0 - acceptRate / rawRate);
      }
    }
    return rval;
  }
}

