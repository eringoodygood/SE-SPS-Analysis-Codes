package dwvisser.nuclear;
import dwvisser.math.*;
import dwvisser.*;
import java.util.*;
import java.io.*;

public class EnergyLoss implements DiffEquations, Serializable{
    
    final static String COEFF_FILE="coeffs.dat";
    final static String ATOM_FILE="atomdata.dat";
    
    /**
     * Conversion Factor from MeV/c^2 to amu
     */
    public final static double MEV_TO_AMU = 0.0010735438521;
    
    public final static double AMU_TO_KEV = 1/(MEV_TO_AMU/1000);
    
    public final static double FINE_STRUCTURE=7.297352533e-3;
    
    public final static double E_TO_4 = Math.pow(4*Math.PI*FINE_STRUCTURE,2);
    /**
     * 10^-24 * Avagodro's number, used for conversion
     */
    public final static double AVAGADRO = 0.60221367;
    
    /**
     * # of elements for which parameters are available
     */
    private final static int NUM_ELEMENTS = 23;
    
    /**
     * Total number of atomic elements
     */
    private static final int ATOMIC_ELEMENTS=92;
    
    /**
     * The A-1 thru A-12 coefficients for the Ziegler Energy loss formulae.
     */
    protected static double [][] coeffs=new double[12][ATOMIC_ELEMENTS+1];
    
    /**
     * Ionization potential for the various elements.
     */
    protected static double [] ionPotential=new double[ATOMIC_ELEMENTS+1];
    
    /**
     * Whether the room temperature phase is gaseous or not.
     */
    protected static boolean [] gas=new boolean[ATOMIC_ELEMENTS+1];
    
    /**
     * The symbol of the element
     */
    protected static String [] symbol=new String[ATOMIC_ELEMENTS+1];
    
    
    /**
     * Names of the elements.
     */
    protected static String [] name=new String[ATOMIC_ELEMENTS+1];
    
    /**
     * "Natural" weights of elements, in amu.
     */
    protected static double [] natWeight = new double[ATOMIC_ELEMENTS+1];
    
    /**
     * "Natural" density of elements, in g/cm^3.
     */
    protected static double [] natDensity = new double[ATOMIC_ELEMENTS+1];
    
    /**
     * Atom density of elements, in atoms/cm^3
     */
    protected static double [] atomDensity = new double[ATOMIC_ELEMENTS+1];
    
    /**
     * Table to look up proton number given elemental symbol.
     */
    protected static Hashtable elementLookUp = new Hashtable(ATOMIC_ELEMENTS);
    
    /**
     * The fractional amount of a chemical element in an absorber.
     */
    protected double [] fractions;
    
    /**
     * The current absorber object to be used in calculations.
     */
    protected Absorber absorber;
    
    /**
     * The atomic numbers of the components of the absorber.
     */
    protected int [] Z;
    
    /**
     * The thickness of the absorber in micrograms/cm^2.
     */
    protected double thickness;
    
    private Nucleus projectile;
    
    public static final int RADIANS=2341;
    public static final int INCIDENCE=2349;
    
    //Bethe correction term coefficients, indexed by (zprojectile+15)/10;
    private static final double [] A0= {5.05,4.41,7.65,11.57,14.59,19.07,
    17.11,15.0,17.8,20.4};
    private static final double [] A1= {2.05,1.88,2.99,4.39,5.46,7.02,6.10,
    5.15,6.18,6.97};
    private static final double [] A2= {.304,.281,.419,.598,.733,.931,.778,
    .626,.766,.874};
    private static final double [] A3= {.020,.018,.025,.035,.042,.053,.043,
    .032,.041,.048};
    private static final double [] A4= {4.7,4.2,5.5,7.5,9.0,11.2,8.6,6.0,
    7.9,9.5};
    //low  (< 150 keV) energy stopping of Hydrogen indexed by Ztarget
    private static final double [] C2 = {1.44,1.397,1.6,2.59,2.815,2.989,
    3.35,3.,2.352,2.199,
    2.869,4.293,4.739,4.7,3.647,3.891,5.714,6.5,5.833,6.252,
    5.884,5.496,5.055,4.489,3.907,3.963,3.535,4.004,4.175,4.75,
    5.697,6.3,6.012,6.656,6.335,7.25,6.429,7.159,7.234,7.603,
    7.791,7.248,7.671,6.887,6.677,5.9,6.354,6.554,7.024,7.227,
    8.48,7.81,8.716,9.289,8.218,8.911,9.071,8.444,8.219,8.,
    7.786,7.58,7.38,7.592,6.996,6.21,5.874,5.706,5.542,5.386,
    5.505,5.657,5.329,5.144,5.851,5.704,5.563,5.034,5.46,4.843,
    5.311,5.982,6.7,6.928,6.979,6.954,7.82,8.448,8.609,8.679,
    8.336,8.204};
    
