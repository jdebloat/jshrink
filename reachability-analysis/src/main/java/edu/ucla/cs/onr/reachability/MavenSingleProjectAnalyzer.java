package edu.ucla.cs.onr.reachability;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;

import soot.G;
import edu.ucla.cs.onr.Application;
import edu.ucla.cs.onr.util.MavenUtils;

/**
 * 
 * This class runs reachability analysis on a single maven project and 
 * handles submodules if any
 * 
 * It is only designed for the experiment purpose. Make sure to do two
 * things before running it
 * 
 * 	1. The maven project is successfully built
 * 
 *  2. The classpath(s) of the maven project are successfully resolved
 *     and logged into a 'onr_classpath_new.log' file in the root dir.
 *     If you haven't done this, run resolve_classpath.sh in the script 
 *     folder first
 * 
 * @author Tianyi Zhang
 *
 */
public class MavenSingleProjectAnalyzer implements IProjectAnalyser {
	public static boolean useTamiFlex = false; // do not run TamiFlex by default
	
	private String project_path;
	
	private final Set<String> libClasses;
	private final Set<MethodData> libMethods;
	private final Set<String> appClasses;
	private final Set<MethodData> appMethods;
	private final Set<String> usedLibClasses;
	private final Set<MethodData> usedLibMethods;
	private final Set<String> usedAppClasses;
	private final Set<MethodData> usedAppMethods;
	private final Map<String,List<File>> app_class_paths;
	private final Map<String,List<File>> app_test_paths;
	private final Map<String,List<File>> lib_class_paths;
	private final HashMap<String, String> classpaths;
	private final EntryPointProcessor entryPointProcessor;
	private final Set<MethodData> entryPoints;
	
	public MavenSingleProjectAnalyzer(String pathToMavenProject, EntryPointProcessor entryPointProc) {
		project_path = pathToMavenProject;
		
		libClasses = new HashSet<String>();
		libMethods = new HashSet<MethodData>();
		appClasses = new HashSet<String>();
		appMethods = new HashSet<MethodData>();
		usedLibClasses = new HashSet<String>();
		usedLibMethods = new HashSet<MethodData>();
		usedAppClasses = new HashSet<String>();
		usedAppMethods = new HashSet<MethodData>();
		app_class_paths = new HashMap<String, List<File>>();
		app_test_paths = new HashMap<String, List<File>>();
		lib_class_paths = new HashMap<String, List<File>>();
		classpaths = new HashMap<String, String>();
		entryPointProcessor = entryPointProc;
		entryPoints = new HashSet<MethodData>();
	}

