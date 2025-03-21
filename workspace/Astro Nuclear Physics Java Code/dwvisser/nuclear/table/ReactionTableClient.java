package dwvisser.nuclear.table;
import dwvisser.nuclear.*;

/**
* There is one reaction client per reaction table, which receives the reaction
* object from it.
*
* @author
* @version
*/
public interface ReactionTableClient {

  public void setReaction(Reaction r, double thickness) throws KinematicsException;

}

