package edu.ucla.cs.jshrinklib.reachability;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ConstantPoolScannerTest {

	public static File clazz;
	@BeforeClass
	public static void setup(){
		clazz = new File(ConstantPoolScannerTest.class.getClassLoader().getResource("simple-test-project"
				+ File.separator + "target" + File.separator + "classes"+File.separator+"Main.class").getFile());
	}
	@Test(expected=Exception.class)
	public void testjavapScanException() throws IOException, InterruptedException {
		File f = null;
		ConstantPoolScanner.getClassReferences(f);
	}
	@Test(expected=Exception.class)
	public void testjavapScanException2() throws IOException, InterruptedException {
		ConstantPoolScanner.getClassReferences(new File(""));
	}
	@Test
	public void testjavapScanCommandLineOutput() throws IOException, InterruptedException{
		Set<String> outputStream = ConstantPoolScanner.getClassReferences(clazz);
		System.out.println(String.join("\n",outputStream));
	}
	@Test
	public void testParseCPOutput() throws IOException, InterruptedException{
		Set<String> dependencies = ConstantPoolScanner.getClassReferences(clazz);
		System.out.println(String.join("\n",dependencies));
	}
}
