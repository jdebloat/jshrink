package edu.ucla.cs.onr.pomeditor;

import org.junit.After;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;

public class ApplicationTest {

	private static File getTestPomFile(){
		return new File(ApplicationTest.class.getClassLoader().getResource("test_pom.xml").getFile());
	}

	private static File getTestPomExpectedFile(){
		return new File(ApplicationTest.class.getClassLoader().getResource("test_pom_expected.xml").getFile());
	}

	@After
	public void cleanup(){
		try {
			File f = new File(ApplicationTest.class.getClassLoader().getResource(".test_pom.xml.swp").getFile());
			if (f.exists()) {
				f.delete();
			}
		} catch(NullPointerException e){
			//Do nothing, it's fine, this just means ".text_pom.xml.swp" couldn't be loaded as it already exists
		}
	}

	@Test
	public void testGetNewPomBasic() throws ParserConfigurationException,
		TransformerException, SAXException, IOException {
		String[] params = new String[1];
		params[0] = getTestPomFile().getAbsolutePath();

		String output = Application.getNewPom(params);
		String expected = Application.getXMLStringFromDocument(
			Application.getDocumentFromFile(ApplicationTest.getTestPomExpectedFile()));

		assertEquals(expected.replaceAll("[ \t]+", "").replaceAll("\n+", "\n"),
			output.replaceAll("[ \t]+", "").replaceAll("\n+", "\n"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNewPomNoParams() throws ParserConfigurationException, TransformerException,
		SAXException, IOException {
		Application.getNewPom(new String[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNewPomTooManyParams() throws ParserConfigurationException, TransformerException,
		SAXException, IOException {
		String[] params = new String[2];
		params[0] = getTestPomFile().getAbsolutePath();
		params[1] = "randomOtherParameter";
		Application.getNewPom(params);
	}
}
