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

/** 
 * Sort routine for two surface barrier detectors arranged in an E-DeltaE
 * telescope.
 *
 * @author Dale Visser
 * @since 16 November 2001
 */
public class PIDtelescope extends SortRoutine {
    //VME properties
    static final int [] ADC_BASE = {0x20000000,0x20010000,0x20020000};
    static final int SCALER_ADDRESS = 0xf0e00000;
    static final int THRESHOLDS = 100;//ADC lower threshold in channels
    static final int LAST_ADC_BIN = 3840;
    
    //histogramming constants
    final int ADC_CHANNELS = 4096;//num of channels per ADC
    final int COMPRESSED_CHANNELS = 512;//number of channels in compressed histogram
    final int TWO_D_CHANNELS = 512; //number of channels per dimension in 2-d histograms
    //bits to shift for compression
    final int COMPRESS_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/COMPRESSED_CHANNELS)/Math.log(2.0)));
    final int TWO_D_FACTOR=Math.round((float)(Math.log(ADC_CHANNELS/TWO_D_CHANNELS)/Math.log(2.0)));
    
    Histogram hDelE, hE, hDelEvsE;
    Histogram hDelEcal, hEcal, hDelEvsEcal;
    Scaler sBic, sClck, sEvntRaw, sEvntAccpt;//scalers
    Monitor mBeam, mClck, mEvntRaw, mEvntAccept;//monitors
    
    //id numbers for the signals;
    int idE, idDelE;
    
    public void initialize() throws Exception {
        vmeMap.setScalerInterval(3);
        //Focal Plane Detector
        idDelE=vmeMap.eventParameter(0, ADC_BASE[0], 11, THRESHOLDS);
        idE=vmeMap.eventParameter(1, ADC_BASE[0], 12, THRESHOLDS);
        
        hDelE = new Histogram("Delta-E", HIST_1D_INT, ADC_CHANNELS, "Front Delta-E");
        hE = new Histogram("E", HIST_1D_INT, ADC_CHANNELS, "Back E");
        hDelEvsE = new Histogram("DelEvsE", HIST_2D_INT, TWO_D_CHANNELS,
        "Delta-E vs. E", "E", "Delta-E");
        hDelEcal = new Histogram("Delta-E-cal", HIST_1D_INT, 550, 
        "Front Delta-E, calibrated", "1 ch = 100 keV", "Counts");
        hEcal = new Histogram("E-cal", HIST_1D_INT, 400, 
        "Back E, calibrated", "1 ch = 100 keV", "Counts");
        hDelEvsEcal = new Histogram("DelEvsE-cal", HIST_2D_INT, 400,550,
        "Delta-E vs. E, calibrated (1 ch = 100 keV, both axes)", "E", "Delta-E");
        
        //scalers
        sBic      =new Scaler("BIC",0);
        sClck      =new Scaler("Clock",1);
        sEvntRaw    =new Scaler("Event Raw", 2);
        sEvntAccpt  =new Scaler("Event Accept",3);
        /* obsolete 
        vmeMap.scalerParameter(2048+sBic.getNumber(), SCALER_ADDRESS, sBic.getNumber(), sBic);
        vmeMap.scalerParameter(2048+sClck.getNumber(), SCALER_ADDRESS, sClck.getNumber(), sClck);
        vmeMap.scalerParameter(2048+sEvntRaw.getNumber(), SCALER_ADDRESS, sEvntRaw.getNumber(), sEvntRaw);
        vmeMap.scalerParameter(2048+sEvntAccpt.getNumber(), SCALER_ADDRESS, sEvntAccpt.getNumber(), sEvntAccpt);
        */
    }
    
    public void sort(int [] dataEvent) throws Exception {
        int DelE = dataEvent[idDelE];
        int E = dataEvent[idE];
        double DelEcal = 6695.5+15.162*(DelE-518.7103);//in keV(0-55k)
        double Ecal = 6615+10.69*(E-658.0936);//in keV(0-40K)
        int chDelEcal = (int)Math.round(DelEcal/100);//100 keV/ch
        int chEcal = (int)Math.round(Ecal/100);//100 keV/ch
        hDelE.inc(DelE);
        hE.inc(E);
        hDelEvsE.inc(E>>TWO_D_FACTOR, DelE>>TWO_D_FACTOR);
        hDelEcal.inc(chDelEcal);
        hEcal.inc(chEcal);
        hDelEvsEcal.inc(chEcal,chDelEcal);
    }
    
    /** Called so the dead time can be calculated.
     * @param name name of monitor to calculate
     * @return floating point value of monitor
     */
    public double monitor(String name){
        return 0.0;
    }
}
