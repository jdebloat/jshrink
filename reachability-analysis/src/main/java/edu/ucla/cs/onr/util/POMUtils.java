package edu.ucla.cs.onr.util;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class POMUtils {
	public static String getArtifactId(String path) {
		File pom_file = new File(path);
		String artifact_id = null;
		try {
			// parse the pom file as xml
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(pom_file);
			doc.getDocumentElement().normalize();
			
			// build the xpath to locate the artifact id
			// note that the parent node also contains a artifactId node
			// so we need to specify that the artifactId node we are looking 
			// for is under the project node
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			XPathExpression expr = xpath.compile("/project/artifactId");
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if(nodes.getLength() != 1) {
            	// the pom file should not contain more than one artifact id node
            	System.err.println("There are zero or multiple artifact ids in " + path);
            } else {
            	artifact_id = nodes.item(0).getTextContent();
            }
		} catch (SAXException | ParserConfigurationException | IOException | XPathExpressionException e) {
			e.printStackTrace();
		}
		
		return artifact_id;
	}
}
