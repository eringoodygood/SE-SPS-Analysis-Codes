
/*
 * Created on Jul 11, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
/*
 */
package sort.newVME;
import jam.data.Gate;
import jam.data.Histogram;
import jam.data.Monitor;
import jam.data.Scaler;
import jam.sort.SortRoutine;
import jam.data.*;
import jam.sort.*;

 
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.StreamTokenizer;

//import dwvisser.analysis.ArrayCalibration;

/** Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>, which
 * was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right way; also 
 * added gate to cathAnde. Now the data shipped across
 * is in the format as produced by the V7x5's.  The EventInputStream this code
 * works with is YaleCAEN_InputStream.
 * Modified 11 February 2002 to include a fourth ADC that has channels
 * to monitor the wire biases, YLSA leakage currents, and backplane signals
 * from the YLSA detectors.
 * 26 July 2002 - copied from Jac4adcs_online in june02 package.
 * I will add FP corrections as determined in June to this.
 *
 * @author Dale Visser
 * @since 26 July 2001
 */
public class SplitPoleYLSA_TAJ_chris_gscint2_tests_2 extends SortRoutine {
	//VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000,0x20030000};
static final int [] TDC_BASE = {0x30000000,0x30010000,0x30020000};
	static final int SCALER_ADDRESS = 0xf0e00000;
	//static final int THRESHOLDS = 200;  //ADC lower threshold in channels
	private final static int THRESHOLDS = 128; //FROM TAJ
	static final int TIME_THRESHOLDS = 30;//TDC lower threshold in channels
	static final int TIME_RANGE = 1200;//ns
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
	final int TWO_D_HIRES = 512;
	//bits to shift for compression
	final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
	final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));
	final int TWO_D_HR_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_HIRES)/Math.log(2.0)));

	//histograms and parameter ID's for YLSA channels
	Histogram [][] hEnergies = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	Histogram [][] hTimes = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	int [][] idEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	int [][] idTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

