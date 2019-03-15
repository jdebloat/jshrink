package edu.ucla.cs.jshrinkapp;

import edu.ucla.cs.jshrinklib.classcollapser.ClassCollapserData;
import edu.ucla.cs.jshrinklib.methodinliner.InlineData;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.reachability.TestOutput;
import edu.ucla.cs.jshrinklib.util.MavenUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
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

	private static Optional<File> simpleTestProject = Optional.empty();
	private static Optional<File> moduleTestProject = Optional.empty();
	private static Optional<File> reflectionTestProject = Optional.empty();
	private static Optional<File> junitProject = Optional.empty();
	private static Optional<File> classCollapserProject = Optional.empty();
	private static Optional<File> lambdaProject = Optional.empty();

	private static File getOptionalFile(Optional<File> optionalFile, String resources){
		if(optionalFile.isPresent()){
			return optionalFile.get();
		}
		ClassLoader classLoader = ApplicationTest.class.getClassLoader();
		File f = new File(classLoader.getResource(resources).getFile());

		try {
			File copy = File.createTempFile("test-project-", "");
			copy.delete();
			copy.mkdir();

			FileUtils.copyDirectory(f, copy);

			optionalFile = Optional.of(copy);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

		return optionalFile.get();
	}

	private static File getSimpleTestProjectDir(){
		return getOptionalFile(simpleTestProject, "simple-test-project");
	}

	private boolean jarIntact(){
		if(simpleTestProject.isPresent()){
			File f = new File(simpleTestProject.get().getAbsolutePath()
				+ File.pathSeparator + "libs" + File.pathSeparator + "standard-stuff-library.jar");
			return f.exists() && !f.isDirectory();
		}

		return true;
    }

	private File getModuleProjectDir() {
		return getOptionalFile(moduleTestProject, "module-test-project");
	}

	private File getReflectionProjectDir(){
		return getOptionalFile(reflectionTestProject, "reflection-test-project");
	}

	private File getJunitProjectDir(){
		return getOptionalFile(junitProject, "junit4");
	}

	private File getClassCollapserDir(){
		return getOptionalFile(classCollapserProject, "classcollapser"
			+ File.separator + "simple-collapse-example");
	}

	private File getLambdaAppProject(){
		return getOptionalFile(lambdaProject, "lambda-test-project");
	}

	private File getTamiFlexJar(){
		File toReturn = new File(
				ApplicationTest.class.getClassLoader().getResource(
					"tamiflex" + File.separator + "poa-2.0.3.jar").getFile());
		return toReturn;
	}

	@After
	public void rectifyChanges() throws IOException{
		simpleTestProject = Optional.empty();
		moduleTestProject = Optional.empty();
		reflectionTestProject = Optional.empty();
		junitProject = Optional.empty();
		classCollapserProject = Optional.empty();
		lambdaProject = Optional.empty();
		G.reset();
	}


	private boolean isPresent(Set<MethodData> methodsRemoved, String className, String methodName){
		for(MethodData methodData : methodsRemoved){
			if(methodData.getClassName().equals(className) && methodData.getName().equals(methodName)){
				return true;
			}
		}

		return false;
	}

	/*
	@Test
	public void test(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project /home/bobbyrbruce/Desktop/nifty ");
		arguments.append("--main-entry ");
		arguments.append("--public-entry ");
		arguments.append("--test-entry ");
		arguments.append("--verbose ");
		arguments.append("--use-spark ");


		Application.main(arguments.toString().split("\\s+"));
	}
	*/

	@Test
	public void mainTest_targetMainEntryPoint(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;


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

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertTrue(classesRemoved.contains("StandardStuffSub"));
		//assertEquals(2, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetMainEntryPoint_withSpark(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--use-spark ");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertTrue(classesRemoved.contains("StandardStuffSub"));
		//assertEquals(2, classesRemoved.size());

		assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetTestEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--test-entry ");
		arguments.append("--remove-methods ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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
		assertFalse(isPresent(methodsRemoved, "Main$1", "compare"));


		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertEquals(2, classesRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetPublicEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--public-entry ");
		arguments.append("--include-exception ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

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

		//assertTrue(classRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertEquals(1, classRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mainTest_targetAllEntryPoints(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--public-entry ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--include-exception \"message_removed\" ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

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

		//assertFalse(classRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertEquals(0, classRemoved.size());

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
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--public-entry ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--include-exception \"message_removed\" ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classRemoved = Application.removedClasses;

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

		//assertFalse(classRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertEquals(0, classRemoved.size());

		assertTrue(jarIntact());
	}


	@Ignore @Test //Ignoring this test right now as it's failing (we think it's a bug in Spark callgraph analysis)
	public void mainTest_targetCustomEntryPoint(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--custom-entry <StandardStuff: public void publicAndTestedButUntouched()> ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		//assertEquals(2, classesRemoved.size());

        assertTrue(jarIntact());
	}

	@Test
	public void mavenTest_mainMethodEntry_withOutTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getModuleProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		//assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		//assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test 
//	@Ignore
	public void mavenTest_mainMethodEntry_withTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getModuleProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry "); //Note: when targeting Maven, we always implicitly target test entry due to TamiFlex
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		//assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		//assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test @Ignore //We don't support "--ignore-classes" for now
	public void ignoreClassTest(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--ignore-classes edu.ucla.cs.onr.test.LibraryClass edu.ucla.cs.onr.test.UnusedClass ");

		try {
			Method method = ApplicationTest.class.getMethod("ignoreClassTest");
			Object o = method.invoke(null);
		}catch(Exception e){

		}

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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

		//assertTrue(classesRemoved.contains("StandardStuffSub"));
		//assertEquals(1, classesRemoved.size());

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

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		//assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		//assertFalse(classesRemoved.contains("ReflectionStuff"));
		//assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test
	public void reflectionTest_mainMethodEntry_withoutTamiFlex(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getReflectionProjectDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry "); //Note: when targeting Maven, we always implicitly target test entry due to TamiFlex

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		//assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		//assertTrue(classesRemoved.contains("ReflectionStuff"));
		//assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

	@Test @Ignore
	public void junit_test(){
		//This tests ensures that all test cases pass before and after the tool is run
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getJunitProjectDir() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--test-entry ");
		arguments.append("--public-entry ");
		arguments.append("--remove-methods ");
		arguments.append("--verbose ");
		arguments.append("-T ");
	//	arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		//arguments.append("--inline ");
		//arguments.append("--class-collapser ");

		Application.main(arguments.toString().split("\\s+"));

		assertEquals(Application.testOutputBefore.getRun(), Application.testOutputAfter.getRun());
		assertEquals(Application.testOutputBefore.getErrors(), Application.testOutputAfter.getErrors());
		assertEquals(Application.testOutputBefore.getFailures(), Application.testOutputAfter.getFailures());
		assertEquals(Application.testOutputBefore.getSkipped(), Application.testOutputAfter.getSkipped());
	}

	@Test @Ignore
	public void lambdaMethodTest(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getLambdaAppProject() + "\" ");
		arguments.append("--main-entry ");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(Application.removedMethod);
		assertTrue(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved, "StandardStuff", "isEven"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved,"Main","isNegativeNumber"));
	}

	@Test @Ignore
	public void lambdaMethodTest_full(){
		/*
		This test fails do to Soot which cannot properly process convert SootClass to .java files which contain Lambda
		expressions. I've set it to ignore for the time being as it's not something we can presently fix.
		 */
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getLambdaAppProject() + "\" ");
		arguments.append("--main-entry ");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertFalse(Application.removedMethod);
		assertTrue(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertFalse(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved, "StandardStuff", "isEven"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved,"Main","isNegativeNumber"));
		assertTrue(isPresent(methodsRemoved,"Main","methodNotUsed"));
		assertEquals(0, methodsRemoved.size());
	}

	@Test
	public void inlineMethodTest(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--inline ");

		Application.main(arguments.toString().split("\\s+"));

		InlineData methodsInlined = Application.inlineData;

		Assert.assertTrue(methodsInlined.getInlineLocations().containsKey(
			"<StandardStuff$NestedClass: void nestedClassMethodCallee()>"));
		assertEquals(1,methodsInlined.getInlineLocations()
			.get("<StandardStuff$NestedClass: void nestedClassMethodCallee()>").size());
		Assert.assertTrue(methodsInlined.getInlineLocations()
			.get("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")
			.contains("<StandardStuff$NestedClass: void nestedClassMethod()>"));

		Assert.assertTrue(methodsInlined.getInlineLocations().containsKey(
			"<StandardStuff: java.lang.String getString()>"));
		assertEquals(1, methodsInlined.getInlineLocations()
			.get("<StandardStuff: java.lang.String getString()>").size());
		Assert.assertTrue(methodsInlined.getInlineLocations().get("<StandardStuff: java.lang.String getString()>")
			.contains("<Main: void main(java.lang.String[])>"));

		Assert.assertTrue(methodsInlined.getInlineLocations().containsKey(
			"<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>"));
		assertEquals(1, methodsInlined.getInlineLocations()
			.get("<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>").size());
		Assert.assertTrue(methodsInlined.getInlineLocations().get("<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>")
			.contains("<Main: void main(java.lang.String[])>"));

		assertTrue(jarIntact());
	}

	@Test
	public void classCollapserTest(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project \"" + getClassCollapserDir().getAbsolutePath() + "\" ");
		arguments.append("--main-entry ");
		arguments.append("--remove-methods ");
		arguments.append("--tamiflex " + getTamiFlexJar().getAbsolutePath() + " ");
		arguments.append("--class-collapser ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;
		ClassCollapserData classCollapserData = Application.classCollapserData;


		assertEquals(1, classCollapserData.getClassesToRemove().size());
		assertTrue(classCollapserData.getClassesToRemove().contains("B"));

		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "Main", "main"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "C", "<init>"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "C", "saySomething"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "C", "uniqueToC"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "B","<init>"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "B", "uniqueToB"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "B", "saySomething"));
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "uniqueToB"));
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "uniqueToA"));
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "<init>"));
		assertTrue(isPresent(classCollapserData.getRemovedMethods(), "A", "saySomething"));
		assertFalse(isPresent(classCollapserData.getRemovedMethods(), "A", "getClassType"));
	}

	@Test
	public void mainTest_targetMainEntryPoint_classCollapser(){
		StringBuilder arguments = new StringBuilder();
		arguments.append("--prune-app ");
		arguments.append("--maven-project " + getSimpleTestProjectDir().getAbsolutePath() + " ");
		arguments.append("--main-entry ");
		arguments.append("--class-collapser ");


		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

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

		//assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		//assertTrue(classesRemoved.contains("StandardStuffSub"));
		//assertEquals(2, classesRemoved.size());

		assertTrue(jarIntact());
	}

}
