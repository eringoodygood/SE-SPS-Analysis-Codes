package dwvisser.analysis.spanc.tables;
import javax.swing.table.*;
import dwvisser.nuclear.*;
import dwvisser.analysis.spanc.*;

/**
 * Data model for <code>TargetDefinitionTable</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W. Visser</a>
 * @version 1.0
 */
public class TargetDefinitionTableModel extends DefaultTableModel {
    static String [] headers={"Layer","Components","Thickness [ug/cm^2]"};
    Class []  columnClasses={Integer.class,String.class,Double.class};
    Target _target;
    Object [] defaultRowData = {new Integer(0), "C 1", new Double(20.0)};
    
    public TargetDefinitionTableModel(Target target) {
        super(headers,target.getNumberOfLayers());
        _target=target;
        setLayerNumbers();
        setComponentsColumn();
        setThicknesses();
    }    
    
    public TargetDefinitionTableModel(){
        super(headers,0);
        addRow(defaultRowData);
    }
    
    
        
    private void setLayerNumbers(){
        for (int i=0; i < _target.getNumberOfLayers(); i++){
            setValueAt(new Integer(i),i,0);
        }
    }
    
    private void setComponentsColumn(){
        for (int i=0; i < _target.getNumberOfLayers(); i++){
            setValueAt(_target.getLayer(i).getText(),i,1);
        }
    }
    
    private void setThicknesses(){
        for (int i=0; i < _target.getNumberOfLayers(); i++){
            setValueAt(new Double(_target.getLayer(i).getThickness()),i,2);
        }
    }
    
    Target makeTarget(String name) throws NuclearException {
        double thickness=20;
        Target rval = new Target(name);
        for (int row=0; row < getRowCount(); row++){
            Object spec = getValueAt(row,1);
            //System.out.println(getValueAt(row,2).getClass().getName());
            Object o_thickness = getValueAt(row,2);
            if (o_thickness instanceof Double) {
                thickness = ((Double)o_thickness).doubleValue();
            } else if (o_thickness instanceof String) {
                thickness = Double.parseDouble((String)o_thickness);
            } else {
                System.err.println("Thickness not String or Double?!:"+o_thickness.getClass().getName()+
                "; Setting thickness to default: "+thickness);
            }
            Solid layer=new Solid((String)spec,thickness);
            rval.addLayer(layer);
        }
        return rval;
    }
            
    void addRow(){
        addRow(defaultRowData);
        renumberLayers();
    }
    
    private void renumberLayers(){
        for (int row=0; row < getRowCount(); row++) 
            setValueAt(new Integer(row),row,0);
    }
    
    void removeLayer(int row){
        removeRow(row);
        renumberLayers();
    }
    
}