    private static final double [] ION = {17.1,45.2,47.,63.,75.,79.,84.4,
    104.8,126.4,150.9,
    141.,149.,162.,159.,168.9,179.2,187.2,200.,189.4,195.,
    215.,228.,237.,257.,275.,284.,304.,314.,330.,323.,
    335.4,323.,354.7,343.4,360.5,368.2,349.7,353.3,365.,382.,
    391.3,393.,416.2,428.6,436.4,456.,470.,466.,479.,511.8,
    491.9,491.3,491.8,489.5,484.8,485.5,493.8,512.7,520.2,540.,
    537.,545.9,547.5,567.,577.2,578.,612.2,583.3,629.2,637.,
    655.1,662.9,682.,695.,713.6,726.6,743.7,760.,742.,768.4,
    764.8,761.,762.9,765.1,761.7,764.2,762.3,760.1,767.9,776.4,
    807.,808.};
    
    private static final double [] IONGS = {19.,42.,0.,0.,0.,66.2,86.,99.,
    118.8,135.,
    0.,0.,0.,0.,0.,0.,170.3,180.,0.,0.,
    0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,
    0.,0.,0.,0.,339.3,347.,0.,0.,0.,0.,
    0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,
    0.,0.,452.4,459.,0.,0.,0.,0.,0.,0.,
    0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,
    0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,
    0.,0.,0.,0.,0.,733.1,0.,0.,0.,0.,
    0.,0.};
    
    private static boolean alreadySetUp = false;
    
    /**
     * Create an energy loss calculator associated with the given absorber.
     * 
     * @param a material that energy losses will be calculated in
     */
    public EnergyLoss(Absorber a) {
    	if (!alreadySetUp) {
    		setup();
    	}
        setAbsorber(a);
    }
    
    private static void setup(){
        getEnergyLossParameters(EnergyLoss.class.getResourceAsStream(COEFF_FILE));
        getAtomicData(EnergyLoss.class.getResourceAsStream(ATOM_FILE));
        alreadySetUp=true;
    }
    
    /**
     * Lookup method for obtaining an atomic number from the element's symbol.
     */
    public static int getElement(String symbol) throws NuclearException {
    	if (!alreadySetUp) {
    		setup();
    	}
        Integer zInt = (Integer)elementLookUp.get(symbol.trim());
        if (zInt==null) throw new NuclearException("No element found for"+
        " symbol: "+symbol);
        return zInt.intValue();
    }
    
    public static double getNaturalWeight(int element){
    	if (!alreadySetUp) {
    		setup();
    	}
        return natWeight[element];
    }
    
    /**
     * Returns density of element in its most common form in g/cm^3.
     */
    public static double getDensity(int element){
    	if (!alreadySetUp) {
    		setup();
    	}
        return natDensity[element];
    }
    
    private static void getEnergyLossParameters(InputStream is){
        int Z;
        String element;
        if (is != null) {
            try {
                ColumnarTextReader ctfr = new ColumnarTextReader(is);
                for (int i=0;i<NUM_ELEMENTS;i++){
                    ctfr.nextLine();
                    Z=ctfr.readInt(2);
                    //System.out.println("Coeffs: "+Z);
                    element=ctfr.readString(2);
                    symbol[Z]=element;
                    elementLookUp.put(element,new Integer(Z));
                    //System.out.println(element+" "+Z);
                    for (int j=0;j<12;j++){
                        coeffs[j][Z]=ctfr.readDouble(10);
                    }
                    ionPotential[Z]=ctfr.readDouble(7);
                    gas[Z]=(ctfr.readString(1).equals("g"));
                }
                ctfr.close();
                ctfr=null;
            } catch (FileNotFoundException fnf) {
                System.err.println("Could not find file: "+fnf);
            } catch (IOException ioe) {
                System.err.println(ioe);
            }
        } else {
            System.out.println(EnergyLoss.class.getName()+
            ".getEnergyLossParameters() called with null argument");
        }
    }
    
