package edu.ucla.cs.onr;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

import com.google.common.collect.Sets;
import edu.ucla.cs.onr.classcollapser.ClassCollapser;
import edu.ucla.cs.onr.classcollapser.ClassCollapserAnalysis;
import edu.ucla.cs.onr.methodinliner.InlineData;
import edu.ucla.cs.onr.methodinliner.MethodInliner;
import edu.ucla.cs.onr.reachability.*;
import edu.ucla.cs.onr.util.EntryPointUtil;
import edu.ucla.cs.onr.util.SootUtils;
import edu.ucla.cs.onr.methodwiper.MethodWiper;
import edu.ucla.cs.onr.util.ClassFileUtils;

import org.apache.log4j.PropertyConfigurator;
import soot.*;

import static edu.ucla.cs.onr.util.SootUtils.convertSootMethodCallGraphToMethodDataCallGraph;

// TODO: We rely on the output of this application when in "--verbose" mode. This is currently a bit of a mess
// ,I suggest we use a logger to manage this better

public class Application {

	private static boolean DEBUG_MODE = true; //Enabled by default, needed for testing
	private static boolean VERBOSE_MODE = false;
	private static final Set<File> decompressedJars = new HashSet<File>();

	//I use this for testing to see if the correct methods have been removed
	/*package*/ static final Set<MethodData> removedMethods = new HashSet<MethodData>();

	//I use this for testing to see if the correct classes have been removed
	/*package*/ static final Set<String> removedClasses = new HashSet<String>();

	//I use this for testing to see if the correct methods have been inlined.
	/*package*/ static InlineData inlineData = null;

	//I use the following for testing to ensure the right kind of method wipe has been used
	/*package*/ static boolean removedMethod = false;
	/*package*/ static boolean wipedMethodBody = false;
	/*package*/ static boolean wipedMethodBodyWithExceptionNoMessage = false;
	/*package*/ static boolean wipedMethodBodyWithExceptionAndMessage = false;

	/*
	Quick hack: this is a way to share this set between Application (where a user may specify classes to ignore at the
	command-line level) and CallGraphAnalysis where this set may be updated by the TamiFlex reflection analysis.
	 */
	public static final Set<String> classesToIgnore = new HashSet<>();

	public static boolean isDebugMode() {
		return DEBUG_MODE;
	}

	public static boolean isVerboseMode(){
		return VERBOSE_MODE;
	}

