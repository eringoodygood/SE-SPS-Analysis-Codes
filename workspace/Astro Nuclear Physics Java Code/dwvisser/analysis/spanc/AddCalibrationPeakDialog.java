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
public class AddCalibrationPeakDialog extends JDialog implements ActionListener,ChangeListener {
    
    static final String TITLE="Add Calibration Peak";
    CalibrationPeakTable cpTable;
    Spanc spanc;
    
    /** Creates new DefineTargetDialog */
    public AddCalibrationPeakDialog(CalibrationPeakTable cpt, Spanc sp) {
        super();
        this.spanc=sp;
        cpTable = cpt;
        setTitle(TITLE);
        buildGUI();
    }
    
    
    private JSlider _reaction = new JSlider(0,
    SpancReaction.getAllReactions().length-1, JSlider.HORIZONTAL);
    private JTextField _exproj = new JTextField(8);
    private JTextField _exres = new JTextField(8);
    private JTextField _delExres = new JTextField(8);
    private JTextField _channel = new JTextField(8);
    private JTextField _delCh = new JTextField(8);
    private JButton b_ok = new JButton("OK");
    private JButton b_apply = new JButton("Apply");
    private JButton b_cancel = new JButton("Cancel");
    private void buildGUI(){
        Container contents=getContentPane();
        contents.setLayout(new BorderLayout());
        JPanel center = new JPanel(new GridLayout(9,2));
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
                CalibrationPeak cp= makePeak();
                if (cp != null){
                    cpTable.addRow(cp);
                    spanc.setButtonStates();
                    spanc.calculateFit();
                } else {
                    System.err.println("There was a problem creating the peak.");
                }
            } else if (source==b_ok){
                CalibrationPeak cp= makePeak();
                if (cp != null){
                    cpTable.addRow(cp);
                    spanc.setButtonStates();
                    spanc.calculateFit();
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
    private CalibrationPeak makePeak(){
        double exproj = Double.parseDouble(_exproj.getText().trim());
        double exres = Double.parseDouble(_exres.getText().trim());
        double delExRes = Double.parseDouble(_delExres.getText().trim())*0.001;
        double channel = Double.parseDouble(_channel.getText().trim());
        double delch = Double.parseDouble(_delCh.getText().trim());
        return new CalibrationPeak(reaction,exproj,
        new UncertainNumber(exres,delExRes), 
        new UncertainNumber(channel,delch));
    }
}
