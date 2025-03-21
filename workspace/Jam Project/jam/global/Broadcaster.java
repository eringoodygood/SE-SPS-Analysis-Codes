package jam.global;
import java.util.Observable;

/**
 * Part of a client server to handle message between packages
 * Broadcast events to all listeners.
 * These listeners must have already registered that they would
 * like to receive events using Broadcaster.addObserver()
 * 
 * @author Ken Swartz
 */
public final class Broadcaster extends Observable {
	
	static private final Broadcaster broadcaster=new Broadcaster();
	
	static public Broadcaster getSingletonInstance(){
		return broadcaster;
	}
	
	private Broadcaster(){
		super();
	}

    /** 
     * Broadcast an event to all registered observers. The necessary
     * synchronization is already taken care of by 
     * <code>Observable.notifyObservers(Object)</code>.
     * 
     * @param command an <CODE>int</CODE> from <CODE>BroadcastEvent</CODE>
     * @param param a parameter to be wrapped in the <CODE>BroadcastEvent</CODE>
     * object
     */
    public void broadcast(int command, Object param) {
        Object broadcastEvent=new BroadcastEvent(command, param);
        setChanged();//necessary for next line to work
        notifyObservers(broadcastEvent);//automatically calls clearChanged()
    }

    /** 
     * Broadcast an event to all registered observers. Calls
     * <code>broadcast(int, null)</code>.
     * 
     * @param command code from <CODE>BroadcastEvent</CODE>, I presume
     */
    public void broadcast(int command) {
        broadcast(command,null);
    }    
}