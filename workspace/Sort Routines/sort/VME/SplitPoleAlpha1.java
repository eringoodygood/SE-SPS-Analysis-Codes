package sort.VME;
import jam.data.*;
import jam.sort.SortRoutine;

/*
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified September 2000 by Dale Visser
 */
public class SplitPoleAlpha1 extends SortRoutine {

    // ungated spectra
    Histogram hCthd,hAnde,hSntr1,hSntr2,hSntrSum, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hSntrCthd, hFrntCthd, hFrntAnde, hFrntSntr, hFrntPRearP;
    Histogram hSilicon, hNaI1, hNaI2;
        
    Histogram hFrntSntrGSC, hFrntCthdGSC;//gate by scintillator cathode    
    Histogram hSntrCthdGFC, hFrntSntrGFC;//gate by Front wire Cathode    
    Histogram hSntrCthdGFS, hFrntCthdGFS;//gate by Front wire Scintillator    
    Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll;//front and rear wire gate on all    
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed
    
    Gate gSilicon, gCthd, gPeak, gGood;//gates 1 d    
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear;//gates 2 d    
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers    
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors

    //id numbers for the signals;
    final int idCthd=0;
    final int idAnde=1;
    final int idScintR=2;
    final int idScintL=3;
    final int idFrntPsn=4;
    final int idRearPsn=5;
    final int idFrntHgh=6;    //front Wire Pulse Height
    final int idRearHgh=7;    //Rear Wire Pulse Height
	final int idSilicon=8;
	final int idNaI1=9;
	final int idNaI2=10;
	final int NUM_PARAMETERS=11;   
    
