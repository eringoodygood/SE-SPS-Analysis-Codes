/*
 */
package sort.VME;
import jam.data.*;
import jam.sort.*;

/*
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified October 2000 by Dale Visser
 */
public class EngeNoScintAndSurfaceBarriers extends SortRoutine {

    // ungated spectra
    Histogram hCthd,hAnde, hFrntPsn, hRearPsn;
    Histogram hFrntHgh;    //front Wire Pulse Height
    Histogram hRearHgh;    //Rear Wire Pulse Height
    Histogram hFrntPH;  // position x height y
    Histogram hRearPH;
    Histogram hCthdAnde, hFrntCthd, hFrntAnde, hFrntPRearP;
    Histogram hSilicon1, hSilicon2;
    Histogram hFrntTDC, hRearTDC;

    Histogram hCthdAndeGFC, hFrntAndeGFC;//gate by Front wire Cathode
    Histogram hCthdAndeGFA, hFrntCthdGFA;//gate by Front wire Anode
    Histogram hFrntAndeGCA, hFrntCthdGCA;//gate by Cathode Anode
    Histogram hFrntGPID, hRearGPID, hFrntRearGPID, hFrntGAll, hRearGAll;//front and rear wire gate on all
    Histogram hcFrntGAll, hcRearGAll;//front and rear wire gated on All compressed

    Gate gSilicon1, gSilicon2, gGood, gCthd;//gates 1 d
    Gate gSntrCthd, gFrntSntr, gFrntCthd, gFrntRear;//gates 2 d
    Gate gCthdAnde,gFrntAnde;
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt, sCathode, sSilicon1, sSilicon2;//scalers
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept, mScint, mCathode;//monitors

    static final int ADC_BASE = 0x20000000;
    static final int ADC_2_BASE = 0x20010000;
    static final int TDC_BASE = 0x30000000;
    static final int TDC_2_BASE = 0x30010000;
    static final int THRESHOLDS = 30;//minimum channel in ADC spectrum

    //id numbers for the signals;
    int idCthd, idAnde, idFrntPsn, idRearPsn, idFrntHgh,
    idRearHgh, idSilicon1, idSilicon2;
    int NUM_PARAMETERS;

