package edu.ucla.cs.onr.reachability;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import soot.G;

public class CallGraphAnalysisSimpleTest {
	
	/* Class Hierarchy for the sample project
	 *    A
	 *   / \
	 *  B   C
	 *  
	 * There are four main classes that demonstrate different usage scenarios.
	 * There are is also one test file with three test cases. 
	 *  
	 */
	
	/**
	 * Test 1. 
	 * 
	 * A a = new B("a", "b");
	 * a.foo();
	 * 
	 * Because Soot assigns the most narrow type to a local variable when generating Jimple,
	 * the declared type of the local variable, a, in the Jimple code is B. 
	 * Since B does not have subclasses, both CHA and Spark generates the same call graph 
	 * for this program.
	 * 
	 */
	@Test
	public void testSparkOnDynamicDispatching1() {
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(3, runner.getUsedAppClasses().size());
        assertEquals(5, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testCHAOnDynamicDispatching1() {
		// disable Spark and use CHA instead
		CallGraphAnalysis.useSpark = false;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(3, runner.getUsedAppClasses().size());
        assertEquals(5, runner.getUsedAppMethods().size());
	}
	
	/**
	 * Test 2. 
	 * 
	 * A a = new A("a");
	 * a.foo();
	 * 
	 * The declared type of the local variable, a, is A.
	 * Spark knows that its real type is A since it keeps track of object allocations.
	 * But CHA does not know that so it looks up to the class hierarchy and finds that
	 * A has two subclasses B and C. So it thinks a.foo() can also be dispatched to B.foo()
	 * and C.foo() and therefore generates a bigger call graph.
	 *    
	 */
	@Test
	public void testSparkOnDynamicDispatching2() {
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main2", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(2, runner.getUsedAppClasses().size());
        assertEquals(3, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testCHAOnDynamicDispatching2() {
		// disable Spark and use CHA instead
		CallGraphAnalysis.useSpark = false;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main2", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(4, runner.getUsedAppClasses().size());
        assertEquals(6, runner.getUsedAppMethods().size());
	}
	
	/**
	 * Test 3. 
	 * 
	 * void main() {
	 *   A b = new B("a", "b");
	 *   delegate(b);
	 * }
	 *   
	 * void delegate(A a) {
	 * 	 a.foo();
	 * }
	 * 
	 * The declared type of the local variable, a, is B.
	 * Spark knows that its real type is A since it keeps track of object allocations
	 * and assignments across procedures.  
	 * But CHA does not know that so it looks up to the class hierarchy and finds that
	 * A has two subclasses B and C. So it thinks a.foo() can also be dispatched to B.foo()
	 * and C.foo() and therefore generates a bigger call graph.
	 *    
	 */
	@Test
	public void testSparkOnDynamicDispatching3() {
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main3", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(3, runner.getUsedAppClasses().size());
        assertEquals(6, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testCHAOnDynamicDispatching3() {
		// disable Spark and use CHA instead
		CallGraphAnalysis.useSpark = false;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main3", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(4, runner.getUsedAppClasses().size());
        assertEquals(8, runner.getUsedAppMethods().size());
	}
	
	/**
	 * Test 3. 
	 * 
	 * void main() {
	 *   A b = new B("a", "b");
	 *   A c = new C("a", 1);
	 *   delegate(b);
	 * }
	 *   
	 * void delegate(A a) {
	 * 	 a.foo();
	 * }
	 * 
	 * Just one more test to show that Spark keeps track of data flow across procedures.
	 * Though other algorithms such as RTA also keeps track of object allocations, but RTA
	 * does not perform points-to analysis but only estimate possible types just based on 
	 * previous object allocations. 
	 *    
	 */
	@Test
	public void testSparkOnDynamicDispatching4() {
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main4", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(4, runner.getUsedAppClasses().size());
        assertEquals(7, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testCHAOnDynamicDispatching4() {
		// disable Spark and use CHA instead
		CallGraphAnalysis.useSpark = false;
		ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
		List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project2/target/classes").getFile()));
        List<File> appTestPath = new ArrayList<File>(); 
        appTestPath.add(new File(classLoader.getResource("simple-test-project2/target/test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry = 
        		new MethodData("main", "Main4", "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner = 
        		new CallGraphAnalysis(libJarPath, appClassPath, appTestPath, new EntryPointProcessor(false, false, false,false, entryMethods));
        runner.run();
        assertEquals(4, runner.getUsedAppClasses().size());
        assertEquals(9, runner.getUsedAppMethods().size());
	}
	
	@After
	public void cleanup() {
		G.reset();
	}
}
