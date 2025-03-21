package jam.sort;
import jam.data.Gate;
import jam.data.Histogram;
import jam.data.Monitor;
import jam.sort.stream.EventException;
import jam.sort.stream.EventOutputStream;

/**
 * <p>Abstract class for sort routines which users extend with their 
 * own specific code.  Defines histograms, gates, scalers, monitors,
 * and parameters. The user writes a sort class which must have the 
 * following methods:</p>
 * <dl>
 * <dt>initialize</dt><dd>called when the sort process is 
 * initialized</dd>
 * <dt>sort</dt><dd>called for each event</dd>
 * <dt>monitor</dt><dd>called each time the monitors are updated</dd>
 * </dl>
 * <p>There are two online modes now.  One is hybrid VME/CAMAC
 * acquisition,and the other is totally VME bus.  In the first case,
 * the user must set a list of CAMAC commands.  In the second, a list
 * of addresses and thresholds for the VME bus.</p>
 *
 * @author Ken Swartz
 * @author Dale Visser
 * @version 1.4
 * @since 1.0
 * @see VME_Map
 * @see CamacCommands
 * @see jam.data.Histogram
 * @see jam.data.Monitor
 * @see jam.data.Gate
 */
public abstract class SortRoutine implements jam.global.Sorter  {

    /** 
     * Indicates the parameter count has been set implicitly by using 
     * CNAF commands.
     */
    public static final int SET_BY_CNAF=395;
    
    /**
     * Indicates the parameter count has been set implicitly by 
     * specifying a VME map.
     */
    public static final int SET_BY_VME_MAP=249;
    
	/**
	 * Indicates that the parameter count has been set explicitly.
	 */
	private static final int SET_EXPLICITLY=876;
    
    /**
     * constant to define a 1d histogram type int
     */
    protected final static int HIST_1D=Histogram.ONE_DIM_INT;

    /**
     * constant to define a 2d histogram type int
     */
    protected final static int HIST_2D=Histogram.TWO_DIM_INT;

    /**
     * constant to define a 1d histogram type int
     */
    protected final static int HIST_1D_INT=Histogram.ONE_DIM_INT;

    /**
     * constant to define a 2d histogram type int
     */
    protected final static int HIST_2D_INT=Histogram.TWO_DIM_INT;

    /**
     * constant to define a 1d histogram type double
     */
    protected final static int HIST_1D_DBL=Histogram.ONE_DIM_DOUBLE;

    /**
     * constant to define a 1d histogram type double
     */
    protected final static int HIST_2D_DBL=Histogram.TWO_DIM_DOUBLE;

    /**
     * constant used to define a  1 d gate
     */
    protected final static int GATE_1D=Gate.ONE_DIMENSION;

    /**
     * constant used to define a 2 d gate
     */
    protected final static int GATE_2D=Gate.TWO_DIMENSION;
    
    /**
     * constant used to define a scaler monitor
     * a monitor whose value is derived from a scaler
     */
    protected final static int MONI_SCAL=Monitor.SCALER;
    
    /**
     * constant used to define a gate monitor
     * a monitor whose value is derived from a gate
     */
    protected final static int MONI_GATE=Monitor.GATE;
    
    /**
     * constant used to define a sort monitor
     * a monitor whose value is derived in the sort routine
     */
    protected final static int MONI_SORT=Monitor.SORT;
    
    /** 
     * Indicates that the parameter count hasn't been set by any means.
     */
    private static final int INIT_NO_MODE=871;
    
    /**
     * Size of buffer to be used by event streams.
     */
    public int BUFFER_SIZE;
    
    /** 
     * Set to true when we're writing out events.
     * (For presorting?)
     */
    protected boolean writeOn;

    /**
     * class which is given list of cnafs
     * init(c,n,a,f);
     * event(c,n,a,f);
     */
    protected CamacCommands cnafCommands;

    /**
     * Object which contains the VME addressing instructions.
     */
    protected VME_Map vmeMap;

    /**
     * Output stream to send pre-processed events to.
     */
    protected EventOutputStream eventOutputStream=null;

    int eventSizeMode = INIT_NO_MODE;
    
    /**
     * Size of an event to be used for offline sorting.
     * The event size is the maximum number of paramters in an event.
     */
    private int eventSize;

	private final String classname=getClass().getName();
	private final static String colon=": ";
	private final static String illegalMode="Illegal value for event size mode: ";

    /**
     * Creates a new sort routine object.
     */
    public SortRoutine(){
        writeOn=false;
        cnafCommands=new CamacCommands(this);
        vmeMap = new VME_Map(this);
    }

