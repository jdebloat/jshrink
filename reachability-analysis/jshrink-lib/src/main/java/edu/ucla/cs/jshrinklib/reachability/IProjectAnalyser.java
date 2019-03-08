package edu.ucla.cs.jshrinklib.reachability;

import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IProjectAnalyser {

    /*
    Setup is needed to populate the classpaths (getAppClasspaths, getLibClasspaths, getTestClasspaths), which we need
    before running the callgraph analysis. Also, getTestOutput.
     */
    public void setup();
    public void run();
    public Set<String> getLibClasses();
    public Set<String> getLibClassesCompileOnly();
    public Set<MethodData> getLibMethods();
    public Set<MethodData> getLibMethodsCompileOnly();
    public Set<String> getAppClasses();
    public Set<MethodData> getAppMethods();
    public Set<String> getUsedLibClasses();
    public Set<String> getUsedLibClassesCompileOnly();
    public Map<MethodData,Set<MethodData>> getUsedLibMethods();
    public Map<MethodData,Set<MethodData>> getUsedLibMethodsCompileOnly();
    public Set<String> getUsedAppClasses();
    public Map<MethodData,Set<MethodData>> getUsedAppMethods();
    public List<File> getAppClasspaths();
    public List<File> getLibClasspaths();
    public List<File> getTestClasspaths();
    public Set<MethodData> getEntryPoints();
    public Set<CallGraph> getCallGraphs();
    public Set<String> classesToIgnore();
    public TestOutput getTestOutput();
}
