/*
*/
package sort.camac;
import jam.data.*;
import jam.sort.SortRoutine;
/*
 * Sort file for Enge SplitPole Online
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified June 99 by Rachel Lewis
 *Modified by William Bradfield-Smith 4/11/99 for low energy 6he and cathode anode coincidience with scintillator veto
 * Modified by dale and rachel august 2000 for use without rear wire*/

public class SplitPoleOffAnodeCathode extends SortRoutine {

    // ungated spectra
    Histogram hCthd;
    Histogram hAnde;
    Histogram hSntr1;
    Histogram hSntr2;
    Histogram hSntrSum;
    Histogram hFrntPsn;
//    Histogram hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
   // Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    //Histogram hRearPH;
    Histogram hCthdAnde;
    Histogram hSntrCthd;
    Histogram hFrntCthd;
    Histogram hFrntAnde;
    Histogram hFrntSntr;

    //Histogram hFrntPRearP;

    //gate by anode cathode
    Histogram hFrntCthdGAC;
    Histogram hFrntAndeGAC;
    
    //gate by cathode front
    Histogram hFrntAndeGFC;
    Histogram hCthdAndeGFC;
    
    //gate by anode front
    Histogram hFrntCthdGAF;
    Histogram hCthdAndeGAF;

    //front and rear wire gate on all
    Histogram hFrntGCSF;
//    Histogram hRearGCSF;
    Histogram hFrntGAll;
//    Histogram hRearGAll;

    //front and rear wire gated on All compressed
    Histogram hcFrntGAll;
//    Histogram hcRearGAll;

    //gates 1 d
    Gate gSilicon;
    Gate gCthd;
    Gate gPeak;
    Gate gGood;

    //gates 2 d
    Gate gFrntCthd;
//    Gate gFrntRear;
    Gate gCthdAnde;
    Gate gFrntAnde;

    //scalers
    Scaler sBic;
    Scaler sClck;
    Scaler sEvntRaw;
    Scaler sEvntAccpt;

    //monitors
    Monitor mBeam;
    Monitor mClck;
    Monitor mEvntRt;
    Monitor mEvntAc;
    Monitor mGood;
    Monitor mLive;

    //id numbers for the signals;
    int idMonitor;
    int idCthd;
    int idAnde;
    int idScintR;
    int idScintL;
    int idFrntPsn;
//    int idRearPsn;
    int idFrntHgh;    //front Wire Pulse Height
//    int idRearHgh;    //Rear Wire Pulse Height
    int idFrntPH;    // position x height y
//    int idRearPH;
    int lastEvntAccpt;

