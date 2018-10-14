package edu.ucla.cs.onr;

import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.WritingClassUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ApplicationTest {
	private static List<File> getAppClassPath(){
		List<File> toReturn = new ArrayList<File>();

		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f = new File(classLoader.getResource("simple-test-project/target/classes").getFile());
		assert(f.exists());
		toReturn.add(f);

		return toReturn;
	}

	private static List<File> getLibClassPath(){
		List<File> toReturn = new ArrayList<File>();

		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f =
			new File(classLoader.getResource("simple-test-project/libs/standard-stuff-library.jar").getFile());
		assert(f.exists());
		toReturn.add(f);

		return toReturn;
	}

	private static List<File> getTestClassPath(){
		List<File> toReturn = new ArrayList<File>();

		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f = new File(classLoader.getResource("simple-test-project/target/test-classes").getFile());
		assert(f.exists());
		toReturn.add(f);

		return toReturn;
	}

	private static String fileListToClasspathString(List<File> fList){
		StringBuilder sb = new StringBuilder();

		for(File f : fList){
			if(!sb.toString().isEmpty()){
				sb.append(File.pathSeparator);
			}
			sb.append(f.getAbsoluteFile());
		}

		return sb.toString();
	}

	@After
	public void rectifyChanges() throws IOException{
		Collection<File> files = new HashSet<File>();
		files.addAll(getAppClassPath());
		files.addAll(getLibClassPath());
		files.addAll(getTestClassPath());
		WritingClassUtils.rectifyChanges(files);
	}

	@Before
	public void cleanup() throws IOException{
		/*
		Unfortunately, sometimes the application will exit, and thus 'rectifyChanges' is never called.
		To fix this, we rectify any changes that might now have been recified during the previous test run.
		 */
		rectifyChanges();
	}

	private boolean isPresent(Set<MethodData> methodsRemoved, String className, String methodName){
		for(MethodData methodData : methodsRemoved){
			if(methodData.getClassName().equals(className) && methodData.getName().equals(methodName)){
				return true;
			}
		}

		return false;
	}

	@Test
	public void mainTest_targetMainEntryPoint(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--entry-point main ");
		arguments.append("--debug");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertFalse(isPresent(methodsRemoved,"LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,"LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,"LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
	}

	@Test
	public void mainTest_targetTestEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--entry-point tests ");
		arguments.append("--debug ");
		arguments.append("--verbose ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved,"LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,"LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,"LibraryClass","privateUntouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,"LibraryClass","<init>"));
		assertTrue(isPresent(methodsRemoved,"Main","main"));
	}

	@Test
	public void mainTest_targetPublicEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--entry-point public ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));


		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertFalse(isPresent(methodsRemoved,"LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,"LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,"LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
	}
}
