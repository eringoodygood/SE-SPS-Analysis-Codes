/*
 * DefineTargetDialog.java
 *
 * Created on December 17, 2001, 2:48 PM
 */
package dwvisser.analysis.spanc;
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionListener;
import dwvisser.analysis.spanc.tables.TargetDefinitionTable;
import dwvisser.nuclear.*;
import dwvisser.analysis.spanc.tables.ReactionTable;
import javax.swing.event.*;
import dwvisser.Spanc;

/**
 * Dialog box for adding a reaction channel to those 
 * considered for the calibration or output.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public class AddReactionDialog extends JDialog implements ActionListener,
ChangeListener {
    
    static final String TITLE="Add Reaction";
    ReactionTable rtable;
    Spanc spanc;
    
    /** Creates new DefineTargetDialog */
    public AddReactionDialog(ReactionTable rt, Spanc sp) {
        super();
        rtable = rt;
        spanc=sp;
        setTitle(TITLE);
        buildGUI();
    }
    
    
    private JTextField _beam = new JTextField(8);
    private JTextField _ebeam = new JTextField(8);
    private JTextField _bfield = new JTextField(8);
    private JComboBox _target = new JComboBox(Target.getComboModel());
    private JSlider _layer = new JSlider(0,
    (Target.getTarget((String)(_target.getSelectedItem()))).getNumberOfLayers()-1,
    JSlider.HORIZONTAL);
    private JComboBox _targetNuclide = new JComboBox((Target.getTarget((String)
            (_target.getSelectedItem()))).getLayerNuclideComboModel(
                _layer.getModel().getValue()));
    private JTextField _projectile = new JTextField(8);
    private JTextField _q = new JTextField(8);
    private JTextField _theta = new JTextField(8);
    private JButton b_ok = new JButton("OK");
    private JButton b_apply = new JButton("Apply");
    private JButton b_cancel = new JButton("Cancel");
    private TargetDefinitionTable table = new TargetDefinitionTable();
    JCheckBox _beamUncertain=new JCheckBox("Mass uncertain?");
    JCheckBox _targetUncertain=new JCheckBox("Mass uncertain?");
    JCheckBox _projectileUncertain=new JCheckBox("Mass uncertain?");
    JCheckBox _residualUncertain=new JCheckBox("Mass uncertain?");
    private void buildGUI(){
        Container contents=getContentPane();
        contents.setLayout(new BorderLayout());
        JPanel center = new JPanel(new GridLayout(0,1));
        JPanel temp = new JPanel(new GridLayout(1,0)); center.add(temp);        
        temp.add(new JLabel("Beam")); temp.add(_beam); temp.add(_beamUncertain);
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("Ebeam [MeV]")); temp.add(_ebeam);
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("B-field [kG]")); temp.add(_bfield);
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("Target")); temp.add(_target);
        _target.addActionListener(this);
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("Layer")); temp.add(_layer);
        setupLayerSlider();
        _layer.addChangeListener(this);
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("Target Nuclide")); temp.add(_targetNuclide); temp.add(_targetUncertain);
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("Projectile")); temp.add(_projectile);temp.add(_projectileUncertain); 
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("Residual Nucleus")); temp.add(_residualUncertain);
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("Q")); temp.add(_q);
        temp = new JPanel(new GridLayout(1,0)); center.add(temp);
        temp.add(new JLabel("Theta [degrees]")); temp.add(_theta);
        contents.add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new GridLayout(1,3));
        south.add(b_ok);
        b_ok.addActionListener(this);
        south.add(b_apply);
        b_apply.addActionListener(this);
        south.add(b_cancel);
        b_cancel.addActionListener(this);
        contents.add(south, BorderLayout.SOUTH);
        pack();
        show();
    }
    
    private void setupLayerSlider(){
        _layer.setMinorTickSpacing(1);
        _layer.setMajorTickSpacing(1);
        _layer.setPaintTicks(true);
        _layer.setPaintLabels(true);
        _layer.setSnapToTicks(true);
        _layer.addChangeListener(this);
    }
    
    public void stateChanged(ChangeEvent change){
        Object source = change.getSource();
        if (source==_layer) {
                Target target=Target.getTarget((String)_target.getSelectedItem());
                _targetNuclide.setModel(target.getLayerNuclideComboModel(
                _layer.getModel().getValue()));
                //_targetNuclide.setEnabled(true);
        }
    }
            
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        //try{
            if (source==b_apply){
                SpancReaction sr= makeReaction();
                if (sr != null){
                    rtable.addRow(sr);
                    spanc.setButtonStates();
                } else {
                    System.err.println("There was a problem creating the reaction.");
                }
            } else if (source==b_ok){
                SpancReaction sr= makeReaction();
                if (sr != null){
                    rtable.addRow(sr);
                    spanc.setButtonStates();
                    dispose();
                } else {
                    System.err.println("There was a problem creating the reaction.");
                }
            } else if (source==b_cancel){
                dispose();
            } else if (source==_target){
                Target target=Target.getTarget((String)_target.getSelectedItem());
                _layer.setMaximum(target.getNumberOfLayers()-1);
                //_layer.setEnabled(true);
                _targetNuclide.setModel(target.getLayerNuclideComboModel(
                _layer.getModel().getValue()));
            } 
        /*} catch (KinematicsException ke) {
            System.err.println(ke);
        }*/
    }
    
    /**
     * returns false if unsuccessful
     */
    private SpancReaction makeReaction(){
        Nucleus beam = Nucleus.parseNucleus(_beam.getText().trim());
        double ebeam = Double.parseDouble(_ebeam.getText().trim());
        double bfield = Double.parseDouble(_bfield.getText().trim());
        Target target = Target.getTarget((String)_target.getSelectedItem());
        int layer = _layer.getModel().getValue();
        Nucleus targetN = (Nucleus)_targetNuclide.getSelectedItem();
        Nucleus projectile = Nucleus.parseNucleus(_projectile.getText().trim());
        int Q = Integer.parseInt(_q.getText().trim());
        double theta = Double.parseDouble(_theta.getText().trim());
        SpancReaction rval =  new SpancReaction(beam, targetN, projectile, ebeam,
        bfield, target, layer, Q, theta);
        if (rval != null){
            rval.setBeamUncertain(_beamUncertain.isSelected());
            rval.setTargetUncertain(_targetUncertain.isSelected());
            rval.setProjectileUncertain(_projectileUncertain.isSelected());
            rval.setResidualUncertain(_residualUncertain.isSelected());
        }
        return rval;
    }
    
    static public void main(String [] args){
        //new Nucleus();
        //new DefineTargetDialog();
    }
}
