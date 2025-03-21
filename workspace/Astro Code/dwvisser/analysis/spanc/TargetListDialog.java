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
package dwvisser.analysis.spanc;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import dwvisser.Spanc;

/**
 *
 * @author  Dale Visser
 * @version 1.2
 * @since 1.0 (17 Dec 2001)
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
