package edu.ucla.cs.onr.test_analysis;

import java.io.File;

public class Application {

    private static String BLURB = "" +
            "This small application will give the total tests passed " + System.lineSeparator() +
            "and the total tests failed in a schema" + System.lineSeparator() +
            " \"<tests_passed>,<tests_failed>,<tests_skipped>\". The tool takes one " +System.lineSeparator() +
            "argument --- the output of running `mvn test --batch-mode -fn`.";

    public static void main(String[] args){
        if(args.length != 1){
            System.err.print("Incorrect number of arguments given. One expected.");
            Application.printBlurb();
        }

        File f = new File(args[0]);

        if(!f.exists()){
            System.err.println("Argument \"" + args[0] + "\" is not a file, it does not exist.");
            Application.printBlurb();
        }

        if(f.isDirectory()){
            System.err.println("Argument \"" + args[0] + "\" is a directory. A file was expected.");
            Application.printBlurb();
        }

        TestResult testResult = TestLogUtils.analyzeTestLog(f);

        System.out.println(testResult.getTestSuccessCount() + "," + testResult.getTestFailureCount() +
                "," + testResult.getTestSkippedCount());
    }

    private static void printBlurb(){
        System.out.println(BLURB);
    }
}
