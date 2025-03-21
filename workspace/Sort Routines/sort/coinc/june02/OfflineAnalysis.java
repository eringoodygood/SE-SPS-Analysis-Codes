/*
 */
package sort.coinc.june02;
import jam.data.DataParameter;
import jam.data.Gate;
import jam.data.Histogram;
import jam.data.Monitor;
import jam.data.Scaler;
import jam.sort.SortRoutine;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.StreamTokenizer;

import sort.coinc.offline.ResidualKinematics;

import dwvisser.analysis.ArrayCalibration;
import dwvisser.nuclear.Nucleus;

/** Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>, which
 * was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right way; also
 * added gate to cathAnde. Now the data shipped across
 * is in the format as produced by the V7x5's.  The EventInputStream this code
 * works with is YaleCAEN_InputStream.
 * 13 July '02 copied from the file I used for offline sorting in April '02: 
 * sort.coinc.V7x5Format.Ne19_offline2 ... I will modify it so that it conforms to the
 * initialization in sort.coinc.june02.Jac4adcs, but have the additional features
 * needed to further clean up PID and correct the focal plane.  In addition, it will 
 * have 2 dimensions for 2D histograms, small(256) and large(512) to conserve memory,
 * but still allow detailed views of the most interesting 2D spectra.
 * 
 * 24 July 2002--modifying so that coincidences will be determined by EsilVsYLSA gate,
 * and each strip will have a FP spectrum from being in the TimeDecay and EsilVsYLSA gates (&PID),
 * plus a FP spectrum from being in the TimeBackgroud, EsilVsYLSA gates, & PID.
 *
 * @author Dale Visser
 * @since 26 July 2001
 */
public class OfflineAnalysis extends SortRoutine {
	//VME properties
	static final int[] ADC_BASE =
		{ 0x20000000, 0x20010000, 0x20020000, 0x20030000 };
	static final int[] TDC_BASE = { 0x30000000, 0x30010000, 0x30020000 };
	static final int SCALER_ADDRESS = 0xf0e00000;
	//static final int THRESHOLDS = 200;  //ADC lower threshold in channels
	static final int TIME_THRESHOLDS = 30; //TDC lower threshold in channels
	static final int TIME_RANGE = 600; //ns
	static final int LAST_ADC_BIN = 3840;

	//LEDA Detectors
	static final int NUM_DETECTORS = 5;
	static final int STRIPS_PER_DETECTOR = 16;

	//names
	static final String DEAD_TIME = "Dead Time %";
	static final String TRUE_DEAD_TIME = "True Dead Time %";

	//histogramming constants
	final int ADC_CHANNELS = 4096; //num of channels per ADC
	final int COMPRESSED_CHANNELS = 512;
	//number of channels in compressed histogram
	final int TWO_D_HIRES = 512;
	// number of channels per dimension in hi-res 2-d histograms
	final int TWO_D_CHANNELS = 256;
	//number of channels per dimension in lo-res 2-d histograms
	//bits to shift for compression
	final int COMPRESS_FACTOR =
		Math.round(
			(float) (Math.log(ADC_CHANNELS / COMPRESSED_CHANNELS)
				/ Math.log(2.0)));
	final int TWO_D_FACTOR =
		Math.round(
			(float) (Math.log(ADC_CHANNELS / TWO_D_CHANNELS) / Math.log(2.0)));
	final int TWO_D_HIRES_FACTOR =
		Math.round(
			(float) (Math.log(ADC_CHANNELS / TWO_D_HIRES) / Math.log(2.0)));

	//histograms and parameter ID's for YLSA channels
	Histogram[][] hEnergies = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	Histogram[][] hTimes = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	int[][] idEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	int[][] idTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

	// ungated spectra
	Histogram hCthd, hAnde, hSntr1, hSntr2, hSntrSum, hFrntPsn, hRearPsn;
	Histogram hFrntHgh; //front Wire Pulse Height
	Histogram hRearHgh; //Rear Wire Pulse Height
	Histogram hFrntPH; // position x height y
	Histogram hRearPH;
	Histogram hCthdAnde,
		hSntrCthd,
		hFrntCthd,
		hFrntAnde,
		hFrntSntr,
		hFrntPRearP;
	Histogram hSntrAnde;
	Histogram hFrntY, hRearY, hYvsPsn, hYvsPsnGPID;

	Histogram hFrntSntrGSC, hFrntCthdGSC; //gate by scintillator cathode
	Histogram hSntrCthdGFC, hFrntSntrGFC; //gate by Front wire Cathode
	Histogram hSntrCthdGFS, hFrntCthdGFS; //gate by Front wire Scintillator
	Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll;
	//front and rear wire gate on all
	Histogram hcFrntGAll, hcRearGAll;
	//front and rear wire gated on All compressed & time
	Histogram hFrntNewGAll, hcFrntNewGAll;
	//corrected FP posn gated on All compressed & time
	Histogram hcFrntGTime, hFrntGTime;
	Histogram hcFrntGBkgd, hFrntGBkgd;
	//front and rear wire gated on All compressed & time

	Gate /*gSilicon*/
	gCthd; //gates 1 d
	Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear, gCthdAnde; //gates 2 d
	Gate gAndeSntr, gAndeFrnt;
	Gate gFRexclude;
	Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode; //scalers
	Scaler sFCLR; //number of FCLR's that went to ADC's

	Histogram hHits, hTvsStrip, hTimeHits, hTvsEhits, hInterHits, hEnergyHits;
	Histogram hMultiplicity;
	Histogram hEvsStripGA, hTvsStripGA; //gain adjusted spectra
	Histogram hAngDist; //for getting angular distribution of alphas

	//coincidence histograms & gates
	Gate gPeak; //added to frontGAll, front gTime
	Histogram hTimeGA, hTimeGAstate, hTimeGAdecay;
	//all strips time gain adjusted (TOF adjustment for alpha)
	Gate gTimeBroad, gTimeState, gTimeDecay, gTimeBkgd1,gTimeBkgd2;
	Histogram hEvsStripBroad, hEvsStripState, hEvsStripDecay;
	Histogram hEvsStripDecay_bkgd;
	Histogram hEvsChBroad, hEvsChState, hEvsChDecay;
	Histogram hEsilVsFP; Gate gEsilVsFP;
	Histogram hRYvsFY;
	
	Histogram [] hCoincSpectrum = new Histogram[this.STRIPS_PER_DETECTOR];
	Histogram [] hBkgdSpectrum = new Histogram[this.STRIPS_PER_DETECTOR];
	
	Gate gEvsS;
	Gate gRYvsFY;
	//id numbers for the signals;
	int idCthd,
		idAnde,
		idScintR,
		idScintL,
		idFrntPsn,
		idRearPsn,
		idFrntHgh,
		idRearHgh,
		idYFrnt,
		idYRear;

	DataParameter dp_thetaOffset, dp_phiOffset;
	DataParameter dp_cathMiddle, dp_scintMiddle, dp_anodeMiddle;
	DataParameter dp_CathThetaSlope, dp_ScintThetaSlope, dp_AndeThetaSlope;
	DataParameter dp_lowChannel, dp_highChannel, dp_lowRho, dp_highRho;
	Histogram CathTheta, CathPhi;
	Histogram ScintTheta, ScintPhi;
	Histogram PosnTheta, PosnPhi;
	Histogram AndeTheta, AndePhi;

