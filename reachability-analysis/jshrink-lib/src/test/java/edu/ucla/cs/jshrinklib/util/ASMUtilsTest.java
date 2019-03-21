package edu.ucla.cs.jshrinklib.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashSet;

import edu.ucla.cs.jshrinklib.reachability.FieldData;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import org.junit.Test;

public class ASMUtilsTest {
	@Test
	public void testReadClassFromJar() {
		File jarPath = new File(ASMUtilsTest.class.getClassLoader().getResource("Jama-1.0.3.jar").getFile());
		HashSet<String> classes = new HashSet<String>();
		HashSet<MethodData> methods = new HashSet<MethodData>();
		HashSet<FieldData> fields = new HashSet<FieldData>();
		ASMUtils.readClass(jarPath, classes, methods, fields);
		assertEquals(9, classes.size());
		assertEquals(118, methods.size());
		assertEquals(35, fields.size());
	}
	
	@Test
	public void testReadClassFromDir() {
		File jarPath = new File(ASMUtilsTest.class.getClassLoader().getResource("Jama-1.0.3").getFile());
		HashSet<String> classes = new HashSet<String>();
		HashSet<MethodData> methods = new HashSet<MethodData>();
		HashSet<FieldData> fields = new HashSet<FieldData>();
		ASMUtils.readClass(jarPath, classes, methods, fields);
		assertEquals(9, classes.size());
		assertEquals(118, methods.size());
		assertEquals(35, fields.size());
	}
}
