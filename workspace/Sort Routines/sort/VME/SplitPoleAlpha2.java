/*
 */
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
public class SplitPoleAlpha2 extends SortRoutine {
    

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
    Histogram hFrntSntrGSCFC, hFrntCthdGSCFS, hSntrCthdGFSFC;//gated by 2 PID's
    Histogram hFrntGCSF, hRearGCSF, hFrntRearGCSF, hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed
    Histogram hScint1Scint2, hScint1Scint2GFC; Gate gScint1Scint2;

    Gate gSilicon, gCthd, gGood;//gates 1 d
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear;//gates 2 d
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sScint, sCathode;//scalers
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors

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
    Histogram [] hScintTheta = new Histogram[NUM_GATES];
    Histogram [] hScintThNew = new Histogram[NUM_GATES];
    Gate [] gPeak= new Gate[NUM_GATES];
    Gate [] gScintC= new Gate[NUM_GATES];//these gates appear in the correlation plots
    Gate [] gCathC= new Gate[NUM_GATES];//and facilitate calculating the slopes
    DataParameter pSlopeFa; // slope = a + b * (x-x0)
    DataParameter pSlopeFb, pSlopeFx0, pSlopeF, pSlopeCa, pSlopeCb, pSlopeCx0, pSlopeS, pSlopeSa, pSlopeSb;
    DataParameter pSlopeSx0, pOffset;
    DataParameter pCentFt[]=new DataParameter[NUM_GATES];
    DataParameter pCentRr[]=new DataParameter[NUM_GATES];

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
        hScint1Scint2 = new Histogram("Scint1Scint2", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator 2 vs. Scintillator 1","Scintillator 1","Scintillator 2");
        //gate on Scintillator Cathode
        hFrntSntrGSC=new Histogram("FrontScintGSC ", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position - ScCa gate","Front Position", "Scintillator");
        hFrntCthdGSC=new Histogram("FrontCathodeGSC", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - ScCa gate","Front Position","Cathode");
        //gate on Front Wire Cathode
        hSntrCthdGFC=new Histogram("ScintCathodeGFC", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator - FwCa gate", "Scintillator","Cathode");
        hFrntSntrGFC=new Histogram("FrontScintGFC", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position - FwCa gate","Front Position", "Scintillator");
        hScint1Scint2GFC = new Histogram("Sc1Sc2GFC", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator 2 vs. Scintillator 1 - FwCa gate","Scintillator 1","Scintillator 2");
        //gate on Front Wire Scintillator
        hSntrCthdGFS=new Histogram("ScintCathodeGFS", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator - FwSc gate","Scintillator","Cathode");
        hFrntCthdGFS=new Histogram("FrontCathodeGFS ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - FwSc gate ","Front Position","Cathode");
        //gated on 2 PID's
        hFrntSntrGSCFC=new Histogram("FrontScintGSCFC", HIST_2D_INT,  TWO_D_CHANNELS, "Scintillator vs Front Position - ScCa & FwCa gates","Front Position", "Scintillator");
        hFrntCthdGSCFS=new Histogram("FrontCathodeGSCFS ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - ScCa & FwSc gates ","Front Position","Cathode");
        hSntrCthdGFSFC=new Histogram("ScintCathodeGFSFC", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Scintillator - FwSc & FwCa gates","Scintillator","Cathode");
        //gated on 3 gates
        hFrntGCSF   =new Histogram("FrontGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc gates");
        hRearGCSF   =new Histogram(   "RearGCSF    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc gates");
        hFrntRearGCSF=new Histogram("FRGateCSF  ",HIST_2D_INT, TWO_D_CHANNELS,"Front vs. Rear - ScCa, FwCa, FwSc gates");
        //gated on 4 gates
        hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
        hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
        hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        //correlation stuff
        //Rear gated peaks, Front-Rear Correlations, raw and corrected
        for (int i=0;i<NUM_GATES;i++){
            hRearPeak[i]   =new Histogram("RearPeak"+i, HIST_1D, 2048, " Rear Position - Peak "+i);
            hFtRrCorr[i]   =new Histogram("FtRrCorr"+i, HIST_2D, 128, " Front Rear Correlation - Peak "+i);
            pCentFt[i]=new DataParameter("Cent Frnt "+i);
            pCentRr[i]=new DataParameter("Cent Rear "+i);
            gPeak[i]  =new Gate("Peak "+i, hFrntGAll);
            hFtRrNewCr[i]  =new Histogram("FtRrCorrNew"+i, HIST_2D, 128, "New Front Rear Correlation - Peak "+i);
            hCthdTheta[i]  =new Histogram("CathThet"+i, HIST_2D, 256,  " Cathode  vs. Theta - Peak"+i,"Theta","Cathode");
            hCthdThNew[i]  =new Histogram("CathThNew"+i, HIST_2D, 256,  " Cathode  vs. Theta Corrected - Peak"+i,"Theta","Cathode");
            hScintTheta[i]  =new Histogram("ScintThet"+i, HIST_2D, 256,  " Scintillator vs. Theta - Peak"+i,"Theta","Scintillator");
            hScintThNew[i]  =new Histogram("ScintThNew"+i, HIST_2D, 256,  " Scintillator  vs. Theta Corrected - Peak"+i,"Theta","Scintillator");
            gScintC[i]  =new Gate("ScintC"+i, hScintTheta[i]);
            gCathC[i]  =new Gate("CathC "+i, hCthdTheta[i]);
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

        // gates 1d
        gCthd   =new Gate("Counts", hCthd);
        gSilicon    = new Gate("Elastics", hSilicon);
        gGood  =new Gate("GoodEvent",hFrntGAll);
        //gates  2d
        gSntrCthd   =new Gate("Ca-Sc", hSntrCthd);      //gate on Scintillator Cathode
        gFrntSntr   =new Gate("Fw-Sc", hFrntSntr);          //gate on Front Scintillator
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);      //gate on Front Cathode
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        gScint1Scint2 = new Gate("Sc1-Sc2",hScint1Scint2);
        hScint1Scint2GFC.addGate(gScint1Scint2);
        hFrntSntrGSC.addGate(gFrntSntr);
        hFrntSntrGFC.addGate(gFrntSntr);
        hFrntSntrGSCFC.addGate(gFrntSntr);
        hFrntCthdGSC.addGate(gFrntCthd);
        hFrntCthdGFS.addGate(gFrntCthd);
        hFrntCthdGSCFS.addGate(gFrntCthd);
        hSntrCthdGFC.addGate(gSntrCthd);
        hSntrCthdGFS.addGate(gSntrCthd);
        hSntrCthdGFSFC.addGate(gSntrCthd);

        hFrntRearGCSF.addGate(gFrntRear);

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
        for (int i=0;i<NUM_GATES; i++) {
            FrntCent[i]=(int)pCentFt[i].getValue();
            RearCent[i]=(int)pCentRr[i].getValue();
        }
        double slopeF=slopeFa+slopeFb*(eFPsn - slopeFx0);
        double slopeC=slopeCa+slopeCb*(eFPsn - slopeCx0);
        double slopeS=slopeSa+slopeSb*(eFPsn - slopeSx0);
        int front=Math.max((int)(eFPsn-slopeF*(eRPsn-eFPsn-offset)),0);
        int cfront=front>>COMPRESS_FACTOR;
        int scintN=Math.max((int)(ecSntr-slopeS*(eRPsn-front-offset)),0);
        int cathN=Math.max((int)(ecCthd-slopeC*(eRPsn-front-offset)),0);
        int theta=(int) (128+eRPsn-eFPsn-offset);

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
        hScint1Scint2.inc(ecSntr1,ecSntr2);

        if ( gSntrCthd.inGate(ecSntr,ecCthd) ){// gate on Scintillator vs Cathode
            hFrntSntrGSC.inc(ecFPsn,ecSntr);
            hFrntCthdGSC.inc(ecFPsn,ecCthd);
        }
        if ( gFrntCthd.inGate(ecFPsn,ecCthd) ){// gate on Front Wire Position vs Cathode
            hSntrCthdGFC.inc(ecSntr,ecCthd);
            hFrntSntrGFC.inc(ecFPsn,ecSntr);
            hScint1Scint2GFC.inc(ecSntr1,ecSntr2);
        }
        if ( gFrntSntr.inGate(ecFPsn,ecSntr) ){// gate on Front Wire Position vs Scintillator
            hSntrCthdGFS.inc(ecSntr,ecCthd);
            hFrntCthdGFS.inc(ecFPsn,ecCthd);
        }

        //gated on 2 other PID gates
        if (gSntrCthd.inGate(ecSntr,ecCthd) && gFrntCthd.inGate(ecFPsn,ecCthd)) {
            hFrntSntrGSCFC.inc(ecFPsn,ecSntr);
        }
        if (gSntrCthd.inGate(ecSntr,ecCthd) && gFrntSntr.inGate(ecFPsn,ecSntr)) {
            hFrntCthdGSCFS.inc(ecFPsn,ecCthd);
        }
        if (gFrntSntr.inGate(ecFPsn,ecSntr) && gFrntCthd.inGate(ecFPsn,ecCthd)) {
            hSntrCthdGFSFC.inc(ecSntr,ecCthd);
        }

        if (( gSntrCthd.inGate(ecSntr,ecCthd) )&&
        ( gFrntCthd.inGate(ecFPsn,ecCthd) )&&
        ( gFrntSntr.inGate(ecFPsn,ecSntr) )){// gated on all 3 gate above
            hFrntGCSF.inc(eFPsn);
            hRearGCSF.inc(eRPsn);
            hFrntRearGCSF.inc(ecFPsn,ecRPsn);
            //hFrntNew.inc(front);
            //hcFrntNew.inc(cfront);
            for (int i=0;i<NUM_GATES;i++){
                if (gPeak[i].inGate(eFPsn)){
                    hRearPeak[i].inc(eRPsn);
                    hCthdTheta[i].inc( theta, ecCthd);
                    hCthdThNew[i].inc( theta, cathN);
                    hScintTheta[i].inc( theta, ecSntr);
                    hScintThNew[i].inc( theta, scintN);
                    hFtRrCorr[i].inc(64+eRPsn-RearCent[i], 64+eFPsn-FrntCent[i]);
                    hFtRrNewCr[i].inc(64+eRPsn-RearCent[i], 64+front-FrntCent[i]);
                }
            }
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
            writeEvent(dataEvent);
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
