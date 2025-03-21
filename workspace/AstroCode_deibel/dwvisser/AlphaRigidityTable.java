/*
 * Created on Jun 4, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package dwvisser;
import dwvisser.math.UncertainNumber;
import dwvisser.nuclear.*;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AlphaRigidityTable {
	
	UncertainNumber [] energies=new UncertainNumber[4];
	
	public AlphaRigidityTable(){
		try{
			Nucleus a=new Nucleus(2,4);
			energies[0]=new UncertainNumber(5.44298,0.00013);
			energies[1]=new UncertainNumber(5.48574,0.00012);
			energies[2]=new UncertainNumber(5.762835,0.000030);
			energies[3]=new UncertainNumber(5.80496,0.00005);
			System.out.println("Ealpha, Brho");
			for (int i=energies.length-1;i>=0;i--){
				System.out.println(energies[i]+","+
				Reaction.getQBrho(a,energies[i],false).divide(a.Z));
			}
		} catch (NuclearException e){
			System.err.println(e);
		}
	}

	public static void main(String[] args) {
		new AlphaRigidityTable();
	}
}
