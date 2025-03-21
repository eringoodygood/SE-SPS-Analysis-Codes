/*
 */
package sort.coinc.oct01;
import jam.data.*;
import jam.sort.SortRoutine;
/*
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified March 2001 by Rachel Lewis for compatibility with SplitPoleBeta (VME acquisition instead of Camac)
 */
public class AngleCorrectionsFCAS extends SortRoutine {

    // ungated spectra
    Histogram hCthd,hAnde,hSntrSum, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    Histogram hRearPH;
    Histogram hAndeCthd, hAndeFrnt, hCthdFrnt, hCthdSntr, hSntrFrnt, hAndeSntr;
    Histogram hRearFrnt, hRearFrntPID;

    Histogram hAndeCthdG5, hAndeFrntG5, hCthdFrntG5, hCthdSntrG5, hSntrFrntG5, hAndeSntrG5  ;//gated by 5 PID's
    Histogram hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed

    Gate gAndeCthd, gAndeFrnt, gCthdFrnt, gCthdSntr, gSntrFrnt, gAndeSntr;
    Gate gRearFrnt;

    //correlation stuff
    final int NUM_GATES = 4;
    int [] FrntCent=new int[NUM_GATES];
    int [] RearCent=new int[NUM_GATES];
    Histogram [] hRearPeak = new Histogram[NUM_GATES];
    Histogram [] hFtRrCorr = new Histogram[NUM_GATES];
    Histogram [] hFtRrNewCr = new Histogram[NUM_GATES];
    Histogram hFrntNew, hcFrntNew;
    Histogram [] hCthdTheta = new Histogram[NUM_GATES]; //gated on three gates; cathode energy vs "theta" for each front wire peak
    Histogram [] hCthdThNew = new Histogram[NUM_GATES]; //above, corrected using SlopeC DataParameter
    Histogram [] hSntrTheta = new Histogram[NUM_GATES];
    Histogram [] hSntrThNew = new Histogram[NUM_GATES];
    Histogram [] hAndeTheta = new Histogram[NUM_GATES];
    Histogram [] hAndeThNew = new Histogram[NUM_GATES];
    Gate [] gPeak= new Gate[NUM_GATES];
    Gate [] gScintC= new Gate[NUM_GATES];//these gates appear in the correlation plots
    Gate [] gCathC= new Gate[NUM_GATES];//and facilitate calculating the slopes
    Gate [] gAndeC = new Gate[NUM_GATES];
    DataParameter pSlopeFa; // slope = a + b * (x-x0)
    DataParameter pSlopeFb, pSlopeFx0, pSlopeF, pSlopeCa, pSlopeCb, pSlopeCx0;
    DataParameter pSlopeSa, pSlopeSb, pSlopeSx0, pSlopeAa, pSlopeAb, pSlopeAx0, pOffset;
    DataParameter pCentFt[]=new DataParameter[NUM_GATES];
    DataParameter pCentRr[]=new DataParameter[NUM_GATES];

    static final int ADC_BASE = 0x20000000; //NEW
    static final int ADC_2_BASE = 0x20010000;
    static final int TDC_BASE = 0x30000000;
    static final int TDC_2_BASE = 0x30010000;
    static final int THRESHOLDS = 0;
    
    //id numbers for the signals;NEW
    int idCthd, idAnde, idScintR, idScintL, idFrntPsn, idRearPsn, idFrntHgh, idRearHgh, idSilicon, idNaI1, idNaI2;
    int idFrontR, idFrontL, idRearR, idRearL;
    int NUM_PARAMETERS;

    /*final int idCthd=0;
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
    final int NUM_PARAMETERS=11;*/

