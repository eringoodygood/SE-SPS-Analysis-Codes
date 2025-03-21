/*
*/
package sort.camac;
import jam.data.*;
import jam.sort.SortRoutine;
/*
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified June 99 by Rachel Lewis
 */
public class SiliconSolar extends SortRoutine {
    //New section for silicon & timing signals
    Histogram hSilicon;
    Histogram hSolarCell;


    //gates 1 d
    Gate gSilicon;
    //Gate gCthd;
    //Gate gPeak;
    //Gate gGood;
/*
    //gates 2 d
    Gate gSntrCthd;
    Gate gFrntSntr;
    Gate gFrntCthd;
    Gate gFrntRear;
    */
    //scalers
    Scaler sBic;
    Scaler sClck;
    Scaler sEvntRaw;
    Scaler sEvntAccpt;

    //monitors
    Monitor mBeam;
    Monitor mClck;
    Monitor mEvntRt;
    Monitor mCthd;
    Monitor mGood;
    Monitor mLive;        
        
    //id numbers for the signals;
    int idSurfBarr;    
    int idSolarCell;
    int lastEvntAccpt;
    
    public void initialize() throws Exception {
    
  //EVENT_SIZE=9;      
//  ***check the addresses for these events***
  //               <C, N, A, F>
  cnafCommands.init(1,28,8,26);      //crate dataway Z   
  cnafCommands.init(1,28,9,26);      //crate dataway C
  cnafCommands.init(1,30,9,26);      //Sets the crate I (Inhibit)
  cnafCommands.init(1,9,12,11);      //adc 811 clear
  cnafCommands.init(1,20,0,10);      //trigger module clear

      //event return id number to be used in sort 
  //Silicon Detector ADC
  idSurfBarr  = cnafCommands.eventRead(1,9,0,0);      //read beam monitor      (slot 9 channel 0)
  idSolarCell = cnafCommands.eventRead(1,9,1,0);      //read solar cell      (slot 9 channel 1)
  cnafCommands.eventCommand(1,9,12,11);        //clear adc  
  
  //Trigger Module        
  cnafCommands.eventCommand(1,20,0,10);        //clear trigger module

      cnafCommands.scaler(1,16,0,0);        //read scaler BIC
  cnafCommands.scaler(1,16,1,0);        //read scaler Clock
  cnafCommands.scaler(1,16,2,0);        //read scaler Event Raw
  cnafCommands.scaler(1,16,3,0);        //read scaler Event Accept

  cnafCommands.clear(1,16,0,9);        //clear scaler

  hSilicon    =new Histogram("Silicon 50u", HIST_1D_INT, 2048, "Silicon Detector");
  hSolarCell  =new Histogram("Solar Cell ", HIST_1D_INT, 2048, "Solar Cell Detector");

  sBic = new Scaler("BCI",0);
  sClck = new Scaler("Clock",1);
  sEvntRaw = new Scaler("Event Raw",2);
  sEvntAccpt = new Scaler("Event Accept",3);

  //monitors
  mBeam=new Monitor("Beam ",sBic);
  mClck=new Monitor("Clock",sClck);
  mEvntRt=new Monitor("Event Rate",sEvntRaw);

  mLive=new Monitor("Live Time", this);
    }
    public void sort(int [] dataEvent) throws Exception {

  //unpack data into convenient names

  int eSil    = dataEvent[idSurfBarr];
  int eSol    = dataEvent[idSolarCell];
  //System.out.println("TOF: "+eTOF);


  hSilicon.inc(eSil);
  hSolarCell.inc(eSol);

/*
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
  // gated on all 3 gate above
  if (( gSntrCthd.inGate(ecSntr,ecCthd) )&&  
      ( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
      ( gFrntSntr.inGate(ecFPsn,ecSntr) )){  
        hFrntGCSF.inc(eFPsn);    
        hRearGCSF.inc(eRPsn);
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
  }
  */          
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
