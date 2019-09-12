package edu.ucla.cs.jshrinklib.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Checkpoint {
	private java.time.Instant timestamp;
	private Path backupPath;
	private Path oldPath;
	private String transformation;
	private boolean testsPassed;
	private boolean isVerbose;

	private void runTests(){

	}

	private void copyFiles(Path oldPath, Path newPath) throws IOException {
		for(Path source: Files.walk(oldPath).collect(Collectors.toList())){
			Files.copy(source, newPath.resolve(oldPath.relativize(source)));
		}
	}
	private boolean backup(String realPath, String backupPath){
		try{
			this.oldPath = Paths.get(realPath);
			File backupFolder = new File(backupPath+File.separator+"backup");
			backupFolder.mkdirs();
			if (!this.oldPath.toFile().isDirectory() ||  (!backupFolder.isDirectory()))
				throw new IllegalArgumentException("Input for backup is not a folder");

			this.backupPath = Paths.get(backupFolder.toURI());
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
		if(!this.backup(realPath, backupFolder))
		{
			throw new IllegalArgumentException("Checkpoint creation failed");
		}
		this.timestamp = java.time.Instant.now();
	}

	public Checkpoint(String realPath, String backupPath, String transformation){
		this(realPath, backupPath, transformation, false);
	}

	public boolean rollBack(){
		return false;
	}

	public boolean isSafe(){
		return this.testsPassed || false;
	}

	public void exit(){
		System.exit(0);
	}
}
