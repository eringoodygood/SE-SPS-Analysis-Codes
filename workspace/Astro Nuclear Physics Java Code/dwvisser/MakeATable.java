package dwvisser;
import dwvisser.nuclear.*;
import java.io.*;
public class MakeATable {

	/** 
	 * quick piece of code to make an ASCII table of some
	 * reaction kinematics
	 */
	public static void main(String[] args) {
		Nucleus beam = new Nucleus(1, 1);
		Nucleus target = new Nucleus(6, 12);
		Nucleus projectile = new Nucleus(1, 3);
		double ExResid = 3.3536; //MeV
		double EbeamMin = 36; //MeV
		double EbeamMax = 36; //MeV
		double EbeamStep = 1; //MeV
		double thetaLabMin = 0; //degrees
		double thetaLabMax = 70;
		double thetaLabStep=2;
		File out = new File("d:\\simulations\\C1033536.dat");
		try {
			FileWriter fw = new FileWriter(out);
			fw.write("Ebeam\tLabTheta\tBrho\n");
			for (double Ebeam = EbeamMin; Ebeam <= EbeamMax; Ebeam += EbeamStep) {
				for (double thetaLab = thetaLabMin; thetaLab <= thetaLabMax; thetaLab += thetaLabStep){
					Reaction rxn =
						new Reaction(target, beam, projectile, Ebeam, thetaLab, ExResid);
					fw.write(Ebeam + "\t" + thetaLab
						+ "\t" + (rxn.getQBrho(0)/projectile.Z)
						+ "\n");
				}
			}
			fw.close();
		} catch (Exception e) {
			System.err.println(e);
		}
		System.out.println("done.");
	}
}