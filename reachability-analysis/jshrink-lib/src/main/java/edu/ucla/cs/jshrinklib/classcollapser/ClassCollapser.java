package edu.ucla.cs.jshrinklib.classcollapser;

import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.util.SootUtils;
import fj.P;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceOfExpr;
import soot.jimple.toolkits.invoke.SiteInliner;

import java.util.*;

public class ClassCollapser {

    private final Set<String> classesToRewrite;
    private final Set<String> classesToRemove;
    private final Set<MethodData> removedMethods;


    public ClassCollapser() {
        this.classesToRemove = new HashSet<String>();
        this.classesToRewrite = new HashSet<String>();
        this.removedMethods = new HashSet<MethodData>();
    }

    public void run(ClassCollapserAnalysis classCollapserAnalysis, Set<String> testClasses){
        HashMap<String, SootClass> nameToSootClass = new HashMap<String, SootClass>();

        for (ArrayList<String> collapse: classCollapserAnalysis.getCollapseList()) {
            String fromName = collapse.get(0);
            String toName = collapse.get(1);
            if (!nameToSootClass.containsKey(fromName)) {
                nameToSootClass.put(fromName, Scene.v().loadClassAndSupport(fromName));
            }
            if (!nameToSootClass.containsKey(toName)) {
                nameToSootClass.put(toName, Scene.v().loadClassAndSupport(toName));
            }
            SootClass from = nameToSootClass.get(fromName);
            SootClass to = nameToSootClass.get(toName);

            Set<MethodData> removedMethods =
                    ClassCollapser.mergeTwoClasses(from, to, ((ClassCollapserAnalysis) classCollapserAnalysis).getProcessedUsedMethods());

            this.removedMethods.addAll(removedMethods);

            this.classesToRewrite.add(to.getName());
            this.classesToRemove.add(from.getName());
        }

        Set<String> allClasses = new HashSet<String>();
        allClasses.addAll(classCollapserAnalysis.appClasses);
        allClasses.addAll(testClasses);

        Map<String, String> nameChangeList = classCollapserAnalysis.getNameChangeList();
        for(String fromName: nameChangeList.keySet()) {
            String toName = nameChangeList.get(fromName);
            if (!nameToSootClass.containsKey(fromName)) {
                nameToSootClass.put(fromName, Scene.v().loadClassAndSupport(fromName));
            }
            if (!nameToSootClass.containsKey(toName)) {
                nameToSootClass.put(toName, Scene.v().loadClassAndSupport(toName));
            }
            SootClass from = nameToSootClass.get(fromName);
            SootClass to = nameToSootClass.get(toName);
            for (String className : allClasses) {
                if(className.equals(fromName)) {
                    // no need to handle the collapsed class, since this class will be removed at the end
                    continue;
                }

                if(className.equals("org.junit.internal.runners.ErrorReportingRunner")) {
                    System.out.println("caught you!");
                }

                if (!nameToSootClass.containsKey(className)) {
                    nameToSootClass.put(className, Scene.v().loadClassAndSupport(className));
                }
                SootClass sootClass = nameToSootClass.get(className);
                if (ClassCollapser.changeClassNamesInClass(sootClass, from, to)) {
                    classesToRewrite.add(sootClass.getName());
                }
            }
        }
    }

