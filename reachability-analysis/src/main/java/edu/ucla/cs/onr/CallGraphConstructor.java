package edu.ucla.cs.onr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

public class CallGraphConstructor {
	public static void main(String[] args) {
		String app_class_path = 
				"/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit4/target/classes";
		String app_test_path = 
				"/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit4/target/test-classes";
		String lib_class_path = "/home/troy/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
				+ ":/home/troy/.m2/repository/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar";
		String entryClassName = "junit.tests.AllTests";
		String entryMethodName = "main";
		
		// count the classes and methods in the library jars and application directories
		HashSet<String> libClasses = new HashSet<String>();
		HashSet<String> libMethods = new HashSet<String>();
		String[] libs = lib_class_path.split(File.pathSeparator);
		for(String lib : libs) {
			ASMUtils.readClassFromJarFile(lib, libClasses, libMethods);
		}
		HashSet<String> appClasses = new HashSet<String>();
		HashSet<String> appMethods = new HashSet<String>();
		ASMUtils.readClassFromDirectory(app_class_path, appClasses, appMethods);
		ASMUtils.readClassFromDirectory(app_test_path, appClasses, appMethods);
		System.out.println("Num of library classes : " + libClasses.size());
		System.out.println("Num of library methods : " + libMethods.size());
		System.out.println("Num of application classes : " + appClasses.size());
		System.out.println("Num of application methods : " + appMethods.size());
		
		String cp = SootUtils.getJREJars();
		cp += File.pathSeparator + lib_class_path;
		cp += File.pathSeparator + app_class_path;
		cp += File.pathSeparator + app_test_path;
		
		// set the Soot classpath and other options
		Options.v().set_soot_classpath(cp);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		
		// set the application directories
		List<String> dirs = new ArrayList<String>();
		dirs.add(app_class_path);
		dirs.add(app_test_path);
		Options.v().set_process_dir(dirs);
		SootClass entryClass = Scene.v().loadClassAndSupport(entryClassName);
		Scene.v().loadNecessaryClasses();
		ArrayList<SootMethod> entryPoints = new ArrayList<SootMethod>();
		SootMethod entryMethod = entryClass.getMethodByName(entryMethodName);
		entryPoints.add(entryMethod);
	    Scene.v().setEntryPoints(entryPoints);
	    
	    HashMap<String, String> opt = SootUtils.getSparkOpt();
		SparkTransformer.v().transform("",opt);

		CallGraph cg = Scene.v().getCallGraph();
		System.out.println("graph done");
		
		HashSet<String> usedMethods = new HashSet<String>();		
		HashSet<String> usedClasses = new HashSet<String>();
		
		SootUtils.visitMethod(entryMethod, cg, usedClasses, usedMethods);
		
		// check for used library classes and methods
		HashSet<String> usedLibClasses = new HashSet<String>(libClasses);
		usedLibClasses.retainAll(usedClasses);
		HashSet<String> usedLibMethods = new HashSet<String>(libMethods);
		usedLibMethods.retainAll(usedMethods);
		
		// check for used application classes and methods
		HashSet<String> usedAppClasses = new HashSet<String>(appClasses);
		usedAppClasses.retainAll(usedClasses);
		HashSet<String> usedAppMethods = new HashSet<String>(appMethods);
		usedAppMethods.retainAll(usedMethods);
		
		System.out.println("Num of used library classes : " + usedLibClasses.size());
		System.out.println("Num of used library methods : " + usedLibMethods.size());
		System.out.println("Num of used application classes : " + usedAppClasses.size());
		System.out.println("Num of used application methods : " + usedAppMethods.size());
	}
}
