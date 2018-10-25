package edu.ucla.cs.onr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import edu.ucla.cs.onr.reachability.MethodData;
import edu.ucla.cs.onr.reachability.ASMClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;


public class ASMUtils {

	public static void readClassFromJarFile(JarFile jarFile, Set<String> classes,
    		Set<MethodData> methods) {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
        	final JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
            	try {
                	ClassReader cr = new ClassReader(jarFile.getInputStream(entry));
                	cr.accept(new ASMClassVisitor(Opcodes.ASM5, classes, methods), ClassReader.SKIP_DEBUG);
                } catch (IllegalArgumentException ex) {
                	continue;
                } catch (IOException ex){
	                //TODO: Fix this. Not sure if here is the best way to handle it, but ok for the meantime
                    System.err.println("An an exception was thrown when reading data from .jar file:");
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }
    
    public static void readClassFromDirectory(File dirPath, Set<String> classes,
    		Set<MethodData> methods) {
    	
    	if(!dirPath.exists()) {
    		// fix NPE due to non-existent file
    		System.err.println(dirPath.getAbsolutePath() + " does not exist.");
    		return;
    	}
    	
    	for(File f : dirPath.listFiles()) {
    		if(f.isDirectory()) {
    			readClassFromDirectory(f, classes, methods);
    		} else {
    			String fName = f.getName();
    			if(fName.endsWith(".class")) {
    				try {
						ClassReader cr = new ClassReader(new FileInputStream(f));
						cr.accept(new ASMClassVisitor(Opcodes.ASM5, classes, methods), ClassReader.SKIP_DEBUG);
					} catch (IOException e) {
						e.printStackTrace();
					}
    			}
    		}
    	}
    }

    public static void readClass(File dir, Set<String> classes, Set<MethodData> methods){
    	if(dir.isDirectory()){
    		readClassFromDirectory(dir,classes,methods);
	    } else if (dir.getName().endsWith(".jar")) {
    		JarFile j = null;
    		try {
    			j = new JarFile(dir);
    			readClassFromJarFile(j,classes,methods);
		    } catch (IOException e){
    			e.printStackTrace();
		    }
	    } else {
	    	System.err.println("Cannot read classes from '" + dir.getAbsolutePath() +
	    			"'. It is neither a directory or a jar.");
	    }
    }
}
