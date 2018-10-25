package edu.ucla.cs.onr.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class PomUtilTest {
	@Test
	public void test1() {
		String path = "src/test/resources/activiti-bpmn-converter-pom.xml";
		String id = POMUtils.getArtifactId(path);
		assertEquals("activiti-json-converter", id);
	}
}
