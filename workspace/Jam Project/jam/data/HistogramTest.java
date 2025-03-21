package jam.data;

import junit.framework.TestCase;
import java.util.List;

/**
 * JUnit tests for <code>jam.data.Histogram</data>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @see Histogram
 * @see AllTests
 */
public class HistogramTest extends TestCase {

	Gate g1, g2; 
	Histogram h1,h2; 

	/**
	 * Constructor for HistogramTest.
	 * 
	 * @param arg0
	 */
	public HistogramTest(String arg0) {
		super(arg0);
	}

	/**
	 * Initialize local variables for the tests.
	 * 
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		h1 = new Histogram("h1", Histogram.ONE_DIM_INT, 100, "h1");
		h2 = new Histogram("h2", Histogram.ONE_DIM_INT, 100, "h2");
		g1 = new Gate("g1",h1);
		g2 = new Gate("g2",h2);
	}

	/**
	 * Test for <code>hasGate(Gate)</code>.
	 *
	 * @see Histogram#hasGate(Gate)
	 */
	public void testHasGate() {
		assertTrue(h1.hasGate(g1));
		assertTrue(h2.hasGate(g2));
		assertTrue(!h1.hasGate(g2));
		assertTrue(!h2.hasGate(g1));
	}
	
	/**
	 * Test for <code>getHistogram(String)</code>.
	 * 
	 * @see Histogram#getHistogram(String)
	 */
	public void testGetHistogram() {
		assertNotNull("h1 nonexistent here", h1);
		assertNotNull("Couldn't find histogram named \""+h1.getName()+"\"",Histogram.getHistogram(h1.getName()));
		assertNotNull("Couldn't find histogram named \""+h2.getName()+"\"",Histogram.getHistogram(h2.getName()));
		assertNull("Found nonexistent histogram named \"notreal\"",Histogram.getHistogram("notreal"));
	}
	
	/**
	 * Test for <code>getGates</code>.
	 *
	 * @see Histogram#getGates
	 */
	public void testGetGates(){
		List h1List=h1.getGates();
		assertEquals(h1List.size(),1);
	}

}
