/**
 * Sort routine for uconn measurement of
 * 7Li(d,p) cross section
 * @Author Ken Swartz Yale University
 * Modified on 11 December 1999 by Jim McDonald
 * Changed cnafCommands.event(n,n,n,n) to either 
 * cnafCommands.eventRead(n,n,n,n) or 
 * cnafCommands.eventCommand(n,n,n,n)
 * Rewritten on 30 December 1999 by Ralph France to properly
 *    include gated latch
 * included latch commands and spectra, et alia for LeCroy 4448
 * Fixed deadtime scaler (updated cnafCommands) 3 Jan 2000
 */
package sort.uconn;				
import jam.data.*;
import jam.sort.SortRoutine; 

public class LiLatch extends SortRoutine {

    int idMH;
    int idMF;
    int idF1;
    int idB1;
    int idF2;
    int idB2;
    int idF3;
    int idB3;
    int idC1;
    int idC2;
    int idC3;
    int idTAC1;
    int idTAC2;
    int idTAC3;
    int idEvent;
    int idLatch;
    
    // Define variable names 
    Histogram hF1;		// Station 1 Front
    Histogram hC1;		// Station 1 center 
    Histogram hB1;		// Station 1 back 
    Histogram hF2;		// Station 2 front  
    Histogram hC2;		// Station 2 center 
    Histogram hB2;		// Station 2 back 
    Histogram hF3;		// Station 3 front  
    Histogram hC3;		// Station 3 center 
    Histogram hB3;		// Station 3 back 
    
    Histogram hMH;		// monitor 1 (now MD)
    Histogram hMF;		// monitor 2 (now MFL)
    
    Histogram lF1;		// Station 1 Front gated on latch
    Histogram lC1;		// Station 1 center gated on latch
    Histogram lB1;		// Station 1 back gated on latch 
    Histogram lF2;		// Station 2 front gated on latch  
    Histogram lC2;		// Station 2 center gated on latch 
    Histogram lB2;		// Station 2 back gated on latch 
    Histogram lF3;		// Station 3 front gated on latch  
    Histogram lC3;		// Station 3 center gated on latch 
    Histogram lB3;		// Station 3 back gated on latch 
    
    Histogram lMH;		// monitor 1 (now MD) gated on latch
    Histogram lMF;		// monitor 2 (now MFL) gated on latch

        
    Histogram hFC1;		// Station 1 front vs center 2D
    Histogram hBC1;		// Station 1 back vs center 2D
    Histogram hFC2;		// Station 2 front vs center 2D
    Histogram hBC2;		// Station 2 back vs center 2D
    Histogram hFC3;		// Station 3 front vs center 2D
    Histogram hBC3;		// Station 3 back vs center 2D
    
    Histogram hFCTac1;		// A Front-Center TAC
    Histogram hBCTac1;	    	// A Back-Center TAC
    Histogram hFCTac2;		// B Front-Center TAC
    Histogram hBCTac2;		// B Back-Center TAC
    Histogram hFCTac3;		// C Front-Center TAC
    Histogram hBCTac3;		// C Back-Center TAC

    //Histogram hLatch;		// Latch
      	
    
    Scaler sPulser;		// raw pulser events       
    
