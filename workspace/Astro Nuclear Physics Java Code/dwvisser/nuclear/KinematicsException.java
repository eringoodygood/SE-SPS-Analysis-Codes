package dwvisser.nuclear;


/**
 * Thrown in kinematically disallowed situations.
 */
public class KinematicsException extends NuclearException {

    public KinematicsException(String s) {
        super(s);
    }
}
