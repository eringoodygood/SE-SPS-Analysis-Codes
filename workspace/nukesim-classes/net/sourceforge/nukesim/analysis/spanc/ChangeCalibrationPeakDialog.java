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
import jade.physics.Energy;
import jade.physics.Quantity;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.nukesim.Spanc;
import net.sourceforge.nukesim.analysis.spanc.tables.CalibrationPeakTable;
import net.sourceforge.nukesim.math.QuantityUtilities;
import net.sourceforge.nukesim.math.UncertainNumber;
import net.sourceforge.nukesim.nuclear.NukeUnits;


/**
 * Dialog for changing calibration peak parameters. 
 *
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class ChangeCalibrationPeakDialog extends JDialog implements 
ActionListener,ChangeListener,NukeUnits {
    
    static final String TITLE="Change Calibration Peak";
    CalibrationPeakTable cpTable;
    Spanc spanc;
    CalibrationPeak peak;
    
    /** Creates new DefineTargetDialog */
    public ChangeCalibrationPeakDialog(CalibrationPeakTable cpt, Spanc sp) {
        super();
        this.spanc=sp;
        cpTable = cpt;
        setTitle(TITLE);
        peak=CalibrationPeak.getPeak(cpTable.getSelectedRow());
        buildGUI();
    }
    
    
    private JSlider _reaction = new JSlider(0,
    SpancReaction.getAllReactions().length-1, JSlider.HORIZONTAL);
    private JTextField _exproj = new JTextField(8);
    private JTextField _exres = new JTextField(8);
    private JTextField _delExres = new JTextField(8);
    private JTextField _channel = new JTextField(8);
    private JTextField _delCh = new JTextField(8);
    private JButton b_ok = new JButton("OK");
    //private JButton b_apply = new JButton("Apply");
    private JButton b_cancel = new JButton("Cancel");
    private void buildGUI(){
        Container contents=getContentPane();
        contents.setLayout(new BorderLayout());
        JPanel center = new JPanel(new GridLayout(9,2));
        center.add(new JLabel("Reaction")); center.add(_reaction);
        setupReactionSlider();
        _reaction.setValue(peak.getReactionIndex());
        _reaction.addChangeListener(this);
        center.add(new JLabel("Ex Projectile [keV]")); center.add(_exproj);
        _exproj.setText(QuantityUtilities.noUnits(peak.getExProjectile(),keV));
        center.add(new JLabel("Ex Residual [keV]")); center.add(_exres);
        center.add(new JLabel("Ex Residual Unc. [keV]")); center.add(_delExres);
        _exres.setText(QuantityUtilities.noUnits(peak.getExResidual(),keV));
        _delExres.setText(Double.toString(QuantityUtilities.errorValue(peak.getExResidual(),keV)));
        center.add(new JLabel("Channel")); center.add(_channel);
        center.add(new JLabel("delCh")); center.add(_delCh);
        _channel.setText(Double.toString(peak.getChannel().value));
        _delCh.setText(Double.toString(peak.getChannel().error));
        contents.add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new GridLayout(1,3));
        south.add(b_ok);
        b_ok.setEnabled(false);
        b_ok.addActionListener(this);
        //south.add(b_apply);
        //b_apply.setEnabled(false);
        //b_apply.addActionListener(this);
        south.add(b_cancel);
        b_cancel.addActionListener(this);
        contents.add(south, BorderLayout.SOUTH);
        pack();
        show();
    }
    
    private void setupReactionSlider(){
        _reaction.setMinorTickSpacing(1);
        _reaction.setMajorTickSpacing(1);
        _reaction.setPaintTicks(true);
        _reaction.setPaintLabels(true);
        _reaction.setSnapToTicks(true);
        _reaction.addChangeListener(this);
        _reaction.setValue(0);
    }
    
    SpancReaction reaction;
    public void stateChanged(ChangeEvent change){
        Object source = change.getSource();
        if (source==_reaction) {
            reaction=(SpancReaction)SpancReaction.getReaction(_reaction.getModel().getValue());
            b_ok.setEnabled(true);
            //b_apply.setEnabled(true);
        }
    }
            
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
			 if (source==b_ok){
			 		modifyPeak();
			 		cpTable.refreshData();
                    spanc.setButtonStates();
                    spanc.calculateFit();
                    dispose();
            } else if (source==b_cancel){
                dispose();
            } 
    }
    
    /**
     * returns false if unsuccessful
     */
    private void modifyPeak(){
        double exproj = Double.parseDouble(_exproj.getText().trim());
        double exres = Double.parseDouble(_exres.getText().trim());
        double delExRes = Double.parseDouble(_delExres.getText().trim());
        double channel = Double.parseDouble(_channel.getText().trim());
        double delch = Double.parseDouble(_delCh.getText().trim());
        peak.setValues(reaction,
        Energy.energyOf(Quantity.valueOf(exproj,keV)),
        Energy.energyOf(Quantity.valueOf(exres,delExRes,keV)), 
        new UncertainNumber(channel,delch));
    }
    
}