    public void initialize() throws Exception {
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
//      idRearPsn=cnafCommands.eventRead(1,3,5,0);      //read Rear TAC
      idFrntHgh=cnafCommands.eventRead(1,3,6,0);      //read front wire pulse height
//      idRearHgh=cnafCommands.eventRead(1,3,7,0);      //read rear wire pulse height
      idMonitor=cnafCommands.eventRead(1,9,1,0);      //read beam monitor      (slot 9 channel 1)
      cnafCommands.eventCommand(1,3,12,11);        //clear adc
      cnafCommands.eventCommand(1,9,12,11);        //clear adc
      cnafCommands.eventCommand(1,20,0,10);        //clear trigger module
      cnafCommands.scaler(1,16,0,0);        //read scaler BIC
      cnafCommands.scaler(1,16,1,0);        //read scaler Clock
      cnafCommands.scaler(1,16,2,0);        //read scaler Event Raw
      cnafCommands.scaler(1,16,3,0);        //read scaler Event Accept
      cnafCommands.clear(1,16,0,9);        //clear scaler
      
      //histograms
      hCthd = new Histogram("Cathode     ", HIST_1D_INT, 2048, "Cathode Raw ");
      hAnde = new Histogram("Anode       ", HIST_1D_INT, 2048, "Anode Raw");
      hSntr1 = new Histogram("Scint1      ", HIST_1D_INT, 2048, "Scintillator PMT 1");
      hSntr2 = new Histogram("Scint2      ", HIST_1D_INT, 2048,   "Scintillator PMT 2");
      hSntrSum = new Histogram("ScintSum    ", HIST_1D_INT, 2048,  "Scintillator Sum");
      hFrntPsn = new Histogram("FrontPosn    ", HIST_1D_INT, 2048, "Front Wire Position");
//      hRearPsn = new Histogram("RearPosn     ", HIST_1D_INT, 2048, "Rear Wire Position");
      hFrntHgh = new Histogram("FrontHeight   ", HIST_1D_INT, 2048, "Front Wire Pulse Height");
 //     hRearHgh = new Histogram("RearHeight    ", HIST_1D_INT, 2048,  "Rear Wire Pulse Height");
      hFrntPH = new Histogram("FrontPvsHeight", HIST_2D_INT,  256,
                              "Pulse Height vs Front Position",
                              "Front Position","Pulse Height");
   /*  hRearPH = new Histogram("RearPvsHeight ", HIST_2D_INT, 256,
                              "Pulse Height vs Rear Position",
                              "Rear Position", "Pulse Height");*/
      hCthdAnde = new Histogram("CathodeAnode  ", HIST_2D_INT, 256,
                                "Cathode vs Anode ","Cathode","Anode");
      hSntrCthd = new Histogram("ScintCathode  ", HIST_2D_INT, 256,
                                "Cathode vs Scintillator","Scintillator",
                                "Cathode");
      hFrntCthd = new Histogram("FrontCathode  ", HIST_2D_INT, 256,
                                "Cathode vs Front Position","Front Position",
                                "Cathode");
      hFrntAnde = new Histogram("FrontAnode  ", HIST_2D_INT, 256,
                                "Anode vs Front Position","Front Position",
                                "Anode");
      hFrntSntr = new Histogram("FrontScint ", HIST_2D_INT, 256,
                                "Scintillator vs Front Position",
                                "Front Position", "Scintillator");
    /*  hFrntPRearP = new Histogram("FrontRear  ", HIST_2D_INT, 256,
                                  "Rear Position vs Front Position",
                                  "Front Position", "Rear Position");*/
      //gate on Anode Cathode
      hFrntCthdGAC = new Histogram("FrontCathodeGAC ", HIST_2D_INT, 256,
                                   "Cathode vs. Front Position - AnCa gate",
                                   "Front Position", "Cathode");
      hFrntAndeGAC = new Histogram("FrontAnodeGAC ", HIST_2D_INT,  256, 
      		"Anode vs. Front Position - AnCa gate","Front Position","Anode");
     
     //gate on Front Cathode
      hCthdAndeGFC = new Histogram("CthdAndeGFC", HIST_2D_INT, 256,
                                   "Anode vs Cathode- FrCa gate",
                                   "Cathode", "Anode");
      hFrntAndeGFC = new Histogram("FrontAnodeGFC", HIST_2D_INT,  256, 
      		"Anode vs Front Position - FrCa gate","Front Position","Anode");

     //gate on Front Anode
      hCthdAndeGAF = new Histogram("CthdAndeGAF", HIST_2D_INT, 256,
                                   "Anode vs Cathode- FrAn gate",
                                   "Cathode", "Anode");
      hFrntCthdGAF = new Histogram("FrontCthdGAF", HIST_2D_INT,  256, 
      		"Cathode vs. Front Position - FrAn gate","Front Position","Cathode");

      //gate on 2 gates
      hFrntGCSF   =new Histogram("FrontGACF    ", HIST_1D_INT, 2048, "Front Position - AnCa,FwCa gates");
  //    hRearGCSF   =new Histogram("RearGACF    ", HIST_1D_INT, 2048, "Rear Position - AnCa,FwCa gates");

      //gate on 3 gates
      hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, 2048, "Front Position -AnCa,FwCa,FwRw gates");
  //    hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, 2048, "Rear Position - AnCa,FwCa,FwRw gates");
      hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, 512, "Front Position comp - ScCa,FwCa,FwSc,FwRw gates");
   //   hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, 512, "Rear Position comp - ScCa,FwCa,FwSc,FwRw gates");

      // gates 1d
      gCthd    =new Gate("Counts", hCthd);
      gPeak    =new Gate("Peak", hFrntGCSF);
      gGood    =new Gate("GoodEvent",hFrntGAll);

      //gates  2d
      gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
