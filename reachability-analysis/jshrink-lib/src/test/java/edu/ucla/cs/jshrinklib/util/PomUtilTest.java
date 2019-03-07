package edu.ucla.cs.jshrinklib.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;

public class PomUtilTest {
	@Test
	public void test1() {
		String path = new File(PomUtilTest.class.getClassLoader()
			.getResource("activiti-bpmn-converter-pom.xml").getFile()).getAbsolutePath();
		String id = POMUtils.getArtifactId(path);
		assertEquals("activiti-json-converter", id);
	}
}