    int lastEvntAccpt;
    
    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS=512;//number of channels in compressed position histogram
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
	//amount of bits to shift for compression
	final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
	final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    public void initialize() throws Exception {
    	System.err.println("# Parameters: "+NUM_PARAMETERS);
    	System.err.println("ADC channels: "+ADC_CHANNELS);
    	System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
    	System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);
		setEventSize(NUM_PARAMETERS);
  		hCthd      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS, "Cathode Raw ");
  		hAnde      =new Histogram("Anode       ", HIST_1D_INT, ADC_CHANNELS, "Anode Raw");
  		hSntr1      =new Histogram("Scint1      ", HIST_1D_INT, ADC_CHANNELS, "Scintillator PMT 1");
  		hSntr2      =new Histogram("Scint2      ", HIST_1D_INT, ADC_CHANNELS, "Scintillator PMT 2");
  		hSntrSum    =new Histogram("ScintSum    ", HIST_1D_INT, ADC_CHANNELS, "Scintillator Sum");
  		hFrntPsn    =new Histogram("FrontPosn    ", HIST_1D_INT, ADC_CHANNELS, "Front Wire Position");
  		hRearPsn    =new Histogram("RearPosn     ", HIST_1D_INT, ADC_CHANNELS, "Rear Wire Position");
  		hFrntHgh    =new Histogram("FrontHeight   ", HIST_1D_INT, ADC_CHANNELS, "Front Wire Pulse Height");
  		hRearHgh    =new Histogram("RearHeight    ", HIST_1D_INT, ADC_CHANNELS, "Rear Wire Pulse Height");
		hSilicon = new Histogram("Silicon     ", HIST_1D_INT, ADC_CHANNELS, "Beam Monitor");
		hNaI1 = new Histogram("NaI 1", HIST_1D_INT, ADC_CHANNELS, "NaI Detector 1");
		hNaI2 = new Histogram("NaI 2", HIST_1D_INT, ADC_CHANNELS, "NaI Detector 2");
  		hFrntPH      =new Histogram("FrontPvsHeight", HIST_2D_INT,  TWO_D_CHANNELS, "Pulse Height vs Front Position","Front Position","Pulse Height");
  		hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,  TWO_D_CHANNELS, "Pulse Height vs Rear Position","Rear Position", "Pulse Height");
  		hCthdAnde   =new Histogram("CathodeAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Anode ","Cathode","Anode");
  		hSntrCthd   =new Histogram("ScintCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator","Scintillator","Cathode");
  		hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position","Front Position","Cathode");
  		hFrntAnde   =new Histogram("FrontAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Front Position","Front Position","Anode");
  		hFrntSntr   =new Histogram("FrontScint ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position","Front Position","Scintillator");
  		hFrntPRearP =new Histogram("FrontRear  ", HIST_2D_INT,  TWO_D_CHANNELS, "Rear Position vs Front Position","Front Position","Rear Position");
  		//gate on Scintillator Cathode
  		hFrntSntrGSC=new Histogram("FrontScintGSC ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position - ScCa gate","Front Position", "Scintillator");
  		hFrntCthdGSC=new Histogram("FrontCathodeGSC", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - ScCa gate","Front Position","Cathode");
      	//gate on Front Wire Cathode
  		hSntrCthdGFC=new Histogram("ScintCathodeGFC", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator - FwCa gate", "Scintillator","Cathode");
  		hFrntSntrGFC=new Histogram("FrontScintGFC", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position - FwCa gate","Front Position", "Scintillator");
  		//gate on Front Wire Scintillator
  		hSntrCthdGFS=new Histogram("ScintCathodeGFS", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator - FwSc gate","Scintillator","Cathode");
  		hFrntCthdGFS=new Histogram("FrontCathodeGFS ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - FwSc gate ","Front Position","Cathode");
  		//gated on 3 gates
  		hFrntGCSF   =new Histogram("FrontGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc gates");
    	hRearGCSF   =new Histogram(   "RearGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc gates");
  		hFrntRearGCSF=new Histogram("FRGateCSF  ",HIST_2D_INT, TWO_D_CHANNELS,"Front vs. Rear - ScCa, FwCa, FwSc gates");
  		//gated on 4 gates
  		hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
  		hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
  		hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
  		hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");

  		// gates 1d
  		gCthd   =new Gate("Counts", hCthd);
  		gPeak   =new Gate("Peak", hFrntGCSF);
  		gSilicon    = new Gate("Elastics", hSilicon);
  		gGood  =new Gate("GoodEvent",hFrntGAll);
		//gates  2d
  		gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
  		gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
  		gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
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
  		sScint    =new Scaler("Scintillator", 4);
  		sCathode  =new Scaler("Cathode",5);

  		//monitors
  		mBeam=new Monitor("Beam ",sBic);
  		mClck=new Monitor("Clock",sClck);
  		mEvntRaw=new Monitor("Raw Events",sEvntRaw);
  		mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
  		mScint=new Monitor("Scintillator",sScint);
  		mCathode=new Monitor("Cathode",sCathode);
  		Monitor mLiveTime=new Monitor("Live Time", this);
   }
   
    public void sort(int [] dataEvent) throws Exception {
  		//unpack data into convenient names
  		int eCthd   =dataEvent[idCthd];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idCthd);
  		int eAnde   =dataEvent[idAnde];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idAnde);
  		int eSntr1  =dataEvent[idScintR];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idScintR);
  		int eSntr2  =dataEvent[idScintL];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idScintL);
  		int eFPsn   =dataEvent[idFrntPsn];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idFrntPsn);
  		int eRPsn   =dataEvent[idRearPsn];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idRearPsn);
  		int eFHgh   =dataEvent[idFrntHgh];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idFrntHgh);
  		int eRHgh   =dataEvent[idRearHgh];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idRearHgh);
  		int eSil    = dataEvent[idSilicon];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idSilicon);
  		int eNaI1 = dataEvent[idNaI1];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idNaI1);
  		int eNaI2 = dataEvent[idNaI2];
  		//System.err.println(getClass().getName()+".sort() accessed index "+idNaI2);

		int eSntr=(int)Math.round(Math.sqrt(eSntr1*eSntr2));

		int ecFPsn=eFPsn>>TWO_D_FACTOR;
  		int ecRPsn=eRPsn>>TWO_D_FACTOR;
  		int ecFHgh=eFHgh>>TWO_D_FACTOR;
  		int ecRHgh=eRHgh>>TWO_D_FACTOR;
  		int ecSntr=eSntr>>TWO_D_FACTOR;
  		int ecCthd=eCthd>>TWO_D_FACTOR;
  		int ecAnde=eAnde>>TWO_D_FACTOR;

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
  		hSilicon.inc(eSil);
  		hNaI1.inc(eNaI1);
  		hNaI2.inc(eNaI2);

  		//singles 2d spectra
  		hFrntPH.inc(ecFPsn,ecFHgh);
  		hRearPH.inc(ecRPsn,ecRHgh);
  		hCthdAnde.inc(ecCthd,ecAnde);
  		hSntrCthd.inc(ecSntr,ecCthd);
  		hFrntCthd.inc(ecFPsn,ecCthd);
  		hFrntAnde.inc(ecFPsn,ecAnde);
  		hFrntSntr.inc(ecFPsn,ecSntr);
  		hFrntPRearP.inc(ecFPsn,ecRPsn);

  		if ( gSntrCthd.inGate(ecSntr,ecCthd) ){// gate on Scintillator vs Cathode
      		hFrntSntrGSC.inc(ecFPsn,ecSntr);
      		hFrntCthdGSC.inc(ecFPsn,ecCthd);
  		}
   		if ( gFrntCthd.inGate(ecFPsn,ecCthd) ){// gate on Front Wire Position vs Cathode
      		hSntrCthdGFC.inc(ecSntr,ecCthd);
      		hFrntSntrGFC.inc(ecFPsn,ecSntr);
  		}   
  		if ( gFrntSntr.inGate(ecFPsn,ecSntr) ){// gate on Front Wire Position vs Scintillator
      		hSntrCthdGFS.inc(ecSntr,ecCthd);
      		hFrntCthdGFS.inc(ecFPsn,ecCthd);
  		}
  		  		
  		if (( gSntrCthd.inGate(ecSntr,ecCthd) )&&
      			( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
      			( gFrntSntr.inGate(ecFPsn,ecSntr) )){// gated on all 3 gate above
        	hFrntGCSF.inc(eFPsn);
        	hRearGCSF.inc(eRPsn);
        	hFrntRearGCSF.inc(ecFPsn,ecRPsn);
  		}
  		// gate on all 3 gates above and the Front wire vs Rear Wire
  		if (( gSntrCthd.inGate(ecSntr,ecCthd) )&&
      			( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
      			( gFrntSntr.inGate(ecFPsn,ecSntr) )&&
      			( gFrntRear.inGate(ecFPsn,ecRPsn) )){
        	hFrntGAll.inc(eFPsn);
        	hRearGAll.inc(eRPsn);
        	hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
        	hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
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
        if (((double)(mEvntRaw.getValue()))>0.0){
          return 100.0*rateEvntAccpt/mEvntRaw.getValue();
        } else {
          return 0.0;
        }
      } else {
        return 50.0;
      }
    } 
}
