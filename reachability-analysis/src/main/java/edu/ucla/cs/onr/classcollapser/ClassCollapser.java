package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.reachability.CallGraphAnalysis;
import edu.ucla.cs.onr.reachability.EntryPointProcessor;
import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.MethodBodyUtils;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.invoke.SiteInliner;

import java.io.File;
import java.util.*;

public class ClassCollapser implements IClassCollapser {

	@Override
	public void collapseClasses(List<File> libClassPath,
                                List<File> appClassPath,
                                Queue<ArrayList<String>> collapseList,
                                Map<String, String> nameChangeList) {
		
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
            if (method.getName().equals("<init>")) {
                Stmt toInLine = null;
                SootMethod inlinee = null;
                Body b = method.retrieveActiveBody();
                for (Unit u : b.getUnits()) {
                    if (u instanceof InvokeStmt) {
                        InvokeExpr expr = ((InvokeStmt)u).getInvokeExpr();
                        SootMethod m = expr.getMethod();
                        if (m.getName().equals(method.getName()) && m.getDeclaringClass().getName().equals(to.getName())) {
                            toInLine = (InvokeStmt) u;
                            inlinee = m;
                        }
                    }
                }
                if (inlinee == null || toInLine == null) {
                    continue;
                }
                inlinee.retrieveActiveBody();
                SiteInliner.inlineSite(inlinee, toInLine, method);
            }
            if (originalMethods.containsKey(method.getName())) {
                to.getMethods().remove(originalMethods.get(method.getName()));
            }
            to.getMethods().add(method);

        }
    }

    /**
     * Changes class names in the body of all methods of a class
     * @param c The class in which we are modifying the bodies
     * @param changeFrom The original name of the class to be changed
     * @param changeTo The new name of the class to be changed
     */
    public static boolean changeClassNamesInClass(SootClass c, SootClass changeFrom, SootClass changeTo) {
        assert c != changeFrom && c != changeTo;
        boolean changed = false;
        for (SootField f: c.getFields()) {
            if (f.getType() == Scene.v().getType(changeFrom.getName())) {
                f.setType(Scene.v().getType(changeTo.getName()));
                changed = true;
            }
        }
        for (SootMethod m: c.getMethods()) {
            changed = changed || changeClassNamesInMethod(m, changeFrom, changeTo);
        }
        return changed;
    }

    private static boolean changeClassNamesInMethod(SootMethod m, SootClass changeFrom, SootClass changeTo) {
        boolean changed = false;
        Body b = m.retrieveActiveBody();
        for (Local l: b.getLocals()) {
            if (l.getType() == Scene.v().getType(changeFrom.getName())) {
                l.setType(Scene.v().getType(changeTo.getName()));
                changed = true;
            }
        }
        for (Unit u : m.retrieveActiveBody().getUnits()) {
            if (u instanceof InvokeStmt) {
                InvokeExpr expr = ((InvokeStmt) u).getInvokeExpr();
                SootMethodRef originalMethodRef = expr.getMethodRef();
                if (originalMethodRef.declaringClass().getName().equals(changeFrom.getName())) {
                    expr.setMethodRef(Scene.v().makeMethodRef(changeTo, originalMethodRef.name(),
                            originalMethodRef.parameterTypes(),
                            originalMethodRef.returnType(),
                            originalMethodRef.isStatic()));
                    ((InvokeStmt) u).setInvokeExpr(expr);
                    changed = true;
                }
            } else if (u instanceof DefinitionStmt) {
                Value rightOp = ((DefinitionStmt) u).getRightOp();
                if (rightOp instanceof NewExpr && rightOp.getType() == Scene.v().getType(changeFrom.getName())) {
                    ((NewExpr) rightOp).setBaseType((RefType)Scene.v().getType(changeTo.getName()));
                    changed = true;
                }
            }
        }
        return changed;
    }
}
