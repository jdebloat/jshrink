package edu.ucla.cs.onr.reachability;

import java.io.File;
import java.util.*;

import edu.ucla.cs.onr.Application;
import edu.ucla.cs.onr.util.ASMUtils;
import edu.ucla.cs.onr.util.EntryPointUtil;
import edu.ucla.cs.onr.util.SootUtils;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;

public class CallGraphAnalysis {
	public static boolean useSpark = true; // use Spark by default

	private List<File> libJarPath;
	private List<File> appClassPath;
	private List<File> appTestPath;
	private Set<MethodData> entryMethods;

	private Set<String> libClasses;
	private Set<MethodData> libMethods;
	private Set<String> appClasses;
	private Set<MethodData> appMethods;
	private Set<String> usedLibClasses;
	private Set<MethodData> usedLibMethods;
	private Set<String> usedAppClasses;
	private Set<MethodData> usedAppMethods;

	public CallGraphAnalysis(List<File> libJarPath,
	                              List<File> appClassPath, 
	                              List<File> appTestPath, 
	                              Set<MethodData> entryMethods) {
		this.libJarPath = libJarPath;
		this.appClassPath = appClassPath;
		this.appTestPath = appTestPath;
		this.entryMethods = entryMethods;

		libClasses = new HashSet<String>();
		libMethods = new HashSet<MethodData>();
		appClasses = new HashSet<String>();
		appMethods = new HashSet<MethodData>();
		usedLibClasses = new HashSet<String>();
		usedLibMethods = new HashSet<MethodData>();
		usedAppClasses = new HashSet<String>();
		usedAppMethods = new HashSet<MethodData>();
	}

	public void run() {
		// 1. use ASM to find all classes and methods 
		this.findAllClassesAndMethods();

		// 2. use Spark to construct the call graph and compute the reachable classes and methods
		this.runCallGraphAnalysis();
	}

	private void findAllClassesAndMethods() {
		for (File lib : this.libJarPath) {
			ASMUtils.readClass(lib, libClasses, libMethods);
		}

		for (File appPath : appClassPath) {
			ASMUtils.readClass(appPath, appClasses, appMethods);
		}

		if(Application.isVerboseMode()) {
			System.out.println("number_lib_classes," + libClasses.size());
			System.out.println("number_lib_methods," + libMethods.size());
			System.out.println("number_app_classes," + appClasses.size());
			System.out.println("number_app_methods," + appMethods.size());
		}
	}

	private void runCallGraphAnalysis() {
		// must call this first, and we only need to call it once
		SootUtils.setup_trimming(this.libJarPath, this.appClassPath, this.appTestPath);

		List<SootMethod> entryPoints = EntryPointUtil.convertToSootMethod(entryMethods);

		if(Application.isVerboseMode()) {
			for (SootMethod sootMethod : entryPoints) {
				System.out.println("entry_point," + sootMethod.getSignature());
			}
		}

		Scene.v().setEntryPoints(entryPoints);

		if(useSpark) {
			Map<String, String> opt = SootUtils.getSparkOpt();
			SparkTransformer.v().transform("", opt);
		} else {
			CHATransformer.v().transform();
		}
		

		CallGraph cg = Scene.v().getCallGraph();

		Set<MethodData> usedMethods = new HashSet<MethodData>();
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
		
		if(Application.isVerboseMode()) {
			System.out.println("number_used_lib_classes," + usedLibClasses.size());
			System.out.println("number_used_lib_methods," + usedLibMethods.size());
			System.out.println("number_used_app_classes," + usedAppClasses.size());
			System.out.println("number_used_app_method," + usedAppMethods.size());
		}
	}

	public Set<String> getLibClasses() {
		return Collections.unmodifiableSet(this.libClasses);
	}

	public Set<MethodData> getLibMethods() {
		return Collections.unmodifiableSet(this.libMethods);
	}

	public Set<String> getAppClasses() {
		return Collections.unmodifiableSet(this.appClasses);
	}

	public Set<MethodData> getAppMethods() {
		return Collections.unmodifiableSet(this.appMethods);
	}

	public Set<String> getUsedLibClasses() {
		return Collections.unmodifiableSet(this.usedLibClasses);
	}

	public Set<MethodData> getUsedLibMethods() {
		return Collections.unmodifiableSet(this.usedLibMethods);
	}

	public Set<String> getUsedAppClasses() {
		return Collections.unmodifiableSet(this.usedAppClasses);
	}

	public Set<MethodData> getUsedAppMethods() {
		return Collections.unmodifiableSet(this.usedAppMethods);
	}
}
