/* 
*/
package sort.jac;				
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
 * Modified by Rachel Lewis, July 99
 * This one plots uncorrected histograms for rough gate setting, does presort
 */
public class JacPreSort extends SortRoutine {

    // ungated spectra 
    Histogram hCthd;    
    Histogram hAnde;
    Histogram hSntr1;    
    Histogram hSntr2;   
    Histogram hSntrSum;            
    Histogram hFrntPsn;
    Histogram hRearPsn;
    Histogram hFrntHgh;		//front Wire Pulse Height
    Histogram hRearHgh;		//Rear Wire Pulse Height
    Histogram hFrntPH;	        // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde;  
    Histogram hSntrCthd;        
    Histogram hFrntCthd;
    Histogram hFrntAnde;   
    Histogram hFrntSntr;    
    Histogram hFrntPRearP;    
    Histogram hSntrAnde;

    //gate by scintillator cathode
    Histogram hFrntSntrGSC;    
    Histogram hFrntCthdGSC;    
    Histogram hFrntAndeGSC;   

    //gate by Front wire Cathode
    Histogram hSntrCthdGFC;        
    Histogram hFrntSntrGFC;    

    //gate by Front wire Scintillator
    Histogram hSntrCthdGFS;    
    Histogram hFrntCthdGFS;    
    Histogram hCthdAndeGFSC;
    
    //front and rear wire gate on all    
    Histogram hFrntGAll;
    Histogram hRearGAll;
    
    //gates 1 d
    Gate gCthd;	        
    Gate gFW;
    
    //gates 2 d		 
    Gate gSntrCthd;
    Gate gFrntSntr;    
    Gate gFrntCthd;
    Gate gFrntRear;    
    Gate gFrntAnde;
    Gate gCthdAnde;
         
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
    DataParameter pSlopeC1;
    DataParameter pSlopeS1;
    
    public void initialize() throws Exception {
    
	//EVENT_SIZE=12; 
	setEventSize(12);   	

	hCthd	    	=new Histogram("Cathode     ", HIST_1D, 2048, " Cathode Raw ","channel", "counts"); 
	hAnde	    	=new Histogram("Anode       ", HIST_1D, 2048, " Anode Raw ", "channel", "counts");   
	hSntr1	    	=new Histogram("Scint1      ", HIST_1D, 2048," Scintillator PMT 1", "channel", "counts");  
	hSntr2	    	=new Histogram("Scint2      ",HIST_1D, 2048," Scintillator PMT 2 ", "channel", "counts");  
	hSntrSum	=new Histogram("ScintSum    ",HIST_1D, 2048, " Scintillator Sum ", "channel", "counts");  		 
	hFrntPsn	=new Histogram("FrontPosn   ",HIST_1D, 2048, " Front Wire Position", "channel", "counts");   
	hRearPsn	=new Histogram("RearPosn    ", HIST_1D, 2048, " Rear Wire Position", "channel", "counts");
	hFrntHgh	=new Histogram("FrontHeight ", HIST_1D, 2048, " Front Wire Pulse Height", "channel", "counts");   
	hRearHgh	=new Histogram("RearHeight  ", HIST_1D, 2048, " Rear Wire Pulse Height", "channel", "counts");   
	hFrntPH	  	=new Histogram("FrontPvsH   ", HIST_2D,  256, " Front Position vs Pulse Height", "channel", "counts"); 
	hRearPH	  	=new Histogram("RearPvsH    ", HIST_2D,  256, " Rear Position vs Pulse Height", "channel", "counts"); 

	hCthdAnde   =new Histogram("CathodeAnode", HIST_2D,  256, " Cathode vs Anode ", "cathode", "anode"); 	
	hSntrCthd   =new Histogram("ScintCathode", HIST_2D,  256, " Scintillator vs Cathode ", "scintillator", "cathode"); 
	hFrntCthd   =new Histogram("FrontCathode", HIST_2D,  256, " Front Position vs Cathode ", "front position", "cathode"); 
	hFrntAnde   =new Histogram("FrontAnode  ", HIST_2D,  256, " Front Position vs Anode ", "front position", "anode"); 
	hFrntSntr   =new Histogram("FrontScint  ", HIST_2D,  256, " Front Position vs Scintillator ", "front position", "scintillator"); 
	hFrntPRearP =new Histogram("FrontRear   ", HIST_2D,  256, " Front Position vs Rear Position ", "front position", "rear position"); 
	hSntrAnde   =new Histogram("ScintAnode  ", HIST_2D,  256, " Scintillator  vs Anode ", "scintillator", "anode");
		
	//gate on Scintillator Cathode
	hFrntSntrGSC=new Histogram("FrontScintGSC ", HIST_2D,  256, " Front Position vs Scintillator - ScCa gate", "front position", "scintillator"); 
	hFrntCthdGSC=new Histogram("FrontCathGSC ", HIST_2D,  256, " Front Position vs Cathode - ScCa gate", "front position", "cathode"); 
	hFrntAndeGSC   =new Histogram("FrontAnodeGSC ", HIST_2D,  256, " Front Position vs Anode - ScCa gate ", "front position", "anode"); 

	//gate on Front Wire Cathode
	hSntrCthdGFC=new Histogram("ScintCathGFC ", HIST_2D,  256, " Scintillator vs Cathode - FwCa gate ", "scintillator", "cathode"); 
	hFrntSntrGFC=new Histogram("FrontScintGFC ", HIST_2D,  256, " Front Position vs Scintillator - FwCa gate", "front position", "scintillator"); 
	
	//gate on Front Wire Scintillator
	hSntrCthdGFS=new Histogram("ScintCathGFS ", HIST_2D,  256, " Scintillator vs Cathode - FwSc gate ", "scintillator", "cathode");
	hFrntCthdGFS=new Histogram("FrontCathGFS ", HIST_2D,  256, " Front Position vs Cathode - FwSc gate ", "front position", "cathode"); 
	hCthdAndeGFSC = new Histogram("CathodeAnodeGFSC ", HIST_2D, 256, "Cathode vs Anode - FwSc and ScCa gates ", "cathode", "anode");
	
	//gate on 3 gate 
	hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D, 2048, " Front Position - ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");   
	hRearGAll   =new Histogram("RearGAll    ", HIST_1D, 2048, " Rear Position - ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");   
	
