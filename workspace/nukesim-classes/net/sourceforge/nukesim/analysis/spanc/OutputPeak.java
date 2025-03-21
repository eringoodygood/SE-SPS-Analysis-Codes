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
package net.sourceforge.nukesim.analysis.spanc;
import jade.physics.Energy;
import jade.physics.Length;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.nukesim.math.MathException;
import net.sourceforge.nukesim.math.QuantityUtilities;
import net.sourceforge.nukesim.math.UncertainNumber;
import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.NukeUnits;
import net.sourceforge.nukesim.statistics.StatisticsException;

/**
 * Abstraction class for containing "ouput peaks" in Spanc, 
 * which are those peaks for which you have a channel and desire the
 * calibration to give you an excitation energy.
 *
 * @author  Dale Visser
 * @version 1.2
 * @since 1.0
 */
public final class OutputPeak implements Serializable,NukeUnits {

    private static final List PEAKS = Collections.synchronizedList(
    new ArrayList());
    private static final CalibrationFit FIT=CalibrationFit.getInstance();
    private SpancReaction reaction;
    private Energy ExProjectile = Energy.ZERO;
    private Energy ExResidual = Energy.ZERO;
    private UncertainNumber channel;
    private Length rho;
    
    /** 
     * Creates new OutputPeak.
     * 
     * @param sr the reaction for this peak
     * @param exp the excitation of the projectile in MeV
     * @param chan the centroid channel
     * @throws KinematicsException if the kinematics can't be
     * calculated
     * @throws StatisticsException if the fit fails
     * @throws MathException if the fit fails
     * @throws NuclearException if something is wrong with the input
     */
    public OutputPeak(SpancReaction sr, Energy exp, 
    UncertainNumber chan) throws KinematicsException,
    StatisticsException, MathException, NuclearException {
		setValues(sr,exp,chan);
        PEAKS.add(this);
    }
    
    /** 
     * Sets the internal values for this peak.
     * 
     * @param sr the reaction for this peak
     * @param exp the excitation of the projectile in MeV
     * @param chan the centroid channel
     * @throws KinematicsException if the kinematics can't be
     * calculated
     * @throws StatisticsException if the fit fails
     * @throws MathException if the fit fails
     * @throws NuclearException if something is wrong with the input
     */
    public void setValues(SpancReaction sr, Energy exp, 
    UncertainNumber chan) throws KinematicsException,
    MathException,StatisticsException, NuclearException {
        synchronized (this){
        	reaction=sr;
        	ExProjectile=exp;
        	channel=chan;
        }
        calculate();
    }    	
    
    private void calculate() throws KinematicsException, StatisticsException, 
    MathException, NuclearException {
    	synchronized (this) {
        	rho = FIT.getRho(channel);
        	ExResidual = reaction.getExResid(ExProjectile,rho);     
        }
    }
    
    /**
     * Remove the peak at the given index from the list of peaks.
     *
     * @param which the index of the peak to remove
     */
    static public void removePeak(int which){
        PEAKS.remove(which);
    }
    
    /**
     * Get the peak at the given index from the list of peaks.
     *
     * @param which the index of the peak to get
     * @return the OutputPeak at the given index
     */
    static public OutputPeak getPeak(int which){
    	return (OutputPeak)PEAKS.get(which);
    }
    
    /**
     * Clear the list of OutputPeak's.
     */
    static public void removeAllPeaks(){
        PEAKS.clear();
    }
    
    /**
     * Get an unmodifiable view of the list of peaks.
     *
     * @return an unmodifiable view of the list of peaks
     */
    static public Collection getPeakCollection(){
        return Collections.unmodifiableCollection(PEAKS);
    }
    
    /**
     * Add the given collection of peaks to the internal list.
     *
     * @param retrievedPeaks a collection of peaks loaded from a file
     */
    static public void refreshData(Collection retrievedPeaks)  {
        PEAKS.addAll(retrievedPeaks);
    }
    
    /**
     * Recalculate the peak positions.
     *
     * @throws KinematicsException if the kinematics can't be
     * calculated
     * @throws StatisticsException if the fit fails
     * @throws MathException if the fit fails
     * @throws NuclearException if something is wrong with the input
     */
    static public void recalculate() throws KinematicsException,
    StatisticsException, MathException, NuclearException {
        for (final Iterator iter = PEAKS.iterator(); iter.hasNext();) {
        	((OutputPeak)iter.next()).calculate();
        }
    }
    
    /**
     * Gets the flight radius of the given peak.
     *
     * @return the flight radius in cm
     * @param adjustError whether to adjust error for the chi^2 of
     * the fit
     */
    public Length getRho(boolean adjustError) {
        return Length.lengthOf(
        QuantityUtilities.scaleError(rho,Math.sqrt(Math.max(1,FIT.getReducedChiSq()))));
    }
    
    /**
     * Get the index of the reaction for this peak.
     *
     * @return the index referring to the reaction for this peak
     */
    public int getReactionIndex(){
        return SpancReaction.getReactionIndex(reaction);
    }
    
    /**
     * Get the excitation energy of the residual nucleus.
     *
     * @param adjustError whether to adjust the error bar using the 
     * chi^2 statistic
     * @return the excitation in MeV
     */
    public Energy getExResidual(boolean adjustError){
    	return Energy.energyOf(
    					QuantityUtilities.scaleError(
    							ExResidual,errorScaleFactor(adjustError)));
    }
    
    private double errorScaleFactor(boolean adjustError){
    	return adjustError ? Math.sqrt(Math.max(1,FIT.getReducedChiSq())) : 1;
    }
    
    /**
     * Get the excitation energy of the projectile.
     * 
     * @return the excitation in MeV
     */
    public Energy getExProjectile(){
        return ExProjectile;
    }
    
    /**
     * Get the channel number for this peak.
     *
     * @return the centroid channel
     */
    public UncertainNumber getChannel(){
        return channel;
    }
    
    /**
     * Get the String representation of this object.
     *
     * @return a verbose description of this OutputPeak
     */
    public String toString(){
    	final String s1="Output Peak for Reaction #";
    	final char cr='\n';
    	final String s2="Ex projectile = ";
    	final String s3="\n";
    	final String s4="Centroid Channel = ";
		final String s5="rho from calibration = ";
		final String s7="\n";
		final String s8="Ex[residual] from calibration = ";
		final String sa="\n";
		final double factorMeVtoKeV=1000.0;
        final StringBuffer rval = new StringBuffer(s1).append(
        getReactionIndex()).append(cr).append(s2).append(ExProjectile.toText(keV)).append(
        s3).append(s4).append(channel).append(cr).append(s5).append(getRho(
        true).toText(cm)).append(s7).append(
        s8).append(
        getExResidual(true).toText(keV)).append(sa);
        return rval.toString();
    }
        
}