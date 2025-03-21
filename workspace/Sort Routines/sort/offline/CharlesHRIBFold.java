/*
 */
package sort.offline;
import jam.data.*;
import jam.global.Sorter;
import jam.sort.SortRoutine;
import jam.JamException;
/*
 * Sort file for 132Te HRIBF Run for Charles J. Barton III
 * Nuclear Structure Group at WNSL, Yale University
 *
 * Convention for 2 d Histograms: x first, then y (x vs y)
 *
 * Author: Charles Barton (modified May 2001)
 * Authors: Ken Swartz, Alan Chen, and Dale Visser
 * Modified by Rachel Lewis, July 99
 * This one plots uncorrected histograms for rough gate setting, does presort
 */
public class CharlesHRIBFold extends SortRoutine {

    // ungated spectra from ADC and TDC
    Histogram hGLatch;
    Histogram hDT2E;
    Histogram hPSTDE;
    Histogram hNaIE;
    Histogram hXrayE;
    Histogram hGeE;
    Histogram hPos1;
    Histogram hPos2;		
    Histogram hPos3;		
    Histogram hPos4;	        
    Histogram hNaIT;
    Histogram hXrayT;
    Histogram hGeT;
    Histogram hDT1T;
    Histogram hDT2T;
    Histogram hPSTDT;
    Histogram hBeamT;
    Histogram hEvminusEv;
    
    //software created histograms
    Histogram hNaIEvPSTDT;
    Histogram hNaIEvDT1T;
    Histogram hNaIEvDT2T;
    Histogram hDT1TvDT2T;
    Histogram hNaIEvDT1DT2;

    Histogram hPosition;
    Histogram hPosAngle;
    
    //Scaler Histograms
    Histogram hStripChartEvents;
    Histogram hStripChartRT;
    Histogram hStripChartLT;
    Histogram hStripChartRB;
    Histogram hStripChartLB;
    
    //gated on CoulEx in NaIE v PSTDT 2D spectrum
    Histogram hNaIEvPSTDTgPSTDT;
    Histogram hNaIEvDT1TgPSTDT;
    Histogram hNaIEvDT2TgPSTDT;
    Histogram hPosgPSTDT;    
    Histogram hNaIEgPSTDT;
    Histogram hDT1TvDT2TgPSTDT;
    Histogram hNaIEvDT1DT2gPSTDT;
    
    //gated on background in NaIE v PSTDT 2D spectrum
    Histogram hbkNaIEvPSTDTgPSTDT;
    Histogram hbkNaIEvDT1TgPSTDT;
    Histogram hbkNaIEvDT2TgPSTDT;
    Histogram hbkPosgPSTDT;
    Histogram hbkNaIEgPSTDT;
    
    //gate by scintillator cathode
//    Histogram hFrntSntrGSC;
//    Histogram hFrntCthdGSC;

    //gate by Front wire Cathode
//    Histogram hSntrCthdGFC;
//    Histogram hFrntSntrGFC;

    //gate by Front wire Scintillator
    Histogram hSntrCthdGFS;
    Histogram hFrntCthdGFS;

    //front and rear wire gate on all
    Histogram hFrntGAll;
    Histogram hRearGAll;

    //gates 2 d
    Gate gPSTDT;
    Gate gbkPSTDT;
    Gate gDT1T;
    Gate gbkDT1T;
    Gate gbkDT2T;
    Gate gDT2T;

    //gates 2 d
    Gate gNaIEvPSTDT2D;

    //scalers
    Scaler sRealTime;
    Scaler sRealBeam;
    Scaler sLiveTime;
    Scaler sLiveBeam;

    //monitors
    Monitor mBeam;
    Monitor mClck;
    Monitor mEvntRt;
    Monitor mCthd;
    //Parameter pSlopeC1;
    //Parameter pSlopeS1;

