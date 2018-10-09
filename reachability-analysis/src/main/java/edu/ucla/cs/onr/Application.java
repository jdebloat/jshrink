package edu.ucla.cs.onr;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import edu.ucla.cs.onr.reachability.SparkCallGraphAnalysis;
import edu.ucla.cs.onr.util.SootUtils;
import edu.ucla.cs.onr.ApplicationCommandLineParser.ENTRY_POINT;
import edu.ucla.cs.onr.methodwiper.MethodWiper;

import org.apache.commons.io.FileUtils;

import org.apache.log4j.PropertyConfigurator;
import soot.*;
import soot.jimple.JasminClass;
import soot.options.Options;
import soot.util.JasminOutputStream;

public class Application {

	public static void main(String[] args) {

		PropertyConfigurator.configure(
			Application.class.getClassLoader().getResourceAsStream("log4j.properties"));

		//Load the command line arguments
		ApplicationCommandLineParser commandLineParser = null;

		try {
			commandLineParser = new ApplicationCommandLineParser(args);
		} catch (Exception e){
			System.err.println(e.getLocalizedMessage());
			System.exit(1);
		}

		assert(commandLineParser != null);

		//Get the entry points
		Set<String> entryPoints =  getEntryPoints(commandLineParser);

		//Run the call graph analysis
		SparkCallGraphAnalysis callGraphAnalysis = new SparkCallGraphAnalysis(commandLineParser.getLibClassPath(),
			commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath(),entryPoints);
		callGraphAnalysis.run();

		//Load the classes to Soot
		SootUtils.setup(commandLineParser.getLibClassPath(),
			commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath());

		Set<SootClass> classesToRewrite = new HashSet<SootClass>();

		//Remove the unused library methods
		Set<String> libMethodsToRemove = new HashSet<String>();
		libMethodsToRemove.addAll(callGraphAnalysis.getLibMethods());
		libMethodsToRemove.removeAll(callGraphAnalysis.getUsedLibMethods());

		for(String methodToRemoveString : libMethodsToRemove) {
			SootMethod sootMethod = Scene.v().getMethod(methodToRemoveString);
			MethodWiper.wipeMethodAndInsertRuntimeException(sootMethod, getExceptionMethod(sootMethod));
			classesToRewrite.add(sootMethod.getDeclaringClass());
		}

		//Remove the unused app methods (if applicable)
		if(commandLineParser.isPruneAppInstance()) {
			Set<String> appMethodToRemove = new HashSet<String>();
			appMethodToRemove.addAll(callGraphAnalysis.getAppMethods());
			appMethodToRemove.removeAll(callGraphAnalysis.getUsedAppMethods());

			for(String methodToRemoveString: appMethodToRemove){
				SootMethod sootMethod = Scene.v().getMethod(methodToRemoveString);
				MethodWiper.wipeMethodAndInsertRuntimeException(sootMethod, getExceptionMethod(sootMethod));
				classesToRewrite.add(sootMethod.getDeclaringClass());
			}
		}
	}

	private static void writeMethod(SootClass sootClass){
		//TODO: Will this work for jars?
		for(SootMethod sootMethod : sootClass.getMethods()){
			sootMethod.retrieveActiveBody();
		}

		try {
			String fileName = SourceLocator.v().getFileNameFor(sootClass, Options.output_format_class);

			File fileToReturn = new File(fileName);
			if(fileToReturn.exists()){ //We overwrite but we create a backup
				File copyLocation = new File(fileToReturn.getAbsolutePath() + "_original");
				FileUtils.copyFile(fileToReturn,copyLocation);
			}
			OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileToReturn));
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

			JasminClass jasminClass = new soot.jimple.JasminClass(sootClass);
			jasminClass.print(writerOut);
			writerOut.flush();
			streamOut.close();

		}catch(Exception e){
			System.err.println("Exception thrown: " + e.getMessage());
			System.exit(1);
		}

	}

	private static String getExceptionMethod(SootMethod sootMethod){
		return "Method '" + sootMethod.getSignature() +"' has been removed";
	}

	private static Set<String> getEntryPoints(ApplicationCommandLineParser commandLineParser){
		Set<String> toReturn = new HashSet<String>();

		SootUtils.setup(commandLineParser.getLibClassPath(),
			commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath());

		if(commandLineParser.getEntryPoint() == ENTRY_POINT.MAIN){
			SootClass mainClass = Scene.v().getMainClass();
			SootMethod mainMethod = mainClass.getMethodByName("main");
			toReturn.add(mainMethod.getSubSignature()); //TODO: Check name, is this right?
		} else if(commandLineParser.getEntryPoint() == ENTRY_POINT.PUBLIC){
			for(File appFile : commandLineParser.getAppClassPath()){
				SootClass sootClass = Scene.v().getSootClass(
					appFile.getName().replace(".class", ""));
				for(SootMethod sootMethod : sootClass.getMethods()){
					if(sootMethod.isPublic()) {
						toReturn.add(sootMethod.getSubSignature());
					}
				}
			}
		} else if(commandLineParser.getEntryPoint() == ENTRY_POINT.TESTS){
			//TODO: Need to complete this
			System.err.println("[Bobby R. Bruce] : I have not implemented the ability to declare entry-points via"
				+ " tests yet, sorry!");
			System.exit(1);
		} else { //Error
			System.err.println("ERROR: Unknown entry-point specified.");
			System.exit(1);
		}

		return toReturn;
	}

}
