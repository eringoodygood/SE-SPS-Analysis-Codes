package net.sourceforge.nukesim;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Generates the "splash" window that displays while Jam
 * is launching.  The window disappears after a specified 
 * timeout period or when the user clicks on it.
 */
public class SplashWindow extends JWindow {
	
	/**
	 * Creates the splash window which will exist for as long
	 * as the specified wait time in milliseconds.
	 * 
	 * @param f parent frame
	 * @param waitTime time in milliseconds after which the window disappears
	 */
	public SplashWindow(Frame f, int waitTime, String file, 
	String title, String versionText) {
		super(f);
		drawWindow(file, title, versionText);
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				setVisible(false);
				dispose();
			}
		});
		final int pause = waitTime;
		final Runnable closerRunner = new Runnable() {
			public void run() {
				setVisible(false);
				dispose();
			}
		};
		Runnable waitRunner = new Runnable() {
			public void run() {
				try {
					Thread.sleep(pause);
					SwingUtilities.invokeAndWait(closerRunner);
				} catch (Exception e) {
					e.printStackTrace();
					// can catch InvocationTargetException
					// can catch InterruptedException
				}
			}
		};
		setVisible(true);
		Thread splashThread = new Thread(waitRunner, "SplashThread");
		splashThread.start();
	}

	private void drawWindow(String file, String title, String versionText) {
		ClassLoader cl = this.getClass().getClassLoader();
		Container cp = getContentPane();
		JPanel west = new JPanel(new FlowLayout());
		west.setBackground(Color.white);
		west.setBorder(BorderFactory.createMatteBorder(1,1,0,0,Color.black));
		ImageIcon nukeicon=new 
		ImageIcon(cl.getResource(file));
		final int sizexy=80;
		nukeicon.setImage(nukeicon.getImage().getScaledInstance(
		sizexy,sizexy,Image.SCALE_SMOOTH));
		west.add(new JLabel(nukeicon));
		JPanel center = new JPanel(new GridLayout(0,1));
		center.setBackground(Color.white);
		center.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.black));
		center.add(new JLabel(title));
		center.add(new JLabel("\u00a9 2003 Yale University"));
		center.add(new JLabel("University of Illinois/NCSA Open Source License"));
		JPanel east=new JPanel(new FlowLayout());
		east.setBackground(Color.white);
		east.setBorder(BorderFactory.createMatteBorder(1,0,0,1,Color.black));
		JLabel osi = new JLabel(
		new ImageIcon(cl.getResource("OSI.gif")));
		osi.setToolTipText("Open Source Initiative. See http://www.opensource.org/");
		east.add(osi);
		JPanel panelSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JLabel versionLabel = new JLabel(versionText,SwingConstants.RIGHT);
		panelSouth.add(versionLabel);
		panelSouth.setBackground(Color.cyan);
		panelSouth.setBorder(
			BorderFactory.createMatteBorder(0, 1, 1, 1, Color.black));
		cp.add(panelSouth, BorderLayout.SOUTH);
		cp.add(west,BorderLayout.WEST);
		cp.add(center,BorderLayout.CENTER);
		cp.add(east,BorderLayout.EAST);
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension labelSize = getSize();
		setLocation(
			screenSize.width / 2 - (labelSize.width / 2),
			screenSize.height / 2 - (labelSize.height / 2));
	}

	/**
	 * Displays this window for up to a minute.
	 */
	static public void main(String[] args) {
		final String text="First Line\nSecondLine\nThirdLine";
		new SplashWindow(null, 60000, "dwvisser/jrelkin96.png",text, "1.0");
	}

}