    public void initialize() throws Exception {

        setEventSize(30);

        hGLatch	    	=new Histogram("Gated Latch ", HIST_1D, 4096, " Gated Latch","channel", "counts");
        hDT2E	    	=new Histogram("DT2 Energy  ", HIST_1D, 4096, " DT2 Energy", "channel", "counts");
        hPSTDE	    	=new Histogram("PSTD Energy ", HIST_1D, 4096, " PSTD Energy", "channel", "counts");
        hNaIE	    	=new Histogram("NaI Energy  ", HIST_1D,  256, " NaI(Tl) Energy RAW ", "Energy channel", "counts");
        hXrayE  	=new Histogram("X-ray E     ", HIST_1D, 4096, " X-ray E ", "Energy channel", "counts");
        hGeE    	=new Histogram("Ge E        ", HIST_1D, 4096, " Ge E", "Energy channel", "counts");
        hPos1   	=new Histogram("Position 1  ", HIST_1D, 4096, " Position 1", "Time channel", "counts");
        hPos2   	=new Histogram("Position 2  ", HIST_1D, 4096, " Position 2", "Time channel", "counts");
        hPos3   	=new Histogram("Position 3  ", HIST_1D, 4096, " Position 3", "Time channel", "counts");
        hPos4     	=new Histogram("Position 4  ", HIST_1D, 4096, " Position 4", "Time channel", "counts");
        hNaIT	  	=new Histogram("NaI Time    ", HIST_1D, 4096, " NaI Time", "Time channel", "counts");
        hXrayT	  	=new Histogram("X-ray Time  ", HIST_1D, 4096, " X-ray Time", "Time channel", "counts");
        hGeT	  	=new Histogram("Ge Time     ", HIST_1D, 4096, " Ge Time", "Time channel", "counts");
        hDT1T	  	=new Histogram("DT1 Time    ", HIST_1D, 4096, " DT1 Time", "Time channel", "counts");
        hDT2T	  	=new Histogram("Dt2 Time    ", HIST_1D, 4096, " DT2 Time", "Time channel", "counts");
        hPSTDT	  	=new Histogram("PSTD Time   ", HIST_1D, 4096, " PSTD Time", "Time channel", "counts");
        hBeamT	  	=new Histogram("Beam Time   ", HIST_1D, 4096, " Beam Time", "Time channel", "counts");
        hEvminusEv  	=new Histogram("Event-Event ", HIST_1D, 4096, " Event-Event Time", "channel", "counts");
                

        hNaIEvPSTDT =new Histogram("NaIEvPSTDT", HIST_2D,  256, "NaI(Tl) Energy v PSTD Time", "NaI Energy", "PSTD Time");
        hNaIEvDT1T  =new Histogram("NaIEvDT1  ", HIST_2D,  256, "NaI(Tl) Energy v DT1 Time", "NaI Energy", "DT1 Time");
        hNaIEvDT2T  =new Histogram("NaIEvDT2  ", HIST_2D,  256, "NaI(Tl) Energy v DT2 Time", "NaI Energy", "DT2 Time");
        hDT1TvDT2T  =new Histogram("DT1vDT2   ", HIST_2D,  256, "DT1T v DT2T", "DT1 Time", "DT2 Time");
        hNaIEvDT1DT2  =new Histogram("NaIEvDT1DT2   ", HIST_2D,  256, "NaIE v DT1T-DT2T+1024", "NaI Energy", "DT1T-DT2T+1024 Time");

        hPosition   =new Histogram("Position  ", HIST_2D,  256, "Position Detector", "X", "Y");
        hPosAngle   =new Histogram("Some Angle", HIST_1D, 16, "Position Angle Projection", "Some Angle", "Counts");
        
        // Scaler Histograms
        hStripChartEvents =new Histogram("Events", HIST_1D, 15000, "Events", "Events", "Events (100)");
        hStripChartRT =new Histogram("Real Time", HIST_1D, 15000, "Real Time", "Real Time", "Events (10 sec)");
        hStripChartLT =new Histogram("Live Time", HIST_1D, 15000, "Live Time", "Live Time", "Events (10 sec)");
        hStripChartRB =new Histogram("Real Beam", HIST_1D, 15000, "Real Beam", "Real Beam", "Events (10 sec)");
        hStripChartLB =new Histogram("Live Beam", HIST_1D, 15000, "Live Beam", "Live Beam", "Events (10 sec)");
        
        //gate on NaIE PSTD Time 2D Spectrum
        hPosgPSTDT        =new Histogram("Position G PSTDT  ", HIST_2D,  256, "Position gated NaIEvPSTDT", "X", "Y");
        hNaIEgPSTDT       =new Histogram("NaI Energy G PSTDT", HIST_1D,  256, "NaIE gated NaIEvPSTDT", "X", "Y");
        hDT1TvDT2TgPSTDT  =new Histogram("DT1vDT2 G PSTDT   ", HIST_2D,  256, "DT1T v DT2T gated NaIEvPSTDT", "DT1 Time", "DT2 Time");
        hNaIEvDT1DT2gPSTDT=new Histogram("NaIvDT1DT2 G PSTDT", HIST_2D,  256, "NaIE v DT1DT2 gated NaIEvPSTDT", "NaI Energy", "DT1T - DT2T +1024");
        hNaIEvDT1TgPSTDT  =new Histogram("NaIEvDT1T G PSTDT ", HIST_2D,  256, "NaIE v DT1T gated NaIEvPSTDT", "NaI Energy", "DT1T");
        hNaIEvDT2TgPSTDT  =new Histogram("NaIEvDT2T G PSTDT ", HIST_2D,  256, "NaIE v DT2T gated NaIEvPSTDT", "NaI Energy", "DT2T");
        hNaIEvPSTDTgPSTDT =new Histogram("NaIEvPSTDT G PSTDT", HIST_2D,  256, "NaIE v PSTDT gated PSTDT", "NaI Energy", "PSTD Time");
        
        //gate on background NaIE v PSTDT spectrum 2D
        hbkPosgPSTDT        =new Histogram("bk Position G PSTDT   ", HIST_2D,  256, "Position gated NaIEvPSTDT", "X", "Y");
        hbkNaIEgPSTDT       =new Histogram("bk NaI Energy G PSTDT ", HIST_1D,  256, "NaIE gated NaIEvPSTDT", "X", "Y");
        hbkNaIEvPSTDTgPSTDT =new Histogram("bk NaIEvPSTDT G PSTDT ", HIST_2D,  256, "NaIEvPSTDT gated PSTDT", "NaIE", "PSTDT");
        hbkNaIEvDT1TgPSTDT  =new Histogram("bk NaIEvDT1T G PSTDT  ", HIST_2D,  256, "bkNaIEvDT1T gated PSTDT", "NaIE", "TD1T");
        hbkNaIEvDT2TgPSTDT  =new Histogram("bk NaIEvDT2T G PSTDT  ", HIST_2D,  256, "bkNaIEvDT2T gated PSTDT", "NaIE", "DT2T");


//        hSntrCthdGFC=new Histogram("ScintCathGFC ", HIST_2D,  256, " Scintillator vs Cathode - FwCa gate ", "scintillator", "cathode");
//        hFrntSntrGFC=new Histogram("FrontScintGFC ", HIST_2D,  256, " Front Position vs Scintillator - FwCa gate", "front position", "scintillator");

        //gate on Front Wire Scintillator
//        hSntrCthdGFS=new Histogram("ScintCathGFS ", HIST_2D,  256, " Scintillator vs Cathode - FwSc gate ", "scintillator", "cathode");
//        hFrntCthdGFS=new Histogram("FrontCathGFS ", HIST_2D,  256, " Front Position vs Cathode - FwSc gate ", "front position", "cathode");

        //gate on 3 gate
//        hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D, 2048, " Front Position - ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");
//        hRearGAll   =new Histogram("RearGAll    ", HIST_1D, 2048, " Rear Position - ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");

        //gates  2d
        gPSTDT   =new Gate("NaIE PSTDT CoulEx", hNaIEvPSTDT);
        gbkPSTDT =new Gate("bk NaIE PSTDT bk", hNaIEvPSTDT);
        gDT1T    =new Gate("NaIE TD1 CoulEx", hNaIEvDT1TgPSTDT);
        gbkDT2T  =new Gate("bk NaIE TD1T bk", hbkNaIEvDT1TgPSTDT);
        gDT2T    =new Gate("NaIE TD2 Coulex", hNaIEvDT2TgPSTDT);
        gbkDT2T  =new Gate("bk NaIE TD2T bk", hbkNaIEvDT2TgPSTDT);
        
//        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);
//        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);
//        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);

        //add gates to gated spectra
//        hFrntSntrGSC.addGate(gFrntSntr);
//        hFrntSntrGFC.addGate(gFrntSntr);

//        hSntrCthdGFC.addGate(gSntrCthd);
//        hSntrCthdGFS.addGate(gSntrCthd);

//        hFrntCthdGFS.addGate(gFrntCthd);
//        hFrntCthdGSC.addGate(gFrntCthd);

        sRealTime   =new Scaler("Real Time",0);
        sRealBeam   =new Scaler("Real Beam",1);
        sLiveTime   =new Scaler("Live Time",2);
        sLiveBeam   =new Scaler("Live Beam",3);

        //mBeam=new Monitor("Beam ",sBic);
        //mClck=new Monitor("Clock",sClck);
        //mEvntRt=new Monitor("Event Rate",sEvntRaw);
        //mCthd=new Monitor("Cathode",gCthd);
    }
    
