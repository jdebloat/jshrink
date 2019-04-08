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
	private final static boolean debug = false;

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

		Comparator<SootMethod> comparator = new Comparator<SootMethod>() {
			@Override
			public int compare(SootMethod o1, SootMethod o2) {
				return o1.getSignature().compareTo(o2.getSignature());
			}
		};

		do{
			SortedMap<SootMethod, Set<SootMethod>> sortedCallgraph = new TreeMap<SootMethod, Set<SootMethod>>(comparator);
			sortedCallgraph.putAll(callgraph);
			callgraphChanged = false;
			for (Map.Entry<SootMethod, Set<SootMethod>> entry : sortedCallgraph.entrySet()) {

				//We are only interested inlining methods if there is only one line site
				if (entry.getValue().size() != 1) {
					continue;
				}

				SootMethod caller = entry.getValue().iterator().next();
				SootMethod callee = entry.getKey();

				if(debug){
					System.out.println();
					System.out.println("Attempting to inline " + callee.getSignature()
						+ " at " + caller.getSignature() + ".");
				}

				//Both the caller and callee classes must be within the current classpaths.
				if (!ClassFileUtils.classInPath(callee.getDeclaringClass().getName(), classpaths)
					|| !ClassFileUtils.classInPath(caller.getDeclaringClass().getName(), classpaths)) {
					if(debug){
						System.out.println("FAILED: Caller or Callee not within the current classpath");
					}
					continue;
				}


				//The caller and the callee must be contained in a SootClasses that are ultimately modifiable.
				if (!SootUtils.modifiableSootClass(caller.getDeclaringClass())
					|| !SootUtils.modifiableSootClass(callee.getDeclaringClass())) {
					if(debug){
						System.out.println("FAILED: Caller or Callee not within modifiable SootClass.");
					}
					continue;
				}

				/*
				We do not inline constructors (unless within a constructor in the same class). Doing so can cause
				problems with the MethodWiper component.
				 */
				if (callee.isConstructor()) {
					if (!(caller.getDeclaringClass().equals(callee.getDeclaringClass()) && caller.isConstructor())) {
						if(debug){
							System.out.println("FAILED: Callee is a constructor.");
						}
						continue;
					}
				}

				//We ignore access methods (created by the compiler for inner classes).
				if (callee.getName().startsWith("access$") || caller.getName().startsWith("access$")) {
					if(debug) {
						System.out.println("Caller or Callee is access$ methood");
					}
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
					if(debug){
						System.out.println("FAILED: More than 1 inline site.");
					}
					continue;
				}

				try {
					callee.retrieveActiveBody();
					caller.retrieveActiveBody();
				} catch (Exception e) {
					//This is a catch all --- if the methods can't be retrieved, we can't inline them.
					if(debug){
						System.out.println("FAILED: Cannot retrieve active body for caller or callee.");
					}
					continue;
				}

				Stmt site = toInline.iterator().next();

				/*
				I'm not sure exactly what this does, but I think it's good to use Soot's own "Inlinability" check here.
				ModifierOptions: "safe", "unsafe", or "nochanges". Though, at the time of writing, "unsafe" is the only
				option that's been implemented. "unsafe" means that the inline may be unsafe but is possible.
				*/
				if (!InlinerSafetyManager.ensureInlinability(callee, site, caller, "unsafe")) {
					if(debug){
						System.out.println("FAILED: InlineSafetyManager.ensureInlinability returned false.");
					}
					continue;
				}

				if(!inlineIsEfficient(callee, site, caller)){
					if(debug){
						System.out.println("FAILED: This inline operation would increase the size of the app.");
					}
					continue;
				}


				//I don't know why I have to do this again, but I get errors otherwise.
				toInline = new ArrayList<Stmt>();
				b = caller.retrieveActiveBody();

				for (Unit u : b.getUnits()) {
					if (u instanceof InvokeStmt) {
						InvokeExpr expr = ((InvokeStmt) u).getInvokeExpr();
						SootMethod sootMethod = expr.getMethod();
						if (sootMethod.equals(callee)) {
							toInline.add((InvokeStmt) u);
						}
					}
				}
				site = toInline.iterator().next();


				//Inline the method
				SiteInliner.inlineSite(callee, site, caller);

				//Record the inlined method.
				toReturn.addInlinedMethods(SootUtils.sootMethodToMethodData(callee),
					SootUtils.sootMethodToMethodData(caller));
				toReturn.addClassModified(caller.getDeclaringClass());

				//Remove update our call graph information (I admit this is a bit inefficient but it's simple).
				for (Map.Entry<SootMethod, Set<SootMethod>> entry2 : callgraph.entrySet()) {
					if (entry2.getValue().contains(callee)) {
						entry2.getValue().remove(callee);
						entry2.getValue().add(caller);
						callgraphChanged=true;
					}
				}

				if(debug){
					System.out.println("SUCCESS!");
				}
			}
		}while(callgraphChanged);

		//Update the call graph with the removed methods.
		for(SootMethod sootMethod : methodsRemoved){
			callgraph.remove(sootMethod);
		}

		return toReturn;
	}

	private static boolean inlineIsEfficient(SootMethod inline, Stmt toInline, SootMethod container){
		SootClass inlineClass = inline.getDeclaringClass();
		SootClass containerClass = container.getDeclaringClass();

		//Obtain the sizes before inlining.
		long inlineClassSizeBefore = ClassFileUtils.getSize(inlineClass);
		long containerClassSizeBefore = ClassFileUtils.getSize(containerClass);

		//Inline and remove the inlined location.

		Body body = container.retrieveActiveBody();
		Body bodyClone = (Body)body.clone();//SerializationUtils.clone(body);
		assert(body.toString().equals(bodyClone.toString()));
		SiteInliner.inlineSite(inline, toInline, container);

		int inlineMethodIndex = inlineClass.getMethods().indexOf(inline);
		inlineClass.getMethods().remove(inline);

		//Obtain the sizes after inlining.
		long inlineClassSizeAfter = ClassFileUtils.getSize(inlineClass);
		long containerClassSizeAfter = ClassFileUtils.getSize(containerClass);

		//Revert the inlining and re-add the removed method.
		inlineClass.getMethods().add(inlineMethodIndex, inline);
		container.setActiveBody(bodyClone);
		assert(bodyClone.toString().equals(container.retrieveActiveBody().toString()));

		//Return true if inlining reduces size, otherwise return false.
		return (inlineClassSizeAfter + containerClassSizeAfter)
			-(inlineClassSizeBefore + containerClassSizeBefore) < 0;

	}
}