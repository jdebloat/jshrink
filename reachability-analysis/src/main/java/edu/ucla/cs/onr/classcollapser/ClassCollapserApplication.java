package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.reachability.CallGraphAnalysis;
import edu.ucla.cs.onr.reachability.EntryPointProcessor;
import edu.ucla.cs.onr.reachability.IProjectAnalyser;
import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.ClassFileUtils;
import soot.Scene;
import soot.SootClass;

import java.io.File;
import java.util.*;

public class ClassCollapserApplication {

    public static void main(String[] args) {
        ArrayList<File> appClassPath = new ArrayList<File>();
//        File f = new File("/Users/zonghengma/Documents/UCLA/capstone_new/test/override");
        File f = new File("/Users/zonghengma/Documents/UCLA/capstone_new/prgms/junit4/target/classes");
//        File f = new File("/Users/zonghengma/Documents/UCLA/capstone_new/prgms/commons-lang/target/classes");
//        System.out.println(f.listFiles().length);
//        File f = new File("/Users/zonghengma/Documents/UCLA/capstone_new/prgms/curator/curator-client/target/classes");
//        System.out.println(f.exists());
//        System.out.println(f.isDirectory());
        appClassPath.add(f);
//        File f2 = new File("/Users/zonghengma/Documents/UCLA/capstone_new/prgms/curator/curator-test/target/classes");
//        appClassPath.add(f2);
        IProjectAnalyser cgAnalysis = new ClassCollapserCallGraphAnalysis(new ArrayList<File>(), appClassPath, new ArrayList<File>(), new EntryPointProcessor(true, true, true, false, new HashSet<MethodData>()));
        cgAnalysis.setup();
        cgAnalysis.run();
//        Set<String> usedAppClasses = cgAnalysis.getUsedAppClasses();
//        for (String c : usedAppClasses) {
//            System.out.println(c);
//        }
//        Set<String> usedClasses = new HashSet<String>();
//        usedClasses.add("B");
//        usedClasses.add("main");
//        Set<MethodData> usedAppMethodData = new HashSet<MethodData>();

        Set<String> classesToRewrite = new HashSet<String>();
        Set<String> classesToRemove = new HashSet<String>();

        IClassCollapserAnalyser ccAnalysis = new ClassCollapserAnalysis(cgAnalysis.getAppClasses(), cgAnalysis.getUsedAppClasses(), cgAnalysis.getUsedAppMethods());
        ccAnalysis.run();

        HashMap<String, SootClass> nameToSootClass = new HashMap<String, SootClass>();

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

            ClassCollapser.mergeTwoClasses(from, to, ((ClassCollapserAnalysis) ccAnalysis).getProcessedUsedMethods());

            classesToRewrite.add(toName);
            classesToRemove.add(fromName);
        }

        Map<String, String> nameChangeList = ccAnalysis.getNameChangeList();
        for(String fromName: nameChangeList.keySet()) {
            String toName = nameChangeList.get(fromName);
            if (!nameToSootClass.containsKey(fromName)) {
                nameToSootClass.put(fromName, Scene.v().loadClassAndSupport(fromName));
            }
            if (!nameToSootClass.containsKey(toName)) {
                nameToSootClass.put(toName, Scene.v().loadClassAndSupport(toName));
            }
            SootClass from = nameToSootClass.get(fromName);
            SootClass to = nameToSootClass.get(toName);
            for (String className : cgAnalysis.getAppClasses()) {
                if (!nameToSootClass.containsKey(className)) {
                    nameToSootClass.put(className, Scene.v().loadClassAndSupport(className));
                    SootClass sootClass = nameToSootClass.get(className);
                    if (ClassCollapser.changeClassNamesInClass(sootClass, from, to)) {
                        classesToRewrite.add(className);
                    }
                }
            }
        }

        Set<File> classPathsOfConcern = new HashSet<File>(appClassPath);
//        for (String className: classesToRewrite) {
//            if (!classesToRemove.contains(className)) {
//                try {
//                    ClassFileUtils.writeClass(nameToSootClass.get(className), classPathsOfConcern);
//                } catch (Exception e) {
//                    System.out.println(e);
//                }
//            }
//        }
//        for (String className: classesToRemove) {
//            try {
//                ClassFileUtils.removeClass(nameToSootClass.get(className), classPathsOfConcern);
//            } catch (Exception e) {
//                System.out.println(e);
//            }
//        }

        for (String n: ccAnalysis.getNameChangeList().keySet()) {
            System.out.println("name change: " + n + " " + ccAnalysis.getNameChangeList().get(n));
        }

        int originCount = cgAnalysis.getAppClasses().size();
        int afterCount = originCount - ccAnalysis.getNameChangeList().size();
        float percentage = (originCount - afterCount) / (float) originCount;
        System.out.printf("Original class count: %d, After class count: %d, percentage: %f\n", originCount, afterCount, percentage);
    }
}
