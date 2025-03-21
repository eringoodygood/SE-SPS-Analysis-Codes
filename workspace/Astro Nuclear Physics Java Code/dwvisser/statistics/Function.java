package dwvisser.statistics;

/**
 * Generic interface for any real function of a real variable.
 */
public interface Function{

	/**
	 * Evaluate the function.
	 * 
	 * @param where to evaluate at
	 * @return value of the function at x
	 */
    public double valueAt(double x);
    
}
