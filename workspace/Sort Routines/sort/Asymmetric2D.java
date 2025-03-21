/*
 */
package sort;
import jam.data.*;
//import jam.sort.Sortable;
import jam.sort.SortRoutine;
/**
 * Sort routine for CI's May 2000 MT run.
 * Singles Spectra: GeLi, NaI,TDC
 * Gated Spectra: Ge1, Ge2, Ge3, Ge4
 * Two-D Spectra: Ge vs. NaI
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * @Author Drew Dummer
 * @Author Carrie Rowland
 * @version 0.2 last modified 07 May 00
 * @version 0.3 last modified 09 May 00 to include a 'double' gate
 * @version 0.4 last modified 08 Jun 00 more gates
 */
public class Asymmetric2D extends SortRoutine {

    private Histogram hOdd, hEven;
    private Gate gOdd, gEven;


    public void initialize() throws Exception {
        setEventSize(2);
        hOdd=new Histogram("Odd", HIST_2D_INT, 16, 256, "256 ch vs. 16 ch");
        gOdd=new Gate("Odd", hOdd);
        hEven=new Histogram("Even", HIST_2D_INT, 256, 256, "256 ch vs. 256 ch");
        gEven=new Gate("Even", hEven);
    }

    /**
     * Sort routine
     */
    public void sort(int [] dataEvent) throws Exception{

    }
}
