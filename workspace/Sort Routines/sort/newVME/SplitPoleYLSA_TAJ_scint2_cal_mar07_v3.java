/*
 * Created on Nov 2, 2006
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
// import jam.sort.*; (commented 16/08/06)

 
// import java.io.FileReader; //(commented 16/08/06)
import java.io.IOException; 
// import java.io.InputStreamReader; (commented 16/08/06)
// import java.io.ObjectInputStream; (commented 16/08/06)
// import java.io.StreamTokenizer; (commented 16/08/06)
// import dwvisser.analysis.ArrayCalibration; (commented 16/08/06)

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
 * 
 * Modified Jan 07 by Catherine Deibel for YSLA calibrations done jan07 for energy and time
 */
    public class SplitPoleYLSA_TAJ_scint2_cal_mar07_v3 extends SortRoutine {
//VME properties
    static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000,0x20030000};
    static final int [] TDC_BASE = {0x30000000,0x30010000,0x30020000};
	static final int SCALER_ADDRESS = 0xf0e00000;
//static final int THRESHOLDS = 200;            //ADC lower threshold in channels
	private final static int THRESHOLDS = 128;  //FROM TAJ
	static final int TIME_THRESHOLDS    = 30;   //TDC lower threshold in channels
	static final int TIME_RANGE         = 1200; //ns
	static final int LAST_ADC_BIN       = 3840;

//LEDA Detectors
	static final int NUM_DETECTORS = 5;
	static final int STRIPS_PER_DETECTOR = 16;

//names
	static final String DEAD_TIME =      "Dead Time %";
	static final String TRUE_DEAD_TIME = "True Dead Time %";

//histogramming constants
	final int ADC_CHANNELS        = 4096; //num of channels per ADC
	final int COMPRESSED_CHANNELS = 512;  //number of channels in compressed histogram
	final int TWO_D_CHANNELS      = 512;  //number of channels per dimension in 2-d histograms
	final int TWO_D_HIRES         = 512;
//bits to shift for compression
	final int COMPRESS_FACTOR = Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
	final int TWO_D_FACTOR    = Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));
	final int TWO_D_HR_FACTOR = Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_HIRES)/Math.log(2.0)));

