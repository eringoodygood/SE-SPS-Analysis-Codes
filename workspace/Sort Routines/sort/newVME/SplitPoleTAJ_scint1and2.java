
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
public class SplitPoleTAJ_scint1and2 extends SortRoutine {
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
  private transient Histogram hSntr1Cthd, hSntr2Cthd, hFrntCthd, hRearCthd,
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
  private transient Histogram hFrntSntr1GSC1, hFrntSntr2GSC2, hFrntCthdGSC1, hFrntCthdGSC2;
  /*
   *  gate by Front wire Cathode
   */
  private transient Histogram hSntr1CthdGFC, hFrntSntr1GFC;
  private transient Histogram hSntr2CthdGFC, hFrntSntr2GFC;
  private transient Histogram hSntr1CthdGFS1, hSntr2CthdGFS2, hFrntCthdGFS1, hFrntCthdGFS2;
  /*
   *  gate by Front wire Scintillator
   */
  private transient Histogram hFrntGCSF1, hFrntGCSF2, hRearGCSF, hFrRightgCSF, hFrntGAll,
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
  private transient Gate gSntr1Cthd, gFrntSntr1, gSntr2Cthd, gFrntSntr2, gFrntCthd, gFrntRear,
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
    

	Histogram hAngDist; //for getting angular distribution of alphas


	//coincidence histograms & gates
	//id numbers for the signals;
   
	int idFrontBias, idRearBias;
	Histogram hFrontBias, hRearBias;
	//Monitor [] mLeakageCurrent = new Monitor[NUM_DETECTORS];
	
   // Scaler [] sYLSArate = new Scaler[NUM_DETECTORS];
   // Monitor [] mYLSArate = new Monitor[NUM_DETECTORS];
    
		Histogram hFrntNewGAll, hcFrntNewGAll;
	/**
	 * Containers of information about any strips with both an energy and time
	 * signal for this event.
	 */
	
	//ResidualKinematics rk;//for calculating Residual trajectory
	//double Mproton;
	//double mAlpha;

