package edu.ucla.cs.onr.util;

import edu.ucla.cs.onr.Application;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JasminClass;
import soot.util.JasminOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.jar.JarFile;

public class ClassFileUtils {

	public static final String ORIGINAL_FILE_POST_FIX="_original"; //package private as used by tests

	public static long getSize(File file) throws IOException{

		if(!file.exists()){
			throw new IOException("File '" + file.getAbsolutePath() + " does not exist");
		}

		long length=0;
		if(file.isDirectory()){
			for(File innerFile : file.listFiles()){
				length += getSize(innerFile);
			}
		} else {
			length += file.length();
		}

		return length;
	}

	private static Optional<File> getClassFile(SootClass sootClass, Collection<File> paths) {
		String classPath = sootClass.getName().replaceAll("\\.", File.separator) + ".class";

		for (File p : paths) {
			File test = new File(p + File.separator + classPath);
			if (test.exists()) {
				return Optional.of(test);
			}
		}
		return Optional.empty();
	}

	public static void decompressJar(File jarFile) throws IOException{

		ZipFile jarToReturn = null;
		try {
			jarToReturn = new ZipFile(jarFile);
		} catch (ZipException e) {
			throw new IOException("File '" + jarFile.getAbsolutePath() + "' is not a zipped file. " +
					"Are you sure it's a valid Jar?");
		}

		try {
			//Extract the jar file into a temporary directory
			File tempDir = File.createTempFile("jar_expansion", "tmp");
			tempDir.delete();
			if(!tempDir.mkdir()){
				throw new IOException("Could not 'mkdir " + tempDir.getAbsolutePath() + "'");
			}

			try {
				jarToReturn.extractAll(tempDir.getAbsolutePath());
			} catch(ZipException e){
				throw new IOException("Failed to extract .jar file. Following exception thrown:" +
						System.lineSeparator() + e.getLocalizedMessage());
			}

			jarToReturn.getFile().delete();
			FileUtils.moveDirectory(tempDir, jarToReturn.getFile());

		} catch(IOException e){
			throw new IOException("Failed to create a temporary directory. The following exception was thrown:"
					+ System.lineSeparator() + e.getLocalizedMessage());
		}
	}

	public static void compressJar(File file) throws IOException{
		try {
			ZipFile zipFile = new ZipFile(File.createTempFile("tmpJarFile", ".jar_tmp"));
			zipFile.getFile().delete();
			ZipParameters zipParameters = new ZipParameters();
			zipParameters.setCompressionLevel(9);

			//It's in a busy state otherwise... hope this is ok
			zipFile.getProgressMonitor().setState(ProgressMonitor.STATE_READY);

			boolean created=false;
			for(File f : file.listFiles()){
				if(f.isDirectory()){
					if(!created){
						zipFile.createZipFileFromFolder(f,zipParameters,false, 0);
						created=true;
					} else {
						zipFile.addFolder(f, zipParameters);
					}
				} else{
					if(!created){
						zipFile.createZipFile(f, zipParameters);
						created=true;
					} else {
						zipFile.addFile(f, zipParameters);
					}
				}
			}

			// Regular file.delete(), does not always work. I have to force it (I don't know why)
			FileUtils.forceDelete(file);
			FileUtils.moveFile(zipFile.getFile(), file);


		} catch(ZipException|IOException e){
			throw new IOException("Unable to create zip (Jar) file '" + file.getAbsolutePath() + "'" +
					" Following exception thrown:" + System.lineSeparator() + e.getLocalizedMessage());
		}
	}

	public static void removeClass(SootClass sootClass, Collection<File> classPath) throws IOException{
		Optional<File> fileToReturn = getClassFile(sootClass, classPath);

		if(!fileToReturn.isPresent()){
			throw new IOException("Cannot find file for class '" +  sootClass.getName() + "'");
		}

		assert(fileToReturn.isPresent());

		if(Application.isDebugMode()){
			File moveLocation = new File(fileToReturn.get().getAbsolutePath() + ORIGINAL_FILE_POST_FIX);
			FileUtils.moveFile(fileToReturn.get(), moveLocation);
		} else {
			FileUtils.forceDelete(fileToReturn.get());
		}
	}

	public static void writeClass(SootClass sootClass, Collection<File> classPath) throws IOException{

		Optional<File> fileToReturn = getClassFile(sootClass, classPath);

		if(!fileToReturn.isPresent()){
			throw new IOException("Cannot find file for class '" + sootClass.getName() + "'");
		}

		assert(fileToReturn.isPresent());

		//I don't fully understand why, but you need to retrieve the methods before writing to the fole
		for (SootMethod sootMethod : sootClass.getMethods()) {
			if(sootMethod.isConcrete()){
				sootMethod.retrieveActiveBody();
			}
		}

		if(Application.isDebugMode()) {
			File copyLocation = new File(fileToReturn.get().getAbsolutePath() + ORIGINAL_FILE_POST_FIX);
			FileUtils.copyFile(fileToReturn.get(), copyLocation);
		}

		OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileToReturn.get()));
		PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

		JasminClass jasminClass = new soot.jimple.JasminClass(sootClass);
		jasminClass.print(writerOut);
		writerOut.flush();
		streamOut.close();
	}


	public static void rectifyChanges(Collection<File> classpaths) throws IOException{
		for(File f : classpaths) {
			if (f.isFile()) {
				JarFile jarFile =null;
				try {
					jarFile = new JarFile(f); //Throws IOException if not a jar file
				} catch (IOException e) { //I.e., it's not a Jar file
					rectifyClassChanges(f);
				}

				if(jarFile != null) {
					rectifyJarChanges(f);
				}
			} else if(f.isDirectory()){
				Collection<File> tempCollection = new ArrayList<File>();
				for(File fileWithin : f.listFiles()){
					tempCollection.add(fileWithin);
				}
				rectifyChanges(tempCollection);
			}
		}
	}

	private static void rectifyJarChanges(File jarFile) throws IOException{

		ClassFileUtils.decompressJar(jarFile);

		//Rectify any changes within the temporary directory
		Collection<File> jarFileCollection = new ArrayList<File>();
		jarFileCollection.add(jarFile);

		rectifyChanges(jarFileCollection);

		ClassFileUtils.compressJar(jarFile);
	}

	private static void rectifyClassChanges(File file) throws IOException{
		assert(file.isFile() && !file.isDirectory());
		if(file.getAbsolutePath().endsWith(ORIGINAL_FILE_POST_FIX)){
			File original = new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - ORIGINAL_FILE_POST_FIX.length()));
			if(original.exists()){
				original.delete();
			}
			FileUtils.moveFile(file, original);
		}
	}
}
