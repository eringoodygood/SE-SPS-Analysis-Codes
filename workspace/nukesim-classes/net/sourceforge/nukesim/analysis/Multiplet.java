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
 * Multiplet.java
 *
 * Created on February 14, 2001, 1:30 PM
 */

package net.sourceforge.nukesim.analysis;
import java.util.*;

/**
 *
 * @author  <a href="mailto:dale@visser.name">Dale Visser</a>
 */
public final class Multiplet extends Vector {

    /** Creates new Multiplet */
    public Multiplet() {
    }

    public Multiplet(Peak p){
        this();
        addPeak(p);
    }

    public Peak [] getAllPeaks(){
        Object [] arr=toArray();
        Peak [] rval = new Peak[arr.length];
        for (int p = 0; p<arr.length; p++){
            rval[p]=(Peak)arr[p];
        }
        return rval;
    }

    public double [] getAllCentroids(){
        Peak [] peaks=getAllPeaks();
        double [] centroids=new double[peaks.length];
        if (peaks.length >0){
            for (int i=0;i<peaks.length;i++){
                centroids[i]=peaks[i].getPosition();
            }
        }
        return centroids;
    }

    public double [] getCentroidErrors(){
        Peak [] peaks=getAllPeaks();
        double [] errors=new double[peaks.length];
        for (int i=0;i<peaks.length;i++){
            errors[i]=peaks[i].getPositionError();
        }
        return errors;
    }

    public void addMultiplet(Multiplet madd){
        addAll(madd);
    }

    /**
     * Removes all peaks with less than the specified area.
     *
     * @param min minimum area to retain the peak
     * @return number of peaks remaining
     */
    public int removeAreaLessThan(double min){
        Multiplet small=new Multiplet();
        for (int p=0; p<size(); p++){
            Peak peak=getPeak(p);
            if (peak.getArea() < min) small.addPeak(peak);
        }
        removeAll(small);//remove any peaks in small from this multiplet
        return size();
    }
    
    /**
     * Removes all peaks with greater than the specified area.
     *
     * @param max maximum area in order to retain the peak
     * @return number of peaks remaining
     */
    public int removeAreaGreaterThan(double max){
        Multiplet large=new Multiplet();
        for (int p=0; p<size(); p++){
            Peak peak=getPeak(p);
            if (peak.getArea() > max) large.addPeak(peak);
        }
        removeAll(large);
        return size();
    }

    /**
     * Returns peak nearest given centroid if distance is less than
     * the given tolerance.
     */
    public Peak getPeakNear(double centroid, double tolerance){
        double diff1= 2*tolerance;
        double diff2;
        Peak p1, p2;
        Iterator it = (new TreeSet(this)).iterator();
        if (it.hasNext()) {
            p1 = (Peak)(it.next());
            diff1=Math.abs(p1.getPosition()-centroid);
        } else {
            return null;
        }
        while (it.hasNext()){
            p2 = (Peak)(it.next());
            diff2 = Math.abs(p2.getPosition()-centroid);
            if (diff2 < diff1) {
                p1=p2;
                diff1=diff2;
            }
        }
        if (diff1 <= tolerance) {
            return p1;
        } else {
            return null;
        }
    }

    /**
     * Returns a new multiplet containing only the last n peaks of this multiplet.
     */
    public Multiplet lastPeaks(int n) {
        Multiplet rval=new Multiplet();
        for (int i=size()-n ; i < size(); i++) rval.addPeak(getPeak(i));
        return rval;
    }


    static Multiplet combineMultiplets(Multiplet [] marray){
        Multiplet rval = new Multiplet();
        for (int i=0; i<marray.length; i++){
            rval.addMultiplet(marray[i]);
        }
        return rval;
    }

    static Multiplet combineMultiplets(Multiplet m0, Multiplet m1){
        Multiplet [] temp = new Multiplet[2];
        temp[0]=m0;
        temp[1]=m1;
        return combineMultiplets(temp);
    }

    public void addPeak(Peak p) {
        if (p != null){
            addElement(p);
        }
    }
    public Peak getPeak(int index) {
        return (Peak)elementAt(index);
    }


}