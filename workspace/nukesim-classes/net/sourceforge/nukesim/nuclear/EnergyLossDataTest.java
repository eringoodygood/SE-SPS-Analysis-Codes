package net.sourceforge.nukesim.nuclear;

import net.sourceforge.nukesim.math.QuantityUtilities;
import jade.JADE;
import jade.physics.models.RelativisticModel;
import junit.framework.TestCase;

/**
 * JUnit tests for EnergyLossData.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 */
public class EnergyLossDataTest extends TestCase {

	EnergyLossData eld;
	
	/**
	 * Constructor for GateTest.
	 * 
	 * @param arg0
	 */
	public EnergyLossDataTest(String arg0) {
		super(arg0);
		JADE.initialize();
		RelativisticModel.select();
		eld=EnergyLossData.instance();
	}

	/**
	 * Initialize local variables for the tests.
	 * 
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGetElement() {
		try{
			final int zFe=eld.getElement("Fe");
			final int zNi=eld.getElement("Ni");
			final int zAu=eld.getElement("Au");
			assertEquals(zFe, 26);
			assertEquals(zNi, 28);
			assertEquals(zAu, 79);
			System.out.println("Z\tNat. Wt. [g/mol]\tg/cm\u00b3");
			final String tab="\t";
			System.out.println(zFe+tab+
					QuantityUtilities.noUnits(eld.getNaturalWeight(zFe),NukeUnits.g_per_mol)+tab+
			QuantityUtilities.noUnits(eld.getDensity(zFe),NukeUnits.g_per_cm3));
			System.out.println(zNi+tab+
					QuantityUtilities.noUnits(eld.getNaturalWeight(zNi),NukeUnits.g_per_mol)+tab+
			QuantityUtilities.noUnits(eld.getDensity(zNi),NukeUnits.g_per_cm3));
			System.out.println(zAu+tab+
					QuantityUtilities.noUnits(eld.getNaturalWeight(zAu),NukeUnits.g_per_mol)+tab+
					QuantityUtilities.noUnits(eld.getDensity(zAu),NukeUnits.g_per_cm3));
		} catch (NuclearException ne){
			ne.printStackTrace();
		}
		
	}

	public void testInGateII() {
	}

}
