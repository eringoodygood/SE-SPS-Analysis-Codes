/*
 * RecentADCevents.java
 *
 * Created on November 14, 2001, 9:21 AM
 */

package sort.VME;
import java.util.*;

/**
 *
 * @author  astro
 * @version
 */
public class RecentADCevents {
    
    private Hashtable events = new Hashtable();
    
    private int oldestCounter = 0;
    private int newestCounter = 0;
    
    private boolean firstEvent=true;
    private static Vector allInstances;
    
    
    /** Creates new RecentADCevents */
    public RecentADCevents() {
        //System.out.println("new "+getClass().getName());
        if (allInstances == null) allInstances=new Vector();
        allInstances.add(this);
    }
    
    public void addEvent(int counter, int [] data) {
        if (counter > newestCounter) newestCounter=counter;
        if (firstEvent) {
            firstEvent=false;
            oldestCounter=counter;
        } else {
            if (counter < oldestCounter) oldestCounter=counter;
        }
        int [] newdata = new int[data.length];
        System.arraycopy(data,0,newdata,0,data.length);
        events.put(new Integer(counter),newdata);
    }
    
    public int getNewestCounter(){
        return newestCounter;
    }
    
    public int getOldestCounter(){
        return oldestCounter;
    }
    
    public boolean hasCounter(int counter) {
        return events.containsKey(new Integer(counter));
    }
    
    static int removeCounter=0;
    public int [] getEvent(int counter) {
        if (!hasCounter(counter)) return null;
        Integer temp = new Integer(counter);
        int [] rval = (int [])events.get(temp);
        events.remove(temp);
        RecentADCevents.removeCounter++;
        if (RecentADCevents.removeCounter == 1000) {
            RecentADCevents.removeCounter=0;
            System.gc();
        }
        return rval;
    }
    
    private void removeEventsUpTo(int counter){
        //System.out.println("Removing up to "+counter);
        for (Enumeration e = events.keys() ; e.hasMoreElements() ;) {
            Integer key_object = (Integer)e.nextElement();
            int key = key_object.intValue();
            if (key <= counter) events.remove(key_object);
        }
        setCounterLimits();
        System.gc();
    }
    
    
    private void setCounterLimits(){
        newestCounter=0;
        oldestCounter=0;
        boolean firstTimeThru=true;
        for (Enumeration e = events.keys() ; e.hasMoreElements() ;) {
            int key = ((Integer)e.nextElement()).intValue();
            if (key > newestCounter) newestCounter=key;
            if (firstTimeThru) {
                oldestCounter = key;
            } else {
                if (key < oldestCounter) oldestCounter=key;
            }
        }
    }
    
    Set getCounterSet() {
        return events.keySet();
    }
    
    /**
     * Returns array of keys for which this and another instance, have had a 
     * chance to get the data.
     */
    static public int [] getSortableCounters() {
        TreeSet sortedCounterSet = new TreeSet();
        //loop through all ADC databases, and make a sorted set of all counter values in them
        for (int i=0; i<allInstances.size(); i++) {
            RecentADCevents current = (RecentADCevents)(allInstances.get(i));
            sortedCounterSet.addAll(current.getCounterSet());
        }
        for (int i=0; i<allInstances.size(); i++) {
            RecentADCevents current = (RecentADCevents)(allInstances.get(i));
            for (Iterator iter = sortedCounterSet.iterator() ; iter.hasNext() ;) {
                int counter = ((Integer)iter.next()).intValue();
                //if current ADC has not had a chance to read the event corresponding
                //to this counter value, then remove this counter value from consideration
                //(iter.remove() also removes from underlying TreeSet)
                if (counter > current.getNewestCounter()) iter.remove();
            }
        }
        int [] rval = new int[sortedCounterSet.size()];
        int i=0;
        for (Iterator iter = sortedCounterSet.iterator(); iter.hasNext(); ) {
            rval[i]=((Integer)iter.next()).intValue();
            i++;
        }
        return rval;
    }
        
        
        

}
