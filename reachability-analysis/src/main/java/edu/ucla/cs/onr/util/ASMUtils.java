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
    private static void readClassFromJarFile(JarFile jarFile, Set<String> classes, Set<MethodData> methods) {
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
                    System.err.println(ex.getLocalizedMessage());
                    System.exit(1);
                }
            }
        }
    }
    
    private static void readClassFromDirectory(File dirPath, Set<String> classes, Set<MethodData> methods) {

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
    	if(dir.isDirectory() || dir.getName().endsWith(".class")){
    		readClassFromDirectory(dir,classes,methods);
	    } else {
    		JarFile j = null;
    		try {
    			j = new JarFile(dir);
		    } catch (IOException e){
    			//TODO: Fix this. Not sure if here is the best way to handle it, but ok for the meantime
    			System.err.println("File '" + dir.getAbsolutePath() +"' is neither a directory, '.class' file " +
				    "or a jar." + "Cannot process.");
    			System.exit(1);
		    }

		    assert(j != null);
    		readClassFromJarFile(j,classes,methods);
	    }
    }
}
