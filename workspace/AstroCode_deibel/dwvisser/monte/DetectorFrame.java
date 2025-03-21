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
package dwvisser.monte;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  Dale Visser
 * @version 1.0 (9 March 2001)
 */
public class DetectorFrame extends JFrame {

    Container me;
    JTextField t_z0, t_incline;
    JTextField [] t_n=new JTextField[16];
    JTextField []  t_theta=new JTextField[16];
    JTextField []  t_theta_s=new JTextField[16];
    JTextField []  t_inc=new JTextField[16];
    JTextField []  t_inc_s=new JTextField[16];
    JTextField [] t_distance = new JTextField[16];
    JTextField [] t_distance_s = new JTextField[16];
    JTextField t_events, t_hits;
    
    java.text.NumberFormat nf;
    /** Creates new DetectorFrame */
    public DetectorFrame(double z0, double incline) {
        nf=java.text.NumberFormat.getInstance();
        nf.setMinimumFractionDigits(3);
        nf.setMaximumFractionDigits(3);
        
        me=getContentPane();
        
        JPanel north=new JPanel(new FlowLayout());
        north.add(new JLabel("z0"));
        t_z0=new JTextField(nf.format(z0));
        north.add(t_z0);
        north.add(new JLabel("Incline"));
        t_incline = new JTextField(nf.format(incline));
        north.add(t_incline);
        me.add(north,BorderLayout.NORTH);
        
        JPanel center=new JPanel(new GridLayout(16+1, 8, 5, 5));
        center.add(new JLabel("Strip"));
        center.add(new JLabel("Hits"));
        center.add(new JLabel("Theta"));
        center.add(new JLabel("delTheta"));
        center.add(new JLabel("Incidence"));
        center.add(new JLabel("delInc"));
        center.add(new JLabel("Distance [mm]"));
        center.add(new JLabel("delDist"));
        for (int i=0; i<t_n.length; i++){
            center.add(new JLabel(""+i));
            t_n[i]=new JTextField("0",8);
            center.add(t_n[i]);
            t_theta[i]=new JTextField(8);
            center.add(t_theta[i]);
            t_theta_s[i]=new JTextField(8);
            center.add(t_theta_s[i]);
            t_inc[i]=new JTextField(8);
            center.add(t_inc[i]);
            t_inc_s[i]=new JTextField(8);
            center.add(t_inc_s[i]);
            t_distance[i]=new JTextField(8);
            center.add(t_distance[i]);
            t_distance_s[i]=new JTextField(8);
            center.add(t_distance_s[i]);
        }
        me.add(center,BorderLayout.CENTER);
        
        JPanel south= new JPanel(new FlowLayout());
        south.add(new JLabel("Number of Events"));
        t_events=new JTextField(12);
        south.add(t_events);
        south.add(new JLabel("Number of Hits"));
        t_hits=new JTextField(12);
        south.add(t_hits);
        me.add(south, BorderLayout.SOUTH);
        
        pack();
        show();
    }
    
    public void updateStrip(int strip, int counts, double th, double ths, double inc, double incs,
    double dist, double dists){
        t_n[strip].setText(""+counts);
        t_theta[strip].setText(nf.format(th));
        t_theta_s[strip].setText(nf.format(ths));
        t_inc[strip].setText(nf.format(inc));
        t_inc_s[strip].setText(nf.format(incs));
        t_distance[strip].setText(nf.format(dist));
        t_distance_s[strip].setText(nf.format(dists));
    }
    
    public void updateEventCount(int count, int hits){
        t_events.setText(""+count);
        t_hits.setText(""+hits);
    }
    
    public static void main(String [] args) {
        DetectorFrame df=new DetectorFrame(78, 0.0);
        df.updateStrip(6,50,150.764, 2.548, 1.234, 0.135, 88.0,0.5);
        df.updateEventCount(5439,345);
        System.out.println("Done.");
    }
}