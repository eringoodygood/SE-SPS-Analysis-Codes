/*
 * Created on Jul 6, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.sourceforge.nukesim.nuclear;
import jade.units.Unit; 

/**
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version Jul 6, 2004
 */
public interface NukeUnits {
	static final Unit amu=Unit.valueOf("u");
	static final Unit MeV=Unit.valueOf("MeV");
	static final Unit keV=Unit.valueOf("keV");
	static final Unit deg=Unit.valueOf("°");
	static final Unit MeV_fm=Unit.valueOf("MeV fm");
	static final Unit MeVperAmu=Unit.valueOf("MeV/u");
	static final Unit keVperAmu=Unit.valueOf("keV/u");
	static final Unit eTm=Unit.valueOf("e T m");
	static final Unit e=Unit.valueOf("e");
	static final Unit Tm=Unit.valueOf("T m");
	static final Unit tesla=Unit.valueOf("T");
	static final Unit kgauss=tesla.multiply(0.1);
	static final Unit µg_per_cmsq=Unit.valueOf("µg/cm²");
	static final Unit mg_per_cmsq=Unit.valueOf("mg/cm²");
	static final Unit keV_per_µg=keV.divide(µg_per_cmsq);
	static final Unit cm=Unit.valueOf("cm");
	static final Unit mm=Unit.valueOf("mm");
	static final Unit inch=Unit.valueOf("in");
	static final Unit mil=inch.multiply(0.001);
	static final Unit g_per_cm3=Unit.valueOf("g/cm^3");
	static final Unit g_per_mol=Unit.valueOf("g/mol");
	static final Unit light_speed=Unit.valueOf("c");
	static final Unit torr=Unit.valueOf("mmHg");
	static final Unit blank=Unit.valueOf(" ");
}
