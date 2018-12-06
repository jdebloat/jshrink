import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StandardStuffTest {

	@Test
	public void getStringTest(){
		StandardStuff s = new StandardStuff();
		assertEquals("Hello world", s.getString());
	}

	@Test
	public void publicAndTestedButUntouchedTest(){
		StandardStuff s = new StandardStuff();
		s.publicAndTestedButUntouched();
	}

	@Test(expected = RuntimeException.class)
	public void standardStuffSubTest(){
		StandardStuffSub s = new StandardStuffSub();
		throw new RuntimeException();
	}
}
