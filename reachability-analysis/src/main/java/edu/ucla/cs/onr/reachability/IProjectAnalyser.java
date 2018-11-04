package edu.ucla.cs.onr.reachability;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface IProjectAnalyser {
    public void setup();
    public void run();
    public Set<String> getLibClasses();
    public Set<MethodData> getLibMethods();
    public Set<String> getAppClasses();
    public Set<MethodData> getAppMethods();
    public Set<String> getUsedLibClasses();
    public Set<MethodData> getUsedLibMethods();
    public Set<String> getUsedAppClasses();
    public Set<MethodData> getUsedAppMethods();
    public List<File> getAppClasspaths();
    public List<File> getLibClasspaths();
    public List<File> getTestClasspaths();
    public Set<MethodData> getEntryPoints();
}
