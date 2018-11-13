package edu.ucla.cs.onr.classcollapser;

import fj.P;
import soot.Scene;
import soot.SootClass;

import java.util.*;

public class ClassCollapserAnalysis implements IClassCollapserAnalyser {
    private Set<String> appClasses;
    private Set<String> usedAppClasses;
    private Set<String> processableLeaves;
    private LinkedList<ArrayList<String>> collapseList;
    private Map<String, String> nameChangeList;

    private Map<String, Set<String>> parentsMap;
    private Map<String, Set<String>> childrenMap;
    private Map<String, SootClass> appClassMap;

    public ClassCollapserAnalysis(Set<String> appCls,
                                  Set<String> usedAppCls) {

        appClasses = appCls;
        usedAppClasses = usedAppCls;
        parentsMap = new HashMap<String, Set<String>>();
        childrenMap = new HashMap<String, Set<String>>();
        processableLeaves = new HashSet<String>();
        collapseList = new LinkedList<ArrayList<String>>();
        nameChangeList = new HashMap<String, String>();
        appClassMap = new HashMap<String, SootClass> ();
    }

    public void run() {
        setup();
        LinkedList<String> queue = new LinkedList<String>();
        for (String leaf: processableLeaves) {
            queue.addLast(leaf);
        }
        while (!queue.isEmpty()) {
            String child = queue.removeFirst();
            if (parentsMap.get(child).size() == 1 && collapsable(child, parentsMap.get(child).iterator().next())) {
                String parent =  parentsMap.get(child).iterator().next();
                ClassCollapser.mergeTwoClasses(appClassMap.get(child), appClassMap.get(parent));
                ArrayList<String> collapse = new ArrayList<String>();
                collapse.add(child);
                collapse.add(parent);
                collapseList.addLast(collapse);
                nameChangeList.put(child, parent);
                queue.add(parent);
            }
        }
        postprocess();
    }

    private void setup() {
        initClassHierarchy();
        initLeaves();
    }

    private void postprocess() {
        List<String> keys = new ArrayList<String>(nameChangeList.keySet());
        for (int i = 0; i < keys.size(); ++i) {
            String key = keys.get(i);
            boolean changed = nameChangeList.containsKey(key);
            while (changed) {
                if (nameChangeList.containsKey(nameChangeList.get(key))) {
                    nameChangeList.put(key, nameChangeList.get(nameChangeList.get(key)));
                } else {
                    changed = false;
                }
            }
        }
    }

    private boolean collapsable(String from, String to) {
        return childrenMap.get(to).size() == 1;
    }

    private void initLeaves() {
        for (String parent: childrenMap.keySet()) {
            if (childrenMap.get(parent).size() == 0) {
                processableLeaves.add(parent);
            }
        }
        LinkedList<String> queue = new LinkedList<String>();
        for (String leaf: processableLeaves) {
            queue.addLast(leaf);
        }
        while (!queue.isEmpty()) {
            String leaf = queue.removeFirst();
            if (!usedAppClasses.contains(leaf)) {
//                classesToRemove.add(leaf);
                processableLeaves.remove(leaf);
                for (String s: parentsMap.get(leaf)) {
                    childrenMap.get(s).remove(leaf);
                    if (childrenMap.get(s).size() == 0) {
                        processableLeaves.add(s);
                        queue.addLast(s);
                    }
                    parentsMap.remove(leaf);
                }
            }
        }
    }

    private void initClassHierarchy() {
        Set<String> visited = new HashSet<String>();

        for (String c: appClasses) {
            parentsMap.put(c, new HashSet<String>());
            childrenMap.put(c, new HashSet<String>());
        }

        for (String c: appClasses) {
            initOneClass(c, visited);
        }
    }

    private void initOneClass(String thisClass, Set<String> visited) {
        if (visited.contains(thisClass)) {
            return;
        }
        visited.add(thisClass);

        SootClass sootClass = Scene.v().loadClassAndSupport(thisClass);
        appClassMap.put(thisClass, sootClass);
        if (sootClass.hasSuperclass() && childrenMap.containsKey(sootClass.getSuperclass().getName())) {
            parentsMap.get(thisClass).add(sootClass.getSuperclass().getName());
            childrenMap.get(sootClass.getSuperclass().getName()).add(thisClass);
        }
        for (SootClass c : sootClass.getInterfaces()) {
            parentsMap.get(thisClass).add(c.getName());
            childrenMap.get(c.getName()).add(thisClass);
        }
    }

    public Queue<ArrayList<String>> getCollapseList() {
        return collapseList;
    }

    public Map<String, String> getNameChangeList() {
        return nameChangeList;
    }
}
