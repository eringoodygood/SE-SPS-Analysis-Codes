/*
*/
package sort.coinc;
import jam.data.*;
import jam.sort.SortRoutine;
/*
 * Sort file for Enge SplitPole Online w/ two surface barriers and relative timing
 * to scintillator.
 *
 * @author Ken Swartz
 * @author Alan Chen
 * @author Dale Visser
 * @author Rachel Lewis, June 1999
 * @author William Bradfield-Smith, September 1999
 */
public class SplitPoleSiliconTiming2 extends SortRoutine {

    // ungated spectra
    Histogram hCthd;
    Histogram hAnde;
    Histogram hSntr1;
    Histogram hSntr2;
    Histogram hSntrSum;
    Histogram hFrntPsn;
    Histogram hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde;
    Histogram hSntrCthd;
    Histogram hFrntCthd;
    Histogram hFrntAnde;
    Histogram hFrntSntr;
    Histogram hFrntPRearP;

    //New section for silicon & timing signals
    Histogram hSiliconL;
    Histogram hSiliconLGatedAll;
    Histogram hSiliconLGatedPeak;
    Histogram hTimeDiffL;
    Histogram hTimeDiffLFrRear;
    Histogram hTimeDiffLGatedAll;
    Histogram hTimeDiffLGatedPeak;
    Histogram hSiliconR;
    Histogram hSiliconRGatedAll;
    Histogram hSiliconRGatedPeak;
    Histogram hTimeDiffR;
    Histogram hTimeDiffRGatedAll;
    Histogram hTimeDiffRGatedPeak;
    Histogram hTimeProt;
    Histogram hTimeDeut;
    Histogram hTimeTrit;
    Histogram hTimeAlph;
    Histogram hSiliconAlpha;

    //gate by scintillator cathode
    Histogram hFrntSntrGSC;
    Histogram hFrntCthdGSC;

    //gate by Front wire Cathode
    Histogram hSntrCthdGFC;
    Histogram hFrntSntrGFC;


    //gate by Front wire Scintillator
    Histogram hSntrCthdGFS;
    Histogram hFrntCthdGFS;

    //front and rear wire gate on all
    Histogram hFrntGAll;
    Histogram hRearGAll;

    //front and rear wire gated on All compressed
    Histogram hcFrntGAll;
    Histogram hcRearGAll;

    //front wire gated on Time Alpha spectrum
    Histogram hFrntGTAL;
    // ungated spectra
    Histogram hGe;
    Histogram hCoinTAC;
    Histogram hGeGate;

    //gates 1 d
    Gate gSiliconL;
    Gate gSiliconR;
    Gate gCthd;
    Gate gPeak;
    Gate gGood;

    //gates 2 d
    Gate gSntrCthd;
    Gate gFrntSntr;
    Gate gFrntCthd;
    Gate gFrCtProt;
    Gate gFrCtDeut;
    Gate gFrCtTrit;
    Gate gFrCtAlph;
    Gate gFrntRear;
    Gate gTime;
    Gate gTimeAlpha;

    //scalers
    Scaler sBic;
    Scaler sClck;
    Scaler sEvntRaw;
    Scaler sEvntAccpt;
    Scaler sSilleft;
    Scaler sSilright;

    //monitors
    Monitor mBeam;
    Monitor mClck;
    Monitor mEvntRt;
    Monitor mGood;
    Monitor mSilicon;
    Monitor mLive;

    //id numbers for the signals;
    int idSiliconL;
    int idSiliconR;
    int idCthd;
    int idAnde;
    int idScintR;
    int idScintL;
    int idFrntPsn;
    int idRearPsn;
    int idFrntHgh;    //front Wire Pulse Height
    int idRearHgh;    //Rear Wire Pulse Height
    int idFrntPH;    // position x height y
    int idRearPH;
    int idTimeDiffL;    // Array position for Flight Time parameter
    int idTimeDiffR;
    int lastEvntAccpt;

