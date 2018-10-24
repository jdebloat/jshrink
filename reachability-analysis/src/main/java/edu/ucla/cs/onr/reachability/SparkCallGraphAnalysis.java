package edu.ucla.cs.onr.reachability;

import java.io.File;
import java.util.*;

import edu.ucla.cs.onr.util.ASMUtils;
import edu.ucla.cs.onr.util.EntryPointUtil;
import edu.ucla.cs.onr.util.SootUtils;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;

public class SparkCallGraphAnalysis {
	public static boolean DEBUG = true;
	
	private List<File> libJarPath;
	private List<File> appClassPath;
	private List<File> appTestPath;
	private Set<String> entryMethods; // in the 'className:methodName' format

	private Set<String> libClasses;
	private Set<String> libMethods;
	private Set<String> appClasses;
	private Set<String> appMethods;
	private Set<String> usedLibClasses;
	private Set<String> usedLibMethods;
	private Set<String> usedAppClasses;
	private Set<String> usedAppMethods;

	public SparkCallGraphAnalysis(List<File> libJarPath,
	                              List<File> appClassPath, 
	                              List<File> appTestPath, 
	                              Set<String> entryMethods) {
		this.libJarPath = libJarPath;
		this.appClassPath = appClassPath;
		this.appTestPath = appTestPath;
		this.entryMethods = entryMethods;

		libClasses = new HashSet<String>();
		libMethods = new HashSet<String>();
		appClasses = new HashSet<String>();
		appMethods = new HashSet<String>();
		usedLibClasses = new HashSet<String>();
		usedLibMethods = new HashSet<String>();
		usedAppClasses = new HashSet<String>();
		usedAppMethods = new HashSet<String>();
	}

	public void run() {
		// 1. use ASM to find all classes and methods 
		this.findAllClassesAndMethods();

		// 2. use Spark to construct the call graph and compute the reachable classes and methods
		this.runCallGraphAnalysis();
	}

	private void findAllClassesAndMethods() {
		for (File lib : this.libJarPath) {
			ASMUtils.readClassFromJarFile(lib, libClasses, libMethods);
		}

		for (File appPath : appClassPath) {
			ASMUtils.readClassFromDirectory(appPath, appClasses, appMethods);
		}

		for (File testPath : appTestPath) {
			ASMUtils.readClassFromDirectory(testPath, appClasses, appMethods);
		}
		
		if(DEBUG) {
			System.out.println("Num of library classes : " + libClasses.size());
			System.out.println("Num of library methods : " + libMethods.size());
			System.out.println("Num of application classes : " + appClasses.size());
			System.out.println("Num of application methods : " + appMethods.size());
		}
	}

	private void runCallGraphAnalysis() {
		// must call this first, and we only need to call it once
		SootUtils.setup(this.libJarPath, this.appClassPath, this.appTestPath);

		List<SootMethod> entryPoints = EntryPointUtil.convertToSootMethod(entryMethods);

		Scene.v().setEntryPoints(entryPoints);

		CHATransformer.v().transform();
//		Map<String, String> opt = SootUtils.getSparkOpt();
//		SparkTransformer.v().transform("", opt);

		CallGraph cg = Scene.v().getCallGraph();

		Set<String> usedMethods = new HashSet<String>();
		Set<String> usedClasses = new HashSet<String>();

		for (SootMethod entryMethod : entryPoints) {
			SootUtils.visitMethodNonRecur(entryMethod, cg, usedClasses, usedMethods);
		}

		// check for used library classes and methods
		this.usedLibClasses.addAll(this.libClasses);
		this.usedLibClasses.retainAll(usedClasses);
		this.usedLibMethods.addAll(this.libMethods);
		this.usedLibMethods.retainAll(usedMethods);

		// check for used application classes and methods
		this.usedAppClasses.addAll(this.appClasses);
		this.usedAppClasses.retainAll(usedClasses);
		this.usedAppMethods.addAll(this.appMethods);
		this.usedAppMethods.retainAll(usedMethods);
		
		if(DEBUG) {
			System.out.println("Num of used library classes : " + usedLibClasses.size());
			System.out.println("Num of used library methods : " + usedLibMethods.size());
			System.out.println("Num of used application classes : " + usedAppClasses.size());
			System.out.println("Num of used application methods : " + usedAppMethods.size());
		}
	}

	public Set<String> getLibClasses() {
		return Collections.unmodifiableSet(this.libClasses);
	}

	public Set<String> getLibMethods() {
		return Collections.unmodifiableSet(this.libMethods);
	}

	public Set<String> getAppClasses() {
		return Collections.unmodifiableSet(this.appClasses);
	}

	public Set<String> getAppMethods() {
		return Collections.unmodifiableSet(this.appMethods);
	}

	public Set<String> getUsedLibClasses() {
		return Collections.unmodifiableSet(this.usedLibClasses);
	}

	public Set<String> getUsedLibMethods() {
		return Collections.unmodifiableSet(this.usedLibMethods);
	}

	public Set<String> getUsedAppClasses() {
		return Collections.unmodifiableSet(this.usedAppClasses);
	}

	public Set<String> getUsedAppMethods() {
		return Collections.unmodifiableSet(this.usedAppMethods);
	}
}
