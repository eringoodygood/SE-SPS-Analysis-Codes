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
package net.sourceforge.nukesim.analysis.spanc;

import jade.physics.Energy;
import jade.physics.Quantity;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.nukesim.math.MathException;
import net.sourceforge.nukesim.math.UncertainNumber;
import net.sourceforge.nukesim.nuclear.KinematicsException;
import net.sourceforge.nukesim.nuclear.NuclearException;
import net.sourceforge.nukesim.nuclear.NukeUnits;
import net.sourceforge.nukesim.statistics.StatisticsException;
/**
 * @author net.sourceforge.nukesim
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ExVsChOutputDialog
	extends JDialog
	implements ActionListener, ChangeListener, NukeUnits {

	private final JTextField tFileName = new JTextField(30);
	private final JTextField tMin = new JTextField("0    ");
	private final JTextField tMax = new JTextField("4095 ");
	private final JTextField tEx = new JTextField("0    ");
	private final JButton ok = new JButton("OK");
	private final JButton apply = new JButton("Apply");

	public ExVsChOutputDialog() {
		Container contents = getContentPane();
		contents.setLayout(new BorderLayout());
		setResizable(false);

		//south "act on it" panel
		JPanel south = new JPanel(new GridLayout(1, 3));
		ok.addActionListener(this);
		ok.setEnabled(false);
		south.add(ok);
		apply.addActionListener(this);
		apply.setEnabled(false);
		south.add(apply);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		south.add(cancel);
		contents.add(south, BorderLayout.SOUTH);

		//north file panel
		JPanel north = new JPanel(new FlowLayout());
		north.add(new JLabel("Output File"));
		north.add(tFileName);
		JButton browse = new JButton("Browse");
		browse.addActionListener(this);
		north.add(browse);
		contents.add(north, BorderLayout.NORTH);

		//reaction selector
		JPanel center = new JPanel(new GridLayout(2, 1));
		JPanel selector = new JPanel(new FlowLayout());
		setupReactionSlider();
		selector.add(_reaction);
		selector.add(new JLabel("Reaction"));
		center.add(selector);

		//ch range
		JPanel range = new JPanel(new GridLayout(1, 6));
		range.add(new JLabel("Low Channel"));
		range.add(tMin);
		range.add(new JLabel("High Channel"));
		range.add(tMax);
		range.add(new JLabel("Projectile Ex"));
		range.add(tEx);
		center.add(range);

		contents.add(center, BorderLayout.CENTER);
		pack();
		show();
	}

	private JSlider _reaction =
		new JSlider(
			0,
			SpancReaction.getAllReactions().length - 1,
			JSlider.HORIZONTAL);
	private void setupReactionSlider() {
		_reaction.setMinorTickSpacing(1);
		_reaction.setMajorTickSpacing(1);
		_reaction.setPaintTicks(true);
		_reaction.setPaintLabels(true);
		_reaction.setSnapToTicks(true);
		_reaction.addChangeListener(this);
		_reaction.setValue(0);
	}

	SpancReaction reaction;
	public void stateChanged(ChangeEvent change) {
		Object source = change.getSource();
		if (source == _reaction) {
			reaction =
				(SpancReaction) SpancReaction.getReaction(
					_reaction.getModel().getValue());
			ok.setEnabled(true);
			apply.setEnabled(true);
		}
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		String text = e.getActionCommand();
		if (text.equals("Browse")) {
			browseForDir();
		}
		if (text.equals("OK") || text.equals("Apply")) {
			try {
				outputFile();
			} catch (Exception except) {
				JOptionPane.showConfirmDialog(
					this,
					except.getMessage(),
					"Error writing to file.",
					JOptionPane.WARNING_MESSAGE);
			}

		}
		if (text.equals("Cancel") || text.equals("OK")) {
			this.dispose();
		}
	}

	/**
	 * add all files in a directory to sort
	 *
	 */
	private void browseForDir() {
		JFileChooser fd =
			new JFileChooser(new File(tFileName.getText().trim()));
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int option = fd.showOpenDialog(this);
		//save current values
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
			tFileName.setText(fd.getSelectedFile().getPath());
		}
	}

	private void outputFile()
		throws
			KinematicsException,
			MathException,
			StatisticsException,
			FileNotFoundException,
			NuclearException {
		PrintWriter pw = null;
		OutputPeak calc = null;
		File file = new File(tFileName.getText().trim());
		String tableHead = "Channel\tEx\n";
		pw = new PrintWriter(new FileOutputStream(file));
		pw.print(tableHead);
		int min = Integer.parseInt(tMin.getText().trim());
		int max = Integer.parseInt(tMax.getText().trim());
		final Energy ExProj = Energy.energyOf(Quantity.valueOf(
				Double.parseDouble(tEx.getText().trim()),keV));
		calc =
			new OutputPeak(
				reaction,ExProj,
				new UncertainNumber((min + max) / 2));
		for (int i = min; i <= max; i++) {
			calc.setValues(reaction, ExProj, new UncertainNumber(i));
			double Ex = calc.getExResidual(false).doubleValue(keV);
			pw.print(i + "\t" + Ex + "\n");
		}
		pw.close();
	}

}