	Histogram hScintNewVsPosn,
		hCathNewVsPosn,
		hCathNewVsScintNew,
		hAndeNewVsCathNew,
		hAndeNewVsScintNew,
		hAndeNewVsPosn;
	Gate gScintNewVsPosn,
		gCathNewVsPosn,
		gCathNewVsScintNew,
		gAndeNewVsCathNew,
		gAndeNewVsScintNew,
		gAndeNewVsPosn;
	Histogram hCathNewVsScintNew_gFC,
		hCathNewVsScintNew_gFS,
		hAndeNewVsCathNew_g3,
		hAndeNewVsScintNew_g3,
		hAndeNewVsPosn_g3;
	Histogram hThetaVsFrontGPID,
		hThetaVsFront_corr,
		hPhiVsFrontGPID,
		hPhiVsFront_corr;

	Gate gYvsPsn;
	Gate gThetaVsPsnCorr;
	Histogram hCoincHits, hCoincHits_bkgd;
	

	int[] idLeakageCurrent = new int[NUM_DETECTORS];
	Histogram[] hLeakageCurrent = new Histogram[NUM_DETECTORS];
	Gate[] gLeakageCurrent = new Gate[NUM_DETECTORS];
	Monitor[] mLeakageCurrent = new Monitor[NUM_DETECTORS];
	Scaler[] sYLSArate = new Scaler[NUM_DETECTORS];
	Monitor[] mYLSArate = new Monitor[NUM_DETECTORS];

	/**
	 * Containers of information about any strips with both an energy and time
	 * signal for this event.
	 */
	int TOTAL_STRIPS = STRIPS_PER_DETECTOR * NUM_DETECTORS;
	int[] detHit = new int[TOTAL_STRIPS];
	int[] stripHit = new int[TOTAL_STRIPS];
	int[] bin = new int[TOTAL_STRIPS];

	int numInterHits; //number of real interStrip hits (in TDC window)
	int[] interDetHit = new int[STRIPS_PER_DETECTOR * NUM_DETECTORS];
	int[] interStripHit = new int[STRIPS_PER_DETECTOR * NUM_DETECTORS];
	int[] interBin = new int[STRIPS_PER_DETECTOR * NUM_DETECTORS];

	ResidualKinematics rk; //for calculating Residual trajectory
	//double Mproton;
	double mAlpha, m3He;

	ArrayCalibration ac;

	/* array of histograms for storing 1d theta-slices of position */
	int[] thetaSliceLowerLimits =
		{
			-159,
			-139,
			-119,
			-99,
			-79,
			-59,
			-39,
			-19,
			0,
			20,
			40,
			60,
			80,
			100,
			140 };
	Histogram[] hPosn_slice = new Histogram[thetaSliceLowerLimits.length];

