/*
 */
package jam;
import jam.data.Gate;
import jam.data.Histogram;
import jam.data.RemoteData;
import jam.global.JamStatus;
import jam.global.MessageHandler;
import jam.global.SortMode;

import java.awt.Button;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Class to make this process into a remote server for Jam or
 * hookup to a remote online acquisition that is a server.
 *
 * @author Ken Swartz
 */
public class SetupRemote extends JDialog implements ActionListener, ItemListener {

	static final String DEFAULT_NAME = "jam";
	static final String DEFAULT_URL = "rmi://meitner.physics.yale.edu/jam";

	final static int SERVER = 0;
	final static int SNAP = 1;
	final static int LINK = 2;

	private MessageHandler msgHandler;

	private JLabel lname;
	private JTextField textName;
	private JCheckBox cserver;
	private JCheckBox csnap;
	private JCheckBox clink;
	private JButton bok;
	private JButton bapply;
	private JCheckBox checkLock;
	
	private static final JamStatus status=JamStatus.instance();

	private int mode; //mode server, snap or link
	RemoteData remoteData;
	RemoteAccess remoteAccess;

	private String[] histogramNames;
	private List histogramList, gateList;
	private boolean inApplet; //are we running in a applet

	private boolean setupLock = false;
	
	private static SetupRemote instance;
	public static SetupRemote getSingletonInstance(){
		if (instance==null){
			instance=new SetupRemote();
		}
		return instance;
	}
		
		

	/**
	 * Constructor for Jam Application creates dialog box, we are in an application
	 */
	public SetupRemote() {
		super(status.getFrame(),"Remote Hookup ", false);
		msgHandler = status.getMessageHandler();
		//create dialog box
		setResizable(false);
		setLocation(20, 50);
		setSize(400, 250);
		final Container dl=getContentPane();
		dl.setLayout(new GridLayout(0, 1, 10, 10));
		// panel for mode     
		Panel pm = new Panel();
		pm.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		dl.add(pm);
		final ButtonGroup cbgmode = new ButtonGroup();
		cserver = new JCheckBox("Server  ", true);
		cbgmode.add(cserver);
		cserver.addItemListener(this);
		pm.add(cserver);
		csnap = new JCheckBox("SnapShot", false);
		cbgmode.add(csnap);
		csnap.addItemListener(this);
		pm.add(csnap);
		clink = new JCheckBox("Link    ", false);
		cbgmode.add(clink);
		clink.addItemListener(this);
		pm.add(clink);
		// panel for name
		Panel pn = new Panel();
		pn.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		dl.add(pn);
		lname = new JLabel("Name:", Label.RIGHT);
		pn.add(lname);
		textName = new JTextField(DEFAULT_NAME);
		textName.setColumns(35);
		textName.setBackground(Color.white);
		pn.add(textName);
		// panel for buttons         
		Panel pb = new Panel();
		pb.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		dl.add(pb);
		bok = new JButton("   OK   ");
		pb.add(bok);
		bok.setActionCommand("ok");
		bok.addActionListener(this);
		bapply = new JButton(" Apply  ");
		pb.add(bapply);
		bapply.setActionCommand("apply");
		bapply.addActionListener(this);
		Button bcancel = new Button(" Cancel ");
		pb.add(bcancel);
		bcancel.setActionCommand("cancel");
		bcancel.addActionListener(this);
		checkLock = new JCheckBox("Setup Locked", false);
		checkLock.setEnabled(false);
		checkLock.addItemListener(this);
		pb.add(checkLock);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		mode = SERVER;
		inApplet = false;
	}

	/**
	 * Executed at a action, button pressed
	 *
	 *
	 */
	public void actionPerformed(ActionEvent ae) {

		String command = ae.getActionCommand();
		String name = textName.getText().trim();
		try {
			if (command == "ok" || command == "apply") {
				if (mode == SERVER) {
					server(name);
					msgHandler.messageOutln("Jam made as server: " + name);
				} else if (mode == SNAP) {
					msgHandler.messageOutln("Trying " + name);
					snap(name);
					msgHandler.messageOutln("Jam remote snapShot: " + name);
				} else if (mode == LINK) {
					msgHandler.messageOutln("Trying " + name);
					link(textName.getText().trim());
					msgHandler.messageOutln("Jam remote link: " + name);
				}

				setActive(true);
				lockFields(true);

				if (command == "ok") {
					dispose();
				}
			} else if (command == "cancel") {
				dispose();
			}
		} catch (JamException je) {
			msgHandler.errorOutln(je.getMessage());
		}
	}

