package edu.ucla.cs.onr.util;

import java.io.File;
import java.util.*;

import edu.ucla.cs.onr.reachability.MethodData;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.options.Options;
import soot.toolkits.scalar.Pair;

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
			if(path.exists()) {
				sb.append(File.pathSeparator + path.getAbsolutePath());
			} else {
				System.err.println(path.getAbsolutePath() + " does not exist.");
			}
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

	public static Map<SootMethod, Set<SootMethod>> convertMethodDataCallGraphToSootMethodCallGraph(
			Map<MethodData, Set<MethodData>> map){
		Map<SootMethod, Set<SootMethod>> toReturn = new HashMap<SootMethod, Set<SootMethod>>();
		for(Map.Entry<MethodData, Set<MethodData>> entry : map.entrySet()){
			SootClass keySootClass = Scene.v().getSootClass(entry.getKey().getClassName());

			/*
			For some reason, I occasionally got a "java.lang.RuntimeException: not declared <method> in class <class>"
			error when runnin the "ApplicationTest:junit_test()" JUnit Test. I therefore added these "declaresMethod"
			checks... doesn't get to the root of the problem but I had to move forward.
			TODO: Investigate this bug.
			 */
			if(keySootClass.declaresMethod(entry.getKey().getSubSignature())) {
				SootMethod keySootMethod = keySootClass.getMethod(entry.getKey().getSubSignature());
				Set<SootMethod> value = new HashSet<SootMethod>();
				for (MethodData methodData : entry.getValue()) {
					SootClass valueSootClass = Scene.v().getSootClass(methodData.getClassName());
					if (valueSootClass.declaresMethod(methodData.getSubSignature())) {
						SootMethod valueSootMethod = valueSootClass.getMethod(methodData.getSubSignature());
						value.add(valueSootMethod);
					}
				}
				toReturn.put(keySootMethod, value);
			}
		}
		return toReturn;
	}

	public static Map<MethodData, Set<MethodData>> convertSootMethodCallGraphToMethodDataCallGraph(
			Map<SootMethod, Set<SootMethod>> map){
		Map<MethodData, Set<MethodData>> toReturn = new HashMap<MethodData, Set<MethodData>>();
		for(Map.Entry<SootMethod, Set<SootMethod>> entry : map.entrySet()){
			Set<MethodData> value = new HashSet<MethodData>();
			for(SootMethod sootMethod : entry.getValue()){
				value.add(sootMethodToMethodData(sootMethod));
			}
			toReturn.put(sootMethodToMethodData(entry.getKey()),value);
		}
		return toReturn;
	}

	public static Map<SootMethod, Set<SootMethod>> mergeCallGraphMaps(
			Map<SootMethod, Set<SootMethod>> map1, Map<SootMethod, Set<SootMethod>> map2){
		Map<SootMethod, Set<SootMethod>> toReturn = new HashMap<SootMethod, Set<SootMethod>>(map1);
		for(Map.Entry<SootMethod, Set<SootMethod>> entry : map2.entrySet()){
			if(!toReturn.containsKey(entry.getKey())){
				toReturn.put(entry.getKey(), new HashSet<SootMethod>());
			}
			toReturn.get(entry.getKey()).addAll(entry.getValue());
		}
		return toReturn;
	}

	public static void setup_trimming(List<File> libJarPath, List<File> appClassPath, List<File> appTestPath){
		String cp = SootUtils.getJREJars();
		cp += listToPathString(libJarPath);
		cp += listToPathString(appClassPath);
		cp += listToPathString(appTestPath);
		Options.v().set_soot_classpath(cp);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		// try the following two options to ignore the static field error in the Jython lib 
		// the first one does not work but the second one works (why?)
		// check the following links for reference:
		// https://github.com/petablox-project/petablox/issues/6
		// https://github.com/Sable/soot/issues/410
		// https://github.com/Sable/soot/issues/717 
//		Options.v().setPhaseOption("jb.tr", "ignore-wrong-staticness:true");
		Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);

		// set the application directories
		List<String> dirs = new ArrayList<String>();

		for(File path : appClassPath) {
			// double check whether the file path exists
			if(path.exists()) {
				dirs.add(path.getAbsolutePath());
			} else {
				System.err.println(path.getAbsolutePath() + " does not exist.");
			}
			
		}

		for(File path : appTestPath) {
			if(path.exists()) {
				dirs.add(path.getAbsolutePath());
			} else {
				System.err.println(path.getAbsolutePath() + " does not exist.");
			}
		}

		for(File path : libJarPath){
			dirs.add(path.getAbsolutePath());
		}

		Options.v().set_process_dir(dirs);
	}

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
	@Deprecated
	public static void visitMethod(SootMethod m, CallGraph cg, Set<String> usedClass, Set<String> visited) {
		String className = m.getDeclaringClass().toString();
		String signature = m.getSubSignature();
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
	}

	public static void visitMethodClassCollapser(SootMethod m, CallGraph cg, Set<String> usedClass, Set<MethodData> usedMethods) {
		//a queue of Pair objects of SootMethods, the second object is the callsite of the first method
		Set<String> visited = new HashSet<String>();
		Stack<Pair<SootMethod, SootMethod>> methods_to_visit = new Stack<Pair<SootMethod, SootMethod>>();
		methods_to_visit.add(new Pair<SootMethod, SootMethod>(m, null));

		while(!methods_to_visit.isEmpty()) {
			Pair<SootMethod, SootMethod> first = methods_to_visit.pop();
			SootMethod firstMethod = first.getO1();
			MethodData firstMethodData = sootMethodToMethodData(firstMethod);
			MethodData callSiteData = (first.getO2() == null) ? null : sootMethodToMethodData(first.getO2());

			String className = firstMethodData.getClassName();
			if(!visited.contains(firstMethod.getSignature())) {
				// avoid recursion
				if (callSiteData == null || !callSiteData.getName().equals("<init>")) {
					usedClass.add(className);
					usedMethods.add(firstMethodData);
				}
				visited.add(firstMethod.getSignature());

				// add callees to the stack
				Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(firstMethod));
				while (targets.hasNext()){
					SootMethod method = (SootMethod)targets.next();
					methods_to_visit.push(new Pair<SootMethod, SootMethod>(method, firstMethod));
				}
			}
		}
	}

	public static void visitMethodNonRecur(SootMethod parent, CallGraph cg, Set<String> usedClass, Map<MethodData,
			Set<MethodData>> visited){
		Queue<SootMethod> stack = new LinkedList<SootMethod>();
		stack.add(parent);

		while(!stack.isEmpty()) {
			SootMethod par = stack.poll();
			MethodData parentMethodData = sootMethodToMethodData(par);
			usedClass.add(parentMethodData.getClassName());

			Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(par));
			while (targets.hasNext()) {
				SootMethod method = (SootMethod) targets.next();
				MethodData methodMethodData = sootMethodToMethodData(method);
				if (!visited.containsKey(methodMethodData)) {
					visited.put(methodMethodData, new HashSet<MethodData>());
				} else if (visited.get(methodMethodData).contains(parentMethodData)) {
					continue;
				}

				visited.get(methodMethodData).add(parentMethodData);
				stack.add(method);
			//	visitMethodNonRecur(method, cg, usedClass, visited);
			}
		}
	}
}
