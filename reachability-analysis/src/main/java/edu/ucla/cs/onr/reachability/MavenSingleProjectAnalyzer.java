package edu.ucla.cs.onr.reachability;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.G;
import edu.ucla.cs.onr.Application;
import edu.ucla.cs.onr.util.ASMUtils;
import edu.ucla.cs.onr.util.EntryPointUtil;
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
public class MavenSingleProjectAnalyzer {
	private String project_path;
	
	private Set<String> libClasses;
	private Set<MethodData> libMethods;
	private Set<String> appClasses;
	private Set<MethodData> appMethods;
	private Set<String> usedLibClasses;
	private Set<MethodData> usedLibMethods;
	private Set<String> usedAppClasses;
	private Set<MethodData> usedAppMethods;
	
	public MavenSingleProjectAnalyzer(String pathToMavenProject) {
		project_path = pathToMavenProject;
		
		libClasses = new HashSet<String>();
		libMethods = new HashSet<MethodData>();
		appClasses = new HashSet<String>();
		appMethods = new HashSet<MethodData>();
		usedLibClasses = new HashSet<String>();
		usedLibMethods = new HashSet<MethodData>();
		usedAppClasses = new HashSet<String>();
		usedAppMethods = new HashSet<MethodData>();
	}
	
	public void run() {
		if(Application.isDebugMode() || Application.isVerboseMode()) {
			System.out.println("Start analyzing " + project_path);
		}
		
		File root_dir = new File(project_path);
		
		// find all submodules if any
		HashMap<String, File> modules = new HashMap<String, File>();
		MavenUtils.getModules(root_dir, modules);
		
		
		// get all classpaths for submodules if any
		String cp_log = project_path + File.separator + "onr_classpath_new.log";
		HashMap<String, String> classpaths = MavenUtils.getClasspaths(cp_log);
		
		int count = 0;
		for(String artifact_id : modules.keySet()) {
			// Note that not all submodules are built
			if(classpaths.containsKey(artifact_id)) {
				if((Application.isDebugMode() || Application.isVerboseMode()) && modules.size() > 1) {
					System.out.println("submodule---" + artifact_id);
				}
				
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
				
				// increment the count of analyzed modules
				count++;
				
				// initialize the arguments for CallGraphAnalaysis
				List<File> app_class_paths = new ArrayList<File>();
				File app_class_path = 
						new File(dir.getAbsolutePath() + File.separator + "target/classes");
				app_class_paths.add(app_class_path);
				
				List<File> app_test_paths = new ArrayList<File>();
				File app_test_path = 
						new File(dir.getAbsolutePath() + File.separator + "target/test-classes");
				app_test_paths.add(app_test_path);
				
				List<File> lib_class_paths = new ArrayList<File>();
				String[] cps = cp.split(File.pathSeparator);
				for(String path: cps){
					if(!path.isEmpty()) {
						lib_class_paths.add(new File(path));
					}
				}
				
				Set<MethodData> entryPoints = new HashSet<MethodData>();
				// get main methods
				HashSet<String> appClasses = new HashSet<String>();
				HashSet<MethodData> appMethods = new HashSet<MethodData>();
				ASMUtils.readClassFromDirectory(app_class_path, appClasses, appMethods);
				Set<MethodData> mainMethods = EntryPointUtil.getMainMethodsAsEntryPoints(appMethods);
				entryPoints.addAll(mainMethods);
				
				// get test methods
				HashSet<String> testClasses = new HashSet<String>();
				HashSet<MethodData> testMethods = new HashSet<MethodData>();
				ASMUtils.readClassFromDirectory(app_test_path, testClasses, testMethods);
		        Set<MethodData> tests = 
		        		EntryPointUtil.getTestMethodsAsEntryPoints(testMethods);
//		        for(MethodData test : tests) {
//		        	System.out.println(test);
//		        }
		        entryPoints.addAll(tests);
				
				CallGraphAnalysis runner = 
						new CallGraphAnalysis(lib_class_paths, app_class_paths, app_test_paths, entryPoints);
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
				
				// make sure to reset Soot after running reachability analysis
				G.reset();
			}
		}
		
		if(count > 1) {
			// When analyzing different submodules, the application classes in one module may
			// be treated as library classes in another module
			// We need to rectify this at the end
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
			
			if(Application.isDebugMode() || Application.isVerboseMode()) {
				System.out.println("---summary of " + count + " modules---");
				System.out.println("number_lib_classes, " + this.libClasses.size());
				System.out.println("number_lib_methods, " + this.libMethods.size());
				System.out.println("number_app_classes, " + this.appClasses.size());
				System.out.println("number_app_methods, " + this.appMethods.size());
				System.out.println("number_used_lib_classes, " + this.usedLibClasses.size());
				System.out.println("number_used_lib_methods, " + this.usedLibMethods.size());
				System.out.println("number_used_app_classes, " + this.usedAppClasses.size());
				System.out.println("number_used_app_method, " + this.usedAppMethods.size());
			}
		}
				
		if(Application.isDebugMode() || Application.isVerboseMode()) {
			System.out.println("Finish analyzing " + project_path);
		}
	}
	
	public Set<String> getLibClasses() {
		return Collections.unmodifiableSet(this.libClasses);
	}

	public Set<MethodData> getLibMethods() {
		return Collections.unmodifiableSet(this.libMethods);
	}

	public Set<String> getAppClasses() {
		return Collections.unmodifiableSet(this.appClasses);
	}

	public Set<MethodData> getAppMethods() {
		return Collections.unmodifiableSet(this.appMethods);
	}

	public Set<String> getUsedLibClasses() {
		return Collections.unmodifiableSet(this.usedLibClasses);
	}

	public Set<MethodData> getUsedLibMethods() {
		return Collections.unmodifiableSet(this.usedLibMethods);
	}

	public Set<String> getUsedAppClasses() {
		return Collections.unmodifiableSet(this.usedAppClasses);
	}

	public Set<MethodData> getUsedAppMethods() {
		return Collections.unmodifiableSet(this.usedAppMethods);
	}
}
