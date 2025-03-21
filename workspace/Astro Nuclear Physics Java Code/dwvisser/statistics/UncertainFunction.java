package dwvisser.statistics;
import dwvisser.math.UncertainNumber;

public interface UncertainFunction extends Function{

    /**
     * Given an array of uncertain numbers, return back a function value
     * with error bars.
     */
    public UncertainNumber evaluate(UncertainNumber [] x) 
            throws StatisticsException;
}
