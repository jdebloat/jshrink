package edu.ucla.cs.jshrinklib.methodinliner;

import edu.ucla.cs.jshrinklib.reachability.CallGraphAnalysis;
import edu.ucla.cs.jshrinklib.reachability.EntryPointProcessor;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.util.ClassFileUtils;
import edu.ucla.cs.jshrinklib.util.SootUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import soot.G;
import soot.Scene;
import soot.SootMethod;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MethodInlinerTest {

	private Map<SootMethod, Set<SootMethod>> callgraph;
	List<File> appClassPath = new ArrayList<File>();
	List<File> appTestPath = new ArrayList<File>();
	List<File> libJarPath = new ArrayList<File>();
	private File original;
	private File backup;

	@After
	public void after(){
		appClassPath.clear();
		appTestPath.clear();
		libJarPath.clear();
		callgraph = null;
		original = null;
		backup.delete();
		backup = null;
		G.reset();
	}

	public Set<File> getClasspaths(){
		Set<File> toReturn = new HashSet<File>();
		toReturn.addAll(appTestPath);
		toReturn.addAll(appClassPath);
		toReturn.addAll(libJarPath);
		return toReturn;
	}

	@Before
	public void setup(){
		ClassLoader classLoader = MethodInlinerTest.class.getClassLoader();
		original = new File(classLoader.getResource("simple-test-project").getFile());

		try{
			backup = File.createTempFile("backup", "");
			backup.delete();
			FileUtils.copyDirectory(original,backup);
		} catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

		appClassPath.add(new File(backup.getAbsolutePath()
				+ File.separator + "target" + File.separator + "classes"));
		appTestPath.add(new File(backup.getAbsolutePath()
				+ File.separator + "target" + File.separator + "test-classes"));
		libJarPath.add(new File(backup.getAbsolutePath()
				+ File.separator + "libs" + File.separator + "standard-stuff-library.jar"));

		EntryPointProcessor epp = new EntryPointProcessor(true, false, false,
				false, new HashSet<MethodData>() );

		CallGraphAnalysis callGraphAnalysis =
				new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, epp,false);
		callGraphAnalysis.setup();
		callGraphAnalysis.run();

		SootUtils.setup_trimming(libJarPath,appClassPath,appTestPath);
		this.callgraph = SootUtils.mergeCallGraphMaps(
				SootUtils.convertMethodDataCallGraphToSootMethodCallGraph(callGraphAnalysis.getUsedAppMethods()),
				SootUtils.convertMethodDataCallGraphToSootMethodCallGraph(callGraphAnalysis.getUsedLibMethods()));
	}

	@Test
	public void inlineMethodsTest() throws IOException{
		Set<File> decompressedJars = ClassFileUtils.extractJars(new ArrayList<File>(getClasspaths()));
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>"))
			.contains(getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")));
		assertEquals(1, inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")).size());
		assertTrue(inlineData.getInlineLocations().get(getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>"))
			.contains(getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")));
		assertEquals(1, inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")).size());
		assertTrue(inlineData.getInlineLocations().get(getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>"))
			.contains(getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		ClassFileUtils.compressJars(decompressedJars);
	}

	@Test
	public void inlineMethodsTest_withoutDecompressedJars() throws IOException{
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>"))
			.contains(getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")));
		assertEquals(1, inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")).size());
		assertTrue(inlineData.getInlineLocations().get(getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>"))
			.contains(getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")));
		assertEquals(1, inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")).size());
		assertTrue(inlineData.getInlineLocations().get(getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>"))
			.contains(getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));
	}

	@Test
	public void inlineMethodsTest_withClassRewrite() throws IOException{
		Set<File> decompressedJars = ClassFileUtils.extractJars(new ArrayList<File>(getClasspaths()));
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>"))
			.contains(getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")));
		assertEquals(1, inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")).size());
		assertTrue(inlineData.getInlineLocations().get(getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>"))
			.contains(getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")));
		assertEquals(1, inlineData.getInlineLocations()
			.get(getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")).size());
		assertTrue(inlineData.getInlineLocations().get(getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>"))
			.contains(getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("StandardStuff"), getClasspaths());
		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("StandardStuff$NestedClass"), getClasspaths());
		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("edu.ucla.cs.onr.test.LibraryClass"), getClasspaths());
		ClassFileUtils.compressJars(decompressedJars);
	}

	private static MethodData getMethodDataFromSignature(String signature) throws IOException{
		signature = signature.trim();
		if(!signature.startsWith("<") || !signature.endsWith(">")){
			throw new IOException("Signature must start with with '<' and end with '>'");
		}

		signature = signature.substring(1,signature.length()-1);
		String[] signatureSplit = signature.split(":");

		if(signatureSplit.length != 2){
			throw new IOException("Method signature must be in format of " +
				"'<[classname]:[public?] [static?] [returnType] [methodName]([args...?])>'");
		}

		String clName = signatureSplit[0];
		String methodString = signatureSplit[1];

		boolean publicMethod;
		if (methodString.toLowerCase().contains("public")) publicMethod = true;
		else publicMethod = false;
		boolean staticMethod = methodString.toLowerCase().contains("static");

		Pattern pattern = Pattern.compile("<?([a-zA-Z][a-zA-Z0-9_]*>?)(\\(.*\\))");
		Matcher matcher = pattern.matcher(methodString);

		if(!matcher.find()){
			throw new IOException("Could not find a method matching our regex pattern ('" + pattern.toString() + "')");
		}

		String method = matcher.group();
		String methodName = method.substring(0,method.indexOf('('));
		String[] methodArgs = method.substring(method.indexOf('(')+1, method.lastIndexOf(')'))
			.split(",");

		for(int i=0; i<methodArgs.length; i++){
			methodArgs[i] = methodArgs[i].trim();
		}

		if(methodArgs.length == 1 && methodArgs[0].isEmpty()){ //For case "... method();
			methodArgs = new String[0];
		}

		String[] temp = methodString.substring(0, methodString.indexOf(methodName)).trim().split("\\s+");
		String methodReturnType = temp[temp.length-1];

		return new MethodData(methodName,clName,methodReturnType, methodArgs, publicMethod, staticMethod);
	}
}
