package edu.ucla.cs.onr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SparkCallGraphAnalysisTest {
	@Test
	public void testJUnit4() {
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(new File(
			"/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit4/target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(new File(
			"/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit4/target/test-classes"));

		List<File> lib_class_path = new ArrayList<File>();
		lib_class_path.add(new File(
			"/media/troy/Disk2/ONR/maven/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"));

		File test_log_path = new File(
			"/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit4/onr_test.log");
		
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
		List<File> app_class_path = new ArrayList<File>();
		app_class_path.add(
			new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/apache_commons-lang/target/classes"));

		List<File> app_test_path = new ArrayList<File>();
		app_test_path.add(
			new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/apache_commons-lang/target/test-classes"));

		List<File> lib_class_path = new ArrayList<File>();
		lib_class_path.add(new File(
			"/media/troy/Disk2/ONR/maven/repository/junit/junit/4.12/junit-4.12.jar"));
		lib_class_path.add(
			new File("/media/troy/Disk2/ONR/maven/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"));
		lib_class_path.add(
			new File("/media/troy/Disk2/ONR/maven/repository/org/hamcrest/hamcrest-all/1.3/hamcrest-all-1.3.jar"));
		lib_class_path.add(
			new File("/media/troy/Disk2/ONR/maven/repository/org/easymock/easymock/3.5.1/easymock-3.5.1.jar"));
		lib_class_path.add(
			new File("/media/troy/Disk2/ONR/maven/repository/org/objenesis/objenesis/2.6/objenesis-2.6.jar"));
		lib_class_path.add(
			new File("/media/troy/Disk2/ONR/maven/repository/org/openjdk/jmh/jmh-core/1.19/jmh-core-1.19.jar"));
		lib_class_path.add(
			new File("/media/troy/Disk2/ONR/maven/repository/net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar"));
		lib_class_path.add(
			new File("/media/troy/Disk2/ONR/maven/repository/org/apache/commons/commons-math3/3.2/commons-math3-3.2.jar"));
		lib_class_path.add(
			new File("/media/troy/Disk2/ONR/maven/repository/org/openjdk/jmh/jmh-generator-annprocess/1.19/jmh-generator-annprocess-1.19.jar"));


		File test_log_path = new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/apache_commons-lang/onr_test.log");
		
		SparkCallGraphAnalysis runner =
			new SparkCallGraphAnalysis(lib_class_path, app_class_path, app_test_path,
				SparkCallGraphAnalysis.getEntryPointsFromTestLog(
					lib_class_path,app_class_path,app_test_path,test_log_path));
		
//		assertEquals(72, runner.getUsedLibClasses().size());
//		assertEquals(356, runner.getUsedLibMethods().size());
//		assertEquals(590, runner.getUsedAppClasses().size());
//		assertEquals(2219, runner.getUsedAppMethods().size());
	}
}
