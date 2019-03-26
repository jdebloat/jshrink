import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StandardStuffTest {

	@Test
	public void testA(){
		A a = new A("hello");
		assertEquals(true, a.bar());
	}
}
