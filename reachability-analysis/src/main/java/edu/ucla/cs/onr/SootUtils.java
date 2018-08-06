package edu.ucla.cs.onr;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

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
	
	public static HashMap<String, String> getSparkOpt() {
		HashMap<String, String> opt = new HashMap<String, String>();
		opt.put("enabled","true");
		opt.put("verbose","true");
		opt.put("ignore-types","false");          
		opt.put("force-gc","false");            
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
		opt.put("simplify-sccs","false");        
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
	
	public static void visitMethod(SootMethod m, CallGraph cg, HashSet<String> usedClass, HashSet<String> visited) {
		Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(m));
		while (targets.hasNext()){
			SootMethod method = (SootMethod)targets.next();
			usedClass.add(method.getDeclaringClass().toString());
			if (!visited.contains(method.toString())) {
				visited.add(method.toString());
				// depth first
				visitMethod(method, cg, usedClass, visited);
			}
		}
	}
}