//histograms and parameter ID's for YLSA channels
	Histogram [][] hEnergies = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	Histogram [][] hTimes    = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	int [][] idEnergies      = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	int [][] idTimes         = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	double [][] offset       = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] gain         = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	double [][] t_offset     = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    double [][] t_gain       = new double[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    
	static final double [] Gains={
//det 0 
	.999,1.008,1.026,1.002,1.004,0.945,1.008,1.005,
    1.042,1.007,0.994,1.040,1.064,1.014,1.039,0.993, 
//det 1
	0.939,1.055,1.019,0.932,1.024,1.014,1.023,0.979,
    1.058,1.020,0.998,1.009,1.084,1.059,1.064,0.978,
//det 2
	1.054,2.278,1.273,1.255,1.269,1.201,1.245,1.264,
	1.289,1.268,1.262,1.259,1.308,1.254,1.284,1.206,
//det 3
	1.263,1.232,1.246,1.318,1.306,1.271,1.221,1.246,
    1.278,1.295,1.255,1.353,1.288,1.327,1.225,1.275,
//det4
    0.980, 1.014, 1.139, 1.009, 1.030, 1.042, 1.035, 1.003, 
    1.040, 1.011, 1.059, 1.063, 1.081, 1.055, 1.061, 0.999};

	static final double [] Offsets={
//det0			
	89,115,119,113,145,294,105,87,
	112,96,105,121,113,106,84,116,
//det1	
	80,91,132,123,128,127,138,122,
    116,121,116,109,133,99,114,103,
//det2
	111,116,118,126,104,102,97,116,
    125,96,107,121,121,59,138,130,
//det3	
	132,141,131,151,131,134,142,117,
    128,106,133,127,148,121,150,111,
//det4	
    44,61,125,131,119,96,124,105,  
    128,125,152,115,130,126,82,104};
    
	static final double [] T_Gains={
//	  det 0 
		.997, 1.008, .995, .991, 1.007, 1.027, .998, .982, 
		1.010, 1.007, 1.010, .975, .975, 1.011, 1.010, .992, 
//	  det 1
		.989, .982, .993, .988, 1.029, .980, .998, .991, 
		.988, 1.022, .998, .986, 0.995, 1.039, 1.014, .989,
//	  det 2
		.953, .998, .963, .975, 1.010, 1.002, .982,.973, 
		1.023, .973, .981, 1.003, 1.001, 1.005, 1.003, .990,
//	  det 3
		1.001, 1.003, 1.006, 1.004, .994, .995, 997, .983, 
		1.015, 1.004, 1.020, 1.016, .996, 1.002, .996, .988,
//	  det4
		1.006, .995, 1.023, 1.001, 1.001, .988, 1.006, 1.002, 
		1.011, 1.018, 1.032, .989, 1.014, 1.006, 1.012, 1.003};

		static final double [] T_Offsets={
//	  det0			
		422, 389, 381, 348, 408, 444, 408, 387,  
		352, 346, 357, 281, 247, 331, 349, 358,
//	  det1	
		392, 404, 404, 424, 454, 392, 440, 411, 
		429, 482, 443, 403, 407, 509, 444, 378,
//	  det2
		361, 369, 351, 379, 422, 425, 408, 374,  
		515, 358, 416, 429, 454, 418, 450, 410,
//	  det3	
		446, 450, 463, 452, 439, 422, 476, 419, 
		484, 510, 524, 488, 437, 475, 463, 422,
//	  det4	
		0, 412, 496, 463, 407, 431, 463, 455,  
		478, 471, 546, 476, 481, 462, 494, 477};
/*	from nov06	
//		det0			
		  87, 59, 75, 56, 66, 72, 65, 64,  
		  15, 13, 39, 62,  0, 25, 37, 65,
//		det1	
		  54, 76, 83, 126, 71, 71, 86, 67,  
		  57, 113, 70, 90, 58, 170, 104, 75,
//		det2
		  138, 34, 64, 96, 105, 94, 90, 81,  
		  170, 76, 138, 126, 147, 111, 163, 139,
//		det3	
		  153, 140, 161, 168, 149, 143, 186, 130,  
		  174, 205, 212, 191, 155, 196, 190, 170,
//		det4	
		  0, 0, 0, 0, 0, 0, 0, 0,  
		  0, 0, 0, 0, 0, 0, 0, 0};*/

// ungated spectra from TAJ
   
  private transient Histogram hCthd, hSntr1, hSntr2, hSntrSum, hSntr1Sntr2, hFrntPsn, hRearPsn, hMonitor;
  private transient Histogram hFrntPH;
  private transient Histogram hRearPH;
  private transient Histogram hSntr2Cthd, hFrntCthd, hRearCthd, hFrntSntr, hFrntSntr1, hFrntSntr2, hFrntPRearP;
  private transient Histogram hFrontTheta, hRearTheta, hFC /*,hFrNewVsOld*/;
  /*
   *  gate by Scintillator 2 cathode
   */
  private transient Histogram hFrntSntr2GSC, hFrntCthdGSC;
  /*
   *  gate by Front wire Cathode
   */
  private transient Histogram hSntr2CthdGFC, hFrntSntr2GFC;
  /*
   *  gate by Front wire Scintillator 2
   */
  private transient Histogram hSntr2CthdGFS, hFrntCthdGFS;
// gate by Scint2Cath and FrontScint2
  private transient Histogram hFrntCthdGFS_SC;
  
  private transient Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll, hFrntGTime;
//  private transient Histogram hcFrntGTime;   //front and rear wire gated on All compressed & time
//  private transient Histogram hCathTheta;
  /*
   *  1D gates
   */
  private transient Gate gCthd, gPeak;
  /*
   *  2D gates
   */
  private transient Gate gSntr2Cthd, gFrntSntr2, gFrntCthd, gFrntRear, gFrontLvsR, gRearLvsR, gFP;
  /*
   *  Scalers and monitors
   */
  private transient Scaler sClck, sEvntRaw, sEvntAccpt, sScint, sCathode, sFCLR, sMon, sGate, sBusy;
  /*
   *  number of FCLR's that went to ADC's
   */
  private transient Monitor mClck, mEvntRaw, mEvntAccept, mScint, mCathode, mFCLR, mMon, mGate, mBusy;
  /*
   *  id numbers for the signals;
   */
  private transient int idCthd, idScintR, idScintL, idFrntPsn, idRearPsn, idMonitor;
  
  private transient DataParameter pXTheta, pThOffset, pCTheta;



	// OLD DET and YLSA
	//gates 1 d
	
    Histogram hHits, hEvsStrip, hTvsStrip, hTimeHits, hTvsEhits, hInterHits, hEnergyHits;
	Histogram hHitsRatio;
	Histogram hMultiplicity;
	Histogram hEvsStripGA, hTvsStripGA, hEvsStripGA_gAll, hTvsStripGA_gAll; //gain adjusted spectra
	Histogram hAngDist, hAngDist_bck, hAngDist_16, hAngDist_16_bck; //for getting angular distribution of alphas
	Histogram hAngDist_gs, hAngDist_bck_gs, hAngDist_16_gs, hAngDist_16_bck_gs;
	Histogram hAngDist_ms, hAngDist_bck_ms, hAngDist_16_ms, hAngDist_16_bck_ms;
	Histogram hAngDist_ex, hAngDist_bck_ex, hAngDist_16_ex, hAngDist_16_bck_ex;
	
	//coincidence histograms & gates
	Histogram hTimeGA;//all strips time gain adjusted (TOF adjustment for alpha)
	Histogram hEvsStripGA_Time;//EsilvsStrip gated on time peak
	Histogram hEvsStrip_16;
//	Histogram hEvsChBroad;
	Histogram hEsilVsFP, hEsilVsFP_gTime;
	Histogram hFPGAll_St;
	Histogram hFPGAll_St_m1, hFPGAll_m1_0_3, hFPGAll_m1_4_7, hFPGAll_m1_8_11, hFPGAll_m1_12_15;
	Histogram hFPGAll_m1_0_3_bck, hFPGAll_m1_4_7_bck, hFPGAll_m1_8_11_bck, hFPGAll_m1_12_15_bck;
	
	Histogram hFPGAll_gs_m1, hFPGAll_gs_m1_0_3, hFPGAll_gs_m1_4_7, hFPGAll_gs_m1_8_11, hFPGAll_gs_m1_12_15;
	Histogram hFPGAll_gs_m1_bck, hFPGAll_gs_m1_0_3_bck, hFPGAll_gs_m1_4_7_bck, hFPGAll_gs_m1_8_11_bck, hFPGAll_gs_m1_12_15_bck;
	Histogram hFPGAll_ms_m1, hFPGAll_ms_m1_0_3, hFPGAll_ms_m1_4_7, hFPGAll_ms_m1_8_11, hFPGAll_ms_m1_12_15; 
	Histogram hFPGAll_ms_m1_bck, hFPGAll_ms_m1_0_3_bck, hFPGAll_ms_m1_4_7_bck, hFPGAll_ms_m1_8_11_bck, hFPGAll_ms_m1_12_15_bck; 
	Histogram hFPGAll_ex_m1, hFPGAll_ex_m1_0_3, hFPGAll_ex_m1_4_7, hFPGAll_ex_m1_8_11, hFPGAll_ex_m1_12_15;
	Histogram hFPGAll_ex_m1_bck, hFPGAll_ex_m1_0_3_bck, hFPGAll_ex_m1_4_7_bck, hFPGAll_ex_m1_8_11_bck, hFPGAll_ex_m1_12_15_bck;
	Histogram hFPGAll_St_m1_bck, hFPGAll_St_m1_bck_test;
	Histogram hFPGAll_St_m2;
//	Histogram hEsilVsFP_peak;
//	Histogram hRYvsFY;
//  Histogram hFrntGTime_noE, hTvsStripGA_noE;
	Histogram hEsilVsFP_m1, hEsilVsFP_m1_bck, hEsilVsFP_m2, hEsilVsFP_m3;
	//added 4/26/07
	Histogram hScint2Cath_inYLSA, hFrntCath_inYLSA, hScint2Frnt_inYLSA;
	
	
	Gate gFRexclude;
	Gate gTime;
	Gate gTime_bckgr;
	Gate gEvsS;
	Gate gEsilVsFP;
	Gate gYLSA_group;
	Gate gEsilVsFP_m1;
	Gate gEsilVsFP_gs, gEsilVsFP_ms, gEsilVsFP_ex;
	Gate gEsilVsFP_m2;
//	Gate gTvsStripGA_noE;
	/**
	 * Containers of information about any strips with both an energy and time
	 * signal for this event.
	 */
	int TOTAL_STRIPS = STRIPS_PER_DETECTOR*NUM_DETECTORS;
	int [] detHit   = new int[TOTAL_STRIPS];
	int [] stripHit = new int[TOTAL_STRIPS];
	int [] bin      = new int[TOTAL_STRIPS];

	int numInterHits;//number of real interStrip hits (in TDC window)
	int [] interDetHit   = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
	int [] interStripHit = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];
	int [] interBin      = new int[STRIPS_PER_DETECTOR*NUM_DETECTORS];

	//ResidualKinematics rk;//for calculating Residual trajectory
	//double Mproton;
	//double mAlpha;

	// ArrayCalibration ac; (commented 16/08/06)
	//Histogram hEcmVsStripBroad,hEcmVsFP;
	/** Sets up objects, called when Jam loads the sort routine.
	 * @throws Exception necessary for Jam to handle exceptions
	 */
	 public void initialize() throws Exception {
		//retrieveCalibration();(commented 16/08/06)
		//readADCthresholds();
		//setADCthresholdsToE();
		//setupCorrections(); CHANGED!!!!!!!!
		for (int i=0; i<NUM_DETECTORS; i++) {
					for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
							int stripBin = i*STRIPS_PER_DETECTOR + j;
							offset[i][j]= Offsets[stripBin];
							gain[i][j]= Gains[stripBin];
						    t_offset[i][j]= T_Offsets[stripBin];
							t_gain[i][j]= T_Gains[stripBin];
							System.out.println("i,j,offset,gain: "+i+","+j+","+offset[i][j]+","+gain[i][j]);
						    System.out.println("i,j,t_offset,t_gain: "+i+","+j+","+t_offset[i][j]+","+t_gain[i][j]);
					}
				}
		setADCthresholdsToE();		
		vmeMap.setScalerInterval(3);
		for (int i=0; i < TDC_BASE.length; i++){
			vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
		} 
		//TAJ
		  
			idScintR  = vmeMap.eventParameter(2, ADC_BASE[0], 2,  THRESHOLDS);
			idScintL  = vmeMap.eventParameter(2, ADC_BASE[0], 3,  THRESHOLDS);
			idFrntPsn = vmeMap.eventParameter(2, ADC_BASE[0], 4,  THRESHOLDS);
			idRearPsn = vmeMap.eventParameter(2, ADC_BASE[0], 5,  THRESHOLDS);
		    idCthd    = vmeMap.eventParameter(2, ADC_BASE[0], 10, THRESHOLDS);
			idMonitor = vmeMap.eventParameter(2, ADC_BASE[0], 1,  THRESHOLDS);
		

			hCthd         = new Histogram("Cathode     ",     HIST_1D_INT, ADC_CHANNELS,        "Cathode Raw ");
			hSntr1        = new Histogram("Scint1      ",     HIST_1D_INT, ADC_CHANNELS,        "Scintillator PMT 1");
			hSntr2        = new Histogram("Scint2      ",     HIST_1D_INT, ADC_CHANNELS,        "Scintillator PMT 2");
			hSntrSum      = new Histogram("ScintSum    ",     HIST_1D_INT, ADC_CHANNELS,        "Scintillator Sum");
			hSntr1Sntr2   = new Histogram("Scint1-Scint2",    HIST_2D_INT, TWO_D_CHANNELS,      "Scintillator PMT 2 versus Scintillator PMT 1",      "Scint 1", "Scint2");
			hFrntPsn      = new Histogram("FrontPosn    ",    HIST_1D_INT, ADC_CHANNELS,        "Front Wire Position");
			hRearPsn      = new Histogram("RearPosn     ",    HIST_1D_INT, ADC_CHANNELS,        "Rear Wire Position");
			                final String FRONT_POS = "Front Position";
			hFrntPH       = new Histogram("FrontPvsHeight",   HIST_2D_INT, TWO_D_CHANNELS,	    "Pulse Height of FrontFront wire vs Front Position", FRONT_POS, "Pulse Height");
			hRearPH       = new Histogram("RearPvsHeight ",   HIST_2D_INT, TWO_D_CHANNELS,	    "Pulse Height of RearMiddle wire vs Rear Position",  "Rear Position", "Pulse Height");
			                final String POS = "Position";
			                final String SCINT = "Scintillator";
			                final String CATH = "Cathode";
			hSntr2Cthd    = new Histogram("Scint2Cathode  ",  HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Scintillator PMT2",                      "Scintillator PMT 2", CATH);
			hFrntCthd     = new Histogram("FrontCathode  ",   HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Front Position",                         FRONT_POS, CATH);
			hRearCthd     = new Histogram("RearCathode  ",    HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Rear Position",                          "Rear Position", CATH);
			hFrntSntr     = new Histogram("FrontScint ",      HIST_2D_INT, TWO_D_CHANNELS,      "Scintillator vs Front Position",                    FRONT_POS, SCINT);
			hFrntSntr1    = new Histogram("FrontScint1 ",     HIST_2D_INT, TWO_D_CHANNELS,      "Scintillator PMT 1 vs Front Position",              FRONT_POS, "Scintillator 1");
			hFrntSntr2    = new Histogram("FrontScint2 ",     HIST_2D_INT, TWO_D_CHANNELS,      "Scintillator PMT 2 vs Front Position",              FRONT_POS, "Scintillator 2");
			hFrntPRearP   = new Histogram("FrontRear  ",      HIST_2D_INT, TWO_D_HIRES,         "Rear Position vs Front Position",                   FRONT_POS, "Rear Position");
			hMonitor      = new Histogram("Monitor",          HIST_1D_INT, ADC_CHANNELS,        "Monitor");	
			//ScintCathode Gated on other 
			hSntr2CthdGFC = new Histogram("Scint2CathodeGFC", HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Scintillator2 - FwCa,FR gate",           "Scintillator PMT 2", CATH);
			hSntr2CthdGFS = new Histogram("Scint2CathodeGFS", HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Scintillator 2 - FwSc,FR gate",          "Scintillator PMT 2", CATH);
			//FrontCathode Gated on other
			hFrntCthdGSC  = new Histogram("FrontCathodeGSC",  HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Front Position - ScCa,FR gate",          FRONT_POS, CATH);
			hFrntCthdGFS  = new Histogram("FrontCathodeGFS ", HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Front Position - FwSc,FR gate ",         FRONT_POS, CATH);
		    hFrntCthdGFS_SC = new Histogram("FrontCathodeGFS_SC ", HIST_2D_INT, TWO_D_CHANNELS,    "Cathode vs Front Position - FwSc,ScCa,FR gate ", FRONT_POS, CATH);
			//FrontScint Gated on other 
			hFrntSntr2GSC = new Histogram("FrontScint2GSC ",  HIST_2D_INT, TWO_D_CHANNELS,      "Scintillator 2 vs Front Position - ScCa,FR gate",   FRONT_POS,"Scintillator PMT 2");
			hFrntSntr2GFC = new Histogram("FrontScint2GFC",   HIST_2D_INT, TWO_D_CHANNELS,      "Scintillator 2 vs Front Position - FwCa,FR gate",   FRONT_POS,"Scintillator PMT 2");
			//gated on 3 gates
			hFrntGCSF     = new Histogram("FrontGCSF    ",    HIST_1D_INT, ADC_CHANNELS,        "Front Position - ScCa,FwCa,FwSc gates");
			hRearGCSF     = new Histogram("RearGCSF    ",     HIST_1D_INT, ADC_CHANNELS,        "Rear Position - ScCa,FwCa,FwSc gates");
		    hFrntRearGCSF = new Histogram("FrontRearGCSF  ",  HIST_2D_INT, TWO_D_HIRES,         "Front vs. Rear - ScCa, FwCa, FwSc gates");
			//gated on 4 gates
			hFrntGAll     = new Histogram("FrontGAll    ",    HIST_1D_INT, ADC_CHANNELS,        "Front Position - ScCa,FwCa,FwSc,FwRw gates");
			hRearGAll     = new Histogram("RearGAll    ",     HIST_1D_INT, ADC_CHANNELS,        "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
			hFrontTheta   = new Histogram("FrontTheta",       HIST_2D_INT, 1024, TWO_D_HIRES,   "Theta vs. Front Wire Position (X)",                 POS, "Theta");
			hRearTheta    = new Histogram("RearTheta",        HIST_2D_INT, 1024, TWO_D_HIRES,   "Theta vs. RearWire Position (X)",                   POS, "Theta");
			hFC           = new Histogram("FrontCorrected  ", HIST_1D_INT, ADC_CHANNELS,        "Front Position - refocused");
			//hCathTheta    = new Histogram("CathodeTheta",     HIST_2D_INT, 1024, TWO_D_HIRES,   "Theta vs. Front Wire Position (X)",                 POS, "Theta");
	    	//hFrntGTime_noE= new Histogram("FrontGTime_noE",   HIST_1D_INT, ADC_CHANNELS,        "Front Position - ScCa,FwCa,FwSc,FwRw & time gates, no \"energy condition\"");//FROM OLD DET
	    	hFrntGTime    = new Histogram("FrontGtime ",      HIST_1D_INT, ADC_CHANNELS,        "Front Position compressed - ScCa,FwCa,FwSc,FwRw & time gates");
      
		//YLSA
		for (int i=0; i<NUM_DETECTORS; i++) {
			for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
				//eventParameter(slot, base address, channel, threshold channel)
				idEnergies[i][j]=vmeMap.eventParameter(whichADCslot(i), whichADCaddress(i), whichADCchannel(i,j), /*thresholds[i][j]*/ 100);
				hEnergies[i][j]=new Histogram("E_D"+i+"_S"+j, HIST_1D_INT, ADC_CHANNELS, "Detector "+i+", Strip "+j);
			}
			for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
				//eventParameter(slot, base address, channel, threshold channel)
				idTimes[i][j]=vmeMap.eventParameter(whichTDCslot(i), whichTDCaddress(i), whichTDCchannel(i,j), TIME_THRESHOLDS);
				hTimes[i][j]=new Histogram("T_D"+i+"_S"+j, HIST_1D_INT, ADC_CHANNELS, "Detector "+i+", Strip "+j+" time");
			}
		}

		System.err.println("# Parameters: "+getEventSize());
		System.err.println("ADC channels: "+ADC_CHANNELS);
		System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
		System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);

		hMultiplicity    = new Histogram("Multiplicity",                 HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Multiplicity of Energy and Time Hits");
		hHits            = new Histogram("Hits",                         HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Hits over ADC and TDC thresholds",                    STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist         = new Histogram("DecayAngDst_64",               HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Angular Distribution of Decays of gated State",       STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_bck     = new Histogram("DecayAngDst_64_bck",           HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Angular Distribution of Decays of gated State, randoms", STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_16      = new Histogram("DecayAngDst_16",               HIST_1D_INT, STRIPS_PER_DETECTOR,                               "Angular Distribution of Decays of gated State",       STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_16_bck  = new Histogram("DecayAngDst_16_bck",           HIST_1D_INT, STRIPS_PER_DETECTOR,                               "Angular Distribution of Decays of gated State, randoms", STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_gs         = new Histogram("DecayAngDst_64_gs",               HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Angular Distribution of Decays of gated ground State",       STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_bck_gs     = new Histogram("DecayAngDst_64_bck_gs",           HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Angular Distribution of Decays of gated ground State, randoms", STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_16_gs      = new Histogram("DecayAngDst_16_gs",               HIST_1D_INT, STRIPS_PER_DETECTOR,                               "Angular Distribution of Decays of gated ground State",       STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_16_bck_gs  = new Histogram("DecayAngDst_16_bck_gs",           HIST_1D_INT, STRIPS_PER_DETECTOR,                               "Angular Distribution of Decays of gated ground State, randoms", STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_ms         = new Histogram("DecayAngDst_64_ms",               HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Angular Distribution of Decays of gated metastable State",       STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_bck_ms     = new Histogram("DecayAngDst_64_bck_ms",           HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Angular Distribution of Decays of gated metastable State, randoms", STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_16_ms      = new Histogram("DecayAngDst_16_ms",               HIST_1D_INT, STRIPS_PER_DETECTOR,                               "Angular Distribution of Decays of gated metastable State",       STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_16_bck_ms  = new Histogram("DecayAngDst_16_bck_ms",           HIST_1D_INT, STRIPS_PER_DETECTOR,                               "Angular Distribution of Decays of gated metastable State, randoms", STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_ex         = new Histogram("DecayAngDst_64_ex",               HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Angular Distribution of Decays of gated 2nd excited State",       STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_bck_ex     = new Histogram("DecayAngDst_64_bck_ex",           HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Angular Distribution of Decays of gated 2nd excited State, randoms", STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_16_ex      = new Histogram("DecayAngDst_16_ex",               HIST_1D_INT, STRIPS_PER_DETECTOR,                               "Angular Distribution of Decays of gated 2nd excited State",       STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hAngDist_16_bck_ex  = new Histogram("DecayAngDst_16_bck_ex",           HIST_1D_INT, STRIPS_PER_DETECTOR,                               "Angular Distribution of Decays of gated 2nd excited State, randoms", STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hInterHits       = new Histogram("InterHits",                    HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Inter-Strip hits",                                    STRIPS_PER_DETECTOR+"*Det+Strip",   "Counts");
		hTimeHits        = new Histogram("Time Hits",                    HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Hits over TDC threshold",                                           "Strip",              "Counts");
		hHitsRatio       = new Histogram("Hits Ratio",                   HIST_1D_DBL, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Ratio of 'Time Hits' to 'Hits'");
		hEnergyHits      = new Histogram("Energy Hits",                  HIST_1D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Hits over ADC threshold",                                           "Strip",              "Counts");
		hTvsEhits        = new Histogram("T vs E hits",                  HIST_2D_INT, NUM_DETECTORS*STRIPS_PER_DETECTOR,                 "Time hits vs Energy hits",                                          "E hits",             "T hits");
		hEvsStrip        = new Histogram("EvsStrip",                     HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Energy vs. Strip, All Detectors",                                   "YLSA Energy",        STRIPS_PER_DETECTOR+"*Det+Strip");
		hTvsStrip        = new Histogram("TvsStrip",                     HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Time vs. Strip, All Detectors, multiplicity one",                   "Time",               STRIPS_PER_DETECTOR+"*Det+Strip");
		hEvsStripGA      = new Histogram("EvsStripGA",                   HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Energy vs. Strip, All Detectors, Gain Adjusted",                    "YLSA Energy",        STRIPS_PER_DETECTOR+"*Det+Strip");
		hTvsStripGA      = new Histogram("TvsStripGA",                   HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Time vs. Strip, All Detectors, Gain Adjusted", "Time",                                    STRIPS_PER_DETECTOR+"*Det+Strip");
		hEvsStripGA_gAll = new Histogram("EvsStripGA_gAll",              HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Energy vs. Strip, All Detectors, Gain Adjusted, gated on FP",       "YLSA Energy",        STRIPS_PER_DETECTOR+"*Det+Strip");
		hEvsStripGA_Time = new Histogram("EvsStripGA_Time",              HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Energy vs. Strip, All Detectors, Gain Adjusted gated on time peak", "YLSA Energy",        STRIPS_PER_DETECTOR+"*Det+Strip");
		hTvsStripGA_gAll = new Histogram("TvsStripGA_gAll, gated on FP", HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Time vs. Strip, All Detectors, Gain Adjusted", "Time",                                    STRIPS_PER_DETECTOR+"*Det+Strip");
//		hTvsStripGA_noE  = new Histogram("TvsStripGA_noE",               HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Time vs. Strip, All Detectors, Gain Adjusted, no \"E condition\"",  "Time",               STRIPS_PER_DETECTOR+"*Det+Strip");
		hEvsStrip_16     = new Histogram("EvsS",                         HIST_2D_INT, TWO_D_CHANNELS, STRIPS_PER_DETECTOR+1,             "Strip vs. Energy Deposited",                                        "YLSA Energy",             "Strip");
		hEsilVsFP        = new Histogram("EsilVsFP",                     HIST_2D_INT, TWO_D_CHANNELS, TWO_D_CHANNELS,                    "E (YLSA) vs. Focal Plane Position",                                 "QBrho [channels]",   "YLSA Energy");
//		hEsilVsFP_peak   = new Histogram("EsilVsFP_peak",                HIST_2D_INT, TWO_D_CHANNELS, TWO_D_CHANNELS,                    "E (YLSA) vs. Focal Plane Position gated on peak in FP",             "QBrho [channels]",   "YLSA Energy");
		hEsilVsFP_gTime  = new Histogram("EsilVsFP_gTime",               HIST_2D_INT, TWO_D_HIRES, TWO_D_HIRES,                    "E (YLSA) vs. Focal Plane Position gated on time peak",              "QBrho [channels]",   "YLSA Energy");
		hFPGAll_St       = new Histogram("FrontGAll_St",                 HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP");
		hFPGAll_St_m1    = new Histogram("FrontGAll_St_m1",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1");
		hFPGAll_m1_0_3    = new Histogram("FrontGAll_m1_0_3",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 0-3");
		hFPGAll_m1_4_7    = new Histogram("FrontGAll_m1_4_7",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 4-7");
		hFPGAll_m1_8_11    = new Histogram("FrontGAll_m1_8_11",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 8-11");
		hFPGAll_m1_12_15    = new Histogram("FrontGAll_m1_12_15",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 12-15");
		hFPGAll_m1_0_3_bck    = new Histogram("FrontGAll_m1_0_3_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 0-3, background");
		hFPGAll_m1_4_7_bck    = new Histogram("FrontGAll_m1_4_7_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 4-7, background");
		hFPGAll_m1_8_11_bck    = new Histogram("FrontGAll_m1_8_11_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 8-11, background");
		hFPGAll_m1_12_15_bck    = new Histogram("FrontGAll_m1_12_15_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 12-15, background");
		
		hFPGAll_gs_m1    = new Histogram("FrontGAll_gs_m1",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1");
		hFPGAll_gs_m1_0_3    = new Histogram("FrontGAll_gs_m1_0_3",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 0-3");
		hFPGAll_gs_m1_4_7    = new Histogram("FrontGAll_gs_m1_4_7",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 4-7");
		hFPGAll_gs_m1_8_11    = new Histogram("FrontGAll_gs_m1_8_11",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 8-11");
		hFPGAll_gs_m1_12_15    = new Histogram("FrontGAll_gs_m1_12_15",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 12-15");
		hFPGAll_gs_m1_bck    = new Histogram("FrontGAll_gs_m1_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 background");
		hFPGAll_gs_m1_0_3_bck    = new Histogram("FrontGAll_gs_m1_0_3_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 0-3 background");
		hFPGAll_gs_m1_4_7_bck    = new Histogram("FrontGAll_gs_m1_4_7_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 4-7 background");
		hFPGAll_gs_m1_8_11_bck    = new Histogram("FrontGAll_gs_m1_8_11_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 8-11 background");
		hFPGAll_gs_m1_12_15_bck    = new Histogram("FrontGAll_gs_m1_12_15_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 12-15 background");
		hFPGAll_ms_m1    = new Histogram("FrontGAll_ms_m1",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1");
		hFPGAll_ms_m1_0_3    = new Histogram("FrontGAll_ms_m1_0_3",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 0-3");
		hFPGAll_ms_m1_4_7    = new Histogram("FrontGAll_ms_m1_4_7",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 4-7");
		hFPGAll_ms_m1_8_11    = new Histogram("FrontGAll_ms_m1_8_11",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 8-11");
		hFPGAll_ms_m1_12_15    = new Histogram("FrontGAll_ms_m1_12_15",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 12-15");
		hFPGAll_ms_m1_bck    = new Histogram("FrontGAll_ms_m1_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 backgoudn");
		hFPGAll_ms_m1_0_3_bck    = new Histogram("FrontGAll_ms_m1_0_3_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 0-3 background");
		hFPGAll_ms_m1_4_7_bck    = new Histogram("FrontGAll_ms_m1_4_7_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 4-7 background");
		hFPGAll_ms_m1_8_11_bck    = new Histogram("FrontGAll_ms_m1_8_11_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 8-11 background");
		hFPGAll_ms_m1_12_15_bck    = new Histogram("FrontGAll_ms_m1_12_15_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 12-15 background");
		hFPGAll_ex_m1    = new Histogram("FrontGAll_ex_m1",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1");
		hFPGAll_ex_m1_0_3    = new Histogram("FrontGAll_ex_m1_0_3",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 0-3");
		hFPGAll_ex_m1_4_7    = new Histogram("FrontGAll_ex_m1_4_7",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 4-7");
		hFPGAll_ex_m1_8_11    = new Histogram("FrontGAll_ex_m1_8_11",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 8-11");
		hFPGAll_ex_m1_12_15    = new Histogram("FrontGAll_ex_m1_12_15",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 12-15");
		hFPGAll_ex_m1_bck    = new Histogram("FrontGAll_ex_m1_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 background");
		hFPGAll_ex_m1_0_3_bck    = new Histogram("FrontGAll_ex_m1_0_3_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 0-3 background");
		hFPGAll_ex_m1_4_7_bck    = new Histogram("FrontGAll_ex_m1_4_7_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 4-7 background");
		hFPGAll_ex_m1_8_11_bck    = new Histogram("FrontGAll_ex_m1_8_11_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 8-11 background");
		hFPGAll_ex_m1_12_15_bck    = new Histogram("FrontGAll_ex_m1_12_15_bck",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1 strips 12-15 background");
		hFPGAll_St_m1_bck= new Histogram("FrontGAll_St_m1_bck",          HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1, randoms");
		hFPGAll_St_m1_bck_test= new Histogram("FrontGAll_St_m1_bck_test",          HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m1, randoms");
		hFPGAll_St_m2    = new Histogram("FrontGAll_St_m2",              HIST_1D_INT, ADC_CHANNELS,                                      "Front Position - ScCa,FwCa,FwSc,FwRw gates and gated on EsilVsFP_m2");
		hEsilVsFP_m1     = new Histogram("EsilVsFP_m1",                  HIST_2D_INT, TWO_D_HIRES, TWO_D_HIRES,                    "E (YLSA) vs. Focal Plane Position, multiplicity=1",                 "QBrho [channels]",   "YLSA Energy");
		hEsilVsFP_m1_bck = new Histogram("EsilVsFP_m1_bck",              HIST_2D_INT, TWO_D_CHANNELS, TWO_D_CHANNELS,                    "E (YLSA) vs. Focal Plane Position, multiplicity=1, randoms",        "QBrho [channels]",   "YLSA Energy");
		hEsilVsFP_m2     = new Histogram("EsilVsFP_m2",                  HIST_2D_INT, TWO_D_CHANNELS, TWO_D_CHANNELS,                    "E (YLSA) vs. Focal Plane Position, multiplicity=2",                 "QBrho [channels]",   "YLSA Energy");
		hEsilVsFP_m3     = new Histogram("EsilVsFP_m3",                  HIST_2D_INT, TWO_D_CHANNELS, TWO_D_CHANNELS,                    "E (YLSA) vs. Focal Plane Position, multiplicity=3",                 "QBrho [channels]",   "YLSA Energy");
//added 4.26.07
		hScint2Cath_inYLSA =new Histogram("Scint2Cath_inYLSA", HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Scintillator 2 - YLSA gate",          "Scintillator PMT 2", CATH);
		hFrntCath_inYLSA = new Histogram("FrntCath_inYLSA", HIST_2D_INT, TWO_D_CHANNELS,      "Cathode vs Front Position - YLSA gate",                         FRONT_POS, CATH);
		hScint2Frnt_inYLSA = new Histogram("FrntScint2_inYLSA", HIST_2D_INT, TWO_D_CHANNELS,      "Scintillator PMT 2 vs Front Position",              FRONT_POS, "Scintillator 2");
//      hEvsChBroad      = new Histogram("EvsChBroad",                   HIST_2D_INT, TWO_D_CHANNELS, NUM_DETECTORS*STRIPS_PER_DETECTOR, "Channel vs. Energy", "Energy", "Channel");
		hTimeGA          = new Histogram("TimeGA",                       HIST_1D_INT, TWO_D_CHANNELS,                                    "Time, Gain Adjusted with alpha TOF subtracted");
		
//		gTvsStripGA_noE  = new Gate("Time_NoE",     hTvsStripGA_noE);
		gEvsS            = new Gate("EvsS",         hEvsStrip_16);
		gEsilVsFP        = new Gate("EsilVsFP",     hEsilVsFP_gTime);
		gYLSA_group      = new Gate("YLSA_group",     hEsilVsFP_gTime);
		gEsilVsFP_m1     = new Gate("EsilVsFP_m1",  hEsilVsFP_m1);
		gEsilVsFP_gs     = new Gate("EsilVsFP_gs",  hEsilVsFP_m1);
		gEsilVsFP_ms     = new Gate("EsilVsFP_ms",  hEsilVsFP_m1);
		gEsilVsFP_ex     = new Gate("EsilVsFP_ex",  hEsilVsFP_m1);
		gEsilVsFP_m2     = new Gate("EsilVsFP_m2",  hEsilVsFP_m2);
		gTime            = new Gate("Time",         hTimeGA);//gates on selected TDC channels
		gTime_bckgr      = new Gate("Time_bckgr",   hTimeGA);//gates on selected TDC channels

		// gates 1d-OLD
		gCthd            = new Gate("Counts", hCthd);
		gPeak            = new Gate("Peak",   hFrntGAll);
        
		//FROM TAJ
			
		//gates  2d-OLD
		gSntr2Cthd       = new Gate("Ca-Sc",     hSntr2Cthd);      //gate on Scintillator Cathode
		gFrntSntr2       = new Gate("Fw-Sc",     hFrntSntr2);      //gate on Front Scintillator
		gFrntCthd        = new Gate("Fw-Ca",     hFrntCthd);       //gate on Front Cathode
		gFrntRear        = new Gate("Fw-Rw",     hFrntPRearP);     //gate on Front Rear
		gFRexclude       = new Gate("FRexclude", hFrntPRearP);
		Gate gDataEvent  = new Gate("DataEvent", hSntr2Cthd);
      
//		FROM TAJ
		hFrntGTime.addGate(gPeak);
		hFrntSntr2GSC.addGate(gFrntSntr2);
		hFrntCthdGSC.addGate(gFrntCthd);
		hSntr2CthdGFC.addGate(gSntr2Cthd);
		hFrntSntr2GFC.addGate(gFrntSntr2);
		hSntr2CthdGFS.addGate(gSntr2Cthd);
		hFrntCthdGFS.addGate(gFrntCthd);
		hFrntCthdGFS_SC.addGate(gFrntCthd);
		hFrntRearGCSF.addGate(gFrntRear);
		hScint2Frnt_inYLSA.addGate(gFrntSntr2);
		hScint2Cath_inYLSA.addGate(gSntr2Cthd);
		hFrntCath_inYLSA.addGate(gFrntCthd);
		



 
		//scalers
		sClck      = new Scaler("Clock",        1);
		sEvntRaw   = new Scaler("Event Raw",    2);
		sEvntAccpt = new Scaler("Event Accept", 3);
		sScint     = new Scaler("Scintillator", 4);
		sCathode   = new Scaler("Cathode",      5);
		sFCLR      = new Scaler("FCLR",         6);
		sMon       = new Scaler("Monitor",      7);
		sGate      = new Scaler("Gate",         9);
		sBusy      = new Scaler("Busy",        10);
        

		//monitors
		mClck         = new Monitor("Clock",           sClck);
		mEvntRaw      = new Monitor("Raw Events",      sEvntRaw);
		mEvntAccept   = new Monitor("Accepted Events", sEvntAccpt);
		mScint        = new Monitor("Scintillator",    sScint);
		mCathode      = new Monitor("Cathode",         sCathode);
		                new Monitor("DataEvent",       gDataEvent);
		mFCLR         = new Monitor("FCLR",            sFCLR);
Monitor mDeadTime     = new Monitor(DEAD_TIME,         this);
Monitor mTrueDeadTime = new Monitor(TRUE_DEAD_TIME,    this);
		mMon          = new Monitor("Monitor",         sMon);
		mGate         = new Monitor("Gate",            sGate);
		mBusy         = new Monitor("Busy",            sBusy);
        
		//TAJ
		pThOffset = new DataParameter("THETA_OFFSET");
		pXTheta   = new DataParameter("(X|Theta) ");
		pCTheta   = new DataParameter("CTheta");
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
/*	private void retrieveCalibration() throws IOException,
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
    
/*	int [][] thresholds=new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
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
	}  (commented 16/08/06) */ 
	
	/**
	 * Sets ADC thresholds to be a certain energy in keV.  
	 * We have to look up gains in the array calibration,
	 * so retrieveCalibration() is assumed to have been 
	 * called.
	 */
	int [][] thresholds=new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	
	private void setADCthresholdsToE() throws IOException{
			double EnergyThresh=0.1;//desired energy threshold, in MeV
			double mce=0.0051;//conversion parameters: energy deposited by proton = gain-matched channels * mce + bce
			double bce=-0.67;
			double ThreshChanGA=(EnergyThresh-bce)/mce;
			for (int strip=0; strip < STRIPS_PER_DETECTOR; strip++){
				for (int det = 0; det < NUM_DETECTORS; det++){
						thresholds[det][strip] = (int)Math.round((ThreshChanGA-offset[det][strip])/gain[det][strip]); 
				}
			}//end for strip
		}//end set ADC thresh 
		
	// private void setADCthresholdsToE() throws IOException{  (commented 16/08/06)
		/* The following lines set up a text file reader
		 * for a file sitting in the same directory as this
		 * code. */
/*		String thresholdFile = "threshold_keV.txt";
		InputStreamReader isr = new InputStreamReader(
		getClass().getResourceAsStream(thresholdFile));  (commented 16/08/06) */
		/* */
/*		StreamTokenizer grabber = new StreamTokenizer(isr);
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
	} (commented 16/08/06) */ 
	
    
    
	/** Called every time Jam has an event it wants to sort.
	 * @param dataEvent contains all parameters for one event
	 * @throws Exception so Jam can handle exceptions
	 */
	public void sort(int [] dataEvent) throws Exception {
               
		//TAJ
	    	     int eCthd    = dataEvent[idCthd];
		   final int eMonitor = dataEvent[idMonitor];
		   final int SCINTR   = dataEvent[idScintR];
		   final int SCINTL   = dataEvent[idScintL];
		   final int FPOS     = dataEvent[idFrntPsn];
		   final int RPOS     = dataEvent[idRearPsn];
		   /*
			*  proper way to add for 2 phototubes at the ends of
			*  scintillating rod see Knoll
			*/
		   final int SCINT       = (int) Math.round(Math.sqrt(SCINTR * SCINTL));
		   final int SCINT1      = SCINTR >> TWO_D_FACTOR;
		   final int SCINT2      = SCINTL >> TWO_D_FACTOR; 
		   final int FPOS_COMPR  = FPOS   >> TWO_D_FACTOR;
		   final int RPOS_COMPR  = RPOS   >> TWO_D_FACTOR;
		   final int SCINT_COMPR = SCINT  >> TWO_D_FACTOR;
		   final double X_THETA  = pXTheta.getValue();
		   /*
			*  use this to correct the focus for different particle groups with
			*  different kinematics
			*/
		   final double THETA_OFFSET = pThOffset.getValue();
		   // center channel of Theta distribution
		   final double THETA_CENTER = pCTheta.getValue();
		   final int    THETA_CHANNEL;
		   final double DELTA_CATHODE;
		   final double THETA_VAL;
		   if (FPOS > 0 && RPOS > 0) {
			 THETA_CHANNEL = RPOS - FPOS;
			 //theta = THETA_CHANNEL;
			 THETA_VAL     = THETA_CHANNEL - THETA_OFFSET;
			 DELTA_CATHODE = (THETA_CENTER * (THETA_VAL));
		   } else {
			 THETA_CHANNEL = 0;
			 DELTA_CATHODE = 0;
			 THETA_VAL     = 0;
		   }
		int theta_calc = (int) Math.round(RPOS -FPOS - THETA_OFFSET);
		//int ePosnNew = getCorrectedPosition(FPOS, theta_calc);
		//int ecPosnNew = ePosnNew >> TWO_D_FACTOR;

		   eCthd += (int) DELTA_CATHODE;
		   final int ecCthd = eCthd >> TWO_D_FACTOR;

//	TAJ	singles spectra
		 hCthd.inc(eCthd);
		 hSntr1.inc(SCINTR);
		 hSntr2.inc(SCINTL);
		 hSntrSum.inc(SCINT);
		 hFrntPsn.inc(FPOS);
		 hRearPsn.inc(RPOS);
		 hMonitor.inc(eMonitor);
		
		final int FPOS_COMP_HI = FPOS >> TWO_D_HR_FACTOR;
		final int RPOS_COMP_HI = RPOS >> TWO_D_HR_FACTOR;
		int iFC = (int) (FPOS + THETA_VAL * X_THETA);
		
		hFC.inc(iFC);
		hSntr1Sntr2.inc(SCINT1,SCINT2);
		hSntr2Cthd.inc(SCINT2, ecCthd);
		hFrntCthd.inc(FPOS_COMPR, ecCthd);
		hRearCthd.inc(RPOS_COMPR, ecCthd);
		hFrntSntr.inc(FPOS_COMPR, SCINT_COMPR);
		hFrntSntr1.inc(FPOS_COMPR, SCINT1);
		hFrntSntr2.inc(FPOS_COMPR, SCINT2);
		hFrntPRearP.inc(FPOS_COMP_HI, RPOS_COMP_HI);
		
		//end TAJ stuff

		int [][] eEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int [][] eTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int [][] eEnergiesGA = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int [][] eTimesGA= new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

		boolean bSC  = gSntr2Cthd.inGate(SCINT2,ecCthd);
		boolean bFC  = gFrntCthd.inGate(FPOS_COMPR,ecCthd);
		boolean bFS  = gFrntSntr2.inGate(FPOS_COMPR,SCINT2);
		boolean bPID = bSC && bFC && bFS;
		boolean bFR  = gFrntRear.inGate(FPOS_COMP_HI, RPOS_COMP_HI) /*&& !gFRexclude.inGate(FPOS_COMP_HI, RPOS_COMP_HI)*/;
             
//taj
//  final boolean FR_RE_INGATE = gFrntRear.inGate(FPOS_COMP_HI, RPOS_COMP_HI);
//  final boolean GOOD_DIREC = FR_RE_INGATE;
//  final boolean ACCEPT = GOOD_DIREC;
  final boolean bGood = bFR && bPID;
  boolean bState = bGood && gPeak.inGate(FPOS);
         //from YLSA/OLD    
         
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
				boolean energy = eEnergies[i][j] >0 && eEnergies[i][j] <= LAST_ADC_BIN;
				boolean time = eTimes[i][j] > 0;
				//double eDeposit = ac.getEnergyDeposited(i,j,eEnergies[i][j]);//deposited in det [MeV]
				eTimesGA[i][j] = (int)Math.round(((((double)eTimes[i][j])+ t_offset[i][j]-1940.8)*t_gain[i][j])+1940.8);
			    int ecTimeGA = eTimesGA[i][j] >> TWO_D_FACTOR;
				//int eChannel = (int)Math.round(eDeposit*20);
				boolean bTime = bGood && gTime.inGate(ecTimeGA);
				boolean bTime_bckgr = bGood && gTime_bckgr.inGate(ecTimeGA);
				if (energy) {
					hEnergyHits.inc(stripBin);
					eEnergiesGA[i][j] = (int)Math.round(((double)eEnergies[i][j] - offset[i][j])*gain[i][j]);
					int eEnergyGA = eEnergiesGA[i][j] >> TWO_D_FACTOR;
					hEvsStripGA.inc(eEnergyGA,stripBin);
				}
				if (time) {
					hTimeHits.inc(stripBin);
//					hTvsStripGA_noE.inc(ecTimeGA,stripBin);
//					if (gTvsStripGA_noE.inGate(ecTimeGA,stripBin)) {
//						hFrntGTime_noE.inc(FPOS);
//					}			
				}
				if (time && energy) {
					hHits.inc(stripBin);
					detHit[multiplicity]=i;
					stripHit[multiplicity]=j;
					bin[multiplicity]=stripBin;
					multiplicity++;
					hTvsStrip.inc(eTimes[i][j] >> TWO_D_FACTOR, stripBin);
					eEnergiesGA[i][j] = (int)Math.round(((double)eEnergies[i][j] - offset[i][j])*gain[i][j]);
					int eEnergyGA = eEnergiesGA[i][j] >> TWO_D_FACTOR;
					if (bPID) {
						hTimeGA.inc(ecTimeGA);
						hTvsStripGA_gAll.inc(ecTimeGA,stripBin);
						//int _Senergy = (int)Math.round(ac.getEnergyDeposited(i,j,eEnergies[i][j])*10);                    	
						hEsilVsFP.inc(FPOS_COMPR,eEnergyGA);  //CHANGED!!
						hEvsStripGA_gAll.inc(eEnergyGA,stripBin);
					}
					if (bTime) {
						//hEvsChBroad.inc(eEnergyGA,stripBin);     
						hEvsStripGA_Time.inc(eEnergyGA,stripBin); 
						//int _Senergy = (int)Math.round(ac.getEnergyDeposited(i,j,eEnergies[i][j])*10);  
						hEsilVsFP_gTime.inc(FPOS_COMPR,eEnergyGA);    
						boolean bEState = gEsilVsFP.inGate(FPOS_COMPR,eEnergyGA);
							if(bEState){
								hFPGAll_St.inc(FPOS);
							}   
						boolean bYLSA_Group = gYLSA_group.inGate(FPOS_COMPR,eEnergyGA);
							if(bYLSA_Group){
								hScint2Cath_inYLSA.inc(SCINT2, ecCthd);
								hFrntCath_inYLSA.inc(FPOS_COMPR, ecCthd);
								hScint2Frnt_inYLSA.inc(FPOS_COMPR, SCINT2);								           	
							}
						if (multiplicity==1){
							hEsilVsFP_m1.inc(FPOS_COMPR,eEnergyGA);
							hEvsStrip_16.inc(eEnergyGA,j);
							boolean bEState_m1 = gEsilVsFP_m1.inGate(FPOS_COMPR,eEnergyGA);
							boolean bEState_gs = gEsilVsFP_gs.inGate(FPOS_COMPR,eEnergyGA);
							boolean bEState_ms = gEsilVsFP_ms.inGate(FPOS_COMPR,eEnergyGA);
							boolean bEState_ex = gEsilVsFP_ex.inGate(FPOS_COMPR,eEnergyGA);
							boolean bEvsS = gEvsS.inGate(eEnergyGA,j);
							boolean bTimeDecay = bState && bEvsS; 
							if(bEState_m1){
								hFPGAll_St_m1.inc(FPOS);
								if(bTimeDecay){
									hAngDist.inc(stripBin);
									hAngDist_16.inc(j);
									if(j>=0 && j<4){
										hFPGAll_m1_0_3.inc(FPOS);
									}
									if(j>=4 && j<8){
										hFPGAll_m1_4_7.inc(FPOS);
									}
									if(j>=8 && j<12){
										hFPGAll_m1_8_11.inc(FPOS);
									}
									if(j>=12 && j<16){
										hFPGAll_m1_12_15.inc(FPOS);
									}
								}
							}  //CHANGED!!!
							if(bEState_gs){
								hFPGAll_gs_m1.inc(FPOS);
								if(bTimeDecay){
									hAngDist_gs.inc(stripBin);
									hAngDist_16_gs.inc(j);
									if(j>=0 && j<4){
										hFPGAll_gs_m1_0_3.inc(FPOS);
									}
									if(j>=4 && j<8){
										hFPGAll_gs_m1_4_7.inc(FPOS);
									}
									if(j>=8 && j<12){
										hFPGAll_gs_m1_8_11.inc(FPOS);
									}
									if(j>=12 && j<16){
										hFPGAll_gs_m1_12_15.inc(FPOS);
									}
								}
							}
							if(bEState_ms){
								hFPGAll_ms_m1.inc(FPOS);
								if(bTimeDecay){
									hAngDist_ms.inc(stripBin);
									hAngDist_16_ms.inc(j);
									if(j>=0 && j<4){
										hFPGAll_ms_m1_0_3.inc(FPOS);
									}
									if(j>=4 && j<8){
										hFPGAll_ms_m1_4_7.inc(FPOS);
									}
									if(j>=8 && j<12){
										hFPGAll_ms_m1_8_11.inc(FPOS);
									}
									if(j>=12 && j<16){
										hFPGAll_ms_m1_12_15.inc(FPOS);
									}
								}					
							}
							if(bEState_ex){
								hFPGAll_ex_m1.inc(FPOS);
								if(bTimeDecay){
									hAngDist_ex.inc(stripBin);
									hAngDist_16_ex.inc(j);
									if(j>=0 && j<4){
										hFPGAll_ex_m1_0_3.inc(FPOS);
									}
									if(j>=4 && j<8){
										hFPGAll_ex_m1_4_7.inc(FPOS);
									}
									if(j>=8 && j<12){
										hFPGAll_ex_m1_8_11.inc(FPOS);
									}
									if(j>=12 && j<16){
										hFPGAll_ex_m1_12_15.inc(FPOS);																		}
								}
							}
						} else if (multiplicity==2){
							hEsilVsFP_m2.inc(FPOS_COMPR,eEnergyGA); 
							boolean bEState_m2 = gEsilVsFP_m2.inGate(FPOS_COMPR,eEnergyGA);
							if(bEState_m2){
								hFPGAll_St_m2.inc(FPOS);
							} //CHANGED!!
						} else if (multiplicity==3) {
							hEsilVsFP_m3.inc(FPOS_COMPR,eEnergyGA);  //CHANGED!!!!
						}
						hFrntGTime.inc(FPOS);
						//hcFrntGTime.inc(FPOS >> COMPRESS_FACTOR);
					}
					if (bTime_bckgr) {						             	
						if (multiplicity==1){
							hEsilVsFP_m1_bck.inc(FPOS_COMPR,eEnergyGA);
							boolean bEState_m1_bck = gEsilVsFP_m1.inGate(FPOS_COMPR,eEnergyGA);
							boolean bEvsS_bck = gEvsS.inGate(eEnergyGA,j);
							boolean bTimeDecay_bck = bState && bEvsS_bck; 
							if(bEState_m1_bck){
								hFPGAll_St_m1_bck.inc(FPOS);
								if(bTimeDecay_bck){
									hFPGAll_St_m1_bck_test.inc(FPOS);
									hAngDist_bck.inc(stripBin);
									hAngDist_16_bck.inc(j);
									if(j>=0 && j<4){
										hFPGAll_m1_0_3_bck.inc(FPOS);
									}
									if(j>=4 && j<8){
										hFPGAll_m1_4_7_bck.inc(FPOS);
									}
									if(j>=8 && j<12){
										hFPGAll_m1_8_11_bck.inc(FPOS);
									}
									if(j>=12 && j<16){
										hFPGAll_m1_12_15_bck.inc(FPOS);
									}
								}
							}
							boolean bEState_gs_bck = gEsilVsFP_gs.inGate(FPOS_COMPR,eEnergyGA);
							if(bEState_gs_bck && bTimeDecay_bck){
								hFPGAll_gs_m1_bck.inc(FPOS);
								hAngDist_bck_gs.inc(stripBin);
								hAngDist_16_bck_gs.inc(j);
								if(j>=0 && j<4){
									hFPGAll_gs_m1_0_3_bck.inc(FPOS);
								}
								if(j>=4 && j<8){
									hFPGAll_gs_m1_4_7_bck.inc(FPOS);
								}
								if(j>=8 && j<12){
									hFPGAll_gs_m1_8_11_bck.inc(FPOS);
								}
								if(j>=12 && j<16){
									hFPGAll_gs_m1_12_15_bck.inc(FPOS);
								}
							}
							boolean bEState_ms_bck = gEsilVsFP_ms.inGate(FPOS_COMPR,eEnergyGA);
							if(bEState_ms_bck && bTimeDecay_bck){
								hFPGAll_ms_m1_bck.inc(FPOS);
								hAngDist_bck_ms.inc(stripBin);
								hAngDist_16_bck_ms.inc(j);
								if(j>=0 && j<4){
									hFPGAll_ms_m1_0_3_bck.inc(FPOS);
								}
								if(j>=4 && j<8){
									hFPGAll_ms_m1_4_7_bck.inc(FPOS);
								}
								if(j>=8 && j<12){
									hFPGAll_ms_m1_8_11_bck.inc(FPOS);
								}
								if(j>=12 && j<16){
									hFPGAll_ms_m1_12_15_bck.inc(FPOS);
								}
							}
							boolean bEState_ex_bck = gEsilVsFP_ex.inGate(FPOS_COMPR,eEnergyGA);
							if(bEState_ex_bck && bTimeDecay_bck){
								hFPGAll_ex_m1_bck.inc(FPOS);
								hAngDist_bck_ex.inc(stripBin);
								hAngDist_16_bck_ex.inc(j);
								if(j>=0 && j<4){
									hFPGAll_ex_m1_0_3_bck.inc(FPOS);
								}
								if(j>=4 && j<8){
									hFPGAll_ex_m1_4_7_bck.inc(FPOS);
								}
								if(j>=8 && j<12){
									hFPGAll_ex_m1_8_11_bck.inc(FPOS);
								}
								if(j>=12 && j<16){
									hFPGAll_ex_m1_12_15_bck.inc(FPOS);
								}
							} 
						} 
					}
					hTvsStripGA.inc(ecTimeGA,stripBin);
				}
				if (((int [])hHits.getCounts())[stripBin]==0){//avoid div by zero and set to zero
					((double [])hHitsRatio.getCounts())[stripBin]=0.0;
				} else {//actually divide
					((double [])hHitsRatio.getCounts())[stripBin]=(double)(((int [])hTimeHits.getCounts())[stripBin])/(double)(((int [])hHits.getCounts())[stripBin]);
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
         
	    	if (bSC && bFR) {
	  	      // gate on Scintillator vs Cathode
			   hFrntSntr2GSC.inc(FPOS_COMPR, SCINT2);
			   hFrntCthdGSC.inc(FPOS_COMPR, ecCthd);
			}
		    if (bFC && bFR) {
		      // gate on Front Wire Position vs Cathode
			    hSntr2CthdGFC.inc(SCINT2, ecCthd);
			    hFrntSntr2GFC.inc(FPOS_COMPR, SCINT2);
			}
			if (bFS && bFR){
		      // gate on Front Wire Position vs Scintillator
				hSntr2CthdGFS.inc(SCINT2, ecCthd);
			    hFrntCthdGFS.inc(FPOS_COMPR, ecCthd);   
			}
		    if (bSC && bFS && bFR) {
				hFrntCthdGFS_SC.inc(FPOS_COMPR, ecCthd);
		    }
			if (bPID) {
			  // gated on all 3 gate above
			  //writeEvent(dataEvent);
			hFrntGCSF.inc(FPOS);
			hRearGCSF.inc(RPOS);
			hFrntRearGCSF.inc(FPOS_COMP_HI, RPOS_COMP_HI);
			    if (bGood) {
				    writeEvent(dataEvent);
				    hFrntGAll.inc(FPOS);
				    hRearGAll.inc(RPOS);
				    hFrontTheta.inc(FPOS >> 2, THETA_CHANNEL);
				    hRearTheta.inc(RPOS >> 2, THETA_CHANNEL);
				}
			}			         
}
} 

