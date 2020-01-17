package edu.ucla.cs.proguard;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;

public class ProGuardRunner {
    private final String proguardJar;

    public ProGuardRunner() {
        URL res = getClass().getClassLoader().getResource("proguard.jar");
        File file = null;
        try {
            file = Paths.get(res.toURI()).toFile();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if(file != null) {
            this.proguardJar = file.getAbsolutePath();
        } else {
            this.proguardJar = "";
        }
    }

    public void run(String jarFilePath, String dependenciesPath) throws IOException, InterruptedException {
        // write out the ProGuard config file
        String configFilePath = config(jarFilePath, dependenciesPath);

        // run ProGuard from command line
        String[] cmd = {"java", "-jar", proguardJar, "@" + configFilePath};
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        InputStream stdout = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdout);
        BufferedReader br = new BufferedReader(isr);

        String output = null;
        while ((output = br.readLine()) != null) {
            System.out.println(output);
        }
        process.waitFor();
    }

    /* set dependenciesPath to an empty string if the input jar file contains all its dependencies */
    private String config(String jarFilePath, String dependenciesPath) {
        String lineBreak = System.lineSeparator();
        String jarFilePathNoExtension = jarFilePath.substring(0, jarFilePath.lastIndexOf('.'));

        String s = "-injars " + jarFilePath +lineBreak;
        s += "-outjars " + jarFilePathNoExtension + "_out.jar" + lineBreak;
        s += "-libraryjars  <java.home>/lib/rt.jar" + lineBreak;
        s += "-libraryjars  <java.home>/lib/jce.jar" + lineBreak;
        String[] dependencies = dependenciesPath.split(";");
        for(String dependency : dependencies) {
            s += "-libraryjars " + dependency + lineBreak;
        }
        s += "-dontobfuscate" + lineBreak;
        s += lineBreak;
        s += "-keep public class * {" + lineBreak;
        s += "    public *;" + lineBreak;
        s += "}";

        File configFile = new File(jarFilePathNoExtension + ".pro");
        try {
            FileUtils.write(configFile, s, Charset.defaultCharset(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return configFile.getAbsolutePath();
    }
}