	/**
	 * What mode has been picked
	 */
	public void itemStateChanged(ItemEvent ie) {

		try {

			if (ie.getItemSelectable() == cserver) {
				mode = SERVER;
				lname.setText("Name:");
				textName.setText(DEFAULT_NAME);
			} else if (ie.getItemSelectable() == csnap) {
				mode = SNAP;
				lname.setText("URL:");
				textName.setText(DEFAULT_URL);
			} else if (ie.getItemSelectable() == clink) {
				mode = LINK;
				lname.setText("URL:");
				textName.setText(DEFAULT_URL);
				//lock up state    
			} else if (ie.getItemSelectable() == checkLock) {
				setActive((checkLock.isSelected()));
				lockFields((checkLock.isSelected()));

				if (!(checkLock.isSelected())) {
					reset();
				}

			}
		} catch (JamException je) {
			msgHandler.errorOutln(je.getMessage());
		}

	}

	/**
	 * Create a histogram server.
	 *
	 * @exception   JamException    sends a message to the console if there is any problem setting up
	 */
	public void server(String name) throws JamException {

		try {
			remoteAccess = new RemoteAccess();
			System.out.println("new remoteAccess");
			Naming.rebind(name, remoteAccess);

			lockFields(true);
		} catch (UnknownHostException unhe) {
			System.out.println(unhe.getMessage());
			throw new JamException(
				"Creating remote server unknown host, name: "
					+ name
					+ " [SetupRemote]");
		} catch (RemoteException re) {
			System.out.println(re.getMessage());
			throw new JamException(
				"Creating remote server " + re.getMessage() + " [SetupRemote]");
		} catch (java.net.MalformedURLException mue) {
			throw new JamException("Creating remote Server malformed URL [SetupRemote]");
		}
	}

	/**
	 * Get a snap shot of data
	 *
	 * @exception   JamException    all exceptions given to <code>JamException</code> go to the console
	 */
	public void snap(String stringURL) throws JamException {

		try {
				if (status.canSetup()) {
					remoteData = (RemoteData) Naming.lookup(stringURL);
					msgHandler.messageOutln("Remote lookup OK!");
				} else {
					throw new JamException("Can't view remotely, sort mode locked [SetupRemote]");
				}
		} catch (RemoteException re) {
			throw new JamException("Remote lookup up failed URL: " + stringURL);
		} catch (java.net.MalformedURLException mue) {
			throw new JamException(
				"Remote look up malformed URL: " + stringURL);
		} catch (NotBoundException nbe) {
			throw new JamException(
				"Remote look up could not find name " + stringURL);
		}
		try {
			System.out.println("get hist names");
			histogramNames = remoteData.getHistogramNames();
			System.out.println("got hist names");
			System.out.println("names 0 " + histogramNames[0]);
			//load histogram list
			histogramList = remoteData.getHistogramList();
			Histogram.setHistogramList(histogramList);
			//load gate list
			gateList = remoteData.getGateList();
			Gate.setGateList(gateList);
		} catch (RemoteException re) {
			System.out.println(re.getMessage());
			throw new JamException("Remote getting histogram list [SetupRemote]");
		}
	}
	
	/**
	 * Link to a jam process
	 * for now just calls snap
	 * 
	 * @exception   JamException    all exceptions given to <code>JamException</code> go to the console
	 */
	public void link(String stringURL) throws JamException {
		snap(stringURL);
	}
	
	/**
	 * Not sure what needs to be done here.
	 */
	public void reset() {
//		remoteAccess = null;
//		remoteData = null;
	}
	
	/**
	 *
	 */
	public void setActive(boolean active) throws JamException {

		if (active) {
			if ((mode != SERVER) && (!inApplet)) {
				status.setSortMode(SortMode.REMOTE);
			}
		} else {
			if ((mode != SERVER) && (!inApplet)) {
				status.setSortMode(SortMode.NO_SORT);
			}
		}
	}
	/**
	 * Locks up the Remote setup so the fields cannot be edited.
	 * and puts us in remote mode
	 *
	 * Author Ken Swartz
	 *
	 */
	private void lockFields(boolean lock) {
		if (lock) {
			setupLock = true;
			checkLock.setSelected(true);
			checkLock.setEnabled(true);
			textName.setEditable(false);
			textName.setBackground(Color.lightGray);
			bok.setEnabled(false);
			bapply.setEnabled(false);

		} else {
			setupLock = false;
			checkLock.setSelected(false);
			checkLock.setEnabled(false);
			textName.setEditable(true);
			textName.setBackground(Color.white);
			bok.setEnabled(true);
			bapply.setEnabled(true);

		}
	}
}