    public LiLatch(){
    }
    // Define histograms, gates and scalers properties
    public void initialize()throws Exception{
	
	cnafCommands.init(1,28,8,26);	    //crate dataway Z 	
	cnafCommands.init(1,28,9,26);	    //crate dataway C
	cnafCommands.init(1,30,9,26);	    //crate I
	cnafCommands.init(1,9,12,11);	    //adc1 clear
	cnafCommands.init(1,10,12,11);	    //adc2 clear
	cnafCommands.init(1,20,0,10);	    //trigger module clear
	cnafCommands.init(1,16,0,9);	    //event scaler clear
	//cnafCommands.init(1,6,2,9);	    //clear deadtime scaler
	cnafCommands.init(1,8,0,9);	    //Clear Latch
	cnafCommands.init(1,8,1,9);
	cnafCommands.init(1,8,2,9);
	
	/* idTimer=cnafCommands.event(1,16,1,0); */	    //read Event Timing Scalar	
	idMH=cnafCommands.eventRead(1,9,0,0);	    //read MH
	idMF=cnafCommands.eventRead(1,9,1,0);	    //read MF
	idF1=cnafCommands.eventRead(1,9,3,0);	    //read F1
	idB1=cnafCommands.eventRead(1,9,4,0);	    //read B1
	idF2=cnafCommands.eventRead(1,9,5,0);	    //read F2
	idB2=cnafCommands.eventRead(1,9,6,0);	    //read B2
	idF3=cnafCommands.eventRead(1,9,7,0);	    //read F3
	idB3=cnafCommands.eventRead(1,10,0,0);	    //read B3
	idC1=cnafCommands.eventRead(1,10,1,0);	    //read C1
	idC2=cnafCommands.eventRead(1,10,2,0);	    //read C2
	idC3=cnafCommands.eventRead(1,10,3,0);	    //read C3

	idTAC1=cnafCommands.eventRead(1,10,4,0);    //read TAC1
	idTAC2=cnafCommands.eventRead(1,10,5,0);    //read TAC2
	idTAC3=cnafCommands.eventRead(1,10,6,0);    //read TAC3
	
	idEvent=cnafCommands.eventRead(1,16,0,0);   //read Event Scalar
	idLatch=cnafCommands.eventRead(1,8,0,0);    //read latch
	
	cnafCommands.eventCommand(1,9,12,11);	    //clear adc
	cnafCommands.eventCommand(1,10,12,11);	    //clear adc
	cnafCommands.eventCommand(1,20,0,10);	    //clear trigger module
	cnafCommands.eventCommand(1,8,0,9);	    //clear Latch module
	cnafCommands.eventCommand(1,8,1,9);
	cnafCommands.eventCommand(1,8,2,9);
		
	cnafCommands.scaler(1,6,2,0);		    //read deadtime scaler
	cnafCommands.clear(1,6,2,9);	    //clear deadtime scaler
	

	//Histograms		    name	number  type  size    title  
	//hLatch =new Histogram("Latch    ", HIST_1D, 16, " Latch ");  
	
	hF1 =new Histogram("F1       ", HIST_1D, 2048, " 1:Alpha Energy Front "); 
	hC1 =new Histogram("C1       ", HIST_1D, 2048, " 1:Alpha Energy Center "); 	
	hB1 =new Histogram("B1       ", HIST_1D, 2048, " 1:Alpha Energy Back ");
	hF2 =new Histogram("F2       ", HIST_1D, 2048, " 2:Alpha Energy Front "); 
	hC2 =new Histogram("C2       ", HIST_1D, 2048, " 2:Alpha Energy Center "); 	
	hB2 =new Histogram("B2       ", HIST_1D, 2048, " 2:Alpha Energy Back ");
	hF3 =new Histogram("F3       ", HIST_1D, 2048, " 3:Alpha Energy Front "); 
	hC3 =new Histogram("C3       ", HIST_1D, 2048, " 3:Alpha Energy Center "); 	
	hB3 =new Histogram("B3       ", HIST_1D, 2048, " 3:Alpha Energy Back ");

	hMH =new Histogram("Hydrogen Mon", HIST_1D, 2048, " Hydrogen Monitor ");   
	hMF =new Histogram("Front Mon   ", HIST_1D, 2048, " Forward Monitor ");   
	
	hFC1=new Histogram("1 F vs C 2D", HIST_2D, 256, " 1:Alpha Energy Front vs Center ");
	hBC1=new Histogram("1 B vs C 2D", HIST_2D, 256, " 1:Alpha Energy Back vs Center ");
	hFC2=new Histogram("2 F vs C 2D", HIST_2D, 256, " 2:Alpha Energy Front vs Center ");
	hBC2=new Histogram("2 B vs C 2D", HIST_2D, 256, " 2:Alpha Energy Back vs Center ");
	hFC3=new Histogram("3 F vs C 2D", HIST_2D, 256, " 3:Alpha Energy Front vs Center ");
	hBC3=new Histogram("3 B vs C 2D", HIST_2D, 256, " 3:Alpha Energy Back vs Center ");
	
	lF1 =new Histogram("F1       ", HIST_1D, 2048, " 1:Latched Alpha Energy Front "); 
	lC1 =new Histogram("C1       ", HIST_1D, 2048, " 1:Latched Alpha Energy Center "); 	
	lB1 =new Histogram("B1       ", HIST_1D, 2048, " 1:Latched Alpha Energy Back ");
	lF2 =new Histogram("F2       ", HIST_1D, 2048, " 2:Latched Alpha Energy Front "); 
	lC2 =new Histogram("C2       ", HIST_1D, 2048, " 2:Latched Alpha Energy Center "); 	
	lB2 =new Histogram("B2       ", HIST_1D, 2048, " 2:Latched Alpha Energy Back ");
	lF3 =new Histogram("F3       ", HIST_1D, 2048, " 3:Latched Alpha Energy Front "); 
	lC3 =new Histogram("C3       ", HIST_1D, 2048, " 3:Latched Alpha Energy Center "); 	
	lB3 =new Histogram("B3       ", HIST_1D, 2048, " 3:Latched Alpha Energy Back ");

	lMH =new Histogram("Hydrogen Mon", HIST_1D, 2048, " Latched Hydrogen Monitor ");   
	lMF =new Histogram("Front Mon   ", HIST_1D, 2048, " Latched Forward Monitor ");   
	
	hFC1=new Histogram("1 F vs C 2D", HIST_2D, 256, " 1:Alpha Energy Front vs Center ");
	hBC1=new Histogram("1 B vs C 2D", HIST_2D, 256, " 1:Alpha Energy Back vs Center ");
	hFC2=new Histogram("2 F vs C 2D", HIST_2D, 256, " 2:Alpha Energy Front vs Center ");
	hBC2=new Histogram("2 B vs C 2D", HIST_2D, 256, " 2:Alpha Energy Back vs Center ");
	hFC3=new Histogram("3 F vs C 2D", HIST_2D, 256, " 3:Alpha Energy Front vs Center ");
	hBC3=new Histogram("3 B vs C 2D", HIST_2D, 256, " 3:Alpha Energy Back vs Center ");

	hFCTac1 =new Histogram("1 TAC F-C ", HIST_1D, 2048, " 1:Tac Front vs Center ");   
	hBCTac1 =new Histogram("1 TAC B-C ", HIST_1D, 2048, " 1:Tac Back vs Center ");   
	hFCTac2 =new Histogram("2 TAC F-C ", HIST_1D, 2048, " 2:Tac Front vs Center ");   
	hBCTac2 =new Histogram("2 TAC B-C ", HIST_1D, 2048, " 2:Tac Back vs Center ");   
	hFCTac3 =new Histogram("3 TAC F-C ", HIST_1D, 2048, " 3:Tac Front vs Center ");   
	hBCTac3 =new Histogram("3 TAC B-C ", HIST_1D, 2048, " 3:Tac Back vs Center ");   
	
	sPulser	    =new Scaler("PulserCts/10", 0 ); 
    }

