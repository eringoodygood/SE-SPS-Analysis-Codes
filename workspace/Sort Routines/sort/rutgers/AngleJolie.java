/* 
*/
package sort.rutgers;				
import jam.data.*;
import jam.sort.SortRoutine;
/**
 * Sort file for Enge SplitPole Focal Plane detector, no scintillator, plus Ge detector
 * Peter Parker Astro Physics Group at Yale University with Jolie Cizewski group at Rutgers
 * 
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * @author Ken Swartz
 * @author Alan Chen  
 * @author Dale Visser
 * last modified May 99
 * modified by Rachel Lewis, June 99: 
 * This one calculates angular corrections for cathode and anode detectors
 */
public class AngleJolie extends SortRoutine {

    final int NUM_GATES = 5;

    double slopeC;
    double slopeA;
    double offset;

    // ungated focal plane spectra 
    Histogram hMonitor;    
    Histogram hCthd;    
    Histogram hAnde;
    Histogram hFrntPsn;
    Histogram hRearPsn;
    Histogram hFrntHgh;		//front Wire Pulse Height
    Histogram hRearHgh;		//rear Wire Pulse Height
    Histogram hFrntPH;		//front position x height y
    Histogram hRearPH;
    Histogram hCthdAnde;  
    Histogram hFrntCthd;
    Histogram hFrntAnde;   
    Histogram hFrntPRearP;    
    //gate focal spectra
    Histogram hFrntAndeGFC;
    Histogram hCthdAndeGFC;
    Histogram hFrntCthdGFA;
    Histogram hCthdAndeGFA;
    Histogram hFrntCthdGCA;
    Histogram hFrntAndeGCA;
    //gamma ray spectra
    Histogram hGamma;
    Histogram hGamTac;
    Histogram hGamRel;
    Histogram hGamRan;
    Histogram hGamPID;                
    Histogram hGamRelPID;
    Histogram hGamRanPID;
    
    //id numbers for the signals;
    int idMonitor;    
    int idCthd;
    int idAnde;
    int idFrntPsn;
    int idRearPsn;
    int idFrntHgh;		//front Wire Pulse Height
    int idRearHgh;		//Rear Wire Pulse Height
    int idFrntPH;		// position x height y
    int idRearPH;
    int idGamma;
    int idGamTac;    

    //front and rear wire gate on all    
    Histogram hFrntGAll;
    Histogram hRearGAll;

//for angular correction
    Histogram [] hRearPeak = new Histogram[NUM_GATES]; 
    Histogram [] hCthdTheta = new Histogram[NUM_GATES];   	        
    Histogram [] hCthdThNew = new Histogram[NUM_GATES]; 
    Histogram [] hAndeTheta = new Histogram[NUM_GATES];   	        
    Histogram [] hAndeThNew = new Histogram[NUM_GATES]; 

    Histogram hCthdNew;
    Histogram hAndeNew;
    Histogram hFrntCthdN;
    Histogram hFrntAndeN;
    Histogram hCthdAndeN;
    
    //define gates        
    Gate gSntrCthd;
    Gate gFrntSntr;    
    Gate gFrntCthd;
    Gate gFrntAnde;
    Gate gFrntRear;
    Gate gCthdAnde;    

    Gate gCthd;	        
    Gate gPeak;
    Gate gGamRel;	        
    Gate gGamRan;

    Gate [] gPeakF= new Gate[NUM_GATES];

    Scaler sEvntRaw;        
    Scaler sEvntAccpt;            
    Scaler sBic;
    Scaler sClck;
    
    Monitor mBeam;
    Monitor mEvntRt;
    Monitor mClck;            
    Monitor mCthd;

    //parameters
    DataParameter pSlopeC;
    DataParameter pSlopeA;
    DataParameter pOffset;
			                        
