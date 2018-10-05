package edu.ucla.cs.onr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ASMUtils {
    public static void readClassFromJarFile(String jarPath, HashSet<String> classes, 
    		HashSet<String> methods) {
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
    
    public static void readClassFromDirectory(String dirPath, HashSet<String> classes, 
    		HashSet<String> methods) {
    	File dir = new File(dirPath);
    	for(File f : dir.listFiles()) {
    		if(f.isDirectory()) {
    			readClassFromDirectory(f.getAbsolutePath(), classes, methods);
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
