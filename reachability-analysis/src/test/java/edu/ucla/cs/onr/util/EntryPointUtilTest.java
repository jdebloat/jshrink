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
}