	//gates  2d
	gSntrCthd   =new Gate("Ca-Sc", hSntrCthd); 
	gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);    	
	gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);   
	gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);    
        gFrntAnde   =new Gate("Fw-An", hFrntAndeGSC);
	gCthdAnde   =new Gate("Ca-An", hCthdAndeGFSC);
	
	//add gates to gated spectra	    	
	hFrntSntrGSC.addGate(gFrntSntr);
	hFrntSntrGFC.addGate(gFrntSntr);	

	hSntrCthdGFC.addGate(gSntrCthd);
	hSntrCthdGFS.addGate(gSntrCthd);

	//added gates by Jac Caggiano
	hFrntAndeGSC.addGate(gFrntAnde);
	hCthdAndeGFSC.addGate(gCthdAnde);
	
	hFrntCthdGFS.addGate(gFrntCthd);	
	hFrntCthdGSC.addGate(gFrntCthd);

	sBic	    =new Scaler("BIC",0);	
	sClck	    =new Scaler("Clock",1);
	sEvntRaw    =new Scaler("Event Raw", 2);       
	sEvntAccpt  =new Scaler("Event Accept",3);            
	sSilRaw     =new Scaler("Monitor Raw", 4);       
	sSilAccpt   =new Scaler("Monitor Accept",5);            

	mBeam=new Monitor("Beam ",sBic);			
	mClck=new Monitor("Clock",sClck);		
	mEvntRt=new Monitor("Event Rate",sEvntRaw);
	mCthd=new Monitor("Cathode",gCthd);	
    }
    public void sort(int [] dataEvent) throws Exception{    

    	//unpack data into convenient names
	int eCthd   = dataEvent[0];
	int eAnde   = dataEvent[1];
	int eSntr1  = dataEvent[2];
	int eSntr2  = dataEvent[3];	
	int eFPsn   = dataEvent[4];
	int eRPsn   = dataEvent[5];
	int eFHgh   = dataEvent[6];
	int eRHgh   = dataEvent[7];
	int eSil    = dataEvent[8];
		    	    
	int eSntr=(eSntr1+eSntr2)/2;
	
	int ecFPsn=eFPsn>>3;
	int ecRPsn=eRPsn>>3;	
	int ecFHgh=eFHgh>>3;
	int ecRHgh=eRHgh>>3;	
		
	int ecSntr=eSntr>>3;
	int ecCthd=eCthd>>3;
	int ecAnde=eAnde>>3;	
	
	double frntF;   
	
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

//singles 2d spectra	
	hFrntPH.inc(ecFPsn,ecFHgh);
	hRearPH.inc(ecRPsn,ecRHgh);	
	hCthdAnde.inc(ecCthd,ecAnde);
	hSntrCthd.inc(ecSntr,ecCthd);
	hFrntCthd.inc(ecFPsn,ecCthd);
	hFrntAnde.inc(ecFPsn,ecAnde);
	hFrntSntr.inc(ecFPsn,ecSntr);
	hFrntPRearP.inc(ecFPsn,ecRPsn);
	hSntrAnde.inc(ecSntr,ecAnde);

//gate on front rear
	if ( gFrntRear.inGate(ecFPsn,ecRPsn) ){    

   // gate on Scintillator vs Cathode
	    if ( gSntrCthd.inGate(ecSntr ,ecCthd) ){
		hFrntSntrGSC.inc(ecFPsn, ecSntr);    
	        hFrntCthdGSC.inc(ecFPsn, ecCthd);
		hFrntAndeGSC.inc(ecFPsn, ecAnde);
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
		if ( gSntrCthd.inGate(ecSntr, ecCthd) ) { 		  
		    hCthdAndeGFSC.inc(ecCthd,ecAnde);
		}
	    }
	
	// gate on all 3 gates above and the Front wire vs Rear Wire	
	    if ( gFrntCthd.inGate(ecFPsn,ecCthd) ){
		if ( gSntrCthd.inGate(ecSntr,ecCthd) ){
		    if ( gFrntSntr.inGate(ecFPsn,ecSntr) ){
		      if ( gFrntAnde.inGate(ecFPsn,ecAnde) ){
		        if ( gCthdAnde.inGate(ecCthd,ecAnde) ){
			  hFrntGAll.inc(eFPsn);    
			  hRearGAll.inc(eRPsn);
			  writeEvent(dataEvent);
			}
		      }
		    }
		}    
	    } //end gate-on-all-three
	}//end gate on front rear
    }//end sorting
}//end routine