    public void sort(int [] dataEvent)throws Exception{    

	//unpack event into convenient names

	int MH    =dataEvent[idMH];
	int MF    =dataEvent[idMF];
	int F1    =dataEvent[idF1];
	int B1    =dataEvent[idB1];
	int F2    =dataEvent[idF2];
	int B2    =dataEvent[idB2];
	int F3    =dataEvent[idF3];
	int B3    =dataEvent[idB3];
	int C1    =dataEvent[idC1];
	int C2    =dataEvent[idC2];
	int C3    =dataEvent[idC3];	
	int tac1  =dataEvent[idTAC1];
	int tac2  =dataEvent[idTAC2];
	int tac3  =dataEvent[idTAC3];
	int Latch =dataEvent[idLatch];
	int evct  =dataEvent[idEvent];
		
	int F1small   =F1>>3;
	int C1small   =C1>>3;
	int B1small   =B1>>3;	    
	int F2small   =F2>>3;
	int C2small   =C2>>3;
	int B2small   =B2>>3;	    
	int F3small   =F3>>3;
	int C3small   =C3>>3;
	int B3small   =B3>>3;	
	
	
	boolean bF1 = ((Latch & 1)    == 1);
	boolean bB1 = ((Latch & 2)    == 2);
	boolean bF2 = ((Latch & 4)    == 4);
	boolean bB2 = ((Latch & 8)    == 8);
	boolean bF3 = ((Latch & 16)   == 16);
	boolean bB3 = ((Latch & 32)   == 32);
	boolean bMH = ((Latch & 64)   == 64);
	boolean bMF = ((Latch & 128)  == 128);
	boolean bC1 = ((Latch & 256)  == 256);
	boolean bC2 = ((Latch & 512)  == 512);
	boolean bC3 = ((Latch & 1024) == 1024);
	
	if (evct > 100) {    
	
	    hF1.inc(F1);
	    hC1.inc(C1);
	    hB1.inc(B1);
	    hF2.inc(F2);
	    hC2.inc(C2);
	    hB2.inc(B2);
	    hF3.inc(F3);
	    hC3.inc(C3);
	    hB3.inc(B3);
			
	    hMH.inc(MH);
	    hMF.inc(MF);
	//    hLatch.inc(Latch);
	    
	    if (bF1) lF1.inc(F1);
	    if (bC1) lC1.inc(C1);
	    if (bB1) lB1.inc(B1);
	    if (bF2) lF2.inc(F2);
	    if (bC2) lC2.inc(C2);
	    if (bB2) lB2.inc(B2);
	    if (bF3) lF3.inc(F3);
	    if (bC3) lC3.inc(C3);
	    if (bB3) lB3.inc(B3);
			
	    if (bMH) lMH.inc(MH);
	    if (bMF) lMF.inc(MF);

    	    if (bF1 & bC1) {
		    hFC1.inc(F1small,C1small);
		    hFCTac1.inc(tac1);
		     }
	    if (bB1 & bC1)  {
		    hBC1.inc(B1small,C1small);
		    hBCTac1.inc(tac1);
		    }	
	    if (bF2 & bC2)  {
		    hFC2.inc(F2small,C2small);
		    hFCTac2.inc(tac2);
		    }
	    if (bB2 & bC2)  {
		    hBC2.inc(B2small,C2small);
		    hBCTac2.inc(tac2);	
		    }
	    if (bF3 & bC3)  {
		    hFC3.inc(F3small,C3small);
		    hFCTac3.inc(tac3);
		    }
	    if (bB3 & bC3)  {
		    hBC3.inc(B3small,C3small);	
		    hBCTac3.inc(tac3);
		    }   
	    }
	}
    
    /*
     * Write this method to update a general monitor
     */
    
    public double monitor(String name){
	//method to calculate monitors
	return 0.0;
    }    
    
}
