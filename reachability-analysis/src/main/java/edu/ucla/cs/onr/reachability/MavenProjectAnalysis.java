package edu.ucla.cs.onr.reachability;

import java.io.File;
import java.util.LinkedHashMap;

import edu.ucla.cs.onr.util.MavenLogUtils;

public class MavenProjectAnalysis {
	final static String maven_project_path = "/media/troy/Disk2/ONR/BigQuery/sample-projects";
	final static String csv_file_path = "analysis_result.csv";
	
	public static void main(String[] args) {
		File rootDir = new File(maven_project_path);
		for (File proj : rootDir.listFiles()) {
			String proj_name = proj.getName();
			if (!proj_name.contains("_"))
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
					LinkedHashMap<String, String> classpaths = MavenLogUtils.getClasspaths(
							proj.getAbsolutePath() + File.separator + "onr_classpath_new.log");
					
					// TODO: A maven project that have src and target directories in the root dir  
					// can also have submodules, e.g., apache_curator. We cannot simply decide 
					// whether a project has submodules just based on the directory structure.
					if(classpaths.size() == 1) {
						String cp = classpaths.values().iterator().next();
						System.out.println("Analyzing " + proj_name);
						System.out.println("Classpath: " + cp);
						
						// create the spark call graph
						String app_class_path = proj_path + File.separator + "target/classes";
						String app_test_path = proj_path + File.separator + "target/test-classes";
						String lib_class_path = cp;
						String test_log_path = proj_path + File.separator + "onr_test.log";
						
//						SparkCallGraphAnalysis runner = 
//								new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
//						runner.run();
					}
				}
			}
		}
	}
}
