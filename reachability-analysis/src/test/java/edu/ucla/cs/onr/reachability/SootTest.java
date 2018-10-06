package edu.ucla.cs.onr.reachability;

import java.util.*;
import java.io.*;

import edu.ucla.cs.onr.util.FilePathProcessor;
import edu.ucla.cs.onr.util.SootUtils;
import soot.*;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.*;
import soot.options.*;

public class SootTest {
	public static void main(String [] args) {
		String inputJarPath = "src/test/resources/Jama-1.0.3.jar";
		String outputJarPath = "src/test/resources/Jama-1.0.3-shortened.jar";
		String inputPath = "src/test/resources/Jama-1.0.3";
		String outputPath = inputPath + "-tmp";
		String entryJarPath = "src/test/resources/jama-client.jar";
		String entryClassName = "Sample";
		String entryMethodName = "main";
		
		String cp = SootUtils.getJREJars();
		
		// add the application jars and the library jars into the classpath
		cp += File.pathSeparator + inputJarPath;
		cp += File.pathSeparator + entryJarPath;
		Options.v().set_soot_classpath(cp);
				
		HashMap<String, String> allClasses = new HashMap<String, String>();
		FilePathProcessor.process(allClasses, inputPath, "", "");
		
//		for (String s : allClasses) {
//			System.out.println(s);
//		}

		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		List<String> libs = new ArrayList<String>();
		// add library dependency to the process-dir (application classes) may cause 
		// memory explosion, so not recommended
//		libs.add(inputJarPath);
		libs.add(entryJarPath);
		Options.v().set_process_dir(libs);
		// temporary workaround---forcing the missing class to be loaded
//		Scene.v().loadClass("Jama.util.Maths",SootClass.BODIES);
//		SootClass jamaUtilClass = Scene.v().getSootClass("Jama.util.Maths");
//		assert jamaUtilClass.isPhantom() == false;
		SootClass entryClass = Scene.v().loadClassAndSupport(entryClassName);
		Scene.v().loadNecessaryClasses();
		ArrayList<SootMethod> entryPoints = new ArrayList<SootMethod>();
		SootMethod entryMethod = entryClass.getMethodByName(entryMethodName);
		entryPoints.add(entryMethod);
	    Scene.v().setEntryPoints(entryPoints);
	    
//		CHATransformer.v().transform();
		HashMap<String, String> opt = SootUtils.getSparkOpt();
		SparkTransformer.v().transform("",opt);

		CallGraph cg = Scene.v().getCallGraph();
		System.out.println("graph done");

		HashSet<String> visited = new HashSet<String>();		
		HashSet<String> usedClass = new HashSet<String>();

		SootUtils.visitMethod(entryMethod, cg, usedClass, visited);
	
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
