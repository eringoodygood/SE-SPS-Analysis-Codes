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
 * InputSpecification.java
 *
 * Created on February 26, 2001, 11:15 AM
 */

package net.sourceforge.nukesim.analysis;
import java.io.*;
import jam.io.hdf.*;

/** Class for containing processing input specifications for
 * <CODE>YLSAcalibration</CODE>.
 * @author Dale Visser
 */
public final class InputSpecification extends Object {
    
    private String [][] spectraNames;
    private HDFIO [][] hdfFiles;
    
    private int numDet, numStrips;
    
    /**
     * Text file from which specification is taken.
     */
    private File file;
    private double sig;
    private double [][] width;

    /** Creates new InputSpecification.
     * The format of the input file is: <br>
     * <code>#detectors #stripPerDetector peakWidth significance<br>
     * #files<br>
     * filename#n #spectraInFile<br>
     * spectrumName#n<br>
     * etc.
     * </code>
     * @param inputSpecFile text file containing specifications
     */
    public InputSpecification(File inputSpecFile) {
        file=inputSpecFile;
        try {
            readInSpecs(inputSpecFile);
        } catch (HDFException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    void readInSpecs(File inputSpecFile) throws IOException, HDFException {
        StreamTokenizer st = new StreamTokenizer(new BufferedReader(new FileReader(inputSpecFile)));
        st.eolIsSignificant(false); //treat end of line as white space
        st.commentChar('#'); //ignore end of line comments after '#'
        st.wordChars('/','/'); //slash can be part of words
        st.wordChars('_','_'); //underscore can be part of words
        st.wordChars(':',':'); //colon can be part of words
        st.wordChars('\\','\\');//backslash can be part of words
        st.wordChars('~','~'); //tilde can be part of words
        int numFiles=readInteger(st);//read in number of files
        numDet=readInteger(st);
        numStrips=readInteger(st);
        width = new double[numDet][numStrips];
        spectraNames=new String[numDet][numStrips];
        hdfFiles=new HDFIO[numDet][numStrips];
        double defaultWidth = readFloat(st);
        for (int det=0; det<numDet; det++) {
            for (int strip=0; strip<numStrips; strip++) {
                width[det][strip]=defaultWidth;
            }
        }
        sig = readFloat(st);
        for (int i=0; i < numFiles; i++){
            String temp=readString(st);
            HDFIO currentFile=new HDFIO(new File(temp));
            int numSpectra = readInteger(st);
            boolean alreadyReadDet = false;
            int det=0;
            for (int sp=0; sp<numSpectra; sp++){
                if (!alreadyReadDet){
                    det = readInteger(st);
                }
                int str = readInteger(st);
                hdfFiles[det][str] = currentFile;
                spectraNames[det][str] = readString(st);
                st.nextToken();
                if (st.ttype == StreamTokenizer.TT_WORD){//assumed to be 'width'
                    width[det][str]=readFloat(st);
                    alreadyReadDet = false;
                } else {//we actually have next det #
                    det = (int)st.nval;
                    alreadyReadDet = true;
                }
            }
        }
    }
    
    private int readInteger(StreamTokenizer st) throws IOException {
        st.nextToken();
        if (st.ttype != StreamTokenizer.TT_NUMBER) throw new IOException(this.getClass().getName()+
                "[\""+shortString()+"\"].readInteger(): Wrong token type: "+st.ttype); 
        return (int)st.nval;
    }

    private double readFloat(StreamTokenizer st) throws IOException {
        st.nextToken();
        if (st.ttype != StreamTokenizer.TT_NUMBER) throw new IOException(this.getClass().getName()+
                "[\""+shortString()+"\"].readFloat(): Wrong token type: "+st.ttype); 
        return st.nval;
    }

    private String readString(StreamTokenizer st) throws IOException {
        st.nextToken();
        if (st.ttype != StreamTokenizer.TT_WORD) throw new IOException(this.getClass().getName()+
                "[\""+shortString()+"\"].readString(): Wrong token type: "+st.ttype); 
        return st.sval;
    }

    /** Returns the specified peak width for the <CODE>PeakFinder</CODE>
     * to use.
     * @return width in channels of peaks for peak finder to use
     */
    public double getWidth(int det, int strip){
       return width[det][strip];
    }

    /** Returns the confidence lever for the <CODE>PeakFinder</CODE> to use.
     * @return significance parameter
     */
    public double getSignificance(){
        return sig;
    }
    
    /** Retrieves the spectrum for the given detector and strip from the
     * appropriate HDF file.
     * @param detector detector of interest
     * @param strip strip of interest in the detector
     * @throws SpecNotAvailableException if user never gave an input specification for this detector
     * and strip
     * @return histogram
     */
    public int [] getSpectrum(int detector, int strip) throws SpecNotAvailableException {
        if (hdfFiles[detector][strip]==null) throw new SpecNotAvailableException(detector,strip);
        HDFIO io = hdfFiles[detector][strip];
        try {
            return io.readIntegerSpectrum(getName(detector,strip));
        } catch (IOException e) {
            System.err.println(e);
            return null;
        } catch (HDFException e) {
            System.err.println(e);
            return null;
        }        
    }
    
    /** Returns the file reference for the requested detector and strip.
     * @param det which detector
     * @param str which strip in the detector
     * @return file to look in
     */
    public File getFile(int det, int str){
        return hdfFiles[det][str].getInputFile();
    }
    
    /** Returns the name of the spectrum in the HDF file for the given
     * detector and strip.
     * @param det which detector
     * @param str which strip in the detector
     * @return spectrum name
     */
    public String getName(int det, int str){
        return spectraNames[det][str];
    }
    
    public int getNumberOfDetectors(){
        return numDet;
    }
    
    public int getStripsPerDetector(){
        return numStrips;
    }
    
    public boolean dataExists(int det, int strip){
        return getName(det,strip)!=null;
    }
    
    public String toString(){
        String rval = "Input Specification from File: "+file+"\n";
        rval += "Significance parameter = "+getSignificance()+"\n";
        for (int d=0; d<numDet; d++){
            for(int s=0; s<numStrips; s++){
                if (!dataExists(d,s)) {
                    rval += "  Det "+d+", Strip "+s+" is not specified in file.\n";
                } else {
                    rval += "  Det "+d+" Strip "+s+" in "+getFile(d,s)+": "+getName(d,s)+"\n";
                }
            }
        }
        return rval;
    }
    
    private String shortString(){
    	String rval=file.toString();
    	return rval;
    }
    
    public static void main (String [] args) {
        File f = new File("/home/jam/pulserData.inp");
        InputSpecification is = new InputSpecification(f);
        System.out.println(is);
    }

}