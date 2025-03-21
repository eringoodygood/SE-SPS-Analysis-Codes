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
/*
 * SpectrumCanvas.java
 *
 * Created on October 23, 2001, 4:03 PM
 */

package dwvisser.nuclear.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import javax.swing.*;

/**
 *
 * @author  dwvisser
 */
public class SpectrumCanvas extends JPanel {

    double[] radii,Ex;
    RadiusRange range;
    
    /** Creates new form SpectrumCanvas */
    public SpectrumCanvas(RadiusRange rr) {
        initComponents();
        this.range=rr;
        setBackground(Color.white);
        setBorder(BorderFactory.createLineBorder(Color.blue,1));
        setOpaque(true);
        this.setPreferredSize(new Dimension(500,50));
    }
    
    public void setRadii(double [] radii){
        this.radii=radii;
    }
    
    public void setEx(double [] Ex){
        this.Ex=Ex;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        setLayout(new java.awt.BorderLayout());

    }//GEN-END:initComponents

    public void paintComponent(Graphics graphics) {
        final Graphics2D g2=(Graphics2D)graphics;
        final FontRenderContext frc=g2.getFontRenderContext();
        final Dimension d=getSize();
        double length=range.getRhoMax()-range.getRhoMin();
        g2.setPaint(Color.darkGray);
        double lastXcoord=d.width;
        for (int i=0; i<radii.length;i++){
            if (radii[i] > range.getRhoMin() && radii[i] < range.getRhoMax()){
                String sEx = Ex[i]+" ";
                double xfrac = (radii[i]-range.getRhoMin())/length;
                double xcoord = xfrac*d.width;
                double diff = lastXcoord-xcoord;
                Rectangle2D stringBounds=g2.getFont().getStringBounds(sEx,frc);
                double lineBottom;
                if (diff >= stringBounds.getWidth()) {
                    g2.drawString(sEx,(float)xcoord,(float)(d.height-
                    stringBounds.getHeight()/10));
                    lastXcoord=xcoord;
                    lineBottom = d.height-stringBounds.getHeight();
                } else {
                    lineBottom = d.height-2*stringBounds.getHeight();
                }
                final Line2D line = new Line2D.Double(xcoord,0,xcoord,lineBottom);
                g2.draw(line);
            }
        }
    }    
}
