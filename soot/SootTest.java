import java.util.*;
import java.io.*;

import soot.*;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.*;
import soot.options.*;

public class SootTest {
	public static void main(String [] args) {
		String inputJarPath = "/Users/zonghengma/Documents/UCLA/capstone/Jama-1.0.3.jar";
		String outputJarPath = "/Users/zonghengma/Documents/UCLA/capstone/Jama-1.0.3-shortened.jar";
		String inputPath = "/Users/zonghengma/Documents/UCLA/capstone/Jama-1.0.3";
		String outputPath = inputPath + "-tmp";
		String entryJarPath = "/Users/zonghengma/Documents/UCLA/capstone/Jama_sample/JamaSample.jar";
		String entryClassName = "Sample";
		String entryMethodName = "main";
		
		Options.v().set_soot_classpath(Scene.v().defaultClassPath());
		if (!entryJarPath.equals("")) {
			Options.v().set_soot_classpath(Options.v().soot_classpath() + ":" + entryJarPath);
		}
		if (!inputJarPath.equals("")) {
			Options.v().set_soot_classpath(Options.v().soot_classpath() + ":" + inputJarPath);
		}
		
//		System.out.println(Options.v().classPa);
		
		HashMap<String, String> allClasses = new HashMap<String, String>();
		FilePathProcessor.process(allClasses, inputPath, "", "");
		
//		for (String s : allClasses) {
//			System.out.println(s);
//		}

		Options.v().set_whole_program(true);
		SootClass entryClass = Scene.v().loadClassAndSupport(entryClassName);
		Scene.v().loadNecessaryClasses();
		ArrayList<SootMethod> entryPoints = new ArrayList<SootMethod>();
		SootMethod entryMethod = entryClass.getMethodByName(entryMethodName);
		entryPoints.add(entryMethod);
	    Scene.v().setEntryPoints(entryPoints);
	    
//		CHATransformer.v().transform();
		HashMap<String, String> opt = getSparkOpt();
		SparkTransformer.v().transform("",opt);

		CallGraph cg = Scene.v().getCallGraph();
		System.out.println("graph done");

		HashSet<String> visited = new HashSet<String>();		
		HashSet<String> usedClass = new HashSet<String>();

		visitMethod(entryMethod, cg, usedClass, visited);
	
		System.out.println("results");
		
		HashSet<String> usedInJar = new HashSet<String>();
		for (String s : usedClass) {
			if (allClasses.containsKey(s)) {
				usedInJar.add(s);
				System.out.println(s);
			}
		}
		
		System.out.println("results end");
		
		for (String s : usedInJar) {
			String relatePath = allClasses.get(s);
			try {
				File origin = new File(inputPath + File.separator + relatePath);
				File desti = new File(outputPath + File.separator + relatePath);
				if (!desti.getParentFile().exists()) {
					desti.getParentFile().mkdirs();
				}
				copyFile(origin, desti);
			} catch(IOException e) {
				System.out.println(e);
				System.exit(1);
			}
		}
		String cmd[] = {"/bin/sh", "-c", "cd " + outputPath + "; jar cf " + outputJarPath +" ." };
		String cmd1[] = {"/bin/sh", "-c", "cd / ; rm -r " + outputPath};
		
		try {
			Runtime.getRuntime().exec(cmd);
//			Runtime.getRuntime().exec(cmd1);
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("success!");
		
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
				visitMethod(method, cg, usedClass, visited);
			}
		}
	}
	
	private static void copyFile(File source, File dest) throws IOException {
	    InputStream is = null;
	    OutputStream os = null;
	    try {
	        is = new FileInputStream(source);
	        os = new FileOutputStream(dest);
	        byte[] buffer = new byte[1024];
	        int length;
	        while ((length = is.read(buffer)) > 0) {
	            os.write(buffer, 0, length);
	        }
	    } finally {
	        is.close();
	        os.close();
	    }
	}
}
