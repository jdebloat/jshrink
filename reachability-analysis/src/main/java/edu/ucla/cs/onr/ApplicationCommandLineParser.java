package edu.ucla.cs.onr;

import edu.ucla.cs.onr.reachability.MethodData;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class ApplicationCommandLineParser {

	private final static String APPLICATION_STATUS = "[APP]";

	private final List<File> libClassPath;
	private final List<File> appClassPath;
	private final List<File> testClassPath;
	private final boolean pruneApp;
	private final boolean mainEntryPoint;
	private final boolean publicEntryPoints;
	private final boolean testEntryPoints;
	private final boolean debug;
	private final boolean verbose;
	private final Set<MethodData> customEntryPoints;
	private final boolean doRemoveMethods;


	private static void printHelp(CommandLine commandLine){
		HelpFormatter helpFormatter = new HelpFormatter();
		String header = "An application to get the call-graph analysis of an application and to wipe unused methods";
		String footer = "";

		helpFormatter.printHelp(APPLICATION_STATUS, header, getOptions(),footer, true);
		System.out.println();
	}

	public ApplicationCommandLineParser(String[] args) throws FileNotFoundException, ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine commandLine = null;

		try{
			commandLine = parser.parse(ApplicationCommandLineParser.getOptions(), args);
		} catch (ParseException e){
			printHelp(commandLine);
			throw new ParseException("Could not parse arguments" + System.lineSeparator()
			+ "Exception information:" + System.lineSeparator() + e.getLocalizedMessage());
		}

		if(commandLine.hasOption('h')){
			printHelp(commandLine);
			System.exit(1);
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

		customEntryPoints = new HashSet<MethodData>();
		if(commandLine.hasOption('c')){
			String[] values = commandLine.getOptionValues('c');
			StringBuilder toAdd = new StringBuilder();
			for(String val : values){
				/*
				Due to the weird way the Apache Commons CLI library works, i need to stitch
				together the strings as they may contain spaces
				 */
				if(val.endsWith(">")){
					toAdd.append(val);
					try {
						customEntryPoints.add(new MethodData(toAdd.toString()));
					} catch(IOException e){
						printHelp(commandLine);
						throw new ParseException("Could not create method from input string " +
							"'" + toAdd.toString() + "' Exception thrown:"
							+ System.lineSeparator() + e.getLocalizedMessage());
					}
					toAdd = new StringBuilder();
				} else {
					toAdd.append(val +" ");
				}

			}
		}

		if(!mainEntryPoint && !publicEntryPoints && ! testEntryPoints && customEntryPoints.isEmpty()){
			printHelp(commandLine);
			throw new ParseException("No entry point was specified");
		}

		this.doRemoveMethods = commandLine.hasOption('r');
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

		Option customEntryPointOption = Option.builder("c")
			.desc("Specify custom entry points in syntax of " +
				"'<[classname]:[public?] [static?] [returnType] [methodName]([args...?])>'")
			.longOpt("custom-entry")
			.hasArgs()
			.valueSeparator()
			.required(false)
			.build();

		Option debugOption = Option.builder("d")
			.desc("Run the program in 'debug' mode. Used for testing")
			.longOpt("debug")
			.hasArg(false)
			.required(false)
			.build();

		Option verboseMove = Option.builder("v")
			.desc("Run the program in 'verbose' mode. Outputs methods analysed and methods touched")
			.longOpt("verbose")
			.hasArg(false)
			.required(false)
			.build();

		Option removeMethodsOption = Option.builder("r")
			.desc("Run remove the untouched methods")
			.longOpt("remove-methods")
			.hasArg(false)
			.required(false)
			.build();

		Option helpOption = Option.builder("h")
			.desc("Help")
			.longOpt("help")
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
		toReturn.addOption(customEntryPointOption);
		toReturn.addOption(debugOption);
		toReturn.addOption(verboseMove);
		toReturn.addOption(removeMethodsOption);
		toReturn.addOption(helpOption);

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

	public Set<MethodData> getCustomEntryPoints(){
		return Collections.unmodifiableSet(customEntryPoints);
	}

	public boolean removeMethods(){
		return doRemoveMethods;
	}
}