	/** Sets up objects, called when Jam loads the sort routine.
	 * @throws Exception necessary for Jam to handle exceptions
	 */
	public void initialize() throws Exception {
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
     		hSntr1Cthd = new Histogram("Scint1Cathode  ", HIST_2D_INT, TWO_D_CHANNELS,
						"Cathode vs Scintillator PMT1", "Scintillator PMT 1", CATH);
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
		hSntr1CthdGFC = new Histogram("Scint1CathodeGFC", HIST_2D_INT, TWO_D_CHANNELS,
			"Cathode vs Scintillator1 - FwCa gate", "Scintillator PMT 1", CATH);
			hSntr2CthdGFC = new Histogram("Scint2CathodeGFC", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Scintillator2 - FwCa gate", "Scintillator PMT 2", CATH);
		hSntr1CthdGFS1 = new Histogram("Scint1CathodeGFS1", HIST_2D_INT, TWO_D_CHANNELS,
			"Cathode vs Scintillator 1 - FwSc gate", "Scintillator PMT 1", CATH);
			hSntr2CthdGFS2 = new Histogram("Scint2CathodeGFS2", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Scintillator 2 - FwSc gate", "Scintillator PMT 2", CATH);
			//FrontCathode Gated on other
			hFrntCthdGSC1 = new Histogram("FrontCathodeGSC1", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Front Position - Sc1Ca gate", FRONT_POS, CATH);
		    hFrntCthdGSC2 = new Histogram("FrontCathodeGSC2", HIST_2D_INT, TWO_D_CHANNELS,
			"Cathode vs Front Position - Sc2Ca gate", FRONT_POS, CATH);
			hFrntCthdGFS1 = new Histogram("FrontCathodeGFS1 ", HIST_2D_INT, TWO_D_CHANNELS,
				"Cathode vs Front Position - FwSc1 gate ", FRONT_POS, CATH);
			hFrntCthdGFS2 = new Histogram("FrontCathodeGFS2 ", HIST_2D_INT, TWO_D_CHANNELS,
			"Cathode vs Front Position - FwSc2 gate ", FRONT_POS, CATH);
			//FrontScint Gated on other
		    hFrntSntr1GSC1 = new Histogram("FrontScint1GSC1 ", HIST_2D_INT, TWO_D_CHANNELS,
			"Scintillator 1 vs Front Position - ScCa gate", FRONT_POS,
			"Scintillator PMT 1");
			hFrntSntr2GSC2 = new Histogram("FrontScint2GSC2 ", HIST_2D_INT, TWO_D_CHANNELS,
				"Scintillator 2 vs Front Position - ScCa gate", FRONT_POS,
				"Scintillator PMT 2");
			hFrntSntr1GFC = new Histogram("FrontScint1GFC", HIST_2D_INT, TWO_D_CHANNELS,
			"Scintillator 1 vs Front Position - FwCa gate", FRONT_POS,
			"Scintillator PMT 1");
			hFrntSntr2GFC = new Histogram("FrontScint2GFC", HIST_2D_INT, TWO_D_CHANNELS,
				"Scintillator 2 vs Front Position - FwCa gate", FRONT_POS,
				"Scintillator PMT 2");
			/*
			 *  gated on 3 gates
			 */
			hFrntGCSF1 = new Histogram("FrontGCSF1    ", HIST_1D_INT, ADC_CHANNELS,
				"Front Position - Sc1Ca,FwCa,FwSc1 gates");
			hFrntGCSF2 = new Histogram("FrontGCSF2    ", HIST_1D_INT, ADC_CHANNELS,
				"Front Position - Sc2Ca,FwCa,FwSc2 gates");
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
			
        hFrntGTime  = new Histogram("FrontGTime    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw & time gates");
		hFrntGAllGTime  = new Histogram("FrontGAllGTime    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw & time gates");
		hcFrntGTime  =new Histogram("FrontGTimecmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw & time gates");
      
		//200 mV = 0.40 uA, so 5 mV = 0.01 uA, or 1 mV(ch) = 2 nA
		//--->500 channels/uA
		//YLSA
		
		System.err.println("# Parameters: "+getEventSize());
		System.err.println("ADC channels: "+ADC_CHANNELS);
		System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
		System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);

		
		// gates 1d-OLD
		gCthd   =new Gate("Counts", hCthd);
		gPeak   =new Gate("Peak", hFrntGAll);
		//hFrntGTime.addGate(gPeak);
        
		//FROM TAJ
			gFSum = new Gate("FrontSum", hFrontSum);
			gRSum = new Gate("RearSum", hRearSum);
			
		//gates  2d-OLD
		gSntr1Cthd   =new Gate("Ca-Sc1", hSntr1Cthd);      //gate on Scintillator Cathode
		gFrntSntr1   =new Gate("Fw-Sc1", hFrntSntr1);          //gate on Front Scintillator
		gSntr2Cthd   =new Gate("Ca-Sc2", hSntr2Cthd);      //gate on Scintillator Cathode
		gFrntSntr2   =new Gate("Fw-Sc2", hFrntSntr2);          //gate on Front Scintillator
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

		hFrntSntr1GSC1.addGate(gFrntSntr1);
		hFrntSntr2GSC2.addGate(gFrntSntr2);
		hFrntCthdGSC1.addGate(gFrntCthd);
		hFrntCthdGSC2.addGate(gFrntCthd);
		hSntr1CthdGFC.addGate(gSntr1Cthd);
		hSntr2CthdGFC.addGate(gSntr2Cthd);
		hFrntSntr1GFC.addGate(gFrntSntr1);
		hSntr1CthdGFS1.addGate(gSntr1Cthd);
		hFrntSntr2GFC.addGate(gFrntSntr2);
		hSntr2CthdGFS2.addGate(gSntr2Cthd);
		hFrntCthdGFS1.addGate(gFrntCthd);
		hFrntCthdGFS2.addGate(gFrntCthd);
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
		hSntr1Cthd.inc(SCINT1, ecCthd);
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

		boolean bSC1    = gSntr1Cthd.inGate(SCINT1,ecCthd);
		boolean bFS1    = gFrntSntr1.inGate(FPOS_COMPR,SCINT1);
		boolean bSC2    = gSntr2Cthd.inGate(SCINT2,ecCthd);
		boolean bFC    = gFrntCthd.inGate(FPOS_COMPR,ecCthd);
		boolean bFS2    = gFrntSntr2.inGate(FPOS_COMPR,SCINT2);
		boolean bPID1   = bSC1 && bFC && bFS1;
		boolean bPID2   = bSC2 && bFC && bFS2;
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
	  final boolean bGood1 = GOOD_DIREC && bPID1;
	  final boolean bGood2 = GOOD_DIREC && bPID2;
	  boolean bState1 = bGood1 && gPeak.inGate(FPOS);//from YLSA/OLD
	  boolean bState2 = bGood2 && gPeak.inGate(FPOS);//from YLSA/OLD

//	  TAJ
        
	       if (bSC1) {
						// gate on Scintillator vs Cathode
						hFrntSntr1GSC1.inc(FPOS_COMPR, SCINT1);
						hFrntCthdGSC1.inc(FPOS_COMPR, ecCthd);
					  }
			if (bSC2) {
				  // gate on Scintillator vs Cathode
				  hFrntSntr2GSC2.inc(FPOS_COMPR, SCINT2);
				  hFrntCthdGSC2.inc(FPOS_COMPR, ecCthd);
				}
				if (bFC) {
				  // gate on Front Wire Position vs Cathode
				  hSntr1CthdGFC.inc(SCINT1, ecCthd);
				  hFrntSntr1GFC.inc(FPOS_COMPR, SCINT1);
				  hSntr2CthdGFC.inc(SCINT2, ecCthd);
				  hFrntSntr2GFC.inc(FPOS_COMPR, SCINT2);
				}
				if (bFS1){
				   // gate on Front Wire Position vs Scintillator
					hSntr1CthdGFS1.inc(SCINT1, ecCthd);
					hFrntCthdGFS1.inc(FPOS_COMPR, ecCthd);
						}
				if (bFS2){
				  // gate on Front Wire Position vs Scintillator
				  hSntr2CthdGFS2.inc(SCINT2, ecCthd);
				  hFrntCthdGFS2.inc(FPOS_COMPR, ecCthd);
				}
				if (bPID1) {
					// gated on all 3 gate above
					//writeEvent(dataEvent);
					hFrntGCSF1.inc(FPOS);
					hRearGCSF.inc(RPOS);
					hFrRightgCSF.inc(FPOS_COMP_HI, RPOS_COMP_HI);
					hFrntYRearY.inc(FY_COMPR, RY_COMPR);
					hYvsPsnGPID.inc(FPOS_COMPR, FY_COMPR);
					if (bGood1 && IN_SUM_GATES) {
						writeEvent(dataEvent);
						hFrntGAll.inc(FPOS);
//						Scintillator tests
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
				if (bPID2) {
				  // gated on all 3 gate above
				  //writeEvent(dataEvent);
				  hFrntGCSF2.inc(FPOS);
				  hRearGCSF.inc(RPOS);
				  hFrRightgCSF.inc(FPOS_COMP_HI, RPOS_COMP_HI);
				  hFrntYRearY.inc(FY_COMPR, RY_COMPR);
				  hYvsPsnGPID.inc(FPOS_COMPR, FY_COMPR);
				  if (bGood2 && IN_SUM_GATES) {
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
