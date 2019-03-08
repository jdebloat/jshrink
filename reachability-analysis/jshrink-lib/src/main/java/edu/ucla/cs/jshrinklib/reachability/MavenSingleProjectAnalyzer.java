package edu.ucla.cs.jshrinklib.reachability;

import java.io.*;
import java.util.*;

import edu.ucla.cs.jshrinklib.util.ClassFileUtils;
import org.apache.commons.io.FileUtils;

import soot.G;
import edu.ucla.cs.jshrinklib.util.MavenUtils;
import soot.jimple.toolkits.callgraph.CallGraph;

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
	
	private String project_path;
	
	private final Set<String> libClasses;
	private final Set<String> libClassesCompileOnly;
	private final Set<MethodData> libMethods;
	private final Set<MethodData> libMethodsCompileOnly;
	private final Set<String> appClasses;
	private final Set<MethodData> appMethods;
	private final Set<String> usedLibClasses;
	private final Set<String> usedLibClassesCompileOnly;
	private final Map<MethodData, Set<MethodData>> usedLibMethods;
	private final Map<MethodData, Set<MethodData>> usedLibMethodsCompileOnly;
	private final Set<String> usedAppClasses;
	private final Map<MethodData, Set<MethodData>> usedAppMethods;
	private final Map<String,List<File>> app_class_paths;
	private final Map<String,List<File>> app_test_paths;
	private final Map<String,List<File>> lib_class_paths;
	private final HashMap<String, String> classpaths;
	private final HashMap<String, String> classpaths_compile_only;
	private final EntryPointProcessor entryPointProcessor;
	private final Set<MethodData> entryPoints;
	private final Optional<File> tamiFlexJar;
	private final Set<CallGraph> callgraphs;
	private final Set<String> classesToIgnore;
	private final boolean useSpark;
	
	public MavenSingleProjectAnalyzer(String pathToMavenProject, EntryPointProcessor entryPointProc,
									  Optional<File> tamiFlex,
	                                  boolean useSpark) {
		project_path = pathToMavenProject;
		
		libClasses = new HashSet<String>();
		libClassesCompileOnly = new HashSet<String>();
		libMethods = new HashSet<MethodData>();
		libMethodsCompileOnly = new HashSet<MethodData>();
		appClasses = new HashSet<String>();
		appMethods = new HashSet<MethodData>();
		usedLibClasses = new HashSet<String>();
		usedLibClassesCompileOnly = new HashSet<String>();
		usedLibMethods = new HashMap<MethodData, Set<MethodData>>();
		usedLibMethodsCompileOnly = new HashMap<MethodData,Set<MethodData>>();
		usedAppClasses = new HashSet<String>();
		usedAppMethods = new HashMap<MethodData, Set<MethodData>>();
		app_class_paths = new HashMap<String, List<File>>();
		app_test_paths = new HashMap<String, List<File>>();
		lib_class_paths = new HashMap<String, List<File>>();
		classpaths = new HashMap<String, String>();
		classpaths_compile_only = new HashMap<String, String>();
		entryPointProcessor = entryPointProc;
		entryPoints = new HashSet<MethodData>();
		tamiFlexJar = tamiFlex;
		this.callgraphs = new HashSet<CallGraph>();
		this.classesToIgnore = new HashSet<String>();
		this.useSpark = useSpark;
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
		String classpathInfo_compile_only = "";
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

			// first get the full classpath (compile scope + test scope) so that we will get a more complete
			// call graph in the static analysis later 
			String[] cmd2 = new String[] {"mvn", "-f", pomFile.getAbsolutePath(), "dependency:build-classpath",
					"-Dmaven.repo.local=" + libsDir.getAbsolutePath(), "--batch-mode"};
			Process process2 = Runtime.getRuntime().exec(cmd2);
			process2.waitFor();

			reader = new BufferedReader(new InputStreamReader(process2.getInputStream()));
			while((line=reader.readLine()) != null) {
				classpathInfo += line + System.lineSeparator();
			}
			reader.close();
			
			if(classpathInfo.contains("BUILD FAILURE")) {
				System.err.println("'mvn dependency:build-classpath' fails.");
			}
			
			// then get the classpath of the compile scope only for the future method removal
			String[] cmd3 = new String[] {"mvn", "-f", pomFile.getAbsolutePath(), "dependency:build-classpath",
					"-Dmaven.repo.local=" + libsDir.getAbsolutePath(), "-DincludeScope=compile", "--batch-mode"};
			Process process3 = Runtime.getRuntime().exec(cmd3);
			process3.waitFor();
			
			reader = new BufferedReader(new InputStreamReader(process3.getInputStream()));
			while((line = reader.readLine()) != null) {
				classpathInfo_compile_only += line + System.lineSeparator();
			}
			reader.close();
			
			if(classpathInfo_compile_only.contains("BUILD FAILURE")) {
				System.err.println("'mvn dependency:build-classpath -DincludeScope=compile' fails.");
			}
		}catch(IOException | InterruptedException e){
			e.printStackTrace();
			System.exit(1);
		}


		classpaths.putAll(MavenUtils.getClasspaths(classpathInfo));
		classpaths_compile_only.putAll(MavenUtils.getClasspaths(classpathInfo_compile_only));

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
					if(!path.isEmpty() && pathFile.exists() && ClassFileUtils.directoryContains(root_dir,pathFile)) {
						lib_class_paths.get(artifact_id).add(new File(path));
					}
				}
			}
		}
	}

	private static void addToMap(Map<MethodData, Set<MethodData>> map, MethodData key, Collection<MethodData> elements){
		if(!map.containsKey(key)){
			map.put(key, new HashSet<MethodData>());
		}
		for(MethodData element : elements){
			map.get(key).add(element);
		}
	}

	private static void addToMap(Map<MethodData,Set<MethodData>> map, Map<MethodData, Set<MethodData>> toAdd){
		for(Map.Entry<MethodData, Set<MethodData>> entry : toAdd.entrySet()){
			addToMap(map, entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void run() {
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
								localTestClassPaths, entryPointProcessor, this.useSpark);
				runner.setup();
				runner.run();

				this.callgraphs.addAll(runner.getCallGraphs());
				
				String cp_compile_only = classpaths_compile_only.get(artifact_id);
				HashSet<String> compile_lib_paths = 
						new HashSet<String>(Arrays.asList(cp_compile_only.split(File.pathSeparator)));
				// aggregate the analysis result of the submodule
				for(String libClass : runner.getLibClasses()) {
					this.libClasses.add(libClass);
					String lib_path = runner.getLibPathOfClass(libClass);
					if(compile_lib_paths.contains(lib_path)) {
						this.libClassesCompileOnly.add(libClass);
					}
				}
				for(MethodData libMethod : runner.getLibMethods()) {
					this.libMethods.add(libMethod);
					String lib_path = runner.getLibPathOfMethod(libMethod);
					if(compile_lib_paths.contains(lib_path)) {
						this.libMethodsCompileOnly.add(libMethod);
					}
				}				
				for(String usedLibClass : runner.getUsedLibClasses()) {
					this.usedLibClasses.add(usedLibClass);
					String lib_path = runner.getLibPathOfClass(usedLibClass);
					if(compile_lib_paths.contains(lib_path)) {
						this.usedLibClassesCompileOnly.add(usedLibClass);
					}
				}
				for(Map.Entry<MethodData,Set<MethodData>> libMethod : runner.getUsedLibMethods().entrySet()) {
					addToMap(this.usedLibMethods, libMethod.getKey(),libMethod.getValue());
					String lib_path = runner.getLibPathOfMethod(libMethod.getKey());
					if(compile_lib_paths.contains(lib_path)) {
						addToMap(this.usedLibMethodsCompileOnly, libMethod.getKey(), libMethod.getValue());
					}
				}
				this.appClasses.addAll(runner.getAppClasses());
				this.appMethods.addAll(runner.getAppMethods());
				this.usedAppClasses.addAll(runner.getUsedAppClasses());
				addToMap(this.usedAppMethods, runner.getUsedAppMethods());
				this.entryPoints.addAll(runner.getEntryPoints());
				
				// make sure to reset Soot after running reachability analysis
				G.reset();
			}
		}
		
		// (optional) use tamiflex to dynamically identify reflection calls
		if(tamiFlexJar.isPresent()) {
			TamiFlexRunner tamiflex = new TamiFlexRunner(tamiFlexJar.get().getAbsolutePath(),
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
			this.classesToIgnore.addAll(accessed_classes); //[BB: ] No idea why we do this
			//Application.classesToIgnore.addAll(accessed_classes);
			
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
							if(!usedAppMethods.containsKey(md)) {
								// this method is already identified as a used method by static analysis
								set.add(md);
								usedAppMethods.put(md, new HashSet<MethodData>());
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
								if(!usedLibMethods.containsKey(md)) {
									// this method is already identified as a used method by static analysis
									set.add(md);
									usedLibMethods.put(md, new HashSet<MethodData>());
									usedLibClasses.add(md.getClassName());
									
									// also need to update usedLibMethodsCompileOnly and usedLibClassesCompile only
									if(libMethodsCompileOnly.contains(md)) {
										usedLibMethodsCompileOnly.put(md, new HashSet<MethodData>());
										usedLibClasses.add(md.getClassName());
									}
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
			for(String module : new_entry_points.keySet()) {				
				HashSet<MethodData> entry_methods = new_entry_points.get(module);
				
				List<File> localLibClassPaths =
						(lib_class_paths.containsKey(module) ? lib_class_paths.get(module) : new ArrayList<File>());

				List<File> localAppClassPaths =
						(app_class_paths.containsKey(module) ? app_class_paths.get(module) : new ArrayList<File>());

				List<File> localTestClassPaths =
						(app_test_paths.containsKey(module) ? app_test_paths.get(module) : new ArrayList<File>());
				CallGraphAnalysis runner = 
						new CallGraphAnalysis(localLibClassPaths, localAppClassPaths,
								localTestClassPaths, null, useSpark);
				runner.setup();
				runner.run(entry_methods);

				this.callgraphs.addAll(runner.getCallGraphs());
				
				// aggregate the analysis result of the submodule
				for(String class_name : runner.getUsedLibClasses()) {
					this.usedLibClasses.add(class_name);
					if(this.libClassesCompileOnly.contains(class_name)) {
						this.usedLibClassesCompileOnly.add(class_name);
					}
				}
				for(Map.Entry<MethodData, Set<MethodData>> entry: runner.getUsedLibMethods().entrySet()){
					addToMap(this.usedLibMethods, entry.getKey(), entry.getValue());

					if(this.libMethodsCompileOnly.contains(entry.getKey())) {
						addToMap(this.usedLibMethodsCompileOnly, entry.getKey(), entry.getValue());
					}
				}
				this.usedAppClasses.addAll(runner.getUsedAppClasses());
				addToMap(this.usedAppMethods, runner.getUsedAppMethods());
				this.entryPoints.addAll(runner.getEntryPoints());
				
				// make sure to reset Soot after running reachability analysis
				G.reset();
			}
		}
		
		if(count > 1) {
			adjustClassesAndMethodsFromSubmodules();
		}
	}
	
	/**
	 * When analyzing different submodules, the application classes in one module may
	 * be treated as library classes in another module. We need to adjust this so that 
	 * application classes/methods are always stored in the application related fields, 
	 * though they are used as library classes/methods in other modules
	 */
	private void adjustClassesAndMethodsFromSubmodules() {
		Set<String> lib_classes_copy = new HashSet<String>(this.libClasses);
		// only keep the app classes from other modules
		lib_classes_copy.retainAll(this.appClasses);
		// after removing the app classes from other modules, only classes from external libs remain 
		this.libClasses.removeAll(lib_classes_copy);
		// do the same to get classes from external libs in the compile scope
		Set<String> lib_classes_compile_only_copy = new HashSet<String>(this.libClassesCompileOnly);
		lib_classes_compile_only_copy.retainAll(this.appClasses);
		this.libClassesCompileOnly.removeAll(lib_classes_compile_only_copy);
		
		Set<MethodData> lib_methods_copy = new HashSet<MethodData>(this.libMethods);
		// only keep the app methods from other modules
		lib_methods_copy.retainAll(this.appMethods);
		// after removing the app methods from other modules, only methods from external libs remain
		this.libMethods.removeAll(lib_methods_copy);
		// do the same to get methods from external libs in the compile scope
		Set<MethodData> lib_methods_compile_only_copy = new HashSet<MethodData>(this.libMethodsCompileOnly);
		lib_methods_compile_only_copy.retainAll(this.appMethods);
		this.libMethodsCompileOnly.removeAll(lib_methods_compile_only_copy);
		
		Set<String> used_lib_classes_copy = new HashSet<String>(this.usedLibClasses);
		// only keep used app classes from other modules
		used_lib_classes_copy.retainAll(this.appClasses);
		// after removing used app classes from other modules, only used classes from external libs remain 
		this.usedLibClasses.removeAll(used_lib_classes_copy);
		// do the same to get used classes from external libs in the compile scope 
		Set<String> used_lib_classes_compile_only_copy = new HashSet<String>(this.usedLibClassesCompileOnly);
		used_lib_classes_compile_only_copy.retainAll(this.appClasses); 
		this.usedLibClassesCompileOnly.removeAll(used_lib_classes_compile_only_copy);
		
		// also need to add the used app classes from other modules back to the set of used app classes
		// just in case that those classes are not used in their own modules
		this.usedAppClasses.addAll(used_lib_classes_copy);
		
		Map<MethodData, Set<MethodData>> used_lib_methods_copy
				= new HashMap<MethodData, Set<MethodData>>();
		// only keep used app methods from other modules
		for(Map.Entry<MethodData,Set<MethodData>> methodData : this.usedLibMethods.entrySet()){
			if(this.appMethods.contains(methodData.getKey())) {
				addToMap(used_lib_methods_copy, methodData.getKey(), methodData.getValue());
			}
		}
		// after removing used app methods from other modules, only used methods from external libs remain
		for(Map.Entry<MethodData, Set<MethodData>> methodData : used_lib_methods_copy.entrySet()){
			this.usedLibMethods.remove(methodData.getKey());
		}
		// do the same to get used methods from external libs in the compile scope
		Map<MethodData,Set<MethodData>> used_lib_methods_compile_only_copy = new HashMap<MethodData,Set<MethodData>>();
		for(Map.Entry<MethodData,Set<MethodData>> entry : this.usedLibMethodsCompileOnly.entrySet()){
			if(this.appMethods.contains(entry.getKey())){
				addToMap(used_lib_methods_compile_only_copy, entry.getKey(), entry.getValue());
			}
		}

		for(Map.Entry<MethodData, Set<MethodData>> entry : used_lib_methods_compile_only_copy.entrySet()){
			this.usedLibMethodsCompileOnly.remove(entry.getKey());
		}

		// also need to add the used app methods from other modules back to the set of used app methods
		// just in case that those methods are not called in their own modules
		addToMap(this.usedAppMethods,used_lib_methods_copy);
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
	public Set<String> getLibClassesCompileOnly() {
		return Collections.unmodifiableSet(this.libClassesCompileOnly);
	}
	
	@Override
	public Set<MethodData> getLibMethodsCompileOnly() {
		return Collections.unmodifiableSet(this.libMethodsCompileOnly);
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
	public Set<String> getUsedLibClassesCompileOnly() {
		return Collections.unmodifiableSet(this.usedLibClassesCompileOnly);
	}

	@Override
	public Map<MethodData, Set<MethodData>> getUsedLibMethods() {
		return Collections.unmodifiableMap(this.usedLibMethods);
	}
	
	@Override
	public Map<MethodData, Set<MethodData>> getUsedLibMethodsCompileOnly() {
		return Collections.unmodifiableMap(this.usedLibMethodsCompileOnly);
	}

	@Override
	public Set<String> getUsedAppClasses() {
		return Collections.unmodifiableSet(this.usedAppClasses);
	}

	@Override
	public Map<MethodData, Set<MethodData>> getUsedAppMethods() {
		return Collections.unmodifiableMap(this.usedAppMethods);
	}

	@Override
	public List<File> getAppClasspaths() {
		return Collections.unmodifiableList(getClassPath(this.app_class_paths));
	}

	@Override
	public List<File> getLibClasspaths() {
		/*
		TODO: This is a bit of a mess --- really don't like this classpath/classpath_compile_only thing. It's a mess.
		Can we clean this up at some point? It's confusing and it'll just introduce more bugs going forward.
		 */
		List<File> toReturn = new ArrayList<File>();
		for(String key : classpaths_compile_only.keySet()){
			String cp_compile_only = classpaths_compile_only.get(key);
			HashSet<String> compile_lib_paths =
					new HashSet<String>(Arrays.asList(cp_compile_only.split(File.pathSeparator)));
			for(String classPath : compile_lib_paths){
				File toAdd = new File(classPath);
				if(ClassFileUtils.directoryContains(new File(this.project_path), toAdd)) {
					toReturn.add(new File(classPath));
				}
			}
		}
		return Collections.unmodifiableList(toReturn);
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

	@Override
	public Set<CallGraph> getCallGraphs(){
		return this.callgraphs;
	}

	@Override
	public Set<String> classesToIgnore(){
		return this.classesToIgnore;
	}
}