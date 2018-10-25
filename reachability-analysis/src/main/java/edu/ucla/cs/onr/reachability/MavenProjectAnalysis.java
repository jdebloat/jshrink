package edu.ucla.cs.onr.reachability;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import edu.ucla.cs.onr.util.ASMUtils;
import edu.ucla.cs.onr.util.EntryPointUtil;
import edu.ucla.cs.onr.util.MavenUtils;

public class MavenProjectAnalysis {
	final static String maven_project_path = "/media/troy/Disk2/ONR/BigQuery/sample-projects";
	final static String csv_file_path = "analysis_result(cha).tsv";
	
	public static void main(String[] args) throws IOException {
		File output = new File(csv_file_path);
		
		// check for analyzed projects	
		Set<String> analyzed_project = new HashSet<String>();
		if(output.exists()) {
			List<String> lines = FileUtils.readLines(output, Charset.defaultCharset());
			for(String line : lines) {
				String[] ss = line.split("\t");
				if(ss.length > 1) {
					analyzed_project.add(ss[0]);
				}
			}
		}
		
		File rootDir = new File(maven_project_path);
		for (File proj : rootDir.listFiles()) {
			String proj_name = proj.getName();
			if (!proj_name.contains("_") || analyzed_project.contains(proj_name))
				continue;
			String usr_name = proj_name.substring(0, proj_name.indexOf('_'));
			String repo_name = proj_name.substring(proj_name.indexOf('_') + 1);
			String repo_link = "https://github.com/" + usr_name + "/"
					+ repo_name;

			File build_log = new File(proj.getAbsolutePath() + File.separator
					+ "onr_build.log");
			File test_log = new File(proj.getAbsolutePath() + File.separator
					+ "onr_test.log");
			if(build_log.exists() && test_log.exists()) {
				// build success
				String proj_path = proj.getAbsolutePath();
				File srcDir = new File(proj_path + File.separator + "src");
				File targetDir = new File(proj_path + File.separator + "target");
				if(srcDir.exists() && targetDir.exists()) {
					// a regular maven project with a single module
					HashMap<String, String> classpaths = MavenUtils.getClasspaths(
							proj.getAbsolutePath() + File.separator + "onr_classpath_new.log");
					
					// traverse from the root directory to identify all POM files
					// each submodule contains one pom file
					
					// TODO: A maven project that have src and target directories in the root dir  
					// can also have submodules, e.g., apache_curator. We cannot simply decide 
					// whether a project has submodules just based on the directory structure.
					if(classpaths.size() == 1) {
						System.out.println("Analyzing " + proj_name);
						String cp = classpaths.values().iterator().next();
						String[] paths = cp.split(File.pathSeparator);
						if(paths.length > 116) {
							// analyze small maven projects first
							continue;
						}
//						System.out.println("Classpath: " + cp);
						
						// create the spark call graph
						File app_class_path = new File(proj_path + File.separator + "target/classes");
						File app_test_path;
						if (proj_name.equals("google_auto") || proj_name.equals("google_flatbuffers")) {
							continue;
						} else if (proj_name.equals("thymeleaf_thymeleaf")) {
							app_test_path = new File(proj_path + File.separator + "thymeleaf-tests/target/test-classes");
						} else {
							app_test_path = new File(proj_path + File.separator + "target/test-classes");
						}
						File test_log_path = new File(proj_path + File.separator + "onr_test.log");
						
						// initialize the arguments for SparkCallGraphAnalysis
						List<File> app_class_paths = new ArrayList<File>();
						app_class_paths.add(app_class_path);
						List<File> app_test_paths = new ArrayList<File>();
						app_test_paths.add(app_test_path);
						List<File> lib_class_paths = new ArrayList<File>();
						for(String path: paths){
							lib_class_paths.add(new File(path));
						}
						Set<MethodData> entryPoints = new HashSet<MethodData>();
						// get main methods
						HashSet<String> appClasses = new HashSet<String>();
						HashSet<MethodData> appMethods = new HashSet<MethodData>();
						ASMUtils.readClassFromDirectory(app_class_path, appClasses, appMethods);
						Set<MethodData> mainMethods = EntryPointUtil.getMainMethodsAsEntryPoints(appMethods);
						entryPoints.addAll(mainMethods);
						
						// get test methods
				        Set<MethodData> testMethods = 
				        		EntryPointUtil.getTestMethodsAsEntryPoints(test_log_path, app_test_path);
				        entryPoints.addAll(testMethods);
						
						SparkCallGraphAnalysis runner = 
								new SparkCallGraphAnalysis(lib_class_paths, app_class_paths, app_test_paths, entryPoints);
						runner.run();
						
						String record = proj_name + "\t"
								+ repo_link + "\t"
								+ paths.length + "\t"
								+ runner.getLibClasses().size() + "\t"
								+ runner.getUsedLibClasses().size() + "\t"
								+ runner.getLibMethods().size() + "\t"
								+ runner.getUsedLibMethods().size() + "\t"
								+ runner.getAppClasses().size() + "\t"
								+ runner.getUsedAppClasses().size() + "\t"
								+ runner.getAppMethods().size() + "\t"
								+ runner.getUsedAppMethods().size() + "\t"
								+ System.lineSeparator();
						
						try {
							FileUtils.writeStringToFile(output, record, Charset.defaultCharset(), true);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						// I have to terminate here and call this program repetitively outside of JVM
						// This is because Soot always throws a method-not-found exception when continuing
						// to analyze the next project
						// Not sure how to reset Soot
						System.exit(0);
					}
				}
			}
		}
	}
}