    double ellipse=0;
    int events=-1;
    int StripChartCh=0;
    int Next10000events=10000;
    int event=0;
    int RealTimeInterval=10000;
    int BeginningRealTime=0;
    int BeginningLiveTime=0;
    int BeginningRealBeam=0;
    int BeginningLiveBeam=0;
    int DifferenceEvents=0;
    int DifferenceRT=0;
    int DifferenceRB=0;
    int DifferenceLT=0;
    int DifferenceLB=0;
    int RealTimeEvents=0;
    int LiveTimeEvents=0;
    int RealBeamEvents=0;
    int LiveBeamEvents=0;
    
    boolean RTOverflow = false;
    boolean RBOverflow = false;
    boolean LTOverflow = false;
    boolean LBOverflow = false;
    
    public void sort(int [] dataEvent) throws Exception{

        //unpack data into convenient names
        int GLatch  =dataEvent[0];
        int DT2E    =dataEvent[1];
        int PSTDE   =dataEvent[2];
        int NaIEevent    =dataEvent[3];
        int XrayE   =dataEvent[4];
        int GeE     =dataEvent[5];
        int Pos1    =dataEvent[6];
        int Pos2    =dataEvent[7];
        int Pos3    =dataEvent[8];
        int Pos4    =dataEvent[9];
        int NaIT    =dataEvent[10];
        int XrayT   =dataEvent[11];
        int GeT     =dataEvent[12];
        int DT1T    =dataEvent[13];
        int DT2T    =dataEvent[14];
        int PSTDT   =dataEvent[15];
        int BeamT   =dataEvent[16];
        int EvminusEv    =dataEvent[17];
        int sRT     =dataEvent[25];
        int sRB     =dataEvent[26];
        int sLT     =dataEvent[27];
        int sLB     =dataEvent[28];

        //int eSntr=(eSntr1+eSntr2)/2;

        //int ecFPsn=eFPsn>>3;
        //int ecRPsn=eRPsn>>3;
        //int ecFHgh=eFHgh>>3;
        //int ecRHgh=eRHgh>>3;

        //	int ecSntr=eSntr>>3;
        //int ecCthd=eCthd>>3;
        //int ecAnde=eAnde>>3;

//        double frntF;

        // singles spectra
        int NaIE=(NaIEevent/16);
        
        hGLatch.inc(GLatch);
        hDT2E.inc(DT2E);
        hPSTDE.inc(PSTDE);
        hNaIE.inc(NaIE);
        hXrayE.inc(XrayE);
        hGeE.inc(GeE);
        hPos1.inc(Pos1);
        hPos2.inc(Pos2);
        hPos3.inc(Pos3);
        hPos4.inc(Pos4);
        hNaIT.inc(NaIT);
        hXrayT.inc(XrayT);
        hGeT.inc(GeT);
        hDT1T.inc(DT1T);
        hDT2T.inc(DT2T);
        hPSTDT.inc(PSTDT);
        hBeamT.inc(BeamT);
        hEvminusEv.inc(EvminusEv);
       

        //singles 2d spectra
        int smallNaIE=(NaIE);
        int smallPSTDT=(PSTDT/16);
        int smallDT1T=(DT1T/16);
        int smallDT2T=(DT2T/16);
        int diffDT1DT2=((DT1T-DT2T+1024)/16);
        
        if (NaIE>-1){
            hNaIEvDT1DT2.inc(smallNaIE,diffDT1DT2);
            if (PSTDT>-1){
                hNaIEvPSTDT.inc(smallNaIE,smallPSTDT);
            }
            if (DT1T>-1){
                hNaIEvDT1T.inc(smallNaIE,smallDT1T);
            }
            if (DT2T>-1){
                hNaIEvDT2T.inc(smallNaIE,smallDT2T);
            }
        }//end NaIE if greater than 40  al timing detectors > 80
        
        if (DT1T>-1){
            if (DT2T>-1){
                hDT1TvDT2T.inc(smallDT1T,smallDT2T); 
            }
        }
        
        //Position Sensitive Timing Detector
        double QLeft  =(double)(Pos1+Pos4);
        double Qright =(double)(Pos2+Pos3);
        double Qup    =(double)(Pos3+Pos4);
        double Qdown  =(double)(Pos1+Pos2);
        double Qtotal =(double)(Pos1+Pos2+Pos3+Pos4);
        
        if ((Pos1>0)&&(Pos2>0)&&(Pos3>0)&&(Pos4>0)){
            double theta = (50.0/180.00)*3.141592653;
            double ct = Math.cos(theta);
            double st = Math.sin(theta);
            double xreal =(200.0*(Qright/Qtotal));
            double yreal =(200.0*(Qup/Qtotal));
            int xint = (int)((xreal*ct) - (yreal*st)+100.0);
            int yint = (int)((xreal*st) + (yreal*ct)-0.0);
            
            hPosition.inc(xint,yint);
            
//            if (100<PSTDT){
//                if (PSTDT<400){
//                    hPosgPSTDT.inc(xint,yint);
//                }
//            }//end PSTDT gate
            
            //Ellipse gate on Position 2D histogram
            double xdreal = ((xreal*ct) - (yreal*st)+100.0);
            double ydreal = ((xreal*st) + (yreal*ct)-0.0);
            double bigellipsecheck=( (Math.pow(((xdreal-92.0)/38.0),2.0)) + (Math.pow(((ydreal-145.0)/85.0),2.0)) );
            double smallellipsecheck=( (Math.pow(((xdreal-92.0)/22.0),2.0)) + (Math.pow(((ydreal-145.0)/45.0),2.0)) );
            
            //Begin Angle Cuts on Position Sensitive & NaIEvPSTDT
            double a1 = 3.5, a2=2*a1, a3=3*a1, a4=4*a1, a5=5*a1, a6=6*a1, a7=7*a1, a8=8*a1, a9=9*a1, a10=10*a1, a11=11*a1, a12=12*a1;
            double b1 = 9.0, b2=2*b1, b3=3*b1, b4=4*b1, b5=5*b1, b6=6*b1, b7=7*b1, b8=8*b1, b9=9*b1, b10=10*b1, b11=11*b1, b12=12*b1;
            double xzero = 92, yzero = 145;
            
//            if ( gPSTDT.inGate(smallNaIE,smallPSTDT) ){
//                if ( (gDT1T.inGate(smallNaIE,smallDT1T)) && (gDT2T.inGate(smallNaIE,smallDT2T)) ){
            if ( (smallNaIE>9)&&(smallNaIE<101)&&(smallPSTDT>13)&&(smallPSTDT<21) ){
                if ( (smallDT1T>5)&&(smallDT1T<25)&&(smallDT2T>11)&&(smallDT2T<31) ){
                    ellipse=Math.pow(((xdreal-xzero)/a1),2.0) + Math.pow(((ydreal-yzero)/b1),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(1);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a2),2.0) + Math.pow(((ydreal-yzero)/b2),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(2);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a3),2.0) + Math.pow(((ydreal-yzero)/b3),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(3);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a4),2.0) + Math.pow(((ydreal-yzero)/b4),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(4);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a5),2.0) + Math.pow(((ydreal-yzero)/b5),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(5);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a6),2.0) + Math.pow(((ydreal-yzero)/b6),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(6);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a7),2.0) + Math.pow(((ydreal-yzero)/b7),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(7);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a8),2.0) + Math.pow(((ydreal-yzero)/b8),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(8);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a9),2.0) + Math.pow(((ydreal-yzero)/b9),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(9);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a10),2.0) + Math.pow(((ydreal-yzero)/b10),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(10);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a11),2.0) + Math.pow(((ydreal-yzero)/b11),2.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(11);
                    }
                    ellipse=Math.pow(((xdreal-xzero)/a12),2.0) + Math.pow(((ydreal-yzero)/b2),12.0) ;
                    if (ellipse<1.0){
                        hPosAngle.inc(12);
                    }
                    if (ellipse>1.0){
                        hPosAngle.inc(13);
                    }
                }
            }

            //Begin Ellipse Gate on Position

           if ( (bigellipsecheck<1.0)&&(smallellipsecheck>1.0) ){
            
            if ( (smallNaIE>9)&&(smallNaIE<101) ){
                if ( (smallDT1T>5)&&(smallDT1T<25) ){
                    if ( (smallDT2T>11)&&(smallDT2T<31) ){
                        if ( (smallPSTDT>10)&&(smallPSTDT<35) ){
                            hNaIEvPSTDTgPSTDT.inc(smallNaIE,smallPSTDT); 
                            hNaIEvDT1TgPSTDT.inc(smallNaIE,smallDT1T);
                            hNaIEvDT2TgPSTDT.inc(smallNaIE,smallDT2T);
//                            hPosgPSTDT.inc(xint,yint);
//                            hNaIEgPSTDT.inc(NaIE);
                            hDT1TvDT2TgPSTDT.inc(smallDT1T,smallDT2T);
                            hNaIEvDT1DT2gPSTDT.inc(smallNaIE,diffDT1DT2);
                            if ( (smallPSTDT>13)&&(smallPSTDT<21) ){
                                hNaIEgPSTDT.inc(NaIE);
                                hPosgPSTDT.inc(xint,yint);
                            }
                        }
                    }
              }
           }
        
        if ( (smallNaIE>9)&&(smallNaIE<101) ){
            if ( (smallDT1T>5)&&(smallDT1T<25) ){
                if ( (smallDT2T>11)&&(smallDT2T<31) ){
                    if ( (smallPSTDT>10)&&(smallPSTDT<35) ){
                        hbkNaIEvPSTDTgPSTDT.inc(smallNaIE,smallPSTDT);
                        hbkNaIEvDT1TgPSTDT.inc(smallNaIE,smallDT1T);
                        hbkNaIEvDT2TgPSTDT.inc(smallNaIE,smallDT2T);
                        if ( (smallPSTDT>21)&&(smallPSTDT<29) ){
                            hbkPosgPSTDT.inc(xint,yint);
                            hbkNaIEgPSTDT.inc(NaIE);
                        }
                    }
                }
            }
        }
            
//            if (gbkPSTDT.inGate(smallNaIE,smallPSTDT) ){
//            if ( (smallNaIE>9)&&(smallNaIE<101)&&(smallPSTDT>9)&&(smallPSTDT<25) ){
//                hbkNaIEvDT1TgPSTDT.inc(smallNaIE,smallDT1T);
//                hbkNaIEvDT2TgPSTDT.inc(smallNaIE,smallDT2T);
//                if ( (gbkDT1T.inGate(smallNaIE,smallDT1T)) && (gbkDT2T.inGate(smallNaIE,smallDT2T)) ){
//                if ( (smallDT1T>31)&&(smallDT1T<42)&&(smallDT2T>40)&&(smallDT2T<51) ){
//                if ( (smallDT1T>11)&&(smallDT1T<16) ){
//                    hbkNaIEvDT2TgPSTDT.inc(smallNaIE,smallDT2T);
//                    hbkPosgPSTDT.inc(xint,yint);
//                    hbkNaIEgPSTDT.inc(NaIE);
//                }
//            }
            
        }//end ellipse gate and PSTDTgate
            
            
        
        }//end Q>0 condition for Position and gates
        
             /*Scalers Handled Here*/
            sRealTime.setValue(sRT);
            sRealBeam.setValue(sRB);
            sLiveTime.setValue(sLT);
            sLiveBeam.setValue(sLB);
            
        //Scaler Software Created Histograms

        events++;
        if (events == 0){
            if (sRT>=0){
                BeginningRealTime=sRT;
            }
            else if (sRT<0){
                BeginningRealTime=32768+sRT;
            }
            if (sRB>=0){
                BeginningRealBeam=sRB;
            }
            else if (sRB<0){
                BeginningRealBeam=32768+sRB;
            }
            if (sLT>=0){
                BeginningLiveTime=sLT;
            }
            else if (sLT<0){
                BeginningLiveTime=32768+sLT;
            }
            if (sLB>=0){
                BeginningLiveBeam=sLB;
            }
            else if (sLB<0){
                BeginningLiveBeam=32768+sLB;
            }
        }//end events loop

        DifferenceEvents = DifferenceEvents + events;

        if (sRT >= 0){
//                System.out.println(StripChartCh);
//                System.out.println(sRT);
//                System.out.println(BeginningRealTime); 
            DifferenceRT = DifferenceRT + sRT - BeginningRealTime;
            if (DifferenceRT<0){
                DifferenceRT = 0;
            }
            BeginningRealTime = sRT;
            RTOverflow=false;   
        } else {  //sRT<0
            if (!RTOverflow) {//flip from + to -
                DifferenceRT = DifferenceRT + (32768 + sRT) + (32768 - BeginningRealTime);
                BeginningRealTime = (32768 + sRT);
                RTOverflow=true;
            } else {
                DifferenceRT = DifferenceRT + (32768 + sRT) - BeginningRealTime;        
                BeginningRealTime = (32768 + sRT);
            }
        }

        for (int i=0; i<DifferenceRT; i++){
            hStripChartRT.inc(StripChartCh);
        }
        RealTimeEvents = RealTimeEvents+DifferenceRT;
        DifferenceRT = 0;
        
        if (sRB >= 0){
            DifferenceRB = DifferenceRB + sRB - BeginningRealBeam;
            BeginningRealBeam = sRB;
            RBOverflow = false;
        }
        else if (sRB < 0){
            if (!RBOverflow) {//flip from + to -    
                DifferenceRB = DifferenceRB + (32768 + sRB) + (32768 - BeginningRealBeam);
                BeginningRealBeam = (32768 + sRB);
                RBOverflow = true;
            }
            else {
                DifferenceRB = DifferenceRB + (32768 + sRB) - BeginningRealBeam;
                BeginningRealBeam = (32768 + sRB);
            }
        }
        for (int i=0; i<DifferenceRB; i++){
            hStripChartRB.inc(StripChartCh);
        }
        RealBeamEvents=RealBeamEvents+DifferenceRB;
        DifferenceRB=0;
        
        if (sLT >= 0){
            DifferenceLT = DifferenceLT + sLT - BeginningLiveTime;
            BeginningLiveTime = sLT;
            LTOverflow = false;
        }
        else if (sLT < 0){
            if (!LTOverflow){
                DifferenceLT = DifferenceLT + (32768 + sLT) + (32768 - BeginningLiveTime);
                BeginningLiveTime = (32768 + sLT);
                LTOverflow = true;
            }
            else {
                DifferenceLT = DifferenceLT + (32768 + sLT) - BeginningLiveTime;
                BeginningLiveTime = (32768 + sLT);
            }
        }
        for (int i=0; i<DifferenceLT; i++){
            hStripChartLT.inc(StripChartCh);
        }
        LiveTimeEvents = LiveTimeEvents+DifferenceLT;
        DifferenceLT=0;
        
        
        if (sLB >= 0){
            DifferenceLB = DifferenceLB + sLB - BeginningLiveBeam;
            BeginningLiveBeam = sLB;
            LBOverflow = false;
        }
        else if (sLB < 0){
            if (!LBOverflow){
                DifferenceLB = DifferenceLB + (32768 + sLB) + (32768 - BeginningLiveBeam);
                BeginningLiveBeam = (32768 + sLB);
                LBOverflow = true;
            }
            else {
                DifferenceLB = DifferenceLB + (32768 + sLB) - BeginningLiveBeam;
                BeginningLiveBeam = (32768 + sLB);
            }
        }
        LiveBeamEvents=LiveBeamEvents+DifferenceLB;
        DifferenceLB=0;

