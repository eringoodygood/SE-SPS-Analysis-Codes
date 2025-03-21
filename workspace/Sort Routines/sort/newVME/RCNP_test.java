package sort.newVME;
import jam.data.*;
import jam.sort.*;

/**
 * sort routine for RCNP run w/ Grand Radian and LAS
 * Modified from SplitPole TAJ_gscint2.java routine used at Yale.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>,
 * which was used in the January 2001 test run.
 * 
 *
 * @author     Catherine Deibel
 * @created    December 4, 2007
 * @since      26 July 2001
 * 
 */
public class RCNP_test extends SortRoutine {

  private final static String DEAD_TIME = "Dead Time %";
  /*
   *  VME properties
   */
  //private final static int[] ADC_BASE = {0x0000000, 0x20000000, 0x20010000};
  private final static int[] crate = {0};
  //private final static int[] TDC_BASE = {0x30080000};
  
  static final int NUM_SLOTS = 20;
  
  int [] idScintR1 = new int [NUM_SLOTS];
  int [] idScintL1 = new int [NUM_SLOTS];
  int [] idScintR2 = new int [NUM_SLOTS];
  int [] idScintL2 = new int [NUM_SLOTS];
  
  
  /*
   *  ADC lower threshold in channels
   */
  private final static int THRESHOLDS = 10;
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
  private transient Histogram hSntr1R, hSntr1L, hSntr2R, hSntr2L, hSntrSum1, hSntrSum2, hSntrLSntrR1_raw, hSntrLSntrR2_raw,hSntrLSntrR1, hSntrLSntrR2,
      hFrntPsn, hRearPsn, hMonitor;
 
  Histogram [] hScintR1 = new Histogram[NUM_SLOTS];
  Histogram [] hScintL1 = new Histogram[NUM_SLOTS];
  Histogram [] hScintR2 = new Histogram[NUM_SLOTS];
  Histogram [] hScintL2 = new Histogram[NUM_SLOTS];
  
  Histogram [] hScint1 = new Histogram[NUM_SLOTS];
  Histogram [] hScint2 = new Histogram[NUM_SLOTS];
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
  private transient int idScint1R, idScint1L, idScint2R, idScint2L;
  


