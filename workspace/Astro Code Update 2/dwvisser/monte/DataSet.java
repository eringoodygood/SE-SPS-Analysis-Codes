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
package dwvisser.monte;

/**
 * Contains a set of numbers, can return set size, mean, and standard
 * deviation.
 * 
 * @author  Dale Visser
 * @version 1.2
 * @since 1.0 (9 March 2001)
 */
public class DataSet extends Object implements WeightingFunction {
    //java.util.Vector data;
    private double [] data = new double[100];
    private int size = 0;
    private double mean, sd;//mean and standard deviation
    private WeightingFunction weight;
    private boolean needToCalculateStats = true;
    
    /** Creates new DataSet */
    public DataSet(WeightingFunction wf) {
        //data = new java.util.Vector(); 
        weight=wf;
    }
    
    /**
     * Use standard non-biased weight.
     */ 
    public DataSet(){
        //data = new java.util.Vector(); 
        weight = this;
    }
    
    public void add(double x){
        data[size]=x; size++;
        if (size==data.length) {
        	double [] temp = new double[2*size];
        	System.arraycopy(data,0,temp,0,size);
        	data = temp;
        	System.gc();
        }
        needToCalculateStats=true;
    }
    
    private void calculateStats(){
        double s = 0.0; 
        double ss=0.0; 
        double sw=0.0;
        for (int i=0; i<size; i++){
            double w=weight(data[i]);
            double term=w*data[i];
            s += term;
            ss += term*term;
            sw += w;
        }
        mean = s/sw;
        sd = Math.sqrt((ss-s*s/sw)/(sw-1));
        needToCalculateStats=false;
    }
    
    public double [] getData(){
        double [] rval = new double[size];
        System.arraycopy(data,0,rval,0,size);
        return rval;
    }
    
    public double getMean(){
    	if (needToCalculateStats) calculateStats();
        return mean;
    }
    
    public double getSD(){
    	if (needToCalculateStats) calculateStats();
        return sd;
    }
    
    public int getSize(){
        return size;
    }
    
    public double getSEM(){
    	return getSD()/Math.sqrt(size);
    }
    
    public int [] getHistogram(double min, double max, double step){
        double realMin = min-step*0.5;
        int [] rval = new int[(int)Math.round((max-min)/step)];
        //double [] ndata = getData();
        for (int i=0; i<size; i++) {
            int bin=(int)Math.floor((data[i]-realMin)/step);
            if (bin >= 0 && bin < rval.length){
                rval[bin]++;
            }
        }
        return rval;
    }
    
    static public void main(String[] args){
        DataSet ds=new DataSet();
        ds.add(3);ds.add(4); ds.add(5);ds.add(6);
        System.out.println("Size = "+ds.getSize()+", Mean = "+ds.getMean()+", SD = "+ds.getSD());
    }

    public double weight(double value) {
        return 1.0;
    }
}