package edu.ucla.cs.onr.util;

import java.io.File;
import java.util.*;

import edu.ucla.cs.onr.reachability.MethodData;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.options.Options;

public class SootUtils {

	public static String getJREJars() { 
		String defaultClasspath = Scene.v().defaultClassPath();
		String cp;
		if(defaultClasspath.contains(":")) {
			// The JRE in Mac returns multiple JRE jars including rt.jar and jce.jar
			cp = defaultClasspath;
		} else {
			// The JRE in Linux returns rt.jar only
			String jrePath = defaultClasspath.substring(0, defaultClasspath.lastIndexOf(File.separator));
			String jcePath = jrePath + File.separator + "jce.jar";
			// add rt.jar and jce.jar to the classpath, as required by Soot
			cp = defaultClasspath + File.pathSeparator + jcePath;
		}
		
		return cp;
	}

	private static String listToPathString(List<File> paths){
		StringBuilder sb = new StringBuilder();
		for(File path : paths){
			sb.append(File.pathSeparator + path.getAbsolutePath());
		}
		return sb.toString();
	}

	public static MethodData sootMethodToMethodData(SootMethod sootMethod){
		String methodName = sootMethod.getName();
		String methodClassName = sootMethod.getDeclaringClass().getName();
		String methodReturnType = sootMethod.getReturnType().toString();
		boolean isPublic = sootMethod.isPublic();
		boolean isStatic = sootMethod.isStatic();

		String[] methodArgs = new String[sootMethod.getParameterCount()];
		for(int i=0; i<sootMethod.getParameterTypes().size(); i++){
			Type type = sootMethod.getParameterTypes().get(i);
			methodArgs[i] = type.toString();
		}

		return new MethodData(methodName,methodClassName,methodReturnType,methodArgs,isPublic, isStatic);
	}

	public static void setup(List<File> libJarPath, List<File> appClassPath, List<File> appTestPath){
		String cp = SootUtils.getJREJars();
		cp += listToPathString(libJarPath);
		cp += listToPathString(appClassPath);
		cp += listToPathString(appTestPath);
		Options.v().set_soot_classpath(cp);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);

		// set the application directories
		List<String> dirs = new ArrayList<String>();

		for(File path : appClassPath) {
			dirs.add(path.getAbsolutePath());
		}

		for(File path : appTestPath) {
			dirs.add(path.getAbsolutePath());
		}

		for(File path : libJarPath){
			dirs.add(path.getAbsolutePath());
		}

		Options.v().set_process_dir(dirs);
	}

	/*
	public static HashMap<String, String> getSparkOpt() {
		HashMap<String, String> opt = new HashMap<String, String>();
		opt.put("enabled","true");
		opt.put("verbose","true");
		opt.put("ignore-types","false");          
		opt.put("force-gc","true");            
		opt.put("pre-jimplify","false");          
		opt.put("vta","false");                   
		opt.put("rta","false");                   
		opt.put("field-based","false");           
		opt.put("types-for-sites","false");        
		opt.put("merge-stringbuffer","true");   
		opt.put("string-constants","false");     
		opt.put("simulate-natives","true");      
		opt.put("simple-edges-bidirectional","false");
		opt.put("on-fly-cg","true");            
		opt.put("simplify-offline","false");    
		opt.put("simplify-sccs","true");        
		opt.put("ignore-types-for-sccs","false");
		opt.put("propagator","worklist");
		opt.put("set-impl","double");
		opt.put("double-set-old","hybrid");         
		opt.put("double-set-new","hybrid");
		opt.put("dump-html","false");           
		opt.put("dump-pag","false");             
		opt.put("dump-solution","false");        
		opt.put("topo-sort","false");           
		opt.put("dump-types","true");             
		opt.put("class-method-var","true");     
		opt.put("dump-answer","false");          
		opt.put("add-tags","false");             
		opt.put("set-mass","false"); 
		return opt;
	}
	
	/**
	 * 
	 * This recursive function does not work well when a call graph is very deep.
	 * Call visitMethodNonRecur instead.
	 * 
	 * @param m
	 * @param cg
	 * @param usedClass
	 * @param visited
	 */
	/*
	@Deprecated
	public static void visitMethod(SootMethod m, CallGraph cg, Set<String> usedClass, Set<String> visited) {
		String className = m.getDeclaringClass().toString();
		String signature = m.getSignature();
		// remove the brackets before and after the method signature
		signature = signature.substring(1, signature.length() - 1);
		if(!visited.contains(signature)) {
			// visited early and avoid recursion
			usedClass.add(className);
			visited.add(signature);
			
			Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(m));
			while (targets.hasNext()){
				SootMethod method = (SootMethod)targets.next();
				visitMethod(method, cg, usedClass, visited);
			}
		}
	}*/
	
	public static void visitMethodNonRecur(SootMethod m, CallGraph cg, Set<String> usedClass, Set<MethodData> visited) {
		Stack<SootMethod> methods_to_visit = new Stack<SootMethod>();
		methods_to_visit.add(m);
		while(!methods_to_visit.isEmpty()) {
			SootMethod first = methods_to_visit.pop();
			MethodData firstMethodData = sootMethodToMethodData(first);

			String className = firstMethodData.getClassName();

			if(!visited.contains(firstMethodData)) {
				// avoid recursion
				usedClass.add(firstMethodData.getClassName());
				visited.add(firstMethodData);
				
				// add callees to the stack
				Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(first));
				while (targets.hasNext()){
					SootMethod method = (SootMethod)targets.next();
					methods_to_visit.push(method);
				}
			}	
		}
	}
}