    int lastEvntAccpt;

    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed position histogram
    final int TWO_D_CHANNELS=512; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    public void initialize() throws Exception {
        vmeMap.setScalerInterval(3);
        //id numbers for the signals;
        idCthd=vmeMap.eventParameter(1, ADC_BASE, 0, THRESHOLDS);
        idAnde=vmeMap.eventParameter(2, ADC_BASE, 1, THRESHOLDS);
        idSilicon1 = vmeMap.eventParameter(3, ADC_BASE, 2, THRESHOLDS);
        idSilicon2 = vmeMap.eventParameter(4, ADC_BASE, 3, THRESHOLDS);
        idFrntPsn=vmeMap.eventParameter(5, ADC_BASE, 4, THRESHOLDS);
        idRearPsn=vmeMap.eventParameter(6, ADC_BASE, 5, THRESHOLDS);
        idFrntHgh=vmeMap.eventParameter(7, ADC_BASE, 6, THRESHOLDS);
        idRearHgh=vmeMap.eventParameter(8, ADC_BASE, 7, THRESHOLDS);

        System.err.println("# Parameters: "+getEventSize());
        System.err.println("ADC channels: "+ADC_CHANNELS);
        System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
        System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);
        hCthd      =new Histogram("Cathode     ", HIST_1D_INT, ADC_CHANNELS, "Cathode Raw ");
        hAnde      =new Histogram("Anode       ", HIST_1D_INT, ADC_CHANNELS, "Anode Raw");
        hFrntPsn    =new Histogram("FrontPosn    ", HIST_1D_INT, ADC_CHANNELS, "Front Wire Position");
        hRearPsn    =new Histogram("RearPosn     ", HIST_1D_INT, ADC_CHANNELS, "Rear Wire Position");
        hFrntHgh    =new Histogram("FrontHeight   ", HIST_1D_INT, ADC_CHANNELS, "Front Wire Pulse Height");
        hRearHgh    =new Histogram("RearHeight    ", HIST_1D_INT, ADC_CHANNELS, "Rear Wire Pulse Height");
        hSilicon1 = new Histogram("Silicon 1", HIST_1D_INT, ADC_CHANNELS,"Silicon Detector 1");
        hSilicon2 = new Histogram("Silicon 2", HIST_1D_INT, ADC_CHANNELS,"Silicon Detector 2");
        hFrntPH      =new Histogram("FrontPvsHeight", HIST_2D_INT,  TWO_D_CHANNELS, "Pulse Height vs Front Position","Front Position","Pulse Height");
        hRearPH      =new Histogram("RearPvsHeight ", HIST_2D_INT,  TWO_D_CHANNELS, "Pulse Height vs Rear Position","Rear Position", "Pulse Height");
        hCthdAnde   =new Histogram("CathodeAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Anode ","Cathode","Anode");
        hFrntCthd   =new Histogram("FrontCathode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position","Front Position","Cathode");
        hFrntAnde   =new Histogram("FrontAnode  ", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Front Position","Front Position","Anode");
        hFrntPRearP =new Histogram("FrontRear  ", HIST_2D_INT,  TWO_D_CHANNELS, "Rear Position vs Front Position","Front Position","Rear Position");
        //gate on Cathode Anode
        hFrntCthdGCA=new Histogram("FrontCathodeGCA", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - CaAn gate","Front Position", "Cathode");
        hFrntAndeGCA=new Histogram("FrontAnodeGCA", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Front Position - CaAn gate","Front Position","Anode");
        //gate on Front Wire Cathode
        hCthdAndeGFC=new Histogram("CathodeAndeGFC", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Cathode - FwCa gate", "Cathode","Anode");
        hFrntAndeGFC=new Histogram("FrontAnodeGFC", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Front Position - FwCa gate","Front Position", "Anode");
        //gate on Front Wire Anode
        hCthdAndeGFA=new Histogram("CthdAnodeGFA", HIST_2D_INT,  TWO_D_CHANNELS, "Anode vs Cathode - FwAn gate","Cathode","Anode");
        hFrntCthdGFA=new Histogram("FrontCathodeGFA ", HIST_2D_INT,  TWO_D_CHANNELS, "Cathode vs Front Position - FwAn gate ","Front Position","Cathode");
        //gated on 3 gates
        hFrntGPID   =new Histogram("FrontGPID", HIST_1D_INT, ADC_CHANNELS, "Front Position - PID gates");
        hRearGPID   =new Histogram("RearGPID", HIST_1D_INT, ADC_CHANNELS, "Rear Position - PID gates");
        hFrntRearGPID=new Histogram("FRGatePID",HIST_2D_INT, TWO_D_CHANNELS,"Front vs. Rear - PID gates");
        //gated on 4 gates
        hFrntGAll   =new Histogram("FrontGAll    ", HIST_1D_INT, ADC_CHANNELS, "Front Position - ScCa,FwCa,FwSc,FwRw gates");
        hRearGAll   =new Histogram("RearGAll    ", HIST_1D_INT, ADC_CHANNELS, "Rear Position - ScCa,FwCa,FwSc,FwRw gates");
        hcFrntGAll  =new Histogram("FrontGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Front Position compressed - ScCa,FwCa,FwSc,FwRw gates");
        hcRearGAll  =new Histogram("RearGAllcmp ", HIST_1D_INT, COMPRESSED_CHANNELS, "Rear Position compressed - ScCa,FwCa,FwSc,FwRw gates");

        // gates 1d
        gCthd   =new Gate("Cath Counts", hCthd);
        gSilicon1    = new Gate("Silicon 1", hSilicon1);
        gSilicon2 = new Gate("Silicon 2", hSilicon2);
        gGood  =new Gate("GoodEvent",hFrntGAll);
        //gates  2d
        gCthdAnde   =new Gate("Ca-An", hCthdAnde);      //gate on Scintillator Cathode
        gFrntCthd   =new Gate("Fw-Ca", hFrntCthd);          //gate on Front Scintillator
        gFrntAnde   =new Gate("Fw-An", hFrntAnde);      //gate on Front Cathode
        gFrntRear   =new Gate("Fw-Rw", hFrntPRearP);      //gate on Front Rear
        hFrntAndeGCA.addGate(gFrntAnde);
        hFrntCthdGCA.addGate(gFrntCthd);
        hCthdAndeGFC.addGate(gCthdAnde);
        hFrntAndeGFC.addGate(gFrntAnde);
        hCthdAndeGFA.addGate(gCthdAnde);
        hFrntCthdGFA.addGate(gFrntCthd);
        hFrntRearGPID.addGate(gFrntRear);
        
        //scalers
        sBic      =new Scaler("BIC",0);
        sClck      =new Scaler("Clock",1);
        sEvntRaw    =new Scaler("Event Raw", 2);
        sEvntAccpt  =new Scaler("Event Accept",3);
        sCathode  =new Scaler("Cathode",4);
        sSilicon1  =new Scaler("Silicon 1",5);
        sSilicon2  =new Scaler("Silicon 2",6);
        
        int SCALER_ADDRESS = 0xf0e00000;
        /* obsolete 
        vmeMap.scalerParameter(2048+sBic.getNumber(), SCALER_ADDRESS, sBic.getNumber(), sBic);
        vmeMap.scalerParameter(2048+sClck.getNumber(), SCALER_ADDRESS, sClck.getNumber(), sClck);
        vmeMap.scalerParameter(2048+sEvntRaw.getNumber(), SCALER_ADDRESS, sEvntRaw.getNumber(), sEvntRaw);
        vmeMap.scalerParameter(2048+sEvntAccpt.getNumber(), SCALER_ADDRESS, sEvntAccpt.getNumber(), sEvntAccpt);
        vmeMap.scalerParameter(2048+sCathode.getNumber(), SCALER_ADDRESS, sCathode.getNumber(), sCathode);
        vmeMap.scalerParameter(2048+sSilicon1.getNumber(), SCALER_ADDRESS, sSilicon1.getNumber(), sSilicon1);
        vmeMap.scalerParameter(2048+sSilicon2.getNumber(), SCALER_ADDRESS, sSilicon2.getNumber(), sSilicon2);
		*/
        //monitors
        mBeam=new Monitor("Beam ",sBic);
        mClck=new Monitor("Clock",sClck);
        mEvntRaw=new Monitor("Raw Events",sEvntRaw);
        mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
        mCathode=new Monitor("Cathode",sCathode);
        Monitor mLiveTime=new Monitor("Live Time", this);
    }

    public void sort(int [] dataEvent) throws Exception {
        //unpack data into convenient names
        int eCthd   =dataEvent[idCthd];
        int eAnde   =dataEvent[idAnde];
        int eFPsn   =dataEvent[idFrntPsn];
        int eRPsn   =dataEvent[idRearPsn];
        int eFHgh   =dataEvent[idFrntHgh];
        int eRHgh   =dataEvent[idRearHgh];
        int eSil1    = dataEvent[idSilicon1];
        int eSil2 =  dataEvent[idSilicon2];

        int ecFPsn=eFPsn>>TWO_D_FACTOR;
        int ecRPsn=eRPsn>>TWO_D_FACTOR;
        int ecFHgh=eFHgh>>TWO_D_FACTOR;
        int ecRHgh=eRHgh>>TWO_D_FACTOR;
        int ecCthd=eCthd>>TWO_D_FACTOR;
        int ecAnde=eAnde>>TWO_D_FACTOR;

        // singles spectra
        hCthd.inc(eCthd);
        hAnde.inc(eAnde);
        hFrntPsn.inc(eFPsn);
        hRearPsn.inc(eRPsn);
        hFrntHgh.inc(eFHgh);
        hRearHgh.inc(eRHgh);
        hSilicon1.inc(eSil1);
        hSilicon2.inc(eSil2);

        //singles 2d spectra
        hFrntPH.inc(ecFPsn,ecFHgh);
        hRearPH.inc(ecRPsn,ecRHgh);
        hCthdAnde.inc(ecCthd,ecAnde);
        hCthdAnde.inc(ecCthd,ecAnde);
        hFrntCthd.inc(ecFPsn,ecCthd);
        hFrntAnde.inc(ecFPsn,ecAnde);
        hFrntPRearP.inc(ecFPsn,ecRPsn);
        
        boolean bCA = gCthdAnde.inGate(ecCthd,ecAnde);
        boolean bFC = gFrntCthd.inGate(ecFPsn,ecCthd);
        boolean bFA = gFrntAnde.inGate(ecFPsn,ecAnde);
        boolean bPID = bCA && bFC && bFA;//all particle ID gates
        boolean bFR = gFrntRear.inGate(ecFPsn,ecRPsn); //proper trajectory in front vs. rear
        
        if (bCA) {
            hFrntCthdGCA.inc(ecFPsn,ecCthd);
            hFrntAndeGCA.inc(ecFPsn,ecAnde);
        }
        if (bFC) {
            hCthdAndeGFC.inc(ecCthd,ecAnde);
            hFrntAndeGFC.inc(ecFPsn,ecAnde);
        }
        if (bFA){
            hCthdAndeGFA.inc(ecCthd,ecAnde);
            hFrntCthdGFA.inc(ecFPsn,ecCthd);
        }
        if (bPID){// gated on all 3 gate above
            hFrntGPID.inc(eFPsn);
            hRearGPID.inc(eRPsn);
            hFrntRearGPID.inc(ecFPsn,ecRPsn);
            if (bFR) {//proper front vs. rear as well
                hFrntGAll.inc(eFPsn);
                hRearGAll.inc(eRPsn);
                hcFrntGAll.inc(eFPsn>>COMPRESS_FACTOR);
                hcRearGAll.inc(eRPsn>>COMPRESS_FACTOR);
                writeEvent(dataEvent);
            }
        }
    }

    /**
     * monitor method
     * calculate the live time
     */
    public double monitor(String name){
        if (name.equals("Live Time")){
            if (((double)(mEvntRaw.getValue()))>0.0){
                return 100.0*mEvntAccept.getValue()/mEvntRaw.getValue();
            } else {
                return 0.0;
            }
        } else {
            return 50.0;
        }
    }
}
