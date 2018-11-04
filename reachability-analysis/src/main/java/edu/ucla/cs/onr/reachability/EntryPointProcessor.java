package edu.ucla.cs.onr.reachability;

import edu.ucla.cs.onr.util.EntryPointUtil;

import java.util.HashSet;
import java.util.Set;

//TODO: I think this could be merged with "EntryPointUtil"

public class EntryPointProcessor {
    private final boolean mainEntry;
    private final boolean publicEntry;
    private final boolean testEntry;
    private Set<MethodData> customEntry;

    public EntryPointProcessor(boolean setMainEntry, boolean setPublicEntry,
                               boolean setTestEntry, Set<MethodData> setCustomEntry){
        this.mainEntry = setMainEntry;
        this.publicEntry = setPublicEntry;
        this.testEntry = setTestEntry;
        this.customEntry = setCustomEntry;
    }

    public Set<MethodData> getEntryPoints(Set<MethodData> appMethods, Set<MethodData> libMethods,
                                          Set<MethodData> testMethods){
        Set<MethodData> toReturn = new HashSet<MethodData>();

        if(this.mainEntry){
            toReturn.addAll(EntryPointUtil.getMainMethodsAsEntryPoints(appMethods));
        }

        if(this.publicEntry){
            toReturn.addAll(EntryPointUtil.getPublicMethodsAsEntryPoints(appMethods));
        }

        if(this.testEntry){
            toReturn.addAll(EntryPointUtil.getTestMethodsAsEntryPoints(testMethods));
        }

        toReturn.addAll(customEntry);

        return toReturn;
    }
}
