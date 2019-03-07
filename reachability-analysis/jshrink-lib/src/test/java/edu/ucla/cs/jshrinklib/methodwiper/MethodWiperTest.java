package edu.ucla.cs.jshrinklib.methodwiper;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import edu.ucla.cs.jshrinklib.util.SootUtils;
import org.junit.After;
import soot.*;
import soot.jimple.JasminClass;
import soot.options.Options;
import soot.util.JasminOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MethodWiperTest {
	private static SootClass getSootClassFromResources(String className){
//		ClassLoader classLoader = MethodWiperTest.class.getClassLoader();
//		File classFile = new File(classLoader.getResource(className + ".class").getFile());
		// the code above throws an exception about unfound resources
		// below is a temporary patch
		//TODO: Fix this --- cannot get load resources working across eclipse version.
		File classFile = new File("src/test/resources/methodwiper/" + className + ".class");

		final String workingClasspath=classFile.getParentFile().getAbsolutePath();
		Options.v().set_soot_classpath(SootUtils.getJREJars() + File.pathSeparator + workingClasspath);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);

		List<String> processDirs = new ArrayList<String>();
		processDirs.add(workingClasspath);
		Options.v().set_process_dir(processDirs);

		SootClass sClass = Scene.v().loadClassAndSupport(className);
		Scene.v().loadNecessaryClasses();


		return sClass;
	}

	private static File createClass(SootClass sootClass){

		// I receive 'Exception thrown: method <init> has no active body' if methods are not retrieved
		for(SootMethod sootMethod : sootClass.getMethods()){
			sootMethod.retrieveActiveBody();
		}

		File fileToReturn = null;
		try {
			String fileName = SourceLocator.v().getFileNameFor(sootClass, Options.output_format_class);
			fileToReturn = new File(fileName);
			OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileToReturn));
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

			JasminClass jasminClass = new soot.jimple.JasminClass(sootClass);
			jasminClass.print(writerOut);
			writerOut.flush();
			streamOut.close();

		}catch(Exception e){
			System.err.println("Exception thrown: " + e.getMessage());
			System.exit(1);
		}

		assert(fileToReturn != null);

		return fileToReturn;
	}

	private static String runClass(SootClass sootClass){
		File classFile = MethodWiperTest.createClass(sootClass);

		String cmd = "java -cp "+classFile.getParentFile().getAbsolutePath() + " "
			+ classFile.getName().replaceAll(".class","");

		Process p =null;
		StringBuilder output = new StringBuilder();
		try {
			p = Runtime.getRuntime().exec(cmd);
			p.waitFor();

			BufferedReader brInputStream = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader brErrorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			String line;
			while((line = brInputStream.readLine())!=null){
				output.append(line + System.lineSeparator());
			}
			brInputStream.close();

			while((line = brErrorStream.readLine()) != null){
				output.append(line + System.lineSeparator());
			}
			brErrorStream.close();

			//} catch(IOException e InterruptedException ie){
		}catch(Exception e){
			System.err.println("Exception thrown when trying to run the following script:");
			StringBuilder sb = new StringBuilder();
			System.err.println(cmd);
			System.err.println("The following error was thrown: ");
			System.err.println(e.getMessage());
			System.exit(1);
		}

		return output.toString();
	}

	@After
	public void before(){
		G.reset();
	}

	@Test
	public void sanityCheck() {
		SootClass sootClass = getSootClassFromResources("Test");
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_Test1_staticVoidMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticVoidMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticIntMethodNoParams() {
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticIntMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticStringMethodNoParam() {
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticStringMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticDoubleMethodNoParams() {
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticDoubleMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticVoidMethodTwoParams() {
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticVoidMethodTwoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticIntMethodTwoParams() {
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticIntMethodTwoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_methodNoParams() {
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("methodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_intMethodNoParams() {
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("intMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_intMethodTwoParams() {
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("intMethodTwoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticBooleanMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticBooleanMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticCharMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticCharMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticByteMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticByteMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticShortMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}

	@Test
	public void wipeMethodBodyTest_staticShortMethodNoParams(){
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBody(sootClass.getMethodByName("staticShortMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();

		assertEquals(expected, output);
	}


	@Test
	public void wipeMethodBodyAndInsertRuntimeException_TestWithMessage(){
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.wipeMethodBodyAndInsertRuntimeException(
			sootClass.getMethodByName("intMethodTwoParams"), "TEST"));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "Exception in thread \"main\" java.lang.RuntimeException: TEST" + System.lineSeparator() +
			"\tat Test.intMethodTwoParams(Test.java)" + System.lineSeparator() +
			"\tat Test.main(Test.java)" + System.lineSeparator();

		assertEquals(expected, output);
	}

	//Interfaces don't ahve bodies to delete, therefore these cases should be handled gracefully

	@Test
	public void wipeInterfaceMethodBody_noReturnTypeNoParameter(){
		SootClass sootClass = getSootClassFromResources("InterfaceTest");
		assertFalse(MethodWiper.wipeMethodBody(sootClass.getMethodByName("interface1")));
	}

	@Test
	public void wipeInterfaceMethodBody_stringReturnTypeNoParameter(){
		SootClass sootClass = getSootClassFromResources("InterfaceTest");
		assertFalse(MethodWiper.wipeMethodBodyAndInsertRuntimeException(sootClass.getMethodByName("interface2"), "TEST"));
	}

	@Test
	public void wipeInterfaceMethodBody_noReturnTypeOneParameter(){
		SootClass sootClass = getSootClassFromResources("InterfaceTest");
		assertFalse(MethodWiper.wipeMethodBodyAndInsertRuntimeException(sootClass.getMethodByName("interface3")));
	}

	@Test
	public void wipeTinyMethodBody(){
		SootClass sootClass = getSootClassFromResources("TinyMethodTest");
		assertFalse(MethodWiper.wipeMethodBodyAndInsertRuntimeException(sootClass.getMethodByName("getNum")
			,"THIS IS A PURPOSELY LONG EXCEPTION SO THAT THE EXCEPTION CODE IS BIGGER THAN WHAT IT REPLACES"));
	}

	@Test
	public void wipeNativeMethodBody(){
		SootClass sootClass = getSootClassFromResources("CornerCases");
		assertFalse(MethodWiper.wipeMethodBody(sootClass.getMethodByName("readByte")));
	}

	@Test
	public void wipeBody(){
		SootClass sootClass = getSootClassFromResources("Test");
		assertTrue(MethodWiper.removeMethod(sootClass.getMethodByName("staticShortMethodNoParams")));
		String output = runClass(sootClass);

		String expected = "staticVoidMethodNoParams touched" + System.lineSeparator();
		expected += "staticIntMethodNoParams touched" + System.lineSeparator();
		expected += "staticStringMethodNoParams touched" + System.lineSeparator();
		expected += "staticDoubleMethodNoParams touched" + System.lineSeparator();
		expected += "staticVoidMethodTwoParams touched" + System.lineSeparator();
		expected += "staticIntMethodTwoParams touched" + System.lineSeparator();
		expected += "methodNoParams touched" + System.lineSeparator();
		expected += "intMethodNoParams touched" + System.lineSeparator();
		expected += "intMethodTwoParams touched" + System.lineSeparator();
		expected += "staticBooleanMethodNoParams touched" + System.lineSeparator();
		expected += "staticCharMethodNoParams touched" + System.lineSeparator();
		expected += "staticByteMethodNoParams touched" + System.lineSeparator();
		expected += "Exception in thread \"main\" java.lang.NoSuchMethodError: Test.staticShortMethodNoParams()Ljava/lang/Short;\n" +
				"\tat Test.main(Test.java)" + System.lineSeparator();

		assertEquals(expected, output);
	}
}