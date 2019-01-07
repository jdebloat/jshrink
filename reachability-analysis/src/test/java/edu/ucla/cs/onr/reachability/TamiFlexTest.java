package edu.ucla.cs.onr.reachability;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;

import edu.ucla.cs.onr.GitGetter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;

public class TamiFlexTest {
	/**
	 * Test the java agent injection on a pom file that (1) explicitly declares surefire plugin,
	 * and (2) has the configuration node but no the argLine node
	 * 
	 * @throws IOException
	 */

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
	public void testJavaAgentInjection1() throws IOException {
		String pom_file = "src/test/resources/tamiflex/junit_pom.xml";
		
		// save a copy of the pom file
		File file = new File(pom_file);
		File copy = new File(file.getAbsolutePath() + ".tmp");
		FileUtils.copyFile(file, copy);
		
		// inject tamiflex as a java agent in the surefire test plugin
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, null, false);
		tamiflex.injectTamiFlex(pom_file);
		String content = FileUtils.readFileToString(new File(pom_file), Charset.defaultCharset());
		assertTrue(content.contains("<argLine>-javaagent:" + tamiflex_jar_path + "</argLine>"));
		
		// make sure we do not inject the java agent repetitively
		tamiflex.injectTamiFlex(pom_file);
		content = FileUtils.readFileToString(new File(pom_file), Charset.defaultCharset());
		int count = StringUtils.countMatches(content, "<argLine>-javaagent:" + tamiflex_jar_path + "</argLine>");
		assertEquals(1, count);
		
