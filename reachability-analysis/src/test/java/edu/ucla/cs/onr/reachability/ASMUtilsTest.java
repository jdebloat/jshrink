package edu.ucla.cs.onr;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashSet;

import org.junit.Test;

public class ASMUtilsTest {
	@Test
	public void testReadClassFromJar() {
		File jarPath = new File("src/test/resources/Jama-1.0.3.jar");
		HashSet<String> classes = new HashSet<String>();
		HashSet<String> methods = new HashSet<String>();
		ASMUtils.readClassFromJarFile(jarPath, classes, methods);
		assertEquals(9, classes.size());
		assertEquals(118, methods.size());
	}
	
	@Test
	public void testReadClassFromDir() {
		File jarPath = new File("src/test/resources/Jama-1.0.3");
		HashSet<String> classes = new HashSet<String>();
		HashSet<String> methods = new HashSet<String>();
		ASMUtils.readClassFromDirectory(jarPath, classes, methods);
		assertEquals(9, classes.size());
		assertEquals(118, methods.size());
	}
}
