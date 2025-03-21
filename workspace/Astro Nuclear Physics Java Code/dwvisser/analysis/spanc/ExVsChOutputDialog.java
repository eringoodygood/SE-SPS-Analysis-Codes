package dwvisser.analysis.spanc;

import java.awt.event.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import dwvisser.math.UncertainNumber;

/**
 * @author dwvisser
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ExVsChOutputDialog
	extends JDialog
	implements ActionListener, ChangeListener {

	JTextField tFileName = new JTextField(30);
	JTextField tMin = new JTextField("0    ");
	JTextField tMax = new JTextField("4095 ");
	JTextField tEx = new JTextField("0    ");
	JButton ok = new JButton("OK");
	JButton apply = new JButton("Apply");
	CalibrationFit fit;
	public ExVsChOutputDialog(CalibrationFit cf) {
		fit = cf;
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
			outputFile();
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

	private void outputFile() {
		PrintWriter pw = null;
		OutputPeak calc=null;
		try {
			File file = new File(tFileName.getText().trim());
			String tableHead = "Channel\tEx\n";
			pw = new PrintWriter(new FileOutputStream(file));
			pw.print(tableHead);
		} catch (IOException e) {
			System.err.println(e);
		}
		int min = Integer.parseInt(tMin.getText().trim());
		int max = Integer.parseInt(tMax.getText().trim());
		double ExProj = Double.parseDouble(tEx.getText().trim());
		try {
			calc =
			new OutputPeak(reaction, ExProj, new UncertainNumber((min+max)/2), fit);
		} catch (Exception e){
			System.err.println(e);
		}
		for (int i = min; i <= max; i++) {
			try{
				calc.setValues(reaction, ExProj, new UncertainNumber(i));
				double Ex = calc.getExResidual(false).value;
				pw.print(i + "\t" + Ex + "\n");
			} catch (Exception e) {
				//do nothing
			}
		}
		pw.close();
		/*} catch (Exception e) {
			System.err.println(e);
		}*/
	}

}
