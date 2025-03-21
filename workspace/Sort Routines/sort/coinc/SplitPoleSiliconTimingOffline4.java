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
public class SplitPoleSiliconTimingOffline4 extends SortRoutine {
  // ungated spectra
  Histogram hCthd,hAnde,hFrntPsn,hRearPsn;
  Histogram hFrntHgh;    //front Wire Pulse Height
  Histogram hRearHgh;    //Rear Wire Pulse Height
  Histogram hCthdAnde,hSntrCthd,hFrntCthd,hFrntAnde,hFrntSntr;
  //New section for silicon & timing signals
  Histogram hSiliconL,hSiliconLGatedAll,hTimeDiffL,hTimeDiffLGatedAll;
  Histogram hSiliconR,hSiliconRGatedAll,hTimeDiffR,hTimeDiffRGatedAll;
  //gate by scintillator cathode
  Histogram hFrntSntrGSC,hFrntCthdGSC;
  //gate by Front wire Cathode
  Histogram hSntrCthdGFC,hFrntSntrGFC;
  //gate by Front wire Scintillator
  Histogram hSntrCthdGFS,hFrntCthdGFS;
  //gated on PID & TDC
  Histogram hSiliFrnt;
  //front and rear wire gate on all
  Histogram hFrntGAll,hRearGAll;
  //front wire gated on Time PID spectrum
  Histogram hFrntGL1,hFrntGL2;
  Histogram hFrntGR1,hFrntGR2;
  Histogram hFrntGL1p,hFrntGL2p;
  Histogram hFrntGR1p,hFrntGR2p;
  //silicon gated on Time PID spectrum
  Histogram hSiliGL1,hSiliGL2;
  Histogram hSiliGR1,hSiliGR2;
  Histogram hSiliGL1p,hSiliGL2p;
  Histogram hSiliGR1p,hSiliGR2p;
  //gates 1 d
  Gate gSiliconL;
  Gate gSiliconR;
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
  //gates on TDC's
  Gate gTimeL1;
  Gate gTimeL2;
  Gate gTimeR1;
  Gate gTimeR2;
  //2d kinematic gates on SiliL vs FP
  Gate alpha0,alpha1,alpha2;
  Histogram hFrontPa0,hFrontPa1,hFrontPa2;
  Histogram hTimeDiffLa0,hTimeDiffLa1,hTimeDiffLa2;
  //scalers
  Scaler sBic;
  Scaler sClck;
  Scaler sEvntRaw;
  Scaler sEvntAccpt;
  Scaler sSilleft;
  Scaler sSilright;
  //id numbers for the signals;
  int idSiliconL,idSiliconR,idCthd,idAnde,idScintR,idScintL,idFrntPsn,idRearPsn;
  int idFrntHgh;    //front Wire Pulse Height
  int idRearHgh;    //Rear Wire Pulse Height
  int idFrntPH;    // position x height y
  int idRearPH;
  int idTimeDiffL;    // Array position for Flight Time parameter
  int idTimeDiffR;
  int lastEvntAccpt;

  public void initialize() throws Exception {
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
    idTimeDiffL=cnafCommands.eventRead(1,13,6,2);  //read TDC ch 6
    idTimeDiffR=cnafCommands.eventRead(1,13,7,2);  //read TDC ch 7 & clear all TDC channels
    //Trigger Module
    cnafCommands.eventCommand(1,20,0,10);        //clear trigger module
    //Scaler commands for reading scalers
    cnafCommands.scaler(1,16,0,0);        //read scaler BIC
    cnafCommands.scaler(1,16,1,0);        //read scaler Clock
    cnafCommands.scaler(1,16,2,0);        //read scaler Event Raw
    cnafCommands.scaler(1,16,3,0);        //read scaler Event Accept
    cnafCommands.scaler(1,16,4,0);        //read scaler Silicon Rate
    cnafCommands.scaler(1,16,5,0);        //read scaler rate divided silicon rate
    cnafCommands.clear(1,16,0,9);        //clear scaler

    hSiliconL=new Histogram("Silicon Left  ", HIST_1D_INT, 2048, "Left Silicon Detector");
    hSiliconLGatedAll=new Histogram("SiliconLGall  ", HIST_1D_INT, 2048, "Left Silicon Gated All");
    hSiliconR    =new Histogram("Silicon Right ", HIST_1D_INT, 2048, "Right Silicon Detector");
    hSiliconRGatedAll=new Histogram("SiliconRGall  ", HIST_1D_INT, 2048, "Right Silicon Gated All");
    hTimeDiffL   =new Histogram("TimeDiff L Raw", HIST_1D_INT, 2048, "TDC Left Silicon");

    hTimeDiffLGatedAll=new Histogram("TimeDiffLGall", HIST_1D_INT, 2048, "TDC Left Silicon Gated All");
    hTimeDiffR   =new Histogram("TimeDiff R Raw", HIST_1D_INT, 2048, "TDC Right Silicon");
    hTimeDiffRGatedAll=new Histogram("TimeDiffRGall", HIST_1D_INT, 2048, "TDC Right Silicon Gated All");

    //Focal Plane Detector
    hCthd       =new Histogram("Cathode       ", HIST_1D_INT, 2048, "Cathode Raw ");
    hFrntPsn     =new Histogram("FrontPosn     ", HIST_1D_INT, 2048, "Front Wire Position");
    hRearPsn     =new Histogram("RearPosn      ", HIST_1D_INT, 2048, "Rear Wire Position");
    hFrntHgh     =new Histogram("FrontHeight   ", HIST_1D_INT, 2048, "Front Wire Pulse Height");
    hRearHgh     =new Histogram("RearHeight    ", HIST_1D_INT, 2048, "Rear Wire Pulse Height");
    hSntrCthd   =new Histogram("ScintCathode  ", HIST_2D_INT,  256, "Cathode vs Scintillator","Scintillator","Cathode");
    hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  256, "Cathode vs Front Position","Front Position","Cathode");
    hFrntSntr   =new Histogram("FrontScint ", HIST_2D_INT,  256, "Scintillator vs Front Position","Front Position","Scintillator");
    hSiliFrnt=new Histogram(   "FrontSili  ", HIST_2D_INT,512,
                            "Silicon E vs. Front Position, gated TDC/PID",
                            "Front Position","Silicon Energy") ;

