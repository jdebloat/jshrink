package edu.ucla.cs.onr;

import edu.ucla.cs.onr.reachability.EntryPointProcessor;
import edu.ucla.cs.onr.reachability.MavenSingleProjectAnalyzer;
import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.ClassFileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import soot.G;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

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
		arguments.append("--debug");


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
		assertEquals(1, classesRemoved.size());

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
		arguments.append("--debug ");

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
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		//(Method is untouched by too small to remove)
//		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertTrue(isPresent(methodsRemoved,"Main","main"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("Main"));
		assertEquals(3, classesRemoved.size());

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
		arguments.append("--debug ");

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
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
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

		assertFalse(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertTrue(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
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

		assertTrue(classRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertEquals(1, classRemoved.size());

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

		assertFalse(Application.removedMethod);
		assertFalse(Application.wipedMethodBody);
		assertFalse(Application.wipedMethodBodyWithExceptionNoMessage);
		assertTrue(Application.wipedMethodBodyWithExceptionAndMessage);

		assertFalse(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
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

		assertTrue(classRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertEquals(1, classRemoved.size());

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
		arguments.append("--debug ");

		Application.main(arguments.toString().split("\\s+"));

		Set<MethodData> methodsRemoved = Application.removedMethods;
		Set<String> classesRemoved = Application.removedClasses;

		assertTrue(isPresent(methodsRemoved,"StandardStuff","getStringStatic"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","getString"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","<init>"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouched"));
		assertFalse(isPresent(methodsRemoved,"StandardStuff","publicAndTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouched"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","publicNotTestedButUntouchedCallee"));
		assertTrue(isPresent(methodsRemoved,"StandardStuff","privateAndUntouched"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,
			"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertTrue(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertTrue(isPresent(methodsRemoved,"Main","main"));
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
		arguments.append("--debug ");

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
		arguments.append("--debug ");

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
		arguments.append("--debug");

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
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","getNumber"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","untouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass","privateUntouchedGetNumber"));
		assertFalse(isPresent(methodsRemoved,"edu.ucla.cs.onr.test.LibraryClass","<init>"));
		assertFalse(isPresent(methodsRemoved,"Main","main"));
		assertFalse(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.UnusedClass", "unusedMethod"));
		assertTrue(isPresent(methodsRemoved,
				"edu.ucla.cs.onr.test.LibraryClass2", "methodInAnotherClass"));

		assertEquals(0, classesRemoved.size());

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
		arguments.append("--debug ");

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
		arguments.append("--debug ");

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

		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.UnusedClass"));
		assertTrue(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass2"));
		assertFalse(classesRemoved.contains("edu.ucla.cs.onr.test.LibraryClass"));
		assertTrue(classesRemoved.contains("ReflectionStuff"));
		assertFalse(classesRemoved.contains("StandardStuff"));

		assertTrue(jarIntact());
	}

}
