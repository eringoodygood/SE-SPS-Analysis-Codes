/*
 */
package sort.lewising;
import jam.data.*;
import jam.global.Sorter;
import jam.sort.SortRoutine;
import jam.JamException;
/*
 * Sort file for Enge SplitPole
 * Nuclear Astrophysics Group at Yale University
 *
 * Convention for 2 d Histograms: x first, then y (x vs y)
 *
 * Authors: Ken Swartz, Alan Chen, and Dale Visser
 * Modified by Rachel Lewis, March 2001 for VME and anode
 * This one assumes that the angular and focal plane correction parameters 
 * are known. It generates new 2-d histograms. The gates set in these new 
 * histograms determine what goes into FrontGAll.
 */
public class AngleCorrections2FCS extends SortRoutine {

  Histogram hFrntPsn;
  Histogram hRearPsn;
  Histogram hSntrCthd;
  Histogram hFrntCthd;
  Histogram hFrntSntr;
  Histogram hFrntPRearP;
  Histogram hSntrCthd2N;
  Histogram hFrntCthd2N;
  Histogram hFrntSntr2N;
  Histogram hFrntGAll;
  Histogram hRearGAll;
  Histogram hcFrntGAll;
  Histogram hcRearGAll;
  Histogram hFrntNew;
  Histogram hcFrntNew;
  Histogram hAndeCthd, hAndeFrnt, hAndeSntr;

  //gates 2 d
  Gate gRearFrnt;
  Gate gSntrCthdN;
  Gate gFrntSntrN;
  Gate gFrntCthdN;
  Gate gAndeCthd, gAndeFrnt, gCthdFrnt, gCthdSntr, gSntrFrnt, gAndeSntr;

  /*scalers
  Scaler sBic;
  Scaler sClck;
  Scaler sEvntRaw;
  Scaler sEvntAccpt;
  Scaler sSilRaw;
  Scaler sSilAccpt;

  //monitors
  Monitor mBeam;
  Monitor mClck;
  Monitor mEvntRt;
  Monitor mCthd;*/

  //parameters
  DataParameter pSlopeFa; // slope = a + b * (x-x0)
  DataParameter pSlopeFb, pSlopeFx0, pSlopeF, pSlopeC, pSlopeCa, pSlopeCb;
  DataParameter pSlopeCx0, pSlopeS, pSlopeSa, pSlopeSb, pSlopeSx0, pOffset;
  DataParameter pSlopeAx0, pSlopeA, pSlopeAa, pSlopeAb;

  /*DataParameter id's
  int idCthd,idAnde,idScintR,idScintL,idFrntPsn,idRearPsn,idFrntHgh,idRearHgh;
  int idSiliconL,idSiliconR,idTimeDiffL,idTimeDiffR;*/
   //id numbers for the signals;NEW
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh, idRearHgh, idSilicon, idNaI1, idNaI2;
    int idFrontR, idFrontL, idRearR, idRearL;
    int NUM_PARAMETERS;
    int lastEvntAccpt;

    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS=1024;//number of channels in compressed position histogram
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    static final int ADC_BASE = 0x20000000; //NEW
    static final int ADC_2_BASE = 0x20010000;
    static final int TDC_BASE = 0x30000000;
    static final int TDC_2_BASE = 0x30010000;
    static final int THRESHOLDS = 0;

