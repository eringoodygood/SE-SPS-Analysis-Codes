package net.sourceforge.nukesim.nuclear;

import jade.physics.Quantity;
import jade.physics.VolumetricDensity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.nukesim.ColumnarTextReader;


/**
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version Feb 20, 2004
 */
final class EnergyLossData implements Serializable,NukeUnits {

	final double [][] COEFFS=
	new double[NUMCOEFFS][ATOMIC_ELEMENTS+1];

	/**
	 * "Natural" weights of elements, in amu.
	 */
	final Quantity [] NATURALWEIGHT = 
	new Quantity[ATOMIC_ELEMENTS+1];

	/**
	 * Total number of atomic elements
	 */
	private static final int ATOMIC_ELEMENTS=92;

	/**
	 * "Natural" density of elements, in g/cm^3.
	 */
	final VolumetricDensity [] NATURALDENSITY = 
	new VolumetricDensity[ATOMIC_ELEMENTS+1];

	/**
	 * Table to look up proton number given elemental symbol.
	 */
	final Map ZTOSYMBOLMAP = Collections.synchronizedMap(
	new HashMap());

	/**
	 * The A-1 thru A-12 coefficients for the Ziegler Energy loss 
	 * formulae.
	 */
	static final int NUMCOEFFS=12;
	
	private static final EnergyLossData ELD=new EnergyLossData();
	
	public static EnergyLossData instance(){
		return ELD;
	}
	
	private EnergyLossData() {
		final String COEFF_FILE="coeffs.dat";
		final String ATOM_FILE="atomdata.dat";
	    getEnergyLossParameters(EnergyLoss.class.getResourceAsStream(
	    COEFF_FILE));
	    getAtomicData(EnergyLoss.class.getResourceAsStream(ATOM_FILE));
	}

	private void getEnergyLossParameters(InputStream inStream){
		/* Ionization potential for the various elements. */
		final double [] IONPOTENTIAL=new double[ATOMIC_ELEMENTS+1];
		/* Whether the room temperature phase is gaseous or not. */
		final boolean [] ROOMTEMPGAS=new boolean[ATOMIC_ELEMENTS+1];
	 	/* The symbol of the element. */
		final String [] SYMBOL=new String[ATOMIC_ELEMENTS+1];
	 	/* # of elements for which parameters are available */
		final int NUM_ELEMENTS = 26;
	    if (inStream != null) {
	        try {
	            final ColumnarTextReader ctfr = new ColumnarTextReader(inStream);
	            ctfr.nextLine();//we'll skip the first line (titles)
	            for (int i=0;i<NUM_ELEMENTS;i++){
	                ctfr.nextLine();
	                final int charge=ctfr.readInt(2);
	                final String element=ctfr.readString(2);
	                SYMBOL[charge]=element;
	                ZTOSYMBOLMAP.put(element,new Integer(charge));
	                for (int j=0;j<NUMCOEFFS;j++){
	                    COEFFS[j][charge]=ctfr.readDouble(10);
	                }
	                IONPOTENTIAL[charge]=ctfr.readDouble(7);
	                ROOMTEMPGAS[charge]=("g".equals(ctfr.readString(1)));
	            }
	            ctfr.close();
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

	private void getAtomicData(InputStream inStream){
	 	/* Names of the elements. */
		final String [] NAME=new String[ATOMIC_ELEMENTS+1];
	 	/* Atom density of elements, in atoms/cm<sup>3</sup>.*/
		final double [] ATOMDENSITY = new double[ATOMIC_ELEMENTS+1];
	    try {
	        final ColumnarTextReader ctfr = new ColumnarTextReader(inStream);
	        ctfr.nextLine();ctfr.nextLine();//skip first two lines of headers
	        for (int i=0;i<ATOMIC_ELEMENTS;i++){
	            ctfr.nextLine();
	            final int element=ctfr.readInt(2);
	            ctfr.skipChars(4);
	            NAME[element]=ctfr.readString(14);
	            ctfr.skipChars(12);
	            NATURALWEIGHT[element]=Quantity.valueOf(ctfr.readDouble(8),g_per_mol);
	            NATURALDENSITY[element]=VolumetricDensity.volumetricDensityOf(
	            		Quantity.valueOf(ctfr.readDouble(8),g_per_cm3));
	            ATOMDENSITY[element]=ctfr.readDouble(9);
	        }
	        ctfr.close();
	    } catch (FileNotFoundException fnf) {
	        System.err.println("Could not find file: "+fnf);
	    } catch (IOException ioe) {
	        System.err.println(ioe);
	    }
	}

	/**
	 * Lookup method for obtaining an atomic number from the element's
	 * symbol.
	 *
	 * @return the atomic number for the given elemental symbol
	 * @param symbol 1 or 2 letter symbol for the element
	 * @throws NuclearException if the symbol can't be found
	 */
	public int getElement(String symbol) throws NuclearException {
	    final Integer zInt = (symbol==null || symbol.length()==0) ? null : 
	    (Integer)ZTOSYMBOLMAP.get(capitalizeSymbol(symbol));
	    if (zInt==null) {
	    	final StringBuffer message=new StringBuffer(
	    	"No element found for symbol: ");
	    	message.append(symbol);
	    	 throw new NuclearException(message.toString());
	    }
	    return zInt.intValue();
	}
	
	private String capitalizeSymbol(String symbol){
		final StringBuffer rval=new StringBuffer(symbol.substring(0,1).toUpperCase());
		rval.append(symbol.substring(1,symbol.length()).trim().toLowerCase());
		return rval.toString();
	}

	/**
	 * Return the mass of one atom.
	 *
	 * @return mass of one atom
	 * @param element the atomic number of the element
	 */
	public Quantity getNaturalWeight(int element){
	    return NATURALWEIGHT[element];
	}

	/**
	 * Returns density of element in its most common form.
	 *
	 * @return density in g/cm<sup>3</sup>
	 * @param element the atomic number of the element
	 */
	public VolumetricDensity getDensity(int element){
	    return NATURALDENSITY[element];
	}
}
