package edu.ucla.cs.jshrinklib.reachability;

public class TestOutput {
	private final int run;
	private final int failures;
	private final int errors;
	private final int skipped;
	private final String testOutputText;

	public TestOutput(int run, int failures, int errors, int skipped, String testOut){
		this.run = run;
		this.failures = failures;
		this.errors = errors;
		this.skipped = skipped;
		this.testOutputText = testOut;
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

	public String getTestOutputText(){
		return this.testOutputText;
	}

	@Override
	public boolean equals(Object o){
		if(o instanceof TestOutput){
			TestOutput testOutput = (TestOutput) o;
			return testOutput.run == this.run && testOutput.failures == this.failures
				&& testOutput.errors == this.errors && testOutput.skipped == this.skipped
				&& testOutput.testOutputText.equals(this.testOutputText);
		}

		return false;
	}

	@Override
	public int hashCode(){
		return this.run + (this.failures * 31) + (this.errors * 31 * 31) + (this.errors * 31 * 31 * 31)
			+ this.testOutputText.hashCode();
	}
}