		// restore the pom file
		FileUtils.copyFile(copy, file);
		copy.delete();
	}
	
	/**
	 * Test the java agent injection on a pom file that (1) explicitly declares surefire plugin,
	 * (2) has no configuration node and of course also no the argLine node
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJavaAgentInjection2() throws IOException {
		String pom_file = "src/test/resources/tamiflex/apache_lang_pom.xml";
		
		// save a copy of the pom file
		File file = new File(pom_file);
		File copy = new File(file.getAbsolutePath() + ".tmp");
		FileUtils.copyFile(file, copy);
		
		// inject tamiflex as a java agent in the surefire test plugin
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, null, false);
		tamiflex.injectTamiFlex(pom_file);
		String content = FileUtils.readFileToString(new File(pom_file), Charset.defaultCharset());
		assertTrue(content.contains("<configuration>"
				+ "<argLine>-javaagent:src/test/resources/tamiflex/poa-2.0.3.jar</argLine>"
				+ "</configuration>"));
				
		// restore the pom file
		FileUtils.copyFile(copy, file);
		copy.delete();
	}
	
	/**
	 * 
	 * Test the java agent injection on a pom file that (1) explicitly declares surefire plugin,
	 * (2) has the configuration node with an argLine node
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJavaAgentInjection3() throws IOException {
		String pom_file = "src/test/resources/tamiflex/hankcs_HanLP_pom.xml";
		
		// save a copy of the pom file
		File file = new File(pom_file);
		File copy = new File(file.getAbsolutePath() + ".tmp");
		FileUtils.copyFile(file, copy);
		
		// inject tamiflex as a java agent in the surefire test plugin
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, null, false);
		tamiflex.injectTamiFlex(pom_file);
		String content = FileUtils.readFileToString(new File(pom_file), Charset.defaultCharset());
		assertTrue(content.contains("<argLine>-Dfile.encoding=UTF-8 -javaagent:" + tamiflex_jar_path + "</argLine>"));
		
		// restore the pom file
		FileUtils.copyFile(copy, file);
		copy.delete();
	}
	
	/**
	 * 
	 * Test the java agent injection on a pom file that (1) does not declare surefire plugin,
	 * (2) and therefore also has no configuration node or argLine node
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJavaAgentInjection4() throws IOException {
		String pom_file = "src/test/resources/tamiflex/amaembo_streamex_pom.xml";
		
		// save a copy of the pom file
		File file = new File(pom_file);
		File copy = new File(file.getAbsolutePath() + ".tmp");
		FileUtils.copyFile(file, copy);
		
		// inject tamiflex as a java agent in the surefire test plugin
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, null, false);
		tamiflex.injectTamiFlex(pom_file);
		String content = FileUtils.readFileToString(new File(pom_file), Charset.defaultCharset());
		assertTrue(content.contains("<plugin>"
				+ "<groupId>org.apache.maven.plugins</groupId>"
				+ "<artifactId>maven-surefire-plugin</artifactId>"
				+ "<version>2.20.1</version>"
				+ "<configuration><argLine>-javaagent:" + tamiflex_jar_path + "</argLine></configuration>"
				+ "</plugin>"));
		
		// restore the pom file
		FileUtils.copyFile(copy, file);
		copy.delete();
	}
	
	/**
	 * 
	 * Test the java agent injection on a pom file that does not even have a build node
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJavaAgentInjection5() throws IOException {
		String pom_file = "src/test/resources/tamiflex/pf4j_plugin1_pom.xml";
		
		// save a copy of the pom file
		File file = new File(pom_file);
		File copy = new File(file.getAbsolutePath() + ".tmp");
		FileUtils.copyFile(file, copy);
		
		// inject tamiflex as a java agent in the surefire test plugin
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, null, false);
		tamiflex.injectTamiFlex(pom_file);
		String content = FileUtils.readFileToString(new File(pom_file), Charset.defaultCharset());
		assertTrue(content.contains("<build><plugins><plugin>"
				+ "<groupId>org.apache.maven.plugins</groupId>"
				+ "<artifactId>maven-surefire-plugin</artifactId>"
				+ "<version>2.20.1</version>"
				+ "<configuration><argLine>-javaagent:" + tamiflex_jar_path + "</argLine></configuration>"
				+ "</plugin></plugins></build>"));
		
		// restore the pom file
		FileUtils.copyFile(copy, file);
		copy.delete();
	}
	
	@Test
	public void testRunMavenTest() throws IOException, InterruptedException {
		String project_path = "src/test/resources/simple-test-project";
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, project_path, false);
		boolean result = tamiflex.runMavenTest();
		assertTrue(result);
	}
	
	@Test
	public void testRunMavenTest2() throws IOException, InterruptedException {
		String project_path = "src/test/resources/square_okhttp";
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, project_path, false);
		boolean result = tamiflex.runMavenTest();
		// if square_okhttp does not pass all its test cases, the assertion will fail
		assertTrue(result);
	}
	
	@Test
	public void testLogAnalysis() {
		TamiFlexRunner tamiflex = new TamiFlexRunner(null, null, false);
		String log = "src/test/resources/tamiflex/junit_refl.log";
		tamiflex.analyze("junit", log);
		assertEquals(1040, tamiflex.accessed_classes.get("junit").size());
		assertEquals(626, tamiflex.accessed_fields.get("junit").size());
		assertEquals(2975, tamiflex.used_methods.get("junit").size());
	}
	
	@Test
	public void testLogAnalysis2() {
		TamiFlexRunner tamiflex = new TamiFlexRunner(null, null, false);
		String log = "src/test/resources/tamiflex/apache_commons_lang_refl.log";
		tamiflex.analyze("commons-lang3", log);
		assertEquals(896, tamiflex.accessed_classes.get("commons-lang3").size());
		assertEquals(867, tamiflex.accessed_fields.get("commons-lang3").size());
		assertEquals(4705, tamiflex.used_methods.get("commons-lang3").size());
	}
	
	@Test
	public void testTamiFlexRunner() {
		String project_path = "src/test/resources/apache_commons-lang";
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, project_path, false);
		try {
			tamiflex.run();
			assertEquals(894, tamiflex.accessed_classes.get("commons-lang3").size());
			assertEquals(861, tamiflex.accessed_fields.get("commons-lang3").size());
			assertEquals(4696, tamiflex.used_methods.get("commons-lang3").size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTamiFlexRerun() {
		String project_path = "src/test/resources/apache_commons-lang";
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, project_path, true);
		try {
			tamiflex.run();
			assertEquals(894, tamiflex.accessed_classes.get("commons-lang3").size());
			assertEquals(861, tamiflex.accessed_fields.get("commons-lang3").size());
			assertEquals(4696, tamiflex.used_methods.get("commons-lang3").size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTamiFlexOnMavenProjectWithOneSubmodule() {
		// the gson project has many submodules but only one submodule is actually built
		String project_path = this.gitGetter.addGitHubProject("google","gson",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/google_gson")).getAbsolutePath();
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, project_path, true);
		try {
			tamiflex.run();
			// flaky
			assertEquals(435, tamiflex.accessed_classes.get("gson").size());
			assertEquals(521, tamiflex.accessed_fields.get("gson").size());
			assertEquals(1668, tamiflex.used_methods.get("gson").size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTamiFlexOnMavenProjectWithMultiSubmodules() {
		// the essentials project has multiple modules compiled but only one module has 
		// real Java class files, the other two only have resources
		String project_path = this.gitGetter.addGitHubProject("greenrobot","essentials",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/greenrobot_essentials")).getAbsolutePath();
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, project_path, true);
		try {
			tamiflex.run();
			assertEquals(1, tamiflex.accessed_classes.size());
			assertEquals(1, tamiflex.accessed_fields.size());
			assertEquals(1, tamiflex.used_methods.size());
			assertEquals(72, tamiflex.accessed_classes.get("essentials").size());
			assertEquals(72, tamiflex.accessed_fields.get("essentials").size());
			// some tests are not deterministic, so the assertion below may fail
			assertEquals(262, tamiflex.used_methods.get("essentials").size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Note that injecting TamiFlex causes test failures in this project. 
	 */
	@Test
	public void testTamiFlexOnMavenProjectWithMultiSubmodules2() {
		// the cglib project has five modules
		// four of them have java class files and only two of them 
		// have test classes
		String project_path = this.gitGetter.addGitHubProject("cglib","cglib",
				new File("/media/troy/Disk2/ONR/BigQuery/sample-projects/cglib_cglib")).getAbsolutePath();
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, project_path, true);
		try {
			tamiflex.run();
			// only one module is tested successfully
			assertEquals(1, tamiflex.accessed_classes.size());
			assertEquals(1, tamiflex.accessed_fields.size());
			assertEquals(1, tamiflex.used_methods.size());
			HashSet<String> all_accessed_classes = new HashSet<String>();
			for(String module : tamiflex.accessed_classes.keySet()) {
				all_accessed_classes.addAll(tamiflex.accessed_classes.get(module));
			}
			assertEquals(56, all_accessed_classes.size());
			HashSet<String> all_accessed_fields = new HashSet<String>();
			for(String module : tamiflex.accessed_fields.keySet()) {
				all_accessed_fields.addAll(tamiflex.accessed_fields.get(module));
			}
			// flaky
			assertEquals(37, all_accessed_fields.size());
			HashSet<String> all_used_methods = new HashSet<String>();
			for(String module : tamiflex.used_methods.keySet()) {
				all_used_methods.addAll(tamiflex.used_methods.get(module));
			}
			// flaky
			assertEquals(391, all_used_methods.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTamiFlexConfig() {
		// create a dummy TamiFlexRunner object
		TamiFlexRunner tamiflex = new TamiFlexRunner("", "", false);
		try {
			tamiflex.checkTamiFlexConfig();
			
			File propFile = new File(
					System.getProperty("user.home") + File.separator + ".tamiflex" + File.separator + "poa.properties");
			assertTrue(propFile.exists());
			String content = FileUtils.readFileToString(propFile, Charset.defaultCharset());
			assertTrue(content.contains("dontDumpClasses = true"));
			assertTrue(content.contains("dontNormalize = true"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
