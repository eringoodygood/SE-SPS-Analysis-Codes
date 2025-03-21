/*
 * SpancReaction.java
 *
 * Created on December 16, 2001, 3:27 PM
 */

package dwvisser.analysis.spanc;
import java.util.Vector;
import java.util.Collection;
import dwvisser.nuclear.*;
import dwvisser.math.UncertainNumber;

/**
 *
 * @author  Dale
 * @version 
 */
public class SpancReaction implements java.io.Serializable {

    private Nucleus beam;
    private boolean beamUncertain=false;
    
    private Nucleus targetNuclide;
    private boolean targetUncertain=false;
    
    private Nucleus projectile;
    private boolean projectileUncertain=false;
    
    private boolean residualUncertain=false;
    
    private double Ebeam;
    private double Bfield;
    
    private Target target;
    
    private int interaction_layer;
    
    private int Qprojectile;
    
    private double thetaDegrees;
    
    private static Vector allReactions = new Vector(1,1);
    
    /** Creates new SpancReaction */
    public SpancReaction(Nucleus beam, Nucleus targetNuclide, Nucleus projectile,
    double Ebeam, double B, Target target, int interaction_layer, int Qprojectile, 
    double thetaDegrees) {
    	setValues(beam, targetNuclide,projectile,Ebeam,B,target,interaction_layer,Qprojectile,
    	thetaDegrees);
        allReactions.addElement(this);
    }
    
    public void setValues(Nucleus beam, Nucleus targetNuclide, Nucleus projectile,
    double Ebeam, double B, Target target, int interaction_layer, int Qprojectile, 
    double thetaDegrees) {
        this.beam=beam;
        this.targetNuclide=targetNuclide;
        this.projectile=projectile;
        this.Ebeam=Ebeam;
        this.Bfield=B;
        this.target = target;
        this.interaction_layer=interaction_layer;
        this.Qprojectile=Qprojectile;
        this.thetaDegrees=thetaDegrees;
    }    	
    
    public void setBeamUncertain(boolean state){
        beamUncertain=state;
    }
    public boolean getBeamUncertain(){
        return beamUncertain;
    }
    public void setTargetUncertain(boolean state){
        targetUncertain=state;
    }
    public boolean getTargetUncertain(){
        return targetUncertain;
    }
    public void setProjectileUncertain(boolean state){
        projectileUncertain=state;
    }
    public boolean getProjectileUncertain(){
        return projectileUncertain;
    }
    public void setResidualUncertain(boolean state){
        residualUncertain=state;
    }
    public boolean getResidualUncertain(){
        return residualUncertain;
    }

    public java.lang.String toString() {
        String rval =  "Reaction "+allReactions.indexOf(this)+"\n";
        rval += description()+"\n";
        rval += "Target Name: "+target.getName()+", interaction layer = "+interaction_layer+"\n";
        rval += "Projectile:  Q = +"+Qprojectile+", Theta = "+thetaDegrees+" deg\n";
        rval += "B-field = "+Bfield+" kG\n";
        return rval;
    }
    
    public String description(){
    	return targetNuclide+"("+Ebeam+" MeV "+beam+","+projectile+")";
    }
    
    static public void removeReaction(int index){
        allReactions.removeElementAt(index);
    }
    
    static public void removeAllReactions(){
        allReactions.removeAllElements();
    }
    
    static public int getReactionIndex(SpancReaction reaction){
        return allReactions.indexOf(reaction);
    }
    
    static public SpancReaction[] getAllReactions(){
        SpancReaction [] rval = new SpancReaction[allReactions.size()];
        allReactions.copyInto(rval);
        return rval;
    }
    
    static public Collection getReactionCollection(){
        return allReactions;
    }
    
    static public void refreshData(Collection retrievedReactions){
        allReactions.addAll(retrievedReactions);
    }
    
    static public SpancReaction getReaction(int index){
        return (SpancReaction)allReactions.elementAt(index);
    }
    
