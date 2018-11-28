package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.util.MethodBodyUtils;
import soot.Scene;
import soot.SootClass;

import edu.ucla.cs.onr.reachability.MethodData;
import soot.SootMethod;


import java.lang.reflect.Method;
import java.util.*;

public class ClassCollapserAnalysis implements IClassCollapserAnalyser {
    private Set<String> appClasses;
    private Set<String> usedAppClasses;
    private Map<String, Set<String>> usedAppMethods;
    private Set<String> processableLeaves;
    private LinkedList<ArrayList<String>> collapseList;
    private Set<String> removeList;
    private Map<String, String> nameChangeList;

    private Map<String, String> parentsMap;
    private Map<String, Set<String>> childrenMap;
    private Map<String, Set<String>> parentsVirtualMap;
    private Map<String, Set<String>> childrenVirtualMap;
    private Map<String, SootClass> appClassMap;

    public ClassCollapserAnalysis(Set<String> appCls,
                                  Set<String> usedAppCls,
                                  Set<MethodData> usedAppMethodData) {

        appClasses = appCls;
        usedAppClasses = new HashSet<String>(usedAppCls);  //We're modifying used appClasses in this analysis, therefore copy here
        parentsMap = new HashMap<String, String>();
        childrenMap = new HashMap<String, Set<String>>();
        parentsVirtualMap  = new HashMap<String, Set<String>>();
        childrenVirtualMap = new HashMap<String, Set<String>>();
        processableLeaves = new HashSet<String>();
        collapseList = new LinkedList<ArrayList<String>>();
        nameChangeList = new HashMap<String, String>();
        removeList = new HashSet<String>();
        appClassMap = new HashMap<String, SootClass> ();

        usedAppMethods = new HashMap<String, Set<String>>();
        for (MethodData m: usedAppMethodData) {
            String className = m.getClassName();
            if (!usedAppMethods.containsKey(className)) {
                usedAppMethods.put(className, new HashSet<String>());
            }
            usedAppMethods.get(className).add(m.getSubSignature());
        }
        System.out.println(usedAppMethods);
    }

