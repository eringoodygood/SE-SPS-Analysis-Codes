/*
 * WeightingFunction.java
 *
 * Created on March 9, 2001, 2:50 PM
 */

package dwvisser.monte;

/**
 * Used by <code>DataSet</code> to define a weighting function
 * in obtaining summary statistics.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale Visser</a>
 */
public interface WeightingFunction {
    
    public double weight(double value);

}
