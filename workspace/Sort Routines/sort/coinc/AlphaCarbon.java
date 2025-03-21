/*
*/


package sort.coinc;
import jam.data.*;
import jam.sort.SortRoutine;
/**
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified June 99 by Rachel Lewis
 *
 * Last Modified by William Bradfield-Smith for run in Nov 99 with Alpha beam
 * on a carbon target.
 */
public class AlphaCarbon extends SortRoutine {


    // ungated spectra
    Histogram hCthd;      //Cathode
    Histogram hAnde;      //Anode
    Histogram hSntr1;     //Scintillator
    Histogram hSntr2;
    Histogram hSntrSum;
    Histogram hFrntPsn;   //Front Wire position
    Histogram hRearPsn;   //Rear wire position
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde;   //Cathode vs Anode
    Histogram hSntrCthd;   //Scintillator vs Cathode
    Histogram hFrntCthd;
    Histogram hFrntAnde;
    Histogram hFrntSntr;
    Histogram hFrntPRearP;

 //   Histogram hSilicon;

    // Gated on Cathode Anode
    Histogram hFrntCthdGCA;
    Histogram hFrntAndeGCA;

    //Gated on Front Cathode
    Histogram hCthdAndeGFC;
    Histogram hFrntAndeGFC;
    //Gated on Front Anode
    Histogram hCthdAndeGFA;
    Histogram hFrntCthdGFA;
    //gated on all
    Histogram hCthdAndeGALL;
    Histogram hFrntCthdGALL;
    Histogram hFrntAndeGALL;

    //gate by Front wire Scintillator
    Histogram hSntrCthdGFS;
    Histogram hFrntCthdGFS;

    //front and rear wire gate on all
    Histogram hFrntGCSF;
    Histogram hRearGCSF;
    Histogram hFrntGAll;
    Histogram hRearGAll;

    //front and rear wire gated on All compressed
    Histogram hcFrntGAll;
    Histogram hcRearGAll;


    //Wire spectra gated on cathode Anode
    Histogram hFrntGCA;
    Histogram hRearGCA;
    //Wire spectra gated on all gates
    Histogram hFrntGALL;
    Histogram hRearGALL;
    //gates 1 d
    Gate gSilicon;
    Gate gCthd;
    Gate gPeak;
    Gate gGood;

    //gates 2 d
    Gate gCthdAnde;
    Gate gFrntCthd;
    Gate gFrntAnde;
    Gate gFrntRear;
    /*    Gate gSntrCthd;
    Gate gFrntSntr;
    Gate gFrntCthd;

*/
    //scalers
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
    Monitor mCthd;
    Monitor mGood;
    Monitor mLive;

    //id numbers for the signals;
    int idMonitor;
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
    int lastEvntAccpt;

