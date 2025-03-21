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
package dwvisser.analysis;
import java.io.*;
import dwvisser.math.*;
import dwvisser.statistics.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** 
 * Program to take an input specification file and .hdf files with calibration spectra, and
 * run auto peak-fitting to give energy calibrations.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public class BatchCalibration extends Object {

    private UncertainNumber [][] pulserOffsets;
    private UncertainNumber [][] rawGains;
    private UncertainNumber [][] newGains;
    private UncertainNumber [][] timeGains;
    private UncertainNumber [][] timeOffsets;

    private ArrayCalibration arrayCalibration;

    /** When given the standard deviation for a gaussian peak, multiplying by this
     * gives the full width at half maximum height.
     */
    static final double SIGMA_TO_FWHM = 2.354;

    /** Maximum separation in sigma between peaks to count them as being in the same
     * multiplet.
     */
    static final double MAX_SEP=1.3;

    /** 
     * Creates new YLSAcalibration.
     * 
     * @param batch input text file formatted as specified above.
     * @param arrayCalFile database of calibration info
     */
    public BatchCalibration(File batch, File arrayCalFile) {
        retrieveDataBase(arrayCalFile);
        processFile(batch);
        saveProgress(arrayCalFile);
    }

    private void processFile(File batch){
        try{
            LineNumberReader lr=new LineNumberReader(new FileReader(batch));
            lr.readLine(); //skip first line
            StreamTokenizer st = new StreamTokenizer(new BufferedReader(lr));
            st.eolIsSignificant(false); //treat end of line as white space
            st.commentChar('#'); //ignore end of line comments after '#'
            st.wordChars('/','/'); //slash can be part of words
            st.wordChars('_','_'); //underscore can be part of words
            st.wordChars(':',':'); //colon can be part of words
            st.wordChars('\\','\\');//backslash can be part of words
            st.wordChars('~','~'); //tilde can be part of words
            do {
                st.nextToken();
                if (st.ttype != StreamTokenizer.TT_EOF){
                    String command=readString(st);
                    processCommand(command, st);
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void retrieveDataBase(File data){
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(data));
            arrayCalibration = (ArrayCalibration)ois.readObject();
            ois.close();
        } catch (FileNotFoundException fnf) {
            System.out.println("Specified file, '"+data+"' does not exist. "+
            "A new database is being created.");
            arrayCalibration = new ArrayCalibration();
        } catch (IOException e) {
            System.err.println(getClass().getName()+".retrieveDataBase(): "+e);
        } catch (ClassNotFoundException e) {
            System.err.println(getClass().getName()+".retrieveDataBase(): "+e);
        }
    }

    private void outputCalibration(File textOut) {
        try {
            FileWriter fw = new FileWriter(textOut);
            System.out.println("Outputing calibration to: "+textOut);
            fw.write(arrayCalibration.toString());
            fw.close();
        } catch (IOException e) {
            System.err.println(getClass().getName()+".textOut(): "+e);
        }
    }

    private void saveProgress(File out){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(out));
            oos.writeObject(arrayCalibration);
            oos.close();
        } catch (IOException e) {
            System.err.println(getClass().getName()+".saveProgress(): "+e);
        }
    }

    /** Process a command in a command text file.
     * @param com the command just read to be processed
     * @param st tokenizer needed to grab parameters for command
     * @throws IOException if something goes wrong
     */
    private void processCommand(String com, StreamTokenizer st) throws IOException {
        System.out.println(com);
        if (com.equals("PULSER")) {
            st.nextToken(); String temp1=readString(st);//input spec file
            st.nextToken(); String temp2=readString(st);//output peaks file
            st.nextToken(); String temp3=readString(st);//output offsets file
            processPulserData(new File(temp1), temp2, temp3);
        } else if (com.equals("THORIUM")) {
            st.nextToken(); String temp1=readString(st);
            st.nextToken(); String temp2=readString(st);
            st.nextToken(); String temp3=readString(st);
            processThoriumData(new File(temp1), temp2, temp3);
        } else if (com.equals("ENERGYCAL")) {
            st.nextToken(); String temp1=readString(st);
            makeEnergyCalibration(new File(temp1));
        } else if (com.equals("TIMECAL")) {
            st.nextToken(); String temp1=readString(st);
            makeTimeCalibration(new File(temp1));
        } else if (com.equals("SELFTIME")) {
            st.nextToken(); File temp1=new File(readString(st));
            st.nextToken(); int numTimes=readInteger(st);
            double [] temp = new double[numTimes];
            for (int i=0; i<numTimes; i++){
                st.nextToken();
                temp[i]=readDouble(st);
            }
            processSelfTime(temp1,temp);
        } else if (com.equals("WRITEPEAKS")) {
            st.nextToken(); File temp1=new File(readString(st));
            st.nextToken(); File temp2=new File(readString(st));
            writeAllPeaks(temp1,temp2);
        } else if (com.equals("AM241")){
            st.nextToken(); File temp1=new File(readString(st));
            process241AmData(temp1);
        } else if (com.equals("OUTPUTCAL")) {
            st.nextToken(); File temp1=new File(readString(st));
            outputCalibration(temp1);
        } else if (com.equals("ADDENERGYCAL")) {
        	st.nextToken(); int numStrips=readInteger(st);
        	int [] det = new int[numStrips];
        	int [] strip = new int[numStrips];
        	double [] offset = new double[numStrips];
        	double [] gain = new double[numStrips];
        	for (int i=0; i<numStrips; i++){
        		st.nextToken(); det[i]=readInteger(st);
        		st.nextToken(); strip[i]=readInteger(st);
        		st.nextToken(); offset[i]=readDouble(st);
        		st.nextToken(); gain[i]=readDouble(st);
        	}
        	addEnergyCalibrations(det,strip,offset,gain);
        } else {
            System.out.println(getClass().getName()+".processCommand: unknown command \""+com+"\"");
        }
    }

    private int readInteger(StreamTokenizer st) throws IOException {
        //st.nextToken();
        if (st.ttype != StreamTokenizer.TT_NUMBER) throw new IOException(this.getClass().getName()+
        ".readInteger(): Wrong token type: "+st.ttype);
        return (int)st.nval;
    }

    private double readDouble(StreamTokenizer st) throws IOException {
        //st.nextToken();
        if (st.ttype != StreamTokenizer.TT_NUMBER) throw new IOException(this.getClass().getName()+
        ".readInteger(): Wrong token type: "+st.ttype);
        return st.nval;
    }

    private String readString(StreamTokenizer st) throws IOException {
        //st.nextToken();
        if (st.ttype != StreamTokenizer.TT_WORD) throw new IOException(this.getClass().getName()+
        ".readString(): Wrong token type: "+st.ttype);
        System.out.println(st.sval);
        return st.sval;
    }

    /** Outputs to a text file energy calibration with channels per MeV, and
     * offsets/gains.  The best procedure is to subtract these offsets, then
     * multiply by the gain, and you obtain spectra with the specified
     * channels per MeV.
     * @param out reference of file to create
     */
    private void makeEnergyCalibration(File out){
        UncertainNumber minGain=new UncertainNumber(1000000.0,0.0);
        int minD=999, minS=999;
        System.out.println("Making Calibration.");
        for (int d=0; d<pulserOffsets.length; d++){
            for (int s=0; s<pulserOffsets[0].length; s++) {
                UncertainNumber temp=rawGains[d][s];
                if (temp != null && temp.value < minGain.value) {
                    minGain=temp;
                    minD=d;
                    minS=s;
                }
            }
        }
        if (minGain.value > 0.0) {//actually found something
            System.out.println("Found Detector "+minD+", Strip "+minS+" had minimum gain.");
            try {
                FileWriter fout=new FileWriter(out);
                fout.write("Final spectra will have "+rawGains[minD][minS]+" channels per MeV.\n");
                fout.write("Det\tStr\tOff\tGain\n");
                for (int d=0; d<pulserOffsets.length; d++){
                    for (int s=0; s<pulserOffsets[0].length; s++) {
                        if (pulserOffsets[d][s] != null && rawGains[d][s] != null){//good pulser and thorium data
                            UncertainNumber gainFac = rawGains[minD][minS].divide(rawGains[d][s]);
                            fout.write(d+"\t"+s+"\t"+pulserOffsets[d][s].value+"\t"+gainFac.value+"\n");
                        }
                    }
                }
                fout.flush();fout.close();
            } catch (IOException ioe) {
                System.err.println(getClass().getName()+
                ".makeCalibration() : "+ioe);
            }
        }
    }

    /** Outputs to a text file energy calibration with channels per nsec, and
     * offsets/gains.  The best procedure is to subtract these offsets, then
     * multiply by the gain, and you obtain spectra with the specified
     * channels per nsec.
     * @param out reference to file to be created
     */
    private void makeTimeCalibration(File out){
        UncertainNumber minGain=new UncertainNumber(1000000.0,0.0);
        int minD=999, minS=999;
        System.out.println("Making Time Calibration.");
        for (int d=0; d<timeGains.length; d++){
            for (int s=0; s<timeGains[0].length; s++) {
                UncertainNumber temp=timeGains[d][s];
                if (temp != null && temp.value < minGain.value) {
                    minGain=temp;
                    minD=d;
                    minS=s;
                }
            }
        }
        if (minGain.value > 0.0) {//actually found something
            System.out.println("Found Detector "+minD+", Strip "+minS+
            " had minimum gain.");
            double ast=timeOffsets[minD][minS].value;
            double bst=timeGains[minD][minS].value;
            try {
                FileWriter fout=new FileWriter(out);
                fout.write("Final spectra will have "+timeGains[minD][minS]+
                " channels per nsec.\n");
                fout.write("Det\tStr\tOff\tGain\n");
                for (int d=0; d<timeOffsets.length; d++){
                    for (int s=0; s<timeOffsets[0].length; s++) {
                        if (timeGains[d][s] != null && timeOffsets[d][s] !=
                        null){//good time data
                            double ai=timeOffsets[d][s].value;
                            double bi=timeGains[d][s].value;
                            double bf=bst/bi;
                            double af=ast-bf*ai;
                            fout.write(d+"\t"+s+"\t"+af+"\t"+bf+"\n");
                        }
                    }
                }
                fout.flush();fout.close();
            } catch (IOException ioe) {
                System.err.println(getClass().getName()+".makeCalibration() : "+ioe);
            }
        }
    }

    /** Take the input specification file for the pulser data, fit the
     * top lines, and fit them to step voltages.  Output the results,
     * including zero-offset values.
     * @param inputSpec file to grab pulser input specs from
     * @param peaks name of text file to create for peak lists
     * @param offsets name of text file to create for offsets list
     * @throws IOException if something goes wrong accessing files
     */
    private void processPulserData(File inputSpec, String peaks, String offsets)
    throws IOException {
        //process file and make calls to AutoPeakFit
        int [] spectrum;
        try {
            System.out.println("Processing pulser data.");
            InputSpecification is=new InputSpecification(inputSpec);
            FileWriter fpeaks=new FileWriter(peaks);
            FileWriter fthresh=new FileWriter(offsets);
            int ndet=is.getNumberOfDetectors();
            int nstr=is.getStripsPerDetector();
            pulserOffsets = new UncertainNumber[ndet][nstr];
            double sensitivity=is.getSignificance();
            for (int d=0; d<ndet; d++){
                for (int s=0; s<nstr; s++){
                    if (is.dataExists(d,s)){
                        double width=is.getWidth(d,s);
                        spectrum = is.getSpectrum(d,s);
                        PeakFinder pf=new PeakFinder(is.getName(d,s),
                        spectrum,sensitivity,width);
                        Multiplet [] results = pf.multiplets;
                        //printResults(spectrum, spectrumName, results);
                        Multiplet [] fits = doFits(spectrum, results, width);//temporary 2.8 and 3 are to copy above
                        //printResults(spectrum, spectrumName, fits);
                        Multiplet mfinal = Multiplet.combineMultiplets(fits);
                        mfinal.removeAreaLessThan(1000.0);//remove spurious small peaks
                        mfinal.removeAreaGreaterThan(4000.0);//remove noise-triggered stuff
                        double [] cent = mfinal.getAllCentroids();
                        double [] cerr = mfinal.getCentroidErrors();
                        fpeaks.write(d+"\t"+s+"\t"+cent.length+"\n");
                        if (cent.length > 2){
                            double [] x = new double[cent.length];
                            for (int p=0; p<x.length; p++) {
                                x[p] = p+1.0;
                            }
                            LinearFitErrY lf = new LinearFitErrY(x,cent,cerr);
                            for (int p=0; p<cent.length; p++) {
                                fpeaks.write(cent[p]+"\t"+cerr[p]+"\t"+
                                lf.residual[p]+"\n");
                            }
                            double chi2=lf.getReducedChiSq();
                            if (chi2 <= 1.0) {
                                System.out.println("Reduced chiSq = "+chi2);
                                fthresh.write(d+"\t"+s+"\t"+lf.getOffset()+"\t"+lf.getOffsetErr()+"\n");
                                pulserOffsets[d][s]=new UncertainNumber(lf.getOffset(),lf.getOffsetErr());
                                System.out.println("Threshold = "+lf.getOffset()+" +/- "+lf.getOffsetErr());
                            } else {
                                System.out.println("Reduced chiSq = "+chi2+" > 1.0 so error is adjusted.");
                                double err = lf.getOffsetErr()*Math.sqrt(chi2);
                                fthresh.write(d+"\t"+s+"\t"+lf.getOffset()+"\t"+err+"\n");
                                pulserOffsets[d][s]=new UncertainNumber(lf.getOffset(), err);
                                System.out.println("Threshold = "+lf.getOffset()+" +/- "+err);
                            }
                            arrayCalibration.setEnergyOffset(d,s,lf.getOffset());
                        }
                    }
                }
            }
            fpeaks.flush(); fpeaks.close();
            fthresh.flush(); fpeaks.close();
        } catch (FitException e) {
            System.err.println(e);
        } catch (MathException e) {
            System.err.println(e);
        } catch (SpecNotAvailableException e) {
            System.err.println(e);
        }
    }

    /** Processes spectra, fitting all peaks, and writing out there parameters
     * to an output file.
     * @param spec text file containing input specifications
     * @param out text file to output peak parameters to
     */
    private void writeAllPeaks(File spec, File out) {
        System.out.println("Writing All Peaks");
        try {
            InputSpecification is=new InputSpecification(spec);
            FileWriter fout=new FileWriter(out);
            int ndet=is.getNumberOfDetectors();
            int nstr=is.getStripsPerDetector();
            double sensitivity=is.getSignificance();
            for (int d=0; d<ndet; d++){
                for (int s=0; s<nstr; s++){
                    if (is.dataExists(d,s)){
                        double width=is.getWidth(d,s);
                        int [] spectrum = is.getSpectrum(d,s);
                        String name = is.getName(d,s);
                        PeakFinder pf=new PeakFinder(name,spectrum,
                        sensitivity, width);
                        Multiplet [] results = pf.multiplets;
                        Multiplet [] fits = doFits(spectrum, results, width);//temporary 2.8 and 3 are to copy above
                        Multiplet mfinal = Multiplet.combineMultiplets(fits);
                        fout.write(is.getName(d,s));
                        for (int n=0; n<mfinal.size(); n++){
                            fout.write("\t"+mfinal.getPeak(n).getPosition());
                        }
                        fout.write("\n");
                    }
                }
            }
            fout.flush(); fout.close();
        } catch (FitException e) {
            System.err.println(getClass().getName()+".writeAllPeaks(): "+e);
        } catch (IOException e) {
            System.err.println(getClass().getName()+".writeAllPeaks(): "+e);
        } catch (SpecNotAvailableException e) {
            System.err.println(getClass().getName()+".writeAllPeaks(): "+e);
        }
    }
    /** Processes spectra with self-time pulser peaks to produce
     * channel-to-time linear calibrations.
     * @param spec text file containing input specifications
     * @param outPeaks text file to output peaks to
     * @param numTimePeaks number of time peaks to expect
     */
    private void processSelfTime(File spec, double [] times) {
        //double [] times = {-155.0,-63.0, -48.0, -32.0, 0.0};
        System.out.println("Processing self-time TDC data");
        try {
            InputSpecification is=new InputSpecification(spec);
            //FileWriter foutpeaks=new FileWriter(outPeaks);
            int ndet=is.getNumberOfDetectors();
            int nstr=is.getStripsPerDetector();
            timeGains = new UncertainNumber[ndet][nstr];
            timeOffsets = new UncertainNumber[ndet][nstr];
            double sensitivity=is.getSignificance();
            for (int d=0; d<ndet; d++){
                for (int s=0; s<nstr; s++){
                    if (is.dataExists(d,s)){
                        double width=is.getWidth(d,s);
                        int [] spectrum = is.getSpectrum(d,s);
                        String name = is.getName(d,s);
                        PeakFinder pf=new PeakFinder(name,spectrum,
                        sensitivity, width);
                        Multiplet [] results = pf.multiplets;
                        Multiplet [] fits = doFits(spectrum, results, width);//temporary 2.8 and 3 are to copy above
                        Multiplet mfinal = Multiplet.combineMultiplets(fits);
                        if (mfinal.size() != times.length) {
                            System.out.println("Detector "+d+", Strip "+s+" had "+mfinal.size()+" peaks!");
                        } else {
                            LinearFitErrY lf=new LinearFitErrY(times,mfinal.getAllCentroids(),
                            mfinal.getCentroidErrors());
                            arrayCalibration.setTimeOffset(d,s,lf.getOffset());
                            arrayCalibration.setTimeGain(d,s,lf.getSlope());
                        }
                    }
                }
            }
            arrayCalibration.setTimeGainFactors();
        } catch (FitException e) {
            System.err.println(getClass().getName()+".processSelfTime(): "+e);
        } catch (MathException e) {
            System.err.println(getClass().getName()+".processSelfTime(): "+e);
        } catch (SpecNotAvailableException e) {
            System.err.println(getClass().getName()+".processSelfTime(): "+e);
        }
    }

    /** Take the input specification file for the thorium-228 data, fit the top lines, and fit
     * them to deposited energy.  Output the results.
     * @param inputSpec file to grab pulser input specs from
     * @param peaks name of text file to create for peak lists
     * @param fitsFile text file containing offsets and gains to map channel to MeV deposited
     * @throws IOException if a problem occurs during file access
     */
    private void processThoriumData(File inputSpec, String peaks, String fitsFile) throws IOException {
        //process file and make calls to AutoPeakFit
        int [] spectrum;
        double Sn;//nuclear stopping term
        double depth=0.2;//depth of Al dead layer in um
        double ZPOW=2.0/3.0;//power to take z's to in nuclear loss formula
        double UNIT_FAC=0.006023;//factor to multiply by to get MeV/um
    double [] T = {5.6832, 6.2885, 6.7773, 8.7850}; //initial energy of alpha in MeV
    double [] A = {2.5, 0.625, 45.7, 0.1, 4.359};//coefficients from Ziegler v.4 p.67 He stopping in Al
        double [] S = new double[T.length]; //total stopping in 2 um Al layer
        double [] eDep = new double[T.length]; //deposited energy in Si to be fit to
        double mAl = 26.981538;//Mass of 27Al in amu
        int zAl = 13; int zHe=2; //nuclear charges
        double mHe = 2.603250;//Mass of 4He in amu
        double nuclFac = 32.53 * mAl / (zAl*zHe*(mAl+mHe)*Math.sqrt(Math.pow(zAl,ZPOW)+Math.pow(zHe,ZPOW)));
        for (int i=0; i<T.length; i++) {
            double Slo = A[0]*Math.pow(T[i]*1000.0,A[1]);//low energy term
            double Shi = A[2]/T[i]*Math.log(1.0+A[3]/T[i]+A[4]*T[i]);//high energy term
            S[i] = UNIT_FAC/(1.0/Slo + 1.0/Shi);//electronic stopping
            double eps=nuclFac*T[i]*1000.0; //Reduced ion energy as per p.66 Ziegler
            if (eps < 0.01) {
                Sn = UNIT_FAC* 1.593 * Math.sqrt(eps);
            } else if (eps > 10.0) {
                Sn = UNIT_FAC * Math.log(0.47*eps) / (2.0 * eps);
            } else {//between 0.01 and 10.0
                Sn = UNIT_FAC * 1.7 * Math.sqrt(eps)*(Math.log(eps+1.0)/(1.0+6.8*eps+Math.sqrt(eps)));
            }
            S[i] += Sn;
            //x[i] = T[i]-S[i]/cos(getInci
        }
        try {
            System.out.println("Processing thorium-228 data-file: "+peaks);
            InputSpecification is=new InputSpecification(inputSpec);
            FileWriter fpeaks=new FileWriter(peaks);
            FileWriter ffits=new FileWriter(fitsFile);
            int ndet=is.getNumberOfDetectors();
            int nstr=is.getStripsPerDetector();
            rawGains=new UncertainNumber[ndet][nstr];
            newGains=new UncertainNumber[ndet][nstr];
            double sensitivity=is.getSignificance();
            for (int d=0; d<ndet; d++){
                for (int s=0; s<nstr; s++){
                    if (is.dataExists(d,s)){
                    	System.out.print("Detector "+d+", Strip "+s+": ");
                        double width=is.getWidth(d,s);
                        spectrum = is.getSpectrum(d,s);
                        String name=is.getName(d,s);
                        PeakFinder pf=new PeakFinder(name, spectrum,
                        sensitivity, width);
                        Multiplet [] results = pf.multiplets;
                        Multiplet [] fits = doFits(spectrum, results, width);//temporary 2.8 and 3 are to copy above
                        Multiplet mfinal = Multiplet.combineMultiplets(fits);
                        int numRemaining = mfinal.removeAreaLessThan(65.0);//minimum area requirement of top 3 peaks
                        if (numRemaining >= 3) {//otherwise not usable
                            for (int i=0; i<T.length; i++){
                                eDep[i]=T[i]-S[i]*depth*
                                arrayCalibration.getIncidence(s);
                            }
                            Multiplet mtemp = mfinal.lastPeaks(3);//just get last 3 peaks
                            double [] cent = mtemp.getAllCentroids();
                            double [] cerr = mtemp.getCentroidErrors();
                            //Produce an array of just the last 3 peaks.
                            double [] tempE = new double[eDep.length-1];
                            System.arraycopy(eDep,1,tempE,0,tempE.length);
                            LinearFitErrY lf = new LinearFitErrY(tempE,cent,cerr);
                            Peak ptemp = mfinal.getPeakNear(lf.calculateY(eDep[0]),5.0);
                            if (ptemp != null) {//if null not really a good thorium spectrum
                                mtemp=Multiplet.combineMultiplets(new Multiplet(ptemp),mtemp);
                                cent = mtemp.getAllCentroids();
                                cerr = mtemp.getCentroidErrors();
                                fpeaks.write(d+"\t"+s+"\t"+cent.length+"\n");
                                lf = new LinearFitErrY(eDep,cent,cerr);
                                double chi2=lf.getReducedChiSq();
                                for (int p=0;p<cent.length;p++){//output peaks with fit residuals
                                    fpeaks.write(cent[p]+"\t"+cerr[p]+"\t"+lf.residual[p]+"\n");
                                }
                                if (chi2 <= 1.0) {
                                    System.out.println("Reduced chiSq = "+chi2);
                                    ffits.write(d+"\t"+s+"\t"+lf.getOffset()+"\t"+lf.getOffsetErr()+"\t"+
                                    lf.getSlope()+"\t"+lf.getSlopeErr()+"\n");
                                    rawGains[d][s]=new UncertainNumber(lf.getSlope(),lf.getSlopeErr());
                                    //System.out.println("Threshold = "+lf.getOffset()+" +/- "+lf.getOffsetErr());
                                } else {
                                    System.out.println("Reduced chiSq = "+chi2+" > 1.0 so error is adjusted.");
                                    double oerr = lf.getOffsetErr()*Math.sqrt(chi2);
                                    double slerr = lf.getSlopeErr()*Math.sqrt(chi2);
                                    ffits.write(d+"\t"+s+"\t"+lf.getOffset()+"\t"+oerr+"\t"+
                                    lf.getSlope()+"\t"+slerr+"\n");
                                    rawGains[d][s]= new UncertainNumber(lf.getSlope(), slerr);
                                    //System.out.println("Threshold = "+lf.getOffset()+" +/- "+err);
                                }
                                arrayCalibration.setEnergyGain(d,s,lf.getSlope());
                                arrayCalibration.setEnergyOffset(d,s,lf.getOffset());
                            } else {
                            	System.out.println("couldn't fit.");
                            }
                        }
                    }
                }
            }
            arrayCalibration.setEnergyGainFactors();
            fpeaks.flush(); fpeaks.close();
            ffits.flush(); ffits.close();
        } catch (FitException e) {
            System.err.println(e);
        } catch (MathException e) {
            System.err.println(e);
        } catch (SpecNotAvailableException e) {
            System.err.println(e);
        }
    }
    
    private void addEnergyCalibrations(int [] det,int [] strip,double [] offset,double [] gain){
    	for (int i=0; i<det.length; i++){
    		arrayCalibration.setEnergyOffset(det[i],strip[i],offset[i]);
    		arrayCalibration.setEnergyGain(det[i],strip[i],gain[i]);
    	}
    	arrayCalibration.setEnergyGainFactors();
    }

    
    /** Take the input specification file for the thorium-228 data, fit the top lines, and fit
     * them to deposited energy.  Output the results.
     * @param inputSpec file to grab pulser input specs from
     * @param peaks name of text file to create for peak lists
     * @param fitsFile text file containing offsets and gains to map channel to MeV deposited
     * @throws IOException if a problem occurs during file access
     */
    private void process241AmData(File inputSpec) throws IOException {
        //process file and make calls to AutoPeakFit
        int [] spectrum;
        double Sn;//nuclear stopping term
        double depth=0.2;//depth of Al dead layer in um
        double ZPOW=2.0/3.0;//power to take z's to in nuclear loss formula
        double UNIT_FAC=0.006023;//factor to multiply by to get MeV/um
    double [] T = {5.48570, 5.44301,5.38840,5.54425,5.51161}; //initial energy of alpha in MeV
    double [] branch = {0.852, 0.128, 0.014, 0.0034, 0.0020};
        double branchSum =0.0; for (int i=0;i<branch.length;i++) branchSum+=branch[i];
    double [] A = {2.5, 0.625, 45.7, 0.1, 4.359};//coefficients from Ziegler v.4 p.67 He stopping in Al
        double [] S = new double[T.length]; //total stopping in 2 um Al layer
        double [] eDep = new double[T.length]; //deposited energy in Si to be fit to
        double mAl = 26.981538;//Mass of 27Al in amu
        int zAl = 13; int zHe=2; //nuclear charges
        double mHe = 2.603250;//Mass of 4He in amu
        double nuclFac = 32.53 * mAl / (zAl*zHe*(mAl+mHe)*Math.sqrt(Math.pow(zAl,ZPOW)+Math.pow(zHe,ZPOW)));
        for (int i=0; i<T.length; i++) {
            double Slo = A[0]*Math.pow(T[i]*1000.0,A[1]);//low energy term
            double Shi = A[2]/T[i]*Math.log(1.0+A[3]/T[i]+A[4]*T[i]);//high energy term
            S[i] = UNIT_FAC/(1.0/Slo + 1.0/Shi);//electronic stopping
            double eps=nuclFac*T[i]*1000.0; //Reduced ion energy as per p.66 Ziegler
            if (eps < 0.01) {
                Sn = UNIT_FAC* 1.593 * Math.sqrt(eps);
            } else if (eps > 10.0) {
                Sn = UNIT_FAC * Math.log(0.47*eps) / (2.0 * eps);
            } else {//between 0.01 and 10.0
                Sn = UNIT_FAC * 1.7 * Math.sqrt(eps)*(Math.log(eps+1.0)/(1.0+6.8*eps+Math.sqrt(eps)));
            }
            S[i] += Sn;
            //x[i] = T[i]-S[i]/cos(getInci
        }
        try {
            System.out.println("Processing Am-241 data.");
            InputSpecification is=new InputSpecification(inputSpec);
            int ndet=is.getNumberOfDetectors();
            int nstr=is.getStripsPerDetector();
            rawGains=new UncertainNumber[ndet][nstr];
            newGains=new UncertainNumber[ndet][nstr];
            double sensitivity=is.getSignificance();
            for (int d=0; d<ndet; d++){
                for (int s=0; s<nstr; s++){
                    if (is.dataExists(d,s)){
                        double width=is.getWidth(d,s);
                        spectrum = is.getSpectrum(d,s);
                        String name=is.getName(d,s);
                        PeakFinder pf=new PeakFinder(name,
                        spectrum, sensitivity, width);
                        Multiplet [] results = pf.multiplets;
                        Multiplet mfinal = Multiplet.combineMultiplets(results);
                        mfinal = mfinal.lastPeaks(1);
                        int position=(int)Math.round(
                        mfinal.getPeak(0).getPosition());
                        double posnAvg=0.0; double norm=0.0;
                        for (int i=position-100; i<position+100; i++){
                            norm += spectrum[i];
                            posnAvg += spectrum[i]*i;
                        }
                        posnAvg /= norm;
                        double eDepAvg = 0.0;
                        for (int i=0; i<T.length; i++){
                            eDep[i]=T[i]-S[i]*depth*
                            arrayCalibration.getIncidence(s);
                            eDepAvg += branch[i]*eDep[i];
                        }
                        eDepAvg /= branchSum;//energy to calibrate to
                        double channelsPerMeV =(posnAvg -
                        arrayCalibration.getEnergyOffset(d,s) ) / eDepAvg;
                        arrayCalibration.setEnergyGain(d,s,channelsPerMeV);
                    }
                }
            }
            arrayCalibration.setEnergyGainFactors();
        } catch (SpecNotAvailableException e) {
            System.err.println(e);
        }
    }

    /** Given a set of multiplets which were found by a peak find procedure,
     * go through the spectrum and perform individual gaussian fits using
     * the given peak parameters as a starting point.
     * @param spectrum the histogram containing the peaks to be fit
     * @param results the results of the peak find process (starting point for fits)
     * @param width width parameter used in the peak find (starting point for fits)
     * @throws FitException if a problem occurs during a fit
     * @return new array of multiplets with improved parameters from fitting
     */
    private Multiplet [] doFits(int [] spectrum, Multiplet [] results,
    double width)
    throws FitException {
        double maxSeparation=MAX_SEP;
        double [] dspec = new double[spectrum.length];
        double [] derr = new double[spectrum.length];
        Multiplet [] mfits = new Multiplet[results.length];
        for (int ch=0; ch<spectrum.length; ch++){
            dspec[ch]=spectrum[ch];
            if (spectrum[ch]==0) {
                derr[ch]=1.0;//weighted same as one count
            } else {// != 0
                derr[ch]=Math.sqrt(dspec[ch]);
            }
        }
        for (int m=0; m<results.length; m++) {
            int begin = (int)Math.round(
            results[m].getPeak(0).getPosition()-width*maxSeparation);
            int end = (int)Math.round(
            results[m].getPeak(results[m].size()-1).getPosition()+
            width*maxSeparation);
            GaussianFit gf = new GaussianFit(dspec, derr, begin, end,
            results[m]);
            gf.doFit();
            mfits[m] = gf.getFitResult();
        }
        return mfits;
    }

    /** 
     * Utility method for printing out the results of a peakfind to the
     * system console.
     * 
     * @param spectrum spectrum that was peakfit
     * @param name of the spectrum
     * @param results all multiplets found in peakfit
     */
    void printResults(int [] spectrum, String name,
    Multiplet [] results){
        System.out.println("Spectrum \""+name+"\" with "+
        spectrum.length+" channels.");
        System.out.println("Peak find resulted in "+results.length+
        " multiplets.");
        for (int m=0; m<results.length; m++){
            System.out.println("--\nMultiplet "+m+" has "+
            results[m].size()+" peaks.");
            for (int p=0; p<results[m].size(); p++){
                System.out.println(results[m].getPeak(p));
            }
        }
    }
    
    static void showHelp(){
        final JFrame helpframe = new JFrame("Help for BatchCalibration");
        Container help = helpframe.getContentPane();
        help.setLayout(new BorderLayout());
        String HELP_STRING=
        "'BatchCalibration' needs to be started with 2 arguments:\n"+
        "\tArgument 1: filename of batch input file describing overall\n"+
        "\t\tprocessing to be done\n"+
        "\tArgument 2: filename of .obj file containing ArrayCalibration\n"+
        "\t\tobject to be modified\n";
        JTextArea text = new JTextArea(HELP_STRING, 5, 40);
        help.add(text,BorderLayout.CENTER);
        JButton ok = new JButton("OK");
        help.add(ok,BorderLayout.SOUTH);
	helpframe.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e){
		helpframe.dispose();
                System.exit(0);
            }
        });
        ok.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ae){
                helpframe.dispose();
                System.exit(0);
            }
        });
        helpframe.pack();
        helpframe.show();
    }                                                   

    /** Launches the calibration, using the command line argument as the input file.
     * @param args input file to process
     */
    public static void main (String args[]) {
        if (args.length==0 || args[0].toLowerCase().equals("help")) {
            showHelp();
        } else {
            File in = new File(args[0]);
            if (!in.exists()) {
                showHelp();
            } else {
                File acFile = new File(args[1]);
                new BatchCalibration(in,acFile);//launch  program
            }
        }
    }
}