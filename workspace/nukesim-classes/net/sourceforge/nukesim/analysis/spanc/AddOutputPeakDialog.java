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
import net.sourceforge.nukesim.analysis.spanc.tables.OutputPeakTable;
import net.sourceforge.nukesim.math.UncertainNumber;
import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.NukeUnits;

/**
 *
 * @author  Dale Visser
 * @version 1.2
 */
public class AddOutputPeakDialog extends JDialog implements ActionListener,
ChangeListener, NukeUnits {
    
    private static final String TITLE="Add Output Peak";
    private final OutputPeakTable opTable;
    private final Spanc spanc;
	private final JSlider _reaction = new JSlider(0,
	SpancReaction.getAllReactions().length-1, JSlider.HORIZONTAL);
	private final JTextField _exproj = new JTextField(8);
	private final JTextField _channel = new JTextField(8);
	private final JTextField _delCh = new JTextField(8);
	private final JButton b_ok = new JButton("OK");
	private final JButton b_apply = new JButton("Apply");
	private final JButton b_cancel = new JButton("Cancel");
    
    /** Creates new DefineTargetDialog */
    public AddOutputPeakDialog(OutputPeakTable opt, Spanc sp) {
        super();
        opTable = opt;
        spanc =sp;
        setTitle(TITLE);
        buildGUI();
    }
    
    private void buildGUI(){
        Container contents=getContentPane();
        contents.setLayout(new BorderLayout());
        JPanel center = new JPanel(new GridLayout(0,2));
        center.add(new JLabel("Reaction")); center.add(_reaction);
        setupReactionSlider();
        _reaction.addChangeListener(this);
        center.add(new JLabel("Ex Projectile [keV]")); center.add(_exproj);
        _exproj.setText("0");
        center.add(new JLabel("Channel")); center.add(_channel);
        center.add(new JLabel("delCh")); center.add(_delCh);
        contents.add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new GridLayout(1,3));
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
    
    SpancReaction reaction;
    public void stateChanged(ChangeEvent change){
        Object source = change.getSource();
        if (source==_reaction) {
            reaction=(SpancReaction)SpancReaction.getReaction(_reaction.getModel().getValue());
            b_ok.setEnabled(true);
            b_apply.setEnabled(true);
        }
    }
            
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
            if (source==b_apply){
                OutputPeak op= makePeak();
                if (op != null){
                    opTable.addRow(op);
                    spanc.setButtonStates();
                } else {
                    System.err.println("There was a problem creating the peak.");
                }
            } else if (source==b_ok){
                OutputPeak op= makePeak();
                if (op != null){
                    opTable.addRow(op);
                    spanc.setButtonStates();
                    dispose();
                } else {
                    System.err.println("There was a problem creating the peak.");
                }
            } else if (source==b_cancel){
                dispose();
            } 
    }
    
    /**
     * returns false if unsuccessful
     */

    private OutputPeak makePeak() {
        double exproj = Double.parseDouble(_exproj.getText().trim());
        double channel = Double.parseDouble(_channel.getText().trim());
        double delch = Double.parseDouble(_delCh.getText().trim());
        try {
            return new OutputPeak(reaction,
            		Energy.energyOf(Quantity.valueOf(exproj,keV)), 
            new UncertainNumber(channel,delch));
        } catch (KinematicsException ke) {
            System.out.println("Problem making output peak: "+ke);
            return null;
        } catch (net.sourceforge.nukesim.statistics.StatisticsException se) {
            System.out.println("Problem making output peak: "+se);
            return null;
        } catch (net.sourceforge.nukesim.math.MathException me) {
            System.out.println("Problem making output peak: "+me);
            return null;
		} catch (NuclearException me) {
			System.out.println("Problem making output peak: "+me);
			return null;
		}
    }
}
