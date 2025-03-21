/***************************************************************
 * Nuclear Simulation Java Class Libraries
 * Copyright (C) 2003 Yale University
 * 
 * Original Developer
 *     Dale Visser (dale@visser.name)
 * 
 * OSI Certified Open Source Software
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the University of Illinois/NCSA 
 * Open Source License.
 * 
 * This program is distributed in the hope that it will be 
 * useful, but without any warranty; without even the implied 
 * warranty of merchantability or fitness for a particular 
 * purpose. See the University of Illinois/NCSA Open Source 
 * License for more details.
 * 
 * You should have received a copy of the University of 
 * Illinois/NCSA Open Source License along with this program; if 
 * not, see http://www.opensource.org/
 **************************************************************/
/*
 * SpancReaction.java
 *
 * Created on December 16, 2001, 3:27 PM
 */

package dwvisser.analysis.spanc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dwvisser.math.UncertainNumber;
import dwvisser.nuclear.KinematicsException;
import dwvisser.nuclear.NuclearException;
import dwvisser.nuclear.Nucleus;
import dwvisser.nuclear.Reaction;

/**
 * Class for holding modifiable specifications of different scattering
 * measurements on a magnetic spectrograph.
 *
 * @author  Dale Visser
 * @version 1.2
 * @since 1.0
 */
public final class SpancReaction implements java.io.Serializable {

    private Nucleus beam;
    private boolean beamUncertain=false;
    private Nucleus targetNuclide;
    private boolean targetUncertain=false;
    private Nucleus projectile;
    private boolean projectileUncertain=false;
    private boolean residualUncertain=false;
    private double beamEnergy;
    private double magneticField;
    private Target target;
    private int interactionLayer;
    private int projectileCharge;
    private double thetaDegrees;
    private static final List REACTIONS = Collections.synchronizedList(
    new ArrayList());
    
    /** 
     * Creates new SpancReaction.
     * 
	 * @param beamN beam nuclear species
	 * @param tn target nuclear species
	 * @param p projectile nuclear species
	 * @param e beam kinetic energy in MeV
	 * @param b spectrograph field in kG
	 * @param t target specification
	 * @param il layer in target for nuclear interaction
	 * @param q charge of the projectile ion
	 * @param angle of the spectrograph in degrees
     */
    public SpancReaction(Nucleus beamN, Nucleus tn, 
    Nucleus p, double e, double b, Target t, 
    int il, int q, double angle) {
    	setValues(beamN,tn,p,e,b,t,il,q,angle);
        REACTIONS.add(this);
    }

	/**
	 * Set the parameters of this reaction.
	 * 
	 * @param beamN beam nuclear species
	 * @param tn target nuclear species
	 * @param p projectile nuclear species
	 * @param e beam kinetic energy in MeV
	 * @param b spectrograph field in kG
	 * @param t target specification
	 * @param il layer in target for nuclear interaction
	 * @param q charge of the projectile ion
	 * @param angle of the spectrograph in degrees
	 */    
    public void setValues(Nucleus beamN, Nucleus tn, Nucleus p,
    double e, double b, Target t, int il, int q, 
    double angle) {
        beam=beamN;
        targetNuclide=tn;
        projectile=p;
        beamEnergy=e;
        magneticField=b;
        target = t;
        interactionLayer=il;
        projectileCharge=q;
        thetaDegrees=angle;
    }    	
    