    public void run() {
        setup();
//        System.out.println(childrenMap);
//        System.out.println(childrenVirtualMap);
//        System.out.println(parentsMap);
//        System.out.println(parentsVirtualMap);
        LinkedList<String> queue = new LinkedList<String>();
        HashSet<String> visited = new HashSet<String>();
        for (String leaf: processableLeaves) {
            queue.addLast(leaf);
            visited.add(leaf);
        }
        while (!queue.isEmpty()) {
            String child = queue.removeFirst();
            System.out.printf("prcessing child class %s\n", child);
            Set<String> parents = new HashSet<String>();
            if (!parentsMap.get(child).isEmpty()) {
                parents.add(parentsMap.get(child));
            }
            for (String p: parentsVirtualMap.get(child)) {
                parents.add(p);
            }

            if (usedAppClasses.contains(child) && parents.size() == 1) {
                String singleParent = parents.iterator().next();
                for (String c: childrenMap.get(singleParent)) {
                    if (!visited.contains(c)) {
                        queue.addLast(child);
                        continue;
                    }
                }
                for (String c: childrenVirtualMap.get(singleParent)) {
                    if (!visited.contains(c)) {
                        queue.addLast(child);
                        continue;
                    }
                }
                if (collapsable(child, singleParent)) {
//                    String parent = parentsMap.get(child);
//                    ClassCollapser.mergeTwoClasses(appClassMap.get(child), appClassMap.get(parent));
                    ArrayList<String> collapse = new ArrayList<String>();
                    collapse.add(child);
                    collapse.add(singleParent);
                    collapseList.addLast(collapse);
                    nameChangeList.put(child, singleParent);
                    usedAppClasses.add(singleParent);
                    removeList.add(child);
//                    queue.add(parent);
                }
            }
            for (String parent: parents) {
                if (childrenMap.get(parent).contains(child)) {
                    childrenMap.get(parent).remove(child);
                }
                if (childrenVirtualMap.get(parent).contains(child)) {
                    childrenVirtualMap.get(parent).remove(child);
                }
                parentsMap.remove(child);
                parentsVirtualMap.remove(child);
//                System.out.printf("parent: %s, children of the parent: %s\n", parent, childrenMap.get(parent));
                if (childrenMap.get(parent).size() == 0 && childrenVirtualMap.get(parent).size() == 0 && !visited.contains(parent)) {
                    queue.addLast(parent);
                    visited.add(parent);
                }
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
        System.out.printf("collapsable: from %s, to %s\n", from, to);
        SootClass fromClass = Scene.v().loadClassAndSupport(from);
        int numUsedChildren = 0;
        for (String child: childrenMap.get(to)) {
            if (usedAppClasses.contains(child)) {
                numUsedChildren += 1;
            }
        }
        for (String child: childrenVirtualMap.get(to)) {
            if (usedAppClasses.contains(child)) {
                numUsedChildren += 1;
            }
        }
        if (numUsedChildren == 1) {
            if (!usedAppClasses.contains(to)) {
                return true;
            }
            SootClass toClass = Scene.v().loadClassAndSupport(to);
            for (SootMethod m: fromClass.getMethods()) {
//                if (m.getName().equals("<init>") && !MethodBodyUtils.isEmptyConstructor(m)) {
//                    return false;
//                }
                System.out.printf("method name: %s, declare: %s, used: %s\n", m.getName(), toClass.declaresMethod(m.getSubSignature()), toClass.declaresMethod(m.getSubSignature())&& usedAppMethods.containsKey(toClass.getName())
                        && usedAppMethods.get(toClass.getName()).contains(m.getSubSignature()));
                System.out.println(toClass.getMethod(m.getSubSignature()).getSignature());
                if (toClass.declaresMethod(m.getSubSignature())
                        && usedAppMethods.containsKey(toClass.getName())
                        && usedAppMethods.get(toClass.getName()).contains(m.getSubSignature())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    private void initLeaves() {
        for (String parent: childrenMap.keySet()) {
            if (childrenMap.get(parent).size() == 0 && childrenVirtualMap.get(parent).size() == 0) {
                processableLeaves.add(parent);
            }
        }
//        LinkedList<String> queue = new LinkedList<String>();
//        for (String leaf: processableLeaves) {
//            queue.addLast(leaf);
//        }
//        while (!queue.isEmpty()) {
//            String leaf = queue.removeFirst();
//            if (!usedAppClasses.contains(leaf)) {
////                classesToRemove.add(leaf);
//                processableLeaves.remove(leaf);
//                for (String s: parentsMap.get(leaf)) {
//                    childrenMap.get(s).remove(leaf);
//                    if (childrenMap.get(s).size() == 0) {
//                        processableLeaves.add(s);
//                        queue.addLast(s);
//                    }
//                    parentsMap.remove(leaf);
//                }
//            }
//        }
    }

    private void initClassHierarchy() {
        Set<String> visited = new HashSet<String>();

        for (String c: appClasses) {
            parentsMap.put(c, "");
            childrenMap.put(c, new HashSet<String>());
            parentsVirtualMap.put(c, new HashSet<String>());
            childrenVirtualMap.put(c, new HashSet<String>());
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
            parentsMap.put(thisClass, sootClass.getSuperclass().getName());
            childrenMap.get(sootClass.getSuperclass().getName()).add(thisClass);
        }
        for (SootClass c : sootClass.getInterfaces()) {
            parentsVirtualMap.get(thisClass).add(c.getName());
            childrenVirtualMap.get(c.getName()).add(thisClass);
        }
    }

    public Queue<ArrayList<String>> getCollapseList() {
        return collapseList;
    }

    public Map<String, String> getNameChangeList() {
        return nameChangeList;
    }

    public Set<String> getRemoveList() {
        return removeList;
    }
}