    int lastEvntAccpt;

    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS=1024;//number of channels in compressed position histogram
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    public void initialize() throws Exception {
        //id numbers for the signals;NEW
        idCthd=vmeMap.eventParameter(0, ADC_BASE, 10, THRESHOLDS);
        idAnde=vmeMap.eventParameter(1, ADC_BASE, 1, THRESHOLDS);
        idScintR=vmeMap.eventParameter(2, ADC_BASE, 2, THRESHOLDS);
        idScintL=vmeMap.eventParameter(3, ADC_BASE, 3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(4, ADC_BASE, 4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(5, ADC_BASE, 5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(6, ADC_BASE, 6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(7, ADC_BASE, 7, THRESHOLDS);
        /*idSilicon=vmeMap.eventParameter(9, ADC_BASE, 8, THRESHOLDS);
        idNaI1=vmeMap.eventParameter(10, ADC_BASE, 9, THRESHOLDS);
        idNaI2=vmeMap.eventParameter(11, ADC_BASE, 10, THRESHOLDS);
        idFrontR=vmeMap.eventParameter(12, TDC_BASE, 0, THRESHOLDS);
        idFrontL=vmeMap.eventParameter(13, TDC_BASE, 1, THRESHOLDS);
        idRearR=vmeMap.eventParameter(14, TDC_BASE, 2, THRESHOLDS);
        idRearL=vmeMap.eventParameter(15, TDC_BASE, 3, THRESHOLDS);*/
//NEW        System.err.println("# Parameters: "+NUM_PARAMETERS);
        System.err.println("# Parameters: "+getEventSize());//NEW
        System.err.println("ADC channels: "+ADC_CHANNELS);
        System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
        System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);
//NEW        setEventSize(NUM_PARAMETERS);
        hCthd      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS, "Cathode Raw ");
        hAnde      =new Histogram("Anode       ", HIST_1D_INT, ADC_CHANNELS, "Anode Raw");
        hSntrSum    =new Histogram("ScintSum    ", HIST_1D_INT, ADC_CHANNELS, "Scintillator Sum");
        hFrntPsn    =new Histogram("FrontPosn    ", HIST_1D_INT, ADC_CHANNELS, "Front Wire Position");
        hRearPsn    =new Histogram("RearPosn     ", HIST_1D_INT, ADC_CHANNELS, "Rear Wire Position");
        hFrntHgh    =new Histogram("FrontHeight   ", HIST_1D_INT, ADC_CHANNELS, "Front Wire Pulse Height");
        hRearHgh    =new Histogram("RearHeight    ", HIST_1D_INT, ADC_CHANNELS, "Rear Wire Pulse Height");
        hFrntPH      =new Histogram("FrontPvsHeight", HIST_2D_INT,  TWO_D_CHANNELS,
        "Pulse Height vs Front Position","Front Position","Pulse Height");
        hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Pulse Height vs Rear Position","Rear Position", "Pulse Height");
        hAndeCthd   =new Histogram( "Ande Vs Cthd ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Cathode ","Cathode","Anode");
        hAndeFrnt   =new Histogram( "Ande Vs Frnt ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Front Position","Front Position","Anode");
        hCthdFrnt   =new Histogram( "Cthd Vs Frnt ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Cathode vs Front Position","Front Position","Cathode");
        hCthdSntr   =new Histogram( "Cthd Vs Sntr ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Cathode vs Scintillator","Scintillator","Cathode");
        hSntrFrnt   =new Histogram( "Sntr Vs Frnt ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Scintillator vs Front Position","Front Position","Scintillator");
        hAndeSntr   =new Histogram( "Ande Vs Sntr ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Scintillator","Scintillator","Anode");
        hAndeCthdG5   =new Histogram( "Ande Vs CthdG", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Cathode gated","Cathode","Anode");
        hAndeFrntG5   =new Histogram( "Ande Vs FrntG", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Front Position gated","Front Position","Anode");
        hCthdFrntG5   =new Histogram( "Cthd Vs FrntG", HIST_2D_INT,  TWO_D_CHANNELS,
        "Cathode vs Front Position gated","Front Position","Cathode");
        hCthdSntrG5   =new Histogram( "Cthd Vs SntrG", HIST_2D_INT,  TWO_D_CHANNELS,
        "Cathode vs Scintillator gated","Scintillator","Cathode");
        hSntrFrntG5   =new Histogram( "Sntr Vs FrntG", HIST_2D_INT,  TWO_D_CHANNELS,
        "Scintillator vs Front Position gated","Front Position","Scintillator");
        hAndeSntrG5   =new Histogram( "Ande Vs SntrG", HIST_2D_INT,  TWO_D_CHANNELS,
        "Anode vs Scintillator gated","Scintillator","Anode");
        hRearFrnt = new Histogram( "Rear Vs Frnt ", HIST_2D_INT,  TWO_D_CHANNELS,
        "Rear Position vs Front Position","Front Position","Rear Position");
        hRearFrntPID = new Histogram( "Rear Vs FrntG", HIST_2D_INT,  TWO_D_CHANNELS,
        "Rear Position vs Front Position gated PID","Front Position","Rear Position");
        hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
        hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
        hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        hFrntNew  =new Histogram("FrntNew", HIST_1D, ADC_CHANNELS, "Front Position Corrected  - New ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");
        hcFrntNew = new Histogram("FrntNwCmp", HIST_1D, COMPRESSED_CHANNELS,"Compressed Front Position Corrected - New ScCa,FwCa,FwSc,FwRw gates", "channel", "counts");
        //correlation stuff
        //Rear gated peaks, Front-Rear Correlations, raw and corrected
        Gate[] gFR=new Gate[NUM_GATES];
        for (int i=0;i<NUM_GATES;i++){
            hRearPeak[i]   =new Histogram("RearPeak"+i, HIST_1D, ADC_CHANNELS, " Rear Position - Peak "+i);
            hFtRrCorr[i]   =new Histogram("FtRrCorr"+i, HIST_2D, 128, " Front Rear Correlation - Peak "+i);
            pCentFt[i]=new DataParameter("Cent Frnt "+i);
            pCentRr[i]=new DataParameter("Cent Rear "+i);
            gPeak[i]  =new Gate("Peak "+i, hFrntGAll);
            hFtRrNewCr[i]  =new Histogram("FtRrCorrNew"+i, HIST_2D, 128, "New Front Rear Correlation - Peak "+i);
            hCthdTheta[i]  =new Histogram("CathThet"+i, HIST_2D, 256,  " Cathode  vs. Theta - Peak"+i,"Theta","Cathode");
            hCthdThNew[i]  =new Histogram("CathThNew"+i, HIST_2D, 256,  " Cathode  vs. Theta Corrected - Peak"+i,"Theta","Cathode");
            hSntrTheta[i]  =new Histogram("ScintThet"+i, HIST_2D, 256,  " Scintillator vs. Theta - Peak"+i,"Theta","Scintillator");
            hSntrThNew[i]  =new Histogram("ScintThNew"+i, HIST_2D, 256,  " Scintillator  vs. Theta Corrected - Peak"+i,"Theta","Scintillator");
            hAndeTheta[i]  =new Histogram("AndeThet"+i, HIST_2D, 256,  " Anode vs. Theta - Peak"+i,"Theta","Scintillator");
            hAndeThNew[i]  =new Histogram("AndeThNew"+i, HIST_2D, 256,  " Anode vs. Theta Corrected - Peak"+i,"Theta","Scintillator");
            gScintC[i]  =new Gate("ScintC"+i, hSntrTheta[i]);
            gCathC[i]  =new Gate("CathC "+i, hCthdTheta[i]);
            gAndeC[i] = new Gate("AndeC "+i, hAndeTheta[i]);
            gFR[i] = new Gate("FR "+i,hFtRrCorr[i]);
        }
        pSlopeFa=new DataParameter("SlopeFR a ");
        pSlopeFb=new DataParameter("SlopeFR b ");
        pSlopeFx0=new DataParameter("SlopeFR x0");
        pOffset=new DataParameter("Offset");
        pSlopeCa=new DataParameter("SlopeCath a ");
        pSlopeCb=new DataParameter("SlopeCath b ");
        pSlopeCx0=new DataParameter("SlopeCath x0");
        pSlopeSa=new DataParameter("SlopeScint a");
        pSlopeSb=new DataParameter("SlopeScint b");
        pSlopeSx0=new DataParameter("SlopeScintx0");
        pSlopeAa=new DataParameter("SlopeAnode a");
        pSlopeAb=new DataParameter("SlopeAnode b");
        pSlopeAx0=new DataParameter("SlopeAnodex0");

        // gates 1d
        //gCthd   =new Gate("Counts", hCthd);
        //gSilicon    = new Gate("Elastics", hSilicon);
        //gGood  =new Gate("GoodEvent",hFrntGAll);
        //gates  2d
        gAndeCthd = new Gate("An-Ca", hAndeCthd); hAndeCthdG5.addGate(gAndeCthd);
        gAndeFrnt = new Gate("An-Fw", hAndeFrnt); hAndeFrntG5.addGate(gAndeFrnt);
        gCthdFrnt   =new Gate("Fw-Ca", hCthdFrnt); hCthdFrntG5.addGate(gCthdFrnt);
        gCthdSntr   =new Gate("Ca-Sc", hCthdSntr); hCthdSntrG5.addGate(gCthdSntr);
        gSntrFrnt   =new Gate("Fw-Sc", hSntrFrnt); hSntrFrntG5.addGate(gSntrFrnt);
        gAndeSntr = new Gate("An-Sc", hAndeSntr); hAndeSntrG5.addGate(gAndeSntr);
        gRearFrnt   =new Gate("Rw-Fw", hRearFrnt); hRearFrntPID.addGate(gRearFrnt);
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
        int eSil    = dataEvent[idSilicon];
        int eNaI1 = dataEvent[idNaI1];
        int eNaI2 = dataEvent[idNaI2];
        int eSntr=(int)Math.round(Math.sqrt(eSntr1*eSntr2));
        //int eSntr=eSntr1;
        int ecFPsn=eFPsn>>TWO_D_FACTOR;
        int ecRPsn=eRPsn>>TWO_D_FACTOR;
        int ecFHgh=eFHgh>>TWO_D_FACTOR;
        int ecRHgh=eRHgh>>TWO_D_FACTOR;
        int ecSntr=eSntr>>TWO_D_FACTOR;
        int ecSntr1=eSntr1>>TWO_D_FACTOR;
        int ecSntr2=eSntr2>>TWO_D_FACTOR;
        int ecCthd=eCthd>>TWO_D_FACTOR;
        int ecAnde=eAnde>>TWO_D_FACTOR;

        //correlation stuff
        double offset=pOffset.getValue();
        double slopeFa=pSlopeFa.getValue();
        double slopeFb=pSlopeFb.getValue();
        double slopeFx0=pSlopeFx0.getValue();
        double slopeCa=pSlopeCa.getValue();
        double slopeCb=pSlopeCb.getValue();
        double slopeCx0=pSlopeCx0.getValue();
        double slopeSa=pSlopeSa.getValue();
        double slopeSb=pSlopeSb.getValue();
        double slopeSx0=pSlopeSx0.getValue();
        double slopeAa=pSlopeAa.getValue();
        double slopeAb=pSlopeAb.getValue();
        double slopeAx0=pSlopeAx0.getValue();
        for (int i=0;i<NUM_GATES; i++) {
            FrntCent[i]=(int)pCentFt[i].getValue();
            RearCent[i]=(int)pCentRr[i].getValue();
        }
        double slopeF=slopeFa+slopeFb*(eFPsn - slopeFx0);
        double slopeC=slopeCa+slopeCb*(eFPsn - slopeCx0);
        double slopeS=slopeSa+slopeSb*(eFPsn - slopeSx0);
        double slopeA=slopeAa+slopeAb*(eFPsn - slopeAx0);

        double diff=eRPsn-eFPsn-offset;
        int front=Math.max((int)(eFPsn-slopeF*diff),0);
        int cfront=front>>COMPRESS_FACTOR;
        int scintN=Math.max((int)(ecSntr-slopeS*diff),0);
        int cathN=Math.max((int)(ecCthd-slopeC*diff),0);
        int andeN=Math.max((int)(ecAnde-slopeA*diff),0);
        int theta=(int) (128+diff);

        // singles spectra
        hCthd.inc(eCthd);
        hAnde.inc(eAnde);
        hSntrSum.inc(eSntr);
        hFrntPsn.inc(eFPsn);
        hRearPsn.inc(eRPsn);
        hFrntHgh.inc(eFHgh);
        hRearHgh.inc(eRHgh);

        //singles 2d spectra
        hFrntPH.inc(ecFPsn,ecFHgh);
        hRearPH.inc(ecRPsn,ecRHgh);
        hAndeCthd.inc(ecCthd,ecAnde);
        hAndeFrnt.inc(ecFPsn,ecAnde);
        hCthdFrnt.inc(ecFPsn,ecCthd);
        hCthdSntr.inc(ecSntr,ecCthd);
        hSntrFrnt.inc(ecFPsn,ecSntr);
        hAndeSntr.inc(ecSntr,ecAnde);

        hRearFrnt.inc(ecFPsn,ecRPsn);
        
        boolean bAC = gAndeCthd.inGate(ecCthd,ecAnde);

        if (bAC &&
        (gAndeFrnt.inGate(ecFPsn,ecAnde)) &&
        (gCthdFrnt.inGate(ecFPsn,ecCthd)) &&
        (gCthdSntr.inGate(ecSntr,ecCthd)) &&
        (gAndeSntr.inGate(ecSntr,ecAnde)) ) hSntrFrntG5.inc(ecFPsn,ecSntr);

        if (bAC &&
        (gAndeFrnt.inGate(ecFPsn,ecAnde)) &&
        (gCthdFrnt.inGate(ecFPsn,ecCthd)) &&
        (gSntrFrnt.inGate(ecFPsn,ecSntr)) &&
        (gAndeSntr.inGate(ecSntr,ecAnde)) ) hCthdSntrG5.inc(ecSntr,ecCthd);

        if (bAC &&
        (gAndeFrnt.inGate(ecFPsn,ecAnde)) &&
        (gCthdSntr.inGate(ecSntr,ecCthd)) &&
        (gSntrFrnt.inGate(ecFPsn,ecSntr)) &&
        (gAndeSntr.inGate(ecSntr,ecAnde)) ) hCthdFrntG5.inc(ecFPsn,ecCthd);

        if (bAC &&
        (gCthdFrnt.inGate(ecFPsn,ecCthd)) &&
        (gCthdSntr.inGate(ecSntr,ecCthd)) &&
        (gSntrFrnt.inGate(ecFPsn,ecSntr)) &&
        (gAndeSntr.inGate(ecSntr,ecAnde)) ) hAndeFrntG5.inc(ecFPsn,ecAnde);

        if ((gAndeFrnt.inGate(ecFPsn,ecAnde)) &&
        (gCthdFrnt.inGate(ecFPsn,ecCthd)) &&
        (gCthdSntr.inGate(ecSntr,ecCthd)) &&
        (gSntrFrnt.inGate(ecFPsn,ecSntr)) &&
        (gAndeSntr.inGate(ecSntr,ecAnde)) ) hAndeCthdG5.inc(ecCthd,ecAnde);

        if (bAC &&
        (gAndeFrnt.inGate(ecFPsn,ecAnde)) &&
        (gCthdFrnt.inGate(ecFPsn,ecCthd)) &&
        (gCthdSntr.inGate(ecSntr,ecCthd)) &&
        (gSntrFrnt.inGate(ecFPsn,ecSntr)) ) hAndeSntrG5.inc(ecSntr,ecAnde);

        if (bAC &&
        (gAndeFrnt.inGate(ecFPsn,ecAnde)) &&
        (gCthdFrnt.inGate(ecFPsn,ecCthd)) &&
        (gCthdSntr.inGate(ecSntr,ecCthd)) &&
        (gSntrFrnt.inGate(ecFPsn,ecSntr)) &&
        (gAndeSntr.inGate(ecSntr,ecAnde)) ){
            hRearFrntPID.inc(ecFPsn,ecRPsn);
            hFrntNew.inc(front);
            hcFrntNew.inc(cfront);
            for (int i=0;i<NUM_GATES;i++){
                if (gPeak[i].inGate(eFPsn)){
                    hRearPeak[i].inc(eRPsn);
                    hCthdTheta[i].inc( theta, ecCthd);
                    hCthdThNew[i].inc( theta, cathN);
                    hSntrTheta[i].inc( theta, ecSntr);
                    hSntrThNew[i].inc( theta, scintN);
                    hAndeTheta[i].inc(theta, ecAnde);
                    hAndeThNew[i].inc(theta, andeN);
                    hFtRrCorr[i].inc(64+(eRPsn-RearCent[i])/2, 64+(eFPsn-FrntCent[i])/2);
                    hFtRrNewCr[i].inc(64+(eRPsn-RearCent[i])/2, 64+(front-FrntCent[i])/2);
                }
            }
        }

        if (bAC &&
        (gAndeFrnt.inGate(ecFPsn,ecAnde)) &&
        (gCthdFrnt.inGate(ecFPsn,ecCthd)) &&
        (gCthdSntr.inGate(ecSntr,ecCthd)) &&
        (gSntrFrnt.inGate(ecFPsn,ecSntr)) &&
        (gAndeSntr.inGate(ecSntr,ecAnde)) &&
        ( gRearFrnt.inGate(ecFPsn,ecRPsn) )){
            hFrntGAll.inc(eFPsn);
            hRearGAll.inc(eRPsn);
            hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
            hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
            writeEvent(dataEvent);
        }
    }

}
