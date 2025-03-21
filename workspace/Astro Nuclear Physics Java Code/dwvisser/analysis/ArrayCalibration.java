/*
 * ArrayCalibration.java
 *
 * Created on May 14, 2001, 2:10 PM
 */
package dwvisser.analysis;
import java.io.*;
import dwvisser.NumericTextFileReader;

/**
 * This class is a database class for containing all the calibration info
 * for the YLSA array.  YLSA is an array of 5 16-strip detectors.  This class
 * will eventually be a repository for linear time and energy calibration data
 * and will be able to service a sort routine with appropriate gain
 * coefficients which compress gains of all channels to match the lowest gain
 * channel.
 *
 * @author  <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class ArrayCalibration extends Object implements Serializable {

    /** Number of detectors in YLSA array.
     */
    static public int NUM_DETECTORS = 5;
    /** Number of strips per detector in YLSA array.
     */
    static public int NUM_STRIPS = 16;
    
    private static double [] incidence = {1.2789, 1.2428, 1.2099, 1.1784, 1.1500, 1.1241,
            1.1016, 1.0818, 1.0643, 1.0508, 1.0408, 1.0358, 1.0336, 1.0265,
        1.0214, 1.0189};

    private static double [] distance = {128.1, 125.0, 122.0, 119.2, 116.5,
        114.0, 111.6, 109.4, 107.4, 105.5, 103.9, 102.6, 101.4, 99.8, 98.1, 
        96.6};
    private static double [] phi = {198.0, 126.0, 54.0, -18.0, -90.0};
    
    /**
     * theta of strips in degrees
     */
    private static double [] theta = 
    {165.95, 164.2, 162.3, 160.4, 158.4, 156.2, 154.0, 151.7,
        149.3, 146.85, 144.3, 141.6, 138.9, 136.2, 133.4, 130.6};
    
    //doubles default to 0.0;
    /**
     * Channels per MeV.
     */
    private double [][] energyGains = new double[NUM_DETECTORS][NUM_STRIPS];
    private double [][] energyOffsets = new double[NUM_DETECTORS][NUM_STRIPS];
    private double [][] energyGainFactor = new double[NUM_DETECTORS][NUM_STRIPS];
    private double minEnergyGain;
    
    /**
     * Channels per nanosecond.
     */
    private double [][] timeGains = new double[NUM_DETECTORS][NUM_STRIPS];
    private double [][] timeOffsets = new double[NUM_DETECTORS][NUM_STRIPS];
    private double [][] timeGainFactor = new double[NUM_DETECTORS][NUM_STRIPS];
    private double minTimeGain;
    
    //booleans default to false
    private boolean [][] eCalibrated = new boolean[NUM_DETECTORS][NUM_STRIPS];
    private boolean [][] tCalibrated = new boolean[NUM_DETECTORS][NUM_STRIPS];

    /** Creates new ArrayCalibration */
