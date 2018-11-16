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
    }

}