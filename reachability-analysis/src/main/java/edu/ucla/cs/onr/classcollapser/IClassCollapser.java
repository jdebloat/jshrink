package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.reachability.MethodData;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface IClassCollapser {

	/**
	 * The purpose of a 'collapseClasses' method is to delete any unused classes and collapse what remains to the
	 * minimum possible. The goal is to reduce the size of the bytecode while preserving overall semantics.
	 *
	 * @param libClassPath The classpath of any libraries needed for the compilation of the app
	 * @param appClassPath The classpath of the app
	 * @param touchedMethods The touched methods
	 *
	 */
	public void collapseClasses(List<File> libClassPath, List<File> appClassPath, Set<MethodData> touchedMethods);
}
