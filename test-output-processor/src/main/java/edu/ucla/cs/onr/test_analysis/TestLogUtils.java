package edu.ucla.cs.onr.test_analysis;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import edu.ucla.cs.onr.test_analysis.TestResult;

public class TestLogUtils {
	final static String test_regex = "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)$";

	public static TestResult analyzeTestLog(File testLog) {
		int test_count = 0;
		int failure_count = 0;
		int success_count = 0;
		int skipped_count = 0;
		// analyze the test log
		String log = "";
		try {
			log = FileUtils.readFileToString(testLog);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		// count the number of executed, succeeded, and failed test
		// cases
		String[] log_lines = log.split(System.lineSeparator());

		Pattern pattern = Pattern.compile(test_regex);
		for (String line : log_lines) {
			if (line.contains("Tests run: ")) {
				Matcher matcher = pattern.matcher(line);
				while (matcher.find()) {
					test_count += Integer.parseInt(matcher.group(1));
					failure_count += Integer.parseInt(matcher.group(2)) //Includes failure and test errors
							+ Integer.parseInt(matcher.group(3));
					skipped_count += Integer.parseInt(matcher.group(4));

				}
			}
		}
		
		success_count = test_count - failure_count - skipped_count;
		return new TestResult(test_count, success_count, failure_count, skipped_count);
	}
}
