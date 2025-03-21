package dwvisser.math;

public interface DiffEquations{

    /**
     * An interface expected by <code>RungeKutta4</code> for evaluating
     * a set of derivatives dy[i]/dx at the values y[i] and x.
     *
     * @param at the x-value
     * @param values the y-values
     * @return the derivatives dy[i]/dx
     */
    public double [] dydx(double at, double [] values);
}
