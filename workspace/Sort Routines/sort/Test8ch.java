/* 
*/
package sort;				
import jam.data.*;
import jam.sort.SortRoutine;
/*
 * Sort file for Testing 
 * sorts 8 channels of data 
 * Peter Parker Astro Physics Group at Yale University
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * @Author Ken Swartz 
 * @version 
 */
public class Test8ch extends SortRoutine {


    // ungated spectra 
    Histogram hCh0;    
    Histogram hCh1;
    Histogram hCh2;    
    Histogram hCh3;
    Histogram hCh4;
    Histogram hCh5;		//front Wire Pulse Height
    Histogram hCh6;		//Rear Wire Pulse Height
    Histogram hCh7;	

    Scaler sNum0;
    Scaler sNum1; 
    Scaler sNum2;                
    Scaler sNum3;    
    
//    Monitor mTest;
    Monitor mMon1;
    Monitor mMon2;
    
    public void initialize() throws Exception {
    
	System.out.println("Test8ch 2");
    
	//EVENT_SIZE=8;	    	
	BUFFER_SIZE=8192;

	hCh0    =new Histogram("Chan 0", HIST_1D, 2048, " Channel 0");   
	hCh1    =new Histogram("Chan 1", HIST_1D, 2048, " Channel 1");   
	hCh2    =new Histogram("Chan 2", HIST_1D, 2048, " Channel 2");   
	hCh3    =new Histogram("Chan 3", HIST_1D, 2048, " Channel 3");   
	hCh4    =new Histogram("Chan 4", HIST_1D, 2048, " Channel 4");   
	hCh5    =new Histogram("Chan 5", HIST_1D, 2048, " Channel 5");   
	hCh6    =new Histogram("Chan 6", HIST_1D, 2048, " Channel 6");   							
	hCh7    =new Histogram("Chan 7", HIST_1D, 2048, " Channel 7");   
			
	//scaler
	sNum0    =new Scaler("Scaler 0 ", 0);
	sNum1    =new Scaler("Scaler 1 ", 1);
	sNum2    =new Scaler("Scaler 2 ", 2);
	sNum3    =new Scaler("Scaler 3 ", 3);		

    }
    public void sort(int [] dataEvent) throws Exception {    
    
	int eCh0   =dataEvent[0];
	int eCh1   =dataEvent[1];
	int eCh2   =dataEvent[2];
	int eCh3   =dataEvent[3];
	int eCh4   =dataEvent[4];
	int eCh5   =dataEvent[5];
	int eCh6   =dataEvent[6];
	int eCh7   =dataEvent[7];


	// singles spectra    
	hCh0.inc(eCh0);
	hCh1.inc(eCh1);
	hCh2.inc(eCh2);
	hCh3.inc(eCh3);
	hCh4.inc(eCh4);
	hCh5.inc(eCh5);					
	hCh6.inc(eCh6);						
	hCh7.inc(eCh7);
		
    }
}
