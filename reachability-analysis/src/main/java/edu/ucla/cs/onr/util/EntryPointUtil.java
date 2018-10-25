package edu.ucla.cs.onr.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ucla.cs.onr.reachability.MethodData;

import org.apache.commons.io.FileUtils;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class EntryPointUtil {

	public static Set<String> testAnnotations(){
		Set<String> toReturn = new HashSet<String>();
		toReturn.add("org.junit.Test");
		toReturn.add("org.junit.Before");
		toReturn.add("org.junit.After");
		toReturn.add("org.junit.AfterClass");
		toReturn.add("org.junit.BeforeClass");
		toReturn.add("org.junit.runner.Runwith");
		toReturn.add("org.junit.Rule");
		toReturn.add("org.junit.experimental.theories.Theory");
		toReturn.add("org.junit.experimental.theories.DataPoint");

		return toReturn;
	}

	/**
	 * This method gets a list of test methods from a test log file.
	 * Now we consider all methods in a test class as test methods, 
	 * represented in the 'className:*' format
	 * 
	 * @param testLog
	 * @return
	 */
	public static Set<MethodData> getTestMethodsAsEntryPoints(File test_log, File test_classes) {
		HashSet<String> testClasses = new HashSet<String>();
		Set<MethodData> testMethods = new HashSet<MethodData>();
		ASMUtils.readClass(test_classes, testClasses, testMethods);
		
		Set<String> executedTests = getTestMethodsAsEntryPoints(test_log);
		
		Set<MethodData> executedTestMethods = new HashSet<MethodData>();
        for(String s : executedTests) {
        	String[] ss = s.split(":");
			String className = ss[0];
			String methodName = ss[1];
			if(methodName.equals("*")) {
				// all methods in this class are considered as entry points
				for(MethodData testMethod : testMethods) {
					if(testMethod.getClassName().equals(className)) {
						executedTestMethods.add(testMethod);
					}
				}
			} else {
				for(MethodData testMethod : testMethods) {
					if(testMethod.getClassName().equals(className) 
							&& testMethod.getName().equals(methodName)) {
						executedTestMethods.add(testMethod);
					}
				}
			}
        }

		return executedTestMethods;
	}
	
	
	public static Set<String> getTestMethodsAsEntryPoints(File test_log) {
		Set<String> executedTests = new HashSet<String>();
		try {
			List<String> lines = FileUtils.readLines(test_log,
					Charset.defaultCharset());
			for (String line : lines) {
				if (line.contains("Running ")) {
					String testClass = line
							.substring(line.indexOf("Running ") + 8);
					if(testClass.matches("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_\\$]+)+$")) {
						// double check whether this is a fully qualified class name
						executedTests.add(testClass + ":*");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return executedTests;
	}

	public static Set<MethodData> getTestMethodsAsEntryPoints(Set<MethodData> methods){
		Set<MethodData> testMethods = new HashSet<MethodData>();

		for(MethodData methodData: methods){
			if(methodData.getAnnotation().isPresent()
				&& testAnnotations().contains(methodData.getAnnotation().get())){
				testMethods.add(methodData);
			}
		}

		return testMethods;
	}

	
	/**
	 * 
	 * This method gets a list of main methods from a set of given methods.
	 * 
	 * @param methods
	 * @return
	 */
	public static Set<MethodData> getMainMethodsAsEntryPoints(Set<MethodData> methods) {
		Set<MethodData> mainMethods = new HashSet<MethodData>();
		for(MethodData s : methods) {
			//TODO: Am I representing all possible implementations of main? What are the rules? Need to do some research
			if(s.isPublic() && s.isStatic() && s.getName().equals("main")){
				mainMethods.add(s);
			}
		}
		return mainMethods;
	}
	
	public static Set<MethodData> getPublicMethodsAsEntryPoints(Set<MethodData> methods) {
		Set<MethodData> publicMethods = new HashSet<MethodData>();

		for(MethodData method: methods){
			if(method.isPublic()){
				publicMethods.add(method);
			}
		}

		return publicMethods;
	}
	
	/**
	 * Convert java methods in the MethodData format to SootMethod objects. 
	 * 
	 * Make sure you set the class path and process directory of Soot before calling this method.
	 * 
	 * @param methods
	 * @return
	 */
	public static List<SootMethod> convertToSootMethod(Set<MethodData> methods) {
		// aggregate the methods by their containing classes first so that we do not have to load the same class repetitively in the next step
		HashMap<String, HashSet<MethodData>> methodByClass = new HashMap<String, HashSet<MethodData>>();
		for(MethodData md : methods) {
			String clsName = md.getClassName();
			HashSet<MethodData> set;
			if(methodByClass.containsKey(clsName)) {
				set = methodByClass.get(clsName);
			} else {
				set = new HashSet<MethodData>();
			}
			set.add(md);
			methodByClass.put(clsName, set);
		}
		List<SootMethod> entryPoints = new ArrayList<SootMethod>();
		for(String cls : methodByClass.keySet()) {
			SootClass entryClass = Scene.v().loadClassAndSupport(cls);
			Scene.v().loadNecessaryClasses();
			HashSet<MethodData> ms = methodByClass.get(cls);
			for(MethodData md : ms) {
				SootMethod entryMethod = entryClass.getMethod(md.getSubSignature());
				entryPoints.add(entryMethod);
//				System.out.println("Loading " + entryMethod);
			}
		}
		return entryPoints;
	}
}
