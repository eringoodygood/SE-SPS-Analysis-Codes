/*
 * DefineTargetDialog.java
 *
 * Created on December 17, 2001, 2:48 PM
 */

package dwvisser.analysis.spanc;
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionListener;
import dwvisser.nuclear.*;
import dwvisser.analysis.spanc.tables.OutputPeakTable;
import javax.swing.event.*;
import dwvisser.math.UncertainNumber;
import dwvisser.Spanc;

/**
 *
 * @author  Dale
 * @version
 */
public class ChangeOutputPeakDialog
	extends JDialog
	implements ActionListener, ChangeListener {

	static final String TITLE = "Change Output Peak";
	OutputPeakTable opTable;
	CalibrationFit calFit;
	Spanc spanc;
	OutputPeak op;

	/** Creates new Change Output Peak Dialog */
	public ChangeOutputPeakDialog(OutputPeakTable opt, CalibrationFit cf, Spanc sp) {
		super();
		opTable = opt;
		calFit = cf;
		spanc = sp;
		setTitle(TITLE);
		op = OutputPeak.getPeak(opTable.getSelectedRow());
		buildGUI();
	}

	private JSlider _reaction =
		new JSlider(0, SpancReaction.getAllReactions().length - 1, JSlider.HORIZONTAL);
	private JTextField _exproj = new JTextField(8);
	private JTextField _channel = new JTextField(8);
	private JTextField _delCh = new JTextField(8);
	private JButton b_ok = new JButton("OK");
	//private JButton b_apply = new JButton("Apply");
	private JButton b_cancel = new JButton("Cancel");
	private void buildGUI() {
		Container contents = getContentPane();
		contents.setLayout(new BorderLayout());
		JPanel center = new JPanel(new GridLayout(0, 2));
		center.add(new JLabel("Reaction"));
		center.add(_reaction);
		setupReactionSlider();
		_reaction.setValue(op.getReactionIndex());
		_reaction.addChangeListener(this);
		center.add(new JLabel("Ex Projectile [MeV]"));
		center.add(_exproj);
		_exproj.setText(Double.toString(op.getExProjectile()));
		center.add(new JLabel("Channel"));
		center.add(_channel);
		center.add(new JLabel("delCh"));
		center.add(_delCh);
		_channel.setText(Double.toString(op.getChannel().value));
		_delCh.setText(Double.toString(op.getChannel().error));
		contents.add(center, BorderLayout.CENTER);
		JPanel south = new JPanel(new GridLayout(1, 3));
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
				(SpancReaction) SpancReaction.getReaction(_reaction.getModel().getValue());
			b_ok.setEnabled(true);
			//b_apply.setEnabled(true);
		}
	}

	public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
		Object source = actionEvent.getSource();
		if (source == b_ok) {
			modifyPeak();
			opTable.refreshData();
			spanc.setButtonStates();
			dispose();
		} else if (source == b_cancel) {
				dispose();
		}
	}

	/**
	 * Modifies peak
	 */

	private void modifyPeak() {
        double exproj = Double.parseDouble(_exproj.getText().trim());
        double channel = Double.parseDouble(_channel.getText().trim());
        double delch = Double.parseDouble(_delCh.getText().trim());
		try{
			op.setValues(
				reaction,
				exproj,
				new UncertainNumber(channel, delch));
		} catch (KinematicsException ke) {
			System.out.println("Problem modifying output peak: " + ke);
		} catch (dwvisser.statistics.StatisticsException se) {
			System.out.println("Problem modifying output peak: " + se);
		} catch (dwvisser.math.MathException me) {
			System.out.println("Problem modifying output peak: " + me);
		}
	}
}