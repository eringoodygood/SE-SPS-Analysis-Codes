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

package dwvisser.analysis.spanc;
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionListener;
import dwvisser.analysis.spanc.tables.CalibrationPeakTable;
import javax.swing.event.*;
import dwvisser.math.UncertainNumber;
import dwvisser.Spanc;


/**
 * Dialog box for adding a spectrum peak that will be
 * an input to the focal plane calibration.
 *
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public class AddCalibrationPeakDialog extends JDialog implements ActionListener,
ChangeListener {
    
    static final String TITLE="Add Calibration Peak";
    private final CalibrationPeakTable cpTable;
    private final Spanc spanc;
    private final JSlider _reaction = new JSlider(0,
    SpancReaction.getAllReactions().length-1, JSlider.HORIZONTAL);
    private final JTextField _exproj = new JTextField(8);
    private final JTextField _exres = new JTextField(8);
    private final JTextField _delExres = new JTextField(8);
    private final JTextField _channel = new JTextField(8);
    private final JTextField _delCh = new JTextField(8);
    private final JButton b_ok = new JButton("OK");
    private final JButton b_apply = new JButton("Apply");
    private final JButton b_cancel = new JButton("Cancel");
    private SpancReaction reaction;
    
    /** 
     * Creates new DefineTargetDialog.
     *
     * @param cpt table to add to
     * @param sp the application
     */
    public AddCalibrationPeakDialog(CalibrationPeakTable cpt, Spanc sp) {
        super();
        this.spanc=sp;
        cpTable = cpt;
        setTitle(TITLE);
        buildGUI();
    }
    
    private void buildGUI(){
        final Container contents=getContentPane();
        contents.setLayout(new BorderLayout());
        final JPanel center = new JPanel(new GridLayout(9,2));
        center.add(new JLabel("Reaction")); center.add(_reaction);
        setupReactionSlider();
        _reaction.addChangeListener(this);
        center.add(new JLabel("Ex Projectile [MeV]")); center.add(_exproj);
        _exproj.setText("0");
        center.add(new JLabel("Ex Residual [MeV]")); center.add(_exres);
        center.add(new JLabel("Ex Residual Unc. [keV]")); center.add(_delExres);
        center.add(new JLabel("Channel")); center.add(_channel);
        center.add(new JLabel("delCh")); center.add(_delCh);
        contents.add(center, BorderLayout.CENTER);
        final JPanel south = new JPanel(new GridLayout(1,3));
        south.add(b_ok);
        b_ok.setEnabled(false);
        b_ok.addActionListener(this);
        south.add(b_apply);
        b_apply.setEnabled(false);
        b_apply.addActionListener(this);
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
    
    /**
     * Handles state change events.
     *
     * @param change the signal of change
     */
    public void stateChanged(ChangeEvent change){
        final Object source = change.getSource();
        if (source.equals(_reaction)) {
            setReaction(SpancReaction.getReaction(
            _reaction.getModel().getValue()));
            b_ok.setEnabled(true);
            b_apply.setEnabled(true);
        }
    }
    
    private void setReaction(SpancReaction sr){
    	synchronized(this){
    		reaction=sr;
    	}
    }
    
    /**
     * Respond to a user-initiated event.
     *
     * @param actionEvent the event signal
     */        
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        final Object source = actionEvent.getSource();
            if (source.equals(b_apply) || source.equals(b_ok)){
                final CalibrationPeak cp= makePeak();
                if (cp != null){
                    cpTable.addRow(cp);
                    spanc.setButtonStates();
                    spanc.calculateFit();
                    if (source.equals(b_ok)){
                    	dispose();
                    }
                } else {
                	final String err="There was a problem creating the peak.";
                    System.err.println(err);
                }
            } else if (source.equals(b_cancel)){
                dispose();
            } 
    }
    
    private CalibrationPeak makePeak(){
        final double exproj = Double.parseDouble(_exproj.getText().trim());
        final double exres = Double.parseDouble(_exres.getText().trim());
        final double delExRes = 
        Double.parseDouble(_delExres.getText().trim())*0.001;
        final double channel = Double.parseDouble(_channel.getText().trim());
        final double delch = Double.parseDouble(_delCh.getText().trim());
        return new CalibrationPeak(reaction,exproj,
        new UncertainNumber(exres,delExRes), 
        new UncertainNumber(channel,delch));
    }
}
