/* 
*/
package sort.camac;				
import jam.data.*;
import jam.sort.SortRoutine;
/*
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified June 99 by Rachel Lewis
 */
public class SingleChannel extends SortRoutine {
	Histogram hist;
    
    public void initialize() throws Exception {    
		setEventSize(1);
    	hist=new Histogram("ADC", Histogram.ONE_DIM_INT, 4096, "Test ADC"); 
    }
    
    public void sort(int [] dataEvent) throws Exception {    
		hist.inc(dataEvent[0]);
    }
    
    /**
     * monitor method
     * calculate the live time
     */
    /*public double monitor(String name){
	int rateEvntAccpt=sEvntAccpt.getValue()-lastEvntAccpt;
    	lastEvntAccpt=sEvntAccpt.getValue();
	
	if (name.equals("Live Time")){	
	    if (mEvntRt.getValue()>0){
		return 100.0*rateEvntAccpt/mEvntRt.getValue();
	    } else {
		return 0.0;
	    }				
	} else {
	    return 50.0;
	}	    	    
    }*/
}
