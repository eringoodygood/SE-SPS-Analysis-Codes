/*
 * Beam.java
 *
 * Created on October 19, 2001, 11:00 PM
 */

package dwvisser.nuclear;

/**
 *
 * @author  dwvisser
 * @version 
 */
public class Beam   {

    public Nucleus nucleus;
    public double energy;
    
    /** Creates new Beam */
    public Beam(Nucleus nucleus, double energy) {
        this.nucleus=nucleus;
        this.energy=energy;
    }

}
