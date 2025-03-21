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
public class CharlesHRIBFassay extends SortRoutine {

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
    
    //Scaler Histograms
    Histogram hStripChartEvents;
    Histogram hStripChartRT;
    Histogram hStripChartLT;
    Histogram hStripChartRB;
    Histogram hStripChartLB;

    Histogram hGeE01;
    Histogram hGeE02;
    Histogram hGeE03;
    Histogram hGeE04;
    Histogram hGeE05;
    Histogram hGeE06;
    Histogram hGeE07;
    Histogram hGeE08;
    Histogram hGeE09;
    Histogram hGeE10;
    Histogram hGeE11;
    Histogram hGeE12;
    Histogram hGeE13;
    Histogram hGeE14;
    Histogram hGeE15;
    Histogram hGeE16;
    Histogram hGeE17;
    Histogram hGeE18;
    Histogram hGeE19;
    Histogram hGeE20;
    Histogram hGeE21;
    Histogram hGeE22;
    Histogram hGeE23;
    Histogram hGeE24;
    Histogram hGeE25;
    Histogram hGeE26;
    Histogram hGeE27;
    Histogram hGeE28;
    Histogram hGeE29;
    Histogram hGeE30;

    //scalers
    Scaler sRealTime;
    Scaler sRealBeam;
    Scaler sLiveTime;
    Scaler sLiveBeam;

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
        
        // Scaler Histograms
        hStripChartEvents =new Histogram("Events", HIST_1D, 6000, "Events", "Events", "Events (100)");
        hStripChartRT  =new Histogram("Real Time", HIST_1D, 6000, "Real Time", "Real Time", "Events (10 sec)");
        hStripChartLT  =new Histogram("Live Time", HIST_1D, 6000, "Live Time", "Live Time", "Events (10 sec)");
        hStripChartRB  =new Histogram("Real Beam", HIST_1D, 6000, "Real Beam", "Real Beam", "Events (10 sec)");
        hStripChartLB  =new Histogram("Live Beam", HIST_1D, 6000, "Live Beam", "Live Beam", "Events (10 sec)");

       //Ge Energy Spectra
       hGeE01       =new Histogram("Ge 01", HIST_1D, 4096, "Ge 01", "Counts", "Channel");
       hGeE02       =new Histogram("Ge 02", HIST_1D, 4096, "Ge 02", "Counts", "Channel");
       hGeE03       =new Histogram("Ge 03", HIST_1D, 4096, "Ge 03", "Counts", "Channel");
       hGeE04       =new Histogram("Ge 04", HIST_1D, 4096, "Ge 04", "Counts", "Channel");
       hGeE05       =new Histogram("Ge 05", HIST_1D, 4096, "Ge 05", "Counts", "Channel");
       hGeE06       =new Histogram("Ge 06", HIST_1D, 4096, "Ge 06", "Counts", "Channel");
       hGeE07       =new Histogram("Ge 07", HIST_1D, 4096, "Ge 07", "Counts", "Channel");
       hGeE08       =new Histogram("Ge 08", HIST_1D, 4096, "Ge 08", "Counts", "Channel");
       hGeE09       =new Histogram("Ge 09", HIST_1D, 4096, "Ge 09", "Counts", "Channel");
       hGeE10       =new Histogram("Ge 10", HIST_1D, 4096, "Ge 10", "Counts", "Channel");
       hGeE11       =new Histogram("Ge 11", HIST_1D, 4096, "Ge 11", "Counts", "Channel");
       hGeE12       =new Histogram("Ge 12", HIST_1D, 4096, "Ge 12", "Counts", "Channel");
       hGeE13       =new Histogram("Ge 13", HIST_1D, 4096, "Ge 13", "Counts", "Channel");
       hGeE14       =new Histogram("Ge 14", HIST_1D, 4096, "Ge 14", "Counts", "Channel");
       hGeE15       =new Histogram("Ge 15", HIST_1D, 4096, "Ge 15", "Counts", "Channel");
       hGeE16       =new Histogram("Ge 16", HIST_1D, 4096, "Ge 16", "Counts", "Channel");
       hGeE17       =new Histogram("Ge 17", HIST_1D, 4096, "Ge 17", "Counts", "Channel");
       hGeE18       =new Histogram("Ge 18", HIST_1D, 4096, "Ge 18", "Counts", "Channel");
       hGeE19       =new Histogram("Ge 19", HIST_1D, 4096, "Ge 19", "Counts", "Channel");
       hGeE10       =new Histogram("Ge 20", HIST_1D, 4096, "Ge 20", "Counts", "Channel");
       hGeE21       =new Histogram("Ge 21", HIST_1D, 4096, "Ge 21", "Counts", "Channel");
       hGeE22       =new Histogram("Ge 22", HIST_1D, 4096, "Ge 22", "Counts", "Channel");
       hGeE23       =new Histogram("Ge 23", HIST_1D, 4096, "Ge 23", "Counts", "Channel");
       hGeE24       =new Histogram("Ge 24", HIST_1D, 4096, "Ge 24", "Counts", "Channel");
       hGeE25       =new Histogram("Ge 25", HIST_1D, 4096, "Ge 25", "Counts", "Channel");
       hGeE26       =new Histogram("Ge 26", HIST_1D, 4096, "Ge 26", "Counts", "Channel");
       hGeE27       =new Histogram("Ge 27", HIST_1D, 4096, "Ge 27", "Counts", "Channel");
       hGeE28       =new Histogram("Ge 28", HIST_1D, 4096, "Ge 28", "Counts", "Channel");
       hGeE29       =new Histogram("Ge 29", HIST_1D, 4096, "Ge 29", "Counts", "Channel");
       hGeE30       =new Histogram("Ge 30", HIST_1D, 4096, "Ge 30", "Counts", "Channel");

