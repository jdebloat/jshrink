package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.Application;
import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.ASMUtils;
import edu.ucla.cs.onr.util.ClassFileUtils;
import edu.ucla.cs.onr.util.SootUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import soot.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static java.lang.System.exit;

public class TestApplication {
    public static void main (String[] args) {
        ArrayList<File> appClassPath = new ArrayList<File>();
        ArrayList<File> testClassPath = new ArrayList<File>();
        //        File f = new File("/Users/zonghengma/Documents/UCLA/capstone_new/test/override");
//        File f = new File("/Users/zonghengma/Documents/UCLA/capstone_new/prgms/junit4/target/classes");
        String appClassPathName = "/Users/zonghengma/Documents/UCLA/capstone_new/prgms/junit4/target/classes";
        String testClassPathName = "/Users/zonghengma/Documents/UCLA/capstone_new/prgms/junit4/target/test-classes";
        File f = new File(appClassPathName);
        File tf = new File(testClassPathName);
        appClassPath.add(f);
        testClassPath.add(tf);
        SootUtils.setup_trimming(new ArrayList<File>(), appClassPath, testClassPath);
        Scene.v().loadNecessaryClasses();
//        SootClass cls = Scene.v().loadClassAndSupport("org.apache.commons.lang3.time.FastDateParser_TimeZoneStrategyTest");
        SootClass subc = Scene.v().loadClassAndSupport("org.junit.validator.AnnotationsValidator$ClassValidator");
        SootClass superc = Scene.v().loadClassAndSupport("org.junit.validator.AnnotationsValidator$AnnotatableValidator");

//        System.out.println("original:");
//        System.out.println(subc.getSuperclass());
//        System.out.println(superc.getSuperclass());

        ClassCollapser.mergeTwoClasses(subc, superc, new HashMap<String, Set<String>>());
//        System.out.println("\nmodified:");
//        System.out.println(subc.getSuperclass());
//
//        Set<File> classPathsOfConcern = new HashSet<File>();
//        classPathsOfConcern.addAll(appClassPath);
//        classPathsOfConcern.addAll(testClassPath);
//
//        HashSet<String> libClasses = new HashSet<>();
//        HashSet<String> appClasses = new HashSet<>();
//        HashSet<String> testClasses = new HashSet<>();
//
//        HashSet<MethodData> libMethods = new HashSet<>();
//        HashSet<MethodData> appMethods = new HashSet<>();
//        HashSet<MethodData> testMethods = new HashSet<>();
//
//        HashSet<String> classesToRemove = new HashSet<>();
//        classesToRemove.add(subc.getName());
//
//        try {
//            ClassFileUtils.writeClass(superc, classPathsOfConcern);
//            ClassFileUtils.writeClass(subc, classPathsOfConcern);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
////        for (File lib : this.libJarPath) {
////            ASMUtils.readClass(lib, libClasses, libMethods);
////        }
//
//        for (File appPath : appClassPath) {
//            ASMUtils.readClass(appPath, appClasses, appMethods);
//        }
//
//        for (File testPath : testClassPath){
//            ASMUtils.readClass(testPath,testClasses,testMethods);
//        }
//
//        Map<String, String> nameChangeList = new HashMap<>();
//        nameChangeList.put(subc.getName(), superc.getName());
//
//        for(String fromName: nameChangeList.keySet()) {
//            String toName = nameChangeList.get(fromName);
//
//            String fromNameFormatted = ASMUtils.formatClassName(fromName);
//            String toNameFormatted = ASMUtils.formatClassName(toName);
//
//            for (String className : appClasses) {
//                if (!classesToRemove.contains(className)) {
//                    String classNameFormatted = ASMUtils.formatClassName(className);
//                    try {
//                        String path = appClassPathName + '/' + classNameFormatted + ".class";
//                        FileInputStream is = new FileInputStream(path);
//                        ClassReader cr = new ClassReader(is);
//                        ClassWriter cw = new ClassWriter(cr, 0);
//                        NameChangeClassWriter nccw = new NameChangeClassWriter(Opcodes.ASM5, cw, fromNameFormatted, toNameFormatted);
//                        cr.accept(nccw, 0);
//                        if (nccw.isChanged()) {
//                            System.out.println("changed: " + className);
//                            ASMUtils.writeClass(path, cw);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            for (String className : testClasses) {
//                if (!classesToRemove.contains(className)) {
//                    String classNameFormatted = ASMUtils.formatClassName(className);
//                    try {
//                        String path = testClassPathName + '/' + classNameFormatted + ".class";
//                        FileInputStream is = new FileInputStream(path);
//                        ClassReader cr = new ClassReader(is);
//                        ClassWriter cw = new ClassWriter(cr, 0);
//                        NameChangeClassWriter nccw = new NameChangeClassWriter(Opcodes.ASM5, cw, fromNameFormatted, toNameFormatted);
//                        cr.accept(nccw, 0);
//                        if (nccw.isChanged()) {
//                            System.out.println("changed: " + className);
//                            ASMUtils.writeClass(path, cw);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }


    }
}
