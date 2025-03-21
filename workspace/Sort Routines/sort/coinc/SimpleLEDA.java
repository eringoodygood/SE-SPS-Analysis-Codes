/*
 */
package sort.coinc;
import jam.data.*;
import jam.sort.*;

/*
 * Simple sort routine for testing a single LEDA detector.
 * The number of strips parameter is adjustable from one to sixteen.
 * Behavior is that strips 0 thru NUM_STRIPS-1 are read out.
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Dale Visser
 * last modified October 2000 by Dale Visser
 */
public class SimpleLEDA extends SortRoutine {
    //VME properties
static final int [] ADC_BASE = {0x20000000,0x20010000};
static final int [] TDC_BASE = {0x30000000,0x30010000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 300;
    static final int TIME_THRESHOLDS = 20;
    static final int LAST_GOOD_BIN = 3840;
    double [] Am241Channels = {2207.0, 1972.0, 2197.9, 2232.0, 2185.70, 2269.4, 2171.7, 2258.0,
    2164.3,2177.2, 2239.8, 2176.3, 2184.4, 2186.5, 2137.2, 2283.4};
    double [] timeChannels = {1634.52, 1678.97, 1803.68, 1654.31, 1535.90, 1652.83, 1539.97, 1638.31,
    1636.25,1440.0, 1531.14, 1622.29, 1605.67, 1406.19, 1520.02, 1600.76};

    //1 LEDA detector
    //static final int NUM_DETECTORS = 1;
    static final int NUM_STRIPS = 16;
    //static final int SAMPLE_STRIP = 4;

    int eventCount=0;
    int lastCount;

int [] whichStrip = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    //histogramming constants
    final int ADC_CHANNELS=4096; //num of channels per ADC
    final int COMPRESSED_CHANNELS = 512; //number of channels in compressed position histogram
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    Histogram [] hEnergy = new Histogram[NUM_STRIPS];
    Histogram [] hTime = new Histogram[NUM_STRIPS];
    int [] idEnergy = new int[NUM_STRIPS];
    int [] idTime = new int[NUM_STRIPS];


    Gate gTime;
    Histogram hHits, hTimeHits, hEvsStrip, hTvsStrip;
    int lastEvntAccpt;
    //Histogram [][] hEvsE=new Histogram[NUM_STRIPS][NUM_STRIPS];
    Histogram hTvsEhits;

    public void initialize() throws Exception {
        vmeMap.setScalerInterval(3);

        vmeMap.setV775Range(whichTDC(0),600);//TDC range to 600 ns
        for (int strip=0; strip<NUM_STRIPS; strip++) {
            idEnergy[strip]=vmeMap.eventParameter(strip, whichADC(0), 
            whichChannel(0,whichStrip[strip]), THRESHOLDS);
            hEnergy[strip]=new Histogram("Energy "+strip, HIST_1D_INT, ADC_CHANNELS, "Energy "+strip);
        }
        for (int strip=0; strip<NUM_STRIPS; strip++) {
            idTime[strip]=vmeMap.eventParameter(strip+NUM_STRIPS, whichTDC(0), 
            whichTDCchannel(0,whichStrip[strip]), TIME_THRESHOLDS);
            hTime[strip]=new Histogram("Time "+strip, HIST_1D_INT, ADC_CHANNELS, "Time "+strip);
        }

        /*for (int i=0; i<NUM_STRIPS; i++) {
        for(int j=0; j<i; j++) {
        hEvsE[i][j] = new Histogram ("E_"+j+" vs E_"+i, HIST_2D_INT, TWO_D_CHANNELS,
        "Energy "+j+" vs. Energy "+i, "Energy "+i, "Energy "+j);
        }
        }*/

        hTvsEhits = new Histogram("T vs E hits", HIST_2D_INT, NUM_STRIPS, "Time hits vs Energy hits",
        "E hits", "T hits");


        System.err.println("# Parameters: "+getEventSize());
        System.err.println("ADC channels: "+ADC_CHANNELS);
        System.err.println("2d channels: "+TWO_D_CHANNELS+", compression factor: "+TWO_D_FACTOR);
        System.err.println("compressed channels: "+COMPRESSED_CHANNELS+", compression factor: "+COMPRESS_FACTOR);

        hHits = new Histogram("Hits", HIST_1D_INT, NUM_STRIPS, "Hits over ADC threshold",
        "Strip","Counts");
        hTimeHits = new Histogram("Time Hits", HIST_1D_INT, NUM_STRIPS, "Hits over TDC threshold",
        "Strip","Counts");
        hEvsStrip = new Histogram("EvsStrip",HIST_2D_INT, TWO_D_CHANNELS, "Energy vs. Strip", "Energy",
        "Strip");
        hTvsStrip = new Histogram("TvsStrip",HIST_2D_INT, TWO_D_CHANNELS, "Time vs. Strip", "Time",
        "Strip");
        //gTime=new Gate("Time", hTvsStrip);//gates on selected TDC channels

        //Scalers
        Scaler sEvntRaw    =new Scaler("Event Raw", 0);
        Scaler sEvntAccpt  =new Scaler("Event Accept",1);
        int SCALER_ADDRESS = 0xf0e00000;
        /* obsolete 
        vmeMap.scalerParameter(2048+sEvntRaw.getNumber(), SCALER_ADDRESS, 2, sEvntRaw);
        vmeMap.scalerParameter(2048+sEvntAccpt.getNumber(), SCALER_ADDRESS, 3, sEvntAccpt);
        */
    }

    //Utility methods for mapping strips to ADC channels

    /**
     * Returns which adc unit given which detector.
     */
    private int whichADC(int detector){
        if (detector == 0) {//Detector 0
            return ADC_BASE[0];
        } else {//Detectors 1 and 2
            return ADC_BASE[1];
        }
    }
    /**
     * Returns which tdc unit given which detector.
     */
    private int whichTDC(int detector){
        if (detector == 2) {//Detector 2
            return TDC_BASE[1];
        } else {//Detectors 0 and 1
            return TDC_BASE[1];
        }
    }

    /**
     * Returns which channel in the adc given which detector and strip.
     */
    private int whichChannel(int detector, int strip){
        if (detector == 0 || detector==2) {//Detectors 0 and 2
            return strip+16;
        } else {//Detector 1
            return strip;
        }
    }

    /**
     * Returns which channel in the tdc given which detector and strip.
     */
    private int whichTDCchannel(int detector, int strip){
        if (detector == 0 || detector==2) {//Detectors 0 and 2
            return strip+16;
        } else {//Detector 1
            return strip+16;
        }
    }

    public void sort(int [] dataEvent) throws Exception {
        int debug=dataEvent[0x200];
        int triggerCount = dataEvent[0x201];
        //int counter2=dataEvent[0x301];
        if (debug != 0) {
            System.err.println("Trigger "+triggerCount+", Debug word (in binary): "
            +Integer.toBinaryString(debug));
        }
        //lastCount=counter1;
        int [] energy = new int[NUM_STRIPS];
        int [] time = new int[NUM_STRIPS];
        int [] energyGA = new int[NUM_STRIPS];
        int [] timeGA = new int[NUM_STRIPS];
         eventCount++;
        if (eventCount % 100 == 0) {
            eventCount = 0;
            for (int i=0; i < 7; i++){
                if (dataEvent[i]>0){
                    System.out.print("P "+i+": "+dataEvent[i]+" ");
                } else {
                    System.out.print("          ");
                }
            }
            System.err.println();
        }
        for (int strip=0; strip<NUM_STRIPS; strip++) {
            energy[strip]=dataEvent[idEnergy[strip]];
            time[strip]=dataEvent[idTime[strip]];
            energyGA[strip]=(int)Math.round(dataEvent[idEnergy[strip]]*2200.0/Am241Channels[strip]);
            timeGA[strip]=(int)Math.round(dataEvent[idTime[strip]]*2200.0/timeChannels[strip]);
            hEnergy[strip].inc(energyGA[strip]);
            hTime[strip].inc(timeGA[strip]);
            if (energy[strip] > 0) {
                hHits.inc(strip);     
            }
            if (time[strip] > 0) {
                hTimeHits.inc(strip);
            }
            hEvsStrip.inc(energyGA[strip]>>TWO_D_FACTOR,strip);
            hTvsStrip.inc(timeGA[strip]>>TWO_D_FACTOR,strip);
            /*for (int j=0; j<strip;j++) {
            hEvsE[strip][j].inc(energy[strip]>>TWO_D_FACTOR, energy[j]>>TWO_D_FACTOR);
            }*/
        }
        for  (int eStrip = 0; eStrip<NUM_STRIPS; eStrip++){
            for (int tStrip=0; tStrip<NUM_STRIPS; tStrip++){
                boolean tHit = time[tStrip] >= TIME_THRESHOLDS && time[tStrip] <= LAST_GOOD_BIN;
                boolean eHit = energy[eStrip] >= THRESHOLDS && energy[eStrip] <= LAST_GOOD_BIN;
                if (tHit && eHit) hTvsEhits.inc(eStrip,tStrip);
            }
        }
    }
}