	/** Sets up objects, called when Jam loads the sort routine.
	 * @throws Exception necessary for Jam to handle exceptions
	 */
	public void initialize() throws Exception {
		retrieveCalibration();
		setADCthresholdsToE();
		new Nucleus(); //initialize nuclear data
		//mProton = (new Nucleus(1,1)).getMass().value;
		mAlpha = (new Nucleus(2, 4)).getMass().value;
		m3He = (new Nucleus(2, 3)).getMass().value;
		double ebeam = 25.0;
		rk =
			new ResidualKinematics(
				ebeam,
				0,
				new Nucleus(2, 3),
				new Nucleus(9, 19),
				new Nucleus(1, 3));
		vmeMap.setScalerInterval(3);
		for (int i = 0; i < TDC_BASE.length; i++) {
			vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
		}
		//Focal Plane Detector      (slot, base address, channel, threshold channel)
		idCthd = vmeMap.eventParameter(2, ADC_BASE[0], 10, 0);
		idAnde = vmeMap.eventParameter(2, ADC_BASE[0], 1, 0);
		idScintR = vmeMap.eventParameter(2, ADC_BASE[0], 2, 0);
		idScintL = vmeMap.eventParameter(2, ADC_BASE[0], 3, 0);
		idFrntPsn = vmeMap.eventParameter(2, ADC_BASE[0], 4, 0);
		idRearPsn = vmeMap.eventParameter(2, ADC_BASE[0], 5, 0);
		idFrntHgh = vmeMap.eventParameter(2, ADC_BASE[0], 6, 0);
		idRearHgh = vmeMap.eventParameter(2, ADC_BASE[0], 7, 0);
		idYFrnt = vmeMap.eventParameter(2, ADC_BASE[0], 8, 0);
		idYRear = vmeMap.eventParameter(2, ADC_BASE[0], 9, 0);
		int idDummy = vmeMap.eventParameter(2, ADC_BASE[0], 0, 0);
		hCthd =
			new Histogram(
				"Cathode     ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Cathode Raw ");
		hAnde =
			new Histogram(
				"Anode       ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Anode Raw");
		hSntr1 =
			new Histogram(
				"Scint1      ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Scintillator PMT 1");
		hSntr2 =
			new Histogram(
				"Scint2      ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Scintillator PMT 2");
		hSntrSum =
			new Histogram(
				"ScintSum    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Scintillator Sum");
		hFrntPsn =
			new Histogram(
				"FrontPosn    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Front Wire Position");
		hRearPsn =
			new Histogram(
				"RearPosn     ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Rear Wire Position");
		hFrntHgh =
			new Histogram(
				"FrontHeight   ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Front Wire Pulse Height");
		hRearHgh =
			new Histogram(
				"RearHeight    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Rear Wire Pulse Height");
		hFrntY =
			new Histogram(
				"Front Y",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Y (vertical) Position at Front Wire");
		hRearY =
			new Histogram(
				"Rear Y",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Y (vertical) Position at Rear Wire");
		hFrntPH =
			new Histogram(
				"FrontPvsHeight",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Pulse Height vs Front Position",
				"Front Position",
				"Pulse Height");
		hRearPH =
			new Histogram(
				"RearPvsHeight ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Pulse Height vs Rear Position",
				"Rear Position",
				"Pulse Height");
		hYvsPsn =
			new Histogram(
				"Y vs Position",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Front Y vs. Front Wire Position (X)",
				"Position",
				"Y");
		gYvsPsn = new Gate("Y vs Position", hYvsPsn);
		hYvsPsnGPID =
			new Histogram(
				"YvsPosnPID",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Front Y vs. Front Wire Position (X) Gated on PID",
				"Position",
				"Y");
		hYvsPsnGPID.addGate(gYvsPsn);
		hRYvsFY =
			new Histogram(
				"RearYvsFrntY",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Rear Y vs. Front Y",
				"Front Y",
				"Rear Y");
		gRYvsFY = new Gate("RYvsFY", hRYvsFY);
		hCthdAnde =
			new Histogram(
				"CathodeAnode  ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Cathode vs Anode ",
				"Cathode",
				"Anode");
		hSntrCthd =
			new Histogram(
				"ScintCathode  ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Cathode vs Scintillator",
				"Scintillator",
				"Cathode");
		hFrntCthd =
			new Histogram(
				"FrontCathode  ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Cathode vs Front Position",
				"Front Position",
				"Cathode");
		hFrntAnde =
			new Histogram(
				"FrontAnode  ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Anode vs Front Position",
				"Front Position",
				"Anode");
		hSntrAnde =
			new Histogram(
				"ScintAnode  ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Anode vs Scintillator",
				"Scintillator",
				"Anode");

		hFrntSntr =
			new Histogram(
				"FrontScint ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Scintillator vs Front Position",
				"Front Position",
				"Scintillator");
		hFrntPRearP =
			new Histogram(
				"FrontRear  ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Rear Position vs Front Position",
				"Front Position",
				"Rear Position");
		//gate on Scintillator Cathode
		hFrntSntrGSC =
			new Histogram(
				"FrontScintGSC ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Scintillator vs Front Position - ScCa gate",
				"Front Position",
				"Scintillator");
		hFrntCthdGSC =
			new Histogram(
				"FrontCathodeGSC",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Cathode vs Front Position - ScCa gate",
				"Front Position",
				"Cathode");
		//gate on Front Wire Cathode
		hSntrCthdGFC =
			new Histogram(
				"ScintCathodeGFC",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Cathode vs Scintillator - FwCa gate",
				"Scintillator",
				"Cathode");
		hFrntSntrGFC =
			new Histogram(
				"FrontScintGFC",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Scintillator vs Front Position - FwCa gate",
				"Front Position",
				"Scintillator");
		//gate on Front Wire Scintillator
		hSntrCthdGFS =
			new Histogram(
				"ScintCathodeGFS",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Cathode vs Scintillator - FwSc gate",
				"Scintillator",
				"Cathode");
		hFrntCthdGFS =
			new Histogram(
				"FrontCathodeGFS ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Cathode vs Front Position - FwSc gate ",
				"Front Position",
				"Cathode");
		//gated on 3 gates
		hFrntGCSF =
			new Histogram(
				"FrontGCSF    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Front Position - ScCa,FwCa,FwSc gates");
		hRearGCSF =
			new Histogram(
				"RearGCSF    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Rear Position - ScCa,FwCa,FwSc gates");
		hFrntRearGCSF =
			new Histogram(
				"FRGateCSF  ",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"Front vs. Rear - ScCa, FwCa, FwSc gates");
		//gated on 4 gates
		hFrntGAll =
			new Histogram(
				"FrontGAll    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Front Position - ScCa,FwCa,FwSc,FwRw gates");
		hRearGAll =
			new Histogram(
				"RearGAll    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Rear Position - ScCa,FwCa,FwSc,FwRw gates");
		hcFrntGAll =
			new Histogram(
				"FrontGAllcmp ",
				HIST_1D_INT,
				COMPRESSED_CHANNELS,
				"Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
		hcRearGAll =
			new Histogram(
				"RearGAllcmp ",
				HIST_1D_INT,
				COMPRESSED_CHANNELS,
				"Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");
		hFrntNewGAll =
			new Histogram(
				"FrontNewGAll ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Corrected Front Position - ScCa,FwCa,FwSc,FwRw gates");
		hcFrntNewGAll =
			new Histogram(
				"FrNewGAllcmp ",
				HIST_1D_INT,
				COMPRESSED_CHANNELS,
				"Corrected Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
		hFrntGTime =
			new Histogram(
				"FrNGTime    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Corrected Front Position - ScCa,FwCa,FwSc,FwRw & time gates");
		hcFrntGTime =
			new Histogram(
				"FrontGTimecmp ",
				HIST_1D_INT,
				COMPRESSED_CHANNELS,
				"Corrected Front Position compressed - ScCa,FwCa,FwSc,FwRw & time gates");
		hFrntGBkgd =
			new Histogram(
				"FrNGBkgd    ",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Corrected Front Position - ScCa,FwCa,FwSc,FwRw & bkgd time gates");
		hcFrntGBkgd =
			new Histogram(
				"FrontGBkgdcmp ",
				HIST_1D_INT,
				COMPRESSED_CHANNELS,
				"Corrected Front Position compressed - ScCa,FwCa,FwSc,FwRw & bkgd time gates");

		CathTheta =
			new Histogram(
				"CathTheta",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Theta vs. Cathode",
				"Cathode",
				"Theta");
		CathPhi =
			new Histogram(
				"CathPhi",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Phi vs. Cathode",
				"Cathode",
				"Phi");
		ScintTheta =
			new Histogram(
				"ScintTheta",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Theta vs. Scintillator",
				"Scintillator",
				"Theta");
		ScintPhi =
			new Histogram(
				"ScintPhi",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Phi vs. Scintillator",
				"Scintillator",
				"Phi");
		AndeTheta =
			new Histogram(
				"AndeTheta",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Theta vs. Anode",
				"Anode",
				"Theta");
		AndePhi =
			new Histogram(
				"AndePhi",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Phi vs. Anode",
				"Anode",
				"Phi");
		PosnTheta =
			new Histogram(
				"PosnTheta",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Theta vs. Position",
				"Position",
				"Theta");

		hScintNewVsPosn =
			new Histogram(
				"ScintNewVsPosn",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Scintillator vs. Position",
				"Position",
				"ScintNew");
		hCathNewVsPosn =
			new Histogram(
				"CathNewVsPosn",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Cathode vs. Position",
				"Position",
				"CathNew");
		hCathNewVsScintNew =
			new Histogram(
				"CathNewVsSntrNew",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Cathode vs. New Scintillator",
				"ScintNew",
				"CathNew");
		hAndeNewVsCathNew =
			new Histogram(
				"AndeNewVsCathNew",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Anode vs. New Cathode",
				"CathNew",
				"AndeNew");
		hAndeNewVsScintNew =
			new Histogram(
				"AndeNewVsSntrNew",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Anode vs. New Scintillator",
				"SntrNew",
				"AndeNew");
		hAndeNewVsPosn =
			new Histogram(
				"AndeNewVsPosn",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Anode vs. Position",
				"Position",
				"AndeNew");

		gScintNewVsPosn = new Gate("ScintNewVsPosn", hScintNewVsPosn);
		gCathNewVsPosn = new Gate("CathNewVsPosn", hCathNewVsPosn);
		gCathNewVsScintNew = new Gate("CathNewVsSntrNew", hCathNewVsScintNew);
		gAndeNewVsCathNew = new Gate("AndeNewVsCathNew", hAndeNewVsCathNew);
		gAndeNewVsScintNew = new Gate("AndeNewVsSntrNew", hAndeNewVsScintNew);
		gAndeNewVsPosn = new Gate("AndeNewVsPosn", hAndeNewVsPosn);

		hCathNewVsScintNew_gFS =
			new Histogram(
				"CathSntrNew_gFS",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Cathode vs. New Scintillator, gated on New FrSc",
				"ScintNew",
				"CathNew");
		hCathNewVsScintNew_gFS.addGate(gCathNewVsScintNew);
		hCathNewVsScintNew_gFC =
			new Histogram(
				"CathSntrNew_gFC",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Cathode vs. New Scintillator, gated on New FrCa",
				"ScintNew",
				"CathNew");
		hCathNewVsScintNew_gFC.addGate(gCathNewVsScintNew);

		hAndeNewVsCathNew_g3 =
			new Histogram(
				"AndeCathNew_g3",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Anode vs. New Cathode, gated on new FrSv, FrCa, ScCa",
				"CathNew",
				"AndeNew");
		hAndeNewVsCathNew_g3.addGate(gAndeNewVsCathNew);
		hAndeNewVsScintNew_g3 =
			new Histogram(
				"AndeSntrNew_g3",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Anode vs. New Scintillator, gated on new FrSv, FrCa, ScCa",
				"SntrNew",
				"AndeNew");
		hAndeNewVsScintNew_g3.addGate(gAndeNewVsScintNew);
		hAndeNewVsPosn_g3 =
			new Histogram(
				"AndeNewVsPosn_g3",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				"New Anode vs. Position, gated on new FrSv, FrCa, ScCa",
				"Position",
				"AndeNew");
		hAndeNewVsPosn_g3.addGate(gAndeNewVsPosn);
		hThetaVsFrontGPID =
			new Histogram(
				"ThetaVsPosnPID",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Theta vs. Position, gated on PID",
				"Position",
				"Theta");
		hThetaVsFront_corr =
			new Histogram(
				"ThetaVsPosnNew",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Theta vs. Corrected Position, gated on PID",
				"New Position",
				"Theta");
		gThetaVsPsnCorr = new Gate("ThetaPsn", hThetaVsFront_corr);
		hPhiVsFrontGPID =
			new Histogram(
				"PhiVsPosnPID",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Phi vs. Position, gated on PID",
				"Position",
				"Phi");
		hPhiVsFront_corr =
			new Histogram(
				"PhiVsPosnNew",
				HIST_2D_INT,
				TWO_D_HIRES,
				"Phi vs. Corrected Position, gated on PID",
				"New Position",
				"Phi");

		for (int i = 0; i < hPosn_slice.length; i++) {
			int ll = thetaSliceLowerLimits[i];
			int ul = ll + 19;
			hPosn_slice[i] =
				new Histogram(
					"ThetaSlice" + i,
					HIST_1D_INT,
					ADC_CHANNELS,
					"Uncorrected position, gated on PID, theta from "
						+ ll
						+ " to "
						+ ul);
		}

		dp_thetaOffset = new DataParameter("thetaOffset");
		dp_phiOffset = new DataParameter("phiOffset");
		dp_cathMiddle = new DataParameter("CathMiddle");
		dp_scintMiddle = new DataParameter("ScintMiddle");
		dp_anodeMiddle = new DataParameter("AnodeMiddle");
		dp_CathThetaSlope = new DataParameter("CathThetaSlope");
		dp_ScintThetaSlope = new DataParameter("ScintThetaSlope");
		dp_AndeThetaSlope = new DataParameter("AnodeThetaSlope");
		dp_lowChannel = new DataParameter("Low Channel");
		dp_highChannel = new DataParameter("High Channel");
		dp_lowRho = new DataParameter("Low Rho");
		dp_highRho = new DataParameter("High Rho");

		//YLSA
		for (int i = 0; i < NUM_DETECTORS; i++) {
			idLeakageCurrent[i] = vmeMap.eventParameter(8, ADC_BASE[3], i, 0);
			hLeakageCurrent[i] =
				new Histogram(
					"Leakage " + i,
					HIST_1D_INT,
					ADC_CHANNELS,
					"Leakage Current of Detector " + i,
					"500 channels/uA",
					"Counts");
			gLeakageCurrent[i] = new Gate("Leakage " + i, hLeakageCurrent[i]);
			mLeakageCurrent[i] = new Monitor("Leakage " + i, this);
			sYLSArate[i] = new Scaler("YLSA hits " + i, 11 + i);
			mYLSArate[i] = new Monitor("YLSA rate " + i, sYLSArate[i]);
			for (int j = 0; j < STRIPS_PER_DETECTOR; j++) {
				//eventParameter(slot, base address, channel, threshold channel)
				idEnergies[i][j] =
					vmeMap.eventParameter(
						whichADCslot(i),
						whichADCaddress(i),
						whichADCchannel(i, j),
						thresholds[i][j]);
				hEnergies[i][j] =
					new Histogram(
						"E_D" + i + "_S" + j,
						HIST_1D_INT,
						ADC_CHANNELS,
						"Detector " + i + ", Strip " + j);
			}
			for (int j = 0; j < STRIPS_PER_DETECTOR; j++) {
				//eventParameter(slot, base address, channel, threshold channel)
				idTimes[i][j] =
					vmeMap.eventParameter(
						whichTDCslot(i),
						whichTDCaddress(i),
						whichTDCchannel(i, j),
						TIME_THRESHOLDS);
				hTimes[i][j] =
					new Histogram(
						"T_D" + i + "_S" + j,
						HIST_1D_INT,
						ADC_CHANNELS,
						"Detector " + i + ", Strip " + j + " time");
			}
		}

		System.err.println("# Parameters: " + getEventSize());
		System.err.println("ADC channels: " + ADC_CHANNELS);
		System.err.println(
			"2d channels: "
				+ TWO_D_CHANNELS
				+ ", compression factor: "
				+ TWO_D_FACTOR);
		System.err.println(
			"compressed channels: "
				+ COMPRESSED_CHANNELS
				+ ", compression factor: "
				+ COMPRESS_FACTOR);

		hMultiplicity =
			new Histogram(
				"Multiplicity",
				HIST_1D_INT,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Multiplicity of Energy and Time Hits");
		hHits =
			new Histogram(
				"Hits",
				HIST_1D_INT,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Hits over ADC and TDC thresholds",
				STRIPS_PER_DETECTOR + "*Det+Strip",
				"Counts");
		hAngDist =
			new Histogram(
				"DecayAngDist",
				HIST_1D_INT,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Angular Distribution of Decays of gated State",
				STRIPS_PER_DETECTOR + "*Det+Strip",
				"Counts");
		hInterHits =
			new Histogram(
				"InterHits",
				HIST_1D_INT,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Inter-Strip hits",
				STRIPS_PER_DETECTOR + "*Det+Strip",
				"Counts");
		hTimeHits =
			new Histogram(
				"Time Hits",
				HIST_1D_INT,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Hits over TDC threshold",
				"Strip",
				"Counts");
		hEnergyHits =
			new Histogram(
				"Energy Hits",
				HIST_1D_INT,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Hits over ADC threshold",
				"Strip",
				"Counts");
		hTvsEhits =
			new Histogram(
				"T vs E hits",
				HIST_2D_INT,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Time hits vs Energy hits",
				"E hits",
				"T hits");
		hTvsStrip =
			new Histogram(
				"TvsStrip",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Time vs. Strip, All Detectors, multiplicity one",
				"Time",
				STRIPS_PER_DETECTOR + "*Det+Strip");
		hEvsStripGA =
			new Histogram(
				"EvsStripGA",
				HIST_2D_INT,
				200,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Energy vs. Strip, All Detectors, Gain Adjusted",
				"20*Edep [MeV]",
				STRIPS_PER_DETECTOR + "*Det+Strip");
		hTvsStripGA =
			new Histogram(
				"TvsStripGA",
				HIST_2D_INT,
				TWO_D_CHANNELS,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Time vs. Strip, All Detectors, Gain Adjusted",
				"Time",
				STRIPS_PER_DETECTOR + "*Det+Strip");
		hEvsStripBroad =
			new Histogram(
				"EvsSbroad",
				HIST_2D_INT,
				200,
				STRIPS_PER_DETECTOR + 1,
				"Strip vs. Energy Deposited",
				"20*Edep [MeV]",
				"Strip");
		hEsilVsFP =
			new Histogram(
				"EsilVsFP",
				HIST_2D_INT,
				TWO_D_HIRES,
				200,
				"E (YLSA) vs. Focal Plane Position",
				"QBrho [channels]",
				"20*E[MeV]");
		gEsilVsFP = new Gate("EsilVsFP", hEsilVsFP);
		hEvsStripState =
			new Histogram(
				"EvsSstate",
				HIST_2D_INT,
				200,
				STRIPS_PER_DETECTOR + 1,
				"Strip vs. Energy Deposited",
				"20*Edep [MeV]",
				"Strip");
		hEvsStripDecay =
			new Histogram(
				"EvsSdecay",
				HIST_2D_INT,
				200,
				STRIPS_PER_DETECTOR + 1,
				"Strip vs. Energy Deposited, gated on FP state, TimeDecay",
				"20*Edep [MeV]",
				"Strip");
		hEvsStripDecay_bkgd =
			new Histogram(
				"EvsSdecay_bkgd",
				HIST_2D_INT,
				200,
				STRIPS_PER_DETECTOR + 1,
				"Strip vs. Energy Deposited, gated on FP state, TimeBkgd",
				"20*Edep [MeV]",
				"Strip");
		hCoincHits =
			new Histogram(
				"Coinc Hits",
				HIST_1D_INT,
				STRIPS_PER_DETECTOR,
				"Hits by strip, gated on FP state, TimeDecay, & EvsS",
				"Strip Number",
				"Counts");
		hCoincHits_bkgd = 
			new Histogram(
				"Bkgd Hits",
				HIST_1D_INT,
				STRIPS_PER_DETECTOR,
				"Background \"hits\" by strip, gated on FP state, TimeBkgd, & EvsS",
				"Strip Number",
				"Counts");
		hEvsChBroad =
			new Histogram(
				"EvsChBroad",
				HIST_2D_INT,
				200,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Channel vs. Energy",
				"20*Edep [MeV]",
				"Channel");
		hEvsChState =
			new Histogram(
				"EvsChState",
				HIST_2D_INT,
				200,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Channel vs. Energy",
				"20*Edep [MeV]",
				"Channel");
		hEvsChDecay =
			new Histogram(
				"EvsChDecay",
				HIST_2D_INT,
				200,
				NUM_DETECTORS * STRIPS_PER_DETECTOR,
				"Channel vs. Energy",
				"20*Edep [MeV]",
				"Channel");

		gEvsS = new Gate("EvsS", hEvsStripBroad);
		hEvsStripState.addGate(gEvsS);
		hEvsStripDecay.addGate(gEvsS);
		hEvsStripDecay_bkgd.addGate(gEvsS);
		hTimeGA =
			new Histogram(
				"TimeGA",
				HIST_1D_INT,
				TWO_D_CHANNELS,
				"Time, Gain Adjusted with alpha TOF subtracted");
		hTimeGAstate =
			new Histogram(
				"TimeGAstate",
				HIST_1D_INT,
				TWO_D_CHANNELS,
				"Time, Gain Adjusted with alpha TOF subtracted");
		hTimeGAdecay =
			new Histogram(
				"TimeGAdecay",
				HIST_1D_INT,
				TWO_D_CHANNELS,
				"Time, Gain Adjusted with alpha TOF subtracted");
		gTimeBroad = new Gate("TimeBroad", hTimeGA);
		//gates on selected TDC channels
		gTimeState = new Gate("TimeState", hTimeGA);
		//gates on selected TDC channels
		gTimeDecay = new Gate("TimeDecay", hTimeGA);
		//gates on selected TDC channels
		hTimeGAstate.addGate(gTimeBroad);
		hTimeGAstate.addGate(gTimeState);
		hTimeGAstate.addGate(gTimeDecay);
		hTimeGAdecay.addGate(gTimeBroad);
		hTimeGAdecay.addGate(gTimeState);
		hTimeGAdecay.addGate(gTimeDecay);
		gTimeBkgd1 = new Gate("TimeBkgd1", hTimeGAdecay);
		gTimeBkgd2 = new Gate("TimeBkgd2", hTimeGAdecay);
		hTimeGAstate.addGate(gTimeBkgd1);
		hTimeGA.addGate(gTimeBkgd1);
		hTimeGAstate.addGate(gTimeBkgd2);
		hTimeGA.addGate(gTimeBkgd2);
		
		for (int i=0; i<this.STRIPS_PER_DETECTOR; i++) {
			hCoincSpectrum[i] = new Histogram("FPcoinc "+i, HIST_1D_INT,
				this.ADC_CHANNELS, "Position New coincidence, strip "+i);
			hBkgdSpectrum[i] = new Histogram("FPbkgd "+i, HIST_1D_INT,
				this.ADC_CHANNELS, "Position New background, strip "+i);
		}


		// gates 1d
		gCthd = new Gate("Counts", hCthd);
		gPeak = new Gate("Peak", hFrntNewGAll);
		hFrntGTime.addGate(gPeak);
		//gates  2d
		gSntrCthd = new Gate("Ca-Sc", hSntrCthd);
		//gate on Scintillator Cathode
		gCthdAnde = new Gate("Ca-An", hCthdAnde); //gate on Anode Cathode
		gFrntSntr = new Gate("Fw-Sc", hFrntSntr); //gate on Front Scintillator
		gFrntCthd = new Gate("Fw-Ca", hFrntCthd); //gate on Front Cathode
		gFrntRear = new Gate("Fw-Rw", hFrntPRearP); //gate on Front Rear
		gFRexclude = new Gate("FRexclude", hFrntPRearP);
		gAndeSntr = new Gate("An-Sc", hSntrAnde);
		gAndeFrnt = new Gate("An-Fw", hFrntAnde);
		hFrntSntrGSC.addGate(gFrntSntr);
		hFrntCthdGSC.addGate(gFrntCthd);
		hSntrCthdGFC.addGate(gSntrCthd);
		hFrntSntrGFC.addGate(gFrntSntr);
		hSntrCthdGFS.addGate(gSntrCthd);
		hFrntCthdGFS.addGate(gFrntCthd);
		hFrntRearGCSF.addGate(gFrntRear);

		//scalers
		sBic = new Scaler("BIC", 0);
		sClck = new Scaler("Clock", 1);
		sEvntRaw = new Scaler("Event Raw", 2);
		sEvntAccpt = new Scaler("Event Accept", 3);
		sScint = new Scaler("Scintillator", 4);
		sCathode = new Scaler("Cathode", 5);
		sFCLR = new Scaler("FCLR", 6);

		setupCorrections();
	}

	//Utility methods for mapping strips to ADC channels
	/**
	 * Returns which adc unit given which detector.
	 */
	private int whichADCaddress(int detector) {
		if (detector == 0) { //Detector 0
			return ADC_BASE[3];
		} else if (detector == 1 || detector == 2) { //Detectors 1 and 2
			return ADC_BASE[1];
		} else { //detectors 3 and 4
			return ADC_BASE[2];
		}
	}
	/**
	 * @return which slot in the VME crate contains the ADC for this detector
	 */
	private int whichADCslot(int detector) {
		if (detector == 0) {
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
	private int whichTDCaddress(int detector) {
		if (detector == 0) { //Detector 0
			return TDC_BASE[0];
		} else if (detector == 1 || detector == 2) { //Detectors 1  and 2
			return TDC_BASE[1];
		} else { //detectors 3 and 4
			return TDC_BASE[2];
		}
	}
	/**
	 * @return which slot in the VME crate contains the TDC for this detector
	 */
	private int whichTDCslot(int detector) {
		if (detector == 0) {
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
	private int whichADCchannel(int detector, int strip) {
		if (detector == 0 || detector == 1 || detector == 3) { //top half
			return strip + 16;
		} else { //detectors 2 and 4, bottom half
			return strip;
		}
	}
	/**
	 * Returns which channel in the tdc given which detector and strip.
	 */
	private int whichTDCchannel(int detector, int strip) {
		if (detector == 0 || detector == 1 || detector == 3) { //top half
			return strip + 16;
		} else { //detetors 2 and 4, bottom half
			return strip;
		}
	}

	int[][] thresholds = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
	/**
	 * Sets ADC thresholds to be a certain energy in keV.
	 * We have to look up gains in the array calibration,
	 * so retrieveCalibration() is assumed to have been
	 * called.
	 */
	private void setADCthresholdsToE() throws IOException {
		/* The following lines set up a text file reader
		 * for a file sitting in the same directory as this
		 * code. */
		String thresholdFile = "threshold_keV.txt";
		InputStreamReader isr =
			new InputStreamReader(
				getClass().getResourceAsStream(thresholdFile));
		/* */
		StreamTokenizer grabber = new StreamTokenizer(isr);
		grabber.commentChar('#');
		grabber.nextToken();
		double E_in_keV = grabber.nval;
		double E_in_MeV = E_in_keV / 1000.0;
		System.out.println("Setting thresholds to " + E_in_keV + " keV.");
		for (int strip = 0; strip < STRIPS_PER_DETECTOR; strip++) {
			for (int det = 0; det < NUM_DETECTORS; det++) {
				double gainFactor = ac.getEnergyGain(det, strip);
				if (gainFactor == 0.0) {
					thresholds[det][strip] = 200;
					//arbitrary threshold that seems to work
				} else {
					double E_at_400 = ac.getEnergyDeposited(det, strip, 400);
					double E_at_500 = ac.getEnergyDeposited(det, strip, 500);
					double channel =
						400
							+ 100 / (E_at_500 - E_at_400) * (E_in_MeV - E_at_400);
					thresholds[det][strip] = (int) Math.round(channel);
				}
			}
		}
	}

	private void retrieveCalibration()
		throws IOException, ClassNotFoundException {
		String calibrationFile = "calibration.obj";
		System.out.println(
			"Attempting to retrieve calibration from: " + calibrationFile);
		ObjectInputStream ois =
			new ObjectInputStream(
				getClass().getResourceAsStream(calibrationFile));
		ac = (ArrayCalibration) ois.readObject();
		System.out.println(
			"Calibration retrieved.  String representation = " + ac);
		ois.close();
	}

	double[][] correctionTable; //table for FP corrections
	int NUM_PEAKS; //number of peaks in correction table
	int NUM_COEFF; //number of coefficients per peak in table
	private void setupCorrections() throws IOException {
		String infile = "FPcoefficients.txt";
		InputStreamReader isr =
			new InputStreamReader(getClass().getResourceAsStream(infile));
		StreamTokenizer st = new StreamTokenizer(isr);
		st.nextToken();
		NUM_PEAKS = (int) Math.round(st.nval);
		st.nextToken();
		NUM_COEFF = (int) Math.round(st.nval);
		correctionTable = new double[NUM_PEAKS][NUM_COEFF];
		for (int peak = 0; peak < NUM_PEAKS; peak++) {
			for (int coeff = 0; coeff < NUM_COEFF; coeff++) {
				st.nextToken(); //get coefficient
				correctionTable[peak][coeff] = st.nval;
				System.out.print(correctionTable[peak][coeff] + " ");
			}
			System.out.println();
		}
		System.out.println("Set up focal-plane corrections table.");
	}

	/**
	 * Interpolate the evaluated correction for values between given peaks,
	 * and use the end-peak corrections for extrapolations.
	 */
	private int getCorrectedPosition(int FPchan, int theta) {
		double[] coeffs1 = new double[NUM_COEFF - 1];
		double[] coeffs2 = new double[NUM_COEFF - 1];
		double x1 = 0;
		double x2 = 0;
		if (FPchan <= correctionTable[0][0]) {
			System.arraycopy(correctionTable[0], 1, coeffs1, 0, NUM_COEFF - 1);
			System.arraycopy(correctionTable[0], 1, coeffs2, 0, NUM_COEFF - 1);
			x1 = correctionTable[0][0];
			x2 = correctionTable[0][0];
		}
		if (FPchan > correctionTable[NUM_PEAKS - 1][0]) {
			System.arraycopy(
				correctionTable[NUM_PEAKS - 1],
				1,
				coeffs1,
				0,
				NUM_COEFF - 1);
			System.arraycopy(
				correctionTable[NUM_PEAKS - 1],
				1,
				coeffs2,
				0,
				NUM_COEFF - 1);
			x1 = correctionTable[NUM_PEAKS - 1][0];
			x2 = correctionTable[NUM_PEAKS - 1][0];
		}
		for (int peak = 0; peak < NUM_PEAKS - 1; peak++) {
			if (FPchan > correctionTable[peak][0]
				&& FPchan <= correctionTable[peak + 1][0]) {
				System.arraycopy(
					correctionTable[peak],
					1,
					coeffs1,
					0,
					NUM_COEFF - 1);
				System.arraycopy(
					correctionTable[peak + 1],
					1,
					coeffs2,
					0,
					NUM_COEFF - 1);
				x1 = correctionTable[peak][0];
				x2 = correctionTable[peak + 1][0];
			}
		}
		double y;
		double y1 = FPchan;
		for (int i = 0; i < NUM_COEFF - 1; i++) {
			y1 -= coeffs1[i] * Math.pow(theta, i + 1);
		}
		if (x2 > x1) {
			double y2 = FPchan;
			for (int i = 0; i < NUM_COEFF - 1; i++) {
				y2 -= coeffs2[i] * Math.pow(theta, i + 1);
			}
			y = y1 + (y2 - y1) * ((FPchan - x1) / (x2 - x1));
		} else { //x2==x1
			y = y1;
		}
		return (int) Math.round(y);
	}

	/** Called every time Jam has an event it wants to sort.
	 * @param dataEvent contains all parameters for one event
	 * @throws Exception so Jam can handle exceptions
	 */
	public void sort(int[] dataEvent) throws Exception {
		double phiOffset = dp_phiOffset.getValue();
		double thetaOffset = dp_thetaOffset.getValue();
		double cathMiddle = dp_cathMiddle.getValue();
		double scintMiddle = dp_scintMiddle.getValue();
		double anodeMiddle = dp_anodeMiddle.getValue();
		double cathThetaSlope = dp_CathThetaSlope.getValue();
		double scintThetaSlope = dp_ScintThetaSlope.getValue();
		double andeThetaSlope = dp_AndeThetaSlope.getValue();
		double chLow = dp_lowChannel.getValue();
		double chHigh = dp_highChannel.getValue();
		double meanCh = (chLow + chHigh) / 2;
		double rhoLow = dp_lowRho.getValue(); //cm
		double rhoHigh = dp_highRho.getValue(); //cm
		double rhoSlope = (rhoHigh - rhoLow) / (chHigh - chLow); //cm/channel
		double meanRho = (rhoHigh + rhoLow) / 2;
		//unpack data into convenient names
		int eCthd = dataEvent[idCthd];
		int eAnde = dataEvent[idAnde];
		int eSntr1 = dataEvent[idScintR];
		int eSntr2 = dataEvent[idScintL];
		int eFPsn = dataEvent[idFrntPsn];
		int eRPsn = dataEvent[idRearPsn];
		int eFHgh = dataEvent[idFrntHgh];
		int eRHgh = dataEvent[idRearHgh];
		int eYF = dataEvent[idYFrnt];
		int eYR = dataEvent[idYRear];

		//proper way to add for 2 phototubes at the ends of scintillating rod
		//see Knoll
		int eSntr = (int) Math.round(Math.sqrt(eSntr1 * eSntr2));
		//theta_calc used for calculations
		int theta_calc = (int) Math.round(eRPsn - eFPsn - thetaOffset);
		//theta used for display
		int theta =
			(int) Math.round(eRPsn - eFPsn - thetaOffset + ADC_CHANNELS / 2);
		int phi = (int) Math.round(eYR - eYF - phiOffset + ADC_CHANNELS / 2);
		double delta = (eFPsn - meanCh) * rhoSlope / meanRho;
		int eSntrNew =
			(int) Math.round(
				(ADC_CHANNELS / 2 - scintMiddle + eSntr)
					- (theta_calc) / scintThetaSlope);
		int eCthdNew =
			(int) Math.round(
				(ADC_CHANNELS / 2 - cathMiddle + eCthd)
					- (theta_calc) / cathThetaSlope);
		int eAndeNew =
			(int) Math.round(
				(ADC_CHANNELS / 2 - anodeMiddle + eAnde)
					- (theta_calc) / andeThetaSlope);

		int ePosnNew = getCorrectedPosition(eFPsn, theta_calc);
		int ecPosnNew_hr = ePosnNew >> TWO_D_HIRES_FACTOR;

		int ecFPsn = eFPsn >> TWO_D_FACTOR;
		int ecRPsn = eRPsn >> TWO_D_FACTOR;
		int ecFHgh = eFHgh >> TWO_D_FACTOR;
		int ecRHgh = eRHgh >> TWO_D_FACTOR;
		int ecSntr = eSntr >> TWO_D_FACTOR;
		int ecCthd = eCthd >> TWO_D_FACTOR;
		int ecAnde = eAnde >> TWO_D_FACTOR;
		int ecYF = eYF >> TWO_D_FACTOR;
		int ecYR = eYR >> TWO_D_FACTOR;
		int ecSntrNew = eSntrNew >> TWO_D_FACTOR;
		int ecCthdNew = eCthdNew >> TWO_D_FACTOR;
		int ecAndeNew = eAndeNew >> TWO_D_FACTOR;

		int c_theta = theta >> TWO_D_HIRES_FACTOR;
		int c_phi = phi >> TWO_D_HIRES_FACTOR;

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
		hFrntY.inc(eYF);
		hRearY.inc(eYR);
		hRYvsFY.inc(ecYF, ecYR);
		hYvsPsn.inc(ecFPsn, ecYF);
		hFrntPH.inc(ecFPsn, ecFHgh);
		hRearPH.inc(ecRPsn, ecRHgh);
		hCthdAnde.inc(ecCthd, ecAnde);
		hSntrCthd.inc(ecSntr, ecCthd);
		hSntrAnde.inc(ecAnde, ecSntr);
		hFrntCthd.inc(ecFPsn, ecCthd);
		hFrntAnde.inc(ecFPsn, ecAnde);
		hFrntSntr.inc(ecFPsn, ecSntr);
		hFrntPRearP.inc(ecFPsn, ecRPsn);

		int[][] eEnergies = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int[][] eTimes = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int[][] eEnergiesGA = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
		int[][] eTimesGA = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

		boolean bSC = gSntrCthd.inGate(ecSntr, ecCthd);
		boolean bCA = gCthdAnde.inGate(ecCthd, ecAnde);
		boolean bFC = gFrntCthd.inGate(ecFPsn, ecCthd);
		boolean bFS = gFrntSntr.inGate(ecFPsn, ecSntr);
		boolean bAF = gAndeFrnt.inGate(ecFPsn, ecAnde);
		boolean bAS = gAndeSntr.inGate(ecSntr, ecAnde);

		boolean bRYvsFY = gRYvsFY.inGate(ecYF, ecYR);
		boolean bSCn = gCathNewVsScintNew.inGate(ecSntrNew, ecCthdNew);
		boolean bCAn = gAndeNewVsCathNew.inGate(ecCthdNew, ecAndeNew);
		boolean bFCn = gCathNewVsPosn.inGate(ecFPsn, ecCthdNew);
		boolean bFSn = gScintNewVsPosn.inGate(ecFPsn, ecSntrNew);
		boolean bAFn = gAndeNewVsPosn.inGate(ecFPsn, ecAndeNew);
		boolean bASn = gAndeNewVsScintNew.inGate(ecSntrNew, ecAndeNew);
		boolean bYP = gYvsPsn.inGate(ecFPsn, ecYF);

		int ecFPsn_hr = eFPsn >> TWO_D_HIRES_FACTOR;
		boolean bTP = gThetaVsPsnCorr.inGate(ecFPsn_hr, c_theta);

		boolean bPID = bSCn && bFCn && bFSn && bCAn && bAFn && bASn && bYP;
		boolean bFR =
			gFrntRear.inGate(ecFPsn, ecRPsn)
				&& !gFRexclude.inGate(ecFPsn, ecRPsn);
		boolean bAlmostGood = bPID && bFR;
		boolean bGood = bAlmostGood && bTP;
		//boolean bState = bGood && gPeak.inGate(ePosnNew);

		if (bRYvsFY) {
			int ecSntr_hr = eSntr >> TWO_D_FACTOR;
			int ecCthd_hr = eCthd >> TWO_D_FACTOR;
			int ecAnde_hr = eAnde >> TWO_D_FACTOR;
			CathTheta.inc(ecCthd_hr, c_theta);
			ScintTheta.inc(ecSntr_hr, c_theta);
			AndeTheta.inc(ecAnde_hr, c_theta);
			PosnTheta.inc(ecFPsn_hr, c_theta);
			CathPhi.inc(ecCthd_hr, c_phi);
			ScintPhi.inc(ecSntr_hr, c_phi);
			AndePhi.inc(ecAnde_hr, c_phi);

			hScintNewVsPosn.inc(ecFPsn, ecSntrNew);
			hCathNewVsPosn.inc(ecFPsn, ecCthdNew);
			hCathNewVsScintNew.inc(ecSntrNew, ecCthdNew);
			hAndeNewVsCathNew.inc(ecCthdNew, ecAndeNew);
			hAndeNewVsScintNew.inc(ecSntrNew, ecAndeNew);
			hAndeNewVsPosn.inc(ecFPsn, ecAndeNew);

			if (bFCn)
				hCathNewVsScintNew_gFC.inc(ecSntrNew, ecCthdNew);
			if (bFSn)
				hCathNewVsScintNew_gFS.inc(ecSntrNew, ecCthdNew);
			if (bFCn && bFSn && bSCn) {
				hAndeNewVsCathNew_g3.inc(ecCthdNew, ecAndeNew);
				hAndeNewVsScintNew_g3.inc(ecSntrNew, ecAndeNew);
				hAndeNewVsPosn_g3.inc(ecFPsn, ecAndeNew);
			}
		}

		//System.out.println("Declared silicon data storage: event "+event);
		int multiplicity = 0;
		for (int i = 0; i < NUM_DETECTORS; i++) {
			for (int j = 0; j < STRIPS_PER_DETECTOR; j++) {
				int stripBin = i * STRIPS_PER_DETECTOR + j;
				eEnergies[i][j] = dataEvent[idEnergies[i][j]];
				hEnergies[i][j].inc(eEnergies[i][j]);
				//hEvsStrip.inc(eEnergies[i][j]>>TWO_D_FACTOR,stripBin);
				eTimes[i][j] = dataEvent[idTimes[i][j]];
				hTimes[i][j].inc(eTimes[i][j]);
				boolean energy =
					eEnergies[i][j] > 0 && eEnergies[i][j] <= LAST_ADC_BIN;
				boolean time = eTimes[i][j] > 0;
				if (time)
					hTimeHits.inc(stripBin);
				if (energy) {
					double eDeposit =
						ac.getEnergyDeposited(i, j, eEnergies[i][j]);
					//deposited in det [MeV]
					int eDep20chPerMeV = (int)Math.floor(eDeposit * 20);
					//binned so that 0-50 keV in ch. 0, 50-100 keV in Ch. 1, etc.
					hEnergyHits.inc(stripBin);
					eEnergiesGA[i][j] =
						ac.getCalibratedEnergyChannel(i, j, eEnergies[i][j]);
					hEvsStripGA.inc(eDep20chPerMeV,stripBin);
					if (time) { //and energy since we are nexted
						hHits.inc(stripBin);
						detHit[multiplicity] = i;
						stripHit[multiplicity] = j;
						bin[multiplicity] = stripBin;
						multiplicity++;
						eTimesGA[i][j] =
							ac.getCalibratedTimeChannel(i, j, eTimes[i][j]);
						hTvsStrip.inc(eTimes[i][j] >> TWO_D_FACTOR, stripBin);
						int ecTimeGA = eTimesGA[i][j] >> TWO_D_FACTOR;
						boolean bTimeBroad =
							bGood && gTimeBroad.inGate(ecTimeGA);
						/*boolean bTimeState =
							bState && gTimeState.inGate(ecTimeGA);*/
						//boolean bEvsS = gEvsS.inGate(eDep20chPerMeV, j);
						boolean bEsilVsFP = bGood && gEsilVsFP.inGate(ecPosnNew_hr, eDep20chPerMeV);
						boolean bTimeDecay =
							bEsilVsFP && gTimeDecay.inGate(ecTimeGA);
						boolean bTimeBkgd = bEsilVsFP && (gTimeBkgd1.inGate(ecTimeGA)||
						gTimeBkgd2.inGate(ecTimeGA));
						if (bGood)
							hTimeGA.inc(ecTimeGA);
						if (bTimeBroad) {
							hEvsChBroad.inc(eDep20chPerMeV, stripBin);
							hEvsStripBroad.inc(eDep20chPerMeV, j);
							hEsilVsFP.inc(ecPosnNew_hr, eDep20chPerMeV);
						}
						if (bTimeDecay) {
							hEvsChDecay.inc(eDep20chPerMeV, stripBin);
							hEvsStripDecay.inc(eDep20chPerMeV, j);
							hCoincSpectrum[j].inc(ePosnNew);
							hFrntGTime.inc(ePosnNew);
							hcFrntGTime.inc(ePosnNew >> COMPRESS_FACTOR);
						}
						if (bTimeBkgd) {
							hEvsStripDecay_bkgd.inc(eDep20chPerMeV, j);
							hBkgdSpectrum[j].inc(ePosnNew);
							hFrntGBkgd.inc(ePosnNew);
							hcFrntGBkgd.inc(ePosnNew >> COMPRESS_FACTOR);
						}

						hTvsStripGA.inc(ecTimeGA, stripBin);
					}//end if time
				}//end if energy
			}//end strip loop
		}//end det loop
		/************
		Processing of T vs. E hits diagnostic plot
		 ************/
		for (int det = 0; det < NUM_DETECTORS; det++) {
			for (int strip = 0; strip < STRIPS_PER_DETECTOR; strip++) {
				if (eTimes[det][strip] > 0) {
					for (int Estrip = 0;
						Estrip < STRIPS_PER_DETECTOR;
						Estrip++) {
						if (eEnergies[det][Estrip] > 0
							&& eEnergies[det][Estrip] < LAST_ADC_BIN) {
							hTvsEhits.inc(
								STRIPS_PER_DETECTOR * det + Estrip,
								STRIPS_PER_DETECTOR * det + strip);
						}
					}
				}
			}
		}
		numInterHits = 0;
		if (multiplicity > 1) {
			for (int m = 0; m < multiplicity; m++) {
				for (int n = m + 1; n < multiplicity; n++) {
					if (detHit[m] == detHit[n]) {
						int diff = Math.abs(stripHit[m] - stripHit[n]);
						if (diff == 1) {
							interDetHit[numInterHits] = detHit[m];
							interStripHit[numInterHits] = stripHit[m];
							interBin[numInterHits] = bin[m];
							numInterHits++;
						}
					}
				}
			}
		}

		for (int i = 0; i < numInterHits; i++){
			hInterHits.inc(interBin[i]);
		}
		hMultiplicity.inc(multiplicity);

		if (bSC && bFR) { // gate on Scintillator vs Cathode
			hFrntSntrGSC.inc(ecFPsn, ecSntr);
			hFrntCthdGSC.inc(ecFPsn, ecCthd);
		}
		if (bFC && bFR) { // gate on Front Wire Position vs Cathode
			hSntrCthdGFC.inc(ecSntr, ecCthd);
			hFrntSntrGFC.inc(ecFPsn, ecSntr);
		}
		if (bFS && bFR) { // gate on Front Wire Position vs Scintillator
			hSntrCthdGFS.inc(ecSntr, ecCthd);
			hFrntCthdGFS.inc(ecFPsn, ecCthd);
		}
		if (bPID) { // gated on all 3 gate above
			//writeEvent(dataEvent);
			hFrntGCSF.inc(eFPsn);
			hRearGCSF.inc(eRPsn);
			hFrntRearGCSF.inc(ecFPsn, ecRPsn);
			hThetaVsFrontGPID.inc(ecFPsn_hr, c_theta);
			for (int i = 0; i < hPosn_slice.length; i++) {
				int ll = thetaSliceLowerLimits[i];
				int ul = ll + 19;
				if (theta_calc >= ll && theta_calc <= ul) {
					hPosn_slice[i].inc(eFPsn);
				}
			}
			if (bAlmostGood)
				hThetaVsFront_corr.inc(ecPosnNew_hr, c_theta);
			if (bGood) {
				writeEvent(dataEvent);
				hFrntGAll.inc(eFPsn);
				hRearGAll.inc(eRPsn);
				hcFrntGAll.inc(eFPsn >> COMPRESS_FACTOR);
				hcRearGAll.inc(eRPsn >> COMPRESS_FACTOR);
				hFrntNewGAll.inc(ePosnNew);
				hcFrntNewGAll.inc(ePosnNew >> COMPRESS_FACTOR);
				hYvsPsnGPID.inc(ecFPsn, eYF >> TWO_D_FACTOR);
				hPhiVsFrontGPID.inc(ecFPsn_hr, c_phi);
				hPhiVsFront_corr.inc(ecPosnNew_hr, c_phi);
			}
		}
	}
}
