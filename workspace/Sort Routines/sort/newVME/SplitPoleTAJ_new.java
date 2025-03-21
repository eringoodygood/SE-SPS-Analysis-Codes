/*
 */
package sort.newVME;
import jam.data.*;
import jam.sort.*;

/** Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>, which
 * was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right way; also 
 * added gate to cathAnde
 *
 * @author Dale Visser
 * @since 26 July 2001
 */
public class SplitPoleTAJ_new extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000};
static final int [] TDC_BASE = {0x30000000,0x30020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
	static final int THRESHOLDS = 128;//ADC lower threshold in channels
	static final int TIME_THRESHOLDS = 30;//TDC lower threshold in channels
    static final int LAST_ADC_BIN = 3840;
	static final int TIME_RANGE =1200;//ns

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

    
    // ungated spectra
    Histogram hCthd, hSntr1,hSntr2,hSntrSum, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;     // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hSntrCthd, hSntr2Cthd, hFrntCthd, hFrntSntr, hFrntSntr2, hFrntPRearP;
    Histogram hFrntY, hRearY, hYvsPsn, hYvsPsnGPID;
    Histogram hSntr1Sntr2;
    Histogram hFrntYRearY;
    
    // new way to measure front and rear positions, using the wire signal as the start and the 
    // left and right signals as the stop for 4 new TACs
    Histogram hFrontLeft,hFrontRight,hRearLeft,hRearRight,hFrontSum,hRearSum;
	Histogram hFrontNew,hRearNew;
	Histogram hFrontLvsR, hRearLvsR;
	Histogram hFrontTheta,hRearTheta,hFCTheta,hFC;
	Histogram hFrontNewvsOld;
	
		
	// new detector, formerly "TeDector", now TAJ for Ted-Anuj-Jac
	// more parameters than with old split pole detector
	Histogram hFrontFrontPH, hFrontMiddlePH,hFrontBackPH;
	Histogram hRearFrontPH, hRearMiddlePH,hRearBackPH;

    //gated spectra
    Histogram hFrntSntrGSC, hFrntSntr2GS2C, hFrntCthdGSC, hFrntCthdGS2C;//gate by scintillator cathode
    Histogram hSntrCthdGFC, hSntr2CthdGFC, hFrntSntrGFC, hFrntSntr2GFC;//gate by Front wire Cathode
    Histogram hSntrCthdGFS, hSntr2CthdGFS2, hFrntCthdGFS, hFrntCthdGFS2;//gate by Front wire Scintillator
    Histogram hFrntGCSF, hFrntGCS2F, hRearGCSF, hRearGCS2F, hFrntRearGCSF, hFrntRearGCS2F, hFrntGAll, hFrntGAll2, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed & time
    Histogram hcFrntGTime, hFrntGTime;//front and rear wire gated on All compressed & time
    Histogram hSntr1Sntr2GSC, hSntr1Sntr2GFC, hSntr1Sntr2GFS;
    Histogram hSntrCthdGS12, hFrntCthdGS12, hFrntSntrGS12;
    Histogram hCathodeTheta;
    
    /*
     * 1D gates
     */
    Gate  gCthd, gPeak; 
    Gate gFSum, gRSum;
    /*
     * 2D gates
     */
    Gate gSntrCthd, gSntr2Cthd, gFrntSntr, gFrntSntr2, gFrntCthd, gFrntRear, gCthdAnde, gXY, gSntr1Sntr2, gFYRY;
    Gate gFrontLvsR, gRearLvsR;
	
    /*
     * Scalers and monitors
     */
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Scaler sFCLR;//number of FCLR's that went to ADC's
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors
    Monitor mFCLR;

    //id numbers for the signals;
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh,
    idRearHgh, idYFrnt, idYRear;
    //Histogram hDebug;
    

    int idFWbias, idRWbias, idBCIRange;
    Histogram hFWbias, hRWbias, hBCIRange;
    int idFrontLeft,idFrontRight,idRearLeft,idRearRight;
	int idFrontMiddlePH,idFrontBackPH;
	int idRearFrontPH,idRearBackPH;
	
	//parameters
	DataParameter pXTheta, pThetaOffset, pCTheta; 
	//parameters added from Corr
	DataParameter pXTheta_p, pThetaOffset_p;
	DataParameter pXTheta_d, pThetaOffset_d;
	DataParameter pXTheta_t, pThetaOffset_t;
	DataParameter pXTheta2, pXTheta3;

    public void initialize() throws Exception {
        vmeMap.setScalerInterval(3);
		for (int i=0; i < TDC_BASE.length; i++){
			vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
		}
		/**Focal Plane Detector      (slot, base address, channel, threshold channel)*/
		int idDummy=vmeMap.eventParameter(2, ADC_BASE[0], 0, 0);
        idAnde=vmeMap.eventParameter(2, ADC_BASE[0], 			1, THRESHOLDS);
        idScintR=vmeMap.eventParameter(2, ADC_BASE[0], 		2, THRESHOLDS);
        idScintL=vmeMap.eventParameter(2, ADC_BASE[0], 		3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(2, ADC_BASE[0], 		4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(2, ADC_BASE[0], 	5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(2, ADC_BASE[0], 	6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(2, ADC_BASE[0], 	7, THRESHOLDS);
        idYFrnt=vmeMap.eventParameter(2, ADC_BASE[0], 			8, THRESHOLDS);
        idYRear=vmeMap.eventParameter(2, ADC_BASE[0], 		9, THRESHOLDS);
		idCthd=vmeMap.eventParameter(2, ADC_BASE[0], 			10, THRESHOLDS);

	idFWbias = vmeMap.eventParameter(2, ADC_BASE[0], 		12, THRESHOLDS);
	idRWbias = vmeMap.eventParameter(2, ADC_BASE[0], 		13, THRESHOLDS);
	idBCIRange = vmeMap.eventParameter(2, ADC_BASE[0], 	14, 16);
	idFrontLeft = vmeMap.eventParameter(2,ADC_BASE[0], 		16+3, THRESHOLDS);
	idFrontRight = vmeMap.eventParameter(2,ADC_BASE[0], 	16+4, THRESHOLDS);
	idRearLeft = vmeMap.eventParameter(2,ADC_BASE[0], 		16+6, THRESHOLDS);
	idRearRight = vmeMap.eventParameter(2,ADC_BASE[0], 	16+7, THRESHOLDS);

// new pulse height signals from TAJ
	idRearBackPH =vmeMap.eventParameter(2,ADC_BASE[0], 16+10, THRESHOLDS);
	idRearFrontPH =vmeMap.eventParameter(2,ADC_BASE[0], 16+11, THRESHOLDS);
	idFrontMiddlePH =vmeMap.eventParameter(2,ADC_BASE[0], 16+12, THRESHOLDS);
	idFrontBackPH =vmeMap.eventParameter(2,ADC_BASE[0], 16+13, THRESHOLDS);
	

        hCthd      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS,
        "Cathode Raw ");
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
		hFrontLeft    =new Histogram("FrontLeft    ", HIST_1D_INT, ADC_CHANNELS,
		"Front Wire Left TAC");
		hFrontRight    =new Histogram("FrontRight    ", HIST_1D_INT, ADC_CHANNELS,
		"Front Wire Right TAC");
		hFrontLvsR   =new Histogram("FrontLvsR  ", HIST_2D_INT,  2*TWO_D_CHANNELS, "Front Right vs Left","Left","Right");
		hFrontNewvsOld =new Histogram("FrontNewvsOld", HIST_2D_INT, 2*TWO_D_CHANNELS,"Front TDC vs TAC","TAC","TDC");
		hFrontSum    =new Histogram("FrontPosnSum    ", HIST_1D_INT, 2*ADC_CHANNELS,
		"Front Wire Position - sum of left TAC plus right TAC");
		hFrontNew    =new Histogram("FrontPosnNew    ", HIST_1D_INT, 2*ADC_CHANNELS,
		"Front Wire Position - two TACs");
		hRearLeft    =new Histogram("RearLeft    ", HIST_1D_INT, ADC_CHANNELS,
		"Rear Wire Left TAC");
		hRearRight    =new Histogram("RearRight    ", HIST_1D_INT, ADC_CHANNELS,
		"Rear Wire Right TAC");
		hRearLvsR   =new Histogram("RearLvsR  ", HIST_2D_INT,  2*TWO_D_CHANNELS, "Rear Right vs Left","Left","Right");
		hRearSum    =new Histogram("RearPosnSum    ", HIST_1D_INT, 2*ADC_CHANNELS,
		"Rear Wire Position - sum of left TAC plus right TAC");
		hRearNew    =new Histogram("RearPosnNew    ", HIST_1D_INT, 2*ADC_CHANNELS,
		"Rear Wire Position - two TACs");
		hFrontFrontPH    =new Histogram("FrontFrontPH   ", HIST_1D_INT, ADC_CHANNELS,
        "Front wire in the front wire assembly Pulse Height");
        hFrontMiddlePH    =new Histogram("FrontMiddlePH    ", HIST_1D_INT, ADC_CHANNELS,
        "Middle wire in the front wire assembly Pulse Height");
		hFrontBackPH    =new Histogram("FrontBackPH   ", HIST_1D_INT, ADC_CHANNELS,
		"Back wire in the rear wire assembly Pulse Height");
		hRearFrontPH    =new Histogram("RearFrontPH   ", HIST_1D_INT, ADC_CHANNELS,
		"Front wire in the front wire assembly Pulse Height");
		hRearMiddlePH    =new Histogram("RearMiddlePH    ", HIST_1D_INT, ADC_CHANNELS,
		"Middle wire in the front wire assembly Pulse Height");
		hRearBackPH    =new Histogram("RearBackPH   ", HIST_1D_INT, ADC_CHANNELS,
		"Back wire in the rear wire assembly Pulse Height");
        hRWbias    =new Histogram("RearBias    ", HIST_1D_INT, ADC_CHANNELS,
        "Rear Wire Bias");
        hFWbias    =new Histogram("FrontBias    ", HIST_1D_INT, ADC_CHANNELS,
        "Front Wire Bias");
        hBCIRange    =new Histogram("BCIRange", HIST_1D_INT, ADC_CHANNELS,
        "BCI Full Scale Range - about 330 mV/position");
        hFrntY = new Histogram("Front Y", HIST_1D_INT, ADC_CHANNELS,
        "Y (vertical) Position at Front Wire");
        hRearY = new Histogram("Rear Y", HIST_1D_INT, ADC_CHANNELS,
        "Y (vertical) Position at Rear Wire");
        hFrntPH      =new Histogram("FrontPvsHeight", HIST_2D_INT,
        TWO_D_CHANNELS, "Pulse Height of FrontFront wire vs Front Position","Front Position",
        "Pulse Height");
        hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,
        TWO_D_CHANNELS, "Pulse Height of RearMiddle wire vs Rear Position","Rear Position",
        "Pulse Height");
        hYvsPsn = new Histogram("Y vs Position", HIST_2D_INT, TWO_D_CHANNELS,
        "Front Y vs. Front Wire Position (X)", "Position", "Y");
        hYvsPsnGPID = new Histogram("YvsPosnPID", HIST_2D_INT, TWO_D_CHANNELS,
        "Front Y vs. Front Wire Position (X) Gated on PID", "Position", "Y");
        hSntrCthd   =new Histogram("ScintCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator","Scintillator","Cathode");
		hSntr2Cthd   =new Histogram("Scint2Cathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator 2","Scintillator 2","Cathode");
        hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position","Front Position","Cathode");
        hFrntSntr   =new Histogram("FrontScint ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position","Front Position","Scintillator");
		hFrntSntr2   =new Histogram("FrontScint2 ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator 2 vs Front Position","Front Position","Scintillator 2");
        hFrntPRearP =new Histogram("FrontRear  ", HIST_2D_INT,  2*TWO_D_CHANNELS, "Rear Position vs Front Position","Front Position","Rear Position");
        hFrntYRearY =new Histogram("FrontY_RearY  ", HIST_2D_INT,  TWO_D_CHANNELS, "Rear Y Position vs Y Front Position","Front Y Position","Rear Y Position");
        //ScintCathode Gated on other
        hSntrCthdGFC=new Histogram("ScintCathodeGFC", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Scintillator - FwCa gate", "Scintillator","Cathode");
		hSntr2CthdGFC=new Histogram("Scint2CathodeGFC", HIST_2D_INT,  TWO_D_CHANNELS, 
		"Cathode vs Scintillator 2 - FwCa gate", "Scintillator 2","Cathode");
		hSntrCthdGFS=new Histogram("ScintCathodeGFS", HIST_2D_INT,  TWO_D_CHANNELS, 
		"Cathode vs Scintillator - FwSc gate","Scintillator","Cathode");
        hSntr2CthdGFS2=new Histogram("Scint2CathodeGFS2", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Scintillator2 - FwSc2 gate","Scintillator 2","Cathode");
        //FrontCathode Gated on other
        hFrntCthdGSC=new Histogram("FrontCathodeGSC", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Front Position - ScCa gate","Front Position","Cathode");
        hFrntCthdGFS=new Histogram("FrontCathodeGFS ", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Cathode vs Front Position - FwSc gate ","Front Position","Cathode");
        //FrontScint Gated on other
        hFrntSntrGSC=new Histogram("FrontScintGSC ", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Scintillator vs Front Position - ScCa gate","Front Position", "Scintillator");
		hFrntSntr2GS2C=new Histogram("FrontScint2GS2C ", HIST_2D_INT,  TWO_D_CHANNELS, 
		"Scintillator2 vs Front Position - Sc2Ca gate","Front Position", "Scintillator 2");
        hFrntSntrGFC=new Histogram("FrontScintGFC", HIST_2D_INT,  TWO_D_CHANNELS, 
        "Scintillator vs Front Position - FwCa gate","Front Position", "Scintillator");
		hFrntSntr2GFC=new Histogram("FrontScint2GFC", HIST_2D_INT,  TWO_D_CHANNELS, 
		"Scintillator 2 vs Front Position - FwCa gate","Front Position", "Scintillator 2");
        //Scint1Scint2 Gated on other
/*        hSntr1Sntr2GSC =new Histogram("Sntr1Sntr2GSC", HIST_2D_INT, TWO_D_CHANNELS, 
        "Scint 2 vs Scint 1--Gated on ScCa", "Scint  1", "Scint 2");
        hSntr1Sntr2GFC =new Histogram("Sntr1Sntr2GFC", HIST_2D_INT, TWO_D_CHANNELS, 
         "Scint 2 vs Scint 1--Gated on FwCa", "Scint  1", "Scint 2");
        hSntr1Sntr2GFS =new Histogram("Sntr1Sntr2GFS", HIST_2D_INT, TWO_D_CHANNELS, 
         "Scint 2 vs Scint 1--Gated on FwSc", "Scint  1", "Scint 2");
        hSntr1Sntr2GCA =new Histogram("Sntr1Sntr2GCA", HIST_2D_INT, TWO_D_CHANNELS, 
         "Scint 2 vs Scint 1--Gated on CaAn", "Scint  1", "Scint 2");
*/        //gated on 3 gates
        hFrntGCSF   =new Histogram("FrontGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc gates");
		hFrntGCS2F   =new Histogram("FrontGCS2F    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - Sc2Ca,FwCa,FwSc2 gates");
        hRearGCSF   =new Histogram(   "RearGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc gates");
		hRearGCS2F   =new Histogram(   "RearGCS2F    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - Sc2Ca,FwCa,FwSc2 gates");
        hFrntRearGCSF=new Histogram("FRGateCSF  ",HIST_2D_INT, 2*TWO_D_CHANNELS,"Front vs. Rear - ScCa, FwCa, FwSc gates");
		hFrntRearGCS2F=new Histogram("FRGateCS2F  ",HIST_2D_INT, 2*TWO_D_CHANNELS,"Front vs. Rear - Sc2Ca, FwCa, FwSc2 gates");
        //gated on 4 gates
		hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
		hFrntGAll2   =new Histogram("FrontGAll2    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - Sc2Ca,FwCa,FwSc2,FwRw gates");
		hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
        hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");
		hFrontTheta = new Histogram("FrontTheta", HIST_2D_INT, 1024, 2*TWO_D_CHANNELS,
		"Theta vs. Front Wire Position (X)", "Position", "Theta");
		hRearTheta = new Histogram("RearTheta", HIST_2D_INT, 1024, 2*TWO_D_CHANNELS,
		"Theta vs. RearWire Position (X)", "Position", "Theta");
		hFC         =new Histogram("FrontCorrected    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - refocused");
		hCathodeTheta = new Histogram("CathodeTheta", HIST_2D_INT, 1024, 2*TWO_D_CHANNELS,
		"Theta vs. Front Wire Position (X)", "Position", "Theta");
		
        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gFSum =new Gate("FrontSum", hFrontSum);
		gRSum =new Gate("RearSum", hRearSum);
        
        //gates  2d
        gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
		gSntr2Cthd   =new Gate("Ca-Sc2", hSntr2Cthd);      //gate on Scintillator Cathode
        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
		gFrntSntr2   =new Gate("Fw-Sc2", hFrntSntr2);          //gate on Front Scintillator
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
		     
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        gXY         =new Gate("XY", hYvsPsn);           // gate on x and y acceptance
        hYvsPsnGPID.addGate(gXY);
        gFYRY       =new Gate("FY-RY", hFrntYRearY);    // gate on y1 vs y2
		gFrontLvsR	=new Gate("FrontLvsR", hFrontLvsR);
		gRearLvsR	=new Gate("RearLvsR", hRearLvsR);
        hFrntSntrGSC.addGate(gFrntSntr);
		hFrntSntr2GS2C.addGate(gFrntSntr2);
        hFrntCthdGSC.addGate(gFrntCthd);
		hFrntCthdGS2C.addGate(gFrntCthd);
        hSntrCthdGFC.addGate(gSntrCthd);
		hSntr2CthdGFC.addGate(gSntr2Cthd);
        hFrntSntrGFC.addGate(gFrntSntr);
		hFrntSntr2GFC.addGate(gFrntSntr2);
        hSntrCthdGFS.addGate(gSntrCthd);
		hSntr2CthdGFS2.addGate(gSntr2Cthd);
        hFrntCthdGFS.addGate(gFrntCthd);
		hFrntCthdGFS2.addGate(gFrntCthd);
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
        mFCLR = new Monitor("FCLR",sFCLR);
        Monitor mDeadTime=new Monitor(DEAD_TIME, this);
        Monitor mTrueDeadTime = new Monitor(TRUE_DEAD_TIME, this);
        
//		Parameters
		pThetaOffset=new DataParameter("ThetaOffset");
		pXTheta=new DataParameter("(X|Theta)    ");
		pCTheta =new DataParameter("CTheta");
		// added parameters from Corr;
		pThetaOffset_p=new DataParameter("ThetaOffset - p");
		pXTheta_p=new DataParameter("(X|Theta) - p");
		pThetaOffset_d=new DataParameter("ThetaOffset - d");
		pXTheta_d=new DataParameter("(X|Theta) - d");
		pThetaOffset_t=new DataParameter("ThetaOffset - t");
		pXTheta_t=new DataParameter("(X|Theta) - t");
		pXTheta2     =new DataParameter("(X|Theta^2) ");
		pXTheta3     =new DataParameter("(X|Theta^3) ");
		

    }

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
		int eFR = dataEvent[idFrontRight];
		int eFL = dataEvent[idFrontLeft];
		int eRR = dataEvent[idRearRight];
		int eRL = dataEvent[idRearLeft];
		int eFMph = dataEvent[idFrontMiddlePH];
		int eFBph = dataEvent[idFrontBackPH];
		int eRFph = dataEvent[idRearFrontPH];
		int eRBph = dataEvent[idRearBackPH];
		int eFFph = eFHgh;
		int eRMph = eRHgh;
       //int eCthdAnde = (eCthd + eAnde)/2;

        //int eAndeADC2 = dataEvent[idAndeADC2];
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
        int ecAnde=eAnde>>TWO_D_FACTOR;
        int ecSntr1 = eSntr1>>TWO_D_FACTOR;
        int ecSntr2 = eSntr2>>TWO_D_FACTOR;
		double XTheta          = pXTheta.getValue();       		// use this to correct the focus for different particle groups with different kinematics
		double ThetaOffset   = pThetaOffset.getValue();		// center channel of Theta distribution
		double CTheta		=pCTheta.getValue();
		//added from Corr
		int iFCp, iFCd, iFCt;
		double XTheta_p      = pXTheta_p.getValue();       	// now for protons
		double ThetaOffset_p = pThetaOffset_p.getValue();	
		double XTheta_d      = pXTheta_d.getValue();       	// for deuterons
		double ThetaOffset_d = pThetaOffset_d.getValue();
		double XTheta_t      = pXTheta_t.getValue();       	// and tritons
		double ThetaOffset_t = pThetaOffset_t.getValue();	
		double XTheta2       = pXTheta2.getValue();       		// this is an aberration term and should apply for all particle groups the same
		double XTheta3       = pXTheta3.getValue(); 
		//end of addition
		int iTheta = 0;
		double temp=0;
		double Theta = 0;
		if (eFPsn>0 && eRPsn>0){
			 iTheta = eRPsn-eFPsn;
			 Theta = iTheta;
			 temp = (CTheta*(Theta-ThetaOffset));
		}
		eCthd += (int) temp;
		int ecCthd=eCthd>>TWO_D_FACTOR;

        // singles spectra
        hCthd.inc(eCthd);
        hSntr1.inc(eSntr1);
        hSntr2.inc(eSntr2);
        hSntrSum.inc(eSntr);
        hFrntPsn.inc(eFPsn);
        hRearPsn.inc(eRPsn);
        hFrntY.inc(eYF);
        hRearY.inc(eYR);
		hFrontFrontPH.inc(eFFph);
		hFrontMiddlePH.inc(eFMph);
		hFrontBackPH.inc(eFBph);
		hRearFrontPH.inc(eRFph);
		hRearMiddlePH.inc(eRMph);
		hRearBackPH.inc(eRBph);
		
//		The following 8 histograms are a different way to measure the wire positions
//			-Jac, 9/9/2003
		hFrontRight.inc(eFR);
		hFrontLeft.inc(eFL);
		hRearRight.inc(eRR);
		hRearLeft.inc(eRL);
		hFrontSum.inc(eFR+eFL);
		hFrontLvsR.inc(eFL/8,eFR/8);
		hRearLvsR.inc(eRL/8,eRR/8);
		int eFnew=0;
		if ((eFR>0)&&(eFL>0)) {
			eFnew = eFL-eFR+4095;
			hFrontNew.inc(eFnew);
		} 
		hRearSum.inc(eRR+eRL);
		int eRnew=0;
		if ((eRR>0)&&(eRL>0)) {
			eRnew  =eRL-eRR+4095;
			 hRearNew.inc(eRnew); 
		} 
		double Front=eFPsn;
		double dTheta = Theta - ThetaOffset;
		int iFC = (int)(Front + dTheta*XTheta);      	
		hFC.inc(iFC);

        hYvsPsn.inc(ecFPsn,eYF>>TWO_D_FACTOR);
        hFrntPH.inc(ecFPsn,ecFHgh);
        hRearPH.inc(ecRPsn,ecRHgh);
        hSntrCthd.inc(ecSntr,ecCthd);
        hFrntCthd.inc(ecFPsn,ecCthd);
        hFrntSntr.inc(ecFPsn,ecSntr);
        hFrntPRearP.inc(eFPsn/8,eRPsn/8);
		
	hRWbias.inc(dataEvent[idRWbias]);
	hFWbias.inc(dataEvent[idFWbias]);
	hBCIRange.inc(dataEvent[idBCIRange]);	        

        boolean bSC = gSntrCthd.inGate(ecSntr,ecCthd);
		boolean bS2C = gSntr2Cthd.inGate(ecSntr2,ecCthd);
        boolean bFC = gFrntCthd.inGate(ecFPsn,ecCthd);
        boolean bFS = gFrntSntr.inGate(ecFPsn,ecSntr);
		boolean bFS2 = gFrntSntr2.inGate(ecFPsn,ecSntr2);
        boolean bXY = gXY.inGate(ecFPsn,eYF>>TWO_D_FACTOR);
        boolean bPID = bSC && bFC && bFS;
		boolean bPID2 = bS2C && bFC && bFS2;
        boolean bFRSum =gFSum.inGate(eFR+eFL) && gRSum.inGate(eRR+eRL);
        boolean bGood = bPID && bXY && gFrntRear.inGate(eFPsn/8,eRPsn/8) && 
        gFYRY.inGate(eYF>>TWO_D_FACTOR,eYR>>TWO_D_FACTOR);
		boolean bGood2 = bPID2 && bXY && gFrntRear.inGate(eFPsn/8,eRPsn/8) && 
		gFYRY.inGate(eYF>>TWO_D_FACTOR,eYR>>TWO_D_FACTOR);
        boolean bState = bGood && gPeak.inGate(eFPsn);
		boolean bState2 = bGood2 && gPeak.inGate(eFPsn);
		boolean bAcceptance = bXY && gFrntRear.inGate(eFPsn/8,eRPsn/8) && 
		gFYRY.inGate(eYF>>TWO_D_FACTOR,eYR>>TWO_D_FACTOR) && bFRSum;
															   
        if (bSC) {// gate on Scintillator vs Cathode
            hFrntSntrGSC.inc(ecFPsn,ecSntr);
            hFrntCthdGSC.inc(ecFPsn,ecCthd);
        }
		if (bS2C) {// gate on Scintillator vs Cathode
			hFrntSntr2GS2C.inc(ecFPsn,ecSntr2);
			hFrntCthdGS2C.inc(ecFPsn,ecCthd);
		}
        if (bFC) {// gate on Front Wire Position vs Cathode
            hSntrCthdGFC.inc(ecSntr,ecCthd);
            hFrntSntrGFC.inc(ecFPsn,ecSntr);
			hSntr2CthdGFC.inc(ecSntr2,ecCthd);
			hFrntSntr2GFC.inc(ecFPsn,ecSntr2);
        }
        if (bFS){// gate on Front Wire Position vs Scintillator
            hSntrCthdGFS.inc(ecSntr,ecCthd);
            hFrntCthdGFS.inc(ecFPsn,ecCthd);
        }
		if (bFS2){// gate on Front Wire Position vs Scintillator
			hSntr2CthdGFS2.inc(ecSntr2,ecCthd);
			hFrntCthdGFS2.inc(ecFPsn,ecCthd);
		}
        if (bPID) {// gated on all 3 gate above
            //writeEvent(dataEvent);
            hFrntGCSF.inc(eFPsn);
            hRearGCSF.inc(eRPsn);
            hFrntRearGCSF.inc(eFPsn/8,eRPsn/8);
            hFrntYRearY.inc(eYF>>TWO_D_FACTOR, eYR>>TWO_D_FACTOR);
            hYvsPsnGPID.inc(ecFPsn, eYF>>TWO_D_FACTOR);
            if(bGood && bFRSum) {
                writeEvent(dataEvent);
                hFrntGAll.inc(eFPsn);
                hRearGAll.inc(eRPsn);
                hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
                hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
				hFrontTheta.inc(eFPsn>>2,iTheta);
				hRearTheta.inc(eRPsn>>2,iTheta);
            }
        }
		if (bPID2) {// gated on all 3 gate above
			//writeEvent(dataEvent);
			hFrntGCS2F.inc(eFPsn);
			hRearGCS2F.inc(eRPsn);
			hFrntRearGCS2F.inc(eFPsn/8,eRPsn/8);
			hFrntYRearY.inc(eYF>>TWO_D_FACTOR, eYR>>TWO_D_FACTOR);
			hYvsPsnGPID.inc(ecFPsn, eYF>>TWO_D_FACTOR);
			if(bGood2 && bFRSum) {
				writeEvent(dataEvent);
				hFrntGAll2.inc(eFPsn);
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
}
