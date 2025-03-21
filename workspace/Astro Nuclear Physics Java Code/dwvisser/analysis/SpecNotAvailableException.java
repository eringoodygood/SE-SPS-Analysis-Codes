/*
 */
package dwvisser.analysis;
/**
 * Exception that is throw if there is a fit exception that can be
 * handled inside fit
 *
 */
public class SpecNotAvailableException extends Exception{

    public SpecNotAvailableException(String msg){
	super(msg);
    }
    
    public SpecNotAvailableException(int det, int strip){
        super("No spectrum for detector "+det+", strip "+strip+" specified.");
    }
    
}