  public void initialize() throws Exception {

    //event return id number to be used in sort
    /*Focal Plane Detector ADC
    idCthd      = cnafCommands.eventRead(1,3,0,0);      //read first channel:Cathode signal
    idAnde      = cnafCommands.eventRead(1,3,1,0);      //read Anode
    idScintR    = cnafCommands.eventRead(1,3,2,0);      //read first channel:Cathode signal
    idScintL    = cnafCommands.eventRead(1,3,3,0);      //read Anode
    idFrntPsn   = cnafCommands.eventRead(1,3,4,0);      //read Front TAC
    idRearPsn   = cnafCommands.eventRead(1,3,5,0);      //read Rear TAC
    idFrntHgh   = cnafCommands.eventRead(1,3,6,0);      //read front wire pulse height
    idRearHgh   = cnafCommands.eventRead(1,3,7,0);      //read rear wire pulse height
    cnafCommands.eventCommand(1,3,12,11);          //clear adc

    //Silicon Detector ADC
    idSiliconL  = cnafCommands.eventRead(1,9,0,0);      //Silicon Left
    idSiliconR  = cnafCommands.eventRead(1,9,1,0);      //Silicon Right
    cnafCommands.eventCommand(1,9,12,11);        //clear adc

    //TDC
    //cnafCommands.eventCommand(1,13,7,25);           //Generate internal start and stop
    // on TDC register 7 at 75% FS
    idTimeDiffL=cnafCommands.eventRead(1,13,6,2);  //read TDC ch 6
    idTimeDiffR=cnafCommands.eventRead(1,13,7,2);  read TDC ch 7 & clear all TDC channels*/
        //id numbers for the signals;NEW
        idCthd=vmeMap.eventParameter(1, ADC_BASE, 0, THRESHOLDS);
        idAnde=vmeMap.eventParameter(2, ADC_BASE, 1, THRESHOLDS);
        idScintR=vmeMap.eventParameter(3, ADC_BASE, 2, THRESHOLDS);
        idScintL=vmeMap.eventParameter(4, ADC_BASE, 3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(5, ADC_BASE, 4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(6, ADC_BASE, 5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(7, ADC_BASE, 6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(8, ADC_BASE, 7, THRESHOLDS);
        idSilicon=vmeMap.eventParameter(9, ADC_BASE, 8, THRESHOLDS);
        idNaI1=vmeMap.eventParameter(10, ADC_BASE, 9, THRESHOLDS);
        idNaI2=vmeMap.eventParameter(11, ADC_BASE, 10, THRESHOLDS);
        idFrontR=vmeMap.eventParameter(12, TDC_BASE, 0, THRESHOLDS);
        idFrontL=vmeMap.eventParameter(13, TDC_BASE, 1, THRESHOLDS);
        idRearR=vmeMap.eventParameter(14, TDC_BASE, 2, THRESHOLDS);
        idRearL=vmeMap.eventParameter(15, TDC_BASE, 3, THRESHOLDS);
//NEW        System.err.println("# Parameters: "+NUM_PARAMETERS);
        System.err.println("# Parameters: "+getEventSize());//NEW
        System.err.println("ADC channels: "+ADC_CHANNELS);
        System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
        System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);
    
    /*Trigger Module
    cnafCommands.eventCommand(1,20,0,10);        clear trigger module*/
    hFrntPRearP     =new Histogram("FrontRear   ", HIST_2D,  TWO_D_CHANNELS, " Front Position vs Rear Position ", "front position", "rear position");
    hSntrCthd       =new Histogram("ScintCathode ", HIST_2D,  TWO_D_CHANNELS, " Scintillator vs Cathode ", "scintillator", "cathode");
    hFrntCthd       =new Histogram("FrontCathode ", HIST_2D,  TWO_D_CHANNELS, " Front Position vs Cathode ", "front position", "cathode");
    hFrntSntr       =new Histogram("FrontScint   ", HIST_2D,  TWO_D_CHANNELS, " Front Position vs Scintillator ", "front position", "scintillator");
    hSntrCthd2N   =new Histogram("ScintCath2N", HIST_2D,  TWO_D_CHANNELS, " Scintillator vs Cathode Corrected - FwSc, FwCa gates ", "scintillator", "cathode");
    hFrntCthd2N	=new Histogram("FrontCath2N ", HIST_2D,  TWO_D_CHANNELS, " Front Position vs Cathode Corrected - FwSc, ScCa gates", "front position", "cathode");
    hFrntSntr2N	=new Histogram("FrontScint2N", HIST_2D,  TWO_D_CHANNELS, " Front Position vs Scintillator Corrected - FwCa, ScCa gates", "front position", "scintillator");
    hAndeCthd   =new Histogram( "Ande Vs Cthd ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Cathode ","Cathode","Anode");
    hAndeFrnt   =new Histogram( "Ande Vs Frnt ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Front Position","Front Position","Anode");
    hAndeSntr   =new Histogram( "Ande Vs Sntr ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Scintillator","Scintillator","Anode");

    hFrntPsn	=new Histogram("FrontPsn    ",HIST_1D, ADC_CHANNELS, " Front Wire Position", "channel", "counts");
    hRearPsn	=new Histogram("RearPsn     ", HIST_1D, ADC_CHANNELS, " Rear Wire Position", "channel", "counts");
    hFrntGAll       =new Histogram("FrontGAll   ", HIST_1D, ADC_CHANNELS, " Front Position - ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");
    hRearGAll       =new Histogram("RearGAll    ", HIST_1D, ADC_CHANNELS, " Rear Position - ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");
    hcFrntGAll       =new Histogram("FrontGAllComp", HIST_1D, COMPRESSED_CHANNELS, " Front Position Compressed- ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");
    hcRearGAll       =new Histogram("RearGAllComp", HIST_1D, COMPRESSED_CHANNELS, " Rear Position Compressed- ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");
    hFrntNew       =new Histogram("FrontGANew", HIST_1D, ADC_CHANNELS, " Front Position Corrected - ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");
    hcFrntNew       =new Histogram("FrontGANewComp", HIST_1D, COMPRESSED_CHANNELS, " Front Position Corrected Compressed- ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");

    //gates  2d
    gAndeCthd = new Gate("An-Ca", hAndeCthd);
    gAndeFrnt = new Gate("An-Fw", hAndeFrnt);
    gCthdFrnt   =new Gate("Fw-Ca", hFrntCthd);
    gCthdSntr   =new Gate("Ca-Sc", hSntrCthd);
    gSntrFrnt   =new Gate("Fw-Sc", hFrntSntr);
    gAndeSntr = new Gate("An-Sc", hAndeSntr); 
    gRearFrnt   =new Gate("Rw-Fw", hFrntPRearP);
    gSntrCthdN   =new Gate("CathodeScintNew ", hSntrCthd2N);
    gFrntSntrN   =new Gate("FrontScintNew   ", hFrntSntr2N);
    gFrntCthdN   =new Gate("FrontCathNew    ", hFrntCthd2N);

/*    sBic	    =new Scaler("BIC",0);
    sClck	    =new Scaler("Clock",1);
    sEvntRaw    =new Scaler("Event Raw", 2);
    sEvntAccpt  =new Scaler("Event Accept",3);
    sSilRaw     =new Scaler("Monitor Raw", 4);
    sSilAccpt   =new Scaler("Monitor Accept",5);*/

        pSlopeFa=new DataParameter("SlopeFR a ");
        pSlopeFb=new DataParameter("SlopeFR b ");
        pSlopeFx0=new DataParameter("SlopeFR x0");
        pOffset=new DataParameter("Offset");
        pSlopeCa=new DataParameter("SlopeCath a ");
        pSlopeCb=new DataParameter("SlopeCath b ");
        pSlopeCx0=new DataParameter("SlopeCath x0");
        pSlopeSa=new DataParameter("SlopeScint a");
        pSlopeSb=new DataParameter("SlopeScint b");
        pSlopeSx0=new DataParameter("SlopeScintx0");
        pSlopeAa=new DataParameter("SlopeAnode a");
        pSlopeAb=new DataParameter("SlopeAnode b");
        pSlopeAx0=new DataParameter("SlopeAnodex0");


/*    mBeam=new Monitor("Beam ",sBic);
    mClck=new Monitor("Clock",sClck);
    mEvntRt=new Monitor("Event Rate",sEvntRaw);*/
  }
  public void sort(int [] dataEvent) throws Exception{

    //get DataParameter values
       double offset=pOffset.getValue();
        double slopeFa=pSlopeFa.getValue();
        double slopeFb=pSlopeFb.getValue();
        double slopeFx0=pSlopeFx0.getValue();
        double slopeCa=pSlopeCa.getValue();
        double slopeCb=pSlopeCb.getValue();
        double slopeCx0=pSlopeCx0.getValue();
        double slopeSa=pSlopeSa.getValue();
        double slopeSb=pSlopeSb.getValue();
        double slopeSx0=pSlopeSx0.getValue();
        double slopeAa=pSlopeAa.getValue();
        double slopeAb=pSlopeAb.getValue();
        double slopeAx0=pSlopeAx0.getValue();

    //unpack data into convenient names
    int eCthd   =dataEvent[idCthd];
    int eAnde   =dataEvent[idAnde];
    int eSntr1  =dataEvent[idScintR];
    int eSntr2  =dataEvent[idScintL];
    int eFPsn   =dataEvent[idFrntPsn];
    int eRPsn   =dataEvent[idRearPsn];
    int eFHgh   =dataEvent[idFrntHgh];
    int eRHgh   =dataEvent[idRearHgh];
//    int eSil    =dataEvent[idSiliconL];

    int eSntr=(eSntr1+eSntr2)/2;

    int ecFPsn=eFPsn>>TWO_D_FACTOR;
    int ecRPsn=eRPsn>>TWO_D_FACTOR;
    int ecFHgh=eFHgh>>TWO_D_FACTOR;
    int ecRHgh=eRHgh>>TWO_D_FACTOR;

    int ecSntr=eSntr>>TWO_D_FACTOR;
    int ecCthd=eCthd>>TWO_D_FACTOR;
    int ecAnde=eAnde>>TWO_D_FACTOR;

    double slopeF=slopeFa+slopeFb*(eFPsn - slopeFx0);
    double slopeC=slopeCa+slopeCb*(eFPsn - slopeCx0);
    double slopeS=slopeSa+slopeSb*(eFPsn - slopeSx0);
    double slopeA=slopeAa+slopeAb*(eFPsn - slopeAx0);

    double diff=eRPsn-eFPsn-offset;
    int front=Math.max((int)(eFPsn-slopeF*diff),0);
    int cfront=front>>TWO_D_FACTOR;
    int scintN=Math.max((int)(ecSntr-slopeS*diff),0);
    int cathN=Math.max((int)(ecCthd-slopeC*diff),0);
    int andeN=Math.max((int)(ecAnde-slopeC*diff),0);
    int theta=(int) (128+diff);

    // singles spectra: all uncorrected
    hFrntPsn.inc(eFPsn);
    hRearPsn.inc(eRPsn);
    hSntrCthd.inc(ecSntr,ecCthd);
    hFrntCthd.inc(ecFPsn,ecCthd);
    hFrntSntr.inc(ecFPsn,ecSntr);
    hFrntPRearP.inc(ecFPsn,ecRPsn);
    hAndeCthd.inc(ecCthd,ecAnde);
    hAndeFrnt.inc(ecFPsn,ecAnde);
    hAndeSntr.inc(ecSntr,ecAnde);

    //gate on FrontRear
    if ( gRearFrnt.inGate(ecFPsn,ecRPsn) ){
      if ( gCthdSntr.inGate(ecSntr,ecCthd) ){
        if ( gCthdFrnt.inGate(ecFPsn,ecCthd) ){
          if ( gSntrFrnt.inGate(ecFPsn,ecSntr) ){
            // writeEvent(dataEvent);//write presorted file of events making it through all raw gates
          }
          hFrntSntr2N.inc(cfront,scintN );
        }
        if ( gSntrFrnt.inGate(ecFPsn,ecSntr) ){
          hFrntCthd2N.inc(cfront,cathN);
        }
      }
      if ( gCthdFrnt.inGate(ecFPsn,ecCthd) ){
        if ( gSntrFrnt.inGate(ecFPsn,ecSntr) ){
          hSntrCthd2N.inc(scintN ,cathN);
        }
      }
      if (gFrntCthdN.inGate(cfront,cathN)){
        if (gSntrCthdN.inGate(scintN,cathN) ){
          if ( gFrntSntrN.inGate(cfront,scintN) ){
            hFrntGAll.inc(eFPsn);
            hRearGAll.inc(eRPsn);
            hcFrntGAll.inc(eFPsn>>1);
            hcRearGAll.inc(eRPsn>>1);
            hFrntNew.inc(front);
            hcFrntNew.inc(front>>1);
            writeEvent(dataEvent);//write presorted file of events in corrected spectra
          }
        }
      }
    }//end gate on front rear
  }//end sort
}//end
