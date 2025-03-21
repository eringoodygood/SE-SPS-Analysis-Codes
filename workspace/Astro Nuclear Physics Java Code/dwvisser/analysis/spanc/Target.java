/*
 * Target.java
 *
 * Created on December 15, 2001, 2:57 PM
 */

package dwvisser.analysis.spanc;
import java.util.*;
import dwvisser.nuclear.*;
import javax.swing.DefaultListModel;
import javax.swing.DefaultComboBoxModel;

/**
 * This class represents a target in a splitpole experiment, possibly containing
 * more than one layer. It handles target energy loss calculations. Each target
 * is uniquely identified by a name.
 *
 * @author  Dale
 * @version
 */
public class Target implements java.io.Serializable {
    
    private Vector layers = new Vector(1,1);
    private Vector fullLosses = new Vector(1,1);
    private Vector halfLosses = new Vector(1,1);
    
    private String name;
    
    private static Hashtable targets = new Hashtable();
    private static DefaultListModel dlm_targets = new DefaultListModel();
    private static DefaultComboBoxModel dcbm_targets = new DefaultComboBoxModel();
    
    /** Creates new Target */
    public Target(String name) {
        this.name=name;
        addTargetToLists();
    }
    
    private void addTargetToLists(){
        targets.put(name,this);
        dlm_targets.addElement(this.name);
        dcbm_targets.addElement(this.name);
    }
    
    static public void removeTarget(Target t){
        targets.remove(t.getName());
        dlm_targets.removeElement(t.name);
        dcbm_targets.removeElement(t.name);
    }
    
    static public void refreshData(Collection retrievedTargets){
        Iterator iter = retrievedTargets.iterator();
        while (iter.hasNext()) {
            Target targ = (Target)iter.next();
            targets.put(targ.getName(),targ);
            dlm_targets.addElement(targ.name);
            dcbm_targets.addElement(targ.name);
        }
    }
    
    static public void removeAllTargets(){
        Iterator it_targ = targets.values().iterator();
        while(it_targ.hasNext()) {
            it_targ.next();
            it_targ.remove();
        }
        dlm_targets.removeAllElements();
        dcbm_targets.removeAllElements();
    }
            
    static public Target getTarget(String name){
        return (Target)targets.get(name);
    }
    
    public void addLayer(Solid layer) {
        layers.addElement(layer);
        fullLosses.addElement(new EnergyLoss(layer));
        Absorber half=layer.getNewInstance(0.5);
        halfLosses.addElement(new EnergyLoss(half));
    }
    
    void removeLayer(int index) {
        layers.removeElementAt(index);
    }
    
    double calculateInteractionEnergy(int interaction_layer, Nucleus beam,
    double Ebeam) {
        double rval=Ebeam;
        for (int i=0; i<interaction_layer; i++){
            rval -= 0.001*getFullLoss(i).getEnergyLoss(beam,rval);
        }
        rval -= 0.001*getHalfLoss(interaction_layer).getEnergyLoss(beam,rval);
        return rval;
    }
        
    private Solid getAbsorber(int layer){
        return (Solid)layers.elementAt(layer);
    }    
    private EnergyLoss getFullLoss(int layer){
        return (EnergyLoss)fullLosses.elementAt(layer);
    }
    private EnergyLoss getHalfLoss(int layer){
        return (EnergyLoss)halfLosses.elementAt(layer);
    }
    
    public int getNumberOfLayers(){
        return layers.size();
    }
    
    public Solid getLayer(int index){
        return (Solid)layers.elementAt(index);
    }
    
    static public DefaultListModel getTargetList(){
        return dlm_targets;
    }  
    
    static public DefaultComboBoxModel getComboModel(){
        return dcbm_targets;
    }
    
        
    double calculateProjectileEnergy(int interaction_layer, Nucleus projectile, 
    double Einit, double thetaRadians) {
        double rval=Einit;
        rval -= 0.001*getHalfLoss(interaction_layer).getEnergyLoss(projectile,
        Einit,thetaRadians);
        for (int i=interaction_layer+1; i < layers.size(); i++){
            rval -= 0.001*getFullLoss(i).getEnergyLoss(projectile,rval,thetaRadians);
        }
        return rval;
    }
    
    double calculateInitialProjectileEnergy(int interaction_layer, 
    Nucleus projectile,
    double Efinal, double thetaRadians) {
        double rval = Efinal;
        for (int i=layers.size()-1; i > interaction_layer; i--){
            rval = getFullLoss(i).reverseEnergyLoss(projectile,rval,
            thetaRadians);
        }
        rval = getHalfLoss(interaction_layer).reverseEnergyLoss(projectile, 
        rval, thetaRadians);
        return rval;
    }
    
    public String getName(){
        return name;
    }
    
    public DefaultComboBoxModel getLayerNumberComboModel(){
        DefaultComboBoxModel rval=new DefaultComboBoxModel();
        for (int i=0; i<layers.size(); i++){
            rval.addElement(new Integer(i));
        }
        return rval;
    }
    
    public DefaultComboBoxModel getLayerNuclideComboModel(int layerIndex){
        DefaultComboBoxModel rval=new DefaultComboBoxModel();
        int [] Z = getLayer(layerIndex).getElements();
        for (int i=0; i<Z.length; i++){
            Vector possible = Nucleus.getIsotopes(Z[i]);
            for (int j=0; j< possible.size(); j++){
                rval.addElement(possible.elementAt(j));
            }
        }
        return rval;
    }
    
    static public Collection getTargetCollection(){
        return targets.values();
    }
    
    public String toString(){
        String rval = "Target: "+name+"\n";
        for (int i=0; i < layers.size(); i++){
            rval += "Layer "+i+": Specification '";
            Solid l = getLayer(i);
            rval += l.getText()+"' "+l.getThickness()+" ug/cm^2\n";
        }
        return rval;
    }
    
    static public void main(String [] args){
//    	new EnergyLoss();
    	Target t=new Target("test");
    	try {
    		t.addLayer(new Solid("C 1",100));
    		//t.addLayer(new Solid("Si 1 O 2",170));
    	} catch (NuclearException ne){
    		System.err.println(ne);
    	}
    	Nucleus proj = new Nucleus(2,6);
    	int layer = 0;
    	double Einit = 31.6; double thetaRad=Math.toRadians(7.5);   
    	double Efinal = t.calculateProjectileEnergy(layer, proj, Einit, 
    	thetaRad);
    	System.out.println("Einit = "+Einit+" -> Efinal = "+Efinal);
    	Einit = t.calculateInitialProjectileEnergy(layer, proj, Efinal, 
    	thetaRad);
    	System.out.println("Efinal = "+Efinal+" -> Einit = "+Einit);
    }    	
    	
}
