package edu.ucla.cs.jshrinklib.reachability;

import edu.ucla.cs.jshrinklib.util.EntryPointUtil;

import java.util.HashSet;
import java.util.Set;

//TODO: I think this could be merged with "EntryPointUtil"

public class EntryPointProcessor {
    private final boolean mainEntry;
    private final boolean publicEntry;
    private final boolean testEntry;

    //As Soot does not properly process Lambda expressions, we have the option to set them as entry points
    private final boolean lambdaExpression;

    private Set<MethodData> customEntry;

    // cache the entry points to avoid re-computation
    private Set<MethodData> entryPoints;

    public EntryPointProcessor(boolean setMainEntry, boolean setPublicEntry,
                               boolean setTestEntry, boolean setLambdaExpression, Set<MethodData> setCustomEntry){
        this.mainEntry = setMainEntry;
        this.publicEntry = setPublicEntry;
        this.testEntry = setTestEntry;
        this.lambdaExpression = setLambdaExpression;
        this.customEntry = setCustomEntry;
        this.entryPoints = new HashSet<MethodData>();
    }

    public Set<MethodData> getEntryPoints(Set<MethodData> appMethods, Set<MethodData> testMethods){
        if(this.mainEntry){
            entryPoints.addAll(EntryPointUtil.getMainMethodsAsEntryPoints(appMethods));
            if(this.lambdaExpression){
                entryPoints.addAll(EntryPointUtil.getLambdaExpressionsAsEntryPoints(appMethods));
            }
        }

        if(this.publicEntry){
            entryPoints.addAll(EntryPointUtil.getPublicMethodsAsEntryPoints(appMethods));
            if(this.lambdaExpression){
                entryPoints.addAll(EntryPointUtil.getLambdaExpressionsAsEntryPoints(appMethods));
            }
        }

        if(this.testEntry){
            entryPoints.addAll(EntryPointUtil.getTestMethodsAsEntryPoints(testMethods));
            if(this.lambdaExpression){
                entryPoints.addAll(EntryPointUtil.getLambdaExpressionsAsEntryPoints(testMethods));
            }
        }

        entryPoints.addAll(customEntry);

        return entryPoints;
    }

    public Set<MethodData> getEntryPoints() {
        return entryPoints;
    }
}
