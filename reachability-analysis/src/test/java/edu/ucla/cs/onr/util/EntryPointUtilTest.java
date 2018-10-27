package edu.ucla.cs.onr.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.EntryPointUtil;

public class EntryPointUtilTest {
	
	@Test
	public void testGetMainMethod() {
		ClassLoader classLoader = EntryPointUtilTest.class.getClassLoader();
		File app_class_dir = 
				new File(classLoader.getResource("simple-test-project2/target/classes").getFile());
		Set<String> classes = new HashSet<String>();
		Set<MethodData> methods = new HashSet<MethodData>();
		ASMUtils.readClassFromDirectory(app_class_dir, classes, methods);
		Set<MethodData> mainMethods = EntryPointUtil.getMainMethodsAsEntryPoints(methods);
		assertEquals(4, mainMethods.size());
	}

	@Test
	public void testGetTestMethodFromLogFile() {
		File test_log = new File("src/test/resources/junit4_test.log");
		Set<String> tests = EntryPointUtil.getTestMethodsAsEntryPoints(test_log);
		assertEquals(172, tests.size());
	}
	
	@Test
	public void testGetTestMethodFromLogFile2() {
		File test_log = new File("src/test/resources/apache_commons_lang_test.log");
		Set<String> tests = EntryPointUtil.getTestMethodsAsEntryPoints(test_log);
		assertEquals(149, tests.size());
	}
	
	@Test
	public void testGetTestMethodFromLogFile3() {
		File test_log = new File("src/test/resources/javaapns_test.log");
		Set<String> tests = EntryPointUtil.getTestMethodsAsEntryPoints(test_log);
		assertEquals(19, tests.size());
	}
	
	/**
	 * 
	 * Tianyi: the file path in this test is to my own machine. 
	 * We need to refactor it to a relative path later.
	 * Please add @Ignore to skip this test if you are running it in your machine.
	 * 
	 */
	@Test
	public void compareTestEntryPointReader() {
		// get executed test cases from mvn test log
		File test_log = new File("src/test/resources/junit4_test.log");
		File test_class_dir = new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit/target/test-classes");
		Set<MethodData> testsFromLog = EntryPointUtil.getTestMethodsAsEntryPoints(test_log, test_class_dir);
		
		// get test cases based on annotations
		Set<String> testClasses = new HashSet<String>();
		Set<MethodData> testMethods = new HashSet<MethodData>();
		ASMUtils.readClassFromDirectory(test_class_dir, testClasses, testMethods);
		Set<MethodData> testsByAnnotations = EntryPointUtil.getTestMethodsAsEntryPoints(testMethods);
		
		
		// compare the test cases from different sources at the class level
		Set<String> cset1 = new HashSet<String>();
		for(MethodData md : testsFromLog) {
			cset1.add(md.getClassName());
		}
		
		Set<String> cset2 = new HashSet<String>();
		for(MethodData md : testsByAnnotations) {
			cset2.add(md.getClassName());
		}
		
		Set<String> cset1_copy = new HashSet<String>(cset1);
		cset1_copy.removeAll(cset2);
		// the test classes identified by annotations should contain all test classes
		// identified from the test log
		assertTrue(cset1_copy.isEmpty());
		
		Set<String> cset2_copy = new HashSet<String>(cset2);
		cset2_copy.removeAll(cset1);
		System.out.println("Test classes found by JUnit annotations but not in the test log :");
		for(String s : cset2_copy) {
			System.out.println(s);
		}
		// but not the other way around because there might be some test classes that are 
		// ignored
		assertFalse(cset2_copy.isEmpty());
	}
}
