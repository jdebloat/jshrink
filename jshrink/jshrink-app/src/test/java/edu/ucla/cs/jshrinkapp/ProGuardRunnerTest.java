package edu.ucla.cs.jshrinkapp;

import edu.ucla.cs.proguard.ProGuardRunner;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ProGuardRunnerTest {
    @Test
    public void testProGuardRunner() throws IOException, InterruptedException {
        String jarFilePath = ProGuardRunner.class.getClassLoader().getResource("proguard" + File.separator + "junit-4.13.jar").getPath();
        String dependenciesPath = ProGuardRunner.class.getClassLoader().getResource("proguard" + File.separator + "hamcrest-core-1.3.jar").getPath();
        ProGuardRunner runner = new ProGuardRunner();
        runner.run(jarFilePath, dependenciesPath);
    }
}
