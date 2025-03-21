/**
 * Sort routine for uconn measurement of
 * 7Li Elastic scattering
 * @Author Jim McDonald UConn
 * Based on code by Ken Swartz Yale University
 * 11 March 2000
 */
package sort.uconn;
import jam.data.*;
import jam.sort.SortRoutine;

public class LiOrtec extends SortRoutine {

    int idMH;      // Monitor for H
    int idAuL;      // Gold detector, left
    int idAuR;      // Gold detector, right
    int idLE;      // PSD Energy signal, left
    int  idLP;      // PSD Position signal, left
    int  idRE;      // PSD Energy signal, right
    int idRP;      // PSD Energy signal, right

    double Lx;      // PSD position, left
    double Rx;      // PSD position, right

    int idEvent;    // Event Scaler

    // Define variable names
    Histogram hMH;    // H Monitor
    Histogram hAuL;    // Left Gold
    Histogram hAuR;    // Right Gold
    Histogram hLE;    // L PSD Energy
    Histogram hLP;    // L PSD Position
    Histogram hRE;    // R PSD Energy
    Histogram hRP;    // R PSD Position

    Histogram hLPE;    // Left Energy-position 2D
    Histogram hRPE;    // Right Energy-Position 2D

    Scaler sPulser;    // raw pulser events       
    
    public LiOrtec(){
    }
    // Define histograms, gates and scalers properties
    public void initialize()throws Exception{
  
  cnafCommands.init(1,28,8,26);      //crate dataway Z   
  cnafCommands.init(1,28,9,26);      //crate dataway C
  cnafCommands.init(1,30,9,26);      //crate I
  cnafCommands.init(1,9,12,11);      //adc1 clear
  cnafCommands.init(1,10,12,11);      //adc2 clear
  cnafCommands.init(1,20,0,10);      //trigger module clear
  cnafCommands.init(1,16,0,9);      //scaler clear
  
  idMH=cnafCommands.eventRead(1,9,0,0);      //read MH
  idAuL=cnafCommands.eventRead(1,9,1,0);      //read AuL
  idAuR=cnafCommands.eventRead(1,9,2,0);      //read AuR
  idLE=cnafCommands.eventRead(1,9,3,0);      //read LE
  idLP=cnafCommands.eventRead(1,9,4,0);      //read LP
  idRE=cnafCommands.eventRead(1,9,5,0);      //read RE
  idRP=cnafCommands.eventRead(1,9,6,0);      //read RP
  
  cnafCommands.eventCommand(1,9,12,11);      //clear adc
  cnafCommands.eventCommand(1,20,0,10);      //clear trigger module
     
  cnafCommands.scaler(1,16,0,0);        //read deadtime scaler
  cnafCommands.clear(1,16,0,9);        //clear deadtime scaler
  

  //Histograms        name  number  type  size    title  
  
  hMH =new Histogram("MH       ", HIST_1D, 2048, " Hydrogen Monitor "); 
  hAuL =new Histogram("AuL       ", HIST_1D, 2048, " Left Au Backscatter ");   
  hAuR =new Histogram("AuR      ", HIST_1D, 2048, " Right Au Backscatter ");
  hLE =new Histogram("LE       ", HIST_1D, 2048, " Left PSD Energy"); 
  hLP =new Histogram("LP       ", HIST_1D, 2048, " Left PSD Position ");   
  hRE =new Histogram("RE       ", HIST_1D, 2048, " Right PSD Energy ");
  hRP =new Histogram("RP       ", HIST_1D, 2048, " Right PSD Position "); 
  
  hLPE=new Histogram("Left PSD 2D", HIST_2D, 256, " Left Energy vs Position ");
  hRPE=new Histogram("Right PSD 2D", HIST_2D, 256, " Right PSD Energy vs. Position ");
  sPulser      =new Scaler("PulserCts/10", 0 ); 
    }

    public void sort(int [] dataEvent)throws Exception{    

  //unpack event into convenient names

  int MH    =dataEvent[idMH];
  int AuL   =dataEvent[idAuL];
  int AuR   =dataEvent[idAuR];
  int LE    =dataEvent[idLE];
  int LP    =dataEvent[idLP];
  int RE    =dataEvent[idRE];
  int RP    =dataEvent[idRP];
  double Lx;
  double Rx;
  int iLx;
  int iRx;
    
  Lx = ((double)LP/(double)LE)*400.;
  Rx = ((double)RP/(double)RE)*500.;
  
  iLx = (int)Lx;
  iRx = (int)Rx;

  int LEsmall   =LE>>3;    //performs a bit-shift to reduce the 
  int Lxsmall   =iLx>>3;    //size of the 2D Histograms
  int REsmall   =RE>>3;      
  int Rxsmall   =iRx>>3;
  
      hMH.inc(MH);
      hAuL.inc(AuL);
      hAuR.inc(AuR);
      hLE.inc(LE);
      hLP.inc(iLx);
      hRE.inc(RE);
      hRP.inc(iRx);
      
      hLPE.inc(Lxsmall,LEsmall);      //graph will be hNAME.inc(x,y)
      hRPE.inc(Rxsmall,REsmall);      
  }
    
    /*
     * Write this method to update a general monitor
     */
    
    public double monitor(String name){
  //method to calculate monitors
  return 0.0;
    }    
    
}
