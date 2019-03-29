package edu.ucla.cs.jshrinklib.reachability;

import fj.P;
import org.junit.After;
import org.junit.Test;
import soot.G;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class FieldReachabilityTest {
    @Test
    public void testSimpleFieldAccess1() {
        // a simple case where only one of two fields in a class is referenced in a used method
        ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
        List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project3"
                + File.separator + "target" + File.separator + "classes").getFile()));
        List<File> appTestPath = new ArrayList<File>();
        appTestPath.add(new File(classLoader.getResource("simple-test-project3"
                + File.separator + "target" + File.separator + "test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry =
                new MethodData("testSameLastName", "StandardStuffTest",
                        "void", new String[] {}, true, false);
        entryMethods.add(entry);
        CallGraphAnalysis runner =
                new CallGraphAnalysis(libJarPath, appClassPath, appTestPath,
                        new EntryPointProcessor(false, false,
                                false,false, entryMethods), false);
        runner.run();
        assertEquals(0, runner.getLibFields().size());
        assertEquals(8, runner.getAppFields().size());
        assertEquals(1, runner.getUsedAppFields().size());
        assertEquals(0, runner.getUsedLibFields().size());
    }

    @Test
    public void testSimpleFieldAccess2() {
        // a simple case where both fields in a class is referenced in a used method
        ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
        List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project3"
                + File.separator + "target" + File.separator + "classes").getFile()));
        List<File> appTestPath = new ArrayList<File>();
        appTestPath.add(new File(classLoader.getResource("simple-test-project3"
                + File.separator + "target" + File.separator + "test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry =
                new MethodData("testSameName", "StandardStuffTest",
                        "void", new String[] {}, true, false);
        entryMethods.add(entry);
        CallGraphAnalysis runner =
                new CallGraphAnalysis(libJarPath, appClassPath, appTestPath,
                        new EntryPointProcessor(false, false,
                                false,false, entryMethods), false);
        runner.run();
        assertEquals(0, runner.getLibFields().size());
        assertEquals(8, runner.getAppFields().size());
        assertEquals(2, runner.getUsedAppFields().size());
        assertEquals(0, runner.getUsedLibFields().size());
    }

    @Test
    public void testStaticFieldAccess() {
        // a complex case where static fields are initialized in a static block, which are executed immediately after class loading
        // as a result, these static fields are always considered used
        ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
        List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("simple-test-project3"
                + File.separator + "target" + File.separator + "classes").getFile()));
        List<File> appTestPath = new ArrayList<File>();
        appTestPath.add(new File(classLoader.getResource("simple-test-project3"
                + File.separator + "target" + File.separator + "test-classes").getFile()));
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry =
                new MethodData("testA", "StandardStuffTest",
                        "void", new String[] {}, true, false);
        entryMethods.add(entry);
        CallGraphAnalysis runner =
                new CallGraphAnalysis(libJarPath, appClassPath, appTestPath,
                        new EntryPointProcessor(false, false,
                                false,false, entryMethods), false);
        runner.run();
        assertEquals(0, runner.getLibFields().size());
        assertEquals(8, runner.getAppFields().size());
        assertEquals(6, runner.getUsedAppFields().size());
        assertEquals(0, runner.getUsedLibFields().size());
    }

    @Test
    public void testSubTypingAndFieldInheritance() {
        // SubClass inherits f1 from SimpleClass
        ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
        List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("fieldwiper").getFile()));
        List<File> appTestPath = new ArrayList<File>();
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        MethodData entry =
                new MethodData("main", "SubClass",
                        "void", new String[] {"java.lang.String[]"}, true, true);
        entryMethods.add(entry);
        CallGraphAnalysis runner =
                new CallGraphAnalysis(libJarPath, appClassPath, appTestPath,
                        new EntryPointProcessor(false, false,
                                false,false, entryMethods), false);
        runner.run();
        assertEquals(2, runner.getUsedAppFields().size());
        assertEquals(0, runner.getUsedLibFields().size());

        FieldData inheritField = new FieldData("f1", "SubClass", false, "java.lang.String");
        FieldData originalField = new FieldData("f1", "SimpleClass", false, "java.lang.String");
        // this reference to an inherited field should be already resolved
        assertFalse(runner.getUsedAppFields().contains(inheritField));
        assertTrue(runner.getUsedAppFields().contains(originalField));

        Set<FieldData> fieldRefInMain = runner.getAppFieldReferences().get(entry);
        assertTrue(fieldRefInMain.contains(originalField));
        assertFalse(fieldRefInMain.contains(inheritField));
    }

    @Test
    public void testSubTypingAndFieldInheritance2() {
        // SubClass inherits f1 from SimpleClass
        ClassLoader classLoader = CallGraphAnalysisSimpleTest.class.getClassLoader();
        List<File> libJarPath = new ArrayList<File>();
        List<File> appClassPath = new ArrayList<File>();
        appClassPath.add(new File(classLoader.getResource("fieldwiper").getFile()));
        List<File> appTestPath = new ArrayList<File>();
        Set<MethodData> entryMethods = new HashSet<MethodData>();
        // use a public method as entry point
        MethodData entry =
                new MethodData("setValue", "SubClass",
                        "void", new String[] {"java.lang.String"}, true, false);
        entryMethods.add(entry);
        CallGraphAnalysis runner =
                new CallGraphAnalysis(libJarPath, appClassPath, appTestPath,
                        new EntryPointProcessor(false, false,
                                false,false, entryMethods), false);
        runner.run();
        assertEquals(1, runner.getUsedAppFields().size());
        assertEquals(0, runner.getUsedLibFields().size());

        FieldData inheritField = new FieldData("f1", "SubClass", false, "java.lang.String");
        FieldData originalField = new FieldData("f1", "SimpleClass", false, "java.lang.String");
        // this reference to an inherited field should be already resolved
        assertFalse(runner.getUsedAppFields().contains(inheritField));
        assertTrue(runner.getUsedAppFields().contains(originalField));

        Set<FieldData> fieldRefInMain = runner.getAppFieldReferences().get(entry);
        assertTrue(fieldRefInMain.contains(originalField));
        assertFalse(fieldRefInMain.contains(inheritField));
    }

    @After
    public void cleanup() {
        G.reset();
    }
}