    UncertainNumber getRho(double ExProjectile, UncertainNumber ExResidual) throws KinematicsException {
        Nucleus tempProjectile = new Nucleus(projectile.Z, projectile.A, ExProjectile);
        int calculateOption = 0;
        double [] rho = new double[2];
        for (int i=0; i<2; i++){
            double exResid = ExResidual.value + i*ExResidual.error;
            Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile, 
            target.calculateInteractionEnergy(interaction_layer, beam, Ebeam),
            thetaDegrees, exResid);
            double KEinit = rxn.getLabEnergyProjectile(0);
            double KEfinal = target.calculateProjectileEnergy(interaction_layer,
            tempProjectile,KEinit, Math.toRadians(thetaDegrees));
            rho[i] = Reaction.getQBrho(tempProjectile,KEfinal)/Qprojectile/Bfield;
        }
        double error_from_ex=Math.abs(rho[0]-rho[1]);
        if (beamUncertain) calculateOption |= Reaction.UNCERTAIN_BEAM_MASS_OPTION;
        if (targetUncertain) calculateOption |= Reaction.UNCERTAIN_TARGET_MASS_OPTION;
        if (projectileUncertain) calculateOption |= Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION;
        if (residualUncertain) calculateOption |= Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION;
        Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile, 
        target.calculateInteractionEnergy(interaction_layer, beam, Ebeam),
        thetaDegrees, ExResidual.value);
        UncertainNumber KEinit = rxn.getLabEnergyProjectile(0,calculateOption);
        double KEfinal = target.calculateProjectileEnergy(interaction_layer,
        tempProjectile,KEinit.value, Math.toRadians(thetaDegrees));
        UncertainNumber qbr_unc_masses = Reaction.getQBrho(tempProjectile,
        new UncertainNumber(KEfinal,KEinit.error),projectileUncertain);
        UncertainNumber rho_unc_masses = qbr_unc_masses.divide(Qprojectile).divide(Bfield);
        return new UncertainNumber(rho[0], Math.sqrt(error_from_ex*error_from_ex+
        rho_unc_masses.error*rho_unc_masses.error));
    }
    
    UncertainNumber getExResid(double ExProjectile, UncertainNumber rho) throws
    KinematicsException {
        Nucleus tempProjectile = new Nucleus(projectile.Z, projectile.A, ExProjectile);
        Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile, 
        target.calculateInteractionEnergy(interaction_layer, beam, Ebeam),
        thetaDegrees, 0.0);
        double [] p= new double[2];
        //need to add target energy loss back to brho for accurate value
        for (int i=0; i<2; i++){
            double qbr = (rho.value+i*rho.error)*Qprojectile*Bfield;
            double KE = Reaction.getKE(tempProjectile,qbr);
            KE = target.calculateInitialProjectileEnergy(interaction_layer,
            tempProjectile,KE,Math.toRadians(thetaDegrees));
            p[i] = Reaction.getQBrho(tempProjectile,KE)*Reaction.QBRHO_TO_P;
        }
        UncertainNumber momentum = new UncertainNumber(p[0],Math.abs(p[1]-p[0]));
        return rxn.getEx4(momentum);
    }
        
        
    
    public Nucleus getBeam(){
        return beam;
    }
    
    public double getEbeam(){
        return Ebeam;
    }
    
    public Target getTarget(){
        return target;
    }
    
    public int getInteractionLayer(){
        return interaction_layer;
    }
    
    public Nucleus getTargetNuclide(){
        return targetNuclide;
    }
    
    public Nucleus getProjectile() {
        return projectile;
    }
    
    public Nucleus getResidual(){
        return new Nucleus(beam.Z+targetNuclide.Z-projectile.Z, 
        beam.A+targetNuclide.A-projectile.A);
    }
    
    public int getQ(){
        return Qprojectile;
    }
    
    public double getTheta(){
        return thetaDegrees;
    }
    
    public double getBfield(){
        return Bfield;
    }
    
    
}
