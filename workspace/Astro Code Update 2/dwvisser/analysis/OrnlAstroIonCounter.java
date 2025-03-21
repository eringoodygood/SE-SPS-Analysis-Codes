/*
 * Created on Nov 14, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package dwvisser.analysis;
import dwvisser.nuclear.*;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class OrnlAstroIonCounter {
	
	Nucleus species;
	EnergyLoss window;
	EnergyLoss de12,de3;
	double pressure=3;//gas pressure in torr
	double emin=0;
	double emax=18;
	double dele=0.5;
	
	double[] energy,wloss,de1loss,de2loss,de3loss;
	
	
	public OrnlAstroIonCounter() throws NuclearException {
		species=new Nucleus(12,24);
		window=new EnergyLoss(Solid.Mylar(200,Absorber.MICROGRAM_CM2));
		de12=new EnergyLoss(Gas.CF4(5,pressure));
		de3=new EnergyLoss(Gas.CF4(20,pressure));
		int count=0;
		for (double e=emin; e<=emax; e+=dele){
			count++;
		}
		energy=new double[count];
		wloss=new double[count];
		de1loss=new double[count];
		de2loss=new double[count];
		de3loss=new double[count];
		double e=emin;
		for (int i=0; i<count; i++){
			double eleft=e;
			energy[i]=e;
			if (eleft > 0.0) {
				wloss[i]=.001*window.getEnergyLoss(species,eleft);
				eleft -= wloss[i];
			} else {
				wloss[i]=0.0;
			}
			if (eleft > 0.0) {
				de1loss[i]=.001*de12.getEnergyLoss(species,eleft);
				eleft -= de1loss[i];
			} else {
				de1loss[i]=0.0;
			}
			if (eleft > 0.0) {
				de2loss[i]=.001*de12.getEnergyLoss(species,eleft);
				eleft -= de2loss[i];
			} else {
				de2loss[i]=0.0;
			}
			if (eleft > 0.0) {
				de3loss[i]=.001*de3.getEnergyLoss(species,eleft);
				eleft -= de3loss[i];
			} else {
				de3loss[i]=0.0;
			}
			e += dele;
		}
		System.out.println("E\tDE1+DE2\tDE1+DE2+DE3");
		for (int i=0; i<count; i++){
			double sum1=de1loss[i]+de2loss[i];
			double sum2=sum1+de3loss[i];
			System.out.println(energy[i]+"\t"+sum1+"\t"+sum2);
		}
	}

	public static void main(String[] args) {
		try{
			new OrnlAstroIonCounter();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
