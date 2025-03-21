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
package dwvisser;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.prefs.Preferences;
//import javax.help.CSH;
//import javax.help.HelpSet;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

import dwvisser.nuclear.KinematicsException;
import dwvisser.nuclear.NuclearException;
import dwvisser.nuclear.swing.ComponentPrintable;
import dwvisser.nuclear.swing.ValuesChooser;
import dwvisser.nuclear.swing.ValuesListener;
import dwvisser.nuclear.swing.MassTableChooserMenuItems;
import dwvisser.nuclear.table.KinematicsOutputTable;
import dwvisser.nuclear.table.KinematicsOutputTableModel;
import dwvisser.nuclear.table.ReactionTable;
import dwvisser.nuclear.table.ReactionTableModel;

/**
 * Program to calculate relativistic kinematics for the SplitPole 
 * detector. Output is presented in a table.
 *
 * @author Dale Visser
 * @version 1.0
 */
public final class JRelKin extends JFrame implements ValuesListener, ActionListener {
	/*private ActionListener getHelpListener() {
		//final HelpSet hs;
		final String helpsetName = "help/jrelkin/jrelkin.hs";
		try {
			final URL hsURL =
				getClass().getClassLoader().getResource(helpsetName);
			//hs = new HelpSet(null, hsURL);
		} catch (Exception ee) {
			return null;
		}
		//return new CSH.DisplayHelpFromSource(hs.createHelpBroker());
	}*/

	private class FilePrintAction extends AbstractAction {

		/** Creates new FilePrintAction */
		public FilePrintAction() {
			super("Print");
		}