//      gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
      gCthdAnde   =new Gate("An-Ca", hCthdAnde);      //gate on Anode Cathode
      gFrntAnde	  =new Gate("Fr-An", hFrntAnde);
      hFrntCthdGAC.addGate(gFrntCthd);
      hFrntAndeGAC.addGate(gFrntAnde);    
      hFrntCthdGAF.addGate(gFrntCthd);
      hFrntAndeGFC.addGate(gFrntAnde);
      hCthdAndeGAF.addGate(gCthdAnde);
      hCthdAndeGFC.addGate(gCthdAnde);
      
      //scalers
      sBic      =new Scaler("BIC",0);
      sClck      =new Scaler("Clock",1);
      sEvntRaw    =new Scaler("Event Raw", 2);
      sEvntAccpt  =new Scaler("Event Accept",3);

      //monitors
      mBeam=new Monitor("Beam ",sBic);
      mClck=new Monitor("Clock",sClck);
      mEvntRt=new Monitor("Event raw",sEvntRaw);
      mEvntAc =new Monitor("Event Accepted",gCthd);
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
//      int eRPsn   =dataEvent[idRearPsn];
      int eFHgh   =dataEvent[idFrntHgh];
//      int eRHgh   =dataEvent[idRearHgh];
      int eSil    = dataEvent[idMonitor];
      int eSntr=(eSntr1+eSntr2)/2;
      int ecFPsn=eFPsn>>3;
   //   int ecRPsn=eRPsn>>3;
      int ecFHgh=eFHgh>>3;
     // int ecRHgh=eRHgh>>3;
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
      //hRearPsn.inc(eRPsn);
      hFrntHgh.inc(eFHgh);
      //hRearHgh.inc(eRHgh);
      //singles 2d spectra
      hFrntPH.inc(ecFPsn,ecFHgh);
      //hRearPH.inc(ecRPsn,ecRHgh);
      hCthdAnde.inc(ecCthd,ecAnde);
      hSntrCthd.inc(ecSntr,ecCthd);
      hFrntCthd.inc(ecFPsn,ecCthd);
      hFrntAnde.inc(ecFPsn,ecAnde);
      
      hFrntSntr.inc(ecFPsn,ecSntr);
    //  hFrntPRearP.inc(ecFPsn,ecRPsn);

  // gated on all 2 gate above
  if (( gCthdAnde.inGate(ecCthd,ecAnde) )&&
      ( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
      ( gFrntAnde.inGate(ecFPsn,ecAnde) )){
        hFrntGCSF.inc(eFPsn);
   //     hRearGCSF.inc(eRPsn);
        writeEvent(dataEvent);
  }
  
  if (gCthdAnde.inGate(ecCthd,ecAnde)){
  	hFrntAndeGAC.inc(ecFPsn,ecAnde);
  	hFrntCthdGAC.inc(ecFPsn,ecCthd);
  }
  if (gFrntAnde.inGate(ecFPsn,ecAnde)){
  	hCthdAndeGAF.inc(ecCthd,ecAnde);
  	hFrntCthdGAF.inc(ecFPsn,ecCthd);
  }
  if (gFrntCthd.inGate(ecFPsn,ecCthd)){
  	hCthdAndeGFC.inc(ecCthd,ecAnde);
  	hFrntAndeGFC.inc(ecFPsn,ecAnde);
  }

  // gate on all 2 gates above and the Front wire vs Rear Wire
  if (( gCthdAnde.inGate(ecCthd,ecAnde) )&&
      ( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
      ( gFrntAnde.inGate(ecFPsn,ecAnde) )//&&
      /*( gFrntRear.inGate(ecFPsn,ecRPsn) )*/){
        hFrntGAll.inc(eFPsn);
      //  hRearGAll.inc(eRPsn);
        hcFrntGAll.inc(eFPsn>>2);
      //  hcRearGAll.inc(eRPsn>>2);
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
