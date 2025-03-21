package jam.io;

import jam.data.Histogram;
import jam.global.JamStatus;
import jam.global.MessageHandler;
import jam.global.RTSI;
import jam.global.BroadcastEvent;
import jam.util.FileUtilities;
import jam.global.Broadcaster;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

/**
 * Dialog for exporting lists of histograms.  Searches 
 * <code>jam.io</code> for
 * all classes extending <code>jam.io.ImpExp</code>.
 * 
 * @author <a href=mailto:dale@visser.name>Dale Visser</a>
 */
public class BatchExport extends JDialog implements ActionListener, Observer {

	private ImpExp[] exportClasses=new ImpExp[0];
	private JRadioButton[] exportChoice;
	private JTextField  txtDirectory;
	private JComboBox cbHist;	
	private JList lstHists;
	private JButton bExport;
	private MessageHandler console;
	private File lastListFile = null;

	public BatchExport(MessageHandler console) {
		super(JamStatus.instance().getFrame(),"Batch Histogram Export");
		this.console = console;
		Broadcaster bc = Broadcaster.getSingletonInstance();
		bc.addObserver(this);
		getClasses();
		buildGUI();
		setup();
	}
	/**
	 * Construct the GUI
	 */
	private void buildGUI() {
		Container contents = getContentPane();
		contents.setLayout(new BorderLayout());
		final int CHOOSER_SIZE=200;
		
		//Top Panel with chooser
		JPanel pTop = new JPanel(new GridLayout(0, 1,5,5));
		pTop.setBorder(new EmptyBorder(10,10,0,10));	
		contents.add(pTop, BorderLayout.NORTH);		
		
		JPanel pChooser = new JPanel(new FlowLayout(FlowLayout.CENTER, 10,10));
		pTop.add(pChooser);
		pChooser.add(new JLabel("Add Histogram"));
		cbHist = new JComboBox();
		cbHist.setActionCommand("select");
		Dimension dim= cbHist.getPreferredSize();
		dim.width=CHOOSER_SIZE;
		cbHist.setPreferredSize(dim);		
		pChooser.add(cbHist);
		
		//Button panel on left			
		JPanel pButtons = new JPanel(new GridLayout(0, 1, 5, 2));
		pButtons.setBorder(new EmptyBorder(10,10,10,10));	
		contents.add(pButtons, BorderLayout.WEST);
				
		JButton bAddAllHist = new JButton("Add All");
		bAddAllHist.setToolTipText("Adds all 1 dimension histograms.");
		bAddAllHist.setActionCommand("addall");
		bAddAllHist.addActionListener(this);
		pButtons.add(bAddAllHist);
		
		JButton bRemoveName = new JButton("Remove Selected");
		bRemoveName.setToolTipText("Removes selected histograms");
		bRemoveName.setActionCommand("removeselect");
		bRemoveName.addActionListener(this);
		pButtons.add(bRemoveName);
		JButton bRemoveAll = new JButton("Remove All");
		bRemoveAll.setToolTipText("Remove all histograms.");
		bRemoveAll.setActionCommand("removeall");
		bRemoveAll.addActionListener(this);
		pButtons.add(bRemoveAll);
		JButton bLoadList = new JButton("Load List");
		bLoadList.setToolTipText("Load list of histograms from file.");
		bLoadList.setActionCommand("loadlist");
		bLoadList.addActionListener(this);
		pButtons.add(bLoadList);
		JButton bSaveList = new JButton("Save List");
		bSaveList.setToolTipText("Save list of histograms to file.");
		bSaveList.setActionCommand("savelist");
		bSaveList.addActionListener(this);
		pButtons.add(bSaveList);

		//List of histograms
		JPanel pList = new JPanel(new GridLayout(1, 1));
		pList.setBorder(new EmptyBorder(10,0,10,10));
		pList.setToolTipText("List of histograms to export.");
		lstHists = new JList(new DefaultListModel());
		lstHists.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		pList.add(new JScrollPane(lstHists));
		//contents.add(lstHists, BorderLayout.CENTER);
		contents.add(pList, BorderLayout.CENTER);

		//Lower button button
		JPanel pBottom = new JPanel(new GridLayout(0,1,5,5));
		contents.add(pBottom, BorderLayout.SOUTH);

		//Options panel
		JPanel pOptions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10,0));
		pBottom.add(pOptions);
		JPanel pBtnOpn = new JPanel(new GridLayout(1, 0,10,0));
		pOptions.add(pBtnOpn);
		ButtonGroup optionButtons = new ButtonGroup();
		exportChoice = new JRadioButton[exportClasses.length];
		for (int i = 0; i < exportClasses.length; i++) {
			exportChoice[i] =
				new JRadioButton(
					exportClasses[i].getFormatDescription());
			exportChoice[i].setToolTipText("Select to export in "
			+exportClasses[i].getFormatDescription()+" format.");
			exportChoice[i].addActionListener(this);
			optionButtons.add(exportChoice[i]);
			pBtnOpn.add(exportChoice[i]);
		}

		//Directory panel
		JPanel pDirectory = new JPanel(new FlowLayout(FlowLayout.CENTER,10,0));
		pDirectory.add(new JLabel("Directory"));
		txtDirectory = new JTextField(System.getProperty("user.home"),40);
		txtDirectory.setToolTipText("Directory to write exported histogram files.");
		pDirectory.add(txtDirectory);
		JButton bBrowse = new JButton("Browse\u2026");
		bBrowse.setActionCommand("browse");
		bBrowse.addActionListener(this);
		pDirectory.add(bBrowse);
		pBottom.add(pDirectory);

		//Button panel		
		JPanel pButton = new JPanel(new FlowLayout(FlowLayout.CENTER,10,5));
		bExport = new JButton("Export");
		bExport.setToolTipText("Export selected histograms.");
		bExport.setEnabled(false);
		bExport.setActionCommand("export");
		bExport.addActionListener(this);
		pButton.add(bExport);
		JButton bCancel = new JButton("Cancel");
		bCancel.setToolTipText("Close this dialog.");
		bCancel.setActionCommand("cancel");
		bCancel.addActionListener(this);
		pButton.add(bCancel);
		pBottom.add(pButton);
		
		pack();		
		setResizable(false);
	}

	private void getClasses() {
		final String here=getClass().getName()+".getClasses(): ";
		final Set set = RTSI.find("jam.io", ImpExp.class,false);
		set.remove(ImpExp.class);
		final java.util.List list=new ArrayList();
		for (Iterator it = set.iterator(); it.hasNext(); ) {
			final Class temp = (Class) it.next();
			try {
				final ImpExp ie=(ImpExp) temp.newInstance();
				if (ie.batchExportAllowed()){
					list.add(ie);
				}
				//exportClasses[i] = (ImpExp) temp.newInstance();
			} catch (InstantiationException e) {
				console.errorOutln(here+e.getMessage());
			} catch (IllegalAccessException e) {
				console.errorOutln(here+e.getMessage());
			}
		}
		exportClasses=(ImpExp [])list.toArray(exportClasses);
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("select")) {
			addSelectedHist();
		} else if (command.equals("addall")) {
			addAllHists();
		} else if (command.equals("removeselect")) {
			removeSelectedHist();
		} else if (command.equals("removeall")) {
			removeAllHists();
		} else if (command.equals("browse")) {
			browseForDir();
		} else if (command.equals("loadlist")) {
			loadList();
		} else if (command.equals("savelist")) {
			saveList();
		} else if (command.equals("cancel")) {
			dispose();
		} else if (command.equals("export")) {
			export();
		}
		setExportEnable();
	}
		
	/**
	 * Enable export button
	 *
	 */		
	private void setExportEnable(){
		boolean selected = false;
		for (int i = 0; i < exportChoice.length; i++) {
			selected |= exportChoice[i].isSelected();
		}
		selected &= lstHists.getModel().getSize()>0;
		bExport.setEnabled(selected);
	}
	/**
	 * Add a selected histogram
	 *
	 */
	private void addSelectedHist(){
		String name = cbHist.getSelectedItem().toString();
		HashSet histSet = new HashSet();		

		//now combine this with stuff already in list
		ListModel lm = lstHists.getModel();
		for (int i = 0; i < lm.getSize(); i++) {
			histSet.add(lm.getElementAt(i));
		}
		histSet.add(name);		
		lstHists.setListData(histSet.toArray());
	}
	/**
	 * Add all 1 d histograms to the list
	 *
	 */
	private void addAllHists() {

		HashSet histSet = new HashSet();
		
		for (Iterator e=Histogram.getHistogramList().iterator();e.hasNext();){
			Histogram h=(Histogram)e.next();
			if ((h.getType()==Histogram.ONE_DIM_INT)||(h.getType()==Histogram.ONE_DIM_DOUBLE)){				
				histSet.add(h.getName());
			}
		}
		lstHists.setListData(histSet.toArray());

	}
	
	/**
	* remove a histogram from the list
	*/
	private void removeSelectedHist() {
		Object[] removeList = lstHists.getSelectedValues();
		ListModel lm = lstHists.getModel();
		Vector v = new Vector();
		for (int i = 0; i < lm.getSize(); i++) {
			v.add(lm.getElementAt(i));
		}
		for (int i = 0; i < removeList.length; i++) {
			v.removeElement(removeList[i]);
		}
		lstHists.setListData(v);
	}

	/**
	 * remove all items from sort list 
	 *
	 */
	private void removeAllHists() {
		lstHists.setListData(new Vector());
	}

	/**
	 * Load a list of histograms to export from a file.
	 *
	 */
	private void loadList() {
		Object listItem;

		JFileChooser fd = new JFileChooser(lastListFile);
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd.setFileFilter(
			new ExtensionFileFilter(
				new String[] { "lst" },
				"List Files (*.lst)"));
		int option = fd.showOpenDialog(this);
		//save current values
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
			lastListFile = fd.getSelectedFile(); //save current directory
			Vector v = new Vector();
			try {
				BufferedReader br =
					new BufferedReader(new FileReader(lastListFile));
				do {
					listItem = Histogram.getHistogram(br.readLine());
					if (listItem != null) {
						v.addElement(listItem);
					}
				} while (listItem != null);
				br.close();
			} catch (IOException ioe) {
				console.errorOutln(ioe.getMessage());
			}
			lstHists.setListData(v);
		}
	}

	/**
	 * Save list of items to export.
	 */
	private void saveList() {
		JFileChooser fd = new JFileChooser(lastListFile);
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd.setFileFilter(
			new ExtensionFileFilter(
				new String[] { "lst" },
				"List Files (*.lst)"));
		int option = fd.showSaveDialog(this);
		//save current values
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
			lastListFile = fd.getSelectedFile(); //save current directory
		}
		try {
			ListModel lm = lstHists.getModel();
			FileWriter saveStream = new FileWriter(fd.getSelectedFile());
			for (int i = 0; i < lm.getSize(); i++) {
				saveStream.write(lm.getElementAt(i) + "\n");
			}
			saveStream.close();
		} catch (IOException ioe) {
			console.errorOutln(ioe.getMessage());
		}
	}
	
