package edu.ucla.cs.onr.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class MavenUtils {
	public static HashMap<String, String> getClasspaths(String logFilePath) {
		HashMap<String, String> cp_map = new HashMap<String, String>();
		File log_file = new File(logFilePath);
		try {
			List<String> lines = FileUtils.readLines(log_file, Charset.defaultCharset());
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line.equals("[INFO] Dependencies classpath:")) {
					// scan backwards till finding the first line that contains 
					// '[INFO] --- maven-dependency-plugin:'
					String header = null;
					for(int j = 1; j <= i; j++) {
						header  = lines.get(i-j);
						if (header.contains("--- maven-dependency-plugin:")) {
							break;
						}
					}
					
					if(header != null) {
						String tmp = header.split("@")[1];
						String name = tmp.substring(0, tmp.indexOf("---")).trim(); 
						String cp = lines.get(i+1);
						cp_map.put(name, cp);
					} else {
						System.err.println("Cannot find the name of "
								+ "this project/module when resolving classpaths.");
					}
				}
			}
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