    //gated on TDC
    hFrntGL1 = new   Histogram("FrontGL1   ",HIST_1D_INT,2048,
                               "Front Gated on TDC Left Peak 1");
    hFrntGL2 = new   Histogram("FrontGL2   ",HIST_1D_INT,2048,
                               "Front Gated on TDC Left Peak 2");
    hFrntGR1 = new   Histogram("FrontGR1   ",HIST_1D_INT,2048,
                               "Front Gated on TDC Right Peak 1");
    hFrntGR2 = new   Histogram("FrontGR2   ",HIST_1D_INT,2048,
                               "Front Gated on TDC Right Peak 2");
    hFrntGL1p = new   Histogram("FrontGL1p ",HIST_1D_INT,2048,
                               "Front Gated All on TDC Left Peak 1");
    hFrntGL2p = new   Histogram("FrontGL2p ",HIST_1D_INT,2048,
                               "Front Gated All on TDC Left Peak 2");
    hFrntGR1p = new   Histogram("FrontGR1p ",HIST_1D_INT,2048,
                               "Front Gated All on TDC Right Peak 1");
    hFrntGR2p = new   Histogram("FrontGR2p ",HIST_1D_INT,2048,
                               "Front Gated All on TDC Right Peak 2");
    hSiliGL1 = new   Histogram("SiliconGL1   ",HIST_1D_INT,2048,
                               "Silicon Gated on TDC Left Peak 1");
    hSiliGL2 = new   Histogram("SiliconGL2   ",HIST_1D_INT,2048,
                               "Silicon Gated on TDC Left Peak 2");
    hSiliGR1 = new   Histogram("SiliconGR1   ",HIST_1D_INT,2048,
                               "Silicon Gated on TDC Right Peak 1");
    hSiliGR2 = new   Histogram("SiliconGR2   ",HIST_1D_INT,2048,
                               "Silicon Gated on TDC Right Peak 2");
    hSiliGL1p = new   Histogram("SiliconGL1p ",HIST_1D_INT,2048,
                               "Silicon Gated All on TDC Left Peak 1");
    hSiliGL2p = new   Histogram("SiliconGL2p ",HIST_1D_INT,2048,
                               "Silicon Gated All on TDC Left Peak 2");
    hSiliGR1p = new   Histogram("SiliconGR1p ",HIST_1D_INT,2048,
                               "Silicon Gated All on TDC Right Peak 1");
    hSiliGR2p = new   Histogram("SiliconGR2p ",HIST_1D_INT,2048,
                               "Silicon Gated All on TDC Right Peak 2");

