/*
 * DefineTargetDialog.java
 *
 * Created on December 17, 2001, 2:48 PM
 */
package dwvisser.analysis.spanc;
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionListener;
import dwvisser.analysis.spanc.tables.TargetDefinitionTable;

/**
 * Dialog box which displays the properties of a given
 * pre-defined target.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public class DisplayTargetDialog extends JDialog implements ActionListener{
    
    static final String TITLE="Display Target";
    Target target;
    private TargetDefinitionTable table;    
    
    public DisplayTargetDialog(Dialog d, Target t){
        super(d,TITLE);
        target=t;
        table = new TargetDefinitionTable(t);
        buildGUI();
    }
    
    private JTextField _name = new JTextField(8);
    private JButton b_ok = new JButton("OK");
 
    private void buildGUI(){
        Container contents=getContentPane();
        contents.setLayout(new BorderLayout());
        JPanel north = new JPanel(new FlowLayout());
        _name.setText(target.getName());
        north.add(_name);
        contents.add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new FlowLayout());
        center.add(new JScrollPane(table));
        contents.add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new GridLayout(1,3));
        south.add(b_ok);
        b_ok.addActionListener(this);
        contents.add(south, BorderLayout.SOUTH);
        pack();
        show();
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            if (actionEvent.getSource()==b_ok){
                dispose();
            }
    }
}
