package edu.ucla.cs.onr.test_analysis;

public class TestResult {
	private int total_num_of_test;
	private int num_of_test_success;
	private int num_of_test_failure;
	private int num_of_skipped_tests;
	
	public TestResult(int totalTestCount, int successTestCount, int failureTestCount, int skippedTestCount) {
		assert(totalTestCount == (successTestCount + failureTestCount + skippedTestCount));
		this.total_num_of_test = totalTestCount;
		this.num_of_test_success = successTestCount;
		this.num_of_test_failure = failureTestCount;
		this.num_of_skipped_tests = skippedTestCount;
	}
	
	public int getTotalTestCount() {
		return total_num_of_test;
	}
	
	public int getTestSuccessCount() {
		return num_of_test_success;
	}
	
	public int getTestFailureCount() {
		return num_of_test_failure;
	}

	public int getTestSkippedCount(){
		return num_of_skipped_tests;
	}
}
