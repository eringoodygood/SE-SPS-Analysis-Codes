/*
 */
package sort.VME;
import java.io.*;
import jam.data.*;
import jam.sort.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;
import sort.coinc.offline.ResidualKinematics;
import sort.coinc.offline.SurfaceAlphaEnergyLoss;
import dwvisser.analysis.ArrayCalibration;

/** Online sort routine for YLSA coincidence with Enge Spectrometer.
 * This was modified from <CODE>sort.coinc.SPplus3LEDA_v3</CODE>, which
 * was used in the January 2001 test run.
 * Changed 10 Aug 2001 to calculate the scintillator event the right way; also
 * added gate to cathAnde
 *
 * @author Dale Visser
 * @since 26 July 2001
 */
public class TwoADCtestReconstruct extends SortRoutine {
    //VME properties
    static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 0;//ADC lower threshold in channels
    static final int LAST_ADC_BIN = 3840;
    
    //histogramming constants
    final int ADC_CHANNELS = 4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed histogram
    final int TWO_D_CHANNELS = 512; //number of channels per dimension in 2-d histograms
    //bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));
    
    Histogram hDebug, hOne, hTwo, hTwoVsOne, hCountDiff, hDiffCorr, hDiffUncorr;
    Gate gTwoVsOneCorr, gTwoVsOneUncorr;
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt;//scalers
    Scaler sFCLR;//number of FCLR's that went to ADC's
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept;//monitors
    Monitor mFCLR;
    
    //id numbers for the signals;
    int idOne, idTwo;
    RecentADCevents [] events = new RecentADCevents[2];
    
    public void initialize() throws Exception {
        System.out.println("Short MAX_VALUE="+Short.MAX_VALUE);
        events[0] = new RecentADCevents();
        events[1] = new RecentADCevents();
        vmeMap.setScalerInterval(3);
        idOne=vmeMap.eventParameter(0, ADC_BASE[0], 1, THRESHOLDS);
        idTwo=vmeMap.eventParameter(1, ADC_BASE[1], 0, THRESHOLDS);
        
        hDebug = new Histogram("Debug", HIST_1D_INT, 8, "Debug Word");
        hOne = new Histogram("One", HIST_1D_INT, ADC_CHANNELS, "ADC One");
        hTwo = new Histogram("Two", HIST_1D_INT, ADC_CHANNELS, "ADC Two");
        hTwoVsOne = new Histogram("TwoVsOne", HIST_2D_INT, TWO_D_CHANNELS,
        "ADC Two vs. ADC One", "One", "Two");
        hCountDiff = new Histogram("Diff-2D", HIST_2D_INT, ADC_CHANNELS*8, 8,
        "Diff vs. Counter 0","ADC 0 Counter","ADC 1 counter - ADC 0 Counter");
        hDiffCorr = new Histogram("DiffCorr", HIST_1D_INT, 128, "Difference Correlated");
        hDiffUncorr = new Histogram("DiffUnCorr",  HIST_1D_INT, 128, "Difference Uncorrelated");
        gTwoVsOneCorr = new Gate("2vs1corr",hTwoVsOne);
        
        //scalers
        sBic      =new Scaler("BIC",0);
        sClck      =new Scaler("Clock",1);
        sEvntRaw    =new Scaler("Event Raw", 2);
        sEvntAccpt  =new Scaler("Event Accept",3);
        
        int SCALER_ADDRESS = 0xf0e00000;
        /* obsolete 
        vmeMap.scalerParameter(2048+sBic.getNumber(), SCALER_ADDRESS, sBic.getNumber(), sBic);
        vmeMap.scalerParameter(2048+sClck.getNumber(), SCALER_ADDRESS, sClck.getNumber(), sClck);
        vmeMap.scalerParameter(2048+sEvntRaw.getNumber(), SCALER_ADDRESS, sEvntRaw.getNumber(), sEvntRaw);
        vmeMap.scalerParameter(2048+sEvntAccpt.getNumber(), SCALER_ADDRESS, sEvntAccpt.getNumber(), sEvntAccpt);
        //vmeMap.scalerParameter(2048+sFCLR.getNumber(), SCALER_ADDRESS, sFCLR.getNumber(), sFCLR);
        */
        
        //monitors
        mBeam=new Monitor("Beam ",sBic);
        mClck=new Monitor("Clock",sClck);
        mEvntRaw=new Monitor("Raw Events",sEvntRaw);
        mEvntAccept=new Monitor("Accepted Events",sEvntAccpt);
        mFCLR = new Monitor("FCLR",sFCLR);
    }
    
    static final int EVENTS_UNTIL_SORT=2;
    int eventsUntilSort = EVENTS_UNTIL_SORT;
    public void sort(int [] dataEvent) throws Exception {
        int datumOne=0;
        int datumTwo=0;
        
        int [] ADC_event_counter = new int[2];
        int eDebug = dataEvent[0x200];
        if (eDebug>0) hDebug.inc(eDebug);
        
        for (int i=0; i<ADC_event_counter.length; i++){
            ADC_event_counter[i] = dataEvent[0x210 + 0x10*i];
            if (ADC_event_counter[i] < 0 )
                ADC_event_counter[i] = ((int)Short.MAX_VALUE-(int)Short.MIN_VALUE)+ADC_event_counter[i]+1;
            ADC_event_counter[i] += dataEvent[0x211 + 0x10*i] << 16;
            if (ADC_event_counter[i] != 0) events[i].addEvent(ADC_event_counter[i], dataEvent);
        }
        eventsUntilSort--;
        if (eventsUntilSort==0){
            eventsUntilSort=EVENTS_UNTIL_SORT;
            int [] eventsToSort = RecentADCevents.getSortableCounters();
            for (int i=0; i<eventsToSort.length; i++){
                int counter=eventsToSort[i];
                //System.out.print("Event "+counter+", ");
                if (events[0].hasCounter(counter))
                    datumOne = events[0].getEvent(counter)[idOne];
                if (events[1].hasCounter(counter))
                    datumTwo = events[1].getEvent(counter)[idTwo];
                hOne.inc(datumOne);
                hTwo.inc(datumTwo);
                hTwoVsOne.inc(datumOne>>COMPRESS_FACTOR, datumTwo>>COMPRESS_FACTOR);
            }
        }
        
    }
    
    /** Called so the dead time can be calculated.
     * @param name name of monitor to calculate
     * @return floating point value of monitor
     */
    public double monitor(String name){
        return 0.0;
    }
}
