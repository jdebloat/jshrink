package edu.ucla.cs.jshrinklib.reachability;

import edu.ucla.cs.jshrinklib.util.MavenUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestOutputTest {

	@Test
	public void TestOutputTest(){
		String toProcess = "-------------------------------------------------------\n" +
			" T E S T S\n" +
			"-------------------------------------------------------\n" +
			"\n" +
			"-------------------------------------------------------\n" +
			" T E S T S\n" +
			"-------------------------------------------------------\n" +
			"Running org.I0Itec.zkclient.util.ZkPathUtilTest\n" +
			"Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.375 sec - " +
			"in org.I0Itec.zkclient.util.ZkPathUtilTest\n" +
			"Running org.I0Itec.zkclient.ServerZkClientTest\n" +
			"Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 42.592 sec - " +
			"in org.I0Itec.zkclient.ServerZkClientTest\n" +
			"Running org.I0Itec.zkclient.InMemoryConnectionTest\n" +
			"Tests run: 2, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.034 sec - " +
			"in org.I0Itec.zkclient.InMemoryConnectionTest\n" +
			"Running org.I0Itec.zkclient.ContentWatcherTest\n" +
			"Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.57 sec - " +
			"in org.I0Itec.zkclient.ContentWatcherTest\n" +
			"Running org.I0Itec.zkclient.ZkClientSerializationTest\n" +
			"Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.282 sec - " +
			"in org.I0Itec.zkclient.ZkClientSerializationTest\n" +
			"Running org.I0Itec.zkclient.ZkConnectionTest\n" +
			"Tests run: 2, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 1.29 sec - " +
			"in org.I0Itec.zkclient.ZkConnectionTest\n" +
			"Running org.I0Itec.zkclient.DistributedQueueTest\n" +
			"Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.801 sec - " +
			"in org.I0Itec.zkclient.DistributedQueueTest\n" +
			"Running org.I0Itec.zkclient.MemoryZkClientTest\n" +
			"Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 15.965 sec - " +
			"in org.I0Itec.zkclient.MemoryZkClientTest\n" +
			"\n" +
			"Results :\n" +
			"\n" +
			"Tests run: 44, Failures: 0, Errors: 0, Skipped: 2\n";

		TestOutput output = MavenUtils.testOutputFromString(toProcess);
		assertEquals(44, output.getRun());
		assertEquals(0, output.getFailures());
		assertEquals(0, output.getErrors());
		assertEquals(2, output.getSkipped());
	}
}
