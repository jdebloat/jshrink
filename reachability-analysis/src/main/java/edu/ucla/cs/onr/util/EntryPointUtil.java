package edu.ucla.cs.onr.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class EntryPointUtil {
	
	/**
	 * This method gets a list of test methods from a test log file.
	 * Now we consider all methods in a test class as test methods, 
	 * represented in the 'className:*' format
	 * 
	 * @param testLog
	 * @return
	 */
	public static Set<String> getTestMethodsAsEntryPoints(File testLog) {
		HashSet<String> methods = new HashSet<String>();
		try {
			List<String> lines = FileUtils.readLines(testLog,
					Charset.defaultCharset());
			for (String line : lines) {
				if (line.contains("Running ")) {
					String testClass = line
							.substring(line.indexOf("Running ") + 8);
					methods.add(testClass + ":*");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return methods;
	}
	
	/**
	 * 
	 * This method gets a list of main methods from a set of given methods.
	 * 
	 * @param appMethods
	 * @return
	 */
	public static Set<String> getMainMethodsAsEntryPoints(Set<String> methods) {
		HashSet<String> mainMethods = new HashSet<String>();
		for(String s : methods) {
			String[] ss = s.split(": ");
			String className = ss[0];
			String methodName = ss[1];
			if(methodName.equals("void main(java.lang.String[])")) {
				mainMethods.add(className + ":main");
			}
		}
		return mainMethods;
	}
	
	public static Set<String> getPublicMethodsAsEntryPoints(Set<String> methods) {
		// TODO: implement later
		return null;
	}
	
	/**
	 * Convert java methods in the 'className:methodName' format to Soot methods. Class names must 
	 * be fully qualified. Method names can be * to represent any methods in a class.
	 * 
	 * Make sure you set the class path and process directory of Soot before calling this method.
	 * 
	 * @param methods
	 * @return
	 */
	public static List<SootMethod> convertToSootMethod(Set<String> methods) {
		List<SootMethod> entryPoints = new ArrayList<SootMethod>();
		for(String s : methods) {
			String[] ss = s.split(":");
			String className = ss[0];
			String methodName = ss[1];
			if(methodName.equals("*")) {
				// all methods are considered as entry points
				SootClass entryClass = Scene.v().loadClassAndSupport(className);
				Scene.v().loadNecessaryClasses();
				List<SootMethod> mList = entryClass.getMethods();
				for(SootMethod m : mList) {
					entryPoints.add(m);
				}
			} else {
				SootClass entryClass = Scene.v().loadClassAndSupport(className);
				Scene.v().loadNecessaryClasses();
				SootMethod entryMethod = entryClass.getMethodByName(methodName);
				entryPoints.add(entryMethod);
			}
		}
		return entryPoints;
	}
}
