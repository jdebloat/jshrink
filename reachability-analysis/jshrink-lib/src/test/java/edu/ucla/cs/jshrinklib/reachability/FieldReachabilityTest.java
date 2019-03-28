package edu.ucla.cs.jshrinklib.reachability;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

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
}
