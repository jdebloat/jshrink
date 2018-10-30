package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.reachability.MethodData;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ClassCollapser implements IClassCollapser {

	@Override
	public void collapseClasses(List<File> libClassPath,
			List<File> appClassPath, Set<MethodData> touchedMethods,
			Set<MethodData> unTouchedMethods, Set<String> touchedClasses,
			Set<String> unTouchedClasses) {
		
	}

    /**
     * Merges one soot class into another
     * @param from The class that will be merged from, and discarded
     * @param to The class that will be merged into, and kept
     */
    public static void mergeTwoClasses(SootClass from, SootClass to) {
        HashMap<String, SootField> originalFields = new HashMap<String, SootField>();
        for (SootField field : to.getFields()) {
            originalFields.put(field.getName(), field);
        }
        for (SootField field : from.getFields()) {
            if (originalFields.containsKey(field.getName())) {
                to.getFields().remove(originalFields.get(field.getName()));
            }
            to.getFields().add(field);
        }

        HashMap<String, SootMethod> originalMethods = new HashMap<String, SootMethod>();
        //TODO: Assuming not method overloading for now
        for (SootMethod method : to.getMethods()) {
            originalMethods.put(method.getName(), method);
        }
        for (SootMethod method : from.getMethods()) {
            if (originalMethods.containsKey(method.getName())) {
                to.getMethods().remove(originalMethods.get(method.getName()));
            }
            to.getMethods().add(method);
        }
    }
}
