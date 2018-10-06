package edu.ucla.cs.onr;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class MavenLogUtils {
	public static LinkedHashMap<String, String> getClasspaths(String logFilePath) {
		LinkedHashMap<String, String> cp_map = new LinkedHashMap<String, String>();
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
}
