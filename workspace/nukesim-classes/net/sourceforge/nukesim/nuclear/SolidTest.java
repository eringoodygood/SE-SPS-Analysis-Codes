/*
 * Created on Aug 20, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.nukesim.nuclear;

import jade.JADE;
import jade.physics.Length;
import jade.physics.Quantity;
import jade.physics.models.RelativisticModel;
import junit.framework.TestCase;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SolidTest extends TestCase implements NukeUnits {
	
	SolidTest(){
		JADE.initialize();
		RelativisticModel.select();
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public static void main(String [] args){
		SolidTest st=new SolidTest();
		try {
			Length l=Length.lengthOf(Quantity.valueOf(0.25,mil));
			System.out.println("1 mil = "+l.toText(cm));
			System.out.println("mylar: "+
					Solid.mylar(l).getDensity().toText(g_per_cm3));
			System.out.println("bc404: "+
					Solid.icru216(l).getDensity().toText(g_per_cm3));
			System.out.println("kapton: "+
					Solid.kapton(l).getDensity().toText(g_per_cm3));
		} catch (NuclearException ne){
			ne.printStackTrace();
		}
	}

}
