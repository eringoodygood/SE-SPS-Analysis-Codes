/*
 * ResidualKinematics.java
 *
 * Created on April 4, 2001, 12:00 PM
 */

package sort.coinc.offline;
import dwvisser.nuclear.*;
import java.io.*;

/** Class for calculating motion/state of residual, given QBrho of
 * projectile.
 * @author dwvisser
 */
public class ResidualKinematics extends Object {
    static final double QBR_TO_P = 0.299792458; //converts e-kG-cm to MeV/c
    double T1; //beam energy [MeV]
    double M1; //beam rest mass [MeV/c^2]
    double M2; //target rest mass [MeV/c^2]
    double ET; //total energy in lab [MeV]
    double M3; //projectile rest mass [MeV/c^2]
    double theta; //projectile angle [radians]
    double M4gs; //residual rest mass of ground state [MeV/c^2]
    double P3; //projectile momentum [MeV/c]
    double T3; //projectile kinetic energy [MeV/c]
    double E4; //total mass-energy of residual [MeV]
    double P4; //momentum of residual [MeV/c]
    double M4; //mass of residual [MeV/c^2]
    double beta4; //velocity of residual [c]
    double zeta; //angle of residual [radians]
    double Ex4; //Excitation energy of residual [MeV]
    Nucleus residual; //nuclear species of residual

    /** Creates new ResidualKinematics object, using beam energy, prjectile angle,
     * and the species of beam, target, and projectile.
     * @param Tbeam beam kinetic energy in MeV
     * @param thetaD angle in degrees of projectile
     * @param beam beam nucleus
     * @param target target nucleus
     * @param projectile projectile nucleus
     */
    public ResidualKinematics(double Tbeam, double thetaD,Nucleus beam, Nucleus target,
    Nucleus projectile) {
        T1 = Tbeam;
        M1 = beam.getMass().value;
        M2 = target.getMass().value;
        ET = T1+M1+M2;
        M3 = projectile.getMass().value;
        theta=Math.toRadians(thetaD);
        residual=new Nucleus(target.Z+beam.Z-projectile.Z,
        target.A+beam.A-projectile.A);
        M4gs = residual.getMass().value;
    }

    /** Call this constructor to load in mass tables of nuclei.
     */
    public ResidualKinematics(){//called to initialize nuclear masses
        initializeNuclearData();
    }

    /** Sets QBrho of projectile in e-kG-cm, then calculates all other known
     * quantities.
     * @param _qbr QBrho of projectile in e-kG-cm
     */
    public void setQBr(double _qbr){
        P3=_qbr*QBR_TO_P;
        double eta2 = Math.pow(P3/M3,2.0);//eta^2, parameter for calculating T
        T3 = M3 * eta2/(1.0+Math.sqrt(1.0+eta2));
        double P12 = T1*T1+2.0*T1*M1;//P1 squared [MeV^2/c^2]
        E4 = ET-(T3+M3);
        double P42 = P3*P3+P12-2*P3*Math.sqrt(P12)*Math.cos(theta);
        P4 = Math.sqrt(P42);
        M4 = Math.sqrt(E4*E4 - P42);
        beta4 = P4/E4;
        zeta = Math.asin(P3/P4*Math.sin(theta));
        Ex4 = M4-M4gs;
    }
    
    public void setSpectrometerKE(double _ke){
        T3 = _ke;
        double tprime = T3/M3;
        P3 = M3 * Math.sqrt(tprime*(tprime+2));
        //P3= Math.sqrt(T3*T3+2*M3*T3);
        double P12 = T1*T1+2.0*T1*M1;//P1 squared [MeV^2/c^2]
        E4 = ET-(T3+M3);
        double P42 = P3*P3+P12-2*P3*Math.sqrt(P12)*Math.cos(theta);
        P4 = Math.sqrt(P42);
        M4 = Math.sqrt(E4*E4 - P42);
        beta4 = P4/E4;
        zeta = Math.asin(P3/P4*Math.sin(theta));
        Ex4 = M4-M4gs;
    }

    /** Returns the speed of the projectile in the lab.
     * @return speed of residual in units of the speed of light
     */
    public double getResidualBeta(){
        return beta4;
    }
    
    /** Returns total mass-energy of residual.
     * @return total energy in MeV
     */
    public double getResidualTotalEnergy(){
        return E4;
    }
    
    public double getResidualKineticEnergy(){
        return E4-M4;
    }
    
    /** Returns lab momentum of residual.
     * @return momentum in MeV/c
     */
    public double getResidualMomentum(){
        return P4;
    }

    /** Returns lab angle of residual.
     * @return lab angle in radians
     */
    public double getResidualAngle(){
        return zeta;
    }

    /** Returns the excitation energy of the residual.
     * @return excitation energy in MeV
     */
    public double getEx4(){
        return Ex4;
    }

    /** Returns an object representing the residual nucleus.
     * @return residual nucleus with proper excitation energy
     */
    public Nucleus getResidual(){
        return residual;
    }

    private void initializeNuclearData(){
        BindingEnergyTable bet=loadBindingEnergyTableObject();
        if (bet==null) {
            new BuildMassTable();
            bet=loadBindingEnergyTableObject();
        }
        new Nucleus(bet);
    }

    private BindingEnergyTable loadBindingEnergyTableObject()  {
        BindingEnergyTable bet;

        try{
            FileInputStream fis=new FileInputStream(BindingEnergyTable.BET_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            bet=(BindingEnergyTable)ois.readObject();
            ois.close();
        } catch (Exception e) {
            System.err.println("Problem loading Binding Energy Table.:\n"+e);
            return null;
        }
        return bet;
    }

    /** For debugging
     * @param args Java-specified, not used here
     */
    public static void main(String [] args){
        double qbr=2001.95;
        double Tbeam=80.0;
        double theta=5.0;
        new ResidualKinematics();
        Nucleus C12 = new Nucleus(6,12);
        Nucleus alpha = new Nucleus(2,4);
        ResidualKinematics rk = new ResidualKinematics(Tbeam, theta, 
        C12, C12, alpha);
        rk.setQBr(qbr);
        System.out.println("QBr of "+qbr+" e-kG-cm corresponds to a residual "+
        rk.getResidual()+".\n\tbeta = "+rk.getResidualBeta()+"\n\ttheta = "+
        rk.getResidualAngle()+" degrees\n\tEx = "+rk.getEx4()+" MeV");
    }
}