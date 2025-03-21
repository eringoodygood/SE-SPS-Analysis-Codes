/*
 */
package sort.unc;
import jam.data.*;
import jam.sort.SortRoutine;

/**
 * Sort routine for RF's July 2002 10B run.
 * Singles Spectra: 2 Si monitor detectors, Faraday cup
 * Right Energy and Right delta E
 * Left Energy and Left delta E
 * Two-D Spectra: Right E vs Right delta E
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * @Author C Fox
 */
public class B10 extends SortRoutine {

    // 1 - D Histograms
    //histograms
    Histogram hRE,hLE;
    Histogram hBCIscale;
    
    static final int ADC_BASE = 0x20000000;
    static final int SCALER_ADDRESS = 0xf0e00000;
    
    final int ADC_CHANNELS=4096;//num of channels per ADC


    //scalers
    Scaler sFaraday;
    Scaler sRate;
    Scaler sClock;
    Scaler sDeadtme;

    //rate monitors
    Monitor mFaraday;
    Monitor mClock;
    
    int idRE, idLE, idBCIscale;// idT; //indices in dataEvent array
    static final int THRESHOLDS=0;

    public void initialize() throws Exception {
	vmeMap.setScalerInterval(3);
        //(slot, base address, channel, threshold channel)
        idRE=vmeMap.eventParameter(2, ADC_BASE, 1, THRESHOLDS);
        idLE=vmeMap.eventParameter(2, ADC_BASE, 2, THRESHOLDS);
        idBCIscale=vmeMap.eventParameter(2,ADC_BASE,14,THRESHOLDS);

	// 
        hRE=new Histogram("EnergyR",          HIST_1D, ADC_CHANNELS, "Right Monitor Energy");
        hLE=new Histogram("EnergyL",          HIST_1D, ADC_CHANNELS, "Left Monitor Energy");
		hBCIscale=new Histogram("BCI Scale",HIST_1D, ADC_CHANNELS, "Scale on BCI");


        //scalers
        // numbers are meaningless, but necessary!

        sFaraday      =new Scaler("Faraday", 	       0);
        sClock	      =new Scaler("Clock",             1);
        sRate         =new Scaler("Events",            2);
        sDeadtme      =new Scaler("Accepted",          3);

        //monitors
        mFaraday	=new Monitor("Faraday ",       sFaraday);
        mClock  	=new Monitor("Clock",          sClock);
    }

    /**
     * Sort routine
     */
    public void sort(int [] dataEvent) throws Exception{

        int eRE  =dataEvent[idRE];
        int eLE = dataEvent[idLE];
        // singles spectra
        hRE.inc(eRE);
        hLE.inc(eLE);
        hBCIscale.inc(dataEvent[idBCIscale]);

     }
}
