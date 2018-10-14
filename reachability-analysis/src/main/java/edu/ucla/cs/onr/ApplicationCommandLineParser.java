package edu.ucla.cs.onr;

import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ApplicationCommandLineParser {

	private final List<File> libClassPath;
	private final List<File> appClassPath;
	private final List<File> testClassPath;
	private final boolean pruneApp;
	private final boolean mainEntryPoint;
	private final boolean publicEntryPoints;
	private final boolean testEntryPoints;
	private final boolean debug;
	private final boolean verbose;


	public ApplicationCommandLineParser(String[] args) throws FileNotFoundException, ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine commandLine = null;

		try{
			commandLine = parser.parse(ApplicationCommandLineParser.getOptions(), args);
		} catch (ParseException e){
			throw new ParseException("Could not parse arguments" + System.lineSeparator()
			+ "Exception information:" + System.lineSeparator() + e.getLocalizedMessage());
		}

		assert(commandLine != null);

		this.pruneApp = commandLine.hasOption("p");

		if(commandLine.hasOption("a")){
			appClassPath = pathToFiles(commandLine.getOptionValue("a"));
		} else {
			appClassPath = new ArrayList<File>();
		}

		if(commandLine.hasOption('l')){
			libClassPath = pathToFiles(commandLine.getOptionValue("l"));
		} else {
			libClassPath = new ArrayList<File>();
		}

		if(commandLine.hasOption('t')){
			testClassPath = pathToFiles(commandLine.getOptionValue("t"));
		} else {
			testClassPath = new ArrayList<File>();
		}

		debug = commandLine.hasOption('d');

		verbose = commandLine.hasOption('v');

		mainEntryPoint = commandLine.hasOption('m');
		publicEntryPoints = commandLine.hasOption('u');
		testEntryPoints = commandLine.hasOption('s');

		if(!mainEntryPoint && !publicEntryPoints && ! testEntryPoints){
			throw new ParseException("No entry point was specified");
		}

	}

	private static List<File> pathToFiles(String path) throws FileNotFoundException {
		List<File> toReturn = new ArrayList<File>();

		String[] filePaths = path.split(File.pathSeparator);

		for(String f : filePaths){
			File toAdd = new File(f);
			if(!toAdd.exists()){
				throw new FileNotFoundException("Input path entry '" + f + "' does not exist");
			}
			toReturn.add(toAdd);
		}

		return toReturn;
	}

	private static Options getOptions(){

		Option pruneAppOption = Option.builder("p")
			.desc("Prune the application classes as well")
			.longOpt("prune-app")
			.hasArg(false)
			.required(false)
			.build();

		Option libClassPathOption = Option.builder("l")
			.desc("The library classpath")
			.longOpt("lib-classpath")
			.hasArg(true)
			.required(false)
			.optionalArg(false)
			.build();

		Option appClassPathOption = Option.builder("a")
			.desc("The application classpath")
			.longOpt("app-classpath")
			.hasArg(true)
			.required(false)
			.optionalArg(false)
			.build();

		Option testClassPathOption = Option.builder("t")
			.desc("The test classpath")
			.longOpt("test-classpath")
			.hasArg(true)
			.required(false)
			.optionalArg(false)
			.build();

		Option mainEntryPointOption = Option.builder("m")
			.desc("Include the main method as the entry point")
			.longOpt("main-entry")
			.hasArg(false)
			.required(false)
			.build();

		Option publicEntryPointOption = Option.builder("u")
			.desc("Include public methods as entry points")
			.longOpt("public-entry")
			.hasArg(false)
			.required(false)
			.build();

		Option  testEntryPointOption = Option.builder("s")
			.desc("Include the test methods as entry points")
			.longOpt("test-entry")
			.hasArg(false)
			.required(false)
			.build();

		Option debugOption = Option.builder("d")
			.desc("Run the program in 'debug' mode. Used for testing")
			.longOpt("debug")
			.hasArg(false)
			.required(false)
			.build();

		Option verboseMove = Option.builder("v")
			.desc("Run the program in 'verbose' mode. Useful for debugging")
			.longOpt("verbose")
			.hasArg(false)
			.required(false)
			.build();

		Options toReturn = new Options();
		toReturn.addOption(libClassPathOption);
		toReturn.addOption(appClassPathOption);
		toReturn.addOption(testClassPathOption);
		toReturn.addOption(mainEntryPointOption);
		toReturn.addOption(publicEntryPointOption);
		toReturn.addOption(testEntryPointOption);
		toReturn.addOption(pruneAppOption);
		toReturn.addOption(debugOption);
		toReturn.addOption(verboseMove);

		return toReturn;
	}

	public List<File> getAppClassPath() {
		return appClassPath;
	}

	public List<File> getLibClassPath() {
		return libClassPath;
	}

	public List<File> getTestClassPath() {
		return testClassPath;
	}

	public boolean isPruneAppInstance() {
		return pruneApp;
	}

	public boolean isDebug(){
		return debug;
	}

	public boolean isVerbose(){
		return verbose;
	}

	public boolean includeMainEntryPoint(){
		return mainEntryPoint;
	}

	public boolean includePublicEntryPoints(){
		return publicEntryPoints;
	}

	public boolean includeTestEntryPoints(){
		return testEntryPoints;
	}
}