    /**
     * Returns the object containing commands for the CAMAC 
     * controller.
     *
     * @return commands to be used while taking data from CAMAC bins
     */
    public final CamacCommands getCamacCommands() {
        return cnafCommands;
    }

    /** Hands Jam the object representing VME acquisition specifications.
     * @return the object containing directives for ADC's and TDC's
     */
    public final VME_Map getVME_Map(){
        return vmeMap;
    }

    /** 
     * Explicitly sets the size of a event for offline sorting. Used 
     * in the absence of CAMAC or VME specification of parameters.  
     * The user has to know what detector each parameter ID 
     * represents.
     *
     * @param size number of parameters per event
     * @throws SortException in case this has been called 
     * inappropriately
     */
    protected void setEventSize(int size) throws SortException {
        setEventSizeMode(SET_EXPLICITLY);
        synchronized (this){
        	eventSize=size;
        }
    }

    /** Sets how the event size is determined.  Generally not called 
     * explicitly by subclasses.
     *
     * @param mode how the event size is determined
     * @throws SortException if called inappropriately
     * @see #SET_EXPLICITLY
     * @see #SET_BY_CNAF
     * @see #SET_BY_VME_MAP
     */
    void setEventSizeMode(int mode) throws SortException {
    	final StringBuffer mess=new StringBuffer(classname).append(colon);
        if ((eventSizeMode != mode) && (eventSizeMode != INIT_NO_MODE)) {
        	final String s1="Illegal attempt to set event size a second time. "; 
        	final String s2="Already set to ";
        	final String s3=", and attempted to set to ";
            throw new SortException(mess.append(s1).append(s2).append(
            eventSizeMode).append(s3).append(mode).append('.').toString());
        }
        if (mode == SET_BY_CNAF || mode == SET_BY_VME_MAP || 
        mode == SET_EXPLICITLY) {
        	synchronized(this){
            	eventSizeMode = mode;
            }
        } else {
            throw new SortException(mess.append(illegalMode).append(
            mode).toString());
        }
    }

    /** 
     * Returns the mode by which the event size was set.
     * 
     * @return whether event size was set explicitly, by CAMAC specs, 
     * or by VME specs
     */
    public int getEventSizeMode(){
        return eventSizeMode;
    }
	

    /** Returns size of a event.
     * @return the size of the events
     * @throws SortException when there is no event size yet
     */
    public int getEventSize() throws SortException {
    	final int rval;
    	final StringBuffer mess=new StringBuffer(classname).append(colon);
        if(eventSizeMode == 0) {
			final String sizeUnknown="Event Size Unkown";
           	throw new SortException(mess.append(sizeUnknown).toString());
        } else if (eventSizeMode != SET_BY_CNAF && 
        eventSizeMode!=SET_BY_VME_MAP && eventSizeMode != SET_EXPLICITLY) {
           throw new SortException(mess.append(illegalMode).append(
            eventSizeMode).toString());
        } else if (eventSizeMode==SET_BY_CNAF) {
            rval=cnafCommands.getEventSize();
        } else if (eventSizeMode==SET_BY_VME_MAP) {
            rval=vmeMap.getEventSize();
        } else {//==SET_EXPLICITLY
        	rval=eventSize;
        }
        return rval;
    }

    /** Set the event stream to use to write out events.
     * @param out stream to which presorted event output will go
     */
    public final void setEventOutputStream(EventOutputStream out){
    	synchronized (this){
        	eventOutputStream=out;
        }
    }

    /**
     * set the state to write out events
     * @param state true to write out events.
     */
    public final void setWriteEnabled(boolean state){
        synchronized (this){
        	writeOn=state;
        }
    }

    /** Writes an event to the event output stream.  Used by the 
     * <code>sort()</code> method, if so desired.
     *
     * @param event event to write out
     * @throws SortException when an unnacceptable error condition 
     * occurs during sorting
     * @see #sort(int[])
     */
    public final void writeEvent(int [] event) throws SortException {
        if(writeOn){
            try{
                eventOutputStream.writeEvent(event);
            } catch (EventException e) {
                throw new SortException(e.toString());
            }
        }
    }

    /**
     * @see jam.global.Sorter#initialize()
     */
    public abstract void initialize() throws Exception;

    /**
     * @see jam.global.Sorter#sort(int[])
     */
    public abstract void sort(int [] dataWords) throws Exception ;

    /** 
     * Required by the <code>Sorter</code> interface.  As written 
     * always returns zero, and should be overwritten whenever using
     * monitors.
     * 
     * @param name  name of monitor value to calculate
     * @see jam.data.Monitor
     * @see jam.global.Sorter#monitor(String)
     * @return floating point value of the monitor
     */
    public double monitor(String name){
        return 0.0;
    }
}