    public void initialize() throws Exception {

  //EVENT_SIZE=9;
//  ***check the addresses for these events***
  //               <C, N, A, F>
  cnafCommands.init(1,28,8,26);      //crate dataway Z
  cnafCommands.init(1,28,9,26);      //crate dataway C
  cnafCommands.init(1,30,9,26);      //crate I
  cnafCommands.init(1,3,12,11);      //adc 811 clear
  cnafCommands.init(1,9,12,11);      //adc 811 clear
  cnafCommands.init(1,20,0,10);      //trigger module clear

  //event return id number to be used in sort
  idCthd=cnafCommands.eventRead(1,3,0,0);      //read first channel:Cathode signal
  idAnde=cnafCommands.eventRead(1,3,1,0);      //read Anode
  idScintR=cnafCommands.eventRead(1,3,2,0);      //read first channel:Cathode signal
  idScintL=cnafCommands.eventRead(1,3,3,0);      //read Anode
  idFrntPsn=cnafCommands.eventRead(1,3,4,0);      //read Front TAC
  idRearPsn=cnafCommands.eventRead(1,3,5,0);      //read Rear TAC
  idFrntHgh=cnafCommands.eventRead(1,3,6,0);      //read front wire pulse height
  idRearHgh=cnafCommands.eventRead(1,3,7,0);      //read rear wire pulse height
  idMonitor=cnafCommands.eventRead(1,9,1,0);      //read beam monitor      (slot 9 channel 1)

  cnafCommands.eventCommand(1,3,12,11);        //clear adc
  cnafCommands.eventCommand(1,9,12,11);        //clear adc
  cnafCommands.eventCommand(1,20,0,10);        //clear trigger module
  cnafCommands.scaler(1,16,0,0);        //read scaler BIC
  cnafCommands.scaler(1,16,1,0);        //read scaler Clock
  cnafCommands.scaler(1,16,2,0);        //read scaler Event Raw
  cnafCommands.scaler(1,16,3,0);        //read scaler Event Accept
  cnafCommands.scaler(1,16,4,0);
  cnafCommands.scaler(1,15,5,0);

  cnafCommands.clear(1,16,0,9);        //clear scaler

//  hSilicon    =new Histogram("Silicon     ", HIST_1D_INT, 2048, "Beam Monitor");
  hCthd      =new Histogram("Cathode     ", HIST_1D_INT, 2048, "Cathode Raw ");
  hAnde      =new Histogram("Anode       ", HIST_1D_INT, 2048, "Anode Raw");
  hSntr1      =new Histogram("Scint1      ", HIST_1D_INT, 2048, "Scintillator PMT 1");
  hSntr2      =new Histogram("Scint2      ", HIST_1D_INT, 2048, "Scintillator PMT 2");
  hSntrSum    =new Histogram("ScintSum    ", HIST_1D_INT, 2048, "Scintillator Sum");
  hFrntPsn    =new Histogram("FrontPosn    ", HIST_1D_INT, 2048, "Front Wire Position");
  hRearPsn    =new Histogram("RearPosn     ", HIST_1D_INT, 2048, "Rear Wire Position");
  hFrntHgh    =new Histogram("FrontHeight   ", HIST_1D_INT, 2048, "Front Wire Pulse Height");
  hRearHgh    =new Histogram("RearHeight    ", HIST_1D_INT, 2048, "Rear Wire Pulse Height");
  hFrntPH      =new Histogram("FrontPvsHeight", HIST_2D_INT,  256, "Pulse Height vs Front Position","Front Position","Pulse Height");
  hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,  256, "Pulse Height vs Rear Position","Rear Position", "Pulse Height");
  hCthdAnde   =new Histogram("CathodeAnode  ", HIST_2D_INT,  256, "Cathode vs Anode ","Cathode","Anode");
  hSntrCthd   =new Histogram("ScintCathode  ", HIST_2D_INT,  256, "Cathode vs Scintillator","Scintillator","Cathode");
  hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  256, "Cathode vs Front Position","Front Position","Cathode");
  hFrntAnde   =new Histogram("FrontAnode  ", HIST_2D_INT,  256, "Anode vs Front Position","Front Position","Anode");
  hFrntSntr   =new Histogram("FrontScint ", HIST_2D_INT,  256, "Scintillator vs Front Position","Front Position","Scintillator");
  hFrntPRearP =new Histogram("FrontRear  ", HIST_2D_INT,  256, "Rear Position vs Front Position","Front Position","Rear Position");
//gated on cathode anode
  hFrntCthdGCA =new Histogram("FrontCathodeGCA",HIST_2D_INT,  256,"Front Position vs Cathode -CtAn gate","Front Position","Cathode");
  hFrntAndeGCA =new Histogram("FrontAndeGCA",HIST_2D_INT,  256,"Front Position vs Anode -CtAn gate","Front Position","Anode");
//gated on front cathode
  hCthdAndeGFC =new Histogram("CathodeAndeGFC",HIST_2D_INT,  256,"Cathode vs Anode -FrCa gate","Cathode","Anode");
  hFrntAndeGFC =new Histogram("FrontAnodeGFC",HIST_2D_INT,  256,"Front vs Anode -FrCa gate","Front","Anode");
  //gated on front anode
  hCthdAndeGFA =new Histogram("CathodeAndeGFA",HIST_2D_INT,  256,"Cathode vs Anode -FrAn gate","Cathode","Anode");
  hFrntCthdGFA =new Histogram("FrontCathodeGFA",HIST_2D_INT,  256,"Front vs Cathode -FrAn gate","Front","Cathode");
  //gated on Cathgode Anode
  hFrntGCA   =new Histogram("FrontGCA    ", HIST_1D_INT, 2048, "Front Position - CaAn gate");
  hRearGCA   =new Histogram("RearGCA     ", HIST_1D_INT, 2048, "Rear Position - CaAn gate");

  //gate on 3 gate  2D and 1D
  hCthdAndeGALL =new Histogram("CathodeAndeGALL",HIST_2D_INT,  256,"Cathode vs Anode -all gates","Cathode","Anode");
  hFrntCthdGALL =new Histogram("FrontCathodeGALL",HIST_2D_INT,  256,"Front vs Cathode -all gates","Front","Cathode");
  hFrntAndeGALL =new Histogram("FrontAnodeGALL",HIST_2D_INT,  256,"Front vs Anode -all gates","Front","Anode");
  hFrntGALL     =new Histogram("FrontGAll    ", HIST_1D_INT, 2048, "Front Position - CaAn,FrCa,FrAn,FwRw gates");
  hRearGALL     =new Histogram("RearGAll    ", HIST_1D_INT, 2048, "Rear Position - CaAn,FrCa,FrAn,FwRw gates");
/*  hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, 1024, "Front Position comp - ScCa,FwCa,FwSc,FwRw gates");
  hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, 1024, "Rear Position comp - ScCa,FwCa,FwSc,FwRw gates");
*/
  // gates 1d
  gCthd   =new Gate("Counts", hCthd);
  gPeak   =new Gate("Peak", hFrntGALL);
