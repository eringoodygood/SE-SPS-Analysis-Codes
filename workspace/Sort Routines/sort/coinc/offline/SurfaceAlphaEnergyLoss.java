/*
 * SurfaceAlphaEnergyLoss.java
 *
 * Created on April 4, 2001, 11:18 AM
 */

package sort.coinc.offline;

/**
 * Class for calculating energy losses through surface layer of LEDA detector.
 * @author  dwvisser
 * @version
 */
public class SurfaceAlphaEnergyLoss extends Object {

    double Sn;//nuclear stopping term
    double depth;//depth of Al dead layer in um
    double ZPOW=2.0/3.0;//power to take z's to in nuclear loss formula
    double UNIT_FAC=0.006023;//factor to multiply by to get MeV/um
    //double [] T = {5.6832, 6.2885, 6.7773, 8.7850}; //initial energy of alpha in MeV
double [] A = {2.5, 0.625, 45.7, 0.1, 4.359};//coefficients from Ziegler v.4 p.67 He stopping in Al
    //double [] S = new double[T.length]; //total stopping in 2 um Al layer
    //double [] eDep = new double[T.length]; //deposited energy in Si to be fit to
    static final double MEV_PER_AMU = 931.5016;
    double mAl = 26.981538;//Mass of 27Al in amu
    int zAl = 13; int zHe=2; //nuclear charges
    double mHe = 2.603250;//Mass of 4He in amu
    double nuclFac = 32.53 * mAl / (zAl*zHe*(mAl+mHe)*Math.sqrt(Math.pow(zAl,ZPOW)+Math.pow(zHe,ZPOW)));
    /** Creates new SurfaceAlphaEnergyLoss
     *
     */
    public SurfaceAlphaEnergyLoss(double thickness) {
        depth=thickness;
        System.out.println(getClass().getName()+": created using depth of "+depth+" um.");
    }

    /**
     * Calculates stopping Power in Al of alpha with given energy.
     *
     * @param Ti initial kinetic energy in MeV of alpha
     * @returns stopping power in MeV/um
     */
    public double getStoppingPower(double Ti){
        double Slo = A[0]*Math.pow(Ti*1000.0,A[1]);//low energy term
        double Shi = A[2]/Ti*Math.log(1.0+A[3]/Ti+A[4]*Ti);//high energy term
        double S = UNIT_FAC/(1.0/Slo + 1.0/Shi);//electronic stopping
        double eps=nuclFac*Ti*1000.0; //Reduced ion energy as per p.66 Ziegler
        if (eps < 0.01) {
            Sn = UNIT_FAC* 1.593 * Math.sqrt(eps);
        } else if (eps > 10.0) {
            Sn = UNIT_FAC * Math.log(0.47*eps) / (2.0 * eps);
        } else {//between 0.01 and 10.0
            Sn = UNIT_FAC * 1.7 * Math.sqrt(eps)*(Math.log(eps+1.0)/(1.0+6.8*eps+Math.sqrt(eps)));
        }
        S += Sn;
        return S;
    }
    
    /**
     * Returns deposited energy for initial kinetic energy of alpha and 
     * given incidence parameter.
     *
     * @param Ti initial kinetic energy in MeV of alpha
     * @param incidence 1/cos(angle from normal)
     * @returns final energy in MeV after passing through (2 um) surface layer
     */
    public double getFinalEnergy(double Ti, double incidence){
        double temp = Ti-getStoppingPower(Ti)*depth*incidence;
        //1st order correct for changing value of stopping power:
        double Savg = (getStoppingPower(Ti)+getStoppingPower(temp))/2.0;
        return Ti-Savg*depth*incidence;
    }

    /**
     * Returns deposited energy for initial kinetic energy of alpha and 
     * given incidence parameter.
     *
     * @param Ti initial kinetic energy in MeV of alpha
     * @param incidence 1/cos(angle from normal)
     * @returns final energy in MeV after passing through (2 um) surface layer
     */
    public double getInitialEnergy(double Tf, double incidence){
        double temp = Tf+getStoppingPower(Tf)*depth*incidence;
        //1st order correct for changing value of stopping power:
        double Savg = (getStoppingPower(Tf)+getStoppingPower(temp))/2.0;
        return Tf+Savg*depth*incidence;
    }
    
    public static void main(String [] args){
        SurfaceAlphaEnergyLoss sael = new SurfaceAlphaEnergyLoss(0.2);
        System.out.println("T -> \tTi -> \tTf");
        for (double T=1.0; T<10.5; T += 1.0){
            double Ti = sael.getInitialEnergy(T,1.1);
            double Tf = sael.getFinalEnergy(Ti,1.1);
            System.out.println(T+"\t"+Ti+"\t"+Tf);
        }
    }
}