package dwvisser.nuclear;
import dwvisser.math.*;
import java.io.*;
import java.util.Vector;

/**
 * Class representing atomic nuclei for the purposes of
 * kinematics calculations.
 * 
 * @author <a href="dale@visser.name">Dale W Visser</a>
 */
public class Nucleus extends Particle implements Serializable {

    /* Values for proton and neutron masses 
     * taken from CODATA 1998 Recommended values
     */
    static public UncertainNumber PROTON_MASS =
    new UncertainNumber(938.271998,0.000038);

    static public UncertainNumber NEUTRON_MASS =
    new UncertainNumber(939.565330,0.000038);

    /**
     * Stores binding energy data for all <code>Nucleus</code> objects
     * to access.
     */
    static BindingEnergyTable bet=null;

    /**
     * Mass number.
     */
    public int A;

    /**
     * Element Number.
     */
    public int Z;

    /**
     * Excitation of nucleus in MeV.
     */
    public UncertainNumber Ex;

	/**
	 * The binding energy in MeV for this <code>Nucleus</code> object.
	 */
    public UncertainNumber bindingEnergy=null;

    /**
     * Calling this constructor causes the Binding energies to be read in.
     */
    Nucleus(int Z, int A, boolean buildingTable){
    	if (buildingTable) {
        	this.Z=Z;
        	this.A=A;
        	this.Ex=new UncertainNumber(0.0,0.0);
    	} else	{
        	System.err.println("This is a special constructor that only BindingEnergyTable should ever call.");
    	}
    }

    /**
     * Default constructor, returns an object representing a particular nucleus.
     * 
     * @param Z element number
     * @param A mass number
     * @param Ex excitation energy in MeV
     */
    public Nucleus(int Z, int A, UncertainNumber Ex) {
    	setup();//make sure tables exist
        this.Z=Z;
        this.A=A;
        this.Ex=Ex;
    }

    /**
     * Returns an object representing a particular nucleus.
     * 
     * @param Z element number
     * @param A mass number
     * @param Ex excitation energy in MeV
     */
    public Nucleus(int Z, int A, double Ex){
        this(Z,A,new UncertainNumber(Ex));
    }

    /**
     * Returns an object representing a particular nucleus in its ground state.
     * 
     * @param Z element number
     * @param A mass number
     * @param Ex excitation energy in MeV
     */
    public Nucleus(int Z, int A){
        this(Z,A,new UncertainNumber(0.0,0.0));
    }
    
    /**
     * Sets up lookup tables.
     */
    static void setup(){
    	if (bet==null) {
        	bet=BindingEnergyTable.load();
    	}
    }
    	
	/**
	 * Returns element symbol for the specified element.
	 * 
	 * @param z_ element number
	 */
    static public String getElementSymbol(int z_){
    	setup();//make sure table exists
        return bet.getSymbol(z_);
    }

	/**
	 * Returns element number for the given Symbol.
	 * 
	 * @param z_ element number
	 */
    static public int getElementNumber(String symbol){
    	setup();//make sure tables exist
    	if (symbol.equals("g")){
    		return 0;
    	}
        return bet.getElementNumber(symbol);
    }

    /**
     * Parses a string like "197Au" into a Nucleus object.
     */
    static public Nucleus parseNucleus(String s){
    	setup();//make sure tables exist
        try{
        	if (s.equals("g")) {
        		return new Nucleus(0,0);
        	}
            StreamTokenizer st=new StreamTokenizer(new StringReader(s));
            st.nextToken();
            int _A=(int)st.nval;
            st.nextToken();
            if (st.sval.equalsIgnoreCase("n")&& _A==1) {
            	return new Nucleus(0,1);
            } else {
            	int _Z=getElementNumber(st.sval);
            	return new Nucleus(_Z,_A);
            }
        } catch (IOException ioe) {
            return null;
        }
    }

	/**
	 * Pass through call to return all stable isotopes for this element.
	 */
    static public Vector getIsotopes(int Z){
    	setup();//make sure tables exist
        return bet.getIsotopes(Z);
    }
    
    /**
     * Mass in MeV/c^2.
     */
    public UncertainNumber getMass() {
    	setup();//make sure tables exist
        return getGroundStateMass().plus(Ex);
    }

    /**
     * Returns ground state mass in MeV/c^2.
     */
    public UncertainNumber getGroundStateMass() {
    	setup();//make sure tables exist
        if (bindingEnergy==null) bindingEnergy=bet.getBindingEnergy(this);
        if (Z==1 && A==1) {
            return PROTON_MASS;
        } else if (A==0) {
        	return new UncertainNumber(0);
        } else {
            return new UncertainNumber(PROTON_MASS.value*Z+NEUTRON_MASS.value*(A-Z)-
            bindingEnergy.value,bindingEnergy.error);
        }
    }

	/**
	 * Returns element symbol for this nucleus.
	 */
    public String getElementSymbol(){
    	setup();//make sure tables exist
    	if (A==0) {
    		return "g";
    	}
        return bet.getSymbol(Z);
    }
    
    /**
     * Checks if object represents the same isotope.
     */
    public boolean equals(Object o) {
        if (!this.getClass().isInstance(o)) {
        	return false;
        }
        Nucleus n = (Nucleus)o;
        if (n.hashCode()!=hashCode()) {
        	return false;
        }
        return true;
    }

	/**
	 * Inherited from <code>java.lang.Object</code> for storing in a HashTable.
	 */
    public int hashCode(){
        int out = Z*10000+A;
        return out;
    }

	/**
	 * Override of <code>java.lang.Object</code> for printing.
	 */
    public String toString(){
        return A+getElementSymbol();
    }

    static public void main(String [] args) {
        Nucleus alpha = new Nucleus(2,4);
        System.out.println(alpha+" has mass "+alpha.getMass()+" MeV/c^2");
    }
}