//from TAJ
  /*
   *  ungated spectra
   */
  private transient Histogram hCthd, hSntr1, hSntr2, hSntrSum,
	  hSntr1Sntr2, hSntr1_Test, hSntr2_Test, hSntr1_Test_G, hSntr2_Test_G, hFrntPsn, hRearPsn, hMonitor;
  /*
   *  Rear Wire Pulse Height
   */
  private transient Histogram hFrntPH;
  /*
   *  position x height y
   */
  private transient Histogram hRearPH;
  private transient Histogram hSntr2Cthd, hFrntCthd, hRearCthd,
	  hFrntSntr, hFrntSntr1, hFrntSntr2, hFrntPRearP, hFrntY, hRearY,
	  hYvsPsn, hYvsPsnGPID, hFrntYRearY;

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
 // private transient Histogram /*hFWbias, hRWbias, */hBCIRange;

  
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
  /*
   *  gate by Front wire Cathode
   */
  private transient Histogram hSntr2CthdGFC, hFrntSntr2GFC;
  private transient Histogram hSntr2CthdGFS, hFrntCthdGFS;
  /*
   *  gate by Front wire Scintillator
   */
  private transient Histogram hFrntGCSF, hRearGCSF, hFrRightgCSF, hFrntGAll,
	  hRearGAll;
	  
	private transient Histogram hcFrntGTime, hFrntGTime, hFrntGAllGTime;//front and rear wire gated on All compressed & time
  /*
   *  front and rear wire gate on all
   */
  private transient Histogram hcFrntGAll, hcRearGAll;

  private transient Histogram /*hFrPosProton, hFrPosDeuts, hFrPosTriton,
	  hFrntGAll_a, */ hCathTheta;

  /*
   *  1D gates
   */
  private transient Gate gCthd, gPeak, gFSum, gRSum;
  /*
   *  2D gates
   */
  private transient Gate gSntr2Cthd, gFrntSntr2, gFrntCthd, gFrntRear,
	  gXY, gFrYvsReY, gFrontLvsR, gRearLvsR, gScintTest, gScintTestG
	  /*gSntrCthd_p, gSntrCthd_d, gSntrCthd_t, gSntrCthd_a,
	  gFrntSntr_p, gFrntSntr_d, gFrntSntr_t, gFrntSntr_a,
	  gFrntCthd_p, gFrntCthd_d, gFrntCthd_t, gFrntCthd_a*/;

  /*
   *  Scalers and monitors
   */
  private transient Scaler /*sBic,*/ sClck, sEvntRaw, sEvntAccpt, sScint, sCathode,
	  /*sNMR,*/ sFCLR, sMon, sGate, sBusy;
  /*
   *  number of FCLR's that went to ADC's
   */
  private transient Monitor /*mBeam,*/ mClck, mEvntRaw, mEvntAccept, mScint,
	  mCathode,/* mNMR,*/ mFCLR, mMon, mGate, mBusy;

  /*
   *  id numbers for the signals;
   */
  private transient int idCthd, idScintR, idScintL, idFrntPsn,
	  idRearPsn, idFrntHgh, idRearHgh, idYFrnt, idYRear, idFWbias, idRWbias,
	 /* idBCIRange,*/ idFrontLeft, idFrontRight, idRearLeft, idRearRight, idMonitor,
	  idFrMidPH, idFrBackPH, idReFrPH, idRearBackPH;
  private transient DataParameter pXTheta, pThOffset, pCTheta;



	// OLD DET and YLSA
    
   // Gate /*gSilicon*/ gCthd;
	//gates 1 d
   
	Gate gFRexclude;
    

	Histogram hHits, hEvsStrip, hTvsStrip, hTimeHits, hTvsEhits, hInterHits,hEnergyHits;
	Histogram hHitsRatio;
	Histogram hMultiplicity;
	Histogram hEvsStripGA,  hTvsStripGA, hEvsStripGA_gAll,  hTvsStripGA_gAll; //gain adjusted spectra
	Histogram hAngDist; //for getting angular distribution of alphas


	//coincidence histograms & gates
	Histogram hTimeGA,hTimeGAstate, hTimeGAdecay;//all strips time gain adjusted (TOF adjustment for alpha)
	Histogram hEvsStripGA_Time;//Esilvsstrip gated on time peak
	Gate gTimeBroad,gTimeState,gTimeDecay;
	Histogram hEvsStripBroad, hEvsStripState, hEvsStripDecay;
	Gate gEvsS;
	Histogram hEvsChBroad, hEvsChState, hEvsChDecay;
	Histogram hEsilVsFP, hEsilVsFP_gTime;
	Histogram hFPGAll_St_cmp;
		Histogram hFPGAll_St_m1;
	Gate gEsilVsFP_m1;
	Histogram hFPGAll_St_m2;
	Gate gEsilVsFP_m2;
	Histogram hEsilVsFP_peak;
	Histogram hEsilVsFP_peak_time;
	Histogram hRYvsFY;
	//id numbers for the signals;
   
	int idFrontBias, idRearBias;
	Histogram hFrontBias, hRearBias;
	int [] idLeakageCurrent = new int[NUM_DETECTORS];
	Histogram [] hLeakageCurrent = new Histogram[NUM_DETECTORS];
	Gate [] gLeakageCurrent = new Gate[NUM_DETECTORS];
	//Monitor [] mLeakageCurrent = new Monitor[NUM_DETECTORS];
	int [] idBackPlane = new int[NUM_DETECTORS];
	Histogram [] hBackPlane = new Histogram[NUM_DETECTORS];
    
   // Scaler [] sYLSArate = new Scaler[NUM_DETECTORS];
   // Monitor [] mYLSArate = new Monitor[NUM_DETECTORS];
    
	Histogram hFRhires, hFrntGTime_noE, hTvsStripGA_noE, hEsilVsFP_noE;
	Gate gTvsStripGA_noE;
	Histogram hFrntNewGAll, hcFrntNewGAll;
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
	Histogram hEsilVsFP_m1,hEsilVsFP_m2,hEsilVsFP_m3;

	//ResidualKinematics rk;//for calculating Residual trajectory
	//double Mproton;
	//double mAlpha;

	ArrayCalibration ac;
	//Histogram hEcmVsStripBroad,hEcmVsFP;
	/** Sets up objects, called when Jam loads the sort routine.
	 * @throws Exception necessary for Jam to handle exceptions
	 */
	public void initialize() throws Exception {
		retrieveCalibration();
		//readADCthresholds();
		setADCthresholdsToE();
		//setupCorrections(); CHANGED!!!!!!!!
		vmeMap.setScalerInterval(3);
		for (int i=0; i < TDC_BASE.length; i++){
			vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
		}
		//TAJ
		  
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
		//	idBCIRange = vmeMap.eventParameter(2, ADC_BASE[0], 14, 16);
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
				HIST_2D_INT, TWO_D_CHANNELS,
				"Scintillator PMT 2 versus Scintillator PMT 1   ", "Scint 1", "Scint2");
			hSntr1_Test = new Histogram("Scint1 Test", HIST_1D_INT, ADC_CHANNELS, "Scintilltor PMT 1 Test");
		    hSntr2_Test = new Histogram("Scint2 Test", HIST_1D_INT, ADC_CHANNELS, "Scintilltor PMT 2 Test");
		    hSntr1_Test_G = new Histogram("Scint1 Test FGAll", HIST_1D_INT, ADC_CHANNELS, "Scintilltor PMT 1 Test on FGAll");
			hSntr2_Test_G = new Histogram("Scint2 Test FGAll", HIST_1D_INT, ADC_CHANNELS, "Scintilltor PMT 2 Test on FGAll");
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
			/*hRWbias = new Histogram("RearBias    ", HIST_1D_INT,
				ADC_CHANNELS, "Rear Wire Bias");
			hFWbias = new Histogram("FrontBias    ", HIST_1D_INT,
				ADC_CHANNELS, "Front Wire Bias");*/
	/*		hBCIRange = new Histogram("BCIRange", HIST_1D_INT, ADC_CHANNELS,
				"BCI Full Scale Range - about 330 mV/position");*/
			hFrntY = new Histogram("Front Y", HIST_1D_INT, ADC_CHANNELS,
				"Y (vertical) Position at Front Wire");
			hRearY = new Histogram("Rear Y", HIST_1D_INT, ADC_CHANNELS,
				"Y (vertical) Position at Rear Wire");
			final String FRONT_POS = "Front Position";
			hFrntPH = new Histogram("FrontPvsHeight", HIST_2D_INT,
				TWO_D_CHANNELS,
				"Pulse Height of FrontFront wire vs Front Position",
				FRONT_POS,
				"Pulse Height");
			hRearPH = new Histogram("RearPvsHeight ", HIST_2D_INT,
				TWO_D_CHANNELS,
				"Pulse Height of RearMiddle wire vs Rear Position",
				"Rear Position", "Pulse Height");
			final String POS = "Position";
			hYvsPsn = new Histogram("Y vs Position", HIST_2D_INT, TWO_D_CHANNELS,
				"Front Y vs. Front Wire Position (X)",
				POS,
				"Y");
			hYvsPsnGPID = new Histogram("YvsPosnPID", HIST_2D_INT, TWO_D_CHANNELS,
				"Front Y vs. Front Wire Position (X) Gated on PID", POS, "Y");
			final String SCINT = "Scintillator";
			final String CATH = "Cathode";
			hSntr2Cthd = new Histogram("Scint2Cathode  ", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Scintillator PMT2", "Scintillator PMT 2", CATH);
			hFrntCthd = new Histogram("FrontCathode  ", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Front Position", FRONT_POS, CATH);
			hRearCthd = new Histogram("RearCathode  ", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Rear Position", "Rear Position", CATH);
			hFrntSntr = new Histogram("FrontScint ", HIST_2D_INT, TWO_D_CHANNELS,
				"Scintillator vs Front Position", FRONT_POS, SCINT);
			hFrntSntr1 = new Histogram("FrontScint1 ", HIST_2D_INT, TWO_D_CHANNELS,
				"Scintillator PMT 1 vs Front Position", FRONT_POS, "Scintillator 1");
			hFrntSntr2 = new Histogram("FrontScint2 ", HIST_2D_INT, TWO_D_CHANNELS,
				"Scintillator PMT 2 vs Front Position", FRONT_POS, "Scintillator 2");
			hFrntPRearP = new Histogram("FrontRear  ", HIST_2D_INT, TWO_D_HIRES,
				"Rear Position vs Front Position", FRONT_POS, "Rear Position");
			hFrntYRearY = new Histogram("FrontY_RearY  ", HIST_2D_INT, TWO_D_CHANNELS,
				"Rear Y Position vs Y Front Position", "Front Y Position",
				"Rear Y Position");
			hMonitor= new Histogram("Monitor", HIST_1D_INT,ADC_CHANNELS, "Monitor");	
			//ScintCathode Gated on other
			hSntr2CthdGFC = new Histogram("Scint2CathodeGFC", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Scintillator2 - FwCa gate", "Scintillator PMT 2", CATH);
			hSntr2CthdGFS = new Histogram("Scint2CathodeGFS", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Scintillator 2 - FwSc gate", "Scintillator PMT 2", CATH);
			//FrontCathode Gated on other
			hFrntCthdGSC = new Histogram("FrontCathodeGSC", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Front Position - ScCa gate", FRONT_POS, CATH);
			hFrntCthdGFS = new Histogram("FrontCathodeGFS ", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Front Position - FwSc gate ", FRONT_POS, CATH);
			//FrontScint Gated on other
			hFrntSntr2GSC = new Histogram("FrontScint2GSC ", HIST_2D_INT, TWO_D_CHANNELS,
				"Scintillator 2 vs Front Position - ScCa gate", FRONT_POS,
				"Scintillator PMT 2");
			hFrntSntr2GFC = new Histogram("FrontScint2GFC", HIST_2D_INT, TWO_D_CHANNELS,
				"Scintillator 2 vs Front Position - FwCa gate", FRONT_POS,
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
		/*	hFrPosProton = new Histogram("FrontGAll_p  ", HIST_1D_INT, ADC_CHANNELS,
				"Front Position - ScCa,FwCa,FwSc,FwRw, proton gates");
			hFrPosDeuts = new Histogram("FrontGAll_d  ", HIST_1D_INT, ADC_CHANNELS,
				"Front Position - ScCa,FwCa,FwSc,FwRw, deuteron gates");
			hFrPosTriton = new Histogram("FrontGAll_t  ", HIST_1D_INT, ADC_CHANNELS,
				"Front Position - ScCa,FwCa,FwSc,FwRw, triton gates");
			hFrntGAll_a = new Histogram("FrontGAll_a  ", HIST_1D_INT, ADC_CHANNELS,
				"Front Position - ScCa,FwCa,FwSc,FwRw, alpha gates");*/
			hRearGAll = new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS,
				"Rear Position - ScCa,FwCa,FwSc,FwRw gates");
			hcFrntGAll = new Histogram("FrontGAllcmp ", HIST_1D_INT,
				COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
			hcRearGAll = new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS,
				"Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");
			hFrontTheta = new Histogram("FrontTheta", HIST_2D_INT, 1024, TWO_D_HIRES,
				"Theta vs. Front Wire Position (X)", POS, "Theta");
			hRearTheta = new Histogram("RearTheta", HIST_2D_INT, 1024, TWO_D_HIRES,
				"Theta vs. RearWire Position (X)", POS, "Theta");
			hFC = new Histogram("FrontCorrected    ", HIST_1D_INT, ADC_CHANNELS,
				"Front Position - refocused");
			hCathTheta = new Histogram("CathodeTheta", HIST_2D_INT, 1024,
				TWO_D_HIRES,"Theta vs. Front Wire Position (X)", POS, "Theta");
			
       
		hFrntGTime_noE  = new Histogram("FrontGTime_noE", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw & time gates, no \"energy condition\"");//FROM OLD DET
		hFrntGTime  = new Histogram("FrontGTime    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw & time gates");
		hFrntGAllGTime  = new Histogram("FrontGAllGTime    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw & time gates");
		hcFrntGTime  =new Histogram("FrontGTimecmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw & time gates");
      
		//200 mV = 0.40 uA, so 5 mV = 0.01 uA, or 1 mV(ch) = 2 nA
		//--->500 channels/uA
		//YLSA
		for (int i=0; i<NUM_DETECTORS; i++) {
			//CHANGE THRES
			idLeakageCurrent[i] = vmeMap.eventParameter(8, ADC_BASE[3], i, THRESHOLDS);
			hLeakageCurrent[i]= new Histogram("Leakage "+i, HIST_1D_INT, ADC_CHANNELS, "Leakage Current of Detector "+i, "500 channels/uA", "Counts");
			gLeakageCurrent[i] = new Gate("Leakage "+i,hLeakageCurrent[i]);
			//mLeakageCurrent[i] = new Monitor("Leakage "+i,this);
		   // sYLSArate[i] = new Scaler("YLSA hits "+i,11+i);
		   // mYLSArate[i] = new Monitor("YLSA rate "+i,sYLSArate[i]);
			for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
				//eventParameter(slot, base address, channel, threshold channel)
				idEnergies[i][j]=vmeMap.eventParameter(whichADCslot(i), 
				whichADCaddress(i), whichADCchannel(i,j), thresholds[i][j]);
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
		hHitsRatio = new Histogram("Hits Ratio", HIST_1D_DBL, NUM_DETECTORS*STRIPS_PER_DETECTOR,
		"Ratio of 'Time Hits' to 'Hits'");
		hEnergyHits = new Histogram("Energy Hits", HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Hits over ADC threshold",
		"Strip","Counts");
		hTvsEhits = new Histogram("T vs E hits", HIST_2D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Time hits vs Energy hits",
		"E hits", "T hits");
		hEvsStrip = new Histogram("EvsStrip",HIST_2D_INT, TWO_D_CHANNELS,NUM_DETECTORS*STRIPS_PER_DETECTOR,
		"Energy vs. Strip, All Detectors", "Energy",
		STRIPS_PER_DETECTOR+"*Det+Strip");
		hTvsStrip = new Histogram("TvsStrip",HIST_2D_INT, TWO_D_CHANNELS,NUM_DETECTORS*STRIPS_PER_DETECTOR,
		"Time vs. Strip, All Detectors, multiplicity one", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
		/*hTvsStripPID = new Histogram("TvsStripPID",HIST_2D_INT, TWO_D_CHANNELS,
		"Time vs. Strip, All Detectors, multiplicity one,PID gates", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");*/
		hEvsStripGA = new Histogram("EvsStripGA",HIST_2D_INT, 500,NUM_DETECTORS*STRIPS_PER_DETECTOR,
		"Energy vs. Strip, All Detectors, Gain Adjusted", "Energy (20 keV/ch)", STRIPS_PER_DETECTOR+"*Det+Strip");
		hTvsStripGA = new Histogram("TvsStripGA",HIST_2D_INT, TWO_D_CHANNELS,NUM_DETECTORS*STRIPS_PER_DETECTOR,
		"Time vs. Strip, All Detectors, Gain Adjusted", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
		hEvsStripGA_gAll = new Histogram("EvsStripGA_gAll",HIST_2D_INT, 500,NUM_DETECTORS*STRIPS_PER_DETECTOR,
		"Energy vs. Strip, All Detectors, Gain Adjusted, gated on FP", "Energy (20 keV/ch)", STRIPS_PER_DETECTOR+"*Det+Strip");
		hEvsStripGA_Time = new Histogram("EvsStripGA_Time",HIST_2D_INT, 500,NUM_DETECTORS*STRIPS_PER_DETECTOR,
				"Energy vs. Strip, All Detectors, Gain Adjusted gated on time peak", "Energy (20 keV/ch)", STRIPS_PER_DETECTOR+"*Det+Strip");
		hTvsStripGA_gAll = new Histogram("TvsStripGA_gAll, gated on FP",HIST_2D_INT, TWO_D_CHANNELS,NUM_DETECTORS*STRIPS_PER_DETECTOR,
		"Time vs. Strip, All Detectors, Gain Adjusted", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
		hTvsStripGA_noE = new Histogram("TvsStripGA_noE",HIST_2D_INT, TWO_D_CHANNELS,NUM_DETECTORS*STRIPS_PER_DETECTOR,
		"Time vs. Strip, All Detectors, Gain Adjusted, no \"E condition\"", "Time", STRIPS_PER_DETECTOR+"*Det+Strip");
		gTvsStripGA_noE = new Gate("Time_NoE",hTvsStripGA_noE);
		hEvsStripBroad = new Histogram("EvsSbroad",HIST_2D_INT
		,TWO_D_CHANNELS,STRIPS_PER_DETECTOR+1,
		"Strip vs. Energy Deposited","Energy","Strip");
		hEsilVsFP = new Histogram("EsilVsFP", HIST_2D_INT,ADC_CHANNELS,100,
		"E (YLSA) vs. Focal Plane Position","QBrho [channels]","E [MeV] * 10");
		hEsilVsFP_peak = new Histogram("EsilVsFP_peak", HIST_2D_INT,TWO_D_CHANNELS,100,
		"E (YLSA) vs. Focal Plane Position gated on peak in FP","QBrho [channels]","E [MeV] * 10");
		hEsilVsFP_peak_time = new Histogram("EsilVsFP_peak_time", HIST_2D_INT,TWO_D_CHANNELS,100,
		"E (YLSA) vs. Focal Plane Position gated on peak in FP and time peak","QBrho [channels]","E [MeV] * 10");
		hEsilVsFP_gTime = new Histogram("EsilVsFP_gTime", HIST_2D_INT,ADC_CHANNELS,100,
		"E (YLSA) vs. Focal Plane Position gated on time peak","QBrho [channels]","E [MeV] * 10");
		hFPGAll_St_cmp = new Histogram("FrontGAll_St_cmp    ", HIST_1D_INT,
		COMPRESSED_CHANNELS, "Front Position Compressed - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP");
		hFPGAll_St_m1 = new Histogram("FrontGAll_St_m1    ", HIST_1D_INT, ADC_CHANNELS,
		"Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1");
		hFPGAll_St_m2 = new Histogram("FrontGAll_St_m2    ", HIST_1D_INT, ADC_CHANNELS,
		"Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m2");
		hEsilVsFP_m1 = new Histogram("EsilVsFP_m1", HIST_2D_INT,TWO_D_CHANNELS,100,
		"E (YLSA) vs. Focal Plane Position, multiplicity=1","QBrho [channels]","E [MeV] * 10");
		hEsilVsFP_m2 = new Histogram("EsilVsFP_m2", HIST_2D_INT,TWO_D_CHANNELS,100,
		"E (YLSA) vs. Focal Plane Position, multiplicity=2","QBrho [channels]","E [MeV] * 10");
		hEsilVsFP_m3 = new Histogram("EsilVsFP_m3", HIST_2D_INT,TWO_D_CHANNELS,100,
		"E (YLSA) vs. Focal Plane Position, multiplicity=3","QBrho [channels]","E [MeV] * 10");
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
		gEsilVsFP_m1 = new Gate("EsilVsFP_m1",hEsilVsFP_m1);
		gEsilVsFP_m2 = new Gate("EsilVsFP_m2",hEsilVsFP_m2);
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

		// gates 1d-OLD
		gCthd   =new Gate("Counts", hCthd);
		gPeak   =new Gate("Peak", hFrntGAll);
		//hFrntGTime.addGate(gPeak);
        
		//FROM TAJ
			gFSum = new Gate("FrontSum", hFrontSum);
			gRSum = new Gate("RearSum", hRearSum);
			
		//gates  2d-OLD
		gSntr2Cthd   =new Gate("Ca-Sc", hSntr2Cthd);      //gate on Scintillator Cathode
		gFrntSntr2   =new Gate("Fw-Sc", hFrntSntr2);          //gate on Front Scintillator
		gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
		gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
		gFRexclude = new Gate("FRexclude", hFrntPRearP);
		Gate gDataEvent = new Gate("DataEvent",hSntr2Cthd);
      
//		FROM TAJ
		gXY = new Gate("XY", hYvsPsn);
		// gate on x and y acceptance
		hYvsPsnGPID.addGate(gXY);
		gFrYvsReY = new Gate("FRONT_Y-REAR_Y", hFrntYRearY);
		// gate on y1 vs y2
		gFrontLvsR = new Gate("FrontLvsR", hFrontLvsR);
		gRearLvsR = new Gate("RearLvsR", hRearLvsR);

//Scint tests
		gScintTest = new Gate("ScintTest", hFrntPsn);
		gScintTestG = new Gate("ScintTestG", hFrntGAll);


		hFrntSntr2GSC.addGate(gFrntSntr2);
		hFrntCthdGSC.addGate(gFrntCthd);
		hSntr2CthdGFC.addGate(gSntr2Cthd);
		hFrntSntr2GFC.addGate(gFrntSntr2);
		hSntr2CthdGFS.addGate(gSntr2Cthd);
		hFrntCthdGFS.addGate(gFrntCthd);
		hFrRightgCSF.addGate(gFrntRear);



 
		//scalers
	 //   sBic      =new Scaler("BIC",0);
		sClck      =new Scaler("Clock",1);
		sEvntRaw    =new Scaler("Event Raw", 2);
		sEvntAccpt  =new Scaler("Event Accept",3);
		sScint    =new Scaler("Scintillator", 4);
		sCathode  =new Scaler("Cathode",5);
		sFCLR = new Scaler("FCLR",6);
		sMon = new Scaler("Monitor",7);
		sGate = new Scaler("Gate",9);
		sBusy = new Scaler("Busy",10);
        

		//monitors
	 //   mBeam=new Monitor("Beam ",sBic);
		mClck=new Monitor("Clock",sClck);
		mEvntRaw=new Monitor("Raw Events",sEvntRaw);
		mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
		mScint=new Monitor("Scintillator",sScint);
		mCathode=new Monitor("Cathode",sCathode);
		new Monitor("DataEvent", gDataEvent);
		mFCLR = new Monitor("FCLR",sFCLR);
		Monitor mDeadTime=new Monitor(DEAD_TIME, this);
		Monitor mTrueDeadTime = new Monitor(TRUE_DEAD_TIME, this);
		mMon=new Monitor("Monitor",sMon);
		mGate = new Monitor("Gate",sGate);
		mBusy = new Monitor("Busy",sBusy);
        
		//TAJ
		pThOffset = new DataParameter("THETA_OFFSET");
		pXTheta = new DataParameter("(X|Theta)    ");
		pCTheta = new DataParameter("CTheta");
        
		System.out.println("Leakage current monitors are roughly in nA.");
	}
	//Utility methods for mapping strips to ADC channels
	/**
	 * Returns which adc unit given which detector.
	 */
	private int whichADCaddress(int detector){
		if (detector == 0) {//Detector 0
			return ADC_BASE[3];
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
			return 8;
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
		String calibrationFile = "calibration.obj";
		System.out.println("Attempting to retrieve calibration from: "+
		calibrationFile);
		ObjectInputStream ois = new ObjectInputStream(
		getClass().getResourceAsStream(calibrationFile));
		ac = (ArrayCalibration)ois.readObject();
		System.out.println("Calibration retrieved.  String representation = "+ac);
		ois.close();        
	}
    
	int [][] thresholds=new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	private void readADCtresholds() throws IOException {
		String thresholdFile = "/data1/mar02/thresholds.txt";
		StreamTokenizer grabber = new StreamTokenizer(new FileReader(thresholdFile));
		grabber.commentChar('#');
		for (int strip=0; strip < STRIPS_PER_DETECTOR; strip++){
			for (int det = 0; det < NUM_DETECTORS; det++){
				grabber.nextToken();
				thresholds[det][strip]=(int)grabber.nval;
			}
		}
	}
	
	/**
	 * Sets ADC thresholds to be a certain energy in keV.  
	 * We have to look up gains in the array calibration,
	 * so retrieveCalibration() is assumed to have been 
	 * called.
	 */
	private void setADCthresholdsToE() throws IOException{
		/* The following lines set up a text file reader
		 * for a file sitting in the same directory as this
		 * code. */
		String thresholdFile = "threshold_keV.txt";
		InputStreamReader isr = new InputStreamReader(
		getClass().getResourceAsStream(thresholdFile));
		/* */
		StreamTokenizer grabber = new StreamTokenizer(isr);
		grabber.commentChar('#');
		grabber.nextToken(); double E_in_keV=grabber.nval;
		double E_in_MeV = E_in_keV/1000.0;
		System.out.println("Setting thresholds to "+E_in_keV+" keV.");
		double minGain = ac.getChannelsPerMeV();
		for (int strip=0; strip < STRIPS_PER_DETECTOR; strip++){
			for (int det = 0; det < NUM_DETECTORS; det++){
				double gainFactor = ac.getEnergyGain(det,strip);
				if (gainFactor==0.0) {
					thresholds[det][strip] = 200;//arbitrary threshold that seems to work
				} else {
					double E_at_400 = ac.getEnergyDeposited(det,strip,400);
					double E_at_500 = ac.getEnergyDeposited(det,strip,500);
					double channel = 400+100/(E_at_500-E_at_400)*(E_in_MeV-E_at_400);
					thresholds[det][strip] = (int)Math.round(channel);
				}
			}
		}
	}
	
    
    
	/** Called every time Jam has an event it wants to sort.
	 * @param dataEvent contains all parameters for one event
	 * @throws Exception so Jam can handle exceptions
	 */
	public void sort(int [] dataEvent) throws Exception {
               
		//TAJ
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
		int theta_calc = (int) Math.round(RPOS -FPOS - THETA_OFFSET);
		//int ePosnNew = getCorrectedPosition(FPOS, theta_calc);
		//int ecPosnNew = ePosnNew >> TWO_D_FACTOR;

		   eCthd += (int) DELTA_CATHODE;
		   final int ecCthd = eCthd >> TWO_D_FACTOR;

     
        
		//TAJ
//		singles spectra
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
		final int FL_COMPR_HI = FRONT_LEFT >> TWO_D_HR_FACTOR;
		final int FR_COMPR_HI = FRONT_RIGHT >> TWO_D_HR_FACTOR;
		hFrontLvsR.inc(FL_COMPR_HI, FR_COMPR_HI);
		final int RL_COMPR_HI = REAR_LEFT >> TWO_D_HR_FACTOR;
		final int RR_COMPR_HI = REAR_RIGHT >> TWO_D_HR_FACTOR;
		hRearLvsR.inc(RL_COMPR_HI, RR_COMPR_HI);
		int eFnew = 0;
		if ((FRONT_RIGHT > 0) && (FRONT_LEFT > 0)) {
		  eFnew = FRONT_LEFT - FRONT_RIGHT + 4095;
		  hFrontNew.inc(eFnew);
		}
		final int FPOS_COMP_HI = FPOS >> TWO_D_HR_FACTOR;
		final int RPOS_COMP_HI = RPOS >> TWO_D_HR_FACTOR;
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
		hFrntCthd.inc(FPOS_COMPR, ecCthd);
		hRearCthd.inc(RPOS_COMPR, ecCthd);
		hFrntSntr.inc(FPOS_COMPR, SCINT_COMPR);
		hFrntSntr1.inc(FPOS_COMPR, SCINT1);
		hFrntSntr2.inc(FPOS_COMPR, SCINT2);
		hFrntPRearP.inc(FPOS_COMP_HI, RPOS_COMP_HI);

		/*hRWbias.inc(dataEvent[idRWbias]);
		hFWbias.inc(dataEvent[idFWbias]);*/
	//	hBCIRange.inc(dataEvent[idBCIRange]);
		
		//end TAJ stuff

		int [][] eEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int [][] eTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int [][] eEnergiesGA = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int [][] eTimesGA= new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

		boolean bSC    = gSntr2Cthd.inGate(SCINT2,ecCthd);
		boolean bFC    = gFrntCthd.inGate(FPOS_COMPR,ecCthd);
		boolean bFS    = gFrntSntr2.inGate(FPOS_COMPR,SCINT2);
		boolean bPID   = bSC && bFC && bFS;
		boolean bFR    = gFrntRear.inGate(FPOS_COMP_HI, RPOS_COMP_HI) /*&& !gFRexclude.inGate(FPOS_COMP_HI, RPOS_COMP_HI)*/;
		
//scint test on raw front position		
		boolean bST =gScintTest.inGate(FPOS);
					   if(bST){
						 hSntr1_Test.inc(SCINTR);
						 hSntr2_Test.inc(SCINTL);       

					   }

//taj
  final boolean XY_INGATE = gXY.inGate(FPOS_COMPR, FY_COMPR);
	  final boolean YY_INGATE = gFrYvsReY.inGate(FY_COMPR, RY_COMPR);
	  final boolean FR_RE_INGATE = gFrntRear.inGate(FPOS_COMP_HI, RPOS_COMP_HI);
	  final boolean IN_SUM_GATES = gFSum.inGate(FRONT_RIGHT + FRONT_LEFT) &&
		  gRSum.inGate(REAR_RIGHT + REAR_LEFT);
	  final boolean GOOD_DIREC = XY_INGATE && FR_RE_INGATE && YY_INGATE;
	  final boolean ACCEPT = GOOD_DIREC && IN_SUM_GATES;
	  final boolean bGood = GOOD_DIREC && bPID;
	  boolean bState = bGood && gPeak.inGate(FPOS);//from YLSA/OLD

//	  TAJ
        
			if (bSC) {
				  // gate on Scintillator vs Cathode
				  hFrntSntr2GSC.inc(FPOS_COMPR, SCINT2);
				  hFrntCthdGSC.inc(FPOS_COMPR, ecCthd);
				}
				if (bFC) {
				  // gate on Front Wire Position vs Cathode
				  hSntr2CthdGFC.inc(SCINT2, ecCthd);
				  hFrntSntr2GFC.inc(FPOS_COMPR, SCINT2);
				}
				if (bFS){
				  // gate on Front Wire Position vs Scintillator
				  hSntr2CthdGFS.inc(SCINT2, ecCthd);
				  hFrntCthdGFS.inc(FPOS_COMPR, ecCthd);
				}
				if (bPID) {
				  // gated on all 3 gate above
				  //writeEvent(dataEvent);
				  hFrntGCSF.inc(FPOS);
				  hRearGCSF.inc(RPOS);
				  hFrRightgCSF.inc(FPOS_COMP_HI, RPOS_COMP_HI);
				  hFrntYRearY.inc(FY_COMPR, RY_COMPR);
				  hYvsPsnGPID.inc(FPOS_COMPR, FY_COMPR);
				  if (bGood && IN_SUM_GATES) {
					writeEvent(dataEvent);
					hFrntGAll.inc(FPOS);
//					Scintillator tests
					boolean bSTG =gScintTestG.inGate(FPOS);
					if(bSTG){
					  hSntr1_Test_G.inc(SCINTR);
					  hSntr2_Test_G.inc(SCINTL);
						  }
					hRearGAll.inc(RPOS);
					hcFrntGAll.inc(FPOS >> COMPRESS_FACTOR);
					hcRearGAll.inc(RPOS >> COMPRESS_FACTOR);
					hFrontTheta.inc(FPOS >> 2, THETA_CHANNEL);
					hRearTheta.inc(RPOS >> 2, THETA_CHANNEL);
				   }
				}



		//System.out.println("Declared silicon data storage: event "+event);
		int multiplicity=0;
		for (int i=0; i<NUM_DETECTORS; i++) {
			hLeakageCurrent[i].inc(dataEvent[idLeakageCurrent[i]]);
//			  hBackPlane[i].inc(dataEvent[idBackPlane[i]]);
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
				double eDeposit = ac.getEnergyDeposited(i,j,eEnergies[i][j]);//deposited in det [MeV]
				eTimesGA[i][j] = ac.getCalibratedTimeChannel(i,j,eTimes[i][j]);
				int ecTimeGA = eTimesGA[i][j] >> TWO_D_FACTOR;
				int eChannel = (int)Math.round(eDeposit*20);
				boolean bTimeBroad = bGood && gTimeBroad.inGate(ecTimeGA);
				boolean bTimeState = bState && gTimeState.inGate(ecTimeGA);
				boolean bEvsS= gEvsS.inGate(eChannel,j);
				boolean bTimeDecay = bState && bEvsS && gTimeDecay.inGate(ecTimeGA);
				if (energy) {
					hEnergyHits.inc(stripBin);
					eEnergiesGA[i][j] = ac.getCalibratedEnergyChannel(i,j,
					eEnergies[i][j]);
					hEvsStripGA.inc((int)Math.round(eDeposit*50),stripBin);
				}
				if (time) {
					hTimeHits.inc(stripBin);
					hTvsStripGA_noE.inc(ecTimeGA,stripBin);
					if (gTvsStripGA_noE.inGate(ecTimeGA,stripBin)) {
						hFrntGTime_noE.inc(FPOS);
					}			
				}
				if (time && energy) {
					hHits.inc(stripBin);
					detHit[multiplicity]=i;
					stripHit[multiplicity]=j;
					bin[multiplicity]=stripBin;
					multiplicity++;
					hTvsStrip.inc(eTimes[i][j] >> TWO_D_FACTOR,stripBin);
					if (bPID && bGood && IN_SUM_GATES) {
						hTimeGA.inc(ecTimeGA);
						hTvsStripGA_gAll.inc(ecTimeGA,stripBin);
						int _Senergy = (int)Math.round(ac.getEnergyDeposited(i,j,eEnergies[i][j])*10);                    	
						hEsilVsFP.inc(FPOS,_Senergy);  //CHANGED!!
						hEvsStripGA_gAll.inc((int)Math.round(eDeposit*50),stripBin);
					}
					if (bTimeBroad) {
						hEvsChBroad.inc(eChannel,stripBin);
						hEvsStripBroad.inc(eChannel,j);      
						hEvsStripGA_Time.inc((int)Math.round(eDeposit*50),stripBin);
						int _Senergy = (int)Math.round(ac.getEnergyDeposited(i,j,eEnergies[i][j])*10);  
						hEsilVsFP_gTime.inc(FPOS,_Senergy);    
						//new
						if (bState && bPID && bGood && IN_SUM_GATES){
							hEsilVsFP_peak_time.inc(FPOS_COMPR,_Senergy);
							}       	
						if (multiplicity==1){
							hEsilVsFP_m1.inc(FPOS_COMPR,_Senergy);
							boolean bEState_m1 = gEsilVsFP_m1.inGate(FPOS_COMPR,_Senergy);
							if(bEState_m1){
								hFPGAll_St_m1.inc(FPOS);
							}  //CHANGED!!!
						} else if (multiplicity==2){
							hEsilVsFP_m2.inc(FPOS_COMPR,_Senergy); 
							boolean bEState_m2 = gEsilVsFP_m2.inGate(FPOS_COMPR,_Senergy);
							if(bEState_m2){
								hFPGAll_St_m2.inc(FPOS);
							} //CHANGED!!
						} else if (multiplicity==3) {
							hEsilVsFP_m3.inc(FPOS_COMPR,_Senergy);  //CHANGED!!!!
						}
						hFrntGTime.inc(FPOS);
						hcFrntGTime.inc(FPOS>>COMPRESS_FACTOR);
					}
					if (bState) {
						hTimeGAstate.inc(ecTimeGA);
						int _Senergy = (int)Math.round(ac.getEnergyDeposited(i,j,eEnergies[i][j])*10);  
						hEsilVsFP_peak.inc(FPOS_COMPR,_Senergy);
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
				if (((int [])hHits.getCounts())[stripBin]==0){//avoid div by zero and set to zero
					((double [])hHitsRatio.getCounts())[stripBin]=0.0;
				} else {//actually divide
					((double [])hHitsRatio.getCounts())[stripBin]=(double)(((int [])hTimeHits.getCounts())[stripBin])/
					(double)(((int [])hHits.getCounts())[stripBin]);
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
        
			

	}
	/** Called so the dead time can be calculated.
	 * @param name name of monitor to calculate
	 * @return floating point value of monitor
	 */
	public double monitor(String name){
		double rval=0.0;
		if (name.equals(DEAD_TIME)){
			double acceptRate = mEvntAccept.getValue();
			double rawRate = mEvntRaw.getValue();
			if (acceptRate > 0.0  && acceptRate <= rawRate){
				rval = 100.0 * (1.0 - acceptRate/rawRate);
			} 
		} else if (name.startsWith("Leakage ")){//leakage currents
			int which = Integer.parseInt(name.substring(8));
			rval = (gLeakageCurrent[which].getCentroid()-80)*2;
			hLeakageCurrent[which].setZero();//zero for next time around            
		} 
		return rval;
	}
    
	/**
	 * Returns channel = Ecm(keV)/10
	 */
	/*private int getEcmChannel(int eChannelRaw, int det, int strip, int FPchannel) 
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
	}*/
    
}
