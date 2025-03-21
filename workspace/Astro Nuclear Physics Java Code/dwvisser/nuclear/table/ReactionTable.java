package dwvisser.nuclear.table;
import javax.swing.*;
import dwvisser.nuclear.*;

/** 
 * Table at top of JRelKin window showing the reaction.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 * @version 1.0
 */
public class ReactionTable extends JTable {

    /** data model
     */
    ReactionTableModel rtm;

    /** Creates the table.
     * @param rtm data model
     */
    public ReactionTable(ReactionTableModel rtm){
        super(rtm);
        this.rtm=rtm;
        setOpaque(true);
        //setDefaultRenderer(Nucleus.class, new NucleusRenderer());
    }

    /** Sets the client for data.
     * @param rtc receiver
     * @throws KinematicsException if something goes wrong
     */
    public void setReactionTableClient(ReactionTableClient rtc) throws
    KinematicsException {
        rtm.setReactionTableClient(rtc);
    }

}
