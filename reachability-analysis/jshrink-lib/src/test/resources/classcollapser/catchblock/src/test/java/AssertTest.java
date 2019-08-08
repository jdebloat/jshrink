import org.junit.Test;

public class AssertTest{
    @Test
    public void testAssertNullNotEqualsString() {
        try {
            Assert1.assertEquals(null, "foo");
            Assert1.fail();
        } catch (ComparisonFailure1 e) {
        }
    }
    @Test
    public void testAssertStringNotEqualsNull() {
        try {
            Assert1.assertEquals("foo", null);
            Assert1.fail();
        } catch (ComparisonFailure1 e) {
            e.getMessage(); // why no assertion?
        }
    }
}