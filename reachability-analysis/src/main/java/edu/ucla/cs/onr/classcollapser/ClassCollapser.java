package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.reachability.CallGraphAnalysis;
import edu.ucla.cs.onr.reachability.EntryPointProcessor;
import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.MethodBodyUtils;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.invoke.SiteInliner;
import soot.util.EmptyChain;

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
     * @param from The class that will be merged from, and discarded (the super class)
     * @param to The class that will be merged into, and kept (the sub class)
     */
    public static void mergeTwoClasses(SootClass from, SootClass to, Map<String, Set<String>> usedMethods) {
        System.out.println(from.getMethodCount() + " " + to.getMethodCount());
        HashMap<String, SootField> originalFields = new HashMap<String, SootField>();
        for (SootField field : to.getFields()) {
            originalFields.put(field.getName(), field);
        }
        to.setModifiers(from.getModifiers());

        for (SootField field : from.getFields()) {
            if (originalFields.containsKey(field.getName())) {
                to.getFields().remove(originalFields.get(field.getName()));
            }
            to.getFields().add(field);
        }

        HashMap<String, SootMethod> originalMethods = new HashMap<String, SootMethod>();
        for (SootMethod method : to.getMethods()) {
            originalMethods.put(method.getSubSignature(), method);
        }
        for (SootMethod method : from.getMethods()) {
            Stmt toInLine = null;
            SootMethod inlinee = null;
            if (method.getName().equals("<init>")) {
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
            }
            if (inlinee != null && toInLine != null) {
                inlinee.retrieveActiveBody();
                SiteInliner.inlineSite(inlinee, toInLine, method);
                if (originalMethods.containsKey(method.getSubSignature())) {
                    to.getMethods().remove(originalMethods.get(method.getSubSignature()));
                }
                to.getMethods().add(method);
            } else {
                if (!originalMethods.containsKey(method.getSubSignature())) {
                    to.getMethods().add(method);
                } else {
                    if (usedMethods.containsKey(from.getName()) && usedMethods.get(from.getName()).contains(method.getSubSignature())) {
                        to.getMethods().remove(originalMethods.get(method.getSubSignature()));
                        to.getMethods().add(method);
                    }
                }
            }
        }
    }

    /**
     * Changes class names in the body of all methods of a class (Legacy soot approach)
     * @param c The class in which we are modifying the bodies
     * @param changeFrom The original name of the class to be changed
     * @param changeTo The new name of the class to be changed
     */
    @Deprecated
    public static boolean changeClassNamesInClass(SootClass c, SootClass changeFrom, SootClass changeTo) {
        assert c != changeFrom && c != changeTo;
        boolean changed = false;
        if (c.hasSuperclass() && c.getSuperclass().getName().equals(changeFrom.getName())) {
            c.setSuperclass(changeTo);
            changed = true;
        }
        if (c.getInterfaces().contains(changeFrom)) {
            c.removeInterface(changeFrom);
            c.addInterface(changeTo);
            changed = true;
        }
        for (SootField f: c.getFields()) {
            if (f.getType() == Scene.v().getType(changeFrom.getName())) {
                f.setType(Scene.v().getType(changeTo.getName()));
                changed = true;
            }
        }
        for (SootMethod m: c.getMethods()) {
            changed = changed || changeClassNamesInMethod(m, changeFrom, changeTo, c.isAbstract());
        }
        System.out.printf("change name in %s, from %s to %s, result %d\n", c.getName(), changeFrom.getName(), changeTo.getName(), changed ? 1:0);
        return changed;
    }

    //Supporting method for changeClassNameInClass
    @Deprecated
    private static boolean changeClassNamesInMethod(SootMethod m, SootClass changeFrom, SootClass changeTo, boolean isAbstract) {
        boolean changed = false;
        if (m.getReturnType() == Scene.v().getType(changeFrom.getName())) {
            m.setReturnType(Scene.v().getType(changeTo.getName()));
            changed = true;
        }
        List<Type> types = m.getParameterTypes();
        ArrayList<Type> newTypes = new ArrayList<Type>();
        boolean changeTypes = false;
        for (int i = 0; i < m.getParameterCount(); ++i) {
            if (types.get(i) ==  Scene.v().getType(changeFrom.getName())) {
                newTypes.add(Scene.v().getType(changeTo.getName()));
                changeTypes = true;
            } else {
                newTypes.add(types.get(i));
            }
        }
        if (changeTypes) {
            m.setParameterTypes(newTypes);
            changed = true;
        }

        boolean changeExceptions = false;
        ArrayList<SootClass> newExceptions = new ArrayList<SootClass>();
        for (SootClass e: m.getExceptions()) {
            if (e.getName().equals(changeFrom.getName())) {
                newExceptions.add(changeTo);
                changeExceptions = true;
            } else {
                newExceptions.add(e);
            }
        }
        if (changeExceptions) {
            m.setExceptions(newExceptions);
            changed = true;
        }

        if (!isAbstract) {
            Body b = m.retrieveActiveBody();
            for (Local l : b.getLocals()) {
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
                        ((NewExpr) rightOp).setBaseType((RefType) Scene.v().getType(changeTo.getName()));
                        changed = true;
                    }
                } else if (u instanceof ReturnStmt) {

                }
            }
        }
        return changed;
    }
}
