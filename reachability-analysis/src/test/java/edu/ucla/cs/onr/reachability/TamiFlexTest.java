package edu.ucla.cs.onr.reachability;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class TamiFlexTest {
	/**
	 * Test the java agent injection on a pom file that (1) explicitly declares surefire plugin,
	 * and (2) has the configuration node but no the argLine node
	 * 
	 * @throws IOException
	 */
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
		assertTrue(result);
	}
	
	@Test
	public void testLogAnalysis() {
		TamiFlexRunner tamiflex = new TamiFlexRunner(null, null, false);
		String log = "src/test/resources/tamiflex/junit_refl.log";
		tamiflex.analyze(log);
		assertEquals(1040, tamiflex.accessed_classes.size());
		assertEquals(698, tamiflex.accessed_fields.size());
		assertEquals(3846, tamiflex.used_methods.size());
	}
	
	@Test
	public void testLogAnalysis2() {
		TamiFlexRunner tamiflex = new TamiFlexRunner(null, null, false);
		String log = "src/test/resources/tamiflex/apache_commons_lang_refl.log";
		tamiflex.analyze(log);
		assertEquals(896, tamiflex.accessed_classes.size());
		assertEquals(985, tamiflex.accessed_fields.size());
		assertEquals(5824, tamiflex.used_methods.size());
	}
	
	@Test
	public void testTamiFlexRunner() {
		String project_path = "src/test/resources/apache_commons-lang";
		String tamiflex_jar_path = "src/test/resources/tamiflex/poa-2.0.3.jar";
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path, project_path, false);
		try {
			tamiflex.run();
			assertEquals(894, tamiflex.accessed_classes.size());
			assertEquals(979, tamiflex.accessed_fields.size());
			assertEquals(5805, tamiflex.used_methods.size());
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
			assertEquals(894, tamiflex.accessed_classes.size());
			assertEquals(979, tamiflex.accessed_fields.size());
			assertEquals(5805, tamiflex.used_methods.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
