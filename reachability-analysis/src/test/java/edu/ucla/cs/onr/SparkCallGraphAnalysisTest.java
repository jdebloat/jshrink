package edu.ucla.cs.onr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SparkCallGraphAnalysisTest {
	@Test
	public void testJUnit4() {
		String app_class_path = 
				"/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit4/target/classes";
		String app_test_path = 
				"/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit4/target/test-classes";
		String lib_class_path = "/media/troy/Disk2/ONR/maven/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar";
		String test_log_path = "/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit4/onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
		
		assertEquals(72, runner.usedLibClasses.size());
		assertEquals(356, runner.usedLibMethods.size());
		assertEquals(590, runner.usedAppClasses.size());
		assertEquals(2219, runner.usedAppMethods.size());
	}
	
	@Test
	public void testApacheCommonsLang() {
		String app_class_path = 
				"/media/troy/Disk2/ONR/BigQuery/sample-projects/apache_commons-lang/target/classes";
		String app_test_path = 
				"/media/troy/Disk2/ONR/BigQuery/sample-projects/apache_commons-lang/target/test-classes";
		String lib_class_path = "/media/troy/Disk2/ONR/maven/repository/junit/junit/4.12/junit-4.12.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/org/hamcrest/hamcrest-all/1.3/hamcrest-all-1.3.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/org/easymock/easymock/3.5.1/easymock-3.5.1.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/org/objenesis/objenesis/2.6/objenesis-2.6.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/org/openjdk/jmh/jmh-core/1.19/jmh-core-1.19.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/org/apache/commons/commons-math3/3.2/commons-math3-3.2.jar"
				+ ":/media/troy/Disk2/ONR/maven/repository/org/openjdk/jmh/jmh-generator-annprocess/1.19/jmh-generator-annprocess-1.19.jar";
		String test_log_path = "/media/troy/Disk2/ONR/BigQuery/sample-projects/apache_commons-lang/onr_test.log";
		
		SparkCallGraphAnalysis runner = 
				new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path, test_log_path);
		runner.run();
		
//		assertEquals(72, runner.usedLibClasses.size());
//		assertEquals(356, runner.usedLibMethods.size());
//		assertEquals(590, runner.usedAppClasses.size());
//		assertEquals(2219, runner.usedAppMethods.size());
	}
}
