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
 * PIDplotYaleOld.java
 *
 * Created on September 18, 2001, 8:43 PM
 */
package dwvisser;
import java.io.*;
import java.util.*;
import javax.swing.*;

import dwvisser.nuclear.Absorber;
import dwvisser.nuclear.EnergyLoss;
import dwvisser.nuclear.Gas;
import dwvisser.nuclear.NuclearException;
import dwvisser.nuclear.Nucleus;
import dwvisser.nuclear.Reaction;
import dwvisser.nuclear.Solid;

import java.awt.*;
import java.awt.event.*;

/**
 * This class will execute a process to simulate the focal plane detector.
 * It is based on the code written by Kazim Yildiz for the Vax machines,
 * which are now dying.  It was too difficult and time-consuming to figure
 * out why Kazim's code wouldn't run properly on the Solaris machines.
 *
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public final class PIDplotYaleOld {

    public class FileQuitAction extends AbstractAction {
        public FileQuitAction() {super("Quit"); }
        public void actionPerformed(ActionEvent ae){
            System.exit(0);
        }
    }

    public class FileOpenAction extends AbstractAction {
        public FileOpenAction() {super("Open..."); }
        public void actionPerformed(ActionEvent ae){
            File in = getFile();
            if (in != null) {
                window.hide();
                window=null;
                System.gc();
                simSpecFile(in);
            }
        }
    }

    static final int ENTRANCE_FOIL=1;
    static final int BLOCKER_FOIL=0;
    static final int EXIT_FOIL=7;
    final static java.util.List reactions = new ArrayList();
    static final int NUM_ABSORBERS=11;
    static final int MAX_RXNS = 50;
    static final int ANODE_INDEX=4;
    static final int SCINT_INDEX=9;
    final static int[] Q= new int[MAX_RXNS];
	private static final String WINDOWNAME="DET -- Yale Enge PID simulator";
    private static final String WTT="Wrong token type: ";
    static private final int DEFAULT_SHAPE=2;
    static private final int BARE_SHAPE=0;
    static private final int MOST_RIGID_SHAPE=1;
    static private final char SP=' ';
    static private final char CR='\n';
    static private final double RANGE_FACTOR=1.05;
    
    static final String SET_COLOR=" grap/set txci ";
    
    /* These gas thicknesses in cm were taken directly from Kazim's code
     * He doesn't explicitly account for the 45 degrees incidence on the
     * detector in his code.  I do, so these have to be divided by cos(45deg)
     * when I produce the absorber objects below.*/
    static final double[] gasThickness = {1.41,3.68,7.07,34.2,4.53};//isobutane in cm
    static final Absorber[] absorbers=new Absorber[NUM_ABSORBERS];
    static final EnergyLoss[] eloss = new EnergyLoss[NUM_ABSORBERS];
    static final String [] SCINT_ELEMENTS = {"C","H"};
    static final double [] SCINT_FRACTIONS = {10,11};
    
    /* places to store results */
    static final double[] radius=new double[MAX_RXNS];//start radius(?)
    static final int[] maxEnergyIndex = new int[MAX_RXNS];
    static final boolean[] firstEnergy = new boolean[MAX_RXNS];
    static final double [][] rho= new double[MAX_RXNS][MAX_RXNS];
    static final double [][] Eproj = new double[MAX_RXNS][MAX_RXNS];
    static final double [][][] losses = new double[MAX_RXNS][MAX_RXNS][NUM_ABSORBERS];
    
    private String plotTitle;
    private Nucleus beam;
    private double Tbeam,Bfield,angle,rhoMin,rhoMax,pressure;
    private int reactionCount=0;
	private JFrame window;
    private File lastFile;
    
    /**
     * <p>This code calculates PID plots
     * for the WNSL Enge spectrograph focal plane detector. The
     * energy loss regions used for calculation are as follows:</p>
     * 
     * <dl>
     * <dt>0</dt><dd>Blocker Foil</dd>
     * <dt>1</dt><dd>Entrance Foil</dd>
     * <dt>2</dt><dd>"Dead" gas region up to start of cathode</dd>
     * <dt>3</dt><dd>1st part of cathode</dd>
     * <dt>4</dt><dd>Anode, 2nd part of cathode</dd>
     * <dt>5</dt><dd>3rd part of cathode</dd>
     * <dt>6</dt><dd>"Dead" gas region between cathode and exit 
     * foil</dd>
     * <dt>7</dt><dd>Exit Foil</dd>
     * <dt>8</dt><dd>Foil on the scintillator</dd>
     * <dt>9</dt><dd>Scintillator</dd>
     * </dl>
     * 
     * @param name the name of the input specification file
     */
    public PIDplotYaleOld(String name)  {
    	final double scintThickness=0.635*1032;// mg/cm^2
        plotTitle=name;
        try{
            /* 6.35 mm, and 1.032 g/cc density */
            absorbers[SCINT_INDEX]=new Solid(scintThickness,
            Absorber.MILLIGRAM_CM2,SCINT_ELEMENTS,
            SCINT_FRACTIONS);//scintillator
        } catch (NuclearException ne) {
            ne.printStackTrace(System.err);
        }
    }
    
    /**
     * Sets initial values. Assumes isobutane gas in the detector.
     *
     * @param b species of beam
     * @param tb beam kinetic energy in MeV
     * @param kG field of spectrometer in kG
     * @param a angle of spectrometer in degrees
     * @param rmin lower limit of detector radius
     * @param rmax upper limit of detector radius
     * @param p gas pressure in torr
     */
    public void initialize(Nucleus b, double tb, double kG,
    double a, double rmin, double rmax, double p) {
        beam=b;
		Tbeam=tb;
		Bfield=kG;
		angle=a;
		rhoMin=rmin;
		rhoMax=rmax;
		pressure=p;
    }

    private void drawWindow(){
        window = new JFrame(WINDOWNAME);
        final Container contents = window.getContentPane();
        contents.setLayout(new BorderLayout());
        final JMenuBar mb = new JMenuBar();
        final JMenu file = new JMenu("File",true);
        file.add(new FileOpenAction()).setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        file.add(new FileQuitAction()).setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
        mb.add(file);
        window.setJMenuBar(mb);
        final int xdim=320;
        final int ydim=80;
        window.setSize(xdim,ydim);
        window.setResizable(false);
        window.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                window.dispose();
                System.exit(0);
            }
        });
        window.show();
    }
    
    /**
     * Read in an unspecified file by opening up a dialog box.
     *
     * @return  <code>true</code> if successful, <code>false</code> if
     * not
     */
    File getFile() {
        final JFileChooser jfile;
        if (lastFile==null){
            jfile = new JFileChooser();
        } else {
            jfile = new JFileChooser(lastFile);
        }
        final int option=jfile.showOpenDialog(window);
        // dont do anything if it was cancel
        if (option == JFileChooser.APPROVE_OPTION && 
        jfile.getSelectedFile() != null) {
            synchronized (this) {
            	lastFile = jfile.getSelectedFile();
            }
            return lastFile;
        }
        return null;
    }
        
    synchronized void setEntranceFoil(Absorber a){
        absorbers[ENTRANCE_FOIL]=a;
    }
    
    synchronized void setBlockerFoil(Absorber a){
        absorbers[BLOCKER_FOIL]=a;
    }
    
    synchronized void setExitFoil(Absorber a){
        absorbers[EXIT_FOIL]=a;
    }
    
    synchronized void setScintFoil(Absorber a){
        absorbers[SCINT_INDEX-1]=a;
    }
    
    synchronized void setupGas() throws NuclearException {
        final double c45 = Math.cos(Math.toRadians(45));
        for (int i=0; i<gasThickness.length; i++){
            final int j=i+2;//index in absorber array
            absorbers[j]=Gas.Isobutane(gasThickness[i]*c45, pressure);
        }
    }
    
    /**
     * <p>After this is run, absorbers[] will contain 10 physical
     * regions for energy loss.</p> 
     */
    private synchronized void initializeElossObjects(){
        for (int i=0; i< NUM_ABSORBERS; i++) {
            if (absorbers[i] != null) {
            	eloss[i]=new EnergyLoss(absorbers[i]);
            }
        }
    }
    
    
    void addReaction(Nucleus target, Nucleus projectile,
    int qproj) throws Exception {
        if (reactionCount < MAX_RXNS) {
        	synchronized (this){
            	Q[reactionCount]=qproj;
            	reactionCount++;
            }
			reactions.add(new Reaction(target, beam, projectile,
			Tbeam, angle, 0));
        } else {
        	throw new Exception("No more than "+MAX_RXNS+" reactions, please.");
        }
    }
    
    /**
     * Determine possible sets of rho values for various
     * reactions and tabulate them, based on kinematics.
     */
    public void calculateRhoValues(){
        final double delRho=(rhoMax-rhoMin)/20;
        for (int i=0; i< reactionCount; i++){
            final Reaction rxn = (Reaction)reactions.get(i);
            final double qbrho = rxn.getQBrho(0);
            final double p0 = qbrho * Reaction.QBRHO_TO_P;// MeV/c
            radius[i]=qbrho/Q[i]/Bfield;
            int energyIndex=0;
            maxEnergyIndex[i]=0;
            if (radius[i] > rhoMax) {
                firstEnergy[i]=false;
                double r=radius[i];
                do {
                    r -= delRho;
                } while (r > rhoMax);
                energyIndex=0;
                do {
                    rho[i][energyIndex] = r;
                    final double qbr = Q[i]*Bfield*r;
                    final double pi = qbr*Reaction.QBRHO_TO_P;// MeV/c
                    final double m3 = rxn.getProjectile().getMass().value;
                    Eproj[i][energyIndex] = rxn.getLabEnergyProjectile(0)*
                    (Math.sqrt(m3*m3+pi*pi)-m3)/(Math.sqrt(m3*m3+p0*p0)-m3);
                    energyIndex++;
                    r -= delRho;
                } while (r > rhoMin && energyIndex <= MAX_RXNS);
                maxEnergyIndex[i] = energyIndex;
            } else if (radius[i] <= rhoMax && radius[i] >= rhoMin) {
                firstEnergy[i]=true;
                double r=radius[i];
                do {
                    r -= delRho;
                } while (r > rhoMax);
                energyIndex=0;
                do {
                    rho[i][energyIndex] = r;
                    final double qbr = Q[i]*Bfield*r;
                    final double pi = qbr*Reaction.QBRHO_TO_P;// MeV/c
                    final double m3 = rxn.getProjectile().getMass().value;
                    Eproj[i][energyIndex] = rxn.getLabEnergyProjectile(0)*
                    (Math.sqrt(m3*m3+pi*pi)-m3)/(Math.sqrt(m3*m3+p0*p0)-m3);
                    energyIndex++;
                    r -= delRho;
                } while (r > rhoMin && energyIndex <= MAX_RXNS);
                maxEnergyIndex[i] = energyIndex;
            } else {
                firstEnergy[i]=false;
                maxEnergyIndex[i]=0;
            }
        }
    }
    
    /**
     * Calculate and tabulate energy losses in various 
     * detector segments
     */
    public void calculateElosses(){
    	final double keV_TO_MeV=0.001;
        initializeElossObjects();
        final double angle = Math.toRadians(45);//45 degrees incidence on detector
        for (int i=0; i<reactionCount;i++){
            final Reaction rxn = (Reaction)reactions.get(i);
            for (int j=0; j< maxEnergyIndex[i]; j++){//skipped if maxEnergyIndex=0
                double energyLeft = Eproj[i][j];
                for (int k = 0; k<NUM_ABSORBERS; k++){
                    if (eloss[k] != null && energyLeft > 0.0) {
                        losses[i][j][k]=eloss[k].getEnergyLoss(rxn.getProjectile(),
                        energyLeft, angle)*keV_TO_MeV;
                        if (k==SCINT_INDEX){
                        	final double temp=losses[i][j][k];
                        	losses[i][j][k]=eloss[k].getPlasticLightOutput(
                        	rxn.getProjectile(),energyLeft,angle);
                        	energyLeft -= temp;
                        } else {
                        	energyLeft -= losses[i][j][k];
                        }
                    }
                }
            }
        }
    }
    
    
    /**
     * Produces a text file in the .kumac format, which can be run by 
     * the PAW program from CernLib to produce screen graphics and 
     * postscript files.
     *
     * @throws java.io.IOException if there's a problem writing the KUMAc file
     * @param path abstract path where file should be written
     * @param outName the file name, minus any folder information
     */
    public void outputPaw(File path, String outName) throws 
    java.io.IOException {
        final FileWriter out=new FileWriter(new File(path,outName+".kumac"));
        out.write(" macro plot\n");
        out.write(" fortran/file 50 "+outName+".ps\n");
        out.write(" meta 50 -114\n");
        out.write(" his/del *\n");
        out.write(" gra/set ygti .3\n");
        out.write(" gra/set gsiz .3\n");
        out.write(" hi/crea/title_gl '"+plotTitle+"'\n");
        writeHistograms(out);
        out.write(" close 50\n");
        out.write(" return\n");
        out.write(" macro symbol x=4. y=60. num=8 shape=0\n");
        out.write(" sym=\"<[shape]\n");
        out.write(" gra/prim/text [x] [y] [num] 0.2 0. C\n");
        out.write(" gra/prim/text [x] [y] [sym] 0.4 0. C\n");
        out.write(" return\n");
        System.out.println("In order to see the graphics, run PAW.");
        System.out.println("At the 'PAW >' prompt, type: exec "+outName);
        System.out.println("PAW will produce a postscript file: "+outName+
        ".ps");
        out.flush();
        out.close();
    }
        
    private int getColor(int Z){
        final int [] colors = {1,2,4,6,3,7};
        return colors[Z%colors.length];
    }
        
    private void writeHistograms(FileWriter out) throws java.io.IOException{
        /* Anode vs. Cathode Histogram */
        String name = "Anode vs. Cathode";
        double xmax=0.0;
        double ymax=0.0;
        final double [][] cathode = new double[MAX_RXNS][MAX_RXNS];
        /* Anode Vs. Cathode */
        for (int i=0; i<reactionCount; i++){
            for (int j=0; j<maxEnergyIndex[i]; j++){
                cathode[i][j]=losses[i][j][ANODE_INDEX-1]+
                losses[i][j][ANODE_INDEX]+
                losses[i][j][ANODE_INDEX+1];
                if (cathode[i][j] >= xmax) {
                	xmax = cathode[i][j];
                }
                if (losses[i][j][ANODE_INDEX] >= ymax){
                 	ymax = losses[i][j][ANODE_INDEX];//anode
                 }
            }
            if (xmax == 0.0) {
            	xmax=1.0;
            }
            if (ymax == 0.0) {
            	ymax=1.0;
            }
        }
        out.write(" kuip/wait '<CR> to view "+name+"' 0\n");
        out.write(SET_COLOR+" 1\n");
        out.write(" his/crea/2dhisto 100 'Anode vs. Cathode' 10 0. "+
        (xmax*RANGE_FACTOR)+" 10 0. "+(ymax*RANGE_FACTOR)+CR);
        out.write(" hi/plot 100\n");
        for (int i=0; i<reactionCount; i++){
            final Reaction rxn = (Reaction)reactions.get(i);
            final int color = getColor(rxn.getProjectile().Z);
            out.write(SET_COLOR+SP+color+CR);
            for (int j=0; j<maxEnergyIndex[i]; j++){
                int shape= DEFAULT_SHAPE;
                if (firstEnergy[i] && j==0) {
                	shape=MOST_RIGID_SHAPE;
                } else if (Q[i] == rxn.getProjectile().Z) {
                	shape=BARE_SHAPE;
                }
                out.write(" exec symbol "+cathode[i][j]+SP+losses[i][j][ANODE_INDEX]+
                SP+rxn.getProjectile().A+" "+shape+CR);
            }
        }
        /* Anode Vs. Position - ymax is retained from previous plot, 
         * posn range is known */
        name="Anode vs. Position";
        out.write(" kuip/wait '<CR> to view "+name+"' 0\n");
        out.write(SET_COLOR+" 1\n");
        out.write(" his/crea/2dhisto 200 'Anode vs. Position' 10 "+(rhoMin-1)+
        " "+(rhoMax+1)+" 10 0. "+(ymax*1.05)+"\n");
        out.write(" hi/plot 200\n");
        for (int i=0; i<reactionCount; i++){
            final Reaction rxn = (Reaction)reactions.get(i);
            final int color = getColor(rxn.getProjectile().Z);
            out.write(SET_COLOR+" "+color+"\n");
            for (int j=0; j<maxEnergyIndex[i]; j++){
                final int shape;
                if (firstEnergy[i] && j==0) {
                	shape=MOST_RIGID_SHAPE;
                } else if (Q[i] == rxn.getProjectile().Z) {
                	shape=BARE_SHAPE;
                } else {
                	shape = DEFAULT_SHAPE;
                }
                out.write(" exec symbol "+rho[i][j]+" "+losses[i][j][4]+
                " "+rxn.getProjectile().A+" "+shape+"\n");
            }
        }
        /* Anode Vs. Scintillator - ymax is retained from 
         * previous plot, posn range is known */
        name = "Anode vs. Scintillator";
        for (int i=0; i<reactionCount; i++){
            for (int j=0; j<maxEnergyIndex[i]; j++){
                if (losses[i][j][SCINT_INDEX] >= xmax) {
                    xmax = losses[i][j][SCINT_INDEX];
                }
            }
            if (xmax == 0.0) {
            	xmax=1.0;
            }
        }
        out.write(" kuip/wait '<CR> to view "+name+"' 0\n");
        out.write(SET_COLOR+" 1\n");
        out.write(" his/crea/2dhisto 300 'Anode vs. Scintillator' 10 0. "+
        (xmax*1.05)+" 10 0. "+(ymax*1.05)+"\n");
        out.write(" hi/plot 300\n");
        for (int i=0; i<reactionCount; i++){
            final Reaction rxn = (Reaction)reactions.get(i);
            final int color = getColor(rxn.getProjectile().Z);
            out.write(SET_COLOR+" "+color+"\n");
            for (int j=0; j<maxEnergyIndex[i]; j++){
                final int shape;
                if (firstEnergy[i] && j==0) {
                	shape=MOST_RIGID_SHAPE;
                } else if (Q[i] == rxn.getProjectile().Z) {
                 	shape=BARE_SHAPE;
                } else {
                	shape = DEFAULT_SHAPE;
                }
                out.write(" exec symbol "+losses[i][j][SCINT_INDEX]+" "
                +losses[i][j][ANODE_INDEX]+
                " "+rxn.getProjectile().A+" "+shape+"\n");
            }
        }
        /* Cathode Vs. Scintillator */
        name = "Cathode vs. Scintillator";
        for (int i=0; i<reactionCount; i++){
            for (int j=0; j<maxEnergyIndex[i]; j++){
                if (cathode[i][j] >= ymax) {
                	ymax = cathode[i][j];
                }
                if (losses[i][j][SCINT_INDEX] >= xmax){
                	xmax = losses[i][j][SCINT_INDEX];
                }
            }
            if (xmax == 0.0) {
            	xmax=1.0;
            }
            if (ymax == 0.0) {
            	ymax=1.0;
            }
        }
        out.write(" kuip/wait '<CR> to view "+name+"' 0\n");
        out.write(SET_COLOR+" 1\n");
        out.write(" his/crea/2dhisto 400 'Cathode vs. Scintillator' 10 0. "+
        (xmax*1.05)+" 10 0. "+(ymax*1.05)+"\n");
        out.write(" hi/plot 400\n");
        for (int i=0; i<reactionCount; i++){
            final Reaction rxn = (Reaction)reactions.get(i);
            final int color = getColor(rxn.getProjectile().Z);
            out.write(SET_COLOR+" "+color+"\n");
            for (int j=0; j<maxEnergyIndex[i]; j++){
                final int shape;
                if (firstEnergy[i] && j==0) {
                	shape=MOST_RIGID_SHAPE;
                } else if (Q[i] == rxn.getProjectile().Z) {
                	shape=BARE_SHAPE;
                } else {
                	shape = DEFAULT_SHAPE;
                }
                out.write(" exec symbol "+losses[i][j][SCINT_INDEX]+" "+
                cathode[i][j]+
                " "+rxn.getProjectile().A+" "+shape+"\n");
            }
        }
        //Cathode Vs. Position--y already set, x known since position
        name = "Cathode vs. Position";
        out.write(" kuip/wait '<CR> to view "+name+"' 0\n");
        out.write(SET_COLOR+" 1\n");
        out.write(" his/crea/2dhisto 500 'Cathode vs. Position' 10 "+(rhoMin-1)+
        " "+(rhoMax+1)+" 10 0. "+(ymax*1.05)+"\n");
        out.write(" hi/plot 500\n");
        for (int i=0; i<reactionCount; i++){
            final Reaction rxn = (Reaction)reactions.get(i);
            final int color = getColor(rxn.getProjectile().Z);
            out.write(SET_COLOR+" "+color+"\n");
            for (int j=0; j<maxEnergyIndex[i]; j++){
                final int shape;
                if (firstEnergy[i] && j==0){
                	shape=MOST_RIGID_SHAPE;
                } else if (Q[i] == rxn.getProjectile().Z) {
                	shape=BARE_SHAPE;
                } else {
                	shape = DEFAULT_SHAPE;
                }
                out.write(" exec symbol "+rho[i][j]+" "+cathode[i][j]+
                " "+rxn.getProjectile().A+" "+shape+"\n");
            }
        }
        //Scintillator Vs. Position-- x known since position
        name = "Scintillator vs. Position";
        for (int i=0; i<reactionCount; i++){
            for (int j=0; j<maxEnergyIndex[i]; j++){
                if (losses[i][j][SCINT_INDEX] >= ymax) {
                	ymax = losses[i][j][SCINT_INDEX];
                }
            }
            if (xmax == 0.0) {
            	xmax=1.0;
            }
            if (ymax == 0.0) {
            	ymax=1.0;
            }
        }
        out.write(" kuip/wait '<CR> to view "+name+"' 0\n");
        out.write(SET_COLOR+" 1\n");
        out.write(" his/crea/2dhisto 600 'Scintillator vs. Position' 10 "+
        (rhoMin-1)+
        " "+(rhoMax+1)+" 10 0. "+(ymax*1.05)+"\n");
        out.write(" hi/plot 600\n");
        for (int i=0; i<reactionCount; i++){
            final Reaction rxn = (Reaction)reactions.get(i);
            final int color = getColor(rxn.getProjectile().Z);
            out.write(SET_COLOR+" "+color+"\n");
            for (int j=0; j<maxEnergyIndex[i]; j++){
                final int shape;
                if (firstEnergy[i] && j==0) {
                	shape=MOST_RIGID_SHAPE;
                } else if (Q[i] == rxn.getProjectile().Z) {
                	shape=BARE_SHAPE;
                } else {
                	shape = DEFAULT_SHAPE;
                }
                out.write(" exec symbol "+rho[i][j]+" "+
                losses[i][j][SCINT_INDEX]+
                " "+rxn.getProjectile().A+" "+shape+"\n");
            }
        }
    }
        
    private static void simSpecFile(File in){
    	final String al="Al";
        PIDplotYaleOld det=null;
        if (in==null || !in.exists()) {
            System.out.println("No input file specified. To run, open one.");
            det=new PIDplotYaleOld("To run, open an input file.");
            det.drawWindow();
            return;//quits out to give open file a chance
        }
        System.out.println("Processing input file: "+in.getAbsolutePath());
        System.out.println("Positions in cm, most energies in MeV.");
        System.out.println("Scintillator light output in units such that");
        System.out.println("an 8.78 alpha = 30.");
        try{
            final LineNumberReader lr=new LineNumberReader(new FileReader(in));
            final StreamTokenizer st = new StreamTokenizer(
            new BufferedReader(lr));
            st.eolIsSignificant(false); //treat end of line as white space
            st.commentChar('#'); //ignore end of line comments after '#'
            st.wordChars('/','/'); //slash can be part of words
            st.wordChars('_','_'); //underscore can be part of words
            st.nextToken(); final double _Bfield = readDouble(st);
            st.nextToken(); final double _angle = readDouble(st);
            st.nextToken(); final double _rhoMin = readDouble(st);
            st.nextToken(); final double _rhoMax = readDouble(st);
            st.nextToken(); final double _pressure = readDouble(st);
            st.nextToken(); final double _blockerMils = readDouble(st);
            st.nextToken(); final String _blockerElement = readString(st);
            st.nextToken(); final double _inMils = readDouble(st);
            st.nextToken(); final double _outMils = readDouble(st);
            st.nextToken(); final double _scintFoilMils = readDouble(st);
            st.nextToken(); final int _Z = readInteger(st);
            st.nextToken(); final int _A = readInteger(st);
            st.nextToken(); final double _energy = readDouble(st);
            boolean firstReaction=true;            
            st.nextToken(); do {
                final int _ztarg = readInteger(st);
                st.nextToken(); final int _atarg = readInteger(st);
                st.nextToken(); final int _zproj = readInteger(st);
                st.nextToken(); final int _aproj = readInteger(st);
                st.nextToken(); final int _qproj = readInteger(st);
                final Nucleus target=new Nucleus(_ztarg,_atarg);
                final Nucleus projectile=new Nucleus(_zproj,_aproj);
                if (firstReaction){
                    Nucleus beam=new Nucleus(_Z,_A);
                    final String title = _energy+" MeV "+target+"("+beam+","+
                    projectile+"), "+_angle+" deg, "+_pressure+" torr, "+
                    _Bfield+" kG";
                    det=new PIDplotYaleOld(title);                    
                    det.initialize(beam, _energy, _Bfield, _angle, _rhoMin, 
                    _rhoMax, _pressure);
                    if (_blockerMils > 0.0) {
                        det.setBlockerFoil(new Solid(_blockerMils,Absorber.MIL, 
                        _blockerElement));
                    }
                    if (_inMils > 0.0) {
                        det.setEntranceFoil(Solid.Mylar(_inMils,Absorber.MIL));
                    }
                    if (_outMils > 0.0) {
                        det.setExitFoil(Solid.Mylar(_outMils,Absorber.MIL));
                    }
                    if (_scintFoilMils > 0.0) {
                        det.setScintFoil(
                        new Solid(_scintFoilMils,Absorber.MIL,al));
                    }
                    det.setupGas();
                    det.initializeElossObjects();
                }
                firstReaction=false;
                det.addReaction(target,projectile,_qproj);
                st.nextToken();
            } while (st.ttype != StreamTokenizer.TT_EOF);
            det.calculateRhoValues();
            det.calculateElosses();   
            String outName = in.getName();
            outName = outName.substring(0,outName.lastIndexOf('.'));
            det.outputPaw(in.getParentFile(),outName);                    
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    private static int readInteger(StreamTokenizer st) throws IOException {
        if (st.ttype != StreamTokenizer.TT_NUMBER) {
        	throw new IOException(WTT+st.ttype);
        }
        return (int)st.nval;
    }

    private static double readDouble(StreamTokenizer st) throws IOException {
        if (st.ttype != StreamTokenizer.TT_NUMBER) {
        	throw new IOException(WTT+st.ttype);
        }
        return st.nval;
    }
    
    private static String readString(StreamTokenizer st) throws IOException {
        if (st.ttype != StreamTokenizer.TT_WORD){
        	throw new IOException(WTT+st.ttype);
        }
        return st.sval;
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println(e);
		}
        File in=null;
        if (args.length >0) {
        	in = new File(args[0]);
        }
        simSpecFile(in);//null flags that a window should be opened
    }
    
}
