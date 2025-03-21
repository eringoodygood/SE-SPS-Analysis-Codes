/*
 * Spanc.java
 *
 * Created on September 18, 2001, 8:43 PM
 */
package dwvisser.nuclear;
import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import dwvisser.nuclear.graphics.*;
import dwvisser.nuclear.swing.ComponentPrintable;
import java.awt.print.*;

/**
 * This class will execute a process to simulate the focal plane detector.
 * It is based on the code written by Kazim Yildiz for the Vax machines,
 * which are now dying.  It was too difficult and time-consuming to figure
 * out why Kazim's code wouldn't run properly on the Solaris machines.
 *
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public class FocalPlanePlot implements RadiusRange, ActionListener {
    
    static File lastFile=null;
    static FocalPlanePlot det=null;
    
    static final int NUM_ABSORBERS=11;
    static final int MAX_RXNS = 50;
    
    static final int ANODE_INDEX=4;
    static final int SCINT_INDEX=9;
    
    static final String SET_COLOR=" grap/set txci ";
    
    //these gas thicknesses in cm were taken directly from Kazim's code
    //he doesn't explicitly account for the 45 degrees incidence on the
    //detector in his code.  I do, so these have to be divided by cos(45deg)
    //when I produce the absorber objects below.
    static double[] gasThickness = {1.41,3.68,7.07,34.2,4.53};//isobutane in cm
    static Absorber[] absorbers=new Absorber[NUM_ABSORBERS];
    static EnergyLoss[] eloss = new EnergyLoss[NUM_ABSORBERS];
    static final String [] SCINT_ELEMENTS = {"C","H"};
    static final double [] SCINT_FRACTIONS = {10,11};
    
    //static int[] histogram = new int[6];
    
    //places to store results
    double[] radius=new double[MAX_RXNS];//start radius(?)
    int[] maxEnergyIndex = new int[MAX_RXNS];
    boolean[] firstEnergy = new boolean[MAX_RXNS];
    double [][] rho= new double[MAX_RXNS][MAX_RXNS];
    double [][] Eproj = new double[MAX_RXNS][MAX_RXNS];
    double [][][] losses = new double[MAX_RXNS][MAX_RXNS][NUM_ABSORBERS];
    
    //given
    static String title;
    static Nucleus beam;
    static double Tbeam,Bfield,angle,pressure;
    static Absorber inFoil,outFoil,scintFoil;
    public double rhoMin,rhoMax;
    
    Vector reactions= new Vector();
    static int[] Q= new int[MAX_RXNS];
    int reactionCount=0;
    
    JFrame window;
    JTextField t_rhoMin, t_rhoMax, t_Tbeam, t_angle, t_Bfield;
    
    //stores nucleus excitations keyed by AAZZ string where ZZ is the element symbol
    Hashtable excitationsTable;
    
    SpectrumCanvas [] canvas;
    
    /**
     * Creates new PIDsimulation.
     */
    public FocalPlanePlot(String name){
        excitationsTable=new Hashtable();
        title=name;
//        new EnergyLoss();//initialize stopping data
        try{
            /* 6.35 mm, and 1.032 g/cc density */
            absorbers[SCINT_INDEX]=new Solid(0.635*1032 /*970.0*/
            ,Absorber.MILLIGRAM_CM2,SCINT_ELEMENTS,
            SCINT_FRACTIONS);//scintillator
        } catch (NuclearException ne) {
            ne.printStackTrace(System.err);
        }
    }
    
    /**
     * Sets initial values. Assumes isobutane gas in the detector.
     *
     * @param beam species of beam
     * @param Tbeam kinetic energy in MeV
     * @param Bfield of spectrometer in kG
     * @param angle of spectrometer in degrees
     * @param rhoMin lower limit of detector radius
     * @param rhoMax upper limit of detector radius
     * @param pressure gas pressure in torr
     */
    public void initialize(Nucleus beam, double Tbeam, double Bfield,
    double angle, double rMin, double rMax, double pressure) {
        FocalPlanePlot.beam=beam;
		FocalPlanePlot.Tbeam=Tbeam;
		FocalPlanePlot.Bfield=Bfield;
		FocalPlanePlot.angle=angle;
        rhoMin=rMin;
        rhoMax=rMax;
		FocalPlanePlot.pressure=pressure;
    }
    
    private PageFormat mPageFormat;
    private Container contents;
    private void drawWindow(){
        System.out.println("Drawing Window...");
        window = new JFrame("PLOT -- focal plane PLOTter");
        contents = window.getContentPane();
        contents.setLayout(new BorderLayout());
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File",true);
        file.add(new FileOpenAction()).setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        file.addSeparator();
        file.add(new FilePrintAction()).setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
        file.add(new FilePageSetupAction()).setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK | Event.SHIFT_MASK));
        file.addSeparator();
        file.add(new FileQuitAction()).setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
        mb.add(file);
        window.setJMenuBar(mb);
        window.setSize(650,100+reactionCount*50);
        if (reactionCount>0){
            JPanel P_reactions = new JPanel(new GridLayout(reactionCount,1));
            JPanel P_labels = new JPanel(new GridLayout(reactionCount,1));
            JPanel P_input = new JPanel(new FlowLayout());
            t_rhoMin = new JTextField(rhoMin+" ");
            t_rhoMin.addActionListener(this);
            t_rhoMax = new JTextField(rhoMax+" ");
            t_rhoMax.addActionListener(this);
            t_Tbeam = new JTextField(Tbeam+" ");
            t_Tbeam.addActionListener(this);
            t_angle = new JTextField(angle+" ");
            t_angle.addActionListener(this);
            t_Bfield = new JTextField(Bfield+" ");
            t_Bfield.addActionListener(this);
            P_input.add(new JLabel("Min. Rho [cm]"));
            P_input.add(t_rhoMin);
            P_input.add(new JLabel("Max. Rho [cm]"));
            P_input.add(t_rhoMax);
            P_input.add(new JLabel("Beam Energy [MeV]"));
            P_input.add(t_Tbeam);
            P_input.add(new JLabel("Theta [deg]"));
            P_input.add(t_angle);
            P_input.add(new JLabel("B-field [kG]"));
            P_input.add(t_Bfield);
            contents.add(P_labels,BorderLayout.WEST);
            contents.add(P_reactions,BorderLayout.CENTER);
            contents.add(P_input,BorderLayout.SOUTH);
            canvas = new SpectrumCanvas[reactionCount];
            for (int i=0; i<reactionCount; i++){
                JPanel jp=new JPanel(new FlowLayout());
                Reaction r=(Reaction)reactions.elementAt(i);
                jp.add(new JLabel(" "+r.getTarget()+"("+r.getBeam()+","));
                JLabel ion=new JLabel(r.getProjectile()+"["+Q[i]+"+]");
                ion.setForeground(Color.red);
                jp.add(ion);
                jp.add(new JLabel(")"+r.getResidual()+" "));
                P_labels.add(jp);
                try {
                    canvas[i]=new SpectrumCanvas(this);
                    canvas[i].setRadii(getRadii(i));
                    canvas[i].setEx(getEx(i));
                    P_reactions.add(canvas[i]);
                } catch (KinematicsException ke) {
                    System.err.println(ke);
                }
            }
        }
        window.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                window.dispose();
                System.exit(0);
            }
        });
        PrinterJob pj=PrinterJob.getPrinterJob();
        mPageFormat=pj.defaultPage();
        mPageFormat.setOrientation(PageFormat.LANDSCAPE);
        window.show();
    }
    
    public void actionPerformed(ActionEvent ae){
        Object source = ae.getSource();
        if (source == t_rhoMin || source == t_rhoMax || source==t_Tbeam ||
        source==t_angle || source == t_Bfield) {
            if (getTextFromFields()) window.repaint();
        }
    }
    
    /**
     * Returns whether to redraw.
     */
    private boolean getTextFromFields() {
        boolean recalculate=false;
        boolean redraw = false;
        boolean newReactions = false;
        double temp = Double.parseDouble(t_rhoMin.getText());
        if (temp != rhoMin) {
            rhoMin = temp;
            redraw = true;
        }
        temp=Double.parseDouble(t_rhoMax.getText());
        if (temp != rhoMax) {
            rhoMax = temp;
            redraw=true;
        }
        temp = Double.parseDouble(t_Tbeam.getText());
        if (Tbeam != temp) {
            Tbeam = temp;
            redraw=true;
            newReactions=true;
            recalculate=true;
        }
        temp = Double.parseDouble(t_angle.getText());
        if (angle != temp) {
            angle = temp;
            redraw=true;
            newReactions=true;
            recalculate=true;
        }
        temp = Double.parseDouble(t_Bfield.getText());
        if (Bfield != temp) {
            Bfield = temp;
            redraw = true;
            recalculate = true;
        }
        if (newReactions) changeReactions();
        if (recalculate) sendRadii();
        return redraw;
    }
    
    private void changeReactions() {
        for (int i=0; i<reactionCount; i++) {
            Reaction r=(Reaction)reactions.elementAt(i);
            try {
                reactions.set(i,new Reaction(r, Tbeam, angle, 0));
            } catch (KinematicsException ke) {
                System.err.println(ke);
            }
        }
    }
    
    private void sendRadii() {
        for (int i=0; i<reactionCount; i++) {
            try {
                canvas[i].setRadii(getRadii(i));
            } catch (KinematicsException ke) {
                System.err.println(ke);
            }
        }
    }
    
    
    
    private double []  getRadii(int reactionNumber) throws KinematicsException {
        double [] rval;
        Reaction react = (Reaction)reactions.elementAt(reactionNumber);
        String nucleus = react.residual.toString();
        Vector v = (Vector)excitationsTable.get(nucleus);
        if (v != null) {
            rval =  new double[v.size()];
        } else {
            rval = new double[1];
        }
        for (int i=0; i< rval.length; i++){
            double Ex;
            if (v !=null) {
                Ex=((Double)v.elementAt(i)).doubleValue();
            } else {
                Ex = 0.0;
            }
            Reaction rEx = new Reaction(react,Ex);
            if (rEx.getAngleDegeneracy() > 0){
                rval[i] = rEx.getQBrho(0)/Q[reactionNumber]/Bfield;
            }
        }
        return rval;
    }
    
    private double []  getEx(int reactionNumber) throws KinematicsException {
        double [] rval;
        Reaction react = (Reaction)reactions.elementAt(reactionNumber);
        String nucleus = react.residual.toString();
        Vector v = (Vector)excitationsTable.get(nucleus);
        if (v != null) {
            Double[] temp = new Double[v.size()];
            rval =  new double[v.size()];
            v.copyInto(temp);
            for (int i=0; i<rval.length; i++){
                rval[i]=temp[i].doubleValue();
            }
        } else {
            rval = new double[1];
        }
        return rval;
    }
    
    
    public void setEntranceFoil(Absorber a){
        absorbers[1]=a;
    }
    
    public void setBlockerFoil(Absorber a){
        absorbers[0]=a;
    }
    
    public void setExitFoil(Absorber a){
        absorbers[7]=a;
    }
    
    public void setScintFoil(Absorber a){
        absorbers[8]=a;
    }
    
    public void setupGas() throws NuclearException {
        double c45 = Math.cos(Math.toRadians(45));
        for (int i=0; i<gasThickness.length; i++){
            int j=i+2;//index in absorber array
            absorbers[j]=Gas.Isobutane(gasThickness[i]*c45, pressure);
        }
    }
    
    private void initializeElossObjects(){
        for (int i=0; i< NUM_ABSORBERS; i++) {
            if (absorbers[i] != null) eloss[i]=new EnergyLoss(absorbers[i]);
        }
    }
    
    public void addReaction(Nucleus target, Nucleus projectile,
    int Qprojectile) throws NuclearException {
        if (reactionCount < MAX_RXNS) {
			FocalPlanePlot.Q[reactionCount]=Qprojectile;
            reactionCount++;
        } else throw new NuclearException("No more than "+MAX_RXNS+" reactions, please.");
        this.reactions.add(new Reaction(target, FocalPlanePlot.beam, projectile,
		FocalPlanePlot.Tbeam, FocalPlanePlot.angle, 0));
    }
    
    private void readExcitations(File in){
        //PIDsimulation det=null;
        try{
            LineNumberReader lr=new LineNumberReader(new FileReader(in));
            StreamTokenizer st = new StreamTokenizer(new BufferedReader(lr));
            st.eolIsSignificant(false); //treat end of line as white space
            st.commentChar('#'); //ignore end of line comments after '#'
            st.wordChars('/','/'); //slash can be part of words
            st.wordChars('_','_'); //underscore can be part of words
            st.nextToken();
            if (st.ttype == StreamTokenizer.TT_NUMBER) {
                do {
                    Vector tempVector=new Vector();
                    int A = readInteger(st);
                    st.nextToken();    String element = readString(st);
                    excitationsTable.put(A+element,tempVector);
                    do {
                        st.nextToken();
                        if (st.ttype == StreamTokenizer.TT_NUMBER)
                            tempVector.add(new Double(readDouble(st)));
                    } while (st.ttype == StreamTokenizer.TT_NUMBER);
                    if (st.ttype == StreamTokenizer.TT_WORD) st.nextToken(); //a word, usually 'end' after all excitations for a nucleus
                } while (st.ttype != StreamTokenizer.TT_EOF);//if not EOF, assumed to be next A
            }
        } catch (IOException e) {
            System.err.println(e);
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    private static void simSpecFile(File in){
        det=null;
        if (in==null || !in.exists()) {
            System.out.println("No file specified.");
            det=new FocalPlanePlot("no file yet");
            det.drawWindow();
        } else {
            try{
                System.out.println("Reading reactions from: "+in);
                LineNumberReader lr=new LineNumberReader(new FileReader(in));
                StreamTokenizer st = new StreamTokenizer(new BufferedReader(lr));
                st.eolIsSignificant(false); //treat end of line as white space
                st.commentChar('#'); //ignore end of line comments after '#'
                st.wordChars('/','/'); //slash can be part of words
                st.wordChars('_','_'); //underscore can be part of words
                st.nextToken();    double _Bfield = readDouble(st);
                st.nextToken();    double _angle = readDouble(st);
                st.nextToken();    double _rhoMin = readDouble(st);
                st.nextToken();    double _rhoMax = readDouble(st);
                st.nextToken();    double _pressure = readDouble(st);
                st.nextToken();    double _blockerMils = readDouble(st);
                st.nextToken();    String _blockerElement = readString(st);
                st.nextToken();    double _inMils = readDouble(st);
                st.nextToken();    double _outMils = readDouble(st);
                st.nextToken();    double _scintFoilMils = readDouble(st);
                st.nextToken();    int _Z = readInteger(st);
                st.nextToken();    int _A = readInteger(st);
                st.nextToken();    double _energy = readDouble(st);
                boolean firstReaction=true;
                int count=0;
                st.nextToken(); do {
                    count++;
                    int _ztarg = readInteger(st);
                    st.nextToken(); int _atarg = readInteger(st);
                    st.nextToken(); int _zproj = readInteger(st);
                    st.nextToken(); int _aproj = readInteger(st);
                    st.nextToken(); int _qproj = readInteger(st);
                    Nucleus target=new Nucleus(_ztarg,_atarg);
                    Nucleus projectile=new Nucleus(_zproj,_aproj);
                    if (firstReaction){
                        Nucleus beam=new Nucleus(_Z,_A);
                        String title = _energy+" MeV "+target+"("+beam+","+
                        projectile+"), "+_angle+" deg, "+_pressure+" torr, "+
                        _Bfield+" kG";
                        det=new FocalPlanePlot(title);
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
                            det.setScintFoil(new Solid(_scintFoilMils,Absorber.MIL,"Al"));
                        }
                        det.setupGas();
                        det.initializeElossObjects();
                    }
                    firstReaction=false;
                    System.out.println("Adding Reaction "+count);
                    det.addReaction(target,projectile,_qproj);
                    st.nextToken();
                } while (st.ttype != StreamTokenizer.TT_EOF);
                det.readExcitations(new File(in.getParentFile(),"excitations.dat"));
                det.drawWindow();
            } catch (IOException e) {
                System.err.println(e);
            } catch (NuclearException e) {
                System.err.println(e);
            }
        }
    }
    
    private static int readInteger(StreamTokenizer st) throws IOException {
        //st.nextToken();
        if (st.ttype != StreamTokenizer.TT_NUMBER) throw new IOException(
        ".readInteger(): Wrong token type: "+st.ttype);
        return (int)st.nval;
    }
    
    private static double readDouble(StreamTokenizer st) throws IOException {
        //st.nextToken();
        if (st.ttype != StreamTokenizer.TT_NUMBER) throw new IOException(
        ".readInteger(): Wrong token type: "+st.ttype);
        return st.nval;
    }
    
    private static String readString(StreamTokenizer st) throws IOException {
        //st.nextToken();
        if (st.ttype != StreamTokenizer.TT_WORD) throw new IOException(
        ".readString(): Wrong token type: "+st.ttype);
        return st.sval;
    }
    
    public double getRhoMin(){
        return rhoMin;
    }
    
    public double getRhoMax(){
        return rhoMax;
    }
    
    /**
     * Read in an unspecified file by opening up a dialog box.
     *
     * @param  mode  whether to open or reload
     * @return  <code>true</code> if successful, <code>false</code> if not
     */
    File getFile() {
        JFileChooser jfile;
        if (lastFile==null){
            jfile = new JFileChooser();
        } else {
            jfile = new JFileChooser(lastFile);
        }
        int option=jfile.showOpenDialog(window);
        // dont do anything if it was cancel
        if (option == JFileChooser.APPROVE_OPTION && jfile.getSelectedFile() != null) {
            lastFile = jfile.getSelectedFile();
            return lastFile;
        }
        return null;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        File in=null;
        if (args.length >0) {
        	in = new File(args[0]);
        }
        simSpecFile(in);
    }
    
    
    public class FilePrintAction extends AbstractAction {
        
        /** Creates new FilePrintAction */
        public FilePrintAction() {
            super("Print");
        }
        
        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            PrinterJob pj=PrinterJob.getPrinterJob();
            ComponentPrintable cp = new ComponentPrintable(contents);
            pj.setPrintable(cp, mPageFormat);
            if (pj.printDialog()){
                try {pj.print();}
                catch (PrinterException e) {
                    System.err.println(e);
                }
            }
        }
        
    }
    
    public class FilePageSetupAction extends AbstractAction {
        public FilePageSetupAction() {super("Page setup...");}
        public void actionPerformed(ActionEvent ae){
            PrinterJob pj = PrinterJob.getPrinterJob();
            mPageFormat = pj.pageDialog(mPageFormat);
        }
    }
    
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
    
}
