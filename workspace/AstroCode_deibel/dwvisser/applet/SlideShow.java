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
package dwvisser.applet;
import java.applet.Applet;
import java.awt.*;

/**
 * Silly little applet for a picture slide show.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 */
public class SlideShow extends Applet implements Runnable {

	int numImages;
	Image[] images;
	int[] widths;
	int[] heights;
	Image screenImage;
	int widthI, heightI;
	int delay;
	Thread thread;
	double aspectRatio;
	boolean random;
	int[] order;

	/**
	 * Initializes the applet.  You never need to call this directly; it is
	 * called automatically by the system once the applet is created.
	 */
	public void init() {
		Color background = getColorParam("background");
		if (background != null) {
			setBackground(background);
		} else {
			System.out.println("Background color=null");
		}
		aspectRatio = getSize().width / getSize().height;
		numImages = getIntParam("numImages");
		images = new Image[numImages];
		heights = new int[numImages];
		widths = new int[numImages];
		order = new int[numImages];
		for (int i = 0; i < numImages; i++)
			order[i] = i;
		random = (getParameter("randomize").equals("true"));
		//for (int i=0; i < numImages; i++) {
		//getImageParam("image"+(i+1),i); // inits width and height, too
		//}
		delay = getIntParam("delayInSeconds") * 1000;
	}

	/**
	 * Called to start the applet.  You never need to call this directly; it
	 * is called when the applet's document is visited.
	 */
	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			//r=new Random();
			thread.start();
		}
	}

	public void run() {
		boolean alreadyLoaded = false;
		int[] sequence = new int[numImages];
		while (thread != null) {
			if (random) {
				sequence = randomSeries(order);
			} else {
				sequence = order;
			}
			if (!alreadyLoaded) {
				// inits width and height, too
				getImageParam("image" + (sequence[0] + 1), sequence[0]);
			}
			for (int i = 0; i < numImages; i++) {
				//System.out.println("i at "+i+" of "+ (numImages-1));
				screenImage = images[sequence[i]];
				widthI = widths[sequence[i]];
				heightI = heights[sequence[i]];
				repaint();
				if ((!alreadyLoaded) && (i < (numImages - 1))) {
					getImageParam(
						"image" + (sequence[i + 1] + 1),
						sequence[i + 1]);
				}
				try {
					Thread.sleep(delay);
				} catch (InterruptedException ie) {
				}
			}
			alreadyLoaded = true;
		}
	}

	/**
	 * Called to stop the applet.  This is called when the applet's document is
	 * no longer on the screen.  It is guaranteed to be called before destroy()
	 * is called.  You never need to call this method directly
	 */
	public void stop() {
		thread = null;
	}

	//override to avoid flickering
	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		g.clearRect(0, 0, getSize().width, getSize().height);
		g.drawImage(
			screenImage,
			(getSize().width - widthI) / 2,
			(getSize().height - heightI) / 2,
			widthI,
			heightI,
			this);
	}

	/**
	 * Cleans up whatever resources are being held.  If the applet is active
	 * it is stopped.
	 */
	public void destroy() {
	}

	private int getIntParam(String paramName) {
		return Integer.parseInt(getParameter(paramName));
	}

	private void getImageParam(String paramName, int num) {
		int h, w; //height, width of image
		Image t2;
		int hn = 0;
		int wn = 0;
		//System.out.println("getImageParam('"+paramName+"', "+num+")");
		Image temp = getImage(getDocumentBase(), getParameter(paramName));
		//System.out.println("temp="+temp);
		do {
			h = temp.getHeight(this);
		} while (h == -1);
		do {
			w = temp.getWidth(this);
		} while (w == -1);
		double imageAspect = (double) w / (double) h;
		if (imageAspect < aspectRatio) {
			hn = getSize().height;
			wn = Math.round((float) hn / (float) h * (float) w);
		} else {
			wn = getSize().width;
			hn = Math.round((float) wn / (float) w * (float) h);
		}
		t2 = temp.getScaledInstance(wn, hn, Image.SCALE_SMOOTH);
		images[num] = t2;
		heights[num] = hn;
		widths[num] = wn;
	}

	//Scrambles the contents of an int array randomly
	private int[] randomSeries(int[] in) {
		if (in.length == 1)
			return in;
		int first = (int) Math.round(Math.floor(Math.random() * in.length));
		int[] out = new int[in.length];
		int[] temp = new int[in.length - 1];
		out[0] = in[first];
		if (first > 0) {
			System.arraycopy(in, 0, temp, 0, first);
			//copy up to first int temp
			if (first < (in.length - 1)) { //more to copy
				System.arraycopy(
					in,
					first + 1,
					temp,
					first,
					in.length - first - 1);
			}
		} else { //just copy the rest
			System.arraycopy(in, 1, temp, 0, in.length - 1);
		}
		System.arraycopy(randomSeries(temp), 0, out, 1, temp.length);
		return out;
	}

	private Color getColorParam(String name) {
		String value = getParameter(name);
		try {
			return new Color(Integer.parseInt(value, 16));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String[][] paramInfo =
		{ { "background", "Hex RGB color value", "Background color" }, {
			"numImages", "integer", "Number of images" }, {
			"image#",
				"gif or jpeg image file",
				"file in same directory as page" },
				{
			"randomize", "true or false", "use random order or not" }, {
			"delayInSeconds", "integer", "time to display each image" }
	};

	public String[][] getParameterInfo() {
		return paramInfo;
	}

}