    hFrontPa0 = new Histogram("FrontPa0",HIST_1D_INT,2048,
                              "Front gated TDC/PID/alpha0");
    hFrontPa1 = new Histogram("FrontPa1",HIST_1D_INT,2048,
                              "Front gated TDC/PID/alpha1");
    hFrontPa2 = new Histogram("FrontPa2",HIST_1D_INT,2048,
                              "Front gated TDC/PID/alpha2");
    hTimeDiffLa0 = new Histogram("TDLa0",HIST_1D_INT,2048,
                              "Time Diff L gated on alpha0");
    hTimeDiffLa1 = new Histogram("TDLa1",HIST_1D_INT,2048,
                              "Time Diff L gated on alpha1");
    hTimeDiffLa2 = new Histogram("TDLa2",HIST_1D_INT,2048,
                              "Time Diff L gated on alpha2");
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
    // gates 1d
    gSiliconL = new Gate("SiliconL", hSiliconL);
    gSiliconR = new Gate("SiliconR", hSiliconR);
    gGood    = new Gate("GoodEvent",hFrntGAll);
    gPeak     = new Gate("Peak", hFrntGAll);
    gTimeL1 = new Gate("TimeDiffL1", hTimeDiffL);
    gTimeL2 = new Gate("TimeDiffL2", hTimeDiffL);
    gTimeR1 = new Gate("TimeDiffR1", hTimeDiffR);
    gTimeR2 = new Gate("TimeDiffR2", hTimeDiffR);
    //gates  2d
    gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
    gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
    gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
    gFrCtProt   =new Gate("FC-1H", hFrntCthd);
    gFrCtDeut   =new Gate("FC-2H", hFrntCthd);
    gFrCtTrit   =new Gate("FC-3H", hFrntCthd);
    gFrCtAlph   =new Gate("FC-He", hFrntCthd);

    alpha0 = new Gate("alpha0", hSiliFrnt);
    alpha1 = new Gate("alpha1", hSiliFrnt);
    alpha2 = new Gate("alpha2", hSiliFrnt);

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
    int ec4FPsn=eFPsn>>2;
    int ec4SilL=eSilL>>2;
    // singles spectra
    hCthd.inc(eCthd);
    hFrntPsn.inc(eFPsn);
    hRearPsn.inc(eRPsn);
    hFrntHgh.inc(eFHgh);
    hRearHgh.inc(eRHgh);
    hSiliconL.inc(eSilL);
    hSiliconR.inc(eSilR);
    //Time of Flight
    hTimeDiffL.inc(eTDL);
    hTimeDiffR.inc(eTDR);
    if (alpha0.inGate(ec4FPsn,ec4SilL)) {
      hTimeDiffLa0.inc(eTDL);
    }
    if (alpha1.inGate(ec4FPsn,ec4SilL)) {
      hTimeDiffLa1.inc(eTDL);
    }
    if (alpha2.inGate(ec4FPsn,ec4SilL)) {
      hTimeDiffLa2.inc(eTDL);
    }
    //singles 2d spectra
    hSntrCthd.inc(ecSntr,ecCthd);
    hFrntCthd.inc(ecFPsn,ecCthd);
    hFrntSntr.inc(ecFPsn,ecSntr);
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
    //gate on TDC peaks
      if (gTimeL1.inGate(eTDL)){
        hFrntGL1.inc(eFPsn);
        hSiliGL1.inc(eSilL);
      }
      if (gTimeR1.inGate(eTDR)){
        hFrntGR1.inc(eFPsn);
        hSiliGR1.inc(eSilR);
      }
      if (gTimeL2.inGate(eTDL)){
        hFrntGL2.inc(eFPsn);
        hSiliGL2.inc(eSilL);
      }
      if (gTimeR2.inGate(eTDR)){
        hFrntGR2.inc(eFPsn);
        hSiliGR2.inc(eSilR);
      }
    // gate on all 3 gates above and the Front wire vs Rear Wire
    if (( gSntrCthd.inGate(ecSntr,ecCthd) )&&
        ( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
        ( gFrntSntr.inGate(ecFPsn,ecSntr) )){
      writeEvent(dataEvent);
      hFrntGAll.inc(eFPsn);
      hRearGAll.inc(eRPsn);
      hSiliconLGatedAll.inc(eSilL);
      hSiliconRGatedAll.inc(eSilR);
      hTimeDiffLGatedAll.inc(eTDL);
      hTimeDiffRGatedAll.inc(eTDR);
      if (gTimeL1.inGate(eTDL)){
        hFrntGL1p.inc(eFPsn);
        hSiliGL1p.inc(eSilL);
        hSiliFrnt.inc(ec4FPsn,ec4SilL);
        if (alpha0.inGate(ec4FPsn,ec4SilL)) {
          hFrontPa0.inc(eFPsn);
        }
        if (alpha1.inGate(ec4FPsn,ec4SilL)) {
          hFrontPa1.inc(eFPsn);
        }
        if (alpha2.inGate(ec4FPsn,ec4SilL)) {
          hFrontPa2.inc(eFPsn);
        }
      }
      if (gTimeR1.inGate(eTDR)){
        hFrntGR1p.inc(eFPsn);
        hSiliGR1p.inc(eSilR);
      }
      if (gTimeL2.inGate(eTDL)){
        hFrntGL2p.inc(eFPsn);
        hSiliGL2p.inc(eSilL);
      }
      if (gTimeR2.inGate(eTDR)){
        hFrntGR2p.inc(eFPsn);
        hSiliGR2p.inc(eSilR);
      }
    }
  }
}
