package jam.global; 
/**
 * Interface for user-written sort classes.
 *
 * User writes a sort class which must have the following methods:
 * <dl><dt><code>initialize</code></dt><dd>called when the sort 
 * process is initialized</dd>    
 * <dt><code>sort</code></dt><dd>called for each event</dd>
 * <dt><code>monitor</code></dt><dd>called each time the monitors are 
 * updated</dd></dl>
 *
 * @author Ken Swartz
 * @version 0.9
 * @since JDK 1.1
 */

 public interface Sorter {
  
    /** 
     * Initializes all variables and process states to be ready to 
     * begin sorting.  Should be called prior to 
     * any calls to <code>sort</code>.
     *
     * @exception Exception any exceptions caught by calls in this 
     * method should be thrown up to the controlling thread
     */
    void initialize() throws Exception;
    
    /**
     * Called to process each event.  The sort method looks at gates 
     * and increments histograms.
     * 
     * @param dataWords event passed as an <code>int</code> array for 
     * speed
     * @exception Exception any exceptions caught by calls in this 
     * method should be thrown up to the controlling thread
     */
    void sort(int [] dataWords) throws Exception;
    
    /**
     * Updates any monitors.
     * 
     * @param	name	name of monitor value to calculate
     * @return 	current value for monitor window to display
     * @see jam.data.Monitor
     */	
    double monitor(String name);
}