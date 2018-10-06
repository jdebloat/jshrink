package edu.ucla.cs.onr;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.io.FileUtils;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

public class SparkCallGraphAnalysis {
	private List<File> libJarPath;
	private List<File> appClassPath;
	private List<File> appTestPath;
	private Set<String> entryMethods;

	private Set<String> libClasses;
	private Set<String> libMethods;
	private Set<String> appClasses;
	private Set<String> appMethods;
	private Set<String> usedLibClasses;
	private Set<String> usedLibMethods;
	private Set<String> usedAppClasses;
	private Set<String> usedAppMethods;

	public SparkCallGraphAnalysis(List<File> libJarPath,
	                              List<File> appClassPath, List<File> appTestPath, Set<String> entryMethods) {
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

		this.run();
	}

	private void run() {
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
	}

	public static Set<String> getEntryPointsFromTestLog(List<File> libJarPath, List<File> appClassPath,
	                                              List<File> appTestPath, File testLog) {

		ArrayList<String> testClasses = new ArrayList<String>();
		try {
			List<String> lines = FileUtils.readLines(testLog, Charset.defaultCharset());
			for (String line : lines) {
				if (line.contains("Running ")) {
					String testClass = line.substring(line.indexOf("Running ") + 8);
					testClasses.add(testClass);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Set<String> entryPoints = new HashSet<String>();
		SootUtils.setupSoot(libJarPath,appClassPath,appTestPath);

		for(String testClass : testClasses){
			for(SootMethod sootMethod : Scene.v().getSootClass(testClass).getMethods()){
				entryPoints.add(sootMethod.getSignature());
			}
		}

		return entryPoints;

	}


	private void runCallGraphAnalysis() {
		SootUtils.setupSoot(this.libJarPath, this.appClassPath, this.appTestPath);

		List<SootMethod> entryPoints = new ArrayList<SootMethod>();
		for (String entry : this.entryMethods) {
			entryPoints.add(Scene.v().getMethod(entry));
		}

		Scene.v().setEntryPoints(entryPoints);

		Map<String, String> opt = SootUtils.getSparkOpt();
		SparkTransformer.v().transform("", opt);

		CallGraph cg = Scene.v().getCallGraph();

		Set<String> usedMethods = new HashSet<String>();
		Set<String> usedClasses = new HashSet<String>();

		for (SootMethod entryMethod : entryPoints) {
			SootUtils.visitMethod(entryMethod, cg, usedClasses, usedMethods);
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
