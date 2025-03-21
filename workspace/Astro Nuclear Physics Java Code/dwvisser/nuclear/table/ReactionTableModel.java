package dwvisser.nuclear.table;
import javax.swing.table.*;
import dwvisser.nuclear.*;
import dwvisser.math.*;

/**
 *
 * @author Dale Visser
 * @version 1.0
 */
public class ReactionTableModel extends AbstractTableModel {
    Reaction react;
    ReactionTableClient rtc;
String [] headers={"","A,Z","Mass [Mev]"};
Class []  columnClasses={String.class,Object.class,UncertainNumber.class};
//used Object.class instead of Nucleus.class to get editing working
    Object [][] data = {
    {"Target(1)",new Nucleus(6,12), new UncertainNumber(0.0,0.0)},
    {"Beam(2)", new Nucleus(6,12), new UncertainNumber(0.0,0.0)},
    {"Projectile(3)", new Nucleus(2,4), new UncertainNumber(0.0,0.0)},
    {"Residual(4)", new Nucleus(10,20),new UncertainNumber(0.0,0.0)},
    {"Q0",null,new UncertainNumber(0.0,0.0)}
    };

    public ReactionTableModel(){
        super();
        setValueAt(data[0][0],0,0);
    }

    public int getRowCount(){
        return data.length;
    }

    public int getColumnCount() {
        return data[0].length;
    }

    public Class getColumnClass(int c) {
        return columnClasses[c];
    }

    public String getColumnName(int c) {
        return headers[c];
    }

    public boolean isCellEditable(int r, int c){
        //System.out.print("Row "+r+", Col "+c+": ");
        if (c != 1 || r > 2) {
            //System.out.println("not");
            return false;
        }
        //System.out.println("editable");
        return true;
    }

    public Object getValueAt(int r, int c){
        return data[r][c];
    }

    public void setValueAt(Object value, int r, int c){
        if (c==1) {
            if (value instanceof String) {
                data[r][c]=Nucleus.parseNucleus((String)value);
            } else if (value instanceof Nucleus) {
                data[r][c]=value;
            }
        } else {
            data[r][c]=value;
        }
        Nucleus target=(Nucleus)data[0][1];
        Nucleus beam=(Nucleus)data[1][1];
        Nucleus projectile=(Nucleus)data[2][1];
        try {
            react=new Reaction(target,beam,projectile);
            if (rtc != null) {
                rtc.setReaction(react,0.0);
            }
        } catch (KinematicsException ke) {
            System.err.println(ke);
        }
        data[3][1]=react.getResidual();
        data[4][2]=react.getQValue();
        for (int i=0;i<4;i++){
            data[i][2]=((Nucleus)data[i][1]).getMass();
        }
        fireTableDataChanged();
    }

    public void setReactionTableClient(ReactionTableClient rtc)
    throws KinematicsException{
        this.rtc=rtc;
        if (rtc == null) {
            System.err.println("RTM.setRTC():should have rxn at this point");
        } else {
            rtc.setReaction(react,0.0);
        }
    }
}
