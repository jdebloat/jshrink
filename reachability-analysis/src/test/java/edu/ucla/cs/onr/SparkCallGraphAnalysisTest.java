package edu.ucla.cs.onr;

import java.io.File;

import org.junit.Test;

public class SparkCallGraphAnalysisTest {
	private static String root_path = "/media/troy/Disk2/ONR/BigQuery/sample-projects"; 
	
	@Test
	public void testJUnit4() {
		String project_folder = "junit-team_junit4";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 2 library dependencies
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testApacheCommonsLang() {
		String project_folder = "apache_commons-lang";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 9 library dependencies
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testSquareJavapoet() {
		String project_folder = "square_javapoet";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 19 library dependencies
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testJJWT() {
		String project_folder = "jwtk_jjwt";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 30 library dependencies
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testDensityConverter() {
		String project_folder = "patrickfav_density-converter";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 49 library dependencies
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
				
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testAmazonEcho() {
		String project_folder = "armzilla_amazon-echo-ha-bridge";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 68 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testElasticSearch() {
		String project_folder = "NLPchina_elasticsearch";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 71 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testSpringRestServiceOauth() {
		String project_folder = "royclarkson_spring-rest-service-oauth";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 75 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testSolo() {
		String project_folder = "b3log_solo";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 78 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testLittleProxy() {
		String project_folder = "adamfisk_LittleProxy";
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 116 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testJeesite() {
		String project_folder = "thinkgem_jeesite";
		// this project has a custom output directory
		String app_class_path = 
				"/media/troy/Disk2/ONR/BigQuery/sample-projects/thinkgem_jeesite/src/main/webapp/WEB-INF/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 136 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testDddsampleCore() {
		String project_folder = "citerus_dddsample-core";
		// this project has a custom output directory
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 152 library dependencies, Spark fails with 10G max heap size
		// CHA passes it
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
	
	@Test
	public void testSparkTimeSeries() {
		String project_folder = "sryza_spark-timeseries";
		// this project has a custom output directory
		String app_class_path = root_path + File.separator + 
				project_folder + File.separator + "target/classes";
		String app_test_path = root_path + File.separator + 
				project_folder + File.separator + "target/test-classes";
		// 173 library dependencies, works when increasing the max heap size to 10G
		String cp_log = root_path + File.separator + 
				project_folder + File.separator + "onr_classpath_new.log";  
		String lib_class_path = MavenLogUtils.getClasspaths(cp_log).values().iterator().next();
		String test_log_path = root_path + File.separator + 
				project_folder + File.separator + "onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
	}
}