        sRealTime   =new Scaler("Real Time",0);
        sRealBeam   =new Scaler("Real Beam",1);
        sLiveTime   =new Scaler("Live Time",2);
        sLiveBeam   =new Scaler("Live Beam",3);

    }
    
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
        if ( RealTimeEvents>=31000){
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

	    if (StripChartCh==0){
		hGeE01.inc(GeE);
	    }
	    if (StripChartCh==1){
		hGeE02.inc(GeE);
	    }
	    if (StripChartCh==2){
		hGeE03.inc(GeE);
	    }
	    if (StripChartCh==3){
		hGeE04.inc(GeE);
	    }
	    if (StripChartCh==4){
		hGeE05.inc(GeE);
	    }
	    if (StripChartCh==5){
		hGeE06.inc(GeE);
	    }
	    if (StripChartCh==6){
		hGeE07.inc(GeE);
	    }
	    if (StripChartCh==7){
		hGeE08.inc(GeE);
	    }
	    if (StripChartCh==8){
		hGeE09.inc(GeE);
	    }
	    if (StripChartCh==9){
		hGeE10.inc(GeE);
	    }
	    if (StripChartCh==10){
		hGeE11.inc(GeE);
	    }
	    if (StripChartCh==11){
		hGeE12.inc(GeE);
	    }
	    if (StripChartCh==12){
		hGeE13.inc(GeE);
	    }
	    if (StripChartCh==13){
		hGeE14.inc(GeE);
	    }
	    if (StripChartCh==14){
		hGeE15.inc(GeE);
	    }
	    if (StripChartCh==15){
		hGeE16.inc(GeE);
	    }
	    if (StripChartCh==16){
		hGeE17.inc(GeE);
	    }
	    if (StripChartCh==17){
		hGeE18.inc(GeE);
	    }
	    if (StripChartCh==18){
		hGeE19.inc(GeE);
	    }
	    if (StripChartCh==19){
		hGeE20.inc(GeE);
	    }
	    if (StripChartCh==20){
		hGeE21.inc(GeE);
	    }
	    if (StripChartCh==21){
		hGeE22.inc(GeE);
	    }
	    if (StripChartCh==22){
		hGeE23.inc(GeE);
	    }
	    if (StripChartCh==23){
		hGeE24.inc(GeE);
	    }
	    if (StripChartCh==24){
		hGeE25.inc(GeE);
	    }
	    if (StripChartCh==25){
		hGeE26.inc(GeE);
	    }
	    if (StripChartCh==26){
		hGeE27.inc(GeE);
	    }
	    if (StripChartCh==27){
		hGeE28.inc(GeE);
	    }
	    if (StripChartCh==28){
		hGeE29.inc(GeE);
	    }
	    if (StripChartCh==29){
		hGeE30.inc(GeE);
	    }

        }//end sorting

    }//end routine
