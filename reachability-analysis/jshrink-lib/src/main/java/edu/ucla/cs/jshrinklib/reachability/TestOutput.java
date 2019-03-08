package edu.ucla.cs.jshrinklib.reachability;

public class TestOutput {
	private final int run;
	private final int failures;
	private final int errors;
	private final int skipped;

	public TestOutput(int run, int failures, int errors, int skipped){
		this.run = run;
		this.failures = failures;
		this.errors = errors;
		this.skipped = skipped;
	}

	public int getRun(){
		return this.run;
	}

	public int getFailures(){
		return this.failures;
	}

	public int getErrors(){
		return this.errors;
	}

	public int getSkipped(){
		return this.skipped;
	}
}