    /**
     * Merges one soot class into another
     * @param from The class that will be merged from, and discarded (the super class)
     * @param to The class that will be merged into, and kept (the sub class)
     * @return The set of methods that have been removed
     */
    /*package*/ static Set<MethodData> mergeTwoClasses(SootClass from, SootClass to, Map<String, Set<String>> usedMethods) {
        Set<MethodData> toReturn = new HashSet<MethodData>();
        HashMap<String, SootField> originalFields = new HashMap<String, SootField>();
        for (SootField field : to.getFields()) {
            originalFields.put(field.getName(), field);
        }
        // reset modifiers
        to.setModifiers(from.getModifiers());

        // find fields that are used in super class constructors
        HashSet<String> fieldsUsedInConstructor = new HashSet<String>();
        for(SootMethod method : to.getMethods()) {
            if(method.getName().equals("<init>")) {
                Body b = method.retrieveActiveBody();
                for(Unit u : b.getUnits()) {
                    Stmt s = (Stmt) u;
                    if(!s.containsFieldRef()) {
                        continue;
                    }
                    FieldRef fr = s.getFieldRef();
                    fieldsUsedInConstructor.add(fr.getField().getName());
                }
            }
        }

        HashSet<String> renamedFields = new HashSet<String>();
        for (SootField field : from.getFields()) {
            String fieldName = field.getName();
            if (originalFields.containsKey(fieldName)) {
                // overridden field
                if(fieldsUsedInConstructor.contains(fieldName)) {
                    // must keep this field and rename
                    renamedFields.add(fieldName);
                    originalFields.get(fieldName).setName("super" + fieldName);
                } else {
                    // safely remove
                    to.getFields().remove(originalFields.get(fieldName));
                }
            }
            // reset the declaring class
            field.setDeclaringClass(to);
            to.getFields().addLast(field);
        }

        HashMap<String, SootMethod> originalMethods = new HashMap<String, SootMethod>();
        for (SootMethod method : to.getMethods()) {
            originalMethods.put(method.getSubSignature(), method);
        }
        HashSet<SootMethod> methodsToMove = new HashSet<SootMethod>();
        for (SootMethod method : from.getMethods()) {
            // find the super constructor calls in a constructor of a subclass
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
                Body b = inlinee.retrieveActiveBody();
                // update the references to the renamed fields before inlining
                for(Unit u : b.getUnits()) {
                   Stmt s = (Stmt) u;
                   if(!s.containsFieldRef()) {
                       continue;
                   }

                   FieldRef fr = s.getFieldRef();
                   SootFieldRef sfr = fr.getFieldRef();
                   if(renamedFields.contains(sfr.name())) {
                       // the original field has been renamed
                       AbstractSootFieldRef new_sfr = new AbstractSootFieldRef(sfr.declaringClass(),
                               "super" + sfr.name(), sfr.type(), sfr.isStatic());
                       fr.setFieldRef(new_sfr);
                    }
                }

                // inline the constructor
                SiteInliner.inlineSite(inlinee, toInLine, method);
                if (originalMethods.containsKey(method.getSubSignature())) {
                    to.getMethods().remove(originalMethods.get(method.getSubSignature()));
                }
                // remove the constructor in the super class since now it is inlined
                to.removeMethod(inlinee);
                // add this method to the methodsToMove list
                methodsToMove.add(method);
            } else {
                if (!originalMethods.containsKey(method.getSubSignature())) {
                    // add this method to the methodsToMove list
                    methodsToMove.add(method);
                } else {
                    if (usedMethods.containsKey(from.getName()) && usedMethods.get(from.getName()).contains(method.getSubSignature())) {
                        to.removeMethod(originalMethods.get(method.getSubSignature()));
                        // add this method to the methodsToMove list
                        methodsToMove.add(method);
                    }
                }
            }
        }

        // move methods from the subclass to the superclass
        for(SootMethod m : methodsToMove) {
            toReturn.add(SootUtils.sootMethodToMethodData(m));
            from.removeMethod(m);
            to.addMethod(m);
        }

