package edu.ucla.cs.onr.test_analysis;

public class TestResult {
	private int total_num_of_test;
	private int num_of_test_success;
	private int num_of_test_failure;
	
	public TestResult(int totalTestCount, int successTestCount, int failureTestCount) {
		this.total_num_of_test = totalTestCount;
		this.num_of_test_success = successTestCount;
		this.num_of_test_failure = failureTestCount;
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
}
