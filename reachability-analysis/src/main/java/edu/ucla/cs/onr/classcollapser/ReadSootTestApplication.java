package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.util.SootUtils;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.io.File;
import java.util.ArrayList;

public class ReadSootTestApplication {
    public static void main(String[] args) {
        ArrayList<File> appClassPath = new ArrayList<File>();
        ArrayList<File> testClassPath = new ArrayList<File>();
        String appClassPathName = "/Users/zonghengma/Documents/UCLA/capstone_new/prgms/junit4/target/classes";
        String testClassPathName = "/Users/zonghengma/Documents/UCLA/capstone_new/prgms/junit4/target/test-classes";
        File f = new File(appClassPathName);
        File tf = new File(testClassPathName);
        appClassPath.add(f);
        testClassPath.add(tf);
        SootUtils.setup_trimming(new ArrayList<File>(), appClassPath, testClassPath);
        Scene.v().loadNecessaryClasses();
        SootClass sootc = Scene.v().loadClassAndSupport("org.junit.validator.AnnotationsValidator$ClassValidator");
        System.out.println(sootc.getSuperclass().getName());
        for (SootMethod m: sootc.getMethods()) {
            System.out.println(m);
            System.out.println(m.retrieveActiveBody());
        }
    }
}
