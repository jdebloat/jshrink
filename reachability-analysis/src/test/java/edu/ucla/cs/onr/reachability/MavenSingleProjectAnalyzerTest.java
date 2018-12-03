package edu.ucla.cs.onr.reachability;

import static org.junit.Assert.*;

import edu.ucla.cs.onr.GitGetter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import soot.G;

import java.io.File;
import java.util.HashSet;

public class MavenSingleProjectAnalyzerTest {

	private static GitGetter gitGetter;

	@BeforeClass
	public static void setup(){
		gitGetter = new GitGetter();
	}

	@AfterClass
	public static void cleanup(){
		gitGetter.removeGitDir();
	}

	@Test
	public void testMavenProjectWithNoSubmodulesSparkOnly() {
		String junit_project = gitGetter.addGitHubProject("junit-team","junit",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit")).getAbsolutePath();

		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(junit_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(72, runner.getUsedLibClasses().size());
		assertEquals(350, runner.getUsedLibMethods().size());
		assertEquals(254, runner.getUsedAppClasses().size());
		assertEquals(1474, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithNoSubmodulesBothSparkAndTamiFlex() {
		MavenSingleProjectAnalyzer.useTamiFlex = true;

		String junit_project = gitGetter.addGitHubProject("junit-team","junit",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit")).getAbsolutePath();
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(junit_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(73, runner.getUsedLibClasses().size());
		assertEquals(359, runner.getUsedLibMethods().size());
		assertEquals(282, runner.getUsedAppClasses().size());
		assertEquals(1567, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithOneSubmoduleSparkOnly() {
		// the gson project has many submodules but only one submodule is actually built
		String gson_project = gitGetter.addGitHubProject("google","gson",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/google_gson")).getAbsolutePath();
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(gson_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(72, runner.getUsedLibClasses().size());
		assertEquals(219, runner.getUsedLibMethods().size());
		assertEquals(163, runner.getUsedAppClasses().size());
		assertEquals(930, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithOneSubmoduleBothSparkAndTamiFlex() {
		MavenSingleProjectAnalyzer.useTamiFlex = true;
		
		// the gson project has many submodules but only one submodule is actually built
		String gson_project = gitGetter.addGitHubProject("google","gson",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/google_gson")).getAbsolutePath();
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(gson_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(89, runner.getUsedLibClasses().size());
		assertEquals(306, runner.getUsedLibMethods().size());
		assertEquals(168, runner.getUsedAppClasses().size());
		assertEquals(940, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithMultiSubmodulesSparkOnly() {
		// the essentials project has multiple modules compiled but only one module has 
		// real Java class files, the other two only have resources
		String essentials_project = gitGetter.addGitHubProject("greenrobot","essentials",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/greenrobot_essentials")).getAbsolutePath();;
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(essentials_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(1168, runner.getUsedLibClasses().size());
		assertEquals(5684, runner.getUsedLibMethods().size());
		assertEquals(26, runner.getUsedAppClasses().size());
		assertEquals(195, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithMultiSubmodulesBothSparkAndTamiFlex() {
		MavenSingleProjectAnalyzer.useTamiFlex = true;
		
		// the essentials project has multiple modules compiled but only one module has 
		// real Java class files, the other two only have resources
		String essentials_project = gitGetter.addGitHubProject("greenrobot","essentials",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/greenrobot_essentials")).getAbsolutePath();;
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(essentials_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(1184, runner.getUsedLibClasses().size());
		assertEquals(5735, runner.getUsedLibMethods().size());
		assertEquals(26, runner.getUsedAppClasses().size());
		assertEquals(195, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithMultiSubmodules2SparkOnly() {
		// the cglib project has five modules
		// four of them have java class files and only two of them 
		// have test classes
		String cglib_project = gitGetter.addGitHubProject("cglib","cglib",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/cglib_cglib")).getAbsolutePath();
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(cglib_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(968, runner.getUsedLibClasses().size());
		assertEquals(4429, runner.getUsedLibMethods().size());
		assertEquals(157, runner.getUsedAppClasses().size());
		assertEquals(902, runner.getUsedAppMethods().size());
	}
	
	/**
	 * Note that injecting TamiFlex causes many test failures in this project.
	 */
	@Test
	public void testMavenProjectWithMultiSubmodules2SparkAndTamiFlex() {
		MavenSingleProjectAnalyzer.useTamiFlex = true;

		// the cglib project has five modules
		// four of them have java class files and only two of them 
		// have test classes
		String cglib_project = gitGetter.addGitHubProject("cglib","cglib",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/cglib_cglib")).getAbsolutePath();
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(cglib_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(970, runner.getUsedLibClasses().size());
		assertEquals(4523, runner.getUsedLibMethods().size());
		assertEquals(158, runner.getUsedAppClasses().size());
		assertEquals(903, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithMultiSubmodules3() {
		// the pf4j project has two submodules and one of them has two subsubmodules
		String pf4j_project = gitGetter.addGitHubProject("decebals","pf4j",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/decebals_pf4j")).getAbsolutePath();
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(pf4j_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
		assertEquals(1159, runner.getUsedLibClasses().size());
		assertEquals(3608, runner.getUsedLibMethods().size());
		assertEquals(64, runner.getUsedAppClasses().size());
		assertEquals(323, runner.getUsedAppMethods().size());
	}
	
	/**
	 * Must manually remove JCTools_JCTools/jctools-experimental/target/classes/
	 * org/jctools/queues/blocking/TemplateBlocking.java to resolve this NPE
	 * Tried to catch it outside of Soot but couldn't
	 */
	@Test
	public void testNPEInJCTools() {
		String jctools_project = gitGetter.addGitHubProject("JCTools","JCTools",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/JCTools_JCTools")).getAbsolutePath();
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(jctools_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
	}
	
	@Test
	public void testClassResolution() {
		CallGraphAnalysis.useSpark = false;
		String project = gitGetter.addGitHubProject("davidmoten","rxjava-extras",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/davidmoten_rxjava-extras")).getAbsolutePath();
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.setup();
		runner.run();
	}
	
	@After
	public void cleanUp() {
		G.reset();
	}
}
