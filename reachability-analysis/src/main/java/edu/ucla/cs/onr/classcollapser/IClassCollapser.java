package edu.ucla.cs.onr.classcollapser;

import java.io.File;
import java.util.*;

public interface IClassCollapser {

	/**
	 * The purpose of a 'collapseClasses' method is to delete any unused classes and collapse what remains to the
	 * minimum possible. The goal is to reduce the size of the bytecode while preserving overall semantics.
	 *
	 * @param libClassPath The classpath of any libraries needed for the compilation of the app
	 * @param appClassPath The classpath of the app
	 * @param collapseList The classes that need to be collapsed, in order
	 * @param nameChangeList The class names that need to be changed in all bodies, unordered
	 *
	 */
	public void collapseClasses(List<File> libClassPath, List<File> appClassPath, Queue<ArrayList<String>> collapseList,
								Map<String, String> nameChangeList);
}