//        if (events==100){
        if ( RealTimeEvents>=1000){
            DifferenceEvents=events;
            for (int i=0; i<DifferenceEvents; i++){
                hStripChartEvents.inc(StripChartCh);
            }
            StripChartCh++;
            events=0;
            RealTimeEvents=0;
            LiveTimeEvents=0;
            RealBeamEvents=0;
            LiveBeamEvents=0;
        }//end if over Real Time Interval for StripCharts
        
       
        
        //hSntrCthd.inc(ecSntr,ecCthd);
        //	hFrntSntr.inc(ecFPsn,ecSntr);

        //gate on front rear
//        if ( gFrntRear.inGate(ecFPsn,ecRPsn) ){

            // gate on Scintillator vs Cathode
            /*if ( gSntrCthd.inGate(ecSntr ,ecCthd) ){
            hFrntSntrGSC.inc(ecFPsn, ecSntr);
            hFrntCthdGSC.inc(ecFPsn, ecCthd);
            }*/
            // gate on Front Wire Position vs Cathode
            /*if ( gFrntCthd.inGate(ecFPsn,ecCthd) ){
            hSntrCthdGFC.inc(ecSntr,ecCthd);
            hFrntSntrGFC.inc(ecFPsn,ecSntr);
            }*/
            // gate on Front Wire Position vs Scintillator
            /*	    if ( gFrntSntr.inGate(ecFPsn,ecSntr) ){
            hSntrCthdGFS.inc(ecSntr,ecCthd);
            hFrntCthdGFS.inc(ecFPsn,ecCthd);
            }*/

            // gate on all 3 gates above and the Front wire vs Rear Wire
            /*    if ( gFrntCthd.inGate(ecFPsn,ecCthd) ){
            if ( gSntrCthd.inGate(ecSntr,ecCthd) ){
            if ( gFrntSntr.inGate(ecFPsn,ecSntr) ){
            hFrntGAll.inc(eFPsn);
            hRearGAll.inc(eRPsn);
            writeEvent(dataEvent);
            }
            }
            } //end gate-on-all-three*/
//            }//end gate on front rear

        }//end sorting
    }//end routine
