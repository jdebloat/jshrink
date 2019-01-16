package edu.ucla.cs.onr.classcollapser;

import java.io.File;
import java.util.*;

import edu.ucla.cs.onr.reachability.*;
import edu.ucla.cs.onr.util.ASMUtils;
import edu.ucla.cs.onr.util.EntryPointUtil;
import edu.ucla.cs.onr.util.SootUtils;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;

public class ClassCollapserCallGraphAnalysis implements IProjectAnalyser {
	public static boolean useSpark = true; // use Spark by default

	private final List<File> libJarPath;
	private final List<File> appClassPath;
	private final List<File> appTestPath;
	private final Set<MethodData> entryMethods;
	private final Set<String> libClasses;
	private final HashMap<String, String> classToLib;
	private final Set<MethodData> libMethods;
	private final HashMap<MethodData, String> methodToLib;
	private final Set<String> appClasses;
	private final Set<MethodData> appMethods;
	private final Set<String> usedLibClasses;
	private final Set<MethodData> usedLibMethods;
	private final Set<String> usedAppClasses;
	private final Set<MethodData> usedAppMethods;
	private final Set<MethodData> testMethods;
	private final Set<String> testClasses;
	private final EntryPointProcessor entryPointProcessor;

	public ClassCollapserCallGraphAnalysis(List<File> libJarPath,
							 List<File> appClassPath,
							 List<File> appTestPath,
							 EntryPointProcessor entryPointProc) {
		this.libJarPath = libJarPath;
		this.appClassPath = appClassPath;
		this.appTestPath = appTestPath;
		this.entryMethods = new HashSet<MethodData>();

		libClasses = new HashSet<String>();
		classToLib = new HashMap<String, String>();
		libMethods = new HashSet<MethodData>();
		methodToLib = new HashMap<MethodData, String>();
		appClasses = new HashSet<String>();
		appMethods = new HashSet<MethodData>();
		usedLibClasses = new HashSet<String>();
		usedLibMethods = new HashSet<MethodData>();
		usedAppClasses = new HashSet<String>();
		usedAppMethods = new HashSet<MethodData>();
		testClasses = new HashSet<String>();
		testMethods = new HashSet<MethodData>();
		entryPointProcessor = entryPointProc;
	}

	@Override
	public void setup() {
		/* Setup is used to get the app/test/lib classpath information. In ClassGraphAnalysis, this is given via the
		constructor and, therefore, does not need generated as in MavenProjectAnalysis
		 */
	}

	@Override
	public void run() {
		// 1. use ASM to find all classes and methods
		this.findAllClassesAndMethods();
		// 2. get entry points
		this.entryMethods.addAll(this.entryPointProcessor.getEntryPoints(appMethods,libMethods,testMethods));
		// 3. construct the call graph and compute the reachable classes and methods
		this.runCallGraphAnalysis();
	}


	/**
	 *
	 * This method is used to run Spark/CHA given a set of entry methods found by TamiFlex
	 *
	 * @param entryPoints
	 */
	public void run(Set<MethodData> entryPoints) {
		// 1. use ASM to find all classes and methods
		this.findAllClassesAndMethods();

		// clear just in case this method is misused---should not call this method twice or call this
		// after calling the overriden run method
		if(!this.entryMethods.isEmpty()) {
			this.entryMethods.clear();
		}

		// 2. add the given entry points
		entryMethods.addAll(entryPoints);


		// 3. run call graph analysis
		this.runCallGraphAnalysis();
	}

	private void findAllClassesAndMethods() {
		for (File lib : this.libJarPath) {
			HashSet<String> classes_in_this_lib = new HashSet<String>();
			HashSet<MethodData> methods_in_this_lib = new HashSet<MethodData>();
			ASMUtils.readClass(lib, classes_in_this_lib, methods_in_this_lib);
			this.libClasses.addAll(classes_in_this_lib);
			this.libMethods.addAll(methods_in_this_lib);

			String lib_path = lib.getAbsolutePath();
			for(String class_name : classes_in_this_lib) {
				classToLib.put(class_name, lib_path);
			}
			for(MethodData md : methods_in_this_lib) {
				methodToLib.put(md, lib_path);
			}
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

		List<SootMethod> entryPoints = EntryPointUtil.convertToSootMethod(entryMethods);
		Scene.v().setEntryPoints(entryPoints);

		if(useSpark) {
			Map<String, String> opt = SootUtils.getSparkOpt();
			SparkTransformer.v().transform("", opt);
		} else {
			CHATransformer.v().transform();
		}

//		System.out.println("call graph analysis starts.");
//		System.out.println(entryPoints.size() + " entry points.");
		CallGraph cg = Scene.v().getCallGraph();
//		System.out.println("call graph analysis done.");

		Set<MethodData> usedMethods = new HashSet<MethodData>();
		Set<String> usedClasses = new HashSet<String>();

		for (SootMethod entryMethod : entryPoints) {
			SootUtils.visitMethodClassCollapser(entryMethod, cg, usedClasses, usedMethods);
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

	@Override
	public Set<String> getLibClasses() {
		return Collections.unmodifiableSet(this.libClasses);
	}

	public String getLibPathOfClass(String libClass) {
		return this.classToLib.get(libClass);
	}

	@Override
	public Set<MethodData> getLibMethods() {
		return Collections.unmodifiableSet(this.libMethods);
	}

	public String getLibPathOfMethod(MethodData methodData) {
		return this.methodToLib.get(methodData);
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

	@Override
	public Set<String> getUsedLibClassesCompileOnly() {
		return this.getUsedLibClasses();
	}

	@Override
	public Set<MethodData> getUsedLibMethodsCompileOnly() {
		return this.getUsedLibMethods();
	}

	@Override
	public Set<String> getLibClassesCompileOnly() {
		return this.getLibClasses();
	}

	@Override
	public Set<MethodData> getLibMethodsCompileOnly() {
		return this.getLibMethods();
	}

	public Set<String> getTestClasses() {return this.testClasses;}
}