/* KBS remove	
	private void addName() {
		String start = txtAddName.getText().trim();
		HashSet goodHists = new HashSet();
		for (Iterator it = Histogram.getHistogramList().iterator();
			it.hasNext();
			) {
			Histogram hist = (Histogram) it.next();
			String name = hist.getName();
			if (name.startsWith(start)) {
				goodHists.add(hist);
			}
		}
		//now combine this with stuff already in list
		ListModel lm = lstHists.getModel();
		for (int i = 0; i < lm.getSize(); i++) {
			goodHists.add(lm.getElementAt(i));
		}
		lstHists.setListData(goodHists.toArray());
	}
*/
	/**
	 * Export the histograms
	 */
	private void export() {
		//select the format
		ImpExp out = null;
		for (int i = 0; i < exportChoice.length; i++) {
			if (exportChoice[i].isSelected()) {
				out = exportClasses[i];
			}
		}
		File dir = new File(txtDirectory.getText().trim());
		if (dir.exists()) {
			if (dir.isDirectory()) {
				//look for any files that might be overwritten
				ListModel lm = lstHists.getModel();
				File[] files = new File[lm.getSize()];
				Histogram[] hist = new Histogram[lm.getSize()];
				boolean already = false;
				for (int i = 0; i < files.length; i++) {
					hist[i] = Histogram.getHistogram((String)lm.getElementAt(i));
					files[i] =
						new File(
							dir,
							FileUtilities.setExtension(hist[i].getName().trim(),
							out.getDefaultExtension(),FileUtilities.APPEND_ONLY));
					already |= files[i].exists();
				}
				if (already) {
					console.errorOutln(
						"At least one file to export already exists. Delete or try a"
							+ " different directory.");
				} else { //go ahead and write
					console.messageOut(
						"Exporting to " + dir.getPath() + ": ",
						MessageHandler.NEW);
					for (int i = 0; i < files.length; i++) {
						console.messageOut(
							files[i].getName(),
							MessageHandler.CONTINUE);
						if (i < files.length - 1)
							console.messageOut(", ", MessageHandler.CONTINUE);
						try {
							out.saveFile(files[i], hist[i]);
						} catch (ImpExpException e) {
							console.errorOutln(
								"Error while trying to write files: "
									+ e.getMessage());
						}
					}
					console.messageOut(".", MessageHandler.END);
				}
			} else { //not a directory
				console.errorOutln(
					"The specified directory is not really a directory.");
			}
		} else { //directory doesn't exist
			console.errorOutln("The specified directory does not exist.");
		}
	}


	/**
	 * add all files in a directory to sort
	 *
	 */
	private void browseForDir() {
		JFileChooser fd =
			new JFileChooser(new File(txtDirectory.getText().trim()));
		fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int option = fd.showOpenDialog(this);
		//save current values
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
				txtDirectory.setText(fd.getSelectedFile().getPath());
		}
	}


	
	/**
	 * Setup histogram chooser
	 *
	 */
	private void setup() {

		cbHist.removeActionListener(this);		
		cbHist.removeAllItems();
		for (Iterator e=Histogram.getHistogramList().iterator();e.hasNext();){
			Histogram h=(Histogram)e.next();
			if ((h.getType()==Histogram.ONE_DIM_INT)||(h.getType()==Histogram.ONE_DIM_DOUBLE)){				
				cbHist.addItem(h.getName());
			}
		}

		cbHist.addActionListener(this);
 
	}
	
	
	/**
	 *Implementation of Observable interface
	 * listeners for broadcast events.
	 * broadcast events where there are
	 * new histograms or histograms added.
	 */
	public void update(Observable observable, Object o){
		BroadcastEvent be=(BroadcastEvent)o;

		if(be.getCommand()==BroadcastEvent.HISTOGRAM_NEW){
			setup();
		} else if(be.getCommand()==BroadcastEvent.HISTOGRAM_ADD){
			setup();
		}

	}
	
	/**
	 * Load list and show
	 */
	public void show() {
		setup();
		super.show();
	}

}
