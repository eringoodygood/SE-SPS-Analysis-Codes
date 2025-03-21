/*
 */
package sort.offline.LEDA;
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
public class Multiplicity extends SortRoutine {
    static final int [] ADC_BASE = new int[2];
    static final int TDC_BASE = 0x30000000;
    static final int TDC_2_BASE = 0x30010000;
    static final int THRESHOLDS = 0;
    static final int SCALER_ADDRESS = 0xf0e00000;

    static final int NUM_DETECTORS=1;
    static final int STRIPS_PER_DETECTOR=16;

    Histogram [][] hStrips = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    Histogram [][] hEnergies = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] idStrips = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] eStrips = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];

    Histogram hSingleHits, hDoubleHits, h1dDoubleHits;

    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed position histogram
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

    final double [] ZERO_OFFSET = {57.5, 64.3, 61.5, 59.4, 47.2, 53.8, 59.2, 34.5, 65.7, 47.0, 58.7, 89.8, 54.2,
    63.2, 69.9, 66.6};
    final double [] AM241 = {2113.0, 2261.7, 2299.4, 2179.0, 2130.1, 2187.6, 2121.8, 2213.2,
    2200.0, 2069.7, 2301.8, 2145.7, 2156.5, 2428.3, 2114.1, 2204.1};
    final double AM241ENERGY = 5412.0;//keV
    double [] offset, slope, energy;

    double ENERGY_THRESHOLD=100.0;//keV


    public void initialize() throws Exception {
        ADC_BASE[0] = 0x20000000;
        ADC_BASE[1] = 0x20010000;
        vmeMap.setScalerInterval(3);
        //id numbers for the signals;
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                int temp=i*STRIPS_PER_DETECTOR+j;
                idStrips[i][j]=vmeMap.eventParameter(temp+1, whichADC(i), whichChannel(i,j), THRESHOLDS);
                //starting at channel 16 in ADC and parameter 1 in event stream
                //FIXME not general for other cases
                hStrips[i][j]=new Histogram("D_"+i+"_S_"+j, HIST_1D_INT, ADC_CHANNELS, "Detector "+i+", Strip "+j);
                hEnergies[i][j]=new Histogram("E_D"+i+"S"+j, HIST_1D_INT, ADC_CHANNELS/4, "Energy of detector "+i+", strip "+j);
            }
        }
        hSingleHits = new Histogram("Singles", HIST_1D_INT, STRIPS_PER_DETECTOR, "Single hits over threshold");
        hDoubleHits = new Histogram("Doubles", HIST_2D_INT, STRIPS_PER_DETECTOR, "Double hits over threshold");
        h1dDoubleHits = new Histogram("1d Doubles", HIST_1D_INT, STRIPS_PER_DETECTOR, "Double hits over threshold");
        offset=new double[AM241.length];
        slope = new double[AM241.length];
        for (int i=0; i<AM241.length; i++){
            slope[i] = AM241ENERGY/(AM241[i]-ZERO_OFFSET[i]);
            offset[i] = -slope[i]*ZERO_OFFSET[i];
        }
        energy = new double[AM241.length];
    }

    /**
     * Returns which adc unit given which detector.
     */
    private int whichADC(int detector){
        if (detector == 0) {
            return ADC_BASE[0];
        } else {//2nd or 3rd detector
            return ADC_BASE[1];
        }
    }

    /**
     * Returns which channel in the adc given which detector and strip.
     */
    private int whichChannel(int detector, int strip){
        if (detector == 0 || detector==2) {//1st and 3rd detector
            return strip+16;
        } else {//2nd detector
            return strip;
        }
    }

    public void sort(int [] dataEvent) throws Exception {
        int multiplicity = 0;
        int [] strip = new int[2];
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                if (i==0){
                    eStrips[i][j]=dataEvent[idStrips[i][j]];
                    energy[j]=0.0;
                    energy[j]=offset[j]+slope[j]*eStrips[i][j];
                    //System.err.println("Strip "+j+", Energy = "+energy[j]+" keV");
                    if (energy[j]>ENERGY_THRESHOLD) {
                        if (multiplicity<2) {
                            strip[multiplicity] = j;
                        }
                        multiplicity++;
                    }
                }
            }
        }
        //Looking only at detector 0:
        switch (multiplicity) {
            case 1: hStrips[0][strip[0]].inc(eStrips[0][strip[0]]);
            hEnergies[0][strip[0]].inc((int)Math.round(energy[strip[0]]/10.0));
            hSingleHits.inc(strip[0]);
            break;
            case 2 : h1dDoubleHits.inc(strip[0]); h1dDoubleHits.inc(strip[1]);
            if (strip[0] < strip [1]) {
                hDoubleHits.inc(strip[0],strip[1]);
            } else {
                hDoubleHits.inc(strip[1],strip[0]);
            }
            break;
            default: break;
        }



    }
}
