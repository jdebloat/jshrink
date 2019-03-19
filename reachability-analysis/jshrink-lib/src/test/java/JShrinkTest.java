import edu.ucla.cs.jshrinklib.JShrink;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.reachability.EntryPointProcessor;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class JShrinkTest {
	private static File simpleTestProjectDir;
	private static EntryPointProcessor entryPointProcessor;
	private static Optional<File> tamiflex;
	private static boolean useSpark;
	private static JShrink jShrink;

	private static void resetSimpleTestProjectDir(){
		try {
			if(simpleTestProjectDir != null){
				simpleTestProjectDir.delete();
			}
			File original = new File(JShrinkTest.class.getClassLoader()
				.getResource("simple-test-project").getFile());
			File copy = File.createTempFile("sample-test-project", "");
			copy.delete();
			copy.mkdir();
			FileUtils.copyDirectory(original, copy);
			simpleTestProjectDir = copy;
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

	}

	@BeforeClass
	public static void beforeClass(){
		resetSimpleTestProjectDir();
		entryPointProcessor =
			new EntryPointProcessor(true, false,
				false,true, new HashSet<MethodData>());
		tamiflex = Optional.empty();
		useSpark = true;
		try {
			jShrink = JShrink.createInstance(simpleTestProjectDir, entryPointProcessor, tamiflex, useSpark);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void reboot(){
		try {
			resetSimpleTestProjectDir();
			this.jShrink = JShrink.resetInstance(this.simpleTestProjectDir,
				this.entryPointProcessor, this.tamiflex, this.useSpark);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Test
	public void getInstanceTest(){
		try {
			assertEquals(this.jShrink, JShrink.getInstance());
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	private boolean isPresent(Set<MethodData> methodSet, String className, String methodName){
		for(MethodData methodData : methodSet){
			if(methodData.getClassName().equals(className) && methodData.getName().equals(methodName)){
				return true;
			}
		}
		return false;
	}


	@Test
	public void getAllAppMethodsTest(){
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "<init>"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "doNothing"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "touchedViaReflection"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "getString"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "getStringStatic"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "publicAndTestedButUntouched"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "publicNotTestedButUntouched"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "privateAndUntouched"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff", "protectedAndUntouched"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff$1", "compare"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff$1", "<init>"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff$NestedClass", "<init>"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff$NestedClass", "nestedClassMethod"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff$NestedClass", "nestedClassMethodCallee"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuff$NestedClass", "nestedClassNeverTouched"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuffSub", "<init>"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"StandardStuffSub", "subMethodUntouched"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"Main", "<init>"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"Main", "access$000"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"Main", "main"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"Main", "compare"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"Main$1", "compare"));
		assertTrue(isPresent(this.jShrink.getAllAppMethods(),
			"Main$1", "<init>"));
		assertEquals(29,this.jShrink.getAllAppMethods().size());
	}

	@Test
	public void getAllLibMethodsTest(){
		assertTrue(isPresent(this.jShrink.getAllLibMethods(),
			"edu.ucla.cs.onr.test.UnusedClass", "<init>"));
		assertTrue(isPresent(this.jShrink.getAllLibMethods(),
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(this.jShrink.getAllLibMethods(),
			"edu.ucla.cs.onr.test.LibraryClass", "<init>"));
		assertTrue(isPresent(this.jShrink.getAllLibMethods(),
			"edu.ucla.cs.onr.test.LibraryClass", "getNumber"));
		assertTrue(isPresent(this.jShrink.getAllLibMethods(),
			"edu.ucla.cs.onr.test.LibraryClass", "untouchedGetNumber"));
		assertTrue(isPresent(this.jShrink.getAllLibMethods(),
			"edu.ucla.cs.onr.test.LibraryClass", "privateUntouchedGetNumber"));
		assertTrue(isPresent(this.jShrink.getAllLibMethods(),
			"edu.ucla.cs.onr.test.LibraryClass2", "<init>"));
		assertTrue(isPresent(this.jShrink.getAllLibMethods(),
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));
		assertEquals(8,this.jShrink.getAllLibMethods().size());
	}

	@Test
	public void getAllAppClassesTest(){
		assertTrue(this.jShrink.getAllAppClasses().contains("StandardStuff"));
		assertTrue(this.jShrink.getAllAppClasses().contains("StandardStuff$NestedClass"));
		assertTrue(this.jShrink.getAllAppClasses().contains("StandardStuff$1"));
		assertTrue(this.jShrink.getAllAppClasses().contains("StandardStuffSub"));
		assertTrue(this.jShrink.getAllAppClasses().contains("Main$1"));
		assertTrue(this.jShrink.getAllAppClasses().contains("Main"));
		assertEquals(6, this.jShrink.getAllAppClasses().size());
	}

	@Test
	public void getAllLibClassesTest(){
		assertTrue(this.jShrink.getAllLibClasses().contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(this.jShrink.getAllLibClasses().contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertTrue(this.jShrink.getAllLibClasses().contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertEquals(3, this.jShrink.getAllLibClasses().size());
	}

	@Test
	public void getUsedAppMethodsTest(){
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"StandardStuff", "<init>"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"StandardStuff", "getString"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"StandardStuff", "getStringStatic"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"StandardStuff$1", "<init>"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"StandardStuff$1", "compare"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"StandardStuff$NestedClass", "<init>"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"StandardStuff$NestedClass", "nestedClassMethod"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"StandardStuff$NestedClass", "nestedClassMethodCallee"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"Main", "main"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"Main$1", "compare"));
		assertTrue(isPresent(this.jShrink.getUsedAppMethods(),
			"Main", "compare"));
		assertEquals(17,this.jShrink.getUsedAppMethods().size());
	}

	@Test
	public void getUsedLibMethodsTest(){
		assertTrue(isPresent(this.jShrink.getUsedLibMethods(),
			"edu.ucla.cs.onr.test.LibraryClass", "<init>"));
		assertTrue(isPresent(this.jShrink.getUsedLibMethods(),
			"edu.ucla.cs.onr.test.LibraryClass", "getNumber"));
		assertEquals(2,this.jShrink.getUsedLibMethods().size());
	}

	@Test
	public void getUsedAppClassesTest(){
		assertTrue(this.jShrink.getUsedAppClasses().contains("StandardStuff"));
		assertTrue(this.jShrink.getUsedAppClasses().contains("StandardStuff$NestedClass"));
		assertTrue(this.jShrink.getUsedAppClasses().contains("Main"));
		assertTrue(this.jShrink.getUsedAppClasses().contains("Main$1"));
		assertTrue(this.jShrink.getAllAppClasses().contains("StandardStuff$1"));
		assertEquals(5, this.jShrink.getUsedAppClasses().size());
	}

	@Test
	public void getSizesTest(){
		assertEquals(10393, this.jShrink.getAppSize(true));
		assertEquals(8014, this.jShrink.getLibSize(true));
	}

	@Test
	public void getTestDataTest(){
		assertTrue(this.jShrink.getTestOutput().isPresent());
		assertEquals(4, this.jShrink.getTestOutput().get().getRun());
		assertEquals(0, this.jShrink.getTestOutput().get().getErrors());
		assertEquals(1, this.jShrink.getTestOutput().get().getFailures());
		assertEquals(0, this.jShrink.getTestOutput().get().getSkipped());
	}

	@Test
	public void getUsedLibClassesTest(){
		assertTrue(this.jShrink.getUsedLibClasses().contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertEquals(1, this.jShrink.getUsedLibClasses().size());
	}

	@Test
	public void getCallGraphsTest(){
		//TODO: This could be more detailed
		assertEquals(1, this.jShrink.getCallGraphs().size());
	}

	@Test
	public void getSimplifiedCallGraphTest(){
		//TODO: Complete this.
	}

	@Test
	public void makeSootPassTest(){
		this.jShrink.makeSootPass(); //Simply ensuring this doesn't crash for now.
		reboot();
	}

	@Test
	public void removeMethodsTest(){
		Set<MethodData> toRemove = new HashSet<MethodData>();
		toRemove.addAll(this.jShrink.getAllAppMethods());
		toRemove.removeAll(this.jShrink.getUsedAppMethods());
		Set<MethodData> methodsRemoved = this.jShrink.removeMethods(toRemove,true);
		this.jShrink.updateClassFiles();

		assertFalse(methodsRemoved.isEmpty());
		for(MethodData removed : methodsRemoved){
			assertFalse(this.jShrink.getAllAppMethods().contains(removed));
		}
		reboot(); //Reset things back to normal
	}

	@Test
	public void removeClassesTest(){
		Set<String> toRemove = new HashSet<String>();
		toRemove.add("edu.ucla.cs.onr.test.LibraryClass");
		toRemove.add("edu.ucla.cs.onr.test.LibraryClass2");
		this.jShrink.removeClasses(toRemove);
		this.jShrink.updateClassFiles();

		for(String removed: toRemove){
			assertFalse(this.jShrink.getAllLibClasses().contains(removed));
		}
		reboot(); //Reset things back to normal
	}
}