//  gSilicon    = new Gate("Elastics", hSilicon);
  gGood  =new Gate("GoodEvent",hFrntGALL);
  //gates 2d
  gCthdAnde =new Gate("Ct-An", hCthdAnde); //gate on anode and cathode coincidence spectrum
  gFrntCthd =new Gate("Fr-Ct", hFrntCthd); //gate on good events in front cathode spectrum
  gFrntAnde =new Gate("Fr-An", hFrntAnde); //gate on good events in front anode spectrum
  gFrntRear =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear

/*  //gates  2d
  gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
  gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
  gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode

*/
  hCthdAndeGFC.addGate(gCthdAnde);
  hCthdAndeGFA.addGate(gCthdAnde);
  hCthdAndeGALL.addGate(gCthdAnde);
  hFrntCthdGCA.addGate(gFrntCthd);
  hFrntCthdGFA.addGate(gFrntCthd);
  hFrntCthdGALL.addGate(gFrntCthd);
  hFrntAndeGFC.addGate(gFrntAnde);
  hFrntAndeGCA.addGate(gFrntAnde);
  hFrntAndeGALL.addGate(gFrntAnde);

  //scalers
  sBic      =new Scaler("BIC",0);
  sClck      =new Scaler("Clock",1);
  sEvntRaw    =new Scaler("Event Raw", 2);
  sEvntAccpt  =new Scaler("Event Accept",3);
  sSilRaw    =new Scaler("Monitor Raw", 4);
  sSilAccpt  =new Scaler("Monitor Accept",5);

  //monitors
  mBeam=new Monitor("Beam ",sBic);
  mClck=new Monitor("Clock",sClck);
  mEvntRt=new Monitor("Event Rate",sEvntRaw);
  mCthd=new Monitor("Cathode",gCthd);
  mGood=new Monitor("Good Events",gGood);

  mLive=new Monitor("Live Time", this);
    }
    public void sort(int [] dataEvent) throws Exception {

  //unpack data into convenient names
  int eCthd   =dataEvent[idCthd];
  int eAnde   =dataEvent[idAnde];
  int eSntr1  =dataEvent[idScintR];
  int eSntr2  =dataEvent[idScintL];
  int eFPsn   =dataEvent[idFrntPsn];
  int eRPsn   =dataEvent[idRearPsn];
  int eFHgh   =dataEvent[idFrntHgh];
  int eRHgh   =dataEvent[idRearHgh];
  int eSil    = dataEvent[idMonitor];

  int eSntr=(eSntr1+eSntr2)/2;

  int ecFPsn=eFPsn>>3;
  int ecRPsn=eRPsn>>3;
  int ecFHgh=eFHgh>>3;
  int ecRHgh=eRHgh>>3;

  int ecSntr=eSntr>>3;
  int ecCthd=eCthd>>3;
  int ecAnde=eAnde>>3;


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
  //hSilicon.inc(eSil);

  //singles 2d spectra
    hFrntPH.inc(ecFPsn,ecFHgh);
    hRearPH.inc(ecRPsn,ecRHgh);
    hCthdAnde.inc(ecCthd,ecAnde);
    hSntrCthd.inc(ecSntr,ecCthd);
    hFrntCthd.inc(ecFPsn,ecCthd);
    hFrntAnde.inc(ecFPsn,ecAnde);
    hFrntSntr.inc(ecFPsn,ecSntr);
    hFrntPRearP.inc(ecFPsn,ecRPsn);

  if (gFrntRear.inGate(ecFPsn,ecRPsn) ){ // Front Wire vs Rear Wire requirement

  if (gCthdAnde.inGate(ecCthd,ecAnde) ) {  //gate on Cathode Anode
      hFrntCthdGCA.inc(ecFPsn,ecCthd);
      hFrntAndeGCA.inc(ecFPsn,ecAnde);
      hFrntGCA.inc(eFPsn);
      hRearGCA.inc(eRPsn);
      }
  if (gFrntCthd.inGate(ecFPsn,ecCthd) ) {  //gate on Front wire and cathode
      hCthdAndeGFC.inc(ecCthd,ecAnde);
      hFrntAndeGFC.inc(ecFPsn,ecAnde);
      }
  if (gFrntAnde.inGate(ecFPsn,ecAnde) ) {  //gate on Front Wire and Anode
      hCthdAndeGFA.inc(ecCthd,ecAnde);
      hFrntCthdGFA.inc(ecFPsn,ecCthd);
      }
      //Gates on all gates
  if  ( (gCthdAnde.inGate(ecCthd,ecAnde)) &&
      (gFrntCthd.inGate(ecFPsn,ecCthd)) &&
      (gFrntAnde.inGate(ecFPsn,ecAnde)) &&
      (ecSntr==0)){
      hCthdAndeGALL.inc(ecCthd,ecAnde);
      hFrntCthdGALL.inc(ecFPsn,ecCthd);
      hFrntAndeGALL.inc(ecFPsn,ecAnde);
      hFrntGALL.inc(eFPsn);
      hRearGALL.inc(eRPsn);
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
