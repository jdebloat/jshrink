package edu.ucla.cs.jshrinklib.methodinliner;

import edu.ucla.cs.jshrinklib.TestUtils;
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

	public void setup_simpleTestProject(){
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

	public void setup_packageInlinerTest(){
		ClassLoader classLoader = MethodInlinerTest.class.getClassLoader();
		original = new File(classLoader.getResource("package-inliner-test").getFile());

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
		setup_simpleTestProject();
		Set<File> decompressedJars = ClassFileUtils.extractJars(new ArrayList<File>(getClasspaths()));
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Object,java.lang.Object)>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff: public void <init>()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Object,java.lang.Object)>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		ClassFileUtils.compressJars(decompressedJars);
	}

	@Test
	public void inlineMethodsTest_withoutDecompressedJars() throws IOException{
		setup_simpleTestProject();
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());

		assertEquals(7, inlineData.getInlineLocations().size());

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Object,java.lang.Object)>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff: public void <init>()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Object,java.lang.Object)>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));
	}

	@Test
	public void inlineMethodsTest_withClassRewrite() throws IOException{
		setup_simpleTestProject();
		Set<File> decompressedJars = ClassFileUtils.extractJars(new ArrayList<File>(getClasspaths()));
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Integer,java.lang.Integer)>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main$1: public int compare(java.lang.Object,java.lang.Object)>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public java.lang.String getString()>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff: public void <init>()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Integer,java.lang.Integer)>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff$1: public int compare(java.lang.Object,java.lang.Object)>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: void nestedClassMethodCallee()>"))
			.contains(TestUtils.getMethodDataFromSignature("<StandardStuff$NestedClass: public void nestedClassMethod()>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<StandardStuff: public static java.lang.String getStringStatic(int)>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		assertTrue(inlineData.getInlineLocations().containsKey(
			TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")));
		assertEquals(1,inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>")).size());
		assertTrue(inlineData.getInlineLocations()
			.get(TestUtils.getMethodDataFromSignature("<edu.ucla.cs.onr.test.LibraryClass: public int getNumber()>"))
			.contains(TestUtils.getMethodDataFromSignature("<Main: public static void main(java.lang.String[])>")));

		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("StandardStuff"), getClasspaths());
		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("StandardStuff$NestedClass"), getClasspaths());
		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("edu.ucla.cs.onr.test.LibraryClass"), getClasspaths());
		ClassFileUtils.compressJars(decompressedJars);
	}

	@Test
	public void packageInlinerTest() throws IOException{
		setup_packageInlinerTest();
		Set<File> decompressedJars = ClassFileUtils.extractJars(new ArrayList<File>(getClasspaths()));
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());
		assertEquals(0, inlineData.getInlineLocations().size());
		ClassFileUtils.compressJars(decompressedJars);
	}
}