    private static void getAtomicData(InputStream is){
        int Z;
        try {
            ColumnarTextReader ctfr = new ColumnarTextReader(is);
            ctfr.nextLine();
            for (int i=0;i<ATOMIC_ELEMENTS;i++){
                ctfr.nextLine();
                Z=ctfr.readInt(2);
                //System.out.println("getAtomData: "+Z);
                ctfr.skipChars(4);
                name[Z]=ctfr.readString(14);
                ctfr.skipChars(12);
                natWeight[Z]=ctfr.readDouble(8);
                natDensity[Z]=ctfr.readDouble(8);
                atomDensity[Z]=ctfr.readDouble(9);
            }
            ctfr.close();
            ctfr=null;
        } catch (FileNotFoundException fnf) {
            System.err.println("Could not find file: "+fnf);
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }
    
    private double iave(int Z){
    	if (!alreadySetUp) {
    		setup();
    	}
        double rval = ION[Z];//solid case
        if (absorber instanceof Gas) {
            if (IONGS[Z] > 0.0) {//else keep solid value
                rval = IONGS[Z];
            }
        }
        return rval;
    }
    
    private double [] StopP(Nucleus projectile, double energy) throws NuclearException {
    	if (!alreadySetUp) {
    		setup();
    	}
        double [] rval = ElStopHI(projectile,energy);
        double ea = energy*1000/projectile.A;
        if (ea <= 2000) {
            double [] nval =  NuSPwr(projectile,energy);
            for (int i=0; i<Z.length;i++) {
            	rval[i]+=nval[i];
            }
        }
        return rval;
    }
    
    public double totalStopP(Nucleus projectile, double energy) throws NuclearException {
    	if (!alreadySetUp) {
    		setup();
    	}
        double rval=0;
        double [] stop = StopP(projectile,energy);
        for (int i=0; i< Z.length; i++){
            double conversion = AVAGADRO/natWeight[Z[i]];
            rval += conversion*stop[i]*fractions[i];
        }
        return rval;
    }
    
    
    /**
     * Calculates electronic stopping of hydrogen, up to 40 MeV/A for passed
     * projectile.  ElStopHI modifies the results of this routine for heavier
     * ions.
     *
     * @see #ElStopHI
     * @param energy kinetic energy in MeV of projectile
     * @return stopping powers in eV/(10^15 atom/cm^2) for each compnent of the absorber
     */
    private double [] ElStopH(Nucleus projectile, double energy) throws
    NuclearException{
    	if (!alreadySetUp) {
    		setup();
    	}
        double [] rval = new double[Z.length];
        double e4 = 2.074e-5; //electrons charge e^4
        double tmc2 = 2*.510998902;//2*Me in MeV, 1998 fundamental constants
        double ea=energy*1000/projectile.A;
        int index = (projectile.Z+15)/10;
        for (int i=0; i<Z.length;i++){
            if (ea <= 150) {
                double slow = C2[Z[i]]*Math.pow(ea,0.45);
                if (ea < 10) {
                    rval[i] =slow*AVAGADRO;
                } else {
                    double shigh=(243-0.375*Z[i])*Z[i]/ea*Math.log(1+500/ea+
                    2.195e-6/iave(Z[i]));
                    rval[i] = slow*shigh/(slow+shigh);
                }
            } else {// ea > 150
                if (ea <= 40000) {//use Bethe formula
                    double ale = Math.log(ea);
                    //Bethe shell correction (< 10% for ea from 1000 to 30000)
                    double _A0 = -A0[index];
                    double _A1 = A1[index];
                    double _A2 = -A2[index];
                    double _A3 = A3[index];
                    double _A4 = -A4[index]*1.0e-4;
                    double czt = _A0+(_A1+(_A2+(_A3+_A4*ale)*ale)*ale)*ale;
                    //double czt=0;
                    double beta2=1-Math.pow(1+ea/AMU_TO_KEV,-2);
                    double spwre=8*Math.PI*e4*Z[i]/(beta2*tmc2);
                    double f = Math.log(beta2*tmc2/(1-beta2))-beta2;
                    double logI = Math.log(iave(Z[i]));
                    double spwr=spwre*(f-logI-czt);
                    //effective charge for hydrogen
                    double zh = 1;
                    double ex = 0.2*Math.sqrt(ea)+ea*(0.0012+1.443e-5*ea);
                    if (ex < 20) zh -= Math.exp(-ex);
                    double zfact=1;
                    if (ea <= 1999) {
                        double b=1;
                        if (Z[i] < 35) b=(Z[i]-1)/34;
                        zfact = 1+b*(0.1097-5.561e-5*ea);
                    }
                    rval[i]=spwr*zh*zh*zfact;
                } else throw new NuclearException(
                "Can't do stopping for E/A > 40 MeV");
            }
        }
        return rval;
    }
    
    /**
     * Calculates electronic stopping of ions of all Z, uses E/A for passed
     * projectile.  ElStopHI calls ElStopH, and if Z>=2, modifies the results.
     *
     * @see #ElStopH
     * @param energy kinetic energy in MeV of projectile
     * @return stopping powers in eV/(10^15 atom/cm^2) for each compnent of the absorber
     */
    private double [] ElStopHI(Nucleus projectile, double energy) throws NuclearException {
    	if (!alreadySetUp) {
    		setup();
    	}
        double [] rval = ElStopH(projectile,energy);
        if (projectile.Z==1) return rval;//Hydrogen projectile
        double ea=energy*1000/projectile.A;
        if (projectile.Z<=3) {//Helium or Lithium projectile
            double x=Math.log(ea);
            double ex=Math.pow(7.6-x,2);
            double [] gamma=new double[Z.length];
            if (ex<20) {
                for (int i=0;i<Z.length;i++){//loop through all components of absorber
                    gamma[i] = 1+(.007+.00005*Z[i])*Math.exp(-ex);
                }
            } else {
                for (int i=0;i<Z.length;i++){//loop through all components of absorber
                    gamma[i] = 1;
                }
            }
            if (projectile.Z==2) {//Helium projectile
                ex=0.7446+(0.1429+(0.01562-(0.00267)*x)*x)*x+1.325e-6*Math.pow(x,8);
                for (int i=0;i<Z.length;i++){
                    double zhezh = 2*gamma[i];
                    if (ex<20) zhezh *= 1-Math.exp(-ex);
                    rval[i] *= zhezh*zhezh;
                }
            } else if (projectile.Z==3){//Lithium projectile
                ex=0.7138+(0.002797+(1.348e-6)*ea)*ea;
                for (int i=0;i<Z.length;i++){
                    double zlizh = 3*gamma[i];
                    if (ex<20) zlizh *= 1-Math.exp(-ex);
                    rval[i] *= zlizh*zlizh;
                }
            }
        } else {//projectile with Z > 3
            double b=0.886/5*Math.sqrt(ea)*Math.pow(projectile.Z,2./3.);
            double a = b+0.0378*Math.sin(1.5708*b);
            double zhizh=projectile.Z;
            if (a<20) zhizh *= 1-Math.exp(-a)*(1.034-0.1777*Math.exp(-0.08114*projectile.Z));
            for (int i=0;i<Z.length;i++){
                rval[i] *= zhizh*zhizh;
            }
        }
        return rval;
    }
    
    /**
     * Calculates nucluear stopping of ions of all Z.
     *
     * @param energy kinetic energy in MeV of projectile
     * @param projectile nuclear species of projectile
     * @return stopping powers in eV/(10^15 atom/cm^2) for each compnent of the absorber
     */
    public double []  NuSPwr(Nucleus projectile, double energy){
    	if (!alreadySetUp) {
    		setup();
    	}
        double [] rval= new double[Z.length];
        int Z1 = projectile.Z;
        double E = energy*1000.0;
        //System.out.println("E="+E+", Z1="+Z1);
        double M1 = projectile.getMass().value*MEV_TO_AMU;
        double two3 = 2.0/3.0;
        for (int i = 0; i < Z.length; i++) {
            int Z2 = Z[i];
            double M2=natWeight[Z2];
            // Ziegler v.5 p. 19, eqn. 17
            double x = (M1+M2)*Math.sqrt(Math.pow(Z1,two3)+Math.pow(Z2,two3));
            double eps = 32.53*M2*E/(Z1*Z2*x);
            // Ziegler v.5 p. 19, eqn. 15
            double sn = 0.5*Math.log(1.0+eps)/
            (eps+0.10718*Math.pow(eps,0.37544));
            // Ziegler v.5 p. 19, eqn. 16, stopping power in eV/1e15 atoms/cm^2
            double Sn = sn*8.462*Z1*Z2*M1/x;
            //System.out.println(Z2+": Sn="+Sn);
            //double conversion=AVAGADRO/M2;
            //System.out.println("conversion="+conversion);
            rval[i] *= Sn;
        }
        return rval;
    }
    
    /**
     * Called whenever one wants to change the absorber in this object.  Also
     * called by the constructor.
     */
    public void setAbsorber(Absorber a){
    	if (!alreadySetUp) {
    		setup();
    	}
        absorber = a;
        Z = absorber.getElements();
        fractions = absorber.getFractions();
        thickness = absorber.getThickness();
        //System.out.println(fractions[0]+"*"+Z[0]+", "+fractions[1]+"*"+Z[1]);
        //System.out.println("thickness: "+thickness);
    }
        
    /**
     * Returns the total stopping power in keV per microgram/cm^2.  Uses the
     * absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     */
    public double getStoppingPower(Nucleus projectile, double energy)
    throws NuclearException {
    	if (!alreadySetUp) {
    		setup();
    	}
        return getElectronicStoppingPower(projectile,energy)
        + getNuclearStoppingPower(projectile,energy);
    }
    
    public void setProjectile(Nucleus p){
        this.projectile=p;
    }
    
    /**
     * For the DiffEquations interface.  Returns dx/dE in (ug/cm2)/MeV.
     *
     * @param energy in MeV
     * @param x one element which is thickness
     */
    public double [] dydx(double energy, double [] x){
        double [] y = new double[1];
        double E=energy;
        //System.err.println("projectile: "+projectile+", Energy: "+E+" MeV");
        try {
            y[0] = 1.0/getStoppingPower(projectile,E);//dx/dE in (ug/cm2)/MeV
        } catch (NuclearException ne) {
            System.err.println(getClass().getName()+".dydx(): "+  ne);
        }
        //System.out.println("dydx("+x[0]+", "+E+"): "+y[0]);
        return y;//FIXME
    }
    
    /**
     * Returns the range in milligrams/cm^2 in the absorber material
     * specified in this
     * instance of <code>EnergyLoss</code>.
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     */
    public UncertainNumber getRange(Nucleus projectile, double energy){
        this.projectile=projectile;
        double stepE = .001; //step size in MeV
        double [] temp = {0.0};
        int numSteps=(int)(energy/stepE);
        if (numSteps < 10) numSteps = 10;
        RungeKutta4 rk=new RungeKutta4(this);
        //System.out.println("getRange("+projectile+", "+energy+" MeV): "+numSteps+" steps.");
        double tempOut=rk.dumbIntegral(energy/numSteps,energy,temp,numSteps)[0];
        return new UncertainNumber(tempOut,tempOut*getFractionError(projectile,energy)) ;
    }
    
    static final double MAX_STEP_FRAC = 0.001;
    /**
     * Returns the total energy loss in keV.  Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.  See p. 16 of v.3 of "Stopping and Ranges
     * of Ions in Matter" by Ziegler
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @param theta angle of incidence (from normal in radians)
     */
    public double getEnergyLoss(Nucleus projectile, double energy, double theta) {
        double x;
        double xc=0;
        x=thickness/Math.cos(theta);
        double Ef=energy;
        boolean stop=false;
        double h=0.25*x;
        double eStopped = 0.05*energy;
        //double maxFractionalStepSize = 0.02;
        try {
            //double range = getRange(projectile,energy).value*1000.0;//ug/cm2
            //if (range < x *.95) return energy*1000.0;
            while (!stop){
                //System.out.println("Ef: "+Ef+", h: "+h+",xc: "+xc);
                if (Math.abs(getStoppingPower(projectile,Ef)*h/(Ef*1000)) <
                MAX_STEP_FRAC) {//step not too big
                    if ((h+xc) > x) {//last step
                        stop = true;
                        h = x-xc;//remaining thickness
                        Ef -= getStoppingPower(projectile,Ef)*h/1000;
                        if (Ef <= eStopped) return energy*1000;
                    } else {
                        xc += h;
                        Ef -= getStoppingPower(projectile,Ef)*h/1000;
                        if (Ef <= eStopped) return energy*1000;
                    }
                } else {//step size too big, adjust lower
                    h /= 2.0;
                }
            }
        } catch (NuclearException ne) {
            ne.printStackTrace();
            return -1.0;
        }
        return (energy-Ef)*1000;
    }
    
    /**
     * Returns initial ion energy in MeV.
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @param theta angle of incidence (from normal in radians)
     */
    public double reverseEnergyLoss(Nucleus projectile, double energy, 
    double theta) {
        double x=thickness/Math.cos(theta);
        double xc=0;
        double Ei=energy;
        boolean stop=false;
        double h=0.25*x;
        try {
            double deltaE = getStoppingPower(projectile,Ei)*h/1000;
            while (!stop){
                if (Math.abs(deltaE/Ei) < MAX_STEP_FRAC ) {//step not too big                   
                    if ((h+xc) > x) {//last step
                        stop = true;
                        h = x-xc;//remaining thickness   
                        deltaE = getStoppingPower(projectile,Ei)*h/1000;                     
                        Ei += deltaE;
                    } else {
                        xc += h;
                        deltaE = getStoppingPower(projectile,Ei)*h/1000;                     
                        Ei += deltaE;
                    }
                } else {//step size too big, adjust lower
                    h /= 2.0;
                    deltaE = getStoppingPower(projectile,Ei)*h/1000;
                }
                //System.out.println(h+" "+deltaE+" "+Ei);
            }
        } catch (NuclearException ne) {
            ne.printStackTrace();
            return -1.0;
        }
        return Ei;
    }
    
    /**
     * Returns the light output produced in a plastic scintillator
     * by an ion losing energy in it.  The units are arbitrary,
     * normalized to L.O.=30 for an 8.78 MeV alpha.  The formula
     * for this comes from NIM 138 (1976) 93-104, table 3, row I. 
     * The formula is implemented in the private method, getL.
     * The formula is technically only good for 0.5 MeV/u to 15
     * MeV/u and assumes complete stopping, so if there is partial
     * energy loss, we take the difference of the light output
     * for the initial and final energies.  The accuracy is +/- 20%.
     */
    public double getPlasticLightOutput(Nucleus projectile, double energy,double theta){
    	double efinal=energy-1000*getEnergyLoss(projectile,energy,theta);
    	if (efinal < 0.0) {
    		return getL(projectile,energy);
    	} else {
    		return getL(projectile,energy)-getL(projectile,efinal);	
    	}
    }
    
    private double getL(Nucleus projectile, double energy){
    	return 4.0*Math.pow(projectile.Z*projectile.A,-0.63)*Math.pow(energy,1.62);
    }
    
    public double getEnergyLoss(Nucleus projectile, double energy){
        return getEnergyLoss(projectile, energy, 0.0);
    }
    public double getThinEnergyLoss(Nucleus projectile, double energy){
        return getThinEnergyLoss(projectile, energy, 0.0);
    }
    
    /**
     * Returns the total energy loss in keV.  Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.  See p. 16 of v.3 of "Stopping and Ranges
     * of Ions in Matter" by Ziegler
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @param theta angle of incidence (from normal in radians)
     */
    public double getThinEnergyLoss(Nucleus projectile, double energy,
    double theta){
        double xc=0;
        double x=thickness/Math.cos(theta);
        double Ef=energy;
        boolean stop=false;
        double h=0.25*x;
        try {
            while (!stop){
                //System.out.println("Ef: "+Ef+", h: "+h+",xc: "+xc);
                if (Math.abs(getStoppingPower(projectile,Ef)*h/(Ef*1000))
                < MAX_STEP_FRAC) {//step not too big
                    if ((h+xc) > x) {//last step
                        stop = true;
                        h = x-xc;//remaining thickness
                        Ef -= getStoppingPower(projectile,Ef)*h/1000;
                    } else {
                        xc += h;
                        Ef -= getStoppingPower(projectile,Ef)*h/1000;
                    }
                } else {//step size too big, adjust lower
                    h /= 2.0;
                }
            }
        } catch (NuclearException ne) {
            ne.printStackTrace();
            return -1.0;
        }
        return (energy-Ef)*1000;
    }
    
    /**
     * Returns the total energy loss in keV.  Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.  See p. 16 of v.3 of "Stopping and Ranges
     * of Ions in Matter" by Ziegler
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     */
    public UncertainNumber getEnergyLossUnc(Nucleus projectile, double energy){
        double eloss=getEnergyLoss(projectile,energy);//in keV
        if (eloss==energy*1000.0) return new UncertainNumber(eloss,0.0);
        return new UncertainNumber(eloss,eloss*getFractionError(projectile,energy-eloss/1000));
    }
    
    /**
     * Returns the fractional error in energy loss.  Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.  See p. 16 of v.3 of "Stopping and Ranges
     * of Ions in Matter" by Ziegler
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     */
    public double getFractionError(Nucleus projectile, double energy){
        double mass = projectile.getMass().value;
        double ea=energy/(mass*MEV_TO_AMU);//MeV/amu
        if (ea > 2.0) {
            return 0.025;
        } else {
            if (absorber instanceof Gas) {
                return 0.1;
            } else {//solid
                return 0.05;
            }
        }
    }
    
    /**
     * Returns the electronic stopping power in keV per microgram/cm^2.
     * Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>. See p. 16 in Andersen & Ziegler,
     * "The Stopping and Ranges of Ions in Matter", volume 3.
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     */
    public double getElectronicStoppingPower(Nucleus projectile, double energy)
    throws NuclearException {
    	if (!alreadySetUp) {
    		setup();
    	}
        /* First part - calculate stopping power for 1H at this energy
         * Find Energy in keV per amu of projectile */
        double mass = projectile.getMass().value;
        double ea=energy*1000.0/(mass*MEV_TO_AMU);//keV/amu
        /* Find contributions of components */
        double [] stopH=new double [Z.length];
        if (ea <= 10.0) {
            for (int i = 0; i < Z.length; i++) {
                stopH[i]=coeffs[0][Z[i]]*Math.sqrt(ea);
            }
        } else if (ea > 10.0 && ea <= 1000.0) {
            for (int i = 0; i < Z.length; i++) {
                double Slow=coeffs[1][Z[i]]*Math.pow(ea,0.45);
                double Shigh=coeffs[2][Z[i]]/ea*
                Math.log(1.0+coeffs[3][Z[i]]/ea+coeffs[4][Z[i]]*ea);
                stopH[i]=Slow*Shigh/(Slow+Shigh);
            }
        } else if (ea > 1000.0 && ea <= 100000.0) {
            /* beta squared */
            double beta2=(energy*(energy+2.*mass))/Math.pow(energy+mass,2.0);
            for (int i = 0; i < Z.length; i++) {
                stopH[i]=coeffs[5][Z[i]]/beta2;
                double logTerm = coeffs[6][Z[i]]*beta2/(1.0-beta2) - beta2 -
                coeffs[7][Z[i]];
                for (int j=1;j<=4;j++){
                    logTerm += coeffs[7+j][Z[i]] * Math.pow(Math.log(ea),j);
                }
                stopH[i] *= Math.log(logTerm);
            }
        } else throw new NuclearException("EnergyLoss.getElectronicStopping"+
        "Power(): E/A in keV/amu  > 100000: "+ea);
        //adjust if not hydrogen
        if (projectile.Z>1){
            for (int i=0;i<Z.length;i++){
                double zratio;//3 diff cases He,Li,Heavy Ion see p.9, v.5
                if (projectile.Z<4){
                    double lea = Math.log(ea);
                    double expterm=Math.pow(7.6-lea,2.0);
                    double gamma = 1.+(.007+.00005*Z[i])*Math.exp(-expterm);
                    //System.out.println("gamma = "+gamma+",lea = "+lea);
                    if (projectile.Z==2){//He ion
                        expterm=.7446+.1429*lea+
                        .01562*lea*lea-.00267*Math.pow(lea,3.0)+
                        1.325e-06*Math.pow(lea,8.0);
                        //System.out.println("EX = "+expterm);
                        zratio=2.*gamma*(1.-Math.exp(-expterm));
                        //System.out.println("zratio="+zratio);
                    } else {//Li ion
                        zratio=3.*gamma*(1.-Math.exp(-(.7138+.002797*ea+
                        1.348e-06*ea*ea)));
                    }
                } else {// projectile.Z>= 4
                    double B=.886*Math.sqrt(ea/25.)/Math.pow(projectile.Z,2./3.);
                    double A=B+.0378*Math.sin(Math.PI*B/2.);
                    zratio=1-Math.exp(-A)*(1.034-.1777*Math.exp(-.08114*
                    projectile.Z));
                    //System.out.println(".");
                    zratio *= projectile.Z;
                }
                stopH[i] *= zratio*zratio;
                //System.out.println("SHI/SH="+zratio*zratio);
            }
        }
        //sum up components
        double Stotal=0.0;
        double conversion=0;
        for (int i=0;i<Z.length;i++){
            conversion += fractions[i]*natWeight[Z[i]];
            //System.out.println("conversion="+conversion);
            Stotal += fractions[i]*stopH[i];
        }
        Stotal *= AVAGADRO/conversion;
        return Stotal;
    }
    
    /**
     * Returns the nuclear stopping power in keV per microgram/cm^2.
     * Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.
     *
     * @param projectile the ion being stopped
     * @param energy the ion kinetic energy in MeV
     */
    public double getNuclearStoppingPower(Nucleus projectile, double energy){
    	if (!alreadySetUp) {
    		setup();
    	}
        int Z1 = projectile.Z;
        double E = energy*1000.0;
        //System.out.println("E="+E+", Z1="+Z1);
        double M1 = projectile.getMass().value*MEV_TO_AMU;
        double two3 = 2.0/3.0;
        double Stotal = 0.0;
        for (int i = 0; i < Z.length; i++) {
            int Z2 = Z[i];
            double M2=natWeight[Z2];
            // Ziegler v.5 p. 19, eqn. 17
            double x = (M1+M2)*Math.sqrt(Math.pow(Z1,two3)+Math.pow(Z2,two3));
            double eps = 32.53*M2*E/(Z1*Z2*x);
            // Ziegler v.5 p. 19, eqn. 15
            double sn = 0.5*Math.log(1.0+eps)/
            (eps+0.10718*Math.pow(eps,0.37544));
            // Ziegler v.5 p. 19, eqn. 16, stopping power in eV/1e15 atoms/cm^2
            double Sn = sn*8.462*Z1*Z2*M1/x;
            //System.out.println(Z2+": Sn="+Sn);
            double conversion=AVAGADRO/M2;
            //System.out.println("conversion="+conversion);
            double Stemp=fractions[i]*Sn*conversion;
            Stotal += Stemp;
        }
        return Stotal;
    }
        
    public static void main(String [] args){
        String [] SCINT_ELEMENTS = {"C","H"};
        double [] SCINT_FRACTIONS = {10,11};
        Absorber target;
        int Z=3; int A=6;
        Nucleus ion=new Nucleus(Z,A);
        try{
            target = new Solid(0.635*1032/*970.0*/,
            Absorber.MILLIGRAM_CM2,SCINT_ELEMENTS,SCINT_FRACTIONS);
            target = Gas.Isobutane(1,15);
            EnergyLoss eloss = new EnergyLoss(target);
            System.out.println(ion+" incident on "+target);
            System.out.println("E\tNuclear\tElectronic\tRange");
            for (int energy=1; energy <= 20; energy += 1){
                double S = eloss.getNuclearStoppingPower(ion,energy);
                System.out.print(energy+"\t");
                System.out.print(S+"\t");
                S = eloss.getElectronicStoppingPower(ion,energy);
                System.out.print(S+"\t");
                UncertainNumber range = eloss.getRange(ion,energy);
                System.out.println(range);
            }
        } catch (NuclearException ne) {
            System.err.println(ne);
        }
    }
}
