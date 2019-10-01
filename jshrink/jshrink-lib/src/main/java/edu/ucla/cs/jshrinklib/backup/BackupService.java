package edu.ucla.cs.jshrinklib.backup;

import java.nio.file.Path;
import java.util.HashMap;

public class BackupService {
	HashMap<String, Checkpoint> checkpoints;
	String backupFolder;
	String realPath;
	boolean isVerbose;
	String lastCheckpoint;

	public BackupService(String realPath, String backupFolder, boolean verbose){
		this.backupFolder = backupFolder;
		this.isVerbose = verbose;
		this.realPath = realPath;
	}
	public boolean addCheckpoint(String checkpointName){
		Checkpoint c = new Checkpoint(this.realPath, this.backupFolder, checkpointName, this.isVerbose);
		if(!c.isValid())
			return false;
		this.checkpoints.put(c.transformation, c);
		this.lastCheckpoint = c.transformation;
		return true;
	}

	public boolean validateCheckpoint(String checkpointName){
		Checkpoint c = checkpoints.get(checkpointName);
		if(c == null)
			return false;
		return c.isValid() && c.isSafe();
	}
	public boolean validateLastCheckpoint() {
		return this.validateCheckpoint(lastCheckpoint);
	}
	//public revertToOriginal
	//public revertToLast

}
