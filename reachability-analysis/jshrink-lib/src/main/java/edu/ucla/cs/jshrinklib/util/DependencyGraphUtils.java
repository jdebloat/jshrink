package edu.ucla.cs.jshrinklib.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import edu.ucla.cs.jshrinklib.reachability.ASMClassVisitor;
import edu.ucla.cs.jshrinklib.reachability.ClassReferenceGraph;
import edu.ucla.cs.jshrinklib.reachability.FieldData;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;


public class DependencyGraphUtils {

    public static void readClassFromJarFile(JarFile jarFile, Set<String> classes,
                                            Set<MethodData> methods, Set<FieldData> fields, Map<MethodData, Set<FieldData>> fieldReferences,
                                            Map<MethodData, Set<MethodData>> virtualMethodCalls,
                                            ClassReferenceGraph dependencyGraph) {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            // there is a module-info.class in jars built by Java 9
            if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info")) {
                try {
                    ClassReader cr = new ClassReader(jarFile.getInputStream(entry));
                    cr.accept(new ASMClassVisitor(Opcodes.ASM5, classes, methods, fields, fieldReferences, virtualMethodCalls), ClassReader.SKIP_DEBUG);
                    if(dependencyGraph != null){
                        Path temp = Files.createTempFile("dependency_graph_", ".class" );
                        Files.copy(jarFile.getInputStream(entry), temp, StandardCopyOption.REPLACE_EXISTING);
                        /*OutputStream outStream = new FileOutputStream(targetFile);
                        InputStream initialStream = jarFile.getInputStream(entry);
                        byte[] buffer = new byte[initialStream.available()];
                        initialStream.read(buffer);
                        outStream.write(buffer);*/
                        dependencyGraph.addClass(cr.getClassName(), temp.toFile().getAbsolutePath());
                        temp.toFile().delete();
                    }
                } catch (IllegalArgumentException ex) {
                    continue;
                } catch (IOException ex){
                    //Not sure if here is the best way to handle it, but ok for the meantime
                    System.err.println("An an exception was thrown when reading data from .jar file:");
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    public static void readClassFromDirectory(File dirPath, Set<String> classes,
                                              Set<MethodData> methods, Set<FieldData> fields, Map<MethodData, Set<FieldData>> fieldReferences,
                                              Map<MethodData, Set<MethodData>> virtualMethodCalls, ClassReferenceGraph dependencyGraph) {

        if(!dirPath.exists()) {
            // fix NPE due to non-existent file
            System.err.println(dirPath.getAbsolutePath() + " does not exist.");
            return;
        }

        for(File f : dirPath.listFiles()) {
            if(f.isDirectory()) {
                readClassFromDirectory(f, classes, methods, fields, fieldReferences, virtualMethodCalls, dependencyGraph);
            } else {
                String fName = f.getName();
                if(fName.endsWith(".class")) {
                    try {
                        FileInputStream fis = new FileInputStream(f);
                        ClassReader cr = new ClassReader(fis);
                        cr.accept(new ASMClassVisitor(Opcodes.ASM5, classes, methods, fields, fieldReferences, virtualMethodCalls), ClassReader.SKIP_DEBUG);
                        fis.close();
                        dependencyGraph.addClass(cr.getClassName(), f.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void readClassWithDependencies(File dir, Set<String> classes, Set<MethodData> methods, Set<FieldData> fields,
                                 Map<MethodData, Set<FieldData>> fieldReferences, Map<MethodData, Set<MethodData>> virtualMethodCalls,
                                 ClassReferenceGraph dependencyGraph){
        try {
            if (dir.isDirectory()) {
                readClassFromDirectory(dir, classes, methods, fields, fieldReferences, virtualMethodCalls, dependencyGraph);
            } else if (dir.getName().endsWith(".jar")) {
                JarFile j = null;
                try {
                    j = new JarFile(dir);
                    readClassFromJarFile(j, classes, methods, fields, fieldReferences, virtualMethodCalls, dependencyGraph);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                throw new IOException("Cannot read classes from '" + dir.getAbsolutePath() +
                        "'. It is neither a directory or a jar.");
            }
        }catch(IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}
