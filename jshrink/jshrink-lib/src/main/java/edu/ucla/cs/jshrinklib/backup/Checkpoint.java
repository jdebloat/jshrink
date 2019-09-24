package edu.ucla.cs.jshrinklib.backup;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Checkpoint {
	private java.time.Instant timestamp;
	private Path backupPath;
	private Path oldPath;
	private String transformation;
	private boolean testsPassed;
	private boolean isVerbose;
	public boolean rollBack;

	private boolean runTests(){
		return false;
	}

	private void copyFiles(Path oldPath, Path newPath) throws IOException {
		FileUtils.copyDirectory(oldPath.toFile(), newPath.toFile());
//		for(Path source: Files.walk(oldPath).collect(Collectors.toSet())){
//			Path target = newPath.resolve(oldPath.getParent().relativize(source));
//			if(source.toFile().isDirectory()){
//				target.toFile().mkdirs();
//			}
//			else{
//				if(!target.getParent().toFile().exists())
//					target.getParent().toFile().mkdirs();
//				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
//			}
//
//		}
	}
	private boolean backup(String realPath, String backupPath){
		try{
			this.oldPath = Paths.get(realPath);
			File backupFolder = new File(backupPath+File.separator+"backup");
			backupFolder.mkdirs();
			if (!this.oldPath.toFile().isDirectory() ||  (!backupFolder.isDirectory()))
				throw new IllegalArgumentException("Input for backup is not a folder");

			this.backupPath = Paths.get(backupFolder.getAbsolutePath()+File.separator+this.oldPath.getFileName());
			copyFiles(this.oldPath, this.backupPath);
		}
		catch(Exception e){
			System.err.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public Checkpoint(String realPath, String backupFolder, String transformation, boolean isVerbose){
		this.transformation = transformation;
		this.isVerbose = isVerbose;
		this.rollBack = false;
		if(!this.backup(realPath, backupFolder))
		{
			throw new IllegalArgumentException("Checkpoint creation failed");
		}
		this.timestamp = java.time.Instant.now();
	}

	public Checkpoint(String realPath, String backupPath, String transformation){
		this(realPath, backupPath, transformation, false);
	}

	public boolean rollBackToBackup(){
		try {
			this.copyFiles(this.backupPath, this.oldPath);
			this.rollBack = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.rollBack;
	}

	public boolean isSafe(){
		return this.testsPassed || this.runTests();
	}

	public void exit(){
		System.exit(0);
	}

	public Path getRealPath(){
		return this.oldPath;
	}

	public Path getBackupPath(){
		return this.backupPath;
	}
}
