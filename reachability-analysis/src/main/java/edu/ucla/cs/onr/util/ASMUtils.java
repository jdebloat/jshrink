package edu.ucla.cs.onr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import edu.ucla.cs.onr.reachability.ASMClassVisitor;

public class ASMUtils {
    public static void readClassFromJarFile(File jarPath, Set<String> classes,
    		Set<String> methods) {
    	if(!jarPath.getName().endsWith(".jar")) {
    		System.err.println(jarPath.getAbsolutePath() + " is not a jar file.");
    		return;
    	}
        try {
        	JarFile jarFile = new JarFile(jarPath);
        	final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
            	final JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                	try {
                		ClassReader cr = new ClassReader(jarFile.getInputStream(entry));
                		cr.accept(new ASMClassVisitor(Opcodes.ASM5, classes, methods), ClassReader.SKIP_DEBUG);
                	} catch (IllegalArgumentException ex) {
                		continue;
                	}
                }
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static void readClassFromDirectory(File dirPath, Set<String> classes,
    		Set<String> methods) {
    	
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
}
