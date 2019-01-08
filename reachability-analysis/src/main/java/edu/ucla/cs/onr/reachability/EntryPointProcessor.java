package edu.ucla.cs.onr.reachability;

import edu.ucla.cs.onr.util.EntryPointUtil;

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

    public EntryPointProcessor(boolean setMainEntry, boolean setPublicEntry,
                               boolean setTestEntry, boolean setLambdaExpression, Set<MethodData> setCustomEntry){
        this.mainEntry = setMainEntry;
        this.publicEntry = setPublicEntry;
        this.testEntry = setTestEntry;
        this.lambdaExpression = setLambdaExpression;
        this.customEntry = setCustomEntry;
    }

    public Set<MethodData> getEntryPoints(Set<MethodData> appMethods, Set<MethodData> libMethods,
                                          Set<MethodData> testMethods){
        Set<MethodData> toReturn = new HashSet<MethodData>();

        if(this.mainEntry){
            toReturn.addAll(EntryPointUtil.getMainMethodsAsEntryPoints(appMethods));
            if(this.lambdaExpression){
                toReturn.addAll(EntryPointUtil.getLambdaExpressionsAsEntryPoints(appMethods));
            }
        }

        if(this.publicEntry){
            toReturn.addAll(EntryPointUtil.getPublicMethodsAsEntryPoints(appMethods));
            if(this.lambdaExpression){
                toReturn.addAll(EntryPointUtil.getLambdaExpressionsAsEntryPoints(appMethods));
            }
        }

        if(this.testEntry){
            toReturn.addAll(EntryPointUtil.getTestMethodsAsEntryPoints(testMethods));
            if(this.lambdaExpression){
                toReturn.addAll(EntryPointUtil.getLambdaExpressionsAsEntryPoints(testMethods));
            }
        }

        toReturn.addAll(customEntry);

        return toReturn;
    }
}
