package dwvisser.nuclear.table;
import javax.swing.*;

/** 
 * Table for displaying results of nuclear reaction kinematics calculations.
 * Used by JRelKin.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public class KinematicsOutputTable extends JTable {
    
    /** 
     * Constructor.
     * @param kotm Data Model for this table
     */
    public KinematicsOutputTable(KinematicsOutputTableModel kotm){
        super(kotm);
        setOpaque(true);
    }
}