    public void initialize() throws Exception {
  System.out.println("Howdy2!");

  //EVENT_SIZE=9;
//  ***check the addresses for these events***
  //               <C, N, A, F>
  cnafCommands.init(1,28,8,26);      //crate dataway Z
  cnafCommands.init(1,28,9,26);      //crate dataway C
  cnafCommands.init(1,30,9,26);      //Sets the crate I (Inhibit)
  cnafCommands.init(1,3,12,11);      //adc 811 clear
  cnafCommands.init(1,9,12,11);      //adc 811 clear
  cnafCommands.init(1,13,0,9);      //TDC Lecroy 2228A clear
  cnafCommands.init(1,20,0,10);      //trigger module clear

      //event return id number to be used in sort
  //Focal Plane Detector ADC
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
  idTimeDiffR=cnafCommands.eventRead(1,13,7,2);  //read TDC ch 7 & clear all TDC channels

  //Trigger Module
  cnafCommands.eventCommand(1,20,0,10);        //clear trigger module

      cnafCommands.scaler(1,16,0,0);        //read scaler BIC
  cnafCommands.scaler(1,16,1,0);        //read scaler Clock
  cnafCommands.scaler(1,16,2,0);        //read scaler Event Raw
  cnafCommands.scaler(1,16,3,0);        //read scaler Event Accept
  cnafCommands.scaler(1,16,4,0);        //read scaler Silicon Rate
  cnafCommands.scaler(1,16,5,0);        //read scaler rate divided silicon rate
  cnafCommands.clear(1,16,0,9);        //clear scaler


  hSiliconL    =new Histogram("Silicon Left  ", HIST_1D_INT, 2048, "Left Silicon Detector");
  hSiliconLGatedAll=new Histogram("SiliconLGall  ", HIST_1D_INT, 2048, "Left Silicon Gated All");
  hSiliconLGatedPeak=new Histogram("SiliconLGpeak  ", HIST_1D_INT, 2048, "Left Silicon Gated Peak");
  hSiliconR    =new Histogram("Silicon Right ", HIST_1D_INT, 2048, "Right Silicon Detector");
  hSiliconRGatedAll=new Histogram("SiliconRGall  ", HIST_1D_INT, 2048, "Right Silicon Gated All");
  hSiliconRGatedPeak=new Histogram("SiliconRGpeak  ", HIST_1D_INT, 2048, "Right Silicon Gated Peak");
  hTimeDiffL   =new Histogram("TimeDiff L Raw", HIST_1D_INT, 2048, "TDC Left Silicon");

  hTimeDiffLGatedAll=new Histogram("TimeDiffLGall", HIST_1D_INT, 2048, "TDC Left Silicon Gated All");
  hTimeDiffLGatedPeak=new Histogram("TimeDiffLGpk", HIST_1D_INT, 2048, "TDC Left Silicon Gated Peak");
  hTimeDiffR   =new Histogram("TimeDiff R Raw", HIST_1D_INT, 2048, "TDC Right Silicon");
  hTimeDiffRGatedAll=new Histogram("TimeDiffRGall", HIST_1D_INT, 2048, "TDC Right Silicon Gated All");
  hTimeDiffRGatedPeak=new Histogram("TimeDiffRGpk", HIST_1D_INT, 2048, "TDC Right Silicon Gated Peak");

    hTimeProt=new Histogram("Time protons", HIST_1D_INT, 2048, "TDC Right Silicon Gated Peak");
  hTimeDeut=new Histogram("Time Deuterons", HIST_1D_INT, 2048, "TDC Right Silicon Gated Peak");
    hTimeTrit=new Histogram("Time Tritons", HIST_1D_INT, 2048, "TDC Right Silicon Gated Peak");
    hTimeAlph=new Histogram("Time Alphas", HIST_1D_INT, 2048, "TDC Right Silicon Gated Peak");

  hSiliconAlpha=new Histogram("Silicon A Gated", HIST_1D_INT, 2048, "Silicon gated on Alpha time spectrum");

  //Focal Plane Detector
  hCthd       =new Histogram("Cathode       ", HIST_1D_INT, 2048, "Cathode Raw ");
  //hAnde       =new Histogram("Anode         ", HIST_1D_INT, 2048, "Anode Raw");
  hSntr1      = new Histogram("Scint1        ", HIST_1D_INT, 2048, "Scintillator PMT 1");
  hSntr2       =new Histogram("Scint2        ", HIST_1D_INT, 2048, "Scintillator PMT 2");
  hSntrSum     =new Histogram("ScintSum      ", HIST_1D_INT, 2048, "Scintillator Sum");
  hFrntPsn     =new Histogram("FrontPosn     ", HIST_1D_INT, 2048, "Front Wire Position");
  hRearPsn     =new Histogram("RearPosn      ", HIST_1D_INT, 2048, "Rear Wire Position");
  hFrntHgh     =new Histogram("FrontHeight   ", HIST_1D_INT, 2048, "Front Wire Pulse Height");
  hRearHgh     =new Histogram("RearHeight    ", HIST_1D_INT, 2048, "Rear Wire Pulse Height");
  hFrntPH       =new Histogram("FrontPvsHeight", HIST_2D_INT,  256, "Pulse Height vs Front Position","Front Position","Pulse Height");
  hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,  256, "Pulse Height vs Rear Position","Rear Position", "Pulse Height");
  //hCthdAnde   =new Histogram("CathodeAnode  ", HIST_2D_INT,  256, "Cathode vs Anode ","Cathode","Anode");
  hSntrCthd   =new Histogram("ScintCathode  ", HIST_2D_INT,  256, "Cathode vs Scintillator","Scintillator","Cathode");
  hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  256, "Cathode vs Front Position","Front Position","Cathode");
  //hFrntAnde   =new Histogram("FrontAnode  ", HIST_2D_INT,  256, "Anode vs Front Position","Front Position","Anode");
  hFrntSntr   =new Histogram("FrontScint ", HIST_2D_INT,  256, "Scintillator vs Front Position","Front Position","Scintillator");
  hFrntPRearP =new Histogram("FrontRear  ", HIST_2D_INT,  256, "Rear Position vs Front Position","Front Position","Rear Position");

  //gate on Scintillator Cathode
  hFrntSntrGSC=new Histogram("FrontScintGSC ", HIST_2D_INT,  256, "Scintillator vs Front Position - ScCa gate","Front Position", "Scintillator");
  hFrntCthdGSC=new Histogram("FrontCathodeGSC", HIST_2D_INT,  256, "Cathode vs Front Position - ScCa gate","Front Position","Cathode");

      //gate on Front Wire Cathode
  hSntrCthdGFC=new Histogram("ScintCathodeGFC", HIST_2D_INT,  256, "Cathode vs Scintillator - FwCa gate", "Scintillator","Cathode");
  hFrntSntrGFC=new Histogram("FrontScintGFC", HIST_2D_INT,  256, "Scintillator vs Front Position - FwCa gate","Front Position", "Scintillator");

  //gate on Front Wire Scintillator
  hSntrCthdGFS=new Histogram("ScintCathodeGFS", HIST_2D_INT,  256, "Cathode vs Scintillator - FwSc gate","Scintillator","Cathode");
  hFrntCthdGFS=new Histogram("FrontCathodeGFS ", HIST_2D_INT,  256, "Cathode vs Front Position - FwSc gate ","Front Position","Cathode");

  //gate on 4 gates
  hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, 2048, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
  hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, 2048, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
  hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, 512, "Front Position comp - ScCa,FwCa,FwSc,FwRw gates");
  hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, 512, "Rear Position comp - ScCa,FwCa,FwSc,FwRw gates");
  hFrntGTAL   =new Histogram("FrontGTAL    ", HIST_1D_INT, 2048, "Front Position -  gated on alpha time spectrum");
  // gates 1d
  gCthd     = new Gate("Counts", hCthd);
  gSiliconL = new Gate("SiliconL", hSiliconL);
  gSiliconR = new Gate("SiliconR", hSiliconR);
  gGood    = new Gate("GoodEvent",hFrntGAll);
  gPeak     = new Gate("Peak", hFrntGAll);
  gTime     = new Gate("Time", hTimeDiffL);
  gTimeAlpha= new Gate("TimeAlpha", hTimeAlph);
  //gates  2d
  gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
  gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
  gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
  gFrCtProt   =new Gate("FC-1H", hFrntCthd);
  gFrCtDeut   =new Gate("FC-2H", hFrntCthd);
  gFrCtTrit   =new Gate("FC-3H", hFrntCthd);
  gFrCtAlph   =new Gate("FC-He", hFrntCthd);
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
  sSilleft    =new Scaler("Silicon Left", 4);
  sSilright   =new Scaler("Silicon Right",5);
  //monitors
  mBeam=new Monitor("Beam ",sBic);
  mClck=new Monitor("Clock",sClck);
  mEvntRt=new Monitor("Event Rate",sEvntRaw);
  mGood=new Monitor("Good Events",gGood);
  mSilicon=new Monitor("Sili Rate",sSilleft);
  mLive=new Monitor("Live Time", this);
    }
    public void sort(int [] dataEvent) throws Exception {

  //unpack data into convenient names
  int eCthd   = dataEvent[idCthd];
  int eAnde   = dataEvent[idAnde];
  int eSntr1  = dataEvent[idScintR];
  int eSntr2  = dataEvent[idScintL];
  int eFPsn   = dataEvent[idFrntPsn];
  int eRPsn   = dataEvent[idRearPsn];
  int eFHgh   = dataEvent[idFrntHgh];
  int eRHgh   = dataEvent[idRearHgh];
  int eSilL   = dataEvent[idSiliconL];
  int eSilR   = dataEvent[idSiliconR];
  int eTDL    = dataEvent[idTimeDiffL];
  int eTDR    = dataEvent[idTimeDiffR];
  int eSntr   = (eSntr1+eSntr2)/2;

  int ecFPsn=eFPsn>>3;
  int ecRPsn=eRPsn>>3;
  int ecFHgh=eFHgh>>3;
  int ecRHgh=eRHgh>>3;

  int ecSntr=eSntr>>3;
  int ecCthd=eCthd>>3;
  //int ecAnde=eAnde>>3;

  // singles spectra
  hCthd.inc(eCthd);
  //hAnde.inc(eAnde);
  hSntr1.inc(eSntr1);
  hSntr2.inc(eSntr2);
      hSntrSum.inc(eSntr);
  hFrntPsn.inc(eFPsn);
  hRearPsn.inc(eRPsn);
  hFrntHgh.inc(eFHgh);
  hRearHgh.inc(eRHgh);
  hSiliconL.inc(eSilL);
  hSiliconR.inc(eSilR);

  //Time of Flight
  hTimeDiffL.inc(eTDL);
  hTimeDiffR.inc(eTDR);

  //singles 2d spectra
  hFrntPH.inc(ecFPsn,ecFHgh);
  hRearPH.inc(ecRPsn,ecRHgh);
  //hCthdAnde.inc(ecCthd,ecAnde);
  hSntrCthd.inc(ecSntr,ecCthd);
  hFrntCthd.inc(ecFPsn,ecCthd);
  //hFrntAnde.inc(ecFPsn,ecAnde);
  hFrntSntr.inc(ecFPsn,ecSntr);
  hFrntPRearP.inc(ecFPsn,ecRPsn);
  if (gFrntRear.inGate(ecFPsn,ecRPsn)) {
    writeEvent(dataEvent);
  }

    //gate on different particle in Front Cathode for time spectra
  if (gFrCtProt.inGate(ecFPsn,ecCthd)) {
      hTimeProt.inc(eTDR);
  }
   if (gFrCtDeut.inGate(ecFPsn,ecCthd)) {
      hTimeDeut.inc(eTDR);
  }
  if (gFrCtTrit.inGate(ecFPsn,ecCthd)) {
      hTimeTrit.inc(eTDR);
  }
  if (gFrCtAlph.inGate(ecFPsn,ecCthd)) {
      hTimeAlph.inc(eTDR);
  }


         // gate on Scintillator vs Cathode
  if ( gSntrCthd.inGate(ecSntr,ecCthd) ){
      hFrntSntrGSC.inc(ecFPsn,ecSntr);
      hFrntCthdGSC.inc(ecFPsn,ecCthd);

  }
  // gate on Front Wire Position vs Cathode
  if ( gFrntCthd.inGate(ecFPsn,ecCthd) ){
      hSntrCthdGFC.inc(ecSntr,ecCthd);
      hFrntSntrGFC.inc(ecFPsn,ecSntr);
  }
  // gate on Front Wire Position vs Scintillator
  if ( gFrntSntr.inGate(ecFPsn,ecSntr) ){
      hSntrCthdGFS.inc(ecSntr,ecCthd);
      hFrntCthdGFS.inc(ecFPsn,ecCthd);
  }

  // gate on all 3 gates above and the Front wire vs Rear Wire
  if (( gSntrCthd.inGate(ecSntr,ecCthd) )&&
      ( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
      ( gFrntSntr.inGate(ecFPsn,ecSntr) )&&
      ( gFrntRear.inGate(ecFPsn,ecRPsn) )){
        hFrntGAll.inc(eFPsn);
        hRearGAll.inc(eRPsn);
        hcFrntGAll.inc(eFPsn>>2);
        hcRearGAll.inc(eRPsn>>2);
        if (gTime.inGate(eTDL)){
      hSiliconLGatedAll.inc(eSilL);
        }
        hSiliconRGatedAll.inc(eSilR);
        hTimeDiffLGatedAll.inc(eTDL);
        hTimeDiffRGatedAll.inc(eTDR);
        if (gPeak.inGate(eFPsn)) {
            hTimeDiffLGatedPeak.inc(eTDL);
            hSiliconLGatedPeak.inc(eSilL);
      hTimeDiffRGatedPeak.inc(eTDR);
      hSiliconRGatedPeak.inc(eSilR);
        }   
        if (gTimeAlpha.inGate(eTDR) ) {
      hSiliconAlpha.inc(eSilR);
      hFrntGTAL.inc(eFPsn);
        }   
  }
          
    }
    /**
     * monitor method
     * calculate the live time
     */
    public double monitor(String name){
  int rateEvntAccpt=sEvntAccpt.getValue()-lastEvntAccpt;
      lastEvntAccpt=sEvntAccpt.getValue();
  
  if (name.equals("Live Time")){  
      if (mEvntRt.getValue()>0){
    return 100.0*rateEvntAccpt/mEvntRt.getValue();
      } else {
    return 0.0;
      }        
  } else {
      return 50.0;
  }            
    }
}
