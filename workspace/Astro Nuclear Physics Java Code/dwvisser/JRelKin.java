package dwvisser;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import dwvisser.nuclear.KinematicsException;
import dwvisser.nuclear.swing.ValuesChooser;
import dwvisser.nuclear.swing.ValuesListener;
import dwvisser.nuclear.table.KinematicsOutputTable;
import dwvisser.nuclear.table.KinematicsOutputTableModel;
import dwvisser.nuclear.table.ReactionTable;
import dwvisser.nuclear.table.ReactionTableModel;

/**
 * Program to calculate relativistic kinematics for the SplitPole detector. Puts out
 * put in a nice table.
 *
 * @author Dale Visser
 * @version 1.0
 */
public class JRelKin extends JFrame implements ValuesListener,ActionListener {
static final double [] INITIAL_BEAM_ENERGIES = {90.0};
static final double [] INITIAL_RESIDUAL_EXCITATIONS = {0.0};
static final double [] INITIAL_LAB_ANGLES = {10.0};

static ReactionTable rt;
    static KinematicsOutputTableModel kotm;
    ValuesChooser be=new ValuesChooser(this,"Beam Energy","MeV",INITIAL_BEAM_ENERGIES);
    ValuesChooser ex4=new ValuesChooser(this,"Ex(Residual)","MeV",INITIAL_RESIDUAL_EXCITATIONS);
    ValuesChooser la3=new ValuesChooser(this,"Lab Angle","deg",INITIAL_LAB_ANGLES);
    JTextField tt=new JTextField("0.0");
    private static final String INTRO =
    "JRelKin 1.0beta written by Dale W. Visser 16 Dec 1999\n"+
    "Modified 30 May 2001\n"+
    "Double-click on nuclei in table to edit them.\n"+
    "Enter experimental parameters in fields at bottom.\n"+
    "Range expects min,max,delta separated by spaces.\n"+
    "Multi expects multiple values separated by spaces.\n"+
    "Changes take effect when user hits return in a field.\n\n"+
    "With finite target thickness the code calculates for beam interaction\n"+
    "at the middle of the target. The only fields modified are:\n"+
    "\t* Beam Energy (at interaction point)\n\t* Projectile KE (after exiting target)"+
        "\n\t* Projectile QBrho (after exiting target)\n";
    
    public JRelKin(){
        super("JRelKin");
        System.out.println(INTRO);
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                System.exit(0);
            }
            public void windowClosed(WindowEvent e){
                System.exit(0);
            }
        });
        Container pane=getContentPane();
        //new Nucleus();
        rt=new ReactionTable(new ReactionTableModel());
        JScrollPane rtsp=new JScrollPane(rt);
        rtsp.setOpaque(true);
        rtsp.setColumnHeaderView(rt.getTableHeader());
        pane.add(getChoicePanel(),BorderLayout.SOUTH);
        try {
            kotm=new KinematicsOutputTableModel(rt,be.getValues(),
            ex4.getValues(),la3.getValues());
        } catch (KinematicsException ke) {
            System.err.println(ke);
        }
        KinematicsOutputTable kot = new KinematicsOutputTable(kotm);
        JScrollPane kotsp =new JScrollPane(kot);
        kotsp.setColumnHeaderView(kot.getTableHeader());
        kotsp.setOpaque(true);
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,rtsp,kotsp);
        pane.add(split,BorderLayout.CENTER);
        setSize(675,600);
        setVisible(true);
        split.setDividerLocation(0.25);
    }

    private JPanel getChoicePanel(){
        JPanel jp=new JPanel(new GridLayout(1,4,5,5));
        jp.setOpaque(true);
        jp.add(be);
        jp.add(ex4);
        jp.add(la3);
        JPanel ptt=new JPanel(new GridLayout(2,1,5,5));
        ptt.add(new JLabel("Target Thickness [ug/cm^2]"));
        tt.addActionListener(this);
        ptt.add(tt);
        jp.add(ptt);
        return jp;
    }
    
    private double getTargetThickness(){
        return Double.parseDouble(tt.getText().trim());
    }

    public void receiveValues(ValuesChooser vc, double [] values)  {
        try {
            if (vc==be) {
                kotm.setBeamEnergies(values);
            } else if (vc==ex4) {
                kotm.setResidualExcitations(values);
            } else if (vc==la3) {
                kotm.setLabAngles(values);
            }
        } catch (KinematicsException ke) {
            System.err.println(ke);
        }
        repaint();
    }

    public static void main(String [] args){
        new JRelKin();
    }
    
    /**
     * Only actions come from target thickness textfield.
     */
    public void actionPerformed(final java.awt.event.ActionEvent ae) {
        double thickness = Double.parseDouble(tt.getText().trim())/2;
        try{
            kotm.setTargetThickness(thickness);
        } catch (KinematicsException ke) {
            System.err.println(getClass().getName()+".actionPerformed(): "+ke);
        }
    }
}
