package edu.ucla.cs.onr;

import java.io.*;
import java.util.*;

import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.reachability.SparkCallGraphAnalysis;
import edu.ucla.cs.onr.util.ASMUtils;
import edu.ucla.cs.onr.util.EntryPointUtil;
import edu.ucla.cs.onr.util.SootUtils;
import edu.ucla.cs.onr.methodwiper.MethodWiper;
import edu.ucla.cs.onr.util.WritingClassUtils;

import org.apache.log4j.PropertyConfigurator;
import soot.*;
import soot.options.Options;

public class Application {

	private static boolean DEBUG_MODE = true; //Enabled by default, needed for testing
	private static boolean VERBOSE_MODE = false;

	//I use this for testing, to see if the correct methods have been removed
	/*package*/ static Set<MethodData> removedMethods = new HashSet<MethodData>();

	public static boolean isDebugMode() {
		return DEBUG_MODE;
	}

	public static boolean isVerboseMode(){
		return VERBOSE_MODE;
	}

	public static void main(String[] args) {

		//Re-initialise this each time Application is run (for testing)
		removedMethods.clear();

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

		if(commandLineParser.removeMethods() && !commandLineParser.methodsToRemove().isEmpty()){
			/*
			Our application has two modes. In this mode, the methods to be removed are specified at the command-line
			level. The call graph analysis is not run, and the methods are directly wiped.
			 */
			Set<File> classPathsOfConcern = new HashSet<File>();
			classPathsOfConcern.addAll(commandLineParser.getAppClassPath());
			classPathsOfConcern.addAll(commandLineParser.getLibClassPath());
			classPathsOfConcern.addAll(commandLineParser.getTestClassPath());


			//Set<MethodData> entryPoints = new HashSet<MethodData>();
			//try {
			//	entryPoints.add(new MethodData("<StandardStuff: public void publicAndTestedButUntouched()>"));
			//} catch (IOException e) {
			//	e.printStackTrace();
			//}
			//Run the call graph analysis
			//SparkCallGraphAnalysis callGraphAnalysis = new SparkCallGraphAnalysis(commandLineParser.getLibClassPath(),
			//	commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath(), entryPoints);
			//callGraphAnalysis.run();

			SootUtils.setup_trimming(commandLineParser.getLibClassPath(),
				commandLineParser.getAppClassPath(),commandLineParser.getTestClassPath());
			Scene.v().loadNecessaryClasses();

			Set<SootClass> classesToRewrite = new HashSet<SootClass>();



			for(MethodData methodData : commandLineParser.methodsToRemove()){
				SootClass sootClass = Scene.v().loadClassAndSupport(methodData.getClassName());

				if(!sootClass.isEnum() && sootClass.declaresMethod(methodData.getSubSignature())) {
					SootMethod sootMethod = sootClass.getMethod(methodData.getSubSignature());
					if (MethodWiper.wipeMethodAndInsertRuntimeException(sootMethod,
						Application.getExceptionMessage(sootMethod))) {
						Application.removedMethods.add(methodData);
						classesToRewrite.add(sootClass);
					}
				}
			}

			modifyClasses(classesToRewrite, classPathsOfConcern);

		} else {
			/*
			In this mode, the call-graph analysis is run to determine what methods are touched and which are untouched.
			*/

			//Get the entry points
			Set<MethodData> entryPoints = getEntryPoints(commandLineParser);

			//Run the call graph analysis
			SparkCallGraphAnalysis callGraphAnalysis = new SparkCallGraphAnalysis(commandLineParser.getLibClassPath(),
				commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath(), entryPoints);
			callGraphAnalysis.run();

			// Setup soot
			// (should have already been done, but not taking any chances, little cost for doing so again)
			SootUtils.setup_trimming(commandLineParser.getLibClassPath(),
				commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath());

			if(Application.isVerboseMode()) {
				for (MethodData method : callGraphAnalysis.getLibMethods()) {
					System.out.println("lib_method," + method.toString());
				}

				for (MethodData method : callGraphAnalysis.getAppMethods()) {
					System.out.println("app_method," + method.toString());
				}

				for (MethodData method : callGraphAnalysis.getUsedLibMethods()) {
					System.out.println("lib_method_touched," + method.toString());
				}

				for (MethodData method : callGraphAnalysis.getUsedAppMethods()) {
					System.out.println("lib_method_touched," + method.toString());
				}
			}

			Set<MethodData> libMethodsRemoved = new HashSet<MethodData>();
			Set<MethodData> appMethodsRemoved = new HashSet<MethodData>();

			if(commandLineParser.removeMethods()) {
				Set<SootClass> classesToRewrite = new HashSet<SootClass>(); //Take note of all classes that have changed
				Set<File> classPathsOfConcern = new HashSet<File>(); //The classpaths where these classes can be found

				//Remove the unused library methods and classes
				Set<MethodData> libMethodsToRemove = new HashSet<MethodData>();
				libMethodsToRemove.addAll(callGraphAnalysis.getLibMethods());
				libMethodsToRemove.removeAll(callGraphAnalysis.getUsedLibMethods());

				classPathsOfConcern.addAll(commandLineParser.getLibClassPath());

				for (MethodData methodToRemoveString : libMethodsToRemove) {
					SootClass sootClass = Scene.v().loadClassAndSupport(methodToRemoveString.getClassName());

					if (!sootClass.isEnum() && sootClass.declaresMethod(methodToRemoveString.getSubSignature())) {
						SootMethod sootMethod = sootClass.getMethod(methodToRemoveString.getSubSignature());
						if (MethodWiper.wipeMethodAndInsertRuntimeException
							(sootMethod, getExceptionMessage(sootMethod))) {
							Application.removedMethods.add(SootUtils.sootMethodToMethodData(sootMethod));
							classesToRewrite.add(sootClass);
							libMethodsRemoved.add(methodToRemoveString);
						}
					}
				}

				//Remove the unused app methods (if applicable)
				if (commandLineParser.isPruneAppInstance()) {
					classPathsOfConcern.addAll(commandLineParser.getAppClassPath());

					Set<MethodData> appMethodToRemove = new HashSet<MethodData>();
					appMethodToRemove.addAll(callGraphAnalysis.getAppMethods());
					appMethodToRemove.removeAll(callGraphAnalysis.getUsedAppMethods());

					for (MethodData methodToRemoveString : appMethodToRemove) {
						SootClass sootClass = Scene.v().loadClassAndSupport(methodToRemoveString.getClassName());
						if (!sootClass.isEnum() && sootClass.declaresMethod(methodToRemoveString.getSubSignature())) {
							SootMethod sootMethod = sootClass.getMethod(methodToRemoveString.getSubSignature());

							if (MethodWiper.wipeMethodAndInsertRuntimeException(sootMethod,
								getExceptionMessage(sootMethod))) {
								Application.removedMethods.add(SootUtils.sootMethodToMethodData(sootMethod));
								classesToRewrite.add(sootClass);
								appMethodsRemoved.add(methodToRemoveString);
							}
						}
					}
				}

				//Rewrite the modified classes
				modifyClasses(classesToRewrite,classPathsOfConcern);
			}

			System.out.println("number_lib_methods_removed," + libMethodsRemoved.size());
			System.out.println("number_app_methods_removed," + appMethodsRemoved.size());
		}

	}

