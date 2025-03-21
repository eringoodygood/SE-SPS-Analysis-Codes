package dwvisser.nuclear.table;
import javax.swing.table.*;
import dwvisser.nuclear.*;

/**
 * Data model for <code>KinematicsOutputTable</code>.
 * 
 * @author  <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public class KinematicsOutputTableModel extends AbstractTableModel implements
ReactionTableClient {
    String [] headers={"T(1)","Ex(4)","Lab Deg(3)",
        "CM Deg(3)", "T(3)", "Lab Deg(4)","T(4)", "Jac(3)",
    "k(3)","z", "QBrho(3)"};
    Class []  columnClasses={Double.class,Double.class,Double.class,
        Double.class,Double.class,Double.class,
        Double.class,Double.class,Double.class,
        Double.class,Double.class
    };
    Object [][] data = new Object[1][headers.length];
    double [] beamEnergies,residExcite,labAngles;
    Reaction reaction;
    EnergyLoss energyLoss;

    public KinematicsOutputTableModel(ReactionTable rt, double [] beamEnergies,
    double [] residExcite, double [] labAngles) throws KinematicsException{
        super();
        this.beamEnergies=beamEnergies;
        this.residExcite=residExcite;
        this.labAngles=labAngles;
//        new EnergyLoss();//called to initialize energy loss routines
        rt.setReactionTableClient(this);
        iterateTable();
        setValueAt(data[0][0],0,0);
    }
    
    /**
     * Sets reaction along with target thickness in ug/cm^2.
     */
    public void setReaction(Reaction r, double thickness) 
    throws KinematicsException{
        reaction=r;
        try {
            if (thickness != 0.0){
                energyLoss = new EnergyLoss(new Solid(thickness,Absorber.MICROGRAM_CM2,
                r.getTarget().getElementSymbol()));
            } else {
                energyLoss=null;
            }
        } catch (NuclearException ne) {
            System.err.println(getClass().getName()+".setReaction(): "+ne);
        }
        iterateTable();
    }
    
    public void setTargetThickness(double thickness) throws KinematicsException{
        try {
            if (thickness != 0.0){
                energyLoss = new EnergyLoss(new Solid(thickness,Absorber.MICROGRAM_CM2,
                reaction.getTarget().getElementSymbol()));
                //System.out.println("Target thickness set to: "+thickness);
            } else {
                energyLoss =null;
            }
        } catch (NuclearException ne) {
            System.err.println(getClass().getName()+".setReaction(): "+ne);
        }
        iterateTable();
    }

    public void setBeamEnergies(double [] be) throws KinematicsException {
        beamEnergies=be;
        iterateTable();
    }

    public void setResidualExcitations(double [] re) throws KinematicsException {
        residExcite=re;
        iterateTable();
    }

    public void setLabAngles(double [] la) throws KinematicsException {
        labAngles=la;
        iterateTable();
    }

    public int getRowCount(){
        return data.length;
    }

    public int getColumnCount() {
        return data[0].length;
    }

public Class getColumnClass(int c) {return columnClasses[c];}
public String getColumnName(int c) {return headers[c];}
    public boolean isCellEditable(int r, int c){
        return false;
    }

    public Object getValueAt(int r, int c){
        return data[r][c];
    }

    public void setValueAt(Object value, int r, int c){
        data[r][c]=value;
    }

    private void iterateTable() throws KinematicsException {
        double [] effectiveBeamEnergy=new double[beamEnergies.length]; 
        double qbr;
        Reaction [] reactions=new Reaction[beamEnergies.length*residExcite.length*labAngles.length];
        int numRows=0;
        int counter=0;
        for (int i=0; i< beamEnergies.length; i++){
            if (energyLoss != null){
                effectiveBeamEnergy[i]=beamEnergies[i]-0.001*
                    energyLoss.getThinEnergyLoss(reaction.getBeam(),beamEnergies[i]);
            } else {
                effectiveBeamEnergy[i]=beamEnergies[i];
            }
            for (int j=0; j<residExcite.length; j++){
                for (int k=0; k<labAngles.length; k++){
                    reactions[counter]=new Reaction(reaction,
                    effectiveBeamEnergy[i],
                    labAngles[k],residExcite[j]);
                    numRows += reactions[counter].getAngleDegeneracy();
                    counter++;
                }
            }
        }
        data = new Object[numRows][headers.length];
        counter=0;
        int row=0;
        for (int i=0; i< beamEnergies.length; i++){
            for (int j=0; j<residExcite.length; j++){
                for (int k=0; k<labAngles.length; k++){
                    for (int l=0; l<reactions[counter].getAngleDegeneracy();  l++){
                        setValueAt(new Double(effectiveBeamEnergy[i]),row,0);
                        setValueAt(new Double(residExcite[j]),row,1);
                        setValueAt(new Double(labAngles[k]),row,2);
                        setValueAt(new Double(reactions[counter].getCMAngleProjectile(l)),row,3);
                        double Tproj= reactions[counter].getLabEnergyProjectile(l);
                        if (energyLoss != null) {
                            Tproj = Tproj - 0.001 * energyLoss.getThinEnergyLoss(reaction.getProjectile(),
                            Tproj,Math.toRadians(labAngles[k]));
                        }
                        setValueAt(new Double(Tproj),row,4);
                        setValueAt(new Double(reactions[counter].getLabAngleResidual(l)),row,5);
                        setValueAt(new Double(reactions[counter].getLabEnergyResidual(l)),row,6);
                        setValueAt(new Double(reactions[counter].getJacobianProjectile(l)),row,7);
                        double kP = reactions[counter].getFocusParameter(l);
                        setValueAt(new Double(kP),row,8);
                        setValueAt(new Double(50.12-57.01*Math.abs(kP)),row,9);
                        if (energyLoss != null){
                            qbr = Reaction.getQBrho(reaction.getProjectile(),Tproj);
                        } else {
                            qbr = reactions[counter].getQBrho(l);
                        }
                        setValueAt(new Double(qbr),row,10);
                        row++;
                    }
                    counter++;
                }
            }
        }
        fireTableDataChanged();
    }
}