//    public ArrayCalibration() {
//    }

    /**
     * Format of the file is <br>
     * <pre>det strip offset delOffset</pre><br>
     *
     * @param offsetsText file containing offsets information
     */
    public void getOffsets(File offsetsText){
        NumericTextFileReader numbers = new NumericTextFileReader(offsetsText);
        int numOffsets=numbers.getSize()/4;
        for (int i=0; i<numOffsets; i++){
            int det=(int)numbers.read();
            int strip=(int)numbers.read();
            double offset=numbers.read();
            numbers.read();//ignore error bar of offset
            energyOffsets[det][strip] = offset;
        }
    }

    /** Sets the zero-offset for the particular channel of electronics.
     * @param det which detector
     * @param strip which strip in the detector
     * @param offset channel corresponding to zero energy
     */
    public void setEnergyOffset(int det, int strip, double offset){
        energyOffsets[det][strip]=offset;
    }

    public void setTimeOffset(int det, int strip, double offset){
        timeOffsets[det][strip]=offset;
    }
    
    /** Gets zero offset for channel in the electronics.
     * @param det which detector
     * @param strip which strip
     * @return channel corresponding to zero energy
     */
    public double getEnergyOffset(int det, int strip){
        return energyOffsets[det][strip];
    }

    /** Sets the number of channels per MeV for this channel of electronics.
     * @param det which detector
     * @param strip which strip
     * @param gain channels per MeV
     */
    public void setEnergyGain(int det, int strip, double gain){
        energyGains[det][strip]=gain;
        eCalibrated[det][strip]=true;
    }
    
    public double getEnergyGain(int det, int strip){
        return energyGains[det][strip];
    }

    /** Sets gain for this channel of electronics.
     * @param det which detector
     * @param strip which strip in the detector
     * @param gain channels per nsec
     */
    public void setTimeGain(int det, int strip, double gain){
        timeGains[det][strip]=gain;
        tCalibrated[det][strip]=true;
    }

    /*public boolean getWhetherEnergyCalibrated(int d, int s){
        return eCalibrated[d][s];
    }*/

    /** Processes energy gains to produce a factor <= 1.0 to multiply.
     * The proper way to use this is to subtract the zero offset from the channel,
     * then multiply by the gain factor.
     */
    public void setEnergyGainFactors(){
        double minGain=1000000.0;
        int minD=999, minS=999;
        for (int d=0; d<NUM_DETECTORS; d++){
            for (int s=0; s<NUM_STRIPS; s++) {
                double temp=energyGains[d][s];
                if (eCalibrated[d][s] && temp < minGain) {
                    minGain=temp;
                    minD=d;
                    minS=s;
                }
            }
        }
        if (minGain > 0.0) {//actually found something
            System.out.println("Found Detector "+minD+", Strip "+minS+" had minimum gain.");
            minEnergyGain = energyGains[minD][minS];
            for (int d=0; d<NUM_DETECTORS; d++) {
                for (int s=0; s<NUM_STRIPS; s++) {
                    if (energyGains[d][s] > 0.0){
                        energyGainFactor[d][s] = minEnergyGain/energyGains[d][s];
                    } else {
                        energyGainFactor[d][s] = 0.0;
                    }
                }
            }
        }
    }
    
    /** Processes time gains to produce a factor <= 1.0 to multiply.
     * The proper way to use this is to subtract the zero offset from the channel,
     * then multiply by the gain factor.
     */
    public void setTimeGainFactors(){
        double initGain = 1000000.0;
        double minGain= initGain;
        int minD=999, minS=999;
        for (int d=0; d<NUM_DETECTORS; d++){
            for (int s=0; s<NUM_STRIPS; s++) {
                double temp=timeGains[d][s];
                if (tCalibrated[d][s] && temp < minGain) {
                    minGain=temp;
                    minD=d;
                    minS=s;
                }
            }
        }
        if (minGain > 0.0  && minGain < initGain) {//actually found something
            System.out.println("Found Detector "+minD+", Strip "+minS+
            " had minimum gain.");
            minTimeGain = timeGains[minD][minS];
            for (int d=0; d<NUM_DETECTORS; d++){
                for (int s=0; s<NUM_STRIPS; s++) {
                    if (timeGains[d][s]>0.0) {
                        timeGainFactor[d][s] = minTimeGain/timeGains[d][s];
                    } else timeGainFactor[d][s] = 0.0;
                }
            }
        }
    }
    
    public double getChannelsPerMeV(){
        return minEnergyGain;
    }
    
    public double getChannelsPerNsec(){
        return minTimeGain;
    }
    
    public int getCalibratedEnergyChannel(int det, int strip, int channel){
        return Math.max(0, (int)(energyGainFactor[det][strip]*(channel-
        energyOffsets[det][strip])));
    }
    
    /**
     * Returns energy deposited in MeV.
     */
    public double getEnergyDeposited(int det, int strip, int channel){
        return energyGainFactor[det][strip]*
        (channel-energyOffsets[det][strip])/minEnergyGain;
    }
    
    /**
     * Returns the uncompressed channel number recommended for
     * displaying calibrated time.
     * 
     * @param det detector number
     * @param strip strip number
     * @param channel raw channel number from TDC
     * @return zero if uncalibrated, value matched to max gain channel otherwise	
     */
    public int getCalibratedTimeChannel(int det, int strip, int channel){
		int rval=0;//default return value if uncalibrated
        if (tCalibrated[det][strip]){
        	rval = Math.max(0, (int)(2048.0+timeGainFactor[det][strip]*(channel-
        	timeOffsets[det][strip])));
        } 
        return rval;
    }

    /** Returns a string representation of this object.
     * @return string representation of the gains for writing out to screen or text file
     */
    public String toString() {
        String tab = "\t";
        String rval = minEnergyGain + " channels per MeV\n";
        rval += minTimeGain + " channels per nsec\n";
        rval += "Det\tStrip\teOffset\teGain\teFactor\ttOffset\ttGain\ttFactor\n";
        for (int det=0; det<NUM_DETECTORS; det++) {
            for (int str=0; str<NUM_STRIPS; str++) {
                rval += det+tab+str+tab+energyOffsets[det][str]+tab+
                energyGains[det][str]+tab+energyGainFactor[det][str]+tab+
                timeOffsets[det][str]+tab+timeGains[det][str]+tab+
                timeGainFactor[det][str]+"\n";
            }
        }
        return rval;
    }
    
    /** Returns 1/cos(inc. angle) for a ray from the target to a particular
     * strip.  What it actually returns
     * is the average value taken from a ~1 million event monte carlo.
     * @param strip strip number in detector (0 further back, 15 further forward)
     * @return 1/cos(incidence angle)
     */
    public double getIncidence(int strip){
        return incidence[strip];
    }
    
    public double getDistance(int strip){
        return distance[strip];
    }
    
    /** Returns theta of strip in radians.
     *
     */
    public double getTheta(int strip){
        return Math.toRadians(theta[strip]);
    }
    
    public double getPhi(int det){
        return Math.toRadians(phi[det]);
    }

    /** Just for debugging.
     * @param args command-line arguments
     */
    public static void main (String [] args) {
        ArrayCalibration ac=new ArrayCalibration();
        try {
            ac.getOffsets(new File(
            "/data/may01/analysis/May2001CorrectedOffsets.dat"));
            ObjectOutputStream oos=new ObjectOutputStream(new
            FileOutputStream(new File("/data/may01/analysis/ArrayCalibration.obj")));
            oos.writeObject(ac);
            oos.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

}