	private static String getExceptionMessage(SootMethod sootMethod) {
		return "Method has been removed";
	}

	private static void modifyClasses(Set<SootClass> classesToRewrite, Set<File> classPaths){
		for (SootClass sootClass : classesToRewrite) {
			try {
				WritingClassUtils.writeClass(sootClass, classPaths);
			} catch (IOException e) {
				System.err.println("An exception was thrown when attempting to rewrite a class:");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private static Set<MethodData> getEntryPoints(ApplicationCommandLineParser commandLineParser) {
		Set<MethodData> toReturn = new HashSet<MethodData>();

		//Get the app methods
		Set<MethodData> appMethods = new HashSet<MethodData>();
		for (File appPath : commandLineParser.getAppClassPath()) {
			ASMUtils.readClass(appPath, new HashSet<String>(), appMethods);
		}

		//Get the test methods
		Set<MethodData> testMethods = new HashSet<MethodData>();
		for(File testPath :  commandLineParser.getTestClassPath()){
			ASMUtils.readClass(testPath, new HashSet<String>(), testMethods);
		}

		if (commandLineParser.includeMainEntryPoint()) {
			toReturn.addAll(EntryPointUtil.getMainMethodsAsEntryPoints(appMethods));
		}

		if (commandLineParser.includePublicEntryPoints()) {
			toReturn.addAll(EntryPointUtil.getPublicMethodsAsEntryPoints(appMethods));
		}

		if (commandLineParser.includeTestEntryPoints()) {
			toReturn.addAll(EntryPointUtil.getTestMethodsAsEntryPoints(testMethods));
		}

		toReturn.addAll(commandLineParser.getCustomEntryPoints());

		if(toReturn.isEmpty()){ //Error
			System.err.println("No entry points specified.");
			System.exit(1);
		}

		return toReturn;
	}

}
