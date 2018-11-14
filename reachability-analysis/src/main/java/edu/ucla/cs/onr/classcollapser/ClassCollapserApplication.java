package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.reachability.CallGraphAnalysis;
import edu.ucla.cs.onr.reachability.EntryPointProcessor;
import edu.ucla.cs.onr.reachability.IProjectAnalyser;
import edu.ucla.cs.onr.reachability.MethodData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ClassCollapserApplication {

    public static void main(String[] args) {
        ArrayList<File> appClassPath = new ArrayList<File>();
        File f = new File("/Users/zonghengma/Documents/UCLA/capstone_new/call-graph-analysis/reachability-analysis/src/test/resources/classcollapser/override/original");
        System.out.println(f.listFiles().length);
        System.out.println(f.exists());
        System.out.println(f.isDirectory());
        appClassPath.add(f);
        IProjectAnalyser cgAnalysis = new CallGraphAnalysis(new ArrayList<File>(), appClassPath, new ArrayList<File>(), new EntryPointProcessor(true, false, false, new HashSet<MethodData>()));
        cgAnalysis.setup();
        cgAnalysis.run();
        Set<String> usedAppClasses = cgAnalysis.getUsedAppClasses();
        for (String c : usedAppClasses) {
            System.out.println(c);
        }
        Set<String> usedClasses = new HashSet<String>();
        usedClasses.add("B");
        usedClasses.add("main");
        IClassCollapserAnalyser ccAnalysis = new ClassCollapserAnalysis(cgAnalysis.getAppClasses(), usedClasses);
        ccAnalysis.run();
//        for (ArrayList<String> collpase: ccAnalysis.getCollapseList()) {
//            System.out.printf("merge %s into %s\n", collpase.get(0), collpase.get(1));
//        }
        //TODO: call ClassCollapser.collapseClasses with the results from ClassCollapserAnalysis
    }
}
