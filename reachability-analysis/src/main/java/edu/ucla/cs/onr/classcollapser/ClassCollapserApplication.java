package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.reachability.CallGraphAnalysis;
import edu.ucla.cs.onr.reachability.EntryPointProcessor;
import edu.ucla.cs.onr.reachability.IProjectAnalyser;
import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.ASMUtils;
import edu.ucla.cs.onr.util.ClassFileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import soot.Scene;
import soot.SootClass;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class ClassCollapserApplication {

    public static void main(String[] args) {
        ArrayList<File> appClassPath = new ArrayList<File>();
        ArrayList<File> testClassPath = new ArrayList<File>();
        String appClassPathName = "/Users/zonghengma/Documents/UCLA/capstone_new/prgms/junit4/target/classes";
        String testClassPathName = "/Users/zonghengma/Documents/UCLA/capstone_new/prgms/junit4/target/test-classes";
//        String appClassPathName = "/Users/zonghengma/Documents/UCLA/capstone_new/call-graph-analysis/reachability-analysis/src/test/resources/classcollapser/ldc";
//        String testClassPathName = null;


        if (appClassPathName != null) {
            File f = new File(appClassPathName);
            appClassPath.add(f);
        }

        if (testClassPathName != null) {
            File tf = new File(testClassPathName);
            testClassPath.add(tf);
        }
        ClassCollapserCallGraphAnalysis cgAnalysis = new ClassCollapserCallGraphAnalysis(new ArrayList<File>(), appClassPath, testClassPath, new EntryPointProcessor(true, true, true, new HashSet<MethodData>()));

//        ClassCollapserMavenSingleProjectAnalyzer cgAnalysis = new ClassCollapserMavenSingleProjectAnalyzer(mavenPath, new EntryPointProcessor(true, true, true, new HashSet<MethodData>()), null);
        cgAnalysis.setup();
        cgAnalysis.run();

        Set<String> classesToRewrite = new HashSet<String>();
        Set<String> classesToRemove = new HashSet<String>();

        IClassCollapserAnalyser ccAnalysis = new ClassCollapserAnalysis(cgAnalysis.getAppClasses(), cgAnalysis.getUsedAppClasses(), cgAnalysis.getUsedAppMethods());
        ccAnalysis.run();

        HashMap<String, SootClass> nameToSootClass = new HashMap<String, SootClass>();

        for (String n: ccAnalysis.getNameChangeList().keySet()) {
            System.out.println("name change: " + n + " " + ccAnalysis.getNameChangeList().get(n));
        }

        for (ArrayList<String> collapse: ccAnalysis.getCollapseList()) {
            System.out.printf("merge %s into %s\n", collapse.get(0), collapse.get(1));
            String fromName = collapse.get(0);
            String toName = collapse.get(1);
            if (!nameToSootClass.containsKey(fromName)) {
                nameToSootClass.put(fromName, Scene.v().loadClassAndSupport(fromName));
            }
            if (!nameToSootClass.containsKey(toName)) {
                nameToSootClass.put(toName, Scene.v().loadClassAndSupport(toName));
            }
            SootClass from = nameToSootClass.get(fromName);
            SootClass to = nameToSootClass.get(toName);

            ClassCollapser.mergeTwoClasses(from, to, ccAnalysis.getProcessedUsedMethods());

//            nameToSootClass.put(toName, to);
            classesToRewrite.add(toName);
            classesToRemove.add(fromName);
        }

//        Map<String, String> nameChangeList = ccAnalysis.getNameChangeList();
//        for(String fromName: nameChangeList.keySet()) {
//            String toName = nameChangeList.get(fromName);
//            if (!nameToSootClass.containsKey(fromName)) {
//                nameToSootClass.put(fromName, Scene.v().loadClassAndSupport(fromName));
//            }
//            if (!nameToSootClass.containsKey(toName)) {
//                nameToSootClass.put(toName, Scene.v().loadClassAndSupport(toName));
//            }
//            SootClass from = nameToSootClass.get(fromName);
//            SootClass to = nameToSootClass.get(toName);
//            for (String className : cgAnalysis.getAppClasses()) {
//                if (!nameToSootClass.containsKey(className)) {
//                    nameToSootClass.put(className, Scene.v().loadClassAndSupport(className));
//                }
//                SootClass sootClass = nameToSootClass.get(className);
//                if (ClassCollapser.changeClassNamesInClass(sootClass, from, to)) {
//                    classesToRewrite.add(className);
//                }
//            }
//            for (String className : cgAnalysis.getTestClasses()) {
//                if (!nameToSootClass.containsKey(className)) {
//                    nameToSootClass.put(className, Scene.v().loadClassAndSupport(className));
//                }
//                SootClass sootClass = nameToSootClass.get(className);
//                if (ClassCollapser.changeClassNamesInClass(sootClass, from, to)) {
//                    classesToRewrite.add(className);
//                }
//            }
//        }

        Set<File> classPathsOfConcern = new HashSet<File>();
        classPathsOfConcern.addAll(appClassPath);
        classPathsOfConcern.addAll(testClassPath);
//        classPathsOfConcern.addAll(cgAnalysis.getAppClasspaths());
//        classPathsOfConcern.addAll(cgAnalysis.getTestClasspaths());
        for (String className: classesToRewrite) {
            if (!classesToRemove.contains(className)) {
                System.out.println("rewriting: " + className);
                try {
                    ClassFileUtils.writeClass(nameToSootClass.get(className), classPathsOfConcern);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }

        for (String className: classesToRemove) {
            try {
                ClassFileUtils.removeClass(nameToSootClass.get(className), classPathsOfConcern);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        Map<String, String> nameChangeList = ccAnalysis.getNameChangeList();
        for(String fromName: nameChangeList.keySet()) {
            String toName = nameChangeList.get(fromName);

            String fromNameFormatted = ASMUtils.formatClassName(fromName);
            String toNameFormatted = ASMUtils.formatClassName(toName);

            for (String className : cgAnalysis.getAppClasses()) {
                if (!classesToRemove.contains(className)) {
                    String classNameFormatted = ASMUtils.formatClassName(className);
                    try {
                        String path = appClassPathName + '/' + classNameFormatted + ".class";
                        FileInputStream is = new FileInputStream(path);
                        ClassReader cr = new ClassReader(is);
                        ClassWriter cw = new ClassWriter(cr, 0);
                        NameChangeClassWriter nccw = new NameChangeClassWriter(Opcodes.ASM5, cw, fromNameFormatted, toNameFormatted);
                        cr.accept(nccw, 0);
                        if (nccw.isChanged()) {
                            System.out.println("changed: " + className);
                            ASMUtils.writeClass(path, cw);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            for (String className : cgAnalysis.getTestClasses()) {
                if (!classesToRemove.contains(className)) {
                    String classNameFormatted = ASMUtils.formatClassName(className);
                    try {
                        String path = testClassPathName + '/' + classNameFormatted + ".class";
                        FileInputStream is = new FileInputStream(path);
                        ClassReader cr = new ClassReader(is);
                        ClassWriter cw = new ClassWriter(cr, 0);
                        NameChangeClassWriter nccw = new NameChangeClassWriter(Opcodes.ASM5, cw, fromNameFormatted, toNameFormatted);
                        cr.accept(nccw, 0);
                        if (nccw.isChanged()) {
                            System.out.println("changed: " + className);
                            ASMUtils.writeClass(path, cw);
                        }
                        is.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        int originCount = cgAnalysis.getAppClasses().size();
        int afterCount = originCount - ccAnalysis.getNameChangeList().size();
        float percentage = (originCount - afterCount) / (float) originCount;
        System.out.printf("Original class count: %d, After class count: %d, percentage: %f\n", originCount, afterCount, percentage);
    }
}
