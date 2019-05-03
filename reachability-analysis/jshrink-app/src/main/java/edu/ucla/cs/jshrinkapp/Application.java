package edu.ucla.cs.jshrinkapp;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

import edu.ucla.cs.jshrinklib.JShrink;
import edu.ucla.cs.jshrinklib.classcollapser.ClassCollapserData;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.methodinliner.InlineData;
import edu.ucla.cs.jshrinklib.reachability.*;

import fj.data.IO;
import org.apache.log4j.PropertyConfigurator;

public class Application {
	//I use this for testing to see if the correct methods have been removed.
	/*package*/ static final Set<MethodData> removedMethods = new HashSet<MethodData>();

	//I use this for testing to see if the correct classes have been removed.
	/*package*/ static final Set<String> removedClasses = new HashSet<String>();

	static final Set<FieldData> removedFields = new HashSet<FieldData>();

	//I use this for testing to see if the correct methods have been inlined.
	/*package*/ static InlineData inlineData = null;

	//I use this for testing to see if the correct methods, etc, have been collapsed.
	/*package*/ static ClassCollapserData classCollapserData = null;

	//I use the following for testing to ensure the right kind of method wipe has been used.
	/*package*/ static boolean removedMethod = false;
	/*package*/ static boolean wipedMethodBody = false;
	/*package*/ static boolean wipedMethodBodyWithExceptionNoMessage = false;
	/*package*/ static boolean wipedMethodBodyWithExceptionAndMessage = false;

	/*package*/ static TestOutput testOutputBefore = null;
	/*package*/ static TestOutput testOutputAfter = null;

