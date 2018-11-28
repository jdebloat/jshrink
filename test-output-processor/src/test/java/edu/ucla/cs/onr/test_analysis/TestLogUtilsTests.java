package edu.ucla.cs.onr.test_analysis;

import java.io.File;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class TestLogUtilsTests {

    @Test
    public void analyzeTestLogTest(){
        ClassLoader classLoader = TestLogUtilsTests.class.getClassLoader();
        File testOutput = new File(classLoader.getResource("test_output1.dat").getFile());

       TestResult testResult = TestLogUtils.analyzeTestLog(testOutput);

        assertEquals(403, testResult.getTotalTestCount());
        assertEquals(402, testResult.getTestSuccessCount());
        assertEquals(1, testResult.getTestFailureCount());
        assertEquals(0, testResult.getTestSkippedCount());
    }

    @Test
    public void analyzeTestLogTest2(){
        ClassLoader classLoader = TestLogUtilsTests.class.getClassLoader();
        File testOutput = new File(classLoader.getResource("test_output2.dat").getFile());

        TestResult testResult = TestLogUtils.analyzeTestLog(testOutput);

        assertEquals(100, testResult.getTotalTestCount());
        assertEquals(0, testResult.getTestSuccessCount());
        assertEquals(99, testResult.getTestFailureCount());
        assertEquals(1, testResult.getTestSkippedCount());
    }

}