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
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path);
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
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path);
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
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path);
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
		TamiFlexRunner tamiflex = new TamiFlexRunner(tamiflex_jar_path);
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
}
