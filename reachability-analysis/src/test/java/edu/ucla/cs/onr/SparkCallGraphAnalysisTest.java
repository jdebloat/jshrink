package edu.ucla.cs.onr;

import java.io.File;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SparkCallGraphAnalysisTest {
	private static String root_path = "/media/troy/Disk2/ONR/BigQuery/sample-projects";
	
	@Test
	public void testJUnit4() {
        String project_folder = "junit-team_junit4";

		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator
			+ project_folder + File.separator + "/target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator
			+ project_folder + File.separator + "/target/test-classes"));

        String cp_log = root_path + File.separator + project_folder + File.separator + "onr_classpath_new.log";
        
		List<File> lib_class_path = new ArrayList<File>();
        String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);

		for(String path: paths){
			lib_class_path.add(new File(path));
		}

        File test_log_path = new File(root_path + File.separator +
                                      project_folder + File.separator + "onr_test.log");
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
					SparkCallGraphAnalysis.getEntryPointsFromTestLog(
						lib_class_path,app_class_path,app_test_path,test_log_path));

		assertEquals(72, runner.getUsedLibClasses().size());
		assertEquals(356, runner.getUsedLibMethods().size());
		assertEquals(590, runner.getUsedAppClasses().size());
		assertEquals(2219, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testApacheCommonsLang() {
		String project_folder = "apache_commons-lang";

		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
				project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
				project_folder + File.separator + "target/test-classes"));

		// 9 library dependencies
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
				project_folder + File.separator + "onr_test.log");
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
					SparkCallGraphAnalysis.getEntryPointsFromTestLog(
						lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testSquareJavapoet() {
		String project_folder = "square_javapoet";
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//19 library dependencies
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testJJWT() {
		String project_folder = "jwtk_jjwt";
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//30 library dependencies
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testDensityConverter() {
		String project_folder = "patrickfav_density-converter";
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//40 library dependencies
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testAmazonEcho() {
		String project_folder = "armzilla_amazon-echo-ha-bridge";
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//68 library dependencies
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testElasticSearch() {
		String project_folder = "NLPchina_elasticsearch";
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//71 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testSpringRestServiceOauth() {
		String project_folder = "royclarkson_spring-rest-service-oauth";
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//75 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testSolo() {
		String project_folder = "b3log_solo";
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//78 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testLittleProxy() {
		String project_folder = "adamfisk_LittleProxy";
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//116 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testJeesite() {
		String project_folder = "thinkgem_jeesite";
		// this project has a custom output directory

		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(
			"/media/troy/Disk2/ONR/BigQuery/sample-projects/thinkgem_jeesite/src/main/webapp/WEB-INF/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
	
	@Test
	public void testDddsampleCore() {
		String project_folder = "citerus_dddsample-core";
		// this project has a custom output directory
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(root_path + File.separator +
			project_folder + File.separator + "target/test-classes"));

		//152 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator +
			project_folder + File.separator + "onr_classpath_new.log";

		List<File> lib_class_path = new ArrayList<File>();


		String[] paths = MavenLogUtils.getClasspaths(cp_log).values().iterator().next().split(File.pathSeparator);
		for(String path : paths){
			lib_class_path.add(new File(path));
		}

		File test_log_path = new File( root_path + File.separator +
			project_folder + File.separator + "onr_test.log");

		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path, app_class_path, app_test_path, test_log_path));
	}
}
