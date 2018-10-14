package edu.ucla.cs.onr.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ucla.cs.onr.reachability.MethodData;
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
		Set<String> methods = new HashSet<String>();
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

	public static Set<MethodData> getTestMethodsAsEntryPoints(Set<MethodData> methods){
		Set<MethodData> testMethods = new HashSet<MethodData>();

		for(MethodData methodData: methods){
			if(methodData.getAnnotation().isPresent()
				&& methodData.getAnnotation().get().equals("org.junit.Test")){
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
	 * Convert java methods in the 'className:methodName' format to Soot methods. Class names must 
	 * be fully qualified. Method names can be * to represent any methods in a class.
	 * 
	 * Make sure you set the class path and process directory of Soot before calling this method.
	 * 
	 * @param methods
	 * @return
	 */
	public static List<SootMethod> convertToSootMethod(Set<MethodData> methods) {
		List<SootMethod> entryPoints = new ArrayList<SootMethod>();
		for(MethodData s : methods) {
			SootClass entryClass = Scene.v().loadClassAndSupport(s.getClassName());
			Scene.v().loadNecessaryClasses();
			SootMethod entryMethod = entryClass.getMethodByName(s.getName());
			entryPoints.add(entryMethod);
		}
		return entryPoints;
	}
}
