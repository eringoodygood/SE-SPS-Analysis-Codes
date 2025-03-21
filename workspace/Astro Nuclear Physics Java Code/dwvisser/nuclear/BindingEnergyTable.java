package dwvisser.nuclear;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import dwvisser.math.UncertainNumber;

/**
 * Class for storing binding energies. Has a hashtable storing binding energies
 * (as <code>UncertainNumber</code> objects) with the <code>
 * Nucleus</code> objects as keys. It also has a table mapping element 
 * symbols to element numbers.
 */
public class BindingEnergyTable extends Hashtable {

	/**
	 * name of ASCII file in <code>dwvisser.nuclear</code> which contains
	 * mass data
	 */
	private static final String ASCII_FILE = "mass_rmd.mas95";
	
	/**
	 * name of file to load stored binding energies as and object
	 */
	private static final String BET_FILENAME = "BindingEnergies.obj";

	/**
	 * array of element symbols keyed to element numbers
	 */
	private String[] symbolTable = new String[112];

	/**
	 * Default constructor, necessary for loading from <code>ObjectInputStream</code>.
	 */
	public BindingEnergyTable() {
	}

	/**
	 * Get the binding energy in MeV of the specified nucleus.
	 */
	public UncertainNumber getBindingEnergy(Nucleus n) {
		return (UncertainNumber) get(n);
	}

	/**
	 * Get the element symbol for the specified element number.
	 */
	public String getSymbol(int Z) {
		return symbolTable[Z];
	}

	/**
	 * Get the element number for the specified element symbol, ignoring
	 * case. Because of the ambiguity between "n" for neutron
	 * and "N" for nitrogen, this only returns the 7, the
	 * element number of Nitrogen, for "n" and "N".
	 */
	public int getElementNumber(String s) {
		for (int i = 1; i < symbolTable.length; i++) {
			if (s.equalsIgnoreCase(symbolTable[i]))
				return i;
		}
		return -1;
	}

	/**
	 * Return a <code>Vector</code> of <code>Nucleus</code> objects 
	 * representing the isotopes of the given element.
	 */
	public Vector getIsotopes(int Z) {
		Nucleus temp;
		Vector rval = new Vector();
		for (Enumeration e = keys(); e.hasMoreElements();) {
			temp = (Nucleus) e.nextElement();
			if (temp.Z == Z)
				rval.addElement(temp);
		}
		return rval;
	}
	
	/**
	 * Associate the given element number and symbol.
	 */
	private void storeSymbol(int Z, String symbol) {
		symbolTable[Z] = symbol;
	}

	static public BindingEnergyTable load() {
		BindingEnergyTable bet;

		try {
			FileInputStream fis = new FileInputStream(BET_FILENAME);
			ObjectInputStream ois = new ObjectInputStream(fis);
			bet = (BindingEnergyTable) ois.readObject();
			ois.close();
		} catch (Exception e) {
			System.err.println(
				"BindingEnergyTable.load(): I was unable to load the binding energy table. Here's why:");
			System.err.println(
				"----------------------------------------------------------");
			System.err.println(e);
			System.err.println(
				"----------------------------------------------------------");
			System.out.println("I will attempt to build the table using \""+
			ASCII_FILE+"\".");
			bet = build();
		}
		return bet;
	}
	
	/**
	 * We end up here if loading the table was unsuccessful. The masses are 
	 * read in from the ASCII mass table, and a new table is constructed and saved.
	 */
	static private BindingEnergyTable build() {
		String betFilePath = "???";
		try {
			betFilePath = new File(BET_FILENAME).getCanonicalPath();
		} catch (Exception ioe) {
			System.err.println(ioe);
		}
		BindingEnergyTable rval = doIt();
		if (rval != null) {
			System.out.println(
				rval.getClass().getName()
					+ ".build(): I successfully read in the mass table from \""
					+ ASCII_FILE
					+ "\"");
			System.out.println(
				"and saved the table in usable form in \""
					+ betFilePath
					+ "\".");
		}
		return rval;
	}

	/**
	 * The workhorse for build(). Attempts to construct a table from the ASCII file <code>
	 * mass_rmd.mas95</code> residing in the <code>dwvisser.nuclear</code>
	 * package.
	 * 
	 * @return true if successful, false if not
	 */
	static private BindingEnergyTable doIt() {
		String s = "";
		StringReader sr;
		int Z, A;
		UncertainNumber binden;
		BindingEnergyTable bet = new BindingEnergyTable();

		try {
			InputStreamReader isr =
				new InputStreamReader(
					bet.getClass().getResourceAsStream(ASCII_FILE));
			LineNumberReader lnr = new LineNumberReader(isr);
			for (int i = 0; i < 40; i++) {
				s = lnr.readLine();
			}
			do {
				sr = new StringReader(s);
				sr.skip(9); // 1st col. & (N-Z) +& N
				Z = readInt(5, sr);
				A = readInt(5, sr);
				String symbol = readString(3, sr).trim();
				sr.skip(26);
				/* Text file in keV, I want MeV. */
				binden =
					new UncertainNumber(
						readDouble(11, sr) / 1000.0,
						readDouble(9, sr) / 1000.0);
				bet.put(new Nucleus(Z, A,true), binden);
				bet.storeSymbol(Z, symbol);
				sr.close();
				s = lnr.readLine();
			} while (s != null);
			lnr.close();
			s = null;
			sr = null;
			lnr = null;
			isr = null;
			FileOutputStream fos =
				new FileOutputStream(BindingEnergyTable.BET_FILENAME);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(bet);
			oos.close();
			oos = null;
			fos = null;
		} catch (Exception e) {
			System.err.println(
				bet.getClass().getName()
					+ ".doIt(): I was unable to load the mass table from \""
					+ ASCII_FILE
					+ "\". Here's why:");
			System.err.println(
				"----------------------------------------------------------");
			System.err.println(e);
			System.err.println(
				"----------------------------------------------------------");
			return null;
		}
		return bet;
	}

	/* private worker methods for reading in strings, ints, and doubles 
	 * from a Reader. */
	
	static private String readString(int len, Reader r) throws IOException {
		char[] temp;
		temp = new char[len];
		r.read(temp);
		String s = new String(temp);
		return s.replace('#', ' ');
	}

	static private int readInt(int len, Reader r) throws IOException {
		return Integer.parseInt(readString(len, r).trim());
	}

	static private double readDouble(int len, Reader r) throws IOException {
		return Double.parseDouble(readString(len, r).trim());
	}

	/**
	 * for testing purposes only
	 */
	public static void main(String args[]) {
		BindingEnergyTable.load();
	}

}