  /**
   *  Description of the Method
   *
   * @exception  Exception  Description of the Exception
   */
  public void initialize() throws Exception {
    vmeMap.setScalerInterval(3);
    /*for (int i = 0; i < TDC_BASE.length; i++) {
      vmeMap.setV775Range(TDC_BASE[i], TIME_RANGE);
    }*/

    vmeMap.eventParameter(0, 0, 0, 0);
    
    idScint1R = vmeMap.eventParameter(2, 0, 1, THRESHOLDS);
    idScint1L = vmeMap.eventParameter(2, 0, 0, THRESHOLDS);
	idScint2R = vmeMap.eventParameter(2, 0, 3, THRESHOLDS);
	idScint2L = vmeMap.eventParameter(2, 0, 2, THRESHOLDS);
    /*
	for (int i=0; i<NUM_SLOTS; i++){
		idScintR1[i]=vmeMap.eventParameter(i, ADC_BASE[0], 1, THRESHOLDS);
		idScintL1[i]=vmeMap.eventParameter(i, ADC_BASE[0], 0, THRESHOLDS);
		idScintR2[i]=vmeMap.eventParameter(i, ADC_BASE[0], 3, THRESHOLDS);
		idScintL2[i]=vmeMap.eventParameter(i, ADC_BASE[0], 2, THRESHOLDS);
		hScintR1[i]=new Histogram("ScintR1"+i, HIST_1D_INT, ADC_CHANNELS, "Scint R1 slot "+i);
		hScintL1[i]=new Histogram("ScintL1"+i, HIST_1D_INT, ADC_CHANNELS, "Scint L1 slot "+i);
		hScintR2[i]=new Histogram("ScintR2"+i, HIST_1D_INT, ADC_CHANNELS, "Scint R2 slot "+i);
		hScintL2[i]=new Histogram("ScintL2"+i, HIST_1D_INT, ADC_CHANNELS, "Scint L2 slot "+i);
		hScint1[i]=new Histogram("Scint1 L vs R"+i, HIST_2D_INT, 512, "Scint 1 L vs R slot "+i);
		hScint2[i]=new Histogram("Scint2 L vs R"+i, HIST_2D_INT, 512, "Scint 2 L vs R slot "+i);
	}
    */
    //1D Histograms
  
    hSntr1R = new Histogram("Scint1R      ", HIST_1D_INT,
        ADC_CHANNELS, "Scintillator PMT 1R");
	hSntr1L = new Histogram("Scint1L      ", HIST_1D_INT,
		ADC_CHANNELS, "Scintillator PMT 1L");
	hSntr2R = new Histogram("Scint2R      ", HIST_1D_INT,
		ADC_CHANNELS, "Scintillator PMT 2R");
    hSntr2L = new Histogram("Scint2L      ", HIST_1D_INT,
        ADC_CHANNELS, "Scintillator PMT 2L");
    hSntrSum1 = new Histogram("ScintSum1    ", HIST_1D_INT,
        ADC_CHANNELS, "Scintillator 1 Sum");
	hSntrSum2 = new Histogram("ScintSum2    ", HIST_1D_INT,
		ADC_CHANNELS, "Scintillator 2 Sum");
	hSntrLSntrR1_raw = new Histogram("ScintR1-ScintL1_raw",
					HIST_2D_INT, 512,
					"Raw Data: Scintillator 1 PMT R versus Scintillator PMT L   ", "Scint 1", "Scint2");
	hSntrLSntrR2_raw = new Histogram("ScintR1-ScintL2_raw",
						HIST_2D_INT, 512,
						"Raw Data: Scintillator 2 PMT R versus Scintillator PMT L   ", "Scint 1", "Scint2");
	hSntrLSntrR1 = new Histogram("ScintR1-ScintL1",
						HIST_2D_INT, 512,
						"Scintillator 1 PMT R versus Scintillator PMT L   ", "Scint 1", "Scint2");
	hSntrLSntrR2 = new Histogram("ScintR1-ScintL2",
							HIST_2D_INT, 512,
							"Scintillator 2 PMT R versus Scintillator PMT L   ", "Scint 1", "Scint2");
    
      //scalers
    sBic = new Scaler("BIC", 0);
    sClck = new Scaler("Clock", 1);
    sEvntRaw = new Scaler("Event Raw", 2);
    sEvntAccpt = new Scaler("Event Accept", 3);
    //sScint = new Scaler(SCINT, 4);
    sFCLR = new Scaler("FCLR", 6);
    sNMR = new Scaler("NMR", 14);

    //monitors
    mBeam = new Monitor("Beam ", sBic);
    mClck = new Monitor("Clock", sClck);
    mEvntRaw = new Monitor("Raw Events", sEvntRaw);
    mEvntAccept = new Monitor("Accepted Events", sEvntAccpt);
    //mScint = new Monitor(SCINT, sScint);
    mFCLR = new Monitor("FCLR", sFCLR);
    mNMR = new Monitor("NMR", sNMR);
    new Monitor(DEAD_TIME, this);

  
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
    int SCINT1R = dataEvent[idScint1R];
    int SCINT1L = dataEvent[idScint1L];
	int SCINT2R = dataEvent[idScint2R];
	int SCINT2L = dataEvent[idScint2L];
      /*
     *  proper way to add for 2 phototubes at the ends of
     *  scintillating rod see Knoll
     */
     
	final int SCINT1 = (int) Math.round(Math.sqrt(SCINT1R * SCINT1L));
	final int SCINT2 = (int) Math.round(Math.sqrt(SCINT2R * SCINT2L));
	final int SCINT1a = SCINT1R >> TWO_D_FACTOR;
	final int SCINT2a = SCINT2R >> TWO_D_FACTOR; 
	final int SCINT1b = SCINT1L >> TWO_D_FACTOR;
	final int SCINT2b = SCINT2L >> TWO_D_FACTOR; 
    final int SCINT1_COMPR = SCINT1 >> TWO_D_FACTOR;
	final int SCINT2_COMPR = SCINT2 >> TWO_D_FACTOR;
	
    /*
     *  use this to correct the focus for different particle groups with
     *  different kinematics
     */
   // center channel of Theta distribution
   
    // singles spectra
    hSntr1R.inc(SCINT1R);
	hSntr1L.inc(SCINT1L);
	hSntr2R.inc(SCINT2R);
    hSntr2L.inc(SCINT2L);
    hSntrSum1.inc(SCINT1);
	hSntrSum2.inc(SCINT2);
	hSntrLSntrR1_raw.inc(SCINT1L,SCINT1R);
	hSntrLSntrR2_raw.inc(SCINT2L,SCINT2R);
    hSntrLSntrR1.inc(SCINT1b,SCINT1a);
	hSntrLSntrR2.inc(SCINT2b,SCINT2a);
	
	
	int [] eScintR1 = new int[NUM_SLOTS];
	int [] eScintL1 = new int[NUM_SLOTS];
	int [] eScintR2 = new int[NUM_SLOTS];
	int [] eScintL2 = new int[NUM_SLOTS];
	/*
	for(int i=0; i<NUM_SLOTS; i++){
		eScintR1[i]=dataEvent[idScintR1[i]];
		eScintL1[i]=dataEvent[idScintL1[i]];
		eScintR2[i]=dataEvent[idScintR2[i]];
		eScintL2[i]=dataEvent[idScintL2[i]];
		hScintR1[i].inc(eScintR1[i]);
		hScintL1[i].inc(eScintL1[i]);
		hScintR2[i].inc(eScintR2[i]);
		hScintL2[i].inc(eScintL2[i]);
		hScint1[i].inc(eScintR1[i],eScintL1[i]);
		hScint2[i].inc(eScintR2[i],eScintL2[i]);
		
	}*/
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

