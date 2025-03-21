package dwvisser.analysis.spanc.tables;
import javax.swing.table.*;
import dwvisser.nuclear.*;
import dwvisser.analysis.spanc.*;
import java.util.Vector;

/**
 * Data model for <code>ReactionTable</code>.
 *
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class ReactionTableModel extends DefaultTableModel {
    static String [] headers={"Reaction","Beam","Energy [MeV]", "B [kG]","Target",
    "Interaction Layer","Target Nuclide","Projectile", "Residual",
    "Q [e]", "Theta [deg]"};
    Class []  columnClasses={Integer.class,String.class,Double.class,Double.class,
    Target.class, Integer.class, String.class,
    String.class, String.class, Boolean.class, Integer.class, Double.class};
    
    public ReactionTableModel() throws KinematicsException{
        super(headers,0);
    }    
       
    private void iterateTable() throws KinematicsException {
        SpancReaction [] rxns = SpancReaction.getAllReactions();
        for (int i = 0; i< rxns.length; i++){
            addRow(rxns[i]);
        }
    }
    
    static java.text.DecimalFormat df = new java.text.DecimalFormat("0.#");    
    public void addRow(SpancReaction sr) {
        Vector temp = new Vector(headers.length);
        temp.addElement(new Integer(getRowCount()));
        String beamString = sr.getBeam().toString();
        if (sr.getBeamUncertain()) {
            beamString += " "+(char)0xb1 + " " + df.format(sr.getBeam().getMass().error*1000) + " keV";
        }
        temp.addElement(beamString);
        temp.addElement(new Double(sr.getEbeam()));
        temp.addElement(new Double(sr.getBfield()));
        temp.addElement(sr.getTarget().getName());
        temp.addElement(new Integer(sr.getInteractionLayer()));
        String targetString = sr.getTargetNuclide().toString();
        if (sr.getTargetUncertain()) {
            targetString += " "+(char)0xb1 + " " + df.format(sr.getTargetNuclide().getMass().error*1000) + " keV";
        }
        temp.addElement(targetString);
        String projectileString = sr.getProjectile().toString();
        if (sr.getProjectileUncertain()) {
            projectileString += " "+(char)0xb1 + " " + df.format(sr.getProjectile().getMass().error*1000) + " keV";
        }
        temp.addElement(projectileString);
        String residualString = sr.getResidual().toString();
        if (sr.getResidualUncertain()) {
            residualString += " "+(char)0xb1 + " " + df.format(sr.getResidual().getMass().error*1000) + " keV";
        }
        temp.addElement(residualString);
        temp.addElement(new Integer(sr.getQ()));
        temp.addElement(new Double(sr.getTheta()));
        addRow(temp);
    }
    
    private void addEmptyRow(){
        Object [] temp = new Object[9];
        addRow(temp);
    }
    
    void refreshData(){
       while (getRowCount()>0) {
            removeRow(0);
       }
       SpancReaction [] rxns = SpancReaction.getAllReactions();
       for (int i=0; i<rxns.length;i++) addRow(rxns[i]);
    }

        
    /*public void notifyOfChange() throws KinematicsException{
        iterateTable();
    }*/
}