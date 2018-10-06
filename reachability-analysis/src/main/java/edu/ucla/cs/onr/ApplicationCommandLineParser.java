package edu.ucla.cs.onr;

import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ApplicationCommandLineParser {

	enum ENTRY_POINT{
		MAIN,
		PUBLIC,
		TESTS
	}

	private final List<File> libClassPath;
	private final List<File> appClassPath;
	private final List<File> testClassPath;
	private final boolean pruneApp;
	private final ENTRY_POINT entryPoint;


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
		String entryPointString = commandLine.getOptionValue('e').toLowerCase().trim();

		if(entryPointString.equals("main")){
			entryPoint = ENTRY_POINT.MAIN;
		} else if(entryPointString.equals("public")){
			entryPoint = ENTRY_POINT.PUBLIC;
		} else if(entryPointString.equals("tests")){
			entryPoint = ENTRY_POINT.TESTS;
		} else {
			throw new ParseException(
				"Could not parse argument for 'entry-point' option. Must be 'main', 'public', or 'tests'");
		}

		if(commandLine.hasOption("a")){
			appClassPath = pathToFiles(commandLine.getOptionValue("a"));
		} else {
			appClassPath = new ArrayList<File>();
		}

		if(commandLine.hasOption('l')){
			libClassPath = pathToFiles(commandLine.getOptionValue("a"));
		} else {
			libClassPath = new ArrayList<File>();
		}

		if(commandLine.hasOption('t')){
			testClassPath = pathToFiles(commandLine.getOptionValue("a"));
		} else {
			testClassPath = new ArrayList<File>();
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
			.hasArg(true)
			.build();

		Option entryPointOption = Option.builder("e")
			.desc("The entry point option ('main', 'public', or 'tests'")
			.longOpt("entry-point")
			.hasArg(true)
			.optionalArg(false)
			.required()
			.build();


		Options toReturn = new Options();
		toReturn.addOption(libClassPathOption);
		toReturn.addOption(appClassPathOption);
		toReturn.addOption(testClassPathOption);
		toReturn.addOption(entryPointOption);
		toReturn.addOption(pruneAppOption);

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

	public ENTRY_POINT getEntryPoint() {
		return entryPoint;
	}


}
