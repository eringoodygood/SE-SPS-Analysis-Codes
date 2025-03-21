/*
 */
package sort.coinc;
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
public class ArrayPulserTests extends SortRoutine {
    static final int [] ADC_BASE = new int[2];
    static final int TDC_BASE = 0x30000000;
    static final int TDC_2_BASE = 0x30010000;
    static final int THRESHOLDS = 0;
    static final int SCALER_ADDRESS = 0xf0e00000;

    static final int NUM_DETECTORS=3;
    static final int STRIPS_PER_DETECTOR=16;
    
    Histogram [][] hStrips = new Histogram[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] idStrips = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
    int [][] eStrips = new int[NUM_DETECTORS][STRIPS_PER_DETECTOR];
 
    final int ADC_CHANNELS=4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed position histogram
    final int TWO_D_CHANNELS=256; //number of channels per dimension in 2-d histograms
    //amount of bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));

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
            }
        }
        //vmeMap.scalerParameter(2048+sBic.getNumber(), SCALER_ADDRESS, sBic.getNumber(), sBic);
    }
    
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
     * Returns which channel in the adc given which detector and strip.
     */
    private int whichChannel(int detector, int strip){
        if (detector == 0 || detector==2) {//Detectors 0 and 2
            return strip+16;
        } else {//Detector 1
            return strip;
        }
    }

    public void sort(int [] dataEvent) throws Exception {
        //unpack data into convenient names
        for (int i=0; i<NUM_DETECTORS; i++) {
            for (int j=0; j<STRIPS_PER_DETECTOR; j++) {
                eStrips[i][j]=dataEvent[idStrips[i][j]];
                hStrips[i][j].inc(eStrips[i][j]);
            }
        }
    }
}
