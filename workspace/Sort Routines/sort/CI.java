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
public class CI extends SortRoutine {

    //histograms
    Histogram hGe;
    Histogram hNaI;
    Histogram hT;
    Histogram h2d;


    //gates

    //scalers
    Scaler sBeam;
    Scaler sGe;
    Scaler sNaI;
    Scaler sClck;

    //rate monitors
    Monitor mBeam;
    Monitor mClck;
    
    int idGe,idNaI, idT; //indices in dataEvent array

    public void initialize() throws Exception {
        //C,N,A,F initialization
        cnafCommands.init(1,28,8,26);      //crate dataway Z
        cnafCommands.init(1,28,9,26);      //crate dataway C
        cnafCommands.init(1,30,9,26);      //crate I
        cnafCommands.init(1,3,12,11);      //adc 811 clear
        cnafCommands.init(1,9,12,11);      //adc 811 clear
        cnafCommands.init(1,20,0,10);      //trigger module clear
        //C,N,A,F executed every command
        //eventRead() returns id number to be used in sort, increments number of params per event
        idGe=cnafCommands.eventRead(1,3,0,0);      //AD 811 slot 3, input 0
        idNaI=cnafCommands.eventRead(1,3,1,0);      //AD 811 slot 3, input 1
        idT=cnafCommands.eventRead(1,3,2,0);      //AD 811 slot 3, input 2
        cnafCommands.eventCommand(1,3,12,11);        //clear adc
        cnafCommands.eventCommand(1,9,12,11);        //clear adc
        cnafCommands.eventCommand(1,20,0,10);        //clear trigger module
        //C,N,A,F executed on scaler reads
        cnafCommands.scaler(1,16,0,0);        //read scaler BIC (Joerger S12, slot 16, input 0)
        cnafCommands.scaler(1,16,1,0);        //read scaler Ge
        cnafCommands.scaler(1,16,2,0);        //read scaler NaI
        cnafCommands.scaler(1,16,3,0);        //read scaler Clock

        hGe=new Histogram("Ge",          HIST_1D, 8192, " Ge");
        hNaI=new Histogram("NaI",        HIST_1D, 8192, " NaI");
        hT=new Histogram("Time",          HIST_1D, 4096, " Time");

        //scalers
        // numbers are meaningless, but necessary!

        sBeam	    =new Scaler("Beam", 	   0);
        sGe         =new Scaler("Ge",              1);
        sNaI	    =new Scaler("NaI",             2);
        sClck       =new Scaler("Clock",           3);

        //monitors
        mBeam	=new Monitor("Beam ",           sBeam);
        mClck 	=new Monitor("Clock",          sClck);
    }

    /**
     * Sort routine
     */
    public void sort(int [] dataEvent) throws Exception{

        int eGe  =dataEvent[idGe];
        int eNaI  =dataEvent[idNaI];
        int eT  =dataEvent[idT];

        int ecGe=eGe>>4;	//compress by 16
        int ecNaI=eNaI>>4;

        // singles spectra
        hGe.inc(eGe);
        hNaI.inc(eNaI);
        hT.inc(eT);

        //singles 2d spectra
        //	h2d.inc(ecGe,ecNaI);

    }
}
