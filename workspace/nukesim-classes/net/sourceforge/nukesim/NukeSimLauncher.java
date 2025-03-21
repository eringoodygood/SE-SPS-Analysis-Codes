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
package net.sourceforge.nukesim;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class NukeSimLauncher extends JFrame {
	
	private String[] noArgs={""};

	
	private NukeSimLauncher(){
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				System.exit(0);
			}
			public void windowClosed(WindowEvent e){
				System.exit(0);
			}
		});
		setupMenu();
		Container pane=getContentPane();
		pane.setLayout(new BorderLayout());
		ClassLoader loader=getClass().getClassLoader();
		JPanel buttonPanel=new JPanel(new GridLayout(0,1));
		JPanel labelPanel=new JPanel(new GridLayout(0,1));
		Icon tempIcon = new ImageIcon(loader.getResource("net/sourceforge/nukesim/jrelkin96.png"));
		JButton tempButton = new JButton(tempIcon);
		tempButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				net.sourceforge.nukesim.JRelKin.main(noArgs);
			}
		});
		labelPanel.add(new JLabel("JRelKin",JLabel.RIGHT));;
		buttonPanel.add(tempButton);
		tempIcon = new ImageIcon(loader.getResource("net.sourceforge.nukesim/plotter96.png"));
		tempButton = new JButton(tempIcon);
		tempButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				net.sourceforge.nukesim.SpecPlot.main(noArgs);
			}
		});
		labelPanel.add(new JLabel("Focal Plane Plotter",JLabel.RIGHT));;
		buttonPanel.add(tempButton);
		tempIcon = new ImageIcon(loader.getResource("net.sourceforge.nukesim/spanc96.png"));
		tempButton = new JButton(tempIcon);
		tempButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				net.sourceforge.nukesim.Spanc.main(noArgs);
			}
		});
		labelPanel.add(new JLabel("Spanc--SPlitpole ANalysis Code",JLabel.RIGHT));;
		buttonPanel.add(tempButton);
		pane.add(buttonPanel,BorderLayout.CENTER);
		pane.add(labelPanel,BorderLayout.WEST);
		this.setTitle("NukeSim-classes Launcher");
		this.setResizable(false);
		pack();
		show();
	}

	private JDialog licenseD;
	private void createLicenseDialog() {
		licenseD =
			new JDialog(
				this,
				"University of Illinois/NCSA Open Source License",
				false);
		Container contents = licenseD.getContentPane();
		licenseD.setForeground(Color.black);
		licenseD.setBackground(Color.lightGray);
		licenseD.setResizable(false);
		licenseD.setLocation(20, 50);
		contents.setLayout(new BorderLayout());
		JPanel center = new JPanel(new GridLayout(0, 1));
		InputStream license_in =
			getClass().getClassLoader().getResourceAsStream("license.txt");
		Reader reader = new InputStreamReader(license_in);
		String text = "";
		int length = 0;
		char[] textarray = new char[2000];
		try {
			length = reader.read(textarray);
		} catch (IOException e) {
			System.err.println(e);
		}
		text = new String(textarray, 0, length);
		System.out.println(text);

		JTextArea textarea = new JTextArea(text);
		center.add(new JScrollPane(textarea));
		contents.add(center, BorderLayout.CENTER);
		JPanel south = new JPanel(new GridLayout(1, 0));
		contents.add(south, BorderLayout.SOUTH);
		JButton bok = new JButton("OK");
		bok.setActionCommand("l_ok");
		bok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				licenseD.dispose();
			}
		});
		south.add(bok);
		licenseD.pack();
		//Recieves events for closing the dialog box and closes it.
		licenseD.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				licenseD.dispose();
			}
		});
	}
	
	private void setupMenu(){
		createLicenseDialog();
		JMenuBar mbar=new JMenuBar();
    	
		JMenu file=new JMenu("File");
		mbar.add(file);
		JMenuItem exit=new JMenuItem("Exit");
		file.add(exit);
		exit.setActionCommand("exit");
		exit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				System.exit(0);
			}
		});
		JMenu help= new JMenu("Help");
		mbar.add(help);
		JMenuItem license=new JMenuItem("License...");
		help.add(license);
		license.setActionCommand("license");
		license.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				licenseD.show();
			}
		});
		setJMenuBar(mbar);
	}


	public static void main(String[] args) {
		//int arg=Integer.parseInt(args[0]);
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println(e);
		}
		new NukeSimLauncher();
	}
}
