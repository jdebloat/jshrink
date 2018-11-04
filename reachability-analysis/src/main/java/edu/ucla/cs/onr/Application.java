package edu.ucla.cs.onr;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

import edu.ucla.cs.onr.reachability.*;
import edu.ucla.cs.onr.util.SootUtils;
import edu.ucla.cs.onr.methodwiper.MethodWiper;
import edu.ucla.cs.onr.util.ClassFileUtils;

import org.apache.log4j.PropertyConfigurator;
import soot.*;

public class Application {

	private static boolean DEBUG_MODE = true; //Enabled by default, needed for testing
	private static boolean VERBOSE_MODE = false;
	private static Set<File> decompressedJars = new HashSet<File>();

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
		decompressedJars.clear();

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

		IProjectAnalyser projectAnalyser = null;

		EntryPointProcessor entryPointProcessor = new EntryPointProcessor(commandLineParser.includeMainEntryPoint(),
				commandLineParser.includePublicEntryPoints(), commandLineParser.includeTestEntryPoints(),
				commandLineParser.getCustomEntryPoints());

		if(commandLineParser.getMavenDirectory().isPresent()){
			projectAnalyser =  new MavenSingleProjectAnalyzer(
					commandLineParser.getMavenDirectory().get().getAbsolutePath(), entryPointProcessor);
		} else {
			projectAnalyser =  new CallGraphAnalysis(commandLineParser.getLibClassPath(),
					commandLineParser.getAppClassPath(), commandLineParser.getTestClassPath(), entryPointProcessor);
		}

		assert(projectAnalyser != null);

		projectAnalyser.setup();

		try {

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

			extractJars(projectAnalyser.getAppClasspaths());
			extractJars(projectAnalyser.getLibClasspaths());
			extractJars(projectAnalyser.getTestClasspaths());

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

			if(commandLineParser.removeMethods() && !commandLineParser.methodsToRemove().isEmpty()){
				/*
				Our application has two modes. In this mode, the methods to be removed are specified at the command-line
				level. The call graph analysis is not run, and the methods are directly wiped.
				 */
				Set<File> classPathsOfConcern = new HashSet<File>();
				classPathsOfConcern.addAll(projectAnalyser.getAppClasspaths());
				classPathsOfConcern.addAll(projectAnalyser.getLibClasspaths());
				classPathsOfConcern.addAll(projectAnalyser.getTestClasspaths());

				SootUtils.setup_trimming(projectAnalyser.getLibClasspaths(),
						projectAnalyser.getAppClasspaths(),projectAnalyser.getTestClasspaths());
				Scene.v().loadNecessaryClasses();

				Set<SootClass> classesToRewrite = new HashSet<SootClass>();



				for(MethodData methodData : commandLineParser.methodsToRemove()){
					SootClass sootClass = Scene.v().loadClassAndSupport(methodData.getClassName());

					if(!sootClass.isEnum() && sootClass.declaresMethod(methodData.getSubSignature())) {
						SootMethod sootMethod = sootClass.getMethod(methodData.getSubSignature());
						if (MethodWiper.wipeMethodAndInsertRuntimeException(sootMethod,
							Application.getExceptionMessage())) {
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
				projectAnalyser.run();

				G.reset();
				SootUtils.setup_trimming(projectAnalyser.getLibClasspaths(),
						projectAnalyser.getAppClasspaths(),projectAnalyser.getTestClasspaths());
				Scene.v().loadNecessaryClasses();


				if(Application.isVerboseMode()) {
					for (MethodData method : projectAnalyser.getLibMethods()) {
						System.out.println("lib_method," + method.toString());
					}

					for (MethodData method : projectAnalyser.getAppMethods()) {
						System.out.println("app_method," + method.toString());
					}

					for (MethodData method : projectAnalyser.getUsedLibMethods()) {
						System.out.println("lib_method_touched," + method.toString());
					}

					for (MethodData method : projectAnalyser.getUsedAppMethods()) {
						System.out.println("app_method_touched," + method.toString());
					}
				}

				Set<MethodData> libMethodsRemoved = new HashSet<MethodData>();
				Set<MethodData> appMethodsRemoved = new HashSet<MethodData>();

				if(commandLineParser.removeMethods()) {
					Set<SootClass> classesToRewrite = new HashSet<SootClass>(); //Take note of all classes that have changed
					Set<File> classPathsOfConcern = new HashSet<File>(); //The classpaths where these classes can be found

					//Remove the unused library methods and classes
					Set<MethodData> libMethodsToRemove = new HashSet<MethodData>();
					libMethodsToRemove.addAll(projectAnalyser.getLibMethods());
					libMethodsToRemove.removeAll(projectAnalyser.getUsedLibMethods());

					classPathsOfConcern.addAll(projectAnalyser.getLibClasspaths());

					for (MethodData methodToRemoveString :libMethodsToRemove) {
						SootClass sootClass = Scene.v().loadClassAndSupport(methodToRemoveString.getClassName());
						if (!sootClass.isEnum() && sootClass.declaresMethod(methodToRemoveString.getSubSignature())) {
                            SootMethod sootMethod = sootClass.getMethod(methodToRemoveString.getSubSignature());
                            if (MethodWiper.wipeMethodAndInsertRuntimeException
                                    (sootMethod, getExceptionMessage())) {
                                Application.removedMethods.add(SootUtils.sootMethodToMethodData(sootMethod));
                                classesToRewrite.add(sootClass);
                                libMethodsRemoved.add(methodToRemoveString);
                            }
                        }
					}

					//Remove the unused app methods (if applicable)
					if (commandLineParser.isPruneAppInstance()) {
						classPathsOfConcern.addAll(projectAnalyser.getAppClasspaths());

						Set<MethodData> appMethodToRemove = new HashSet<MethodData>();
						appMethodToRemove.addAll(projectAnalyser.getAppMethods());
						appMethodToRemove.removeAll(projectAnalyser.getUsedAppMethods());

						for (MethodData methodToRemoveString : appMethodToRemove) {
							SootClass sootClass = Scene.v().loadClassAndSupport(methodToRemoveString.getClassName());
							if (!sootClass.isEnum() && sootClass.declaresMethod(methodToRemoveString.getSubSignature())) {
								SootMethod sootMethod = sootClass.getMethod(methodToRemoveString.getSubSignature());
								if (MethodWiper.wipeMethodAndInsertRuntimeException(sootMethod,
									getExceptionMessage())) {
									Application.removedMethods.add(SootUtils.sootMethodToMethodData(sootMethod));
									classesToRewrite.add(sootClass);
									appMethodsRemoved.add(methodToRemoveString);
								}
							}
						}
					}

					modifyClasses(classesToRewrite,classPathsOfConcern);
				}

				if(Application.isVerboseMode()) {
					System.out.println("number_lib_methods_removed," + libMethodsRemoved.size());
					System.out.println("number_app_methods_removed," + appMethodsRemoved.size());
				}
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
				compressJars();
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

	private static void extractJars(List<File> classPaths) throws IOException{
		for(File file : new HashSet<File>(classPaths)){
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(file);
			} catch (IOException e) {
				continue;
			}

			assert(jarFile != null);

			ClassFileUtils.decompressJar(file);
			decompressedJars.add(file);
		}
	}

	private static void compressJars() throws IOException {
		for(File file : decompressedJars){
			if(!file.exists()){
				System.out.println("File '" + file.getAbsolutePath() + "' does not exist");
			} else if(!file.isDirectory()){
				System.out.println("File '" + file.getAbsolutePath() + "' is not a directory");
			}
			assert(file.exists() && file.isDirectory());
			ClassFileUtils.compressJar(file);
		}
	}
}
