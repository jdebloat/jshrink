package edu.ucla.cs.jshrinklib.reachability;

import java.util.*;

public class ClassReferenceGraph {
    private HashMap<String, Set<String>> graph=null;
    public ClassReferenceGraph(){
        graph = new HashMap<>();
    }

    public void addClass(String className, String classPath){
        Set<String> references, refererrs;
        className = className.replaceAll("/",".");
        try{
            references = ConstantPoolScanner.getClassReferences(classPath);
            references.remove(className);
            for(String ref: references){
                refererrs = graph.getOrDefault(ref, new HashSet<String>());
                refererrs.add(className);
                graph.put(ref, refererrs);
            }
        }
        catch (Exception e) {
            System.err.println("An an exception was thrown while getting references for Class "+className+" at "+classPath);
            e.printStackTrace();
            //System.exit(1);
        }
    }

    public void addAll(HashMap<String, String> classNamePathMap) {
        Set<String> references, refererrs;
        for(String clazz: classNamePathMap.keySet()){
            this.addClass(clazz, classNamePathMap.get(clazz));
        }
    }
    public Set<String> getReferences(String className){
        Set<String> references = new HashSet<String>();
        for(Map.Entry<String, Set<String>> e:graph.entrySet()){
            if(e.getValue().contains(className))
                references.add(e.getKey());
        }
        return references;
    }

    public Set<String> getReferredBy(String className){
        return graph.get(className);
    }

    public Set<String> getNodes(){
        return graph.keySet();
    }
}
