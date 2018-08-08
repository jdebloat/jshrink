package edu.ucla.cs.onr;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

public class SparkCallGraphAnalysis {
	String lib_jar_path;
	String app_class_path;
	String app_test_path;
	String test_log_path;
	
	HashSet<String> libClasses;
	HashSet<String> libMethods;
	HashSet<String> appClasses;
	HashSet<String> appMethods;
	HashSet<String> usedLibClasses;
	HashSet<String> usedLibMethods;
	HashSet<String> usedAppClasses;
	HashSet<String> usedAppMethods;
	
	public SparkCallGraphAnalysis(String lib_jar_path, 
			String app_class_path, String app_test_path, String test_log_path) {
		this.lib_jar_path = lib_jar_path;
		this.app_class_path = app_class_path;
		this.app_test_path = app_test_path;
		this.test_log_path = test_log_path;
		
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
		findAllClassesAndMethods();
		
		// 2. use Spark to construct the call graph and compute the reachable classes and methods
		runCallGraphAnalysis();
	}

	private void findAllClassesAndMethods() {
		String[] libs = lib_jar_path.split(File.pathSeparator);
		for(String lib : libs) {
			ASMUtils.readClassFromJarFile(lib, libClasses, libMethods);
		}
		ASMUtils.readClassFromDirectory(app_class_path, appClasses, appMethods);
		ASMUtils.readClassFromDirectory(app_test_path, appClasses, appMethods);
		
		// for debug purposes only
		System.out.println("Num of library classes : " + libClasses.size());
		System.out.println("Num of library methods : " + libMethods.size());
		System.out.println("Num of application classes : " + appClasses.size());
		System.out.println("Num of application methods : " + appMethods.size());
	}
	
	private void runCallGraphAnalysis() {
		// set the Soot classpath and other options
		String cp = SootUtils.getJREJars();
		cp += File.pathSeparator + lib_jar_path;
		cp += File.pathSeparator + app_class_path;
		cp += File.pathSeparator + app_test_path;
		Options.v().set_soot_classpath(cp);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		
		// set the application directories
		List<String> dirs = new ArrayList<String>();
		dirs.add(app_class_path);
		dirs.add(app_test_path);
		Options.v().set_process_dir(dirs);
		
		// collect test cases from the test log file
		ArrayList<String> testClasses = new ArrayList<String>();
		try {
			List<String> lines = FileUtils.readLines(new File(test_log_path), Charset.defaultCharset());
			for(String line : lines) {
				if(line.contains("Running ")) {
					String testClass = line.substring(line.indexOf("Running ") + 8);
					testClasses.add(testClass);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ArrayList<SootMethod> entryPoints = new ArrayList<SootMethod>();
		// set all methods in a test class as entry points
//		for(String testClass : testClasses) {
//			SootClass entryClass = Scene.v().loadClassAndSupport(testClass);
//			Scene.v().loadNecessaryClasses();
//			List<SootMethod> methods = entryClass.getMethods();
//			for(SootMethod m : methods) {
//				entryPoints.add(m);
//			}
//		}
		// scan all methods and add all main methods as entry points except the main method is 
		// in a test class, since it has bee added already
		for(String s : appMethods) {
			String[] ss = s.split(": ");
			String className = ss[0];
			String methodName = ss[1];
			if(!testClasses.contains(className) &&
					methodName.equals("void main(java.lang.String[])")) {
				SootClass entryClass = Scene.v().loadClassAndSupport(className);
				Scene.v().loadNecessaryClasses();
				SootMethod entryMethod = entryClass.getMethodByName("main");
				entryPoints.add(entryMethod);
			}
		}
		
	    Scene.v().setEntryPoints(entryPoints);
	    
	    HashMap<String, String> opt = SootUtils.getSparkOpt();
		SparkTransformer.v().transform("",opt);

		CallGraph cg = Scene.v().getCallGraph();
		System.out.println("graph done");
		
		HashSet<String> usedMethods = new HashSet<String>();		
		HashSet<String> usedClasses = new HashSet<String>();
		
		for(SootMethod entryMethod : entryPoints) {
			SootUtils.visitMethod(entryMethod, cg, usedClasses, usedMethods);
		}
		
		// check for used library classes and methods
		usedLibClasses.addAll(libClasses);
		usedLibClasses.retainAll(usedClasses);
		usedLibMethods.addAll(libMethods);
		usedLibMethods.retainAll(usedMethods);
		
		// check for used application classes and methods
		usedAppClasses.addAll(appClasses);
		usedAppClasses.retainAll(usedClasses);
		usedAppMethods.addAll(appMethods);
		usedAppMethods.retainAll(usedMethods);
		
		// for the debugging purpose only
		System.out.println("Num of used library classes : " + usedLibClasses.size());
		System.out.println("Num of used library methods : " + usedLibMethods.size());
		System.out.println("Num of used application classes : " + usedAppClasses.size());
		System.out.println("Num of used application methods : " + usedAppMethods.size());
	}
}