	public void cleanup(){
		//This is just used for cleaning up after testing. It just runs "mvn clean"
		File pomFile = new File(project_path + File.separator + "pom.xml");
		File libsDir = new File(project_path + File.separator + "libs");

		try{
			Process process = Runtime.getRuntime().exec("mvn -f " + pomFile.getAbsolutePath() + " " +
					"clean");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String maven_log = "";
			String line;
			while((line=reader.readLine()) != null) {
				maven_log += line + System.lineSeparator();
			}
			reader.close();
			
			if(maven_log.contains("BUILD FAILURE")) {
				System.err.println("'mvn clean' fails.");
			}

			try {
				FileUtils.forceDelete(libsDir);
			}catch(FileNotFoundException e){
				//Do nothing
			}

		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void setup(){
		File root_dir = new File(project_path);

		// find all submodules if any
		HashMap<String, File> modules = new HashMap<String, File>();
		MavenUtils.getModules(root_dir, modules);

		// get all classpaths for submodules if any
		String classpathInfo = "";

		try {
			 File pomFile = new File(root_dir + File.separator + "pom.xml");
			 File libsDir = new File(root_dir + File.separator + "libs");

			// Ensure the project is compiled.
			// Prepare the command and its arguments in a String array in case there is a space or special 
			// character in the pom file path or lib dir path.
			String[] cmd = new String[] {"mvn", "-f", pomFile.getAbsolutePath(), "install", 
					"-Dmaven.repo.local=" + libsDir.getAbsolutePath(), "--quiet", "--batch-mode", "-fn"};
			Process process1 = Runtime.getRuntime().exec(cmd);
			process1.waitFor();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process1.getInputStream()));
			
			String line;
			String maven_log = "";
			while((line=reader.readLine()) != null) {
				maven_log += line + System.lineSeparator();
			}
			reader.close();
			
			if(maven_log.contains("BUILD FAILURE")) {
				System.err.println("'mvn install' fails.");
			}

			// Get the classpath information
			// We cannot only get the classpath of libraries in the compile scope, since it will make our
			// call graph incomplete when using test cases as entry points
			String[] cmd2 = new String[] {"mvn", "-f", pomFile.getAbsolutePath(), "dependency:build-classpath",
					"-Dmaven.repo.local=" + libsDir.getAbsolutePath(), "--batch-mode"};
			Process process2 = Runtime.getRuntime().exec(cmd2);
			process2.waitFor();
			
			// Process process2 = Runtime.getRuntime().exec("mvn -f " + pomFile.getAbsolutePath() +
			//		" dependency:build-classpath " + "-Dmaven.repo.local=" + libsDir.getAbsolutePath() +
			//		" -DincludeScope=compile --batch-mode");

			reader = new BufferedReader(new InputStreamReader(process2.getInputStream()));
			while((line=reader.readLine()) != null){
				classpathInfo += line + System.lineSeparator();
			}
			reader.close();
			
			if(classpathInfo.contains("BUILD FAILURE")) {
				System.err.println("'mvn dependency:build-classpath' fails.");
			}
		}catch(IOException | InterruptedException e){
			e.printStackTrace();
			System.exit(1);
		}


		classpaths.putAll(MavenUtils.getClasspaths(classpathInfo));

		for(String artifact_id : modules.keySet()) {
			// Note that not all submodules are built
			if(classpaths.containsKey(artifact_id)) {

				String cp = classpaths.get(artifact_id);
				File dir = modules.get(artifact_id);

				// make sure there are src and target directories in this submodule
				File srcDir =
						new File(dir.getAbsolutePath() + File.separator + "src");
				File targetDir =
						new File(dir.getAbsolutePath() + File.separator + "target");
				if(!srcDir.exists() || !targetDir.exists()) {
					System.err.println("There are no src or target directories in "
							+ dir.getAbsolutePath());
					continue;
				}

				// initialize the arguments for CallGraphAnalaysis
				File app_class_path =
						new File(dir.getAbsolutePath() + File.separator + "target/classes");
				if(app_class_path.exists()) {
					app_class_paths.put(artifact_id, Arrays.asList(app_class_path));
				}

				File app_test_path =
						new File(dir.getAbsolutePath() + File.separator + "target/test-classes");
				if(app_test_path.exists()) {
					app_test_paths.put(artifact_id, Arrays.asList(app_test_path));
				}

				String[] cps = cp.split(File.pathSeparator);
				lib_class_paths.put(artifact_id, new ArrayList<File>());
				for(String path: cps){
					File pathFile = new File(path);
					if(!path.isEmpty() && pathFile.exists() && directoryContains(root_dir,pathFile)) {
						lib_class_paths.get(artifact_id).add(new File(path));
					}
				}
			}
		}
	}

	private static boolean directoryContains(File dir, File file){
		/* To find if a file is within a directory, we simply keep calling file.getParentFile(), until we find
		a parent directory equal to the directory. We will eventually get to a point where file.getParentFile() == null
		in the case where a file is not within a directory.
		*/

		if(file.getParentFile() == null){
			return false;
		}

		if(file.getParentFile().equals(dir)){
			return true;
		}

		return directoryContains(dir, file.getParentFile());
	}

