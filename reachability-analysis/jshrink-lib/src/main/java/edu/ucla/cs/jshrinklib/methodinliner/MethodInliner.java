package edu.ucla.cs.jshrinklib.methodinliner;

import edu.ucla.cs.jshrinklib.util.ClassFileUtils;
import edu.ucla.cs.jshrinklib.util.SootUtils;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.invoke.InlinerSafetyManager;
import soot.jimple.toolkits.invoke.SiteInliner;

import java.io.File;
import java.util.*;

public class MethodInliner {


	/**
	 * This method will inline any methods called from a single location (the callee method is subsequently removed)
	 * @param callgraph The call graph, represented as a callgraph (Map<Callee, Set<Caller>>). Modified as the actual
	 * 	callgraph is
	 * @param classpaths The classpaths of the code that are deemed modifiable.
	 * @return A InlineData object --- this documents what methods have been inlined, where, and what methods have been\
	 * 	modified
	 */
	public static InlineData inlineMethods(Map<SootMethod, Set<SootMethod>> callgraph, Set<File> classpaths){

		InlineData toReturn = new InlineData();
		Set<SootMethod> methodsRemoved = new HashSet<SootMethod>();

		boolean callgraphChanged = false;

		do{
			callgraphChanged = false;
			for (Map.Entry<SootMethod, Set<SootMethod>> entry : callgraph.entrySet()) {

				//We are only interested inlining methods if there is only one line site
				if (entry.getValue().size() != 1) {
					continue;
				}

				SootMethod caller = entry.getValue().iterator().next();
				SootMethod callee = entry.getKey();

				//Both the caller and callee classes must be within the current classpaths.
				if (!ClassFileUtils.getClassFile(callee.getDeclaringClass(), classpaths).isPresent()
					|| !ClassFileUtils.getClassFile(caller.getDeclaringClass(), classpaths).isPresent()) {
					continue;
				}


				//The caller and the callee must be contained in a SootClasses that are ultimately modifiable.
				if (!SootUtils.modifiableSootClass(caller.getDeclaringClass())
					|| !SootUtils.modifiableSootClass(callee.getDeclaringClass())) {
					continue;
				}

			/*
			We do not inline constructors (unless within a constructor in the same class). Doing so can cause
			problems with the MethodWiper component.
			 */
				if (callee.isConstructor()) {
					if (!(caller.getDeclaringClass().equals(callee.getDeclaringClass()) && caller.isConstructor())) {
						continue;
					}
				}

				//We ignore access methods (created by the compiler for inner classes).
				if (callee.getName().startsWith("access$") || caller.getName().startsWith("access$")) {
					continue;
				}

				List<Stmt> toInline = new ArrayList<Stmt>();
				Body b = caller.retrieveActiveBody();

				for (Unit u : b.getUnits()) {
					if (u instanceof InvokeStmt) {
						InvokeExpr expr = ((InvokeStmt) u).getInvokeExpr();
						SootMethod sootMethod = expr.getMethod();
						if (sootMethod.equals(callee)) {
							toInline.add((InvokeStmt) u);
						}
					}
				}

				// There must be exactly 1 inline site in the caller method.
				if (toInline.size() != 1) {
					continue;
				}

				try {
					callee.retrieveActiveBody();
					caller.retrieveActiveBody();
				} catch (Exception e) {
					//This is a catch all --- if the methods can't be retrieved, we can't inline them.
					continue;
				}

				Stmt site = toInline.iterator().next();

			/*
			I'm not sure exactly what this does, but I think it's good to use Soot's own "Inlinability" check here.
			ModifierOptions: "safe", "unsafe", or "nochanges". Though, at the time of writing, "unsafe" is the only
			option that's been implemented. "unsafe" means that the inline may be unsafe but is possible.
			*/
				if (!InlinerSafetyManager.ensureInlinability(callee, site, caller, "unsafe")) {
					continue;
				}

				//Inline the method
				SiteInliner.inlineSite(callee, site, caller);

				//Record the inlined method.
				toReturn.addInlinedMethods(callee.getSignature(), caller.getSignature());
				toReturn.addClassModified(caller.getDeclaringClass());

				//Remove update our call graph information (I admit this is a bit inefficient but it's simple).
				for (Map.Entry<SootMethod, Set<SootMethod>> entry2 : callgraph.entrySet()) {
					if (entry2.getValue().contains(callee)) {
						entry2.getValue().remove(callee);
						entry2.getValue().add(caller);
						callgraphChanged=true;
					}
				}

				//Remove the callee method from its class.
				toReturn.addClassModified(callee.getDeclaringClass());
				SootClass calleeSootClass = callee.getDeclaringClass();
				calleeSootClass.getMethods().remove(callee);
				methodsRemoved.add(callee);
			}
		}while(callgraphChanged);

		//Update the call graph with the removed methods.
		for(SootMethod sootMethod : methodsRemoved){
			callgraph.remove(sootMethod);
		}

		return toReturn;
	}
}