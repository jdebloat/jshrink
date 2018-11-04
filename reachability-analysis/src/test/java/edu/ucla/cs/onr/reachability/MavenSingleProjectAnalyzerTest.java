package edu.ucla.cs.onr.reachability;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

import soot.G;

import java.util.HashSet;

public class MavenSingleProjectAnalyzerTest {
	@Test
	public void testMavenProjectWithNoSubmodules() {
		String junit_project = "/media/troy/Disk2/ONR/BigQuery/sample-projects/junit-team_junit";
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(junit_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.run();
		assertEquals(72, runner.getUsedLibClasses().size());
		assertEquals(350, runner.getUsedLibMethods().size());
		assertEquals(254, runner.getUsedAppClasses().size());
		assertEquals(1474, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithOneSubmodule() {
		// the gson project has many submodules but only one submodule is actually built
		String gson_project = "/media/troy/Disk2/ONR/BigQuery/sample-projects/google_gson";
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(gson_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.run();
		assertEquals(72, runner.getUsedLibClasses().size());
		assertEquals(219, runner.getUsedLibMethods().size());
		assertEquals(163, runner.getUsedAppClasses().size());
		assertEquals(930, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithMultiSubmodules() {
		// the essentials project has multiple modules compiled but only one module has 
		// real Java class files, the other two only have resources
		String essentials_project = "/media/troy/Disk2/ONR/BigQuery/sample-projects/greenrobot_essentials";
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(essentials_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.run();
		assertEquals(1168, runner.getUsedLibClasses().size());
		assertEquals(5684, runner.getUsedLibMethods().size());
		assertEquals(26, runner.getUsedAppClasses().size());
		assertEquals(195, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithMultiSubmodules2() {
		// the cglib project has five modules
		// four of them have java class files and only two of them 
		// have test classes
		String cglib_project = "/media/troy/Disk2/ONR/BigQuery/sample-projects/cglib_cglib";
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(cglib_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.run();
		assertEquals(968, runner.getUsedLibClasses().size());
		assertEquals(4429, runner.getUsedLibMethods().size());
		assertEquals(157, runner.getUsedAppClasses().size());
		assertEquals(902, runner.getUsedAppMethods().size());
	}
	
	@Test
	public void testMavenProjectWithMultiSubmodules3() {
		// the pf4j project has two submodules and one of them has two subsubmodules
		String pf4j_project = "/media/troy/Disk2/ONR/BigQuery/sample-projects/decebals_pf4j";
		MavenSingleProjectAnalyzer runner = new MavenSingleProjectAnalyzer(pf4j_project, new EntryPointProcessor(true, false, true, new HashSet<MethodData>()));
		runner.run();
		assertEquals(1159, runner.getUsedLibClasses().size());
		assertEquals(3608, runner.getUsedLibMethods().size());
		assertEquals(64, runner.getUsedAppClasses().size());
		assertEquals(323, runner.getUsedAppMethods().size());
	}
	
	@After
	public void cleanUp() {
		G.reset();
	}
}
