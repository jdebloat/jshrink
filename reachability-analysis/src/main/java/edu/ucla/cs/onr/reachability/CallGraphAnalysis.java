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

public class CallGraphAnalysis implements IProjectAnalyser {
	public static boolean useSpark = true; // use Spark by default

	private final List<File> libJarPath;
	private final List<File> appClassPath;
	private final List<File> appTestPath;
	private final Set<MethodData> entryMethods;
	private final Set<String> libClasses;
	private final Set<MethodData> libMethods;
	private final Set<String> appClasses;
	private final Set<MethodData> appMethods;
	private final Set<String> usedLibClasses;
	private final Set<MethodData> usedLibMethods;
	private final Set<String> usedAppClasses;
	private final Set<MethodData> usedAppMethods;
	private final Set<MethodData> testMethods;
	private final Set<String> testClasses;
	private final EntryPointProcessor entryPointProcessor;
	private final boolean verbose;

	public CallGraphAnalysis(List<File> libJarPath,
	                              List<File> appClassPath, 
	                              List<File> appTestPath, 
	                              EntryPointProcessor entryPointProc,
							 		boolean isVerbose) {
		this.libJarPath = libJarPath;
		this.appClassPath = appClassPath;
		this.appTestPath = appTestPath;
		this.entryMethods = new HashSet<MethodData>();

		libClasses = new HashSet<String>();
		libMethods = new HashSet<MethodData>();
		appClasses = new HashSet<String>();
		appMethods = new HashSet<MethodData>();
		usedLibClasses = new HashSet<String>();
		usedLibMethods = new HashSet<MethodData>();
		usedAppClasses = new HashSet<String>();
		usedAppMethods = new HashSet<MethodData>();
		testClasses = new HashSet<String>();
		testMethods = new HashSet<MethodData>();
		entryPointProcessor = entryPointProc;
		verbose = isVerbose;
	}

	@Override
	public void setup() {
		// 1. use ASM to find all classes and methods
		//TODO: We assume "setup()" has not already been run; there is potential for errors.
		this.findAllClassesAndMethods();
		this.entryMethods.addAll(this.entryPointProcessor.getEntryPoints(appMethods,libMethods,testMethods));
	}

	@Override
	public void run() {
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

		for (File testPath : this.appTestPath){
			ASMUtils.readClass(testPath,testClasses,testMethods);
		}
	}

	private void runCallGraphAnalysis() {
		// must call this first, and we only need to call it once
		SootUtils.setup_trimming(this.libJarPath, this.appClassPath, this.appTestPath);

		if(this.verbose) {
			for (MethodData methodData : entryMethods) {
				System.out.println("entry_point," + methodData.getSignature());
			}
		}

		List<SootMethod> entryPoints = EntryPointUtil.convertToSootMethod(entryMethods);
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
		
		if(this.verbose) {
			System.out.println("number_lib_classes," + libClasses.size());
			System.out.println("number_lib_methods," + libMethods.size());
			System.out.println("number_app_classes," + appClasses.size());
			System.out.println("number_app_methods," + appMethods.size());
			System.out.println("number_used_lib_classes," + usedLibClasses.size());
			System.out.println("number_used_lib_methods," + usedLibMethods.size());
			System.out.println("number_used_app_classes," + usedAppClasses.size());
			System.out.println("number_used_app_method," + usedAppMethods.size());
		}
	}

	@Override
	public Set<String> getLibClasses() {
		return Collections.unmodifiableSet(this.libClasses);
	}

	@Override
	public Set<MethodData> getLibMethods() {
		return Collections.unmodifiableSet(this.libMethods);
	}

	@Override
	public Set<String> getAppClasses() {
		return Collections.unmodifiableSet(this.appClasses);
	}

	@Override
	public Set<MethodData> getAppMethods() {
		return Collections.unmodifiableSet(this.appMethods);
	}

	@Override
	public Set<String> getUsedLibClasses() {
		return Collections.unmodifiableSet(this.usedLibClasses);
	}

	@Override
	public Set<MethodData> getUsedLibMethods() {
		return Collections.unmodifiableSet(this.usedLibMethods);
	}

	@Override
	public Set<String> getUsedAppClasses() {
		return Collections.unmodifiableSet(this.usedAppClasses);
	}

	@Override
	public Set<MethodData> getUsedAppMethods() {
		return Collections.unmodifiableSet(this.usedAppMethods);
	}

	@Override
	public List<File> getAppClasspaths() {
		return Collections.unmodifiableList(this.appClassPath);
	}

	@Override
	public List<File> getLibClasspaths() {
		return Collections.unmodifiableList(this.libJarPath);
	}

	@Override
	public List<File> getTestClasspaths() {
		return Collections.unmodifiableList(this.appTestPath);
	}

	@Override
	public Set<MethodData> getEntryPoints() {
		return Collections.unmodifiableSet(this.entryMethods);
	}
}
