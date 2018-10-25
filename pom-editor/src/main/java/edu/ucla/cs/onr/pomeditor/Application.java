package edu.ucla.cs.onr.pomeditor;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/*
The purpose of this application is to take in a Maven pom.xml file and output (via stdout), an alternative version
we use for experimental purposes. This alternative pom.xml looks for all directories in the "target/lib" directory.
 */

public class Application {
	public static void main(String[] args){

		try {
			System.out.println(Application.getNewPom(args));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*package*/ static String getNewPom(String[] args) throws IllegalArgumentException,
		ParserConfigurationException, IOException, SAXException, TransformerException{
		if(args.length == 0){
			throw new IllegalArgumentException("No command line arguments given. This application requires one:" +
				"the pom.xml to be edited.");
		} else if(args.length > 1){
			throw new IllegalArgumentException("Too many command line arguments given. This application only requires" +
				"one: the pom.xml to be edited.");
		}

		assert(args.length == 1);

		File f = new File(args[0]);
		if(!f.exists()){
			throw new IOException("File '" + f + "' does not exist");
		} else if(!f.isFile()){
			throw new IOException("'" + f + "' is not a file");
		}

		Document doc = getDocumentFromFile(f);

		if(doc.getElementsByTagName("dependencies").getLength() != 1){
			System.out.println("Here");
		}
		Node dependencies = doc.getElementsByTagName("dependencies").item(0);
		List<Node> dependencyList = new ArrayList<Node>();
		for(int i=0; i<dependencies.getChildNodes().getLength(); i++){
			if(dependencies.getChildNodes().item(i).getNodeName().equals("dependency")){
				dependencyList.add(dependencies.getChildNodes().item(i));
			}
		}

		for(Node dependency : dependencyList){
			//Node dependency = dependencies.getChildNodes().item(i);
			NodeList children = dependency.getChildNodes();

			if(!getNode("artifactId", children).isPresent()
				|| !getNode("version", children).isPresent()){
				continue;
			}

			Node artifactId = getNode("artifactId", children).get();
			Node version = getNode("version", children).get();

			boolean toChange = true;

			Optional<Node> systemPathNode = getNode("systemPath", children);
			Optional<Node> scopeNode = getNode("scope", children);

			if(!scopeNode.isPresent() || !scopeNode.get().getFirstChild().getNodeValue().equals("test")) {

				if(scopeNode.isPresent()){
					dependency.removeChild(scopeNode.get());
				}

				Element scope = doc.createElement("scope");
				scope.appendChild(doc.createTextNode("system"));
				dependency.appendChild(scope);

				String jarName = artifactId.getFirstChild().getNodeValue() +
					"-" + version.getFirstChild().getNodeValue() + ".jar";

				if(systemPathNode.isPresent()){
					jarName = (new File(systemPathNode.get().getFirstChild().getNodeValue())).getName();
					dependency.removeChild(systemPathNode.get());
				}

				Element systemPath = doc.createElement("systemPath");
				systemPath.appendChild(doc.createTextNode("${project.basedir}/target/lib/" + jarName));
				dependency.appendChild(systemPath);
			}
		}

		return Application.getXMLStringFromDocument(doc);

	}

	private static Optional<Node> getNode(String toFind, NodeList nodeList){
		for(int i=0; i<nodeList.getLength(); i++){
			Node node = nodeList.item(i);
			if(node.getNodeName().equals(toFind)){
				return Optional.of(node);
			}
		}

		return Optional.empty();
	}


	/*package*/ static Document getDocumentFromFile(File file) throws ParserConfigurationException,
		IOException, SAXException, IOException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);

		return doc;
	}

	/*package*/ static String getXMLStringFromDocument(Document doc) throws TransformerException{
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));

		return writer.getBuffer().toString();
	}
}
