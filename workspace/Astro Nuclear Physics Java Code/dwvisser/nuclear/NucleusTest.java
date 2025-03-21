package dwvisser.nuclear;
import dwvisser.math.UncertainNumber;
import junit.framework.TestCase;

/**
 * JUnit test of <code>Nucleus</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class NucleusTest extends TestCase {

	/**
	 * Constructor for NucleusTest.
	 * @param arg0
	 */
	public NucleusTest(String arg0) {
		super(arg0);
	}

	/*
	 * Test for boolean equals(Object)
	 */
	public void testEqualsObject() {
		UncertainNumber ex=new UncertainNumber(20.1,1.0);
		Nucleus a1=new Nucleus(2,4);
		Nucleus a2=new Nucleus(2,4,0.0);
		Nucleus a3=new Nucleus(2,4,ex.value);
		Nucleus a4=new Nucleus(2,4,ex);
		assertEquals(a1,a2);
		assertEquals(a2,a3);
		assertEquals(a3,a4);
		assertEquals(a3.Ex.value,a4.Ex.value,a3.Ex.error);
	}
}
