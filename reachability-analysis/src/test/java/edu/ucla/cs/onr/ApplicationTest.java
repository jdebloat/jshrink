package edu.ucla.cs.onr;

import edu.ucla.cs.onr.reachability.CallGraphAnalysis;
import edu.ucla.cs.onr.reachability.EntryPointProcessor;
import edu.ucla.cs.onr.reachability.MavenSingleProjectAnalyzer;
import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.ClassFileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import soot.G;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
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

	private boolean jarIntact(){
        ClassLoader classLoader = ApplicationTest.class.getClassLoader();
        File f =
                new File(classLoader.getResource("simple-test-project/libs/standard-stuff-library.jar").getFile());
        return f.exists() && !f.isDirectory();
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

	private File getModuleProjectDir(){
		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f = new File(classLoader.getResource("module-test-project").getFile());
		assert(f.exists());
		assert(f.isDirectory());
		return f;
	}

	private File getReflectionProjectDir(){
		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f = new File(classLoader.getResource("reflection-test-project").getFile());
		assert(f.exists());
		assert(f.isDirectory());
		return f;
	}

	private File getJunitProjectDir(){
		ClassLoader classLoader = Application.class.getClassLoader();
		File f = new File(classLoader.getResource("junit4").getFile());
		assert(f.exists());
		assert(f.isDirectory());
		return f;
	}

	private List<File> getModueFilesToRectify(){
		List<File> toReturn = new ArrayList<File>();

		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f = new File(classLoader.getResource(
				"module-test-project/simple-test-project/libs/standard-stuff-library.jar").getFile());
		assert(f.exists());
		toReturn.add(f);

		return toReturn;
	}

	private List<File> getReflectionFilesToRectify(){
		List<File> toReturn = new ArrayList<File>();

		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f = new File(classLoader.getResource(
				"reflection-test-project/simple-test-project/libs/standard-stuff-library.jar").getFile());
		assert(f.exists());
		toReturn.add(f);

		return toReturn;
	}

	private File getTamiFlexJar(){
		File toReturn = new File(
				ApplicationTest.class.getClassLoader().getResource("tamiflex/poa-2.0.3.jar").getFile());
		return toReturn;
	}

	private void revertModule(){

		MavenSingleProjectAnalyzer mavenSingleProjectAnalyzer = new MavenSingleProjectAnalyzer(
				getModuleProjectDir().getAbsolutePath(),
				new EntryPointProcessor(false, false, false, new HashSet<MethodData>()),
				Optional.empty());
		mavenSingleProjectAnalyzer.cleanup();
	}

	private void revertReflection(){
		MavenSingleProjectAnalyzer mavenSingleProjectAnalyzer = new MavenSingleProjectAnalyzer(
				getReflectionProjectDir().getAbsolutePath(),
				new EntryPointProcessor(false, false, false, new HashSet<MethodData>()),
				Optional.empty());
		mavenSingleProjectAnalyzer.cleanup();
	}

	private void revertJunit(){
		MavenSingleProjectAnalyzer mavenSingleProjectAnalyzer = new MavenSingleProjectAnalyzer(
				getJunitProjectDir().getAbsolutePath(),
				new EntryPointProcessor(false, false, false, new HashSet<MethodData>()),
				Optional.empty());
		mavenSingleProjectAnalyzer.cleanup();
	}

	@After
	public void rectifyChanges() throws IOException{
		Collection<File> files = new HashSet<File>();
		files.addAll(getAppClassPath());
		files.addAll(getLibClassPath());
		files.addAll(getTestClassPath());
		files.addAll(getModueFilesToRectify());
		files.addAll(getReflectionFilesToRectify());
		ClassFileUtils.rectifyChanges(files);
		revertModule();
		revertReflection();
		revertJunit();
		G.reset();
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
		arguments.append("--main-entry ");
		arguments.append("--remove-classes ");
		arguments.append("--debug");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(Application.removedMethod);
		assertTrue(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("StandardStuffSub"));
		assertEquals(2, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetMainEntryPoint_withSpark(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--main-entry ");
		arguments.append("--remove-classes ");
		arguments.append("--use-spark ");
		arguments.append("--debug");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertTrue(CallGraphAnalysis.useSpark);

		assertFalse(Application.removedMethod);
		assertTrue(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("StandardStuffSub"));
		assertEquals(2, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetTestEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--test-entry ");
		arguments.append("--remove-methods ");
		arguments.append("--remove-classes ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertTrue(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertFalse(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		//(Method is untouched by too small to remove)
//		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertTrue(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("Main"));
		assertTrue(classesRemoved.contains("Main$1"));

		assertEquals(4, classesRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetPublicEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--public-entry ");
		arguments.append("--include-exception ");
		arguments.append("--remove-classes ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertTrue(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertFalse(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved, "edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertEquals(1, classRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetAllEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--public-entry ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--include-exception \"message_removed\" ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertTrue(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertFalse(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertFalse(classRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertEquals(0, classRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetAllEntryPoints_withTamiFlex(){
		/*
		Note: There is actually no reflection in this target, i just want to ensure reflection isn't making anything
		crash.
		 */
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--public-entry ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--include-exception \"message_removed\" ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertTrue(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertFalse(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertFalse(classRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertEquals(0, classRemoved.size());

		assertTrue(jarIntact());
	}


	@Ignore @Test //Ignoring this test right now as it's failing (we think it's a bug in Spark callgraph analysis)
	public void mainTest_targetCustomEntryPoint(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--custom-entry <StandardStuff: public void publicAndTestedButUntouched()> ");
		arguments.append("--remove-classes ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertTrue(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff", "doNothing"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethod"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassMethodCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff$NestedClass","nestedClassNeverTouched"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertTrue(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved, "Main", "compare"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertEquals(2, classesRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mavenTest_mainMethodEntry_withOutTamiFlex(){
		//Warning: This test takes a while! 6 minutes on my system
		//TODO: The big overhead is the deletion of unused methods. Perhaps we should look into this
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getModuleProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--remove-classes ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved, "StandardStuff", "touchedViaReflection"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));

		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test @Ignore
	public void mavenTest_mainMethodEntry_withTamiFlex(){
		//Warning: This test takes a while! 6 minutes on my system
		//TODO: The big overhead is the deletion of unused methods. Perhaps we should look into this
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getModuleProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry "); //Note: when targeting Maven, we always implicitly target test entry due to TamiFlex
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--remove-classes ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved, "StandardStuff", "touchedViaReflection"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));

		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));

		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test
	public void ignoreClassTest(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--lib-classpath " + fileListToClasspathString(getLibClassPath()) + " ");
		arguments.append("--app-classpath " + fileListToClasspathString(getAppClassPath()) + " ");
		arguments.append("--test-classpath " + fileListToClasspathString(getTestClassPath()) + " ");
		arguments.append("--main-entry ");
		arguments.append("--ignore-classes edu.ucla.cs.onr.test.LibraryClass edu.ucla.cs.onr.test.UnusedClass ");
		arguments.append("--remove-classes ");
		arguments.append("--debug");

		try {
			Method method = ApplicationTest.class.getMethod("ignoreClassTest");
			Object o = method.invoke(null);
		}catch(Exception e){

		}

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "protectedAndUntouched"));
		assertTrue(isPresent(methodsRemoved, "StandardStuffSub", "subMethodUntouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved, "Main", "compare"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("StandardStuffSub"));
		assertEquals(1, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void reflectionTest_mainMethodEntry_withTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getReflectionProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry "); //Note: when targeting Maven, we always implicitly target test entry due to TamiFlex
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--remove-classes ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved, "ReflectionStuff", "touchedViaReflection"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertFalse(classesRemoved.contains("ReflectionStuff"));
		assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test
	public void reflectionTest_mainMethodEntry_withoutTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getReflectionProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry "); //Note: when targeting Maven, we always implicitly target test entry due to TamiFlex
		arguments.append("--remove-classes ");
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(CallGraphAnalysis.useSpark);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertTrue(isPresent(methodsRemoved, "ReflectionStuff", "touchedViaReflection"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertTrue(classesRemoved.contains("ReflectionStuff"));
		assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	public String getJunitTestOutput(){
		File junitDir = this.getJunitProjectDir();
		File pomFile = new File(this.getJunitProjectDir().getAbsolutePath() + File.separator + "pom.xml");
		File libsDir = new File(this.getJunitProjectDir().getAbsolutePath() + File.separator + "libs");
		String test_regex = "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)$";

		String maven_log = "";
		try {
			String[] cmd = new String[]{"mvn", "-f", pomFile.getAbsolutePath(), "test",
					"-Dmaven.repo.local=" + libsDir.getAbsolutePath(), "--batch-mode", "-fn"};
			Process process1 = Runtime.getRuntime().exec(cmd);
			//process1.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process1.getInputStream()));

			String line;

			while ((line = reader.readLine()) != null) {
				maven_log += line + System.lineSeparator();
			}
			reader.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

		String testLog="";

		String[] log_lines = maven_log.split(System.lineSeparator());

		Pattern pattern = Pattern.compile(test_regex);
		for (String line : log_lines) {
			if (line.contains("Tests run: ")) {
				Matcher matcher = pattern.matcher(line);
				while (matcher.find()) {
					testLog += matcher.group() + System.lineSeparator();

				}
			}
		}

		return testLog;
	}

	@Test
	public void junit_test(){
		//This tests ensures that all test cases pass before and after the tool is run
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getReflectionProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--public-entry ");
		arguments.append("--remove-methods ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--debug ");

		String before = getJunitTestOutput();
		Application.main(arguments.toString().split("\\s+"));
		String after =getJunitTestOutput();

		assertEquals(before,after);

	}

}
