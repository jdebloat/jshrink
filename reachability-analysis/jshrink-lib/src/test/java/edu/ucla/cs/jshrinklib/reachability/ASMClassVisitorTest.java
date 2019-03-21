package edu.ucla.cs.jshrinklib.reachability;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ASMClassVisitorTest {
    @Test
    public void testVisitClassMethodsAndFields() {
        ClassLoader classLoader = ASMClassVisitorTest.class.getClassLoader();
        String pathToClassFile = classLoader.getResource("ASMClassVisitor.class").getFile();
        Set<String> classes = new HashSet<String>();
        Set<MethodData> methods = new HashSet<MethodData>();
        Set<FieldData> fields = new HashSet<FieldData>();
        try {
            FileInputStream fis = new FileInputStream(pathToClassFile);
            ClassReader cr = new ClassReader(fis);
            cr.accept(new ASMClassVisitor(Opcodes.ASM5, classes, methods, fields), ClassReader.SKIP_DEBUG);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(4, methods.size());
        assertEquals(5, fields.size());
    }
}