		/**
		 * Create the hardcopy.
		 * 
		 * @param actionEvent the event created by the user requesting a hardcopy
		 */
		 public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
			final PrinterJob pj = PrinterJob.getPrinterJob();
			final ComponentPrintable cp = new ComponentPrintable(pane);
			pj.setPrintable(cp, mPageFormat);
			if (pj.printDialog()) {
				try {
					pj.print();
				} catch (PrinterException e) {
					System.err.println(e);
				}
			}
		}

	}	private class TextExportAction extends AbstractAction {

		private File lastFile;

		/** Creates new TextExportAction */
		TextExportAction() {
			super("Export Text");
		}

		public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
			final JFileChooser jfile;
			final File file;
		
			if (lastFile == null) {
				jfile = new JFileChooser();
			} else {
				jfile = new JFileChooser(lastFile);
			}
			final int option = jfile.showSaveDialog(pane);
			if (option == JFileChooser.APPROVE_OPTION
				&& jfile.getSelectedFile() != null) {
				lastFile = jfile.getSelectedFile();
				file=lastFile;
			} else {
				file=null;
			}
			if (file !=null){
				exportResults(file);
			}
		}
		
		void exportResults(File file){
			try{
				final FileWriter fw=new FileWriter(file);
				fw.write(rt.getTarget()+"("+rt.getBeam()+","+rt.getProjectile()+
				")"+rt.getResidual()+"\n");
				fw.write("Q0 = "+rt.getQ0()+" MeV\n");
				if (getTargetThickness()>0.0){
					fw.write("Assumed "+getTargetThickness()+" ug/cm^2 of pure "+
					rt.getTarget()+".\n");
				} else {
					fw.write("Assumed zero target thickness.\n");
				}
				fw.write("--\n");
				final int rows=kotm.getRowCount();
				final int cols=kotm.getColumnCount();
				for (int i=0; i<cols; i++){
					fw.write(kotm.getColumnName(i));
					if (i<(cols-1)){
						fw.write("\t");
					} 
				}
				fw.write("\n");
				for (int i=0; i<rows; i++){
					for (int j=0; j<cols; j++){
						fw.write(kotm.getValueAt(i,j).toString());
						if (j<(cols-1)){
							fw.write("\t");
						}
					}
					fw.write("\n");
				}
				fw.flush();
				fw.close();
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(
					pane,
					ioe.getMessage(),
					"Kinematics Error",
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	static final double[] INITIAL_BEAM_ENERGIES = { 90.0 };
	static final double[] INITIAL_RESIDUAL_EXCITATIONS = { 0.0 };
	static final double[] INITIAL_LAB_ANGLES = { 10.0 };

	private static ReactionTable rt;
	private static KinematicsOutputTableModel kotm;
	private static final int VERSION_MAJOR=1;
	private static final int VERSION_MINOR=2;
	private static final String VERSION=VERSION_MAJOR+"."+VERSION_MINOR;
	private JDialog instructD, licenseD;
	private final ValuesChooser be =
		new ValuesChooser(this, "Beam Energy", "MeV", INITIAL_BEAM_ENERGIES);
	private final ValuesChooser ex4 =
		new ValuesChooser(
			this,
			"Ex(Residual)",
			"MeV",
			INITIAL_RESIDUAL_EXCITATIONS);
	private final ValuesChooser la3 =
		new ValuesChooser(this, "Lab \u03b8(Projectile)", "\u00b0", INITIAL_LAB_ANGLES);
	private final JTextField tt = new JTextField("0.0");

	private final Container pane;
	private PageFormat mPageFormat;
	


	/**
	 * Creates a JRelKin window.
	 */
	public JRelKin() {
		super("JRelKin");
		new SplashWindow(this, 10000, "dwvisser/jrelkin96.png", getTitle(), "v 1.2");
		final ClassLoader cl = getClass().getClassLoader();
		final Image icon =
			(new ImageIcon(cl.getResource("dwvisser/jrelkin96.png")).getImage());
		this.setIconImage(icon);
		//System.out.println(INTRO);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});
		pane = getContentPane();
		final PrinterJob pj = PrinterJob.getPrinterJob();
		mPageFormat = pj.defaultPage();
		mPageFormat.setOrientation(PageFormat.LANDSCAPE);
		rt = new ReactionTable(new ReactionTableModel(this));
		setupMenu();
		pane.add(rt, BorderLayout.NORTH);
		final JPanel pcenter = new JPanel(new BorderLayout());
		pane.add(pcenter, BorderLayout.CENTER);
		pcenter.add(getChoicePanel(), BorderLayout.NORTH);
		try {
			kotm =
				new KinematicsOutputTableModel(
					rt,
					be.getValues(),
					ex4.getValues(),
					la3.getValues());
		} catch (KinematicsException ke) {
			JOptionPane.showMessageDialog(
				this,
				ke.getMessage(),
				"Kinematics Error",
				JOptionPane.ERROR_MESSAGE);
		} catch (NuclearException ke) {
			JOptionPane.showMessageDialog(
				this,
				ke.getMessage(),
				"Kinematics Error",
				JOptionPane.ERROR_MESSAGE);
		}
		final KinematicsOutputTable kot = new KinematicsOutputTable(kotm);
		final JScrollPane kotsp = new JScrollPane(kot);
		kotsp.setColumnHeaderView(kot.getTableHeader());
		kotsp.setOpaque(true);
		pcenter.add(kotsp, BorderLayout.CENTER);
		final Runnable startGui = new Runnable() {
			public void run() {
				pack();
				setSize((getWidth()*6)/5,getHeight());
				show();
			}
		};
		SwingUtilities.invokeLater(startGui);
	}

	private final void setupMenu() {
		createLicenseDialog();
		final JMenuBar mbar = new JMenuBar();
		final JMenu file = new JMenu("File");
		mbar.add(file);
		final JMenuItem exit = new JMenuItem("Exit");
		file.add(new FilePrintAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
		file.add(new TextExportAction()).setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
		file.add(exit);
		exit.setActionCommand("exit");
		exit.addActionListener(this);
		final JMenu pref = new JMenu("Preferences");
		/*pref.add(m1995);
		pref.add(m2003);*/
		final MassTableChangeListener mtcl=new MassTableChangeListener();
		new MassTableChooserMenuItems(pref,mtcl);
		mbar.add(pref);
		final JMenu help = new JMenu("Help");
		mbar.add(help);
		JMenuItem toc=new JMenuItem("Table of Contents...");
		//toc.addActionListener(getHelpListener());
		help.add(toc);
		setJMenuBar(mbar);
	}
	
	private final class MassTableChangeListener implements ItemListener {
		public void itemStateChanged(ItemEvent ie){
			rt.setValueAt(rt.getValueAt(1, 1), 1, 1);			
		}
	}

	private JPanel getChoicePanel() {
		final char micro='\u03bc';
		final char up2='\u00b2';
		final String units=micro+"g/cm"+up2;
		final JPanel jp = new JPanel(new GridLayout(1, 4, 5, 5));
		jp.setOpaque(true);
		jp.add(be);
		jp.add(ex4);
		jp.add(la3);
		final JPanel ptt = new JPanel(new GridLayout(2, 1, 5, 5));
		ptt.add(new JLabel("Target Thickness ["+units+"]"));
		tt.addActionListener(this);
		ptt.add(tt);
		jp.add(ptt);
		return jp;
	}

	private double getTargetThickness() {
		return Double.parseDouble(tt.getText().trim());
	}

	/**
	 * Receive the values from one of the values choosers and act on 
	 * them.
	 * 
	 * @param vc the chooser
	 * @param values the numerical values received
	 * @return whether the the values were valid
	 */
	public boolean receiveValues(ValuesChooser vc, double[] values) {
		boolean rval = true;
		try {
			if (vc == be) {
				kotm.setBeamEnergies(values);
			} else if (vc == ex4) {
				kotm.setResidualExcitations(values);
			} else if (vc == la3) {
				kotm.setLabAngles(values);
			}
		} catch (KinematicsException ke) {
			JOptionPane.showMessageDialog(
				this,
				ke.getMessage(),
				"Kinematics Error",
				JOptionPane.WARNING_MESSAGE);
			rval = false;
		} catch (NuclearException ke) {
			JOptionPane.showMessageDialog(
				this,
				ke.getMessage(),
				"Nuclear Error",
				JOptionPane.WARNING_MESSAGE);
			rval = false;
		}
		repaint();
		return rval;
	}

	/**
	 * Launch JRelKin.
	 */
	public static void main(String[] args) {
		final String linux = "Linux";
		final String kunststoff =
			"com.incors.plaf.kunststoff.KunststoffLookAndFeel";
		boolean useKunststoff = linux.equals(System.getProperty("os.name"));
		if (useKunststoff) {
			try {
				UIManager.setLookAndFeel(kunststoff);
			} catch (ClassNotFoundException e) {
				useKunststoff = false;
			} catch (Exception e) { //all other exceptions
				final String title = "Error setting GUI appearance";
				JOptionPane.showMessageDialog(
					null,
					e.getMessage(),
					title,
					JOptionPane.WARNING_MESSAGE);
			}
		}
		if (!useKunststoff) {
			try {
				UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				final String title = "Error setting GUI appearance";
				JOptionPane.showMessageDialog(
					null,
					e.getMessage(),
					title,
					JOptionPane.WARNING_MESSAGE);
			}
		}
		new JRelKin();
	}

	private final void createLicenseDialog() {
		licenseD =
			new JDialog(
				this,
				"University of Illinois/NCSA Open Source License",
				false);
		final Container contents = licenseD.getContentPane();
		licenseD.setResizable(true);
		licenseD.setLocation(20, 50);
		contents.setLayout(new BorderLayout());
		final JPanel center = new JPanel(new GridLayout(0, 1));
		final InputStream license_in =
			getClass().getClassLoader().getResourceAsStream("license.txt");
		final Reader reader = new InputStreamReader(license_in);
		String text = "";
		int length = 0;
		final char[] textarray = new char[2000];
		try {
			length = reader.read(textarray);
		} catch (IOException e) {
			System.err.println(e);
		}
		text = new String(textarray, 0, length);
		final JTextArea textarea = new JTextArea(text);
		center.add(new JScrollPane(textarea));
		contents.add(center, BorderLayout.CENTER);
		final JPanel south = new JPanel(new GridLayout(1, 0));
		contents.add(south, BorderLayout.SOUTH);
		final JButton bok = new JButton("OK");
		bok.setActionCommand("l_ok");
		bok.addActionListener(this);
		south.add(bok);
		licenseD.pack();
		final Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
		licenseD.setSize(licenseD.getWidth(),screen.height/2);
		final int posx=20;
		licenseD.setLocation(posx,screen.height/4);
		/* Recieves events for closing the dialog box and closes it. */
		licenseD.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				licenseD.dispose();
			}
		});
		final String defaultVal="notseen";
		final String key="JRelKinLicense";
		final Preferences helpnode=Preferences.userNodeForPackage(getClass());
		if (!VERSION.equals(helpnode.get(key,defaultVal))){
			showLicense();
			helpnode.put(key,VERSION);
		}
	}

	/**
	 * Show Jam's open source license text.
	 */
	public void showLicense() {
		licenseD.show();
	}

	/**
	 * Only actions come from target thickness textfield.
	 * 
	 * @param ae the user-precipitated event
	 */
	public void actionPerformed(final java.awt.event.ActionEvent ae) {
		final String command = ae.getActionCommand();
		if (command.equals("i_ok")) {
			instructD.dispose();
		} else if (command.equals("l_ok")) {
			licenseD.dispose();
		} else if (command.equals("exit")) {
			System.exit(0);
		} else {
			final double thickness = Double.parseDouble(tt.getText().trim());
			try {
				kotm.setTargetThickness(thickness);
			} catch (KinematicsException ke) {
				JOptionPane.showMessageDialog(
					this,
					ke.getMessage(),
					"Kinematics Error",
					JOptionPane.WARNING_MESSAGE);
			} catch (NuclearException ke) {
				JOptionPane.showMessageDialog(
					this,
					ke.getMessage(),
					"Nuclear Error",
					JOptionPane.WARNING_MESSAGE);
			}
		}
	}
}