package dwvisser.math;

public class RungeKutta4{

    private double [] y;
    private double [] dydx;
    private double x,h;
    private DiffEquations derivs;


    public RungeKutta4(DiffEquations de){
      derivs=de;
    }

    /**
     * See numerical recipes Section 16.1
     */
    public void setVariables(double evaluateAt, double [] initialValues,
      double interval) /*throws Exception*/ {
      //if (initialValues.length != dydx.length) {
        //throw new Exception("Dimensions don't match!");
      //}
      y=initialValues;
      x=evaluateAt;
      //dydx=initialDerivs;
      dydx = derivs.dydx(x,y);
      h=interval;
    }

    /**
     * Almost verbatim routine rk4 in Numerical Recipes.
     */
    public double [] step(){
      double [] dym = new double[y.length];
      double [] dyt = new double[y.length];
      double [] yt  = new double[y.length];
      double [] yout = new double[y.length];
      double hh = h * 0.5;
      double h6 = h / 6.0;
      double xh = x + hh;
      for (int i=0;i<y.length;i++) yt[i]=y[i]+hh*dydx[i];
      dyt = derivs.dydx(xh,yt);
      for (int i=0;i<y.length;i++) yt[i]=y[i]+hh*dyt[i];
      dym = derivs.dydx(xh,yt);
      for (int i=0;i<y.length;i++) {
          yt[i] = y[i]+h*dym[i];
          dym[i] += dyt[i];
      }
      dyt = derivs.dydx(x+h,yt);
      for (int i=0;i<y.length;i++) yout[i]=y[i]+h6*(dydx[i]+dyt[i]+2.0*dym[i]);
      return yout;
    }

    public double [] dumbIntegral(double start, double end,
          double [] initValues, int numberOfSteps){
       /*System.out.println("dumbIntegral("+start+", "+end+", "+initValues[0]+", "+numberOfSteps
       		+")");*/
      setVariables(start,initValues,(end-start)/numberOfSteps);
      for (int i=0; i< numberOfSteps; i++){
      	//System.out.println("h: "+h+", x: "+x+", thickness: "+y[0]);
        y=step();
        x += h;
        dydx = derivs.dydx(x,y);
      }
      return y;
    }



}