	@Override
	public void run() {
		setup();
		File root_dir = new File(project_path);
		
		// find all submodules if any
		HashMap<String, File> modules = new HashMap<String, File>();
		MavenUtils.getModules(root_dir, modules);

		int count = 0;
		for(String artifact_id : modules.keySet()) {
			// Note that not all submodules are built
			if(classpaths.containsKey(artifact_id)) {
				
				// increment the count of analyzed modules
				count++;

				List<File> localLibClassPaths =
					(lib_class_paths.containsKey(artifact_id) ? lib_class_paths.get(artifact_id) : new ArrayList<File>());

				List<File> localAppClassPaths =
					(app_class_paths.containsKey(artifact_id) ? app_class_paths.get(artifact_id) : new ArrayList<File>());

				List<File> localTestClassPaths =
					(app_test_paths.containsKey(artifact_id) ? app_test_paths.get(artifact_id) : new ArrayList<File>());

				CallGraphAnalysis runner = 
						new CallGraphAnalysis(localLibClassPaths, localAppClassPaths,
								localTestClassPaths, entryPointProcessor);
				runner.setup();
				runner.run();
				
				// aggregate the analysis result of the submodule
				this.libClasses.addAll(runner.getLibClasses());
				this.libMethods.addAll(runner.getLibMethods());
				this.appClasses.addAll(runner.getAppClasses());
				this.appMethods.addAll(runner.getAppMethods());
				this.usedLibClasses.addAll(runner.getUsedLibClasses());
				this.usedLibMethods.addAll(runner.getUsedLibMethods());
				this.usedAppClasses.addAll(runner.getUsedAppClasses());
				this.usedAppMethods.addAll(runner.getUsedAppMethods());
				this.entryPoints.addAll(runner.getEntryPoints());
				
				// make sure to reset Soot after running reachability analysis
				G.reset();
			}
		}
		
		if(count > 1) {
			adjustClassesAndMethodsFromSubmodules();
		}
		
		// (optional) use tamiflex to dynamically identify reflection calls
		if(useTamiFlex) {
			// TODO: hardcode the path to the tamiflex jar for now, but need to refactor this and let 
			// the user to pass it in later
			TamiFlexRunner tamiflex = new TamiFlexRunner("src/test/resources/tamiflex/poa-2.0.3.jar", 
					project_path, false);
			try {
				tamiflex.run();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// aggregate all accessed classes from each submodule (if any) into one set 
			HashSet<String> accessed_classes = new HashSet<String>();
			for(String module : tamiflex.accessed_classes.keySet()) {
				accessed_classes.addAll(tamiflex.accessed_classes.get(module));
			}
			HashSet<String> all_classes_in_scope = new HashSet<String>();
			all_classes_in_scope.addAll(libClasses);
			all_classes_in_scope.addAll(appClasses);
			accessed_classes.retainAll(all_classes_in_scope);
			Application.classesToIgnore.addAll(accessed_classes);
			
			// aggregate all used methods from each submodule (if any) into one set
			HashMap<String, HashSet<MethodData>> new_entry_points = new HashMap<String, HashSet<MethodData>>();
			for(String module : tamiflex.used_methods.keySet()) {
				HashSet<MethodData> set = new HashSet<MethodData>();
				for(String record : tamiflex.used_methods.get(module)) {
					String[] ss = record.split(": ");
					String class_name1 = ss[0];
					String method_signature1 = ss[1];
					
					boolean foundInApp = false;
					for(MethodData md : appMethods) {
						String class_name2 = md.getClassName();
						// The signature generated below only contains return type, method name, 
						// and arguments. There are no access modifiers.
						String method_signature2 = md.getSubSignature(); 
						if(class_name1.equals(class_name2) && method_signature1.equals(method_signature2)) {
							// this is a library method 
							if(!usedAppMethods.contains(md)) {
								// this method is already identified as a used method by static analysis
								set.add(md);
								usedAppMethods.add(md);
								usedAppClasses.add(md.getClassName());
								foundInApp = true;
								break;
							}
						}
					}
					
					if(!foundInApp) {
						for(MethodData md : libMethods) {
							String class_name2 = md.getClassName();
							// The signature generated below only contains return type, method name, 
							// and arguments. There are no access modifiers.
							String method_signature2 = md.getSubSignature(); 
							if(class_name1.equals(class_name2) && method_signature1.equals(method_signature2)) {
								// this is a library method 
								if(!usedLibMethods.contains(md)) {
									// this method is already identified as a used method by static analysis
									set.add(md);
									usedLibMethods.add(md);
									usedLibClasses.add(md.getClassName());
									foundInApp = true;
									break;
								}
							}
						}
					}
					
					new_entry_points.put(module, set);
				}
			}
			
			
			// set those methods that are invoked via reflection as entry points and redo the
			// static analysis
			int count2 = 0;
			for(String module : new_entry_points.keySet()) {
				count2 ++;
				
				HashSet<MethodData> entry_methods = new_entry_points.get(module);
				
				List<File> localLibClassPaths =
						(lib_class_paths.containsKey(module) ? lib_class_paths.get(module) : new ArrayList<File>());

				List<File> localAppClassPaths =
						(app_class_paths.containsKey(module) ? app_class_paths.get(module) : new ArrayList<File>());

				List<File> localTestClassPaths =
						(app_test_paths.containsKey(module) ? app_test_paths.get(module) : new ArrayList<File>());
				CallGraphAnalysis runner = 
						new CallGraphAnalysis(localLibClassPaths, localAppClassPaths,
								localTestClassPaths, null);
				runner.setup();
				runner.run(entry_methods);
				
				// aggregate the analysis result of the submodule
				this.usedLibClasses.addAll(runner.getUsedLibClasses());
				this.usedLibMethods.addAll(runner.getUsedLibMethods());
				this.usedAppClasses.addAll(runner.getUsedAppClasses());
				this.usedAppMethods.addAll(runner.getUsedAppMethods());
				this.entryPoints.addAll(runner.getEntryPoints());
				
				// make sure to reset Soot after running reachability analysis
				G.reset();
			}
			
			if (count2 > 1) {
				adjustClassesAndMethodsFromSubmodules();
			}
		}
	}
	
	private void adjustClassesAndMethodsFromSubmodules() {
		// When analyzing different submodules, the application classes in one module may
		// be treated as library classes in another module
		// We need to adjust this so that application classes/methods are always stored in
		// the application related fields, though they are used as library classes/methods 
		// in other modules
		Set<String> lib_classes_copy = new HashSet<String>(this.libClasses);
		lib_classes_copy.retainAll(this.appClasses);
		this.libClasses.removeAll(lib_classes_copy);
		Set<MethodData> lib_methods_copy = new HashSet<MethodData>(this.libMethods);
		lib_methods_copy.retainAll(this.appMethods);
		this.libMethods.removeAll(lib_methods_copy);
		Set<String> used_lib_classes_copy = new HashSet<String>(this.usedLibClasses);
		used_lib_classes_copy.retainAll(this.appClasses);
		this.usedLibClasses.removeAll(used_lib_classes_copy);
		this.usedAppClasses.addAll(used_lib_classes_copy);
		Set<MethodData> used_lib_methods_copy = new HashSet<MethodData>(this.usedLibMethods);
		used_lib_methods_copy.retainAll(this.appMethods);
		this.usedLibMethods.removeAll(used_lib_methods_copy);
		this.usedAppMethods.addAll(used_lib_methods_copy);
	}

	@Override
	public Set<String> getLibClasses() {
		return Collections.unmodifiableSet(this.libClasses);
	}

	@Override
	public Set<MethodData> getLibMethods() {
		return Collections.unmodifiableSet(this.libMethods);
	}

	@Override
	public Set<String> getAppClasses() {
		return Collections.unmodifiableSet(this.appClasses);
	}

	@Override
	public Set<MethodData> getAppMethods() {
		return Collections.unmodifiableSet(this.appMethods);
	}

	@Override
	public Set<String> getUsedLibClasses() {
		return Collections.unmodifiableSet(this.usedLibClasses);
	}

	@Override
	public Set<MethodData> getUsedLibMethods() {
		return Collections.unmodifiableSet(this.usedLibMethods);
	}

	@Override
	public Set<String> getUsedAppClasses() {
		return Collections.unmodifiableSet(this.usedAppClasses);
	}

	@Override
	public Set<MethodData> getUsedAppMethods() {
		return Collections.unmodifiableSet(this.usedAppMethods);
	}

	@Override
	public List<File> getAppClasspaths() {
		return Collections.unmodifiableList(getClassPath(this.app_class_paths));
	}

	@Override
	public List<File> getLibClasspaths() {
		return Collections.unmodifiableList(getClassPath(this.lib_class_paths));
	}

	@Override
	public List<File> getTestClasspaths() {
		return Collections.unmodifiableList(getClassPath(this.app_test_paths));
	}

	private List<File> getClassPath(Map<String, List<File>> paths){
		List<File> toReturn = new ArrayList<File>();
		for(Map.Entry<String, List<File>> mapEntry : paths.entrySet()){
			for(File file : mapEntry.getValue()){
				if(!toReturn.contains(file)){
					toReturn.add(file);
				}
			}
		}

		return toReturn;
	}

	@Override
	public Set<MethodData> getEntryPoints(){
		return this.entryPoints;
	}
}