	public static void main(String[] args) {

		long startTime = System.nanoTime();
		//Re-initialise this each time Application is run (for testing).
		removedMethods.clear();
		removedClasses.clear();
		removedFields.clear();
		inlineData = null;
		classCollapserData = null;
		removedMethod = false;
		wipedMethodBody = false;
		wipedMethodBodyWithExceptionNoMessage = false;
		wipedMethodBodyWithExceptionAndMessage = false;
		testOutputBefore = null;
		testOutputAfter = null;

		StringBuilder toLog = new StringBuilder();

		//I just put this in to stop an error.
		PropertyConfigurator.configure(
			Application.class.getClassLoader().getResourceAsStream("log4j.properties"));

		//Load the command line arguments.
		ApplicationCommandLineParser commandLineParser = null;

		try {
			commandLineParser = new ApplicationCommandLineParser(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		assert (commandLineParser != null);

		//TODO: Classes in which all methods are removed, and have no fields that are accessed, should be removed.

		EntryPointProcessor entryPointProcessor = new EntryPointProcessor(commandLineParser.includeMainEntryPoint(),
			commandLineParser.includePublicEntryPoints(),
			commandLineParser.includeTestEntryPoints(),
			true,
			commandLineParser.getCustomEntryPoints());

		// These can all be seen as TODOs for now.
		if (!commandLineParser.getMavenDirectory().isPresent()) {
			System.err.println("Sorry, we can only process Maven directories for now!");
			System.exit(1);
		}

		if(!commandLineParser.getClassesToIgnore().isEmpty()){
			System.err.println("Sorry, we do not support the \"classes to ignore\" functionality for now!");
			System.exit(1);
		}


		if(commandLineParser.isVerbose()){
			System.out.println("Creating jShrink instance...");
		}

		//Initialize the jShrink instance.
		JShrink jShrink = null;
		try {
			if(JShrink.instanceExists()){
				jShrink = JShrink.resetInstance(commandLineParser.getMavenDirectory().get(), entryPointProcessor,
					commandLineParser.getTamiflex(), commandLineParser.useSpark(), commandLineParser.isVerbose());
			} else {
				jShrink = JShrink.createInstance(commandLineParser.getMavenDirectory().get(), entryPointProcessor,
					commandLineParser.getTamiflex(), commandLineParser.useSpark(), commandLineParser.isVerbose());
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		assert (jShrink != null);

		if(commandLineParser.isVerbose()){
			System.out.println("Done creating jShrink instance!");
			System.out.println("Making \"soot pass\"...");
		}

		jShrink.makeSootPass();

		if(commandLineParser.isVerbose()){
			System.out.println("Done making \"soot pass\"!");
		}

		toLog.append("app_size_before," + jShrink.getAppSize(true) + System.lineSeparator());
		toLog.append("libs_size_before," + jShrink.getLibSize(true) + System.lineSeparator());


		testOutputBefore = jShrink.getTestOutput();
		if(!testOutputBefore.isTestBuildSuccess()){
			System.err.println("Cannot build tests for the target application (after \"soot pass\").");
			System.exit(1);
		}


		toLog.append("tests_run_before," + testOutputBefore.getRun() + System.lineSeparator());
		toLog.append("tests_errors_before," + testOutputBefore.getErrors() + System.lineSeparator());
		toLog.append("tests_failed_before," + testOutputBefore.getFailures() + System.lineSeparator());
		toLog.append("tests_skipped_before," + testOutputBefore.getSkipped() + System.lineSeparator());

		//Note the number of library and application methods and fields before and transformations.
		Set<MethodData> allAppMethodsBefore = jShrink.getAllAppMethods();
		Set<MethodData> allLibMethodsBefore = jShrink.getAllLibMethods();
		Set<FieldData> allAppFieldsBefore = jShrink.getAllAppFields();
		Set<FieldData> allLibFieldsBefore = jShrink.getAllLibFields();

		toLog.append("app_num_methods_before," + allAppMethodsBefore.size() + System.lineSeparator());
		toLog.append("libs_num_methods_before," + allLibMethodsBefore.size() + System.lineSeparator());
		toLog.append("app_num_fields_before," + allAppFieldsBefore.size() + System.lineSeparator());
		toLog.append("libs_num_fields_before," + allLibFieldsBefore.size() + System.lineSeparator());

		//These two sets will be used to keep track of the application and library methods and fields removed.
		Set<MethodData> appMethodsRemoved = new HashSet<MethodData>();
		Set<MethodData> libMethodsRemoved = new HashSet<MethodData>();
		Set<FieldData> appFieldsRemoved = new HashSet<FieldData>();
		Set<FieldData> libFieldsRemoved = new HashSet<FieldData>();

		//Run the method removal.
		if(!commandLineParser.isSkipMethodRemoval()) {
			if(commandLineParser.isVerbose()){
				System.out.println("Removing unused methods...");
			}
			Set<MethodData> appMethodsToRemove = new HashSet<MethodData>();
			Set<MethodData> libMethodsToRemove = new HashSet<MethodData>();
			libMethodsToRemove.addAll(jShrink.getAllLibMethods());
			libMethodsToRemove.removeAll(jShrink.getUsedLibMethods());
			if (commandLineParser.isPruneAppInstance()) {
				appMethodsToRemove.addAll(jShrink.getAllAppMethods());
				appMethodsToRemove.removeAll(jShrink.getUsedAppMethods());
			}

			// find all virtually invoked methods
			Set<MethodData> appVirtualMethodsToWipe = new HashSet<MethodData>();
			Set<MethodData> libVirtualMethodsToWipe = new HashSet<MethodData>();
			Set<MethodData> entryPoints = jShrink.getAllEntryPoints();
			Map<MethodData, Set<MethodData>> callGraph = jShrink.getSimplifiedCallGraph();
			for(MethodData m : jShrink.getUsedLibMethods()) {
				Set<MethodData> callers = callGraph.get(m);
				if(callers.isEmpty() && !entryPoints.contains(m)) {
					// this method is only invoked virtually
					libVirtualMethodsToWipe.add(m);
				}
			}
			if(commandLineParser.isPruneAppInstance()) {
				for(MethodData m : jShrink.getUsedAppMethods()) {
					Set<MethodData> callers = callGraph.get(m);
					if(callers.isEmpty() && !entryPoints.contains(m)) {
						// this method is only invoked virtually
						appVirtualMethodsToWipe.add(m);
					}
				}
			}

			if (commandLineParser.removeMethods()) {
				appMethodsRemoved.addAll(jShrink.removeMethods(appMethodsToRemove, commandLineParser.removeClasses()));
				libMethodsRemoved.addAll(jShrink.removeMethods(libMethodsToRemove, commandLineParser.removeClasses()));

				// wipe the body of virtually invoked methods, keep their method headers
				appMethodsRemoved.addAll(jShrink.wipeMethods(appVirtualMethodsToWipe));
				appMethodsRemoved.addAll(jShrink.wipeMethods(libVirtualMethodsToWipe));

				removedMethod = true;
			} else if (commandLineParser.includeException()) {
				appMethodsRemoved.addAll(jShrink.wipeMethodAndAddException(appMethodsToRemove,
					commandLineParser.getExceptionMessage()));
				libMethodsRemoved.addAll(jShrink.wipeMethodAndAddException(libMethodsToRemove,
					commandLineParser.getExceptionMessage()));

				appMethodsRemoved.addAll(jShrink.wipeMethodAndAddException(appVirtualMethodsToWipe,
						commandLineParser.getExceptionMessage()));
				appMethodsRemoved.addAll(jShrink.wipeMethodAndAddException(libVirtualMethodsToWipe,
						commandLineParser.getExceptionMessage()));

				if (commandLineParser.getExceptionMessage().isPresent()) {
					wipedMethodBodyWithExceptionAndMessage = true;
				} else {
					wipedMethodBodyWithExceptionNoMessage = true;
				}
			} else {
				appMethodsRemoved.addAll(jShrink.wipeMethods(appMethodsToRemove));
				libMethodsRemoved.addAll(jShrink.wipeMethods(libMethodsToRemove));

				appMethodsRemoved.addAll(jShrink.wipeMethods(appVirtualMethodsToWipe));
				appMethodsRemoved.addAll(jShrink.wipeMethods(libVirtualMethodsToWipe));

				wipedMethodBody = true;
			}

			removedClasses.addAll(jShrink.classesToRemove());

			if(commandLineParser.isVerbose()){
				System.out.println("Done removing unused methods!");
			}

			//Run the field removal
			if(commandLineParser.removedFields()) {
				if(commandLineParser.isVerbose()){
					System.out.println("Removing unused fields...");
				}
				Set<FieldData> libFieldsToRemove = new HashSet<FieldData>();
				libFieldsToRemove.addAll(jShrink.getAllLibFields());
				libFieldsToRemove.removeAll(jShrink.getUsedLibFields());
				libFieldsRemoved.addAll(jShrink.removeFields(libFieldsToRemove));
				removedFields.addAll(libFieldsRemoved);

				if(commandLineParser.isPruneAppInstance()) {
					Set<FieldData> appFieldsToRemove = new HashSet<FieldData>();
					appFieldsToRemove.addAll(jShrink.getAllAppFields());
					appFieldsToRemove.removeAll(jShrink.getUsedAppFields());
					appFieldsRemoved.addAll(jShrink.removeFields(appFieldsToRemove));
					removedFields.addAll(appFieldsRemoved);
				}

				if(commandLineParser.isVerbose()){
					System.out.println("Done removing unused fields!");
				}
			}

			//Run the class collapser.
			if (commandLineParser.collapseClasses()) {
				if(commandLineParser.isVerbose()){
					System.out.println("Collapsing collapsable classes...");
				}
				classCollapserData = jShrink.collapseClasses(commandLineParser.isPruneAppInstance(), true);

				//Update our sets to note what has been removed.
				appMethodsRemoved.addAll(classCollapserData.getRemovedMethods());
				libMethodsRemoved.addAll(classCollapserData.getRemovedMethods());
				appMethodsRemoved.retainAll(allAppMethodsBefore);
				libMethodsRemoved.retainAll(allLibMethodsBefore);

				removedClasses.addAll(jShrink.classesToRemove());
				if(commandLineParser.isVerbose()){
					System.out.println("Done collapsing collapsable classes!");
				}
			}

			// filter out unmodifiable classes after debloating
			filterUnmodifiableClassesAfterDebloating(jShrink, appMethodsRemoved,
					libMethodsRemoved, appFieldsRemoved, libFieldsRemoved);

			jShrink.updateClassFiles();
		}


		//Run the method inliner.
		if (commandLineParser.inlineMethods()) {
			if(commandLineParser.isVerbose()){
				System.out.println("Inlining inlinable methods...");
			}
			inlineData = jShrink.inlineMethods(commandLineParser.isPruneAppInstance(), true);

			//Remove all the methods that have been inlined
			for(MethodData methodInlined : inlineData.getInlineLocations().keySet()){
				if (!jShrink.removeMethods(new HashSet<MethodData>(Arrays.asList(methodInlined))
					,commandLineParser.removeClasses()).isEmpty()) {
					if (allAppMethodsBefore.contains(methodInlined)) {
						appMethodsRemoved.add(methodInlined);
					} else if (allLibMethodsBefore.contains(methodInlined)) {
						libMethodsRemoved.add(methodInlined);
					}
				}
			}

			removedClasses.addAll(jShrink.classesToRemove());
			if(commandLineParser.isVerbose()){
				System.out.println("Done inlining inlinable methods!");
			}

			filterUnmodifiableClassesAfterDebloating(jShrink, appMethodsRemoved,
					libMethodsRemoved, appFieldsRemoved, libFieldsRemoved);

			jShrink.updateClassFiles();
		}

		toLog.append("app_num_methods_after," +
			(allAppMethodsBefore.size() - appMethodsRemoved.size()) + System.lineSeparator());
		toLog.append("libs_num_methods_after," +
			(allLibMethodsBefore.size() - libMethodsRemoved.size()) + System.lineSeparator());
		toLog.append("app_num_fields_after," +
				(allAppFieldsBefore.size() - appFieldsRemoved.size()) + System.lineSeparator());
		toLog.append("libs_num_fields_after," +
				(allLibFieldsBefore.size() - libFieldsRemoved.size()) + System.lineSeparator());
		toLog.append("app_size_after," + jShrink.getAppSize(true) + System.lineSeparator());
		toLog.append("libs_size_after," + jShrink.getLibSize(true) + System.lineSeparator());

		testOutputAfter = jShrink.getTestOutput();


		toLog.append("tests_run_after," + testOutputAfter.getRun() + System.lineSeparator());
		toLog.append("tests_errors_after," + testOutputAfter.getErrors() + System.lineSeparator());
		toLog.append("tests_failed_after," + testOutputAfter.getFailures() + System.lineSeparator());
		toLog.append("tests_skipped_after," + testOutputAfter.getSkipped() + System.lineSeparator());

		removedMethods.addAll(appMethodsRemoved);
		removedMethods.addAll(libMethodsRemoved);

		long endTime = System.nanoTime();
		toLog.append("time_elapsed," + TimeUnit.NANOSECONDS.toSeconds((endTime - startTime)) + System.lineSeparator());

		outputToLogDirectory(commandLineParser.getLogDirectory(), toLog.toString(),
			testOutputBefore.getTestOutputText(), testOutputAfter.getTestOutputText());

		if(commandLineParser.isVerbose()){
			System.out.println("Output logging info to \"" + commandLineParser.getLogDirectory() + "\".");
		}
	}

	private static void filterUnmodifiableClassesAfterDebloating(JShrink jShrink, Set<MethodData> appMethodsRemoved,
																 Set<MethodData> libMethodsRemoved,
																 Set<FieldData> appFieldsRemoved,
																 Set<FieldData> libFieldsRemoved) {
		Set<String> classes = jShrink.filterUnmodifiableClass();
		HashSet<MethodData> methodsNotRemoved = new HashSet<MethodData>();
		for(MethodData removedMethod : appMethodsRemoved) {
			if(classes.contains(removedMethod.getClassName())) {
				methodsNotRemoved.add(removedMethod);
			}
		}
		appMethodsRemoved.removeAll(methodsNotRemoved);
		methodsNotRemoved.clear();
		for(MethodData removedMethod : libMethodsRemoved) {
			if(classes.contains(removedMethod.getClassName())) {
				methodsNotRemoved.add(removedMethod);
			}
		}
		libMethodsRemoved.removeAll(methodsNotRemoved);
		methodsNotRemoved.clear();
		HashSet<FieldData> fieldsNotRemoved = new HashSet<FieldData>();
		for(FieldData removedField : appFieldsRemoved) {
			if(classes.contains(removedField.getClassName())) {
				fieldsNotRemoved.add(removedField);
			}
		}
		appFieldsRemoved.removeAll(fieldsNotRemoved);
		fieldsNotRemoved.clear();
		for(FieldData removedField : libFieldsRemoved) {
			if(classes.contains(removedField.getClassName())) {
				fieldsNotRemoved.add(removedField);
			}
		}
		libFieldsRemoved.removeAll(fieldsNotRemoved);
		fieldsNotRemoved.clear();
	}

	private static void outputToLogDirectory(File directory, String log,
	                                         String testOutputBefore, String testOutputAfter){

		try {
			FileWriter fileWriter =
				new FileWriter(directory.getAbsolutePath() + File.separator + "log.dat");
			fileWriter.write(log);
			fileWriter.close();

			fileWriter =
				new FileWriter(directory.getAbsolutePath() + File.separator + "test_output_before.dat");
			fileWriter.write(testOutputBefore);
			fileWriter.close();

			fileWriter =
				new FileWriter(directory.getAbsolutePath() + File.separator + "test_output_after.dat");
			fileWriter.write(testOutputAfter);
			fileWriter.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
