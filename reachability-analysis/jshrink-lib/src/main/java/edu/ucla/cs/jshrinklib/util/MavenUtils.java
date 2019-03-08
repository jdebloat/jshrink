package edu.ucla.cs.jshrinklib.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ucla.cs.jshrinklib.reachability.TestOutput;
import org.apache.commons.io.FileUtils;

public class MavenUtils {

	public static HashMap<String, String> getClasspaths(String log_file_contents){
		HashMap<String, String> cp_map = new HashMap<String, String>();

		String[] lines = log_file_contents.split(System.lineSeparator());
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.equals("[INFO] Dependencies classpath:")) {
				// scan backwards till finding the first line that contains
				// '[INFO] --- maven-dependency-plugin:'
				String header = null;
				for(int j = 1; j <= i; j++) {
					header  = lines[i-j];
					if (header.contains("--- maven-dependency-plugin:")) {
						break;
					}
				}

				if(header != null) {
					String tmp = header.split("@")[1];
					String name = tmp.substring(0, tmp.indexOf("---")).trim();
					String cp = lines[i+1];
					cp_map.put(name, cp);
				} else {
					System.err.println("Cannot find the name of "
						+ "this project/module when resolving classpaths.");
				}
			}
		}

		return cp_map;
	}

	public static TestOutput testOutputFromString(String mavenOutput){
		String test_regex = "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)$";

		String[] log_lines = mavenOutput.split(System.lineSeparator());

		Pattern pattern = Pattern.compile(test_regex);
		int run = -1;
		int failures = -1;
		int errors = -1;
		int skipped = -1;
		for (String line : log_lines) {
			if (pattern.matcher(line).matches()) {
				Matcher matcher = Pattern.compile("\\d+").matcher(line);
				matcher.find();
				run = Integer.parseInt(matcher.group());
				matcher.find();
				failures = Integer.parseInt(matcher.group());
				matcher.find();
				errors = Integer.parseInt(matcher.group());
				matcher.find();
				skipped = Integer.parseInt(matcher.group());
				break;
			}
		}

		return new TestOutput(run, failures, errors, skipped);
	}

	public static HashMap<String, String> getClasspathsFromFile(File log_file) {
		HashMap<String, String> cp_map = new HashMap<String, String>();
		try {
			String log_file_contents = FileUtils.readFileToString(log_file, Charset.defaultCharset());
			cp_map.putAll(getClasspaths(log_file_contents));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cp_map;
	}
	
	public static void getModules(File dir, HashMap<String, File> modules) {
		String pom_path = dir.getAbsolutePath() + File.separator + "pom.xml";
		File pom = new File(pom_path);
		if(pom.exists()) {
			// double check whether there is a src dir
			String src_dir = dir.getAbsolutePath() + File.separator + "src";
			if(new File(src_dir).exists()) {
				// only consider the artifacts with src directories
				String artifact_id = POMUtils.getArtifactId(pom_path);
				modules.put(artifact_id, dir);
			}
			
			for(File f : dir.listFiles()) {
				getModules(f, modules);
			}
		} else {
			// stop traversing in this dir
			return;
		}
	}
}