    public void initialize() throws Exception {

	//EVENT_SIZE=12;	    	
	
	cnafCommands.init(1,28,8,26);	    //crate dataway Z 	
	cnafCommands.init(1,28,9,26);	    //crate dataway C
	cnafCommands.init(1,30,9,26);	    //crate I
	cnafCommands.init(1,3,12,11);	    //adc 811 clear
	cnafCommands.init(1,9,12,11);	    //adc 811 clear
	cnafCommands.init(1,20,0,10);	    //trigger module clear
	
	//event return id number to be used in sort 
	idCthd=cnafCommands.eventRead(1,3,0,0);	    //read first channel:Cathode signal
	idAnde=cnafCommands.eventRead(1,3,1,0);	    //read Anode
	idFrntPsn=cnafCommands.eventRead(1,3,4,0);	    //read Front TAC
	idRearPsn=cnafCommands.eventRead(1,3,5,0);	    //read Rear TAC
	idFrntHgh=cnafCommands.eventRead(1,3,6,0);	    //read front wire pulse height
	idRearHgh=cnafCommands.eventRead(1,3,7,0);	    //read rear wire pulse height
	idGamma=cnafCommands.eventRead(1,9,0,0);	    //read gamma ray energy (slot 9 channel 0)
	idMonitor=cnafCommands.eventRead(1,9,1,0);      //read beam monitor	    (slot 9 channel 1)
	idGamTac=cnafCommands.eventRead(1,9,2,0);	    //read gamma ray Tac    (slot 9 channel 2)	
	
    	cnafCommands.eventCommand(1,3,12,11);		    //clear adc
	cnafCommands.eventCommand(1,9,12,11);		    //clear adc
	cnafCommands.eventCommand(1,20,0,10);		    //clear trigger module

    	cnafCommands.scaler(1,16,0,0);		    //read scaler BIC
	cnafCommands.scaler(1,16,1,0);		    //read scaler Clock
	cnafCommands.scaler(1,16,2,0);		    //read scaler Event Raw
	cnafCommands.scaler(1,16,3,0);		    //read scaler Event Accept
	
	cnafCommands.clear(1,16,0,9);		    //clear scaler

	hMonitor    = new Histogram("Beam Monitor    ", HIST_1D, 2048,"Surface Barrier Beam Monitor");
	hMonitor.setLabelX("Particle Energy");
	hCthd	    = new Histogram("Cathode Energy  ", HIST_1D, 2048, "Cathode Energy Ungated"); 
	hCthd.setLabelX("Energy");
	hAnde	    = new Histogram("Anode Energy    ", HIST_1D, 2048, "Anode Raw");  
	hAnde.setLabelX("Energy"); 
	hFrntPsn    = new Histogram("Front Position  ", HIST_1D, 2048, "Front Wire Position");
	hFrntPsn.setLabelX("Position");
	hRearPsn    = new Histogram("Rear Position   ", HIST_1D, 2048, "Rear Wire Position");
	hRearPsn.setLabelX("Position");
	hFrntHgh    = new Histogram("Front Height    ", HIST_1D, 2048, "Front Wire Pulse Height");
	hFrntHgh.setLabelX("Pulse Height");
	hRearHgh    = new Histogram("Rear Height     ", HIST_1D, 2048, "Rear Wire Pulse Height");
	hRearHgh.setLabelX("Pulse Height");
	hFrntCthd   = new Histogram("Cathode vs Front", HIST_2D,  256, "Cathode Energy vs Front Wire Position");
	hFrntCthd.setLabelY("Cathode Energy");
	hFrntCthd.setLabelX("Position");
	hFrntAnde   = new Histogram("Anode vs Front  ", HIST_2D,  256, "Anode Energy vs Front Position");
	hFrntAnde.setLabelY("Anode Energy");
	hFrntAnde.setLabelX("Position");
	hCthdAnde   = new Histogram("Anode vs Cathode", HIST_2D,  256, "Anode Energy vs Cathode Energy"); 
	hCthdAnde.setLabelY("Anode Energy");
	hCthdAnde.setLabelX("Cathode Energy");
	hFrntAndeGFC= new Histogram("AndevsFrnt:CthFr", HIST_2D,  256, "Anode Energy vs Front Position, Gated on Cathode vs Front");
	hFrntAndeGFC.setLabelY("Anode Energy");
	hFrntAndeGFC.setLabelX("Position");
	hCthdAndeGFC= new Histogram("AndevsCthd:CthFr", HIST_2D,  256, "Anode Energy vs Cathode Energy, Gated on Cathode vs Front");
	hCthdAndeGFC.setLabelY("Anode Energy");
	hCthdAndeGFC.setLabelX("Cathode Energy");
	hFrntCthdGFA= new Histogram("ChtdvsFrnt:AndFr", HIST_2D,  256, "Cathode Energy vs Front Position, Gated on Anode vs Front");
	hFrntCthdGFA.setLabelY("Cathode Energy");
	hFrntCthdGFA.setLabelX("Position");
	hCthdAndeGFA= new Histogram("AndevsCthd:AndFr", HIST_2D,  256, "Anode Energy vs Cathode Energy, Gated on Anode vs Front");
	hCthdAndeGFA.setLabelY("Anode Energy");
	hCthdAndeGFA.setLabelX("Cathode Energy");
	hFrntCthdGCA= new Histogram("CthdvsFrnt:AnCth", HIST_2D,  256, "Cathode Energy vs Front Position, Gated on Anode vs Cathode");
	hFrntCthdGCA.setLabelY("Cathode Energy");
	hFrntCthdGCA.setLabelX("Position");
	hFrntAndeGCA= new Histogram("AndevsFrnt:AnCth", HIST_2D,  256, "Anode Energy vs Front Position, Gated on Anode vs Cathode");
	hFrntAndeGCA.setLabelY("Anode Energy");
	hFrntAndeGCA.setLabelX("Position");
	hFrntPH	    = new Histogram("Frnt H vs P ", HIST_2D,  256, "Front Pulse Height vs Position");
	hFrntPH.setLabelY("Pulse Height");
	hFrntPH.setLabelX("Position");
	hRearPH	    = new Histogram("Rear H vs P ", HIST_2D,  256, "Rear Pulse Height vs Postion"); 
	hRearPH.setLabelY("Pulse Height");
	hRearPH.setLabelX("Position");
	hFrntPRearP = new Histogram("Rear vs Frnt", HIST_2D,  256, "Rear Position vs Front Position");
	hFrntPRearP.setLabelY("Rear Position");
	hFrntPRearP.setLabelX("Front Position");
											
	//gate on 3 gate 
	hFrntGAll   = new Histogram("FrntGAll        ", HIST_1D, 2048, " Front Position - CthPos,AnPos,CthAn,FrntRear gates");   
	hRearGAll   = new Histogram("RearGAll        ", HIST_1D, 2048, " Rear Position -  CthPos,AnPos,CthAn,FrntRear gates");   

	hCthdNew        =new Histogram("CthdNew    ", HIST_1D, 2048, " Cathode Corrected", "channel", "counts");   		
	hAndeNew	=new Histogram("AnodeNew   ", HIST_1D, 2048, " Anode Corrected ", "channel", "counts");   
	hFrntCthdN      =new Histogram("FrntCthdNew", HIST_2D, 256, " Front vs. Cathode Corrected ", "front position", "cathode");   		
	hFrntAndeN      =new Histogram("FrntAndeNew", HIST_2D,  256, " Front Position vs Anode Corrected", "front position", "anode"); 
	hCthdAndeN      =new Histogram("CthdAndeNew", HIST_2D,  256, " Cathode vs Anode Corrected", "cathode","anode"); 

	for (int i=0;i<NUM_GATES;i++){
	    hRearPeak[i]   =new Histogram("RearPeak"+i, HIST_1D, 2048, " Rear Position - Peak "+i);
	    hCthdTheta[i]  =new Histogram("CathThet"+i, HIST_2D, 256,  " Cathode  vs. Theta - Peak"+i,"Theta","Cathode");
	    hCthdThNew[i]  =new Histogram("CathThNew"+i, HIST_2D, 256,  " Cathode  vs. Theta Corrected - Peak"+i,"Theta","Cathode");
	    hAndeTheta[i]  =new Histogram("AnodeThet"+i, HIST_2D, 256,  " Anode  vs. Theta - Peak"+i,"Theta","Anode");
	    hAndeThNew[i]   =new Histogram("AnThetNew"+i, HIST_2D, 256,  " Anode  vs. Theta Corrected - Peak"+i,"Theta","Anode");
	}	
			
	//gamma ray spectra 
	hGamma      = new Histogram("Gamma Ray Energy", HIST_1D, 2048, "Gamma Ray Energy");
	hGamma.setLabelX("Energy");
	hGamTac     = new Histogram("Gamma Ray TAC", HIST_1D, 2048, "Gamma Ray TAC with focal plane");
	hGamTac.setLabelX("Time");
	
	hGamRel     = new Histogram("Gamma E : Real  ", HIST_1D, 2048, "Gamma Ray Energy Gated on Reals");
	hGamRel.setLabelX("Energy");
	hGamRan	    = new Histogram("Gamma E : Random", HIST_1D, 2048, " Gamma Ray Energy Gated on Randoms");	
	hGamRan.setLabelX("Energy");	
	hGamPID  = new Histogram("GammaE:PID", HIST_1D, 2048, " Gamma Ray Energy -  CthPos,AnPos,CthAn,FrntRear gates");
	hGamPID.setLabelX("Energy");			
	hGamRelPID  = new Histogram("GammaE:Real,PID", HIST_1D, 2048, " Gamma Ray Energy -  Real, CthPos,AnPos,CthAn,FrntRear gates");
	hGamRelPID.setLabelX("Energy");		
	hGamRanPID  = new Histogram("GammaE:Randm,PID", HIST_1D, 2048, " Gamma Ray Energy -  Random, CthPos,AnPos,CthAn,FrntRear gates");	
	hGamRanPID.setLabelX("Energy");		
	
	// 2d gate
	gCthdAnde   = new Gate("Anode vs Cathode", hCthdAnde);
	hCthdAndeGFC.addGate(gCthdAnde);
	hCthdAndeGFA.addGate(gCthdAnde);
	gFrntCthd   = new Gate("Cathode vs Front", hFrntCthd);
	hFrntCthdGFA.addGate(gFrntCthd);
	hFrntCthdGCA.addGate(gFrntCthd);
	gFrntAnde   = new Gate("Anode vs Front  ", hFrntAnde);
	hFrntAndeGFC.addGate(gFrntAnde);
	hFrntAndeGCA.addGate(gFrntAnde);
	gFrntRear   = new Gate("Rear vs Front   ", hFrntPRearP);    
	gCthd   = new Gate("Peak", hCthd);    
	
	gGamRel   = new Gate("Real",   hGamTac);    
	gGamRan   = new Gate("Random", hGamTac);    

//add gates to new spectra
	hCthdAndeN.addGate(gCthdAnde);
	hFrntAndeN.addGate(gFrntAnde);
	hFrntCthdN.addGate(gFrntCthd);

	sBic	    = new Scaler("BIC",0);	
	sClck	    = new Scaler("Clock",1);		
	sEvntRaw    = new Scaler("Event Raw", 2);       
	sEvntAccpt  = new Scaler("Event Accept",3);            

	pOffset=new DataParameter("Offset");					
	pSlopeC=new DataParameter("SlopeCath");
	pSlopeA=new DataParameter("SlopeAnode");
	
	mBeam	= new Monitor("Beam ",sBic);			
	mClck	= new Monitor("Clock",sClck);		
	mEvntRt	= new Monitor("Event Rate",sEvntRaw);
	mCthd	= new Monitor("Cathode",gCthd);	

    }
    /**
     * Sort routine 
     */
    public void sort(int [] dataEvent) throws Exception {    

	//get parameter values
	offset=pOffset.getValue();
	slopeC=pSlopeC.getValue();
	slopeA=pSlopeA.getValue();

	int eMon    =dataEvent[idMonitor];        
	int eCthd   =dataEvent[idCthd];
	int eAnde   =dataEvent[idAnde];
	int eFPsn   =dataEvent[idFrntPsn];
	int eRPsn   =dataEvent[idRearPsn];
	int eFHgh   =dataEvent[idFrntHgh];
	int eRHgh   =dataEvent[idRearHgh];
	int eGamma  =dataEvent[idGamma];
	int eGamTac =dataEvent[idGamTac];

	int ecFPsn=eFPsn>>3;
	int ecRPsn=eRPsn>>3;	
	int ecFHgh=eFHgh>>3;
	int ecRHgh=eRHgh>>3;	
	int ecCthd=eCthd>>3;
	int ecAnde=eAnde>>3;	
    
	// singles spectra    
	hMonitor.inc(eMon);
	hCthd.inc(eCthd);
	hAnde.inc(eAnde);
	hFrntPsn.inc(eFPsn);
	hRearPsn.inc(eRPsn);
	hFrntHgh.inc(eFHgh);
	hRearHgh.inc(eRHgh);
	hGamma.inc(eGamma);	
	hGamTac.inc(eGamTac);

	//singles 2d spectra	
	hFrntPH.inc(ecFPsn,ecFHgh);
	hRearPH.inc(ecRPsn,ecRHgh);	
	hCthdAnde.inc(ecCthd,ecAnde);
	hFrntCthd.inc(ecFPsn,ecCthd);
	hFrntAnde.inc(ecFPsn,ecAnde);
	hFrntPRearP.inc(ecFPsn,ecRPsn);


	// gated on Front vs. Cathode  
	if ( gFrntCthd.inGate(ecFPsn,ecCthd) ){
		hFrntAndeGFC.inc(ecFPsn,ecAnde);
		hCthdAndeGFC.inc(ecCthd,ecAnde);
	}
	// gated on Front vs. Anode  
	if ( gFrntAnde.inGate(ecFPsn,ecAnde) ){
		hFrntCthdGFA.inc(ecFPsn,ecCthd);
		hCthdAndeGFA.inc(ecCthd,ecAnde);
	}
	// gated on Cathode vs Anode
	if ( gCthdAnde.inGate(ecCthd,ecAnde) ){
		hFrntAndeGCA.inc(ecFPsn,ecAnde);
		hFrntCthdGCA.inc(ecFPsn,ecCthd);
	}
	// gammas gated on reals
	if( gGamRel.inGate(eGamTac) ){
	    hGamRel.inc(eGamma);
	}	
	//gammas gated on randoms			
	if( gGamRan.inGate(eGamTac) ){
	    hGamRan.inc(eGamma);	
	}			

	// gate on all 3 gates above and the Front wire vs Rear Wire	
	if (( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
	    ( gFrntAnde.inGate(ecFPsn,ecAnde) )&&
	    ( gCthdAnde.inGate(ecCthd,ecAnde) )&& 
	    ( gFrntRear.inGate(ecFPsn,ecRPsn) )) {
		//front and rear wires.
		hFrntGAll.inc(eFPsn);    
		hRearGAll.inc(eRPsn);
		hGamPID.inc(eGamma);		
		
		//hard wired test					    					
		for (int i=0;i<NUM_GATES;i++){
		    if (gPeakF[i].inGate(eFPsn)){
			hRearPeak[i].inc(eRPsn);
			hCthdTheta[i].inc( (int) (128+eRPsn-eFPsn-offset), ecCthd);
			hCthdThNew[i].inc( (int) (128+eRPsn-eFPsn-offset), (int) (ecCthd-slopeC*(eRPsn-eFPsn-offset)) );
			hAndeTheta[i].inc( (int) (128+eRPsn-eFPsn-offset), ecAnde);
			hAndeThNew[i].inc( (int) (128+eRPsn-eFPsn-offset), (int) (ecAnde-slopeA*(eRPsn-eFPsn-offset)) );
		    }
		}
		
		// gammas gated on reals
		if( gGamRel.inGate(eGamTac) ){
		    hGamRelPID.inc(eGamma);
		}	
		//gammas gated on randoms			
		if( gGamRan.inGate(eGamTac) ){
		    hGamRanPID.inc(eGamma);	
		}			
	}  
	//corrected ungated histograms
	if (gFrntRear.inGate(ecFPsn,ecRPsn)){
	    hCthdNew.inc((int)(ecCthd-slopeC*(eRPsn-eFPsn-offset)) );
	    hAndeNew.inc((int)(ecAnde-slopeA*(eRPsn-eFPsn-offset)) );
	    hCthdAndeN.inc((int)(ecCthd-slopeC*(eRPsn-eFPsn-offset)),(int)(ecAnde-slopeA*(eRPsn-eFPsn-offset)) );
	    hFrntAndeN.inc(ecFPsn,(int)(ecAnde-slopeA*(eRPsn-eFPsn-offset)) );
	    hFrntCthdN.inc(ecFPsn, (int)(ecCthd-slopeC*(eRPsn-eFPsn-offset)) );
	}
    }
}
