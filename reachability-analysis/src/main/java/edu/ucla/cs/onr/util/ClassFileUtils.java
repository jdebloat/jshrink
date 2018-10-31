package edu.ucla.cs.onr.util;

import edu.ucla.cs.onr.Application;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.Zip4jConstants;
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
import java.util.jar.JarEntry;
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

			//Search for the classfile within the directory
		//	Collection<File> tempF = new ArrayList<>();
		//	tempF.add(tempDir);
		//	fileToReturn = getClassFile(sootClass, tempF);
		//	assert(fileToReturn.isPresent()); //If returned by 'getJarFile', the jar should contain the class

			// Take a note of the expanded (temp) file and it's original .jar source
			//recompress = Optional.of(new Pair<ZipFile, File>(jarToReturn.get(),tempDir));

		} catch(IOException e){
			new IOException("Failed to create a temporary directory. The following exception was thrown:"
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

	private static Optional<ZipFile> getJarFile(SootClass sootClass, Collection<File> paths) throws IOException{
		String classPath = sootClass.getName().replaceAll("\\.", File.separator) + ".class";

		for (File p : paths) {
			try {
				JarFile jarFile = new JarFile(p);
				JarEntry entry = jarFile.getJarEntry(classPath);
				if (entry != null) {
					jarFile.close();
					return Optional.of(new ZipFile(p));
				}
			} catch (IOException e) {
				continue;
			} catch (ZipException e){
				throw new IOException("Failure to case JarFile to ZipFile. Following Exception thrown:" +
					System.lineSeparator() + e.getLocalizedMessage());
			}
		}

		return Optional.empty();
	}

	public static void writeClass(SootClass sootClass, Collection<File> classPath) throws IOException{

		//Optional<Pair<ZipFile,File>> recompress = Optional.empty();

		Optional<File> fileToReturn = getClassFile(sootClass, classPath);
		/*if (!fileToReturn.isPresent()) { //If the '.class' file could not be found in a classpath, it may be in a jar
			Optional<ZipFile> jarToReturn = getJarFile(sootClass, classPath); //Check the jar files...
			if (!jarToReturn.isPresent()) {
				throw new IOException("Source file for class '" + sootClass.getName() + "' could not be found!");
			}

			assert (jarToReturn.isPresent());

			try {
				//Extract the jar file into a temporary directory
				File tempDir = File.createTempFile("jar_expansion", "tmp");
				tempDir.delete();
				if(!tempDir.mkdir()){
					throw new IOException("Could not 'mkdir " + tempDir.getAbsolutePath() + "'");
				}

				try {
					jarToReturn.get().extractAll(tempDir.getAbsolutePath());
				} catch(ZipException e){
					throw new IOException("Failed to extract .jar file. Following exception thrown:" +
						System.lineSeparator() + e.getLocalizedMessage());
				}

				//Search for the classfile within the directory
				Collection<File> tempF = new ArrayList<>();
				tempF.add(tempDir);
				fileToReturn = getClassFile(sootClass, tempF);
				assert(fileToReturn.isPresent()); //If returned by 'getJarFile', the jar should contain the class

				// Take a note of the expanded (temp) file and it's original .jar source
				recompress = Optional.of(new Pair<ZipFile, File>(jarToReturn.get(),tempDir));

			} catch(IOException e){
				new IOException("Failed to create a temporary directory. The following exception was thrown:"
						+ System.lineSeparator() + e.getLocalizedMessage());
			}
		}*/

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

		//If the class were contained within a compressed Jar, recompression is required
		/*if(recompress.isPresent()){
			ZipFile zFile = recompress.get().getKey();
			File tempFile = recompress.get().getValue();

			try {
				// Delete the .jar file
				zFile.getFile().delete();
				//... and compress the temp file to replace the deleted .jar file
				boolean created=false;
				ZipParameters zipParameters = new ZipParameters();
				zipParameters.setCompressionLevel(9);
				for(File f : tempFile.listFiles()){
					if(f.isDirectory()){
						if(!created){
							zFile.createZipFileFromFolder(f,zipParameters,false, 0);
							created=true;
						} else {
							zFile.addFolder(f, zipParameters);
						}
					} else{
						if(!created){
							zFile.createZipFile(f, zipParameters);
							created=true;
						} else {
							zFile.addFile(f, zipParameters);
						}
					}
				}
			}catch(ZipException e){
				String exceptionString = "Failure to recompress file." + System.lineSeparator() +
					"Failure to recompress file." + System.lineSeparator() +
					"TempFile: " + tempFile.getAbsolutePath() + System.lineSeparator() +
					"ZipFile: " + zFile.getFile().getAbsolutePath() + System.lineSeparator() +
					"The following exception was thrown: " + System.lineSeparator() +
					"Failure to recompress file." +System.lineSeparator() +
					e.getLocalizedMessage();
				throw new IOException(exceptionString);
			}
		}*/
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
					/*ZipFile zipFile = null;
					try {
						zipFile = new ZipFile(f);
					} catch(ZipException e){
						throw new IOException("File '" + f.getAbsolutePath() + "' is a zip file" +
							System.lineSeparator() + e.getLocalizedMessage());
					}
					assert(zipFile != null);*/
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

		//Extract the jarFile to a temporary directory
	/*	File tempExtractionDir = null;

		try {
			tempExtractionDir =File.createTempFile("temp_jar_dir","");
			if(!tempExtractionDir.delete() || !tempExtractionDir.mkdir()){
				throw new IOException("Unable to mkdir '" + tempExtractionDir.getAbsolutePath() + "'");
			}
			assert(tempExtractionDir.exists());
			assert(tempExtractionDir.isDirectory());

			jarFile.extractAll(tempExtractionDir.getAbsolutePath());

		} catch(ZipException e){
			throw new IOException("Unable to extract jarFile '" + jarFile.getFile().getAbsolutePath() + "'. " +
				"The following exception was thrown:" + System.lineSeparator() + e.getLocalizedMessage());
		}

		assert(tempExtractionDir != null);*/

		ClassFileUtils.decompressJar(jarFile);

		//Rectify any changes within the temporary directory
		Collection<File> jarFileCollection = new ArrayList<File>();
		jarFileCollection.add(jarFile);

		rectifyChanges(jarFileCollection);

		ClassFileUtils.compressJar(jarFile);

		//Archive/compress the directory back to the original; overwriting the original jar file
		/*try {
			jarFile.getFile().delete();
			ZipParameters zipParameters = new ZipParameters();
			zipParameters.setCompressionLevel(9);
			//It's in a busy state otherwise... hope this is ok
			jarFile.getProgressMonitor().setState(ProgressMonitor.STATE_READY);
			ArrayList<File> fileList = new ArrayList<File>();
			ArrayList<File> folderList = new ArrayList<File>();
			//System.err.println("HERE");

			boolean created=false;
			for(File f : tempExtractionDir.listFiles()){
				if(f.isDirectory()){
					if(!created){
						jarFile.createZipFileFromFolder(f,zipParameters,false, 0);
						created=true;
					} else {
						jarFile.addFolder(f, zipParameters);
					}
				} else{
					if(!created){
						jarFile.createZipFile(f, zipParameters);
						created=true;
					} else {
						jarFile.addFile(f, zipParameters);
					}
				}
			}
		} catch(ZipException e){
			throw new IOException("Unable to create zip (Jar) file '" + jarFile.getFile().getAbsolutePath() + "'" +
				" Following exception thrown:" + System.lineSeparator() + e.getLocalizedMessage());
		}*/
	}

	private static void rectifyClassChanges(File file) throws IOException{
		assert(file.isFile() && !file.isDirectory());
		if(file.getName().endsWith(".class")){
			File original = new File(file.getAbsolutePath() + ORIGINAL_FILE_POST_FIX);
			if(original.exists()){
				try {
					file.delete();
					Files.move(Paths.get(original.getAbsolutePath()), Paths.get(file.getAbsolutePath()));
				}catch(IOException e){
					throw new IOException("Unable to move original file '" + original.getAbsolutePath() + " to '"+
						file.getAbsolutePath() + "'. Exception thrown:" + System.lineSeparator() +
						e.getLocalizedMessage());
				}
			}
		}
	}
}
