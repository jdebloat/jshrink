package edu.ucla.cs.jshrinkapp;

import java.io.*;
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


		//Initialize the jShrink instance.
		JShrink jShrink = null;
		try {
			if(JShrink.instanceExists()){
				jShrink = JShrink.resetInstance(commandLineParser.getMavenDirectory().get(), entryPointProcessor,
					commandLineParser.getTamiflex(), commandLineParser.useSpark());
			} else {
				jShrink = JShrink.createInstance(commandLineParser.getMavenDirectory().get(), entryPointProcessor,
					commandLineParser.getTamiflex(), commandLineParser.useSpark());
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		assert (jShrink != null);

		jShrink.makeSootPass();

		if(commandLineParser.isVerbose()){
			System.out.println("app_size_before," + jShrink.getAppSize(true));
			System.out.println("libs_size_before," + jShrink.getLibSize(true));
		}

		Optional<TestOutput> testOutput = jShrink.getTestOutput();

		if(!testOutput.isPresent()){
			System.err.println("Cannot build/run tests for the target application.");
			System.exit(1);
		}

		testOutputBefore = jShrink.getTestOutput().get();
		if(commandLineParser.isTestOutput()){
			System.out.println("tests_run_before," + testOutputBefore.getRun());
			System.out.println("tests_errors_before," + testOutputBefore.getErrors());
			System.out.println("tests_failed_before," + testOutputBefore.getFailures());
			System.out.println("tests_skipped_before," + testOutputBefore.getSkipped());
			if(testOutputBefore.getErrors() > 0 || testOutputBefore.getFailures() > 0){
				System.out.println("Test Failure before processing. Output below: ");
				System.out.println(testOutputBefore.getTestOutputText());
			}
		}

		//Note the number of library and application methods and fields before and transformations.
		Set<MethodData> allAppMethodsBefore = jShrink.getAllAppMethods();
		Set<MethodData> allLibMethodsBefore = jShrink.getAllLibMethods();
		Set<FieldData> allAppFieldsBefore = jShrink.getAllAppFields();
		Set<FieldData> allLibFieldsBefore = jShrink.getAllLibFields();

		if(commandLineParser.isVerbose()){
			System.out.println("app_num_methods_before," + allAppMethodsBefore.size());
			System.out.println("libs_num_methods_before," + allLibMethodsBefore.size());
			System.out.println("app_num_fields_before," + allAppFieldsBefore.size());
			System.out.println("libs_num_fields_before," + allLibFieldsBefore.size());
		}

		//These two sets will be used to keep track of the application and library methods and fields removed.
		Set<MethodData> appMethodsRemoved = new HashSet<MethodData>();
		Set<MethodData> libMethodsRemoved = new HashSet<MethodData>();
		Set<FieldData> appFieldsRemoved = new HashSet<FieldData>();
		Set<FieldData> libFieldsRemoved = new HashSet<FieldData>();

		//Run the method inliner.
		if (commandLineParser.inlineMethods()) {
			inlineData = jShrink.inlineMethods(commandLineParser.isPruneAppInstance(), true);

			//Remove all the methods that have been inlined
			for(String methodInlined : inlineData.getInlineLocations().keySet()){
				try {
					MethodData toRemove = new MethodData(methodInlined);
					if (!jShrink.removeMethods(new HashSet<MethodData>(Arrays.asList(toRemove))
						,commandLineParser.removeClasses()).isEmpty()) {
						if (allAppMethodsBefore.contains(toRemove)) {
							appMethodsRemoved.add(toRemove);
						} else if (allLibMethodsBefore.contains(toRemove)) {
							libMethodsRemoved.add(toRemove);
						}
					}
				}catch(IOException e){
					e.printStackTrace();
					System.exit(1);
				}
			}

			removedClasses.addAll(jShrink.classesToRemove());
			jShrink.updateClassFiles();
		}

		//Run the class collapser.
		if (commandLineParser.collapseClasses()) {
			classCollapserData = jShrink.collapseClasses(commandLineParser.isPruneAppInstance(), true);

			//Update our sets to note what has been removed.
			appMethodsRemoved.addAll(classCollapserData.getRemovedMethods());
			libMethodsRemoved.addAll(classCollapserData.getRemovedMethods());
			appMethodsRemoved.retainAll(allAppMethodsBefore);
			libMethodsRemoved.retainAll(allLibMethodsBefore);

			removedClasses.addAll(jShrink.classesToRemove());
			jShrink.updateClassFiles();
		}

		//Run the method removal.
		if(!commandLineParser.isSkipMethodRemoval()) {
			Set<MethodData> appMethodsToRemove = new HashSet<MethodData>();
			Set<MethodData> libMethodsToRemove = new HashSet<MethodData>();
			libMethodsToRemove.addAll(jShrink.getAllLibMethods());
			libMethodsToRemove.removeAll(jShrink.getUsedLibMethods());
			if (commandLineParser.isPruneAppInstance()) {
				appMethodsToRemove.addAll(jShrink.getAllAppMethods());
				appMethodsToRemove.removeAll(jShrink.getUsedAppMethods());
			}


			if (commandLineParser.removeMethods()) {
				appMethodsRemoved.addAll(jShrink.removeMethods(appMethodsToRemove, commandLineParser.removeClasses()));
				libMethodsRemoved.addAll(jShrink.removeMethods(libMethodsToRemove, commandLineParser.removeClasses()));
				removedMethod = true;
			} else if (commandLineParser.includeException()) {
				appMethodsRemoved.addAll(jShrink.wipeMethodAndAddException(appMethodsToRemove,
					commandLineParser.getExceptionMessage()));
				libMethodsRemoved.addAll(jShrink.wipeMethodAndAddException(libMethodsToRemove,
					commandLineParser.getExceptionMessage()));
				if (commandLineParser.getExceptionMessage().isPresent()) {
					wipedMethodBodyWithExceptionAndMessage = true;
				} else {
					wipedMethodBodyWithExceptionNoMessage = true;
				}
			} else {
				appMethodsRemoved.addAll(jShrink.wipeMethods(appMethodsToRemove));
				libMethodsRemoved.addAll(jShrink.wipeMethods(libMethodsToRemove));
				wipedMethodBody = true;
			}

			removedClasses.addAll(jShrink.classesToRemove());
			jShrink.updateClassFiles();
		}

		if(commandLineParser.removedFields()) {
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

			jShrink.updateClassFiles();
		}

		if(commandLineParser.isVerbose()){
			System.out.println("app_num_methods_after," +
				(allAppMethodsBefore.size() - appMethodsRemoved.size()));
			System.out.println("libs_num_methods_after," +
				(allLibMethodsBefore.size() - libMethodsRemoved.size()));
			System.out.println("app_num_fields_after," +
					(allAppFieldsBefore.size() - appFieldsRemoved.size()));
			System.out.println("libs_num_fields_after," +
					(allLibFieldsBefore.size() - libFieldsRemoved.size()));
		}

		if(commandLineParser.isVerbose()){
			System.out.println("app_size_after," + jShrink.getAppSize(true));
			System.out.println("libs_size_after," + jShrink.getLibSize(true));
		}

		//TODO: Stuff like this is quite inefficient if we're not running a JUnit Test or having a verbose output.

		testOutput = jShrink.getTestOutput();
		if(!testOutput.isPresent()){
			testOutputAfter = new TestOutput(-1,-1,-1,-1, "");
		} else {
			testOutputAfter = testOutput.get();
		}
		if(commandLineParser.isTestOutput()){
			System.out.println("tests_run_after," + testOutputAfter.getRun());
			System.out.println("tests_errors_after," + testOutputAfter.getErrors());
			System.out.println("tests_failed_after," + testOutputAfter.getFailures());
			System.out.println("tests_skipped_after," + testOutputAfter.getSkipped());
			if(testOutputAfter.getErrors() > 0 || testOutputAfter.getFailures() > 0){
				System.out.println("Test Failure after processing. Output below: ");
				System.out.println(testOutputAfter.getTestOutputText());
			}
		}

		removedMethods.addAll(appMethodsRemoved);
		removedMethods.addAll(libMethodsRemoved);

		long endTime = System.nanoTime();
		if(commandLineParser.isVerbose()){
			System.out.println("time_elapsed," + TimeUnit.NANOSECONDS.toSeconds((endTime - startTime)));
		}
	}
}