	public static void main(String[] args) {

		//Re-initialise this each time Application is run (for testing)
		removedMethods.clear();
		decompressedJars.clear();
		removedClasses.clear();
		classesToIgnore.clear();
		inlineData = null;
		removedMethod = false;
		wipedMethodBody = false;
		wipedMethodBodyWithExceptionNoMessage = false;
		wipedMethodBodyWithExceptionAndMessage = false;

		//I just put this in to stop an error
		PropertyConfigurator.configure(
			Application.class.getClassLoader().getResourceAsStream("log4j.properties"));

		//Load the command line arguments
		ApplicationCommandLineParser commandLineParser = null;

		try {
			commandLineParser = new ApplicationCommandLineParser(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		assert (commandLineParser != null);

		DEBUG_MODE = commandLineParser.isDebug();
		VERBOSE_MODE = commandLineParser.isVerbose();
		classesToIgnore.addAll(commandLineParser.getClassesToIgnore());

		IProjectAnalyser projectAnalyser = null;


		//I'm not a fan of this kind of stuff... variable setting TODO: Fix this at some point
		CallGraphAnalysis.useSpark = commandLineParser.useSpark();


		EntryPointProcessor entryPointProcessor = new EntryPointProcessor(commandLineParser.includeMainEntryPoint(),
				commandLineParser.includePublicEntryPoints(), commandLineParser.includeTestEntryPoints(), true,
				commandLineParser.getCustomEntryPoints());

		if(commandLineParser.getMavenDirectory().isPresent()){
			projectAnalyser =  new MavenSingleProjectAnalyzer(
					commandLineParser.getMavenDirectory().get().getAbsolutePath(), entryPointProcessor,
					commandLineParser.getTamiflex());
		} else {
			projectAnalyser =  new CallGraphAnalysis(commandLineParser.getLibClassPath(),
					commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath(), entryPointProcessor);
		}

		assert(projectAnalyser != null);

		projectAnalyser.setup();

		try {
			/*
			In this mode, the call-graph analysis is run to determine what methods are touched and which are untouched.
			*/
			projectAnalyser.run();

			G.reset();
			SootUtils.setup_trimming(projectAnalyser.getLibClasspaths(),
					projectAnalyser.getAppClasspaths(),projectAnalyser.getTestClasspaths());
			Scene.v().loadNecessaryClasses();


			if(Application.isVerboseMode()) {

				System.out.println("number_lib_classes," + projectAnalyser.getLibClassesCompileOnly().size());
				System.out.println("number_lib_methods," + projectAnalyser.getLibMethodsCompileOnly().size());
				System.out.println("number_app_classes," + projectAnalyser.getAppClasses().size());
				System.out.println("number_app_methods," + projectAnalyser.getAppMethods().size());
				System.out.println("number_used_lib_classes," + projectAnalyser.getUsedLibClassesCompileOnly().size());
				System.out.println("number_used_lib_methods," + projectAnalyser.getUsedLibMethodsCompileOnly().size());
				System.out.println("number_used_app_classes," + projectAnalyser.getUsedAppClasses().size());
				System.out.println("number_used_app_methods," + projectAnalyser.getUsedAppMethods().size());

				for(MethodData entrypoint : projectAnalyser.getEntryPoints()){
					System.out.println("entry_point," + entrypoint.getSignature());
				}
			}


			if(Application.isVerboseMode()){
				for(File file : projectAnalyser.getAppClasspaths()) {
					System.out.println("app_size_before_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}
				for(File file: projectAnalyser.getLibClasspaths()){
					System.out.println("lib_size_before_debloat_" + file.getAbsolutePath() + ","
							+ClassFileUtils.getSize(file));
				}
			}

			decompressedJars.addAll(ClassFileUtils.extractJars(projectAnalyser.getAppClasspaths()));
			decompressedJars.addAll(ClassFileUtils.extractJars(projectAnalyser.getLibClasspaths()));
			decompressedJars.addAll(ClassFileUtils.extractJars(projectAnalyser.getTestClasspaths()));

			if(Application.isVerboseMode()){
				for(File file : projectAnalyser.getAppClasspaths()){
					System.out.println("app_size_decompressed_before_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}

				for(File file : projectAnalyser.getLibClasspaths()){
					System.out.println("lib_size_decompressed_before_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}
			}

			Set<MethodData> methodsToRemove = new HashSet<MethodData>();

			Set<SootClass> classesToRewrite = new HashSet<SootClass>(); //Take note of all classes that have changed
			Set<SootClass> classesToRemove = new HashSet<SootClass>(); //Take note of all classes that need to be removed
			Set<File> classPathsOfConcern = new HashSet<File>(); //The classpaths where these classes can be found

			//Get classpaths of concern
			classPathsOfConcern.addAll(projectAnalyser.getLibClasspaths());
			if(commandLineParser.isPruneAppInstance()){
				classPathsOfConcern.addAll(projectAnalyser.getAppClasspaths());
			}

			//Inline methods
			if(commandLineParser.inlineMethods()){
				Map<SootMethod, Set<SootMethod>> callGraph =
						commandLineParser.isPruneAppInstance() ? SootUtils.mergeCallGraphMaps(
								SootUtils.convertMethodDataCallGraphToSootMethodCallGraph(
										projectAnalyser.getUsedLibMethodsCompileOnly()),
								SootUtils.convertMethodDataCallGraphToSootMethodCallGraph(
										projectAnalyser.getUsedAppMethods()))
								: SootUtils.convertMethodDataCallGraphToSootMethodCallGraph(
								projectAnalyser.getUsedLibMethodsCompileOnly());
				inlineData = MethodInliner.inlineMethods(callGraph, classPathsOfConcern);
				classesToRewrite.addAll(inlineData.getClassesModified());
				if(Application.isVerboseMode()){
					for(Map.Entry<String, Set<String>> entry : inlineData.getInlineLocations().entrySet()){
						//inline_<method inlined>,<ultimate inline location>
						System.out.println("inline_"+entry.getKey() + ","
								+ inlineData.getUltimateInlineLocations(entry.getKey()).get());
					}
				}
			}

			//Collapse classes
			if(commandLineParser.collapseClasses()){
				Set<String> classes = new HashSet<String>();
				Set<String> usedClasses = new HashSet<String>();
				Set<MethodData> usedMethods = new HashSet<MethodData>();
				if(commandLineParser.isPruneAppInstance()) {
					classes.addAll(projectAnalyser.getAppClasses());
					usedClasses.addAll(projectAnalyser.getUsedAppClasses());
					usedMethods.addAll(projectAnalyser.getUsedAppMethods().keySet());
				}
				classes.addAll(projectAnalyser.getLibClasses());
				classes.addAll(projectAnalyser.getUsedLibClasses());
				usedMethods.addAll(projectAnalyser.getUsedLibMethods().keySet());

				ClassCollapserAnalysis classCollapserAnalysis
						= new ClassCollapserAnalysis(classes, usedClasses,usedMethods);
				classCollapserAnalysis.run();

				ClassCollapser classCollapser = new ClassCollapser();
				classCollapser.run(classCollapserAnalysis);

				classesToRewrite.addAll(classCollapser.getClassesToRewrite());
				classesToRemove.addAll(classCollapser.getClassesToRemove());
				removedMethods.addAll(classCollapser.getRemovedMethods());
				for(SootClass sootClass : classCollapser.getClassesToRemove()){
					removedClasses.add(sootClass.getName());
				}
			}



			//Note the unused Library methods
			for(MethodData methodData : projectAnalyser.getLibMethodsCompileOnly()){
				if(!classesToIgnore.contains(methodData.getClassName())
					&& !projectAnalyser.getUsedLibMethodsCompileOnly().keySet().contains(methodData)
					&& !classesToRemove.contains(Scene.v().getSootClass(methodData.getClassName()))){
					methodsToRemove.add(methodData);
				}
			}

			//Note the unused app methods (if applicable)
			if (commandLineParser.isPruneAppInstance()) {
				for(MethodData methodData : projectAnalyser.getAppMethods()){
					if(!classesToIgnore.contains(methodData.getClassName())
						&& !projectAnalyser.getUsedAppMethods().keySet().contains(methodData)
						&& !classesToRemove.contains(Scene.v().getSootClass(methodData.getClassName()))){
						methodsToRemove.add(methodData);
					}
				}
			}

			//Note removed classes
			if(commandLineParser.removeClasses()) {
				//Not the classes in which all the methods are removed
				Map<SootClass, Set<MethodData>> classIntCount = new HashMap<SootClass, Set<MethodData>>();
				for (MethodData method : methodsToRemove) {
					SootClass sootClass = Scene.v().loadClassAndSupport(method.getClassName());
					if (!classIntCount.containsKey(sootClass)) {
						Set<MethodData> methods = new HashSet<MethodData>();
						for (SootMethod sootMethod : sootClass.getMethods()) {
							methods.add(SootUtils.sootMethodToMethodData(sootMethod));
						}
						classIntCount.put(sootClass, methods);
					}
					classIntCount.get(sootClass).remove(method);
				}

				for (Map.Entry<SootClass, Set<MethodData>> entry : classIntCount.entrySet()) {
					if (entry.getValue().isEmpty()) {
						SootClass sootClass = entry.getKey();
						boolean containsAccessibleStaticFields = false;
						for (SootField sootField : sootClass.getFields()) {
							if (!sootField.isPrivate() && sootField.isStatic()) {
								containsAccessibleStaticFields = true;
								break;
							}
						}

						if (!containsAccessibleStaticFields) {
							classesToRemove.add(entry.getKey());
							Set<MethodData> methods = new HashSet<MethodData>();
							for (SootMethod sootMethod : entry.getKey().getMethods()) {
								methods.add(SootUtils.sootMethodToMethodData(sootMethod));
							}
							//If we remove the class we obviously remove the method
							Application.removedMethods.addAll(methods);
							Application.removedClasses.add(entry.getKey().getName());
							methodsToRemove.removeAll(methods);
						}
					}
				}
			}


			for (MethodData method : methodsToRemove) {
				SootClass sootClass = Scene.v().loadClassAndSupport(method.getClassName());
				if (!sootClass.isEnum() && sootClass.declaresMethod(method.getSubSignature())) {
					SootMethod sootMethod = sootClass.getMethod(method.getSubSignature());
					boolean success = false;
					if(commandLineParser.removeMethods()){
						removedMethod = true;
						success = MethodWiper.wipeMethod(sootMethod);
					} else if(commandLineParser.includeException()){
						if(commandLineParser.getExceptionMessage().isPresent()) {
							wipedMethodBodyWithExceptionAndMessage = true;
							success = MethodWiper.wipeMethodBodyAndInsertRuntimeException(sootMethod,
									commandLineParser.getExceptionMessage().get());
						} else {
							wipedMethodBodyWithExceptionNoMessage = true;
							success = MethodWiper.wipeMethodBodyAndInsertRuntimeException(sootMethod);
						}
					} else {
						wipedMethodBody = true;
						success = MethodWiper.wipeMethodBody(sootMethod);
					}

					if (success) {
						Application.removedMethods.add(SootUtils.sootMethodToMethodData(sootMethod));
						classesToRewrite.add(sootClass);
					}
				}
			}


			removeClasses(classesToRemove,classPathsOfConcern);
			modifyClasses(classesToRewrite,classPathsOfConcern);


			if(Application.isVerboseMode()) {
				int numberLibMethodsRemoved = Sets.intersection(projectAnalyser.getLibMethodsCompileOnly(),
						Application.removedMethods).size();
				int numberAppMethodsRemoved = Sets.intersection(projectAnalyser.getAppMethods(),
						Application.removedMethods).size();
				System.out.println("number_lib_methods_removed," + numberLibMethodsRemoved);
				System.out.println("number_app_methods_removed," + numberAppMethodsRemoved);
			}

			if(Application.isVerboseMode()){
				for(File file : projectAnalyser.getAppClasspaths()){
					System.out.println("app_size_decompressed_after_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}

				for(File file : projectAnalyser.getLibClasspaths()){
					System.out.println("lib_size_decompressed_after_debloat_" + file.getAbsolutePath() + ","
							+ ClassFileUtils.getSize(file));
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				ClassFileUtils.compressJars(decompressedJars);
				if(Application.isVerboseMode()){
					for(File file : projectAnalyser.getAppClasspaths()){
						System.out.println("app_size_after_debloat_" + file.getAbsolutePath() + ","
								+ ClassFileUtils.getSize(file));
					}

					for(File file : projectAnalyser.getLibClasspaths()){
						System.out.println("lib_size_after_debloat_" + file.getAbsolutePath() + ","
								+ ClassFileUtils.getSize(file));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static String getExceptionMessage() {
		return "Method has been removed";
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