        return toReturn;
    }

    /**
     * Changes class names in the body of all methods of a class (Legacy soot approach)
     * @param c The class in which we are modifying the bodies
     * @param changeFrom The original name of the class to be changed
     * @param changeTo The new name of the class to be changed
    **/
    /*package*/ static boolean changeClassNamesInClass(SootClass c, SootClass changeFrom, SootClass changeTo) {
        assert c != changeFrom;

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
        List<SootMethod> sootMethods = c.getMethods();
        for (int i = 0; i < sootMethods.size(); i++) {
            SootMethod m = sootMethods.get(i);
            boolean changed2 = changeClassNamesInMethod(m, changeFrom, changeTo);
            // do not inline change2 since Java do short circuit evaluation
            // we still want to make sure type references in each method body is updated correctly
            changed = changed || changed2;
        }

        return changed;
    }


    //Supporting method for changeClassNameInClass
    private static boolean changeClassNamesInMethod(SootMethod m, SootClass changeFrom, SootClass changeTo) {
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

        if (!m.isAbstract() && !m.isNative()) {
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
                        continue;
                    }
                } else if (u instanceof DefinitionStmt) {
                    Value rightOp = ((DefinitionStmt) u).getRightOp();

                    if (rightOp instanceof NewExpr && rightOp.getType() == Scene.v().getType(changeFrom.getName())) {
                        ((NewExpr) rightOp).setBaseType((RefType) Scene.v().getType(changeTo.getName()));
                        changed = true;
                    } else if (rightOp instanceof JCastExpr) {
                        JCastExpr expr = (JCastExpr) rightOp;
                        if (expr.getType() == Scene.v().getType(changeFrom.getName())) {
                            expr.setCastType(Scene.v().getType(changeTo.getName()));
                            changed = true;
                        }
                    } else if (rightOp instanceof InvokeExpr) {
                        InvokeExpr expr = (InvokeExpr) rightOp;
                        SootMethodRef originalMethodRef = expr.getMethodRef();
                        if (originalMethodRef.declaringClass().getName().equals(changeFrom.getName())) {
                            expr.setMethodRef(Scene.v().makeMethodRef(changeTo, originalMethodRef.name(),
                                    originalMethodRef.parameterTypes(),
                                    originalMethodRef.returnType(),
                                    originalMethodRef.isStatic()));
                            changed = true;
                        }
                    } else if (rightOp instanceof JInstanceOfExpr) {
                        JInstanceOfExpr expr = (JInstanceOfExpr) rightOp;
                        if(expr.getCheckType() == Scene.v().getType(changeFrom.getName())) {
                            expr.setCheckType(Scene.v().getType(changeTo.getName()));
                            changed = true;
                        }
                    }

                    // handle field references
                    if(u instanceof JIdentityStmt) {
                        JIdentityStmt stmt = (JIdentityStmt) u;
                        if(rightOp instanceof ParameterRef && rightOp.getType() == Scene.v().getType(changeFrom.getName())) {
                            ParameterRef oldRef = (ParameterRef) rightOp;
                            ParameterRef newRef = new ParameterRef(Scene.v().getType(changeTo.getName()), oldRef.getIndex());
                            stmt.setRightOp(newRef);
                            changed = true;
                            continue;
                        } else if (rightOp instanceof ThisRef && rightOp.getType() == Scene.v().getType(changeFrom.getName())) {
                            ThisRef newRef = new ThisRef(RefType.v(changeTo.getName()));
                            stmt.setRightOp(newRef);
                            changed = true;
                            continue;
                        }
                    }  else if (u instanceof JAssignStmt) {
                        JAssignStmt stmt = (JAssignStmt) u;

                        if (stmt.containsFieldRef()) {
                            FieldRef fr = stmt.getFieldRef();
                            if (fr instanceof InstanceFieldRef) {
                                InstanceFieldRef v = (InstanceFieldRef) fr;
                                if (v.getType().toString().equals(changeFrom.getName())) {
                                    // the referenced field is in the type of a removed class/interface
                                    SootFieldRef oldFieldRef = v.getFieldRef();
                                    AbstractSootFieldRef newFieldRef =
                                            new AbstractSootFieldRef(oldFieldRef.declaringClass(), oldFieldRef.name(),
                                                    Scene.v().getType(changeTo.getName()), oldFieldRef.isStatic());
                                    v.setFieldRef(newFieldRef);
                                    changed = true;
                                    continue;
                                } else if (fr.getFieldRef().declaringClass().getName().equals(changeFrom.getName())) {
                                    // use a field from the collapsed class
                                    // reset the declaring class of this field reference to the collapse-to class
                                    SootFieldRef oldFieldRef = v.getFieldRef();
                                    AbstractSootFieldRef newFieldRef =
                                            new AbstractSootFieldRef(changeTo, oldFieldRef.name(),
                                                    oldFieldRef.type(), oldFieldRef.isStatic());
                                    v.setFieldRef(newFieldRef);
                                    changed = true;
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }
        return changed;
    }

    public ClassCollapserData getClassCollapserData(){
        return new ClassCollapserData(this.removedMethods, this.classesToRemove, this.classesToRewrite);
    }
}
