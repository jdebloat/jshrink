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
	 * @param touchedMethods The touched methods. We conform to the following schema:
	 *                        '[qualified class name]:[method]'
	 *                        or '[qualified class name]:*', when declaring all methods in a class
	 *                        E.g., 'edu.ucla.cs.onr.methodwiper.MethodWiper:wipeMethodStart'
	 *
	 *                       Note: We do not support method overloading yet. Two methods, with the same name,
	 *                       will be treated as the same, even if their signatures differ. E.g., we can not distinguish
	 *                       between 'wipeMethodStart()' and 'wipeMethodStart(int)'. If 'wipeMethodStart()' were
	 *                       touched, please consider 'wipeMethodStart(int) touched also.
	 *
	 */
	public void collapseClasses(List<File> libClassPath, List<File> appClassPath, Set<MethodData> touchedMethods);
}
