package edu.ucla.cs.onr.methodinliner;

import soot.SootClass;

import java.util.*;

public class InlineData {
	//A container class
	private Set<SootClass> classesModified = new HashSet<SootClass>();
	/*
	Note: Need to store methods as strings : as they are removed, they no loner exist. We store their former
	signatures.
	 */
	private Map<String, Set<String>> inlineLocations = new HashMap<String, Set<String>>();

	public InlineData(){}

	/*package*/ void addInlinedMethods(String calleeMethod, String callerMethod){
		if(!this.inlineLocations.containsKey(calleeMethod)){
			this.inlineLocations.put(calleeMethod, new HashSet<String>());
		}
		this.inlineLocations.get(calleeMethod).add(callerMethod);
	}

	/*package*/ void addClassModified(SootClass modifiedClass){
		this.classesModified.add(modifiedClass);
	}

	public Set<SootClass> getClassesModified(){
		return Collections.unmodifiableSet(this.classesModified);
	}

	public Map<String, Set<String>> getInlineLocations(){
		return Collections.unmodifiableMap(this.inlineLocations);
	}

	public Optional<Set<String>> getUltimateInlineLocations(String sig){
			/*
			As inlining may happen in a chain, it's useful to know where after all inlining is done, where the inlined
			method ultimately exists. Returns empty optional if the signature is not a valid inlined method
			*/
		Optional<Set<String>> toReturn = this.inlineLocations.containsKey(sig)
				? Optional.of(this.inlineLocations.get(sig)) : Optional.empty();
		if(toReturn.isPresent()) {
			Set<String> temp = new HashSet<String>(toReturn.get());
			for (String loc : temp) {
				Optional<Set<String>> locLocs = getUltimateInlineLocations(loc);
				if(locLocs.isPresent()){
					toReturn.get().remove(loc);
					toReturn.get().addAll(locLocs.get());
				}
			}
		}

		assert((toReturn.isPresent() && !toReturn.get().isEmpty()) || !toReturn.isPresent());
		return toReturn;
	}
}
