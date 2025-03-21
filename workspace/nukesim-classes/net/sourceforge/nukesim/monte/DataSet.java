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
package net.sourceforge.nukesim.monte;

import jade.JADE;
import jade.physics.Quantity;
import jade.physics.models.RelativisticModel;
import jade.units.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.nukesim.math.QuantityUtilities;
import net.sourceforge.nukesim.nuclear.NukeUnits;

/**
 * Contains a set of numbers, can return set size, mean, and standard
 * deviation.
 * 
 * @author  Dale Visser
 * @version 1.2
 * @since 1.0 (9 March 2001)
 */
public class DataSet extends Object implements WeightingFunction {
    //private Quantity [] data = new Quantity [100];
	private List data = Collections.synchronizedList(new ArrayList());
    private Unit unit;
    //private int size = 0;
    private Quantity mean;//mean and standard deviation
    private WeightingFunction weight;
    private boolean needToCalculateStats = true;
    
    /** Creates new DataSet */
    public DataSet(Unit u, WeightingFunction wf) {
        weight=wf;
        unit=u;
    }
    
    /**
     * Use standard non-biased weight.
     */ 
    public DataSet(Unit u){
        weight = this;
        unit=u;
    }
    
    public synchronized void add(Quantity x){
    	data.add(x);
        needToCalculateStats=true;
    }
    
    private void calculateStats(){
        Quantity s = Quantity.valueOf(0,unit.pow(-1)); 
        Quantity sw= Quantity.valueOf(0,unit.pow(-2));
        final int size=data.size();
        for (final Iterator iterator=data.iterator(); iterator.hasNext(); ){
        	final Quantity datum=(Quantity)iterator.next();
            final Quantity w=weight(datum);
            final Quantity term=datum.multiply(w);
            s = s.add(term);
            sw = sw.add(w);
        }
        mean = s.divide(sw);
        Quantity sd=sw.inverse().root(2);
        mean = Quantity.valueOf(mean.doubleValue(unit),sd.doubleValue(unit),unit);
        needToCalculateStats=false;
    }
    
    public synchronized List getData(){
    	return Collections.unmodifiableList(data);
    }
    
    public synchronized Quantity getMean(){
    	if (needToCalculateStats) {
    		calculateStats();
    	}
    	return mean;
    }
        
    public synchronized int getSize(){
        return data.size();
    }
    
    public synchronized Quantity getSEM(){
    	final Quantity mu=getMean();
    	return mu.multiply(mu.getRelativeError());
    }
    
    public synchronized Quantity getSD(){
    	return getSEM().multiply(Math.sqrt(data.size()));
    }
    
    public int [] getHistogram(Quantity min, Quantity max, Quantity step){
        final Quantity realMin = min.subtract(step.multiply(0.5));
        final int [] rval = new int[(int)Math.round(max.subtract(min).divide(step).doubleValue())];
        for (final Iterator iterator=data.iterator(); iterator.hasNext(); ) {
        	final Quantity datum=(Quantity)iterator.next();
            final int bin=(int)Math.floor(datum.subtract(realMin).divide(step).doubleValue());
            if (bin >= 0 && bin < rval.length){
                rval[bin]++;
            }
        }
        return rval;
    }
    
    public String toString(){
    	final StringBuffer rval=new StringBuffer("Dataset size: ");
    	rval.append(getSize()).append(", Mean: ");
    	rval.append(QuantityUtilities.reportQuantity(getMean(),unit,true));
    	rval.append('\n');
    	return rval.toString();
    }
    
    static public void main(String[] args){
    	JADE.initialize();
    	RelativisticModel.select();
    	Unit u=NukeUnits.cm;
        DataSet ds=new DataSet(u);
        ds.add(Quantity.valueOf(3,u));
        ds.add(Quantity.valueOf(4,u)); 
        ds.add(Quantity.valueOf(5,u));
        ds.add(Quantity.valueOf(6,u)); 
        System.out.println(ds);
    }

    public Quantity weight(Quantity value) {
    	return Quantity.valueOf(1.0,unit.pow(-2));
    }
}