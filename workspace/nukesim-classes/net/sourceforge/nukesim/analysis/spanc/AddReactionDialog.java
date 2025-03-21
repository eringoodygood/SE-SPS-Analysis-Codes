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
 * DefineTargetDialog.java
 *
 * Created on December 17, 2001, 2:48 PM
 */
package net.sourceforge.nukesim.analysis.spanc;
import jade.physics.Angle;
import jade.physics.Energy;
import jade.physics.MagneticFluxDensity;
import jade.physics.Quantity;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.nukesim.Spanc;
import net.sourceforge.nukesim.analysis.spanc.tables.ReactionTable;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.Nucleus;
import net.sourceforge.nukesim.nuclear.NukeUnits;

/**
 * Dialog box for adding a reaction channel to those 
 * considered for the calibration or output.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public class AddReactionDialog extends JDialog implements ActionListener,
ChangeListener, NukeUnits {
    
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
        temp.add(new JLabel("B-field [T]")); temp.add(_bfield);
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
        try{
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
        } catch (NuclearException ne) {
            JOptionPane.showMessageDialog(this,ne.getMessage(),"Nuclear Error",
            JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * returns null if unsuccessful
     */
    private SpancReaction makeReaction(){
    	Nucleus beam,projectile;
    	try {
			beam = Nucleus.parseNucleus(_beam.getText().trim());
			projectile = Nucleus.parseNucleus(_projectile.getText().trim());
    	} catch (NuclearException ne){
    		JOptionPane.showConfirmDialog(spanc,ne.getMessage(),"Error parsing nucleus",JOptionPane.ERROR_MESSAGE);
    		return null;
    	}
        Energy ebeam = Energy.energyOf(Quantity.valueOf(
        		Double.parseDouble(_ebeam.getText().trim()),MeV));
        MagneticFluxDensity bfield = MagneticFluxDensity.magneticFluxDensityOf(
        		Quantity.valueOf(Double.parseDouble(_bfield.getText().trim()),tesla));;
        Target target = Target.getTarget((String)_target.getSelectedItem());
        int layer = _layer.getModel().getValue();
        Nucleus targetN = (Nucleus)_targetNuclide.getSelectedItem();
        final int Q = Integer.parseInt(_q.getText().trim());
        Angle theta = Angle.angleOf(Quantity.valueOf(
        		Double.parseDouble(_theta.getText().trim()),deg));
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
}
