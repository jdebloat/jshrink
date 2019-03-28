package edu.ucla.cs.jshrinklib;

import edu.ucla.cs.jshrinklib.util.SootUtils;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.jimple.JasminClass;
import soot.options.Options;
import soot.util.JasminOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {
    /*
     * Calling this method will cause the classpath of Soot to be reset
     * Therefore, do not use this method if you are also using Soot for other analysis
     */
    public static SootClass getSootClass(String classPath, String className){
        Options.v().set_soot_classpath(SootUtils.getJREJars() + File.pathSeparator + classPath);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);

        List<String> processDirs = new ArrayList<String>();
        processDirs.add(classPath);
        Options.v().set_process_dir(processDirs);

        SootClass sClass = Scene.v().loadClassAndSupport(className);
        Scene.v().loadNecessaryClasses();

        return sClass;
    }

    public static File createClass(SootClass sootClass){

        // I receive 'Exception thrown: method <init> has no active body' if methods are not retrieved
        for(SootMethod sootMethod : sootClass.getMethods()){
            sootMethod.retrieveActiveBody();
        }

        File fileToReturn = null;
        try {
            String fileName = SourceLocator.v().getFileNameFor(sootClass, Options.output_format_class);
            fileToReturn = new File(fileName);
            OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileToReturn));
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

            JasminClass jasminClass = new JasminClass(sootClass);
            jasminClass.print(writerOut);
            writerOut.flush();
            streamOut.close();

        } catch(Exception e){
            System.err.println("Exception thrown: " + e.getMessage());
            System.exit(1);
        }

        assert(fileToReturn != null);

        return fileToReturn;
    }

    public static String runClass(SootClass sootClass){
        File classFile = createClass(sootClass);

        String cmd = "java -cp "+classFile.getParentFile().getAbsolutePath() + " "
            + classFile.getName().replaceAll(".class","");

        Process p =null;
        StringBuilder output = new StringBuilder();
        try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();

            BufferedReader brInputStream = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader brErrorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String line;
            while((line = brInputStream.readLine())!=null){
                output.append(line + System.lineSeparator());
            }
            brInputStream.close();

            while((line = brErrorStream.readLine()) != null){
                output.append(line + System.lineSeparator());
            }
            brErrorStream.close();

            //} catch(IOException e InterruptedException ie){
        } catch(Exception e){
            System.err.println("Exception thrown when trying to run the following script:");
            StringBuilder sb = new StringBuilder();
            System.err.println(cmd);
            System.err.println("The following error was thrown: ");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        return output.toString();
    }
}
