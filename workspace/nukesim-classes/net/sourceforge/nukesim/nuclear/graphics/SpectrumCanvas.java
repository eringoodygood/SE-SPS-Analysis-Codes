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

package net.sourceforge.nukesim.nuclear.graphics;
import jade.physics.Energy;
import jade.physics.Length;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.sourceforge.nukesim.nuclear.NukeUnits;

/**
 * Graphically displays radii of states in a reaction resonance for a 
 * spectrograph measurement.
 *
 * @author  Dale Visser
 */
public class SpectrumCanvas extends JPanel implements NukeUnits {

	private static final Color DARK_RED=new Color(192,0,0);
    private Length[] radii;
	private Energy[] excitation;
    private RadiusRange range;
    private boolean fakeExcitations=false;
    
    /** Creates new form SpectrumCanvas */
    public SpectrumCanvas(RadiusRange rr) {
		setLayout(new java.awt.BorderLayout());
        range=rr;
		setOpaque(true);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1,1,0,1,Color.BLACK));
        setPreferredSize(new Dimension(500,50));
    }
    
    public void setRadii(Length [] r){
        radii=r;
    }
    
    public void setEx(Energy [] excite, boolean fake){
        excitation=excite;
        fakeExcitations=fake;
    }

    public void paintComponent(Graphics graphics) {
    	super.paintComponent(graphics);
        final Graphics2D g2=(Graphics2D)graphics;
        final FontRenderContext frc=g2.getFontRenderContext();
        final Dimension d=getSize();
        final Length length=Length.lengthOf(range.getRhoMax().subtract(range.getRhoMin()));
        double lastStringX=d.width;
        boolean outOfRange=true;
        final int xmid=d.width/2;
        final int ymid=d.height/2;
        if (fakeExcitations){
        	final String s="No excitations in table.";
        	final float fontSize=g2.getFont().getSize();
        	g2.setFont(g2.getFont().deriveFont((float)(2*fontSize)));
        	g2.setPaint(Color.PINK);
        	final Rectangle bounds=g2.getFont().getStringBounds(
			s,frc).getBounds();
        	g2.drawString(s,xmid-bounds.width/2,ymid+bounds.height/2);
        	g2.setFont(g2.getFont().deriveFont(fontSize));
        }
        for (int i=0; i<radii.length;i++){
            if (radii[i].doubleValue(cm) > range.getRhoMin().doubleValue(cm) && 
            		radii[i].doubleValue(cm) < range.getRhoMax().doubleValue(cm)){
            	outOfRange=false;
                final String sEx = " "+
				String.valueOf((int)Math.round(excitation[i].doubleValue(keV)))+" ";
                final double xfrac = radii[i].subtract(range.getRhoMin()).divide(length).doubleValue();
                final double xcoord = xfrac*d.width;
                final Rectangle2D stringBounds=g2.getFont().getStringBounds(sEx,frc);
                final double width=stringBounds.getWidth();
				final double stringX=xcoord-width/2;
				final double diff = lastStringX-stringX;
                if (diff >= width) {
					g2.setPaint(DARK_RED);
                    g2.drawString(sEx,(float)stringX,(float)(d.height-
                    stringBounds.getHeight()/10));
                    lastStringX=stringX;
                } else {
					g2.setPaint(Color.BLACK);
                }
				final double lineBottom = d.height-stringBounds.getHeight();
                final Line2D line = new Line2D.Double(xcoord,0,xcoord,lineBottom);
                g2.draw(line);
            }
        }
        if (outOfRange){
        	g2.setPaint(Color.RED);
        	final double rcm=radii[0].doubleValue(cm);
        	final boolean low=rcm<range.getRhoMin().doubleValue(cm);
        	final int space=5;
        	if (low){
        		final int r=(int)Math.ceil(rcm);
        		final String arrow="\u2190 "+radii.length+" states below "+r+
        		" cm";
        		g2.drawString(arrow,space,ymid);
        	} else {
        		final int r=(int)Math.floor(radii[radii.length-1].doubleValue(cm));
        		final String arrow=radii.length+" states above "+r+" cm \u2192";
        		final Rectangle bounds=g2.getFont().getStringBounds(arrow,frc).getBounds();
        		g2.drawString(arrow,d.width-bounds.width-space,ymid+bounds.height/2);
        	}
        }
    }    
}
