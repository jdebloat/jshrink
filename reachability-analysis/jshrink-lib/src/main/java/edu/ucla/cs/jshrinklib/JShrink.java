package edu.ucla.cs.jshrinklib;

import edu.ucla.cs.jshrinklib.classcollapser.ClassCollapser;
import edu.ucla.cs.jshrinklib.classcollapser.ClassCollapserAnalysis;
import edu.ucla.cs.jshrinklib.classcollapser.ClassCollapserData;
import edu.ucla.cs.jshrinklib.methodinliner.InlineData;
import edu.ucla.cs.jshrinklib.methodinliner.MethodInliner;
import edu.ucla.cs.jshrinklib.methodwiper.MethodWiper;
import edu.ucla.cs.jshrinklib.reachability.*;
import edu.ucla.cs.jshrinklib.util.ClassFileUtils;
import edu.ucla.cs.jshrinklib.util.SootUtils;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JShrink {
	private File projectDir;
	private EntryPointProcessor entryPointProcessor;
	private Optional<File> tamiflex;
	private boolean useSpark;
	private Optional<Set<CallGraph>> callGraphs = Optional.empty();
	private Optional<Map<MethodData,Set<MethodData>>> smallCallGraph = Optional.empty();
	private Optional<Set<MethodData>> allAppMethods = Optional.empty();
	private Optional<Set<MethodData>> allLibMethods = Optional.empty();
	private Optional<Set<MethodData>> usedAppMethods = Optional.empty();
	private Optional<Set<MethodData>> usedLibMethods = Optional.empty();
	private Optional<IProjectAnalyser> projectAnalyser = Optional.empty();
	private boolean projectAnalyserRun = false;
	private Optional<Set<String>> allAppClasses = Optional.empty();
	private Optional<Set<String>> allLibClasses = Optional.empty();
	private Optional<Set<String>> usedAppClasses = Optional.empty();
	private Optional<Set<String>> usedLibClasses = Optional.empty();
	private Optional<Set<String>> classesToIgnore = Optional.empty();
	private Optional<TestOutput> testOutput = Optional.empty();
	private Set<SootClass> classesToModify = new HashSet<SootClass>();
	private Set<SootClass> classesToRemove = new HashSet<SootClass>();

	private static Optional<JShrink> instance = Optional.empty();

	/*
	At present this only work on Maven Directories. I should expand this to be more general (some code exists to
	support this but TamiFlex is a big hurdle for now --- we need to run test cases).
	*/

	//TODO: Expand for projects that are not just Maven (see above).

	//TODO: Yet to implement the functionality to ignore certain classes for modification.

	//TODO: Yet to implement functionality to remove classes that are completely unused.



	public static JShrink createInstance(File projectDir,
	                                  EntryPointProcessor entryPointProcessor,
	                                  Optional<File> tamiflex,
	                                  boolean useSpark) throws IOException{
		/*
		Due to Soot using a singleton pattern, I use a singleton pattern here to ensure safety.
		E.g., only one project can be worked on at once.
		 */
		if(instance.isPresent()){
			throw new IOException("Instance of JShrink already exists. Please use \"getInstance\".");
		}
		instance = Optional.of(new JShrink(projectDir, entryPointProcessor, tamiflex, useSpark));
		return instance.get();
	}

	public static boolean instanceExists(){
		return instance.isPresent();
	}

	public static JShrink getInstance() throws IOException{
		if(instance.isPresent()){
			return instance.get();
		}
		throw new IOException("Instance of JShrink does not exist. Please used \"createInstance\".");
	}

	public static JShrink resetInstance(File projectDir,
	                                    EntryPointProcessor entryPointProcessor,
	                                    Optional<File> tamiflex,
	                                    boolean useSpark) throws IOException{
		if(instance.isPresent()){
			instance.get().reset();
			instance.get().projectDir = projectDir;
			instance.get().entryPointProcessor = entryPointProcessor;
			instance.get().tamiflex = tamiflex;
			instance.get().useSpark = useSpark;
			return instance.get();
		}
		throw new IOException("Instance of JShrink does not exist. Please use \"createInstance\".");
	}

	private JShrink(File projectDir, EntryPointProcessor entryPointProcessor, Optional<File> tamiflex, boolean useSpark){
			this.projectDir = projectDir;
			this.entryPointProcessor = entryPointProcessor;
			this.tamiflex = tamiflex;
			this.useSpark = useSpark;
	}

	public Set<CallGraph> getCallGraphs(){
		if(callGraphs.isPresent()){
			return this.callGraphs.get();
		}

		this.callGraphs = Optional.of(this.getProjectAnalyser().getCallGraphs());
		return this.callGraphs.get();
	}

	private IProjectAnalyser getProjectAnalyser(){
		//Will return setup, not guaranteed to have been run. Use "getProjectAnalyserRun" for this.
		if(this.projectAnalyser.isPresent()){
			return this.projectAnalyser.get();
		}

		//Just supporting MavenSingleProjectAnalysis for now
		this.projectAnalyser = Optional.of(
			new MavenSingleProjectAnalyzer(this.projectDir.getAbsolutePath(),
				this.entryPointProcessor, this.tamiflex, this.useSpark));

		this.projectAnalyser.get().setup();

		return this.projectAnalyser.get();
	}

	private IProjectAnalyser getProjectAnalyserRun(){
		if(!this.projectAnalyserRun){
			this.getProjectAnalyser().run();

			G.reset();
			SootUtils.setup_trimming(this.getProjectAnalyser().getLibClasspaths(),
				this.getProjectAnalyser().getAppClasspaths(), this.getProjectAnalyser().getTestClasspaths());
			Scene.v().loadNecessaryClasses();

			this.projectAnalyserRun = true;
		}
		return this.getProjectAnalyser();
	}

	public Set<MethodData> getAllAppMethods(){
		if(this.allAppMethods.isPresent()){
			return this.allAppMethods.get();
		}

		this.allAppMethods = Optional.of(this.getProjectAnalyserRun().getAppMethods());
		return this.allAppMethods.get();
	}

	public Set<MethodData> getAllLibMethods(){
		if(this.allLibMethods.isPresent()){
			return this.allLibMethods.get();
		}

		this.allLibMethods = Optional.of(this.getProjectAnalyserRun().getLibMethodsCompileOnly());
		return this.allLibMethods.get();
	}

	public Set<MethodData> getUsedAppMethods(){
		if(this.usedAppMethods.isPresent()){
			return this.usedAppMethods.get();
		}

		this.usedAppMethods = Optional.of(this.getProjectAnalyserRun().getUsedAppMethods().keySet());
		return this.usedAppMethods.get();
	}

	public Set<MethodData> getUsedLibMethods(){
		if(this.usedLibMethods.isPresent()){
			return this.usedLibMethods.get();
		}

		this.usedLibMethods = Optional.of(this.getProjectAnalyserRun().getUsedLibMethodsCompileOnly().keySet());
		return this.usedLibMethods.get();
	}

	public Set<String> getAllAppClasses(){
		if(this.allAppClasses.isPresent()){
			return this.allAppClasses.get();
		}

		this.allAppClasses = Optional.of(new HashSet<String>(this.getProjectAnalyserRun().getAppClasses()));
		return this.allAppClasses.get();
	}

	public Set<String> getAllLibClasses(){
		if(this.allLibClasses.isPresent()){
			return this.allLibClasses.get();
		}

		this.allLibClasses = Optional.of(new HashSet<String>(this.getProjectAnalyserRun().getLibClassesCompileOnly()));
		return this.allLibClasses.get();
	}

	public Set<String> getUsedAppClasses(){
		if(this.usedAppClasses.isPresent()){
			return this.usedAppClasses.get();
		}

		this.usedAppClasses = Optional.of(new HashSet<String>(this.getProjectAnalyserRun().getUsedAppClasses()));
		return this.usedAppClasses.get();
	}

	public Set<String> getUsedLibClasses(){
		if(this.usedLibClasses.isPresent()){
			return this.usedLibClasses.get();
		}

		this.usedLibClasses = Optional.of(new HashSet<String>(this.getProjectAnalyserRun().getUsedLibClassesCompileOnly()));
		return this.usedLibClasses.get();
	}

	public ClassCollapserData collapseClasses(boolean collapseAppClasses, boolean collapseLibClasses){
		Set<String> allClasses = new HashSet<String>();
		Set<String> usedClasses = new HashSet<String>();
		Set<MethodData> usedMethods = new HashSet<MethodData>();

		if(collapseAppClasses) {
			allClasses.addAll(this.getAllAppClasses());
			usedClasses.addAll(this.getUsedAppClasses());
			usedMethods.addAll(this.getUsedAppMethods());
		}
		if(collapseLibClasses) {
			allClasses.addAll(this.getAllLibClasses());
			usedClasses.addAll(this.getUsedLibClasses());
			usedMethods.addAll(this.getUsedLibMethods());
		}

		ClassCollapserAnalysis classCollapserAnalysis =
			new ClassCollapserAnalysis(allClasses, usedClasses, usedMethods);
		ClassCollapser classCollapser = new ClassCollapser();
		classCollapser.run(classCollapserAnalysis);

		ClassCollapserData classCollapserData = classCollapser.getClassCollapserData();
		for(String classToRewrite : classCollapserData.getClassesToRewrite()){
			SootClass sootClass = Scene.v().loadClassAndSupport(classToRewrite);
			this.classesToModify.add(sootClass);
		}
		for(String classToRemove : classCollapserData.getClassesToRemove()){
			SootClass sootClass = Scene.v().loadClassAndSupport(classToRemove);
			this.classesToModify.remove(sootClass);
			this.classesToRemove.add(sootClass);
		}

		return classCollapserData;
	}

	public Map<MethodData, Set<MethodData>> getSimplifiedCallGraph(){
		if(this.smallCallGraph.isPresent()){
			return this.smallCallGraph.get();
		}

		HashMap<MethodData, Set<MethodData>> toAdd = new HashMap<MethodData, Set<MethodData>>();
		toAdd.putAll(this.getProjectAnalyserRun().getUsedAppMethods());
		toAdd.putAll(this.getProjectAnalyserRun().getUsedLibMethods());

		this.smallCallGraph = Optional.of(toAdd);
		return this.smallCallGraph.get();
	}

	public InlineData inlineMethods(boolean inlineAppClassMethods, boolean inlineLibClassMethods){
		Map<SootMethod, Set<SootMethod>> callgraph = new HashMap<SootMethod, Set<SootMethod>>();
		Set<MethodData> validMethods = new HashSet<MethodData>();

		if(inlineAppClassMethods){
			validMethods.addAll(this.getAllAppMethods());
		}
		if(inlineLibClassMethods){
			validMethods.addAll(this.getAllLibMethods());
		}

		for(Map.Entry<SootMethod, Set<SootMethod>> entry :
			SootUtils.convertMethodDataCallGraphToSootMethodCallGraph(this.getSimplifiedCallGraph()).entrySet()){
				callgraph.put(entry.getKey(),entry.getValue());
		}
		InlineData output = MethodInliner.inlineMethods(callgraph, this.getClassPaths());
		this.classesToModify.addAll(output.getClassesModified());

		return output;
	}

	public Set<String> getClassesToIgnore(){
		if(this.classesToIgnore.isPresent()){
			return this.classesToIgnore.get();
		}

		this.classesToIgnore = Optional.of(new HashSet<String>(this.getProjectAnalyserRun().classesToIgnore()));
		return this.classesToIgnore.get();
	}

	public Set<MethodData> removeMethods(Set<MethodData> toRemove){
		Set<MethodData> removedMethods = new HashSet<MethodData>();
		for(MethodData methodData : toRemove){
			SootClass sootClass = Scene.v().loadClassAndSupport(methodData.getClassName());
			if(!sootClass.isEnum() && sootClass.declaresMethod(methodData.getSubSignature())) {
				SootMethod sootMethod = sootClass.getMethod(methodData.getSubSignature());
				if(MethodWiper.removeMethod(sootMethod)) {
					removedMethods.add(methodData);
					this.classesToModify.add(sootClass);
				}
			}
		}
		return removedMethods;
	}

	public Set<MethodData> wipeMethods(Set<MethodData> toRemove){
		Set<MethodData> removedMethods = new HashSet<MethodData>();
		for(MethodData methodData : toRemove){
			SootClass sootClass = Scene.v().loadClassAndSupport(methodData.getClassName());
			if(!sootClass.isEnum() && sootClass.declaresMethod(methodData.getSubSignature())) {
				SootMethod sootMethod = sootClass.getMethod(methodData.getSubSignature());
				if(MethodWiper.wipeMethodBody(sootMethod)) {
					removedMethods.add(methodData);
					this.classesToModify.add(sootClass);
				}
			}
		}
		return removedMethods;
	}

	public Set<MethodData> wipeMethodAndAddException(Set<MethodData> toRemove, Optional<String> exceptionMethod){
		Set<MethodData> removedMethods = new HashSet<MethodData>();
		for(MethodData methodData : toRemove){
			SootClass sootClass = Scene.v().loadClassAndSupport(methodData.getClassName());
			if(!sootClass.isEnum() && sootClass.declaresMethod(methodData.getSubSignature())) {
				SootMethod sootMethod = sootClass.getMethod(methodData.getSubSignature());
				boolean success = false;
				if (exceptionMethod.isPresent()) {
					success = MethodWiper.wipeMethodBodyAndInsertRuntimeException(sootMethod, exceptionMethod.get());
				} else {
					success = MethodWiper.wipeMethodBodyAndInsertRuntimeException(sootMethod);
				}
				if (success) {
					removedMethods.add(methodData);
					this.classesToModify.add(sootClass);
				}
			}
		}
		return removedMethods;
	}


	public void removeClasses(Set<String> classes){
		for(String className : classes){
			SootClass sootClass = Scene.v().loadClassAndSupport(className);
			this.classesToRemove.add(sootClass);
			this.classesToModify.remove(sootClass);
		}
	}

	private Set<File> getClassPaths(){
		Set<File> classPaths = new HashSet<File>();

		classPaths.addAll(this.getProjectAnalyser().getAppClasspaths());
		classPaths.addAll(this.getProjectAnalyser().getLibClasspaths());
		classPaths.addAll(this.getProjectAnalyser().getTestClasspaths());

		return classPaths;
	}

	public void updateClassFiles(){
		try {
			Set<File> classPaths = this.getClassPaths();
			Set<File> decompressedJars =
				new HashSet<File>(ClassFileUtils.extractJars(new ArrayList<File>(classPaths)));
			JShrink.removeClasses(this.classesToRemove, classPaths);
			this.classesToRemove.clear();
			JShrink.modifyClasses(this.classesToModify, classPaths);
			ClassFileUtils.compressJars(decompressedJars);
			this.classesToModify.clear();
			this.reset();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void reset(){
		this.callGraphs = Optional.empty();
		this.smallCallGraph = Optional.empty();
		this.allAppMethods = Optional.empty();
		this.allLibMethods = Optional.empty();
		this.usedAppMethods = Optional.empty();
		this.usedLibMethods = Optional.empty();
		this.projectAnalyser = Optional.empty();
		this.projectAnalyserRun = false;
		this.allAppClasses = Optional.empty();
		this.allLibClasses = Optional.empty();
		this.usedAppClasses = Optional.empty();
		this.usedLibClasses = Optional.empty();
		this.classesToIgnore = Optional.empty();
		this.classesToModify.clear();
		this.classesToRemove.clear();
		this.testOutput = Optional.empty();
		G.reset();
	}

	private long getSize(boolean withJarsDecompressed, List<File> classPaths){
		Set<File> decompressedJars = new HashSet<File>();
		long toReturn = 0;
		try {
			if(withJarsDecompressed){
				decompressedJars =
					new HashSet<File>(ClassFileUtils.extractJars(new ArrayList<File>(classPaths)));
			}

			for(File file : classPaths){
				toReturn+=ClassFileUtils.getSize(file);
			}

			ClassFileUtils.compressJars(decompressedJars);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

		return toReturn;
	}

	public long getLibSize(boolean withJarsDecompressed){
		if(withJarsDecompressed){
			return getSize(true,
				this.getProjectAnalyser().getLibClasspaths());
		} else {
			return getSize(false, this.getProjectAnalyser().getLibClasspaths());
		}
	}

	public long getAppSize(boolean withJarsDecompressed){
		if(withJarsDecompressed){
			return getSize(true,
				this.getProjectAnalyser().getAppClasspaths());
		} else {
			return getSize(false, this.getProjectAnalyser().getAppClasspaths());
		}
	}

	public TestOutput getTestOutput(){
		if(this.testOutput.isPresent()){
			return this.testOutput.get();
		}

		this.testOutput = Optional.of(this.getProjectAnalyser().getTestOutput());
		return this.testOutput.get();
	}

	private static void modifyClasses(Set<SootClass> classesToRewrite, Set<File> classPaths){
		for (SootClass sootClass : classesToRewrite) {
			try {
				ClassFileUtils.writeClass(sootClass, classPaths);
			} catch (IOException e) {
				System.err.println("An exception was thrown when attempting to rewrite a class:");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}


	private static void removeClasses(Set<SootClass> classesToRemove, Set<File> classPaths){
		for(SootClass sootClass : classesToRemove){
			try{
				ClassFileUtils.removeClass(sootClass, classPaths);
			} catch (IOException e){
				System.err.println("An exception was thrown when attempting to delete a class:");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