    /**
     * Set whether to consider the beam mass uncertain when
     * determining statistical error bars. Should be true
     * when multiple beams are used for calibration.
     * 
     * @param state true if the beam mass is uncertain
     */
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
        String rval =  "Reaction "+REACTIONS.indexOf(this)+"\n";
        rval += description()+"\n";
        rval += "Target Name: "+target.getName()+", interaction layer = "+interactionLayer+"\n";
        rval += "Projectile:  Q = +"+projectileCharge+", Theta = "+thetaDegrees+" deg\n";
        rval += "B-field = "+magneticField+" kG\n";
        return rval;
    }
    
    public String description(){
    	return targetNuclide+"("+beamEnergy+" MeV "+beam+","+projectile+")";
    }
    
    static public void removeReaction(int index){
        REACTIONS.remove(index);
    }
    
    static public void removeAllReactions(){
        REACTIONS.clear();
    }
    
    static public int getReactionIndex(SpancReaction reaction){
        return REACTIONS.indexOf(reaction);
    }
    
    static public SpancReaction[] getAllReactions(){
        SpancReaction [] rval = new SpancReaction[REACTIONS.size()];
        REACTIONS.toArray(rval);
        return rval;
    }
    
    static public Collection getReactionCollection(){
        return REACTIONS;
    }
    
    static public void refreshData(Collection retrievedReactions){
        REACTIONS.addAll(retrievedReactions);
    }
    
    static public SpancReaction getReaction(int index){
        return (SpancReaction)REACTIONS.get(index);
    }
    
    UncertainNumber getRho(double ExProjectile, UncertainNumber ExResidual) throws KinematicsException, NuclearException {
        Nucleus tempProjectile = new Nucleus(projectile.Z, projectile.A, ExProjectile);
        int calculateOption = 0;
        double [] rho = new double[2];
        for (int i=0; i<2; i++){
            double exResid = ExResidual.value + i*ExResidual.error;
            Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile, 
            target.calculateInteractionEnergy(interactionLayer, beam, beamEnergy),
            thetaDegrees, exResid);
            double KEinit = rxn.getLabEnergyProjectile(0);
            double KEfinal = target.calculateProjectileEnergy(interactionLayer,
            tempProjectile,KEinit, Math.toRadians(thetaDegrees));
            rho[i] = Reaction.getQBrho(tempProjectile,KEfinal)/projectileCharge/magneticField;
        }
        double error_from_ex=Math.abs(rho[0]-rho[1]);
        if (beamUncertain) calculateOption |= Reaction.UNCERTAIN_BEAM_MASS_OPTION;
        if (targetUncertain) calculateOption |= Reaction.UNCERTAIN_TARGET_MASS_OPTION;
        if (projectileUncertain) calculateOption |= Reaction.UNCERTAIN_PROJECTILE_MASS_OPTION;
        if (residualUncertain) calculateOption |= Reaction.UNCERTAIN_RESIDUAL_MASS_OPTION;
        Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile, 
        target.calculateInteractionEnergy(interactionLayer, beam, beamEnergy),
        thetaDegrees, ExResidual.value);
        UncertainNumber KEinit = rxn.getLabEnergyProjectile(0,calculateOption);
        double KEfinal = target.calculateProjectileEnergy(interactionLayer,
        tempProjectile,KEinit.value, Math.toRadians(thetaDegrees));
        UncertainNumber qbr_unc_masses = Reaction.getQBrho(tempProjectile,
        new UncertainNumber(KEfinal,KEinit.error),projectileUncertain);
        UncertainNumber rho_unc_masses = qbr_unc_masses.divide(projectileCharge).divide(magneticField);
        return new UncertainNumber(rho[0], Math.sqrt(error_from_ex*error_from_ex+
        rho_unc_masses.error*rho_unc_masses.error));
    }
    
    UncertainNumber getExResid(double ExProjectile, UncertainNumber rho) throws
    KinematicsException, NuclearException {
        Nucleus tempProjectile = new Nucleus(projectile.Z, projectile.A, ExProjectile);
        Reaction rxn = new Reaction(targetNuclide, beam, tempProjectile, 
        target.calculateInteractionEnergy(interactionLayer, beam, beamEnergy),
        thetaDegrees, 0.0);
        double [] p= new double[2];
        //need to add target energy loss back to brho for accurate value
        for (int i=0; i<2; i++){
            double qbr = (rho.value+i*rho.error)*projectileCharge*magneticField;
            double KE = Reaction.getKE(tempProjectile,qbr);
            KE = target.calculateInitialProjectileEnergy(interactionLayer,
            tempProjectile,KE,Math.toRadians(thetaDegrees));
            p[i] = Reaction.getQBrho(tempProjectile,KE)*Reaction.QBRHO_TO_P;
        }
        UncertainNumber momentum = new UncertainNumber(p[0],Math.abs(p[1]-p[0]));
        return rxn.getEx4(momentum);
    }
    
    public Nucleus getBeam(){
        return beam;
    }
    
    public double getBeamEnergy(){
        return beamEnergy;
    }
    
    public Target getTarget(){
        return target;
    }
    
    public int getInteractionLayer(){
        return interactionLayer;
    }
    
    public Nucleus getTargetNuclide(){
        return targetNuclide;
    }
    
    public Nucleus getProjectile() {
        return projectile;
    }
    
    public Nucleus getResidual() throws NuclearException {
        return new Nucleus(beam.Z+targetNuclide.Z-projectile.Z, 
        beam.A+targetNuclide.A-projectile.A);
    }
    
    public int getQ(){
        return projectileCharge;
    }
    
    public double getTheta(){
        return thetaDegrees;
    }
    
    public double getMagneticField(){
        return magneticField;
    }
    
    
}
