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
package net.sourceforge.nukesim;
import net.sourceforge.nukesim.statistics.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LineFit extends JFrame implements ActionListener {

    /**
     * The <code>ContentPane</code> of this application window.
     */
    Container window;

    /**
     * The file menu.
     */
    JMenu filemenu;

    /**
     * Menu Items.
     */
    JMenuItem openData,saveData,saveOutput,exit;

    /**
     * Text Field for spread sheet size
     */
    JTextField numPointsJTF;

    /**
     * The number of data points
     */
    int numPoints;

    /**
     * The table for data entry.
     */
    JTable table;

    /**
     * Scrolling Panel for table.
     */
    JScrollPane dataPane;

    /**
     * The text area for output.
     */
    JTextArea outputTextArea;

    /**
     * The Scrollpane for output text.
     */
    JScrollPane outputPane;

    /**
     * Constructor.
     */
    public LineFit () {
      super("Line Fit Utility");
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
              public void windowClosed(WindowEvent e){
                System.exit(0);
              }
            }
        );
      try{
        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
      } catch (Exception ie) {
        System.out.println("Couldn't find look.\n"+ie);
      }
      window=getContentPane();

        Box windowPanes = Box.createVerticalBox();
        window.add(windowPanes);

        //JPanel menuPane = new JPanel(new FlowLayout(FlowLayout.LEFT),true);
        
        JMenuBar menuBar=new JMenuBar();
        //menuPane.add(menuBar);
        JMenu fileMenu=new JMenu("File");
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        openData = new JMenuItem("Open Data...");
        saveData = new JMenuItem("Save Data...");
        saveOutput = new JMenuItem("Save Output...");
        exit = new JMenuItem("Exit");
        
        fileMenu.add(openData);
        fileMenu.addSeparator();
        fileMenu.add(saveData);
        fileMenu.add(saveOutput);
        fileMenu.addSeparator();
        fileMenu.add(exit); 
        
        
        JPanel controlPane = new JPanel(true);//double buffered
        controlPane.setLayout(new BoxLayout(controlPane,BoxLayout.X_AXIS));    numPointsJTF = new JTextField(3);
    JLabel numPointsLabel = new JLabel("Number of Data Points");
    numPointsLabel.setLabelFor(numPointsJTF);
        //JButton update = new JButton("Update");
        //update.setActionCommand("update");
        //update.addActionListener(this);
        //controlPane.add(update);
        JButton go = new JButton("Do Fit");
        go.setActionCommand("go");
        go.addActionListener(this);
        controlPane.add(Box.createRigidArea(new Dimension(10,0)));
        controlPane.add(numPointsLabel);
        controlPane.add(Box.createRigidArea(new Dimension(10,0)));
        controlPane.add(numPointsJTF);
        controlPane.add(go);
         
        numPoints=50;
        setInput(numPoints);
        table.setPreferredScrollableViewportSize(new Dimension(320,200));
        dataPane = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        outputTextArea = new JTextArea(10,80);
        //outputTextArea.setPreferredScrollableViewportSize(new Dimension(320,200));
        outputPane = new JScrollPane(outputTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                
        
        //windowPanes.add(menuPane);  
        windowPanes.add(controlPane);
        windowPanes.add(dataPane);
        windowPanes.add(outputPane);
        pack();
        setResizable(true);
        setVisible(true);     
    }

    public void actionPerformed(ActionEvent e){
        if (e.getActionCommand().equals("update")){
            int n = Integer.parseInt(numPointsJTF.getText().trim());
            System.out.println("Update Pressed: "+n);
            setInput(n);
            dataPane.removeAll();
            repaint();
        } else if (e.getActionCommand().equals("go")){
            System.out.println("go");
            go();
        }
    }

    private void setInput(int n) {
        String [][] rowData;
        
        String [] columnNames = {"x","dx","y","dy","fit - y"};
        rowData=new String[n][columnNames.length];
        if (table==null){
            for (int i=0;i<n;i++){
                for (int j=0; j<columnNames.length; j++){
                    rowData[i][j]=new String(""+0.0);
                }
            }
        } /*else if (numPoints<n){
            rowData = new String[n][columnNames.length];
            for (int i=0;i<numPoints;i++){
                for (int j=0;j<columnNames.length;j++){
                    rowData[i][j]=(String)table.getValueAt(i,j);
                }
            }
        }*/
        table = new JTable(rowData,columnNames);
        numPoints=n;
    }
    
    private void go() {
        numPoints = Integer.parseInt(numPointsJTF.getText().trim());
        double [] x=new double[numPoints];
        double [] dx=new double[numPoints];
        double [] y=new double[numPoints];
        double [] dy=new double[numPoints];
        for (int i=0;i<numPoints;i++){
            x[i]=getIt(i,0);
            dx[i]=getIt(i,1);
            y[i]=getIt(i,2);
            dy[i]=getIt(i,3);
            //outputTextArea.append(x[i]+" +- "+dx[i]+", "+y[i]+" +- "+dy[i]+"\n");
       }
       LinearFitErrXY lfxy=new LinearFitErrXY();
       double [] xprime; 
       try{
            double delx=lfxy.getTranslation(x,dx);
            outputTextArea.append("y = a + b (x-x0)\n\n");
            outputTextArea.append("x0 = "+delx+"\n");
            xprime=lfxy.translate(x,dx);
            lfxy.doFit(xprime,y,dx,dy);
            outputTextArea.append("a = "+lfxy.a+" +- "+lfxy.siga+"\n");
            outputTextArea.append("b = "+lfxy.b+" +- "+lfxy.sigb+"\n\n");
            outputTextArea.append("degrees of freedom = "+lfxy.dof+"\n");
            outputTextArea.append("chisq/dof = "+lfxy.chi2/lfxy.dof+"\n");
            outputTextArea.append("goodness-of-fit probability = "+lfxy.q+"\n");
            LinearFunction lf=new LinearFunction(lfxy.a,lfxy.siga,
                    lfxy.b,lfxy.sigb,delx);
            if ((lfxy.chi2/lfxy.dof) > 1.0) {
                outputTextArea.append("Chisq value is high (>1.0).\n");
                double factor=Math.sqrt(lfxy.chi2/lfxy.dof);
                outputTextArea.append("Multiplying errors by "+factor+"\n");
                lfxy.siga=lfxy.siga*factor;
                lfxy.sigb=lfxy.sigb*factor;
                outputTextArea.append("siga = "+lfxy.siga+"\n");
                outputTextArea.append("sigb = "+lfxy.sigb+"\n");
            }
            outputTextArea.append("\n\n");
            lf=new LinearFunction(lfxy.a,lfxy.siga,
                    lfxy.b,lfxy.sigb,delx);
            for (int i=0;i<numPoints;i++){
                table.setValueAt(new Double(lf.valueAt(x[i])),i,4);
            }
        } catch (StatisticsException se) {
            System.err.println();
            System.err.println(se);
        } 
    }
    
    private double getIt(int i, int j){
        return (Double.valueOf((String)table.getValueAt(i,j))).doubleValue();
    }
            
    
    public static void main(String args[]) {
        new LineFit();   
    }
}

