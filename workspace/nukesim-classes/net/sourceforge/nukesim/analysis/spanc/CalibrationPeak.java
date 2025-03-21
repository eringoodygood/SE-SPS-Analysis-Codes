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
 * CalibrationPeak.java
 *
 * Created on December 19, 2001, 11:12 AM
 */

package net.sourceforge.nukesim.analysis.spanc;
import jade.physics.Energy;
import jade.physics.Length;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sourceforge.nukesim.math.UncertainNumber;
import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;

/**
 * Representation of a fitted peak used for calibration of 
 * a spectrum.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version 1.1
 */
public class CalibrationPeak implements java.io.Serializable {
    
    private final static List peaks = Collections.synchronizedList(
    new ArrayList());
    private SpancReaction reaction;
    private Energy ExProjectile = Energy.ZERO;
    private Energy ExResidual = Energy.ZERO;
    private UncertainNumber channel;
    
    /** 
     * Creates new CalibrationPeak.
     *
     * @param re the reaction the peak is associated with
     * @param exp the projectile excitation in MeV
     * @param exr the residual excitation in MeV
     * @param ch the channel centroid
     */
    public CalibrationPeak(SpancReaction re, Energy exp,
    Energy exr, UncertainNumber ch) {
    	/* the following four lines avoid a call to the overridable setValues()
    	   in this constructor */
		setValues(re,exp,exr,ch);
        peaks.add(this);
    } 
        
    final void setValues(SpancReaction sr, Energy exp,
    Energy exr, UncertainNumber ch) {
    	synchronized (this){
        	reaction=sr;
        	ExProjectile=exp;
        	ExResidual=exr;
        	channel=ch;
        }
    }
    
    /**
     * Remove the specified peak.
     *
     * @param which index of the peak to remove
     */ 
    static public void removePeak(int which){
        peaks.remove(which);
    }
    
    /**
     * Get the specified peak.
     * 
     * @param which index of the desired peak
     * @return peak at the given index
     */
    static public CalibrationPeak getPeak(int which){
    	return (CalibrationPeak)peaks.get(which);
    }
    
    /**
     * Destroy all calibration peaks.
     */
    static public void removeAllPeaks(){
        peaks.clear();
    }
    
    /**
     * Get the collection of all peaks.
     * 
     * @return an unmodifiable view of the peaks collection
     */
    static public Collection getPeakCollection(){
        return Collections.unmodifiableCollection(peaks);
    }
    
    /**
     * Populate the peaks collection with the given collection.
     * 
     * @param retrievedPeaks a collection of peaks
     */
    static public void refreshData(Collection retrievedPeaks){
        peaks.addAll(retrievedPeaks);
    }
    
    /**
     * Get the radius in the spectrograph.
     * 
     * @return radius in cm in the spectrograph
     * @throws KinematicsException
     * @throws NuclearException
     */
    public Length getRho() throws KinematicsException, 
    NuclearException {
        return reaction.getRho(ExProjectile,ExResidual);
    }
    
    /**
     * Get the index of the reaction this peak is associated with.
     * 
     * @return the index to this peak's reaction
     */
    public int getReactionIndex(){
        return SpancReaction.getReactionIndex(reaction);
    }
    
    /**
     * Get the excitation of the residual for this peak.
     * 
     * @return excitation of the residual in MeV
     */
    public Energy getExResidual(){
        return ExResidual;
    }
    
    /**
     * Get the excitation of the projectile for this peak.
     * 
     * @return excitation of the projectile in MeV
     */
    public Energy getExProjectile(){
        return ExProjectile;
    }
    
    /**
     * Get the channel centroid of this peak.
     * 
     * @return the centroid channel
     */
    public UncertainNumber getChannel(){
        return channel;
    }
    
    /**
     * Get the radii in cm for all the calibration peaks.
     * 
     * @return the peak radii in cm
     */
    static public Length [] getY(){
        final Length [] rval = new Length[peaks.size()];
        for (int index=0; index<peaks.size(); index++){
            final CalibrationPeak cp = (CalibrationPeak)peaks.get(index);
            try {
                rval[index] = cp.getRho();
            } catch (KinematicsException ke) {
                System.err.println(ke);
			} catch (NuclearException ke) {
				System.err.println(ke);
			}
        }
        return rval;
    }
    
    /**
     * Get the channel centroids for all peaks.
     * 
     * @return all centroid channels
     */
    static public UncertainNumber [] getX(){
        final UncertainNumber [] rval = new UncertainNumber[peaks.size()];
        for (int index=0; index<peaks.size(); index++){
            final CalibrationPeak cp = (CalibrationPeak)peaks.get(index);
            rval[index] = cp.channel;
        }
        return rval;
    }
    
    /**
     * Return the string rep for this calibration peak.
     * 
     * @return a String representing this object
     * @see java.lang.Object#toString()
     */
    public String toString(){
        final StringBuffer rval = new StringBuffer("Calibration Peak ");
        
        try {
            rval.append(peaks.indexOf(this)).append(" from reaction #");
            rval.append(SpancReaction.getReactionIndex(
            reaction)).append('\n');
            rval.append("Ex of projectile: ").append(ExProjectile);
            rval.append(" MeV\n");
            rval.append("Ex of residual: ").append(ExResidual);
            rval.append(" MeV\n");
            rval.append("rho of projectile: ").append(getRho());
            rval.append(" cm\n");
            rval.append("Peak Centroid: ").append(channel).append('\n');
        } catch (KinematicsException ke) {
            System.err.println(ke);
		} catch (NuclearException ke) {
			System.err.println(ke);
		}
        return rval.toString();
    }
    
}
