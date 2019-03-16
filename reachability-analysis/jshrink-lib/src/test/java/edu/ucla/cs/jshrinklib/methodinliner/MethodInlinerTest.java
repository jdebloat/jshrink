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
				"<StandardStuff$NestedClass: void nestedClassMethodCallee()>"));
		assertEquals(1,inlineData.getInlineLocations()
			.get("<StandardStuff$NestedClass: void nestedClassMethodCallee()>").size());
		assertTrue(inlineData.getInlineLocations()
			.get("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")
			.contains("<StandardStuff$NestedClass: void nestedClassMethod()>"));

		assertTrue(inlineData.getInlineLocations().containsKey(
				"<StandardStuff: java.lang.String getString()>"));
		assertEquals(1, inlineData.getInlineLocations()
			.get("<StandardStuff: java.lang.String getString()>").size());
		assertTrue(inlineData.getInlineLocations().get("<StandardStuff: java.lang.String getString()>")
			.contains("<Main: void main(java.lang.String[])>"));

		assertTrue(inlineData.getInlineLocations().containsKey(
				"<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>"));
		assertEquals(1, inlineData.getInlineLocations()
			.get("<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>").size());
		assertTrue(inlineData.getInlineLocations().get("<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>")
			.contains("<Main: void main(java.lang.String[])>"));

		ClassFileUtils.compressJars(decompressedJars);
	}

	@Test
	public void inlineMethodsTest_withoutDecompressedJars() throws IOException{
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());

		assertTrue(inlineData.getInlineLocations().containsKey(
			"<StandardStuff$NestedClass: void nestedClassMethodCallee()>"));
		assertEquals(1,inlineData.getInlineLocations()
			.get("<StandardStuff$NestedClass: void nestedClassMethodCallee()>").size());
		assertTrue(inlineData.getInlineLocations()
			.get("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")
			.contains("<StandardStuff$NestedClass: void nestedClassMethod()>"));

		assertTrue(inlineData.getInlineLocations().containsKey(
			"<StandardStuff: java.lang.String getString()>"));
		assertEquals(1, inlineData.getInlineLocations()
			.get("<StandardStuff: java.lang.String getString()>").size());
		assertTrue(inlineData.getInlineLocations().get("<StandardStuff: java.lang.String getString()>")
			.contains("<Main: void main(java.lang.String[])>"));

		assertTrue(inlineData.getInlineLocations().containsKey(
			"<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>"));
		assertEquals(1, inlineData.getInlineLocations()
			.get("<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>").size());
		assertTrue(inlineData.getInlineLocations().get("<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>")
			.contains("<Main: void main(java.lang.String[])>"));
	}

	@Test
	public void inlineMethodsTest_withClassRewrite() throws IOException{
		Set<File> decompressedJars = ClassFileUtils.extractJars(new ArrayList<File>(getClasspaths()));
		InlineData inlineData = MethodInliner.inlineMethods(this.callgraph, getClasspaths());

		assertTrue(inlineData.getInlineLocations().containsKey(
			"<StandardStuff$NestedClass: void nestedClassMethodCallee()>"));
		assertEquals(1,inlineData.getInlineLocations()
			.get("<StandardStuff$NestedClass: void nestedClassMethodCallee()>").size());
		assertTrue(inlineData.getInlineLocations()
			.get("<StandardStuff$NestedClass: void nestedClassMethodCallee()>")
			.contains("<StandardStuff$NestedClass: void nestedClassMethod()>"));

		assertTrue(inlineData.getInlineLocations().containsKey(
			"<StandardStuff: java.lang.String getString()>"));
		assertEquals(1, inlineData.getInlineLocations()
			.get("<StandardStuff: java.lang.String getString()>").size());
		assertTrue(inlineData.getInlineLocations().get("<StandardStuff: java.lang.String getString()>")
			.contains("<Main: void main(java.lang.String[])>"));

		assertTrue(inlineData.getInlineLocations().containsKey(
			"<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>"));
		assertEquals(1, inlineData.getInlineLocations()
			.get("<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>").size());
		assertTrue(inlineData.getInlineLocations().get("<edu.ucla.cs.onr.test.LibraryClass: int getNumber()>")
			.contains("<Main: void main(java.lang.String[])>"));

		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("StandardStuff"), getClasspaths());
		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("StandardStuff$NestedClass"), getClasspaths());
		ClassFileUtils.writeClass(
			Scene.v().loadClassAndSupport("edu.ucla.cs.onr.test.LibraryClass"), getClasspaths());
		ClassFileUtils.compressJars(decompressedJars);
	}
}
