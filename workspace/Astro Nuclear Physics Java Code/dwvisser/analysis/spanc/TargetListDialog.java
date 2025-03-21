/*
 * TargetListDialog.java
 *
 * Created on December 17, 2001, 5:18 PM
 */

package dwvisser.analysis.spanc;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import dwvisser.Spanc;

/**
 *
 * @author  Dale
 * @version 
 */
public class TargetListDialog extends JDialog implements ActionListener {

    Spanc spanc;
    
    /** Creates new TargetListDialog */
    public TargetListDialog(Spanc f) {
        super(f,"Target List");
        spanc=f;
        setupGUI();
    }

    JList _list = new JList(Target.getTargetList());
    JButton b_add = new JButton("Add");
    JButton b_remove = new JButton("Remove");
    JButton b_display = new JButton("Display");
    private JButton b_ok = new JButton("OK"); 
    private void setupGUI(){
        Container contents=getContentPane();
        contents.setLayout(new BorderLayout());
        JPanel west = new JPanel(new GridLayout(3,1));
        west.add(b_add);
        b_add.addActionListener(this);
        west.add(b_remove);
        b_remove.addActionListener(this);
        west.add(b_display);
        b_display.addActionListener(this);
        contents.add(west,BorderLayout.WEST);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(b_ok);
        b_ok.addActionListener(this);
        contents.add(south, BorderLayout.SOUTH);
        JPanel center = new JPanel(new FlowLayout());
        center.add(new JScrollPane(_list));
        contents.add(center, BorderLayout.CENTER);
        pack();
    }
        
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source == b_add) {
            new DefineTargetDialog(this,spanc);
        } else if (source == b_remove) {
            Object [] values = _list.getSelectedValues();
            for (int i=0; i<values.length; i++){
                Target.removeTarget(Target.getTarget((String)values[i]));
            }
            spanc.setButtonStates();
        } else if (source == b_display) {
            Target selected = (Target)Target.getTarget((String)_list.getSelectedValue());
            if (selected != null) {
                new DisplayTargetDialog(this,selected);
            } else {
                System.err.println("No target selected.");
            }
        } else if (source == b_ok) {
            hide();
        }
    }    
            
        
}
