package edu.ucla.cs.onr.methodinliner;

import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.util.ClassFileUtils;
import soot.*;
import soot.baf.StaticInvokeInst;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.invoke.SiteInliner;

import java.io.File;
import java.util.*;

public class MethodInliner {


	/**
	 * This method will inline any methods called from a single location (the callee method is subsequently removed)
	 * @param map The call graph, represented as a map (Map<Callee, Set<Caller>>). Modified as the actual callgraph is
	 * @return The call graph elements inlined (Map<Callee, Set<Caller>>).
	 */
	public static InlineData inlineMethods(Map<SootMethod, Set<SootMethod>> map, Set<File> classpaths){

		InlineData toReturn = new InlineData();
		Set<SootMethod> methodsRemoved = new HashSet<SootMethod>();
		//For each method...


		for(Map.Entry<SootMethod, Set<SootMethod>> entry : map.entrySet()){
			if(entry.getValue().size() == 1) { //If only called by one other method...
				SootMethod caller = entry.getValue().iterator().next();
				SootMethod callee = entry.getKey();
				
				//Both the caller and callee classes must be within the current classpaths
				if (ClassFileUtils.getClassFile(callee.getDeclaringClass(), classpaths).isPresent()
					&& ClassFileUtils.getClassFile(caller.getDeclaringClass(),classpaths).isPresent()) {
					if (inline(caller, callee)) { //Inline the method if applicable.
						toReturn.addInlinedMethods(callee.getSignature(), caller.getSignature());
						toReturn.addClassModified(caller.getDeclaringClass());
						//Remove update our call graph information (I admit this is a bit inefficient but it's simple)
						for (Map.Entry<SootMethod, Set<SootMethod>> entry2 : map.entrySet()) {
							if (entry2.getValue().contains(callee)) {
								entry2.getValue().remove(callee);
								entry2.getValue().add(caller);
							}
						}
						//Remove from its class
						toReturn.addClassModified(callee.getDeclaringClass());
						SootClass calleeSootClass = callee.getDeclaringClass();
						//calleeSootClass.removeMethod(callee);
						calleeSootClass.getMethods().remove(callee);
						//callee.getDeclaringClass().removeMethod(callee);
						methodsRemoved.add(callee);
					}
				}
			}
		}
		for(SootMethod sootMethod : methodsRemoved){
			map.remove(sootMethod);
		}

		return toReturn;
	}

	private static boolean inline(SootMethod caller, SootMethod callee){
		/*
		We do not inline constructors (unless within a constructor in the same class). Doing so can cause problems with
		the MethodWiper component.
		 */
		if(callee.isConstructor()){
			if(!(caller.getDeclaringClass().equals(callee.getDeclaringClass()) && caller.isConstructor())) {
				return false;
			}
		}

		//ignore access methods (created by the compiler for inner classes)
		if(callee.getName().startsWith("access$") || caller.getName().startsWith("access$")){
			return false;
		}

		List<Stmt> toInline = new ArrayList<Stmt>();
		Body b = caller.retrieveActiveBody();

		for(Unit u : b.getUnits()){
			if(u instanceof InvokeStmt){
				InvokeExpr expr = ((InvokeStmt)u).getInvokeExpr();
				SootMethod sootMethod = expr.getMethod();
				if(sootMethod.equals(callee)){
					toInline.add((InvokeStmt) u);
				}
			}
		}
		if(toInline.size() == 1) {
			callee.retrieveActiveBody();
			caller.retrieveActiveBody();
			SiteInliner.inlineSite(callee,toInline.iterator().next(),caller);
			return true;
		}
		return false;
	}
}
