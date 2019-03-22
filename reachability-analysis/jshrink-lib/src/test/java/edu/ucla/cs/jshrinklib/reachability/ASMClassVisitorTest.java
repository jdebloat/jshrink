package edu.ucla.cs.jshrinklib.reachability;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ASMClassVisitorTest {
    @Test
    public void testVisitClassMethodsAndFields() {
        ClassLoader classLoader = ASMClassVisitorTest.class.getClassLoader();
        String pathToClassFile = classLoader.getResource("ASMClassVisitor.class").getFile();
        Set<String> classes = new HashSet<String>();
        Set<MethodData> methods = new HashSet<MethodData>();
        Set<FieldData> fields = new HashSet<FieldData>();
        Map<MethodData, Set<FieldData>> fieldRefs = new HashMap<MethodData, Set<FieldData>>();
        try {
            FileInputStream fis = new FileInputStream(pathToClassFile);
            ClassReader cr = new ClassReader(fis);
            ASMClassVisitor cv = new ASMClassVisitor(Opcodes.ASM5, classes, methods, fields, fieldRefs);
            cr.accept(cv, ClassReader.SKIP_DEBUG);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(4, methods.size());
        assertEquals(5, fields.size());
    }

    @Test
    public void testVisitFieldReferences() {
        ClassLoader classLoader = ASMClassVisitorTest.class.getClassLoader();
        String pathToClassFile = classLoader.getResource("simple-test-project"
                + File.separator + "target" + File.separator + "classes" + File.separator + "Main.class").getFile();
        Set<String> classes = new HashSet<String>();
        Set<MethodData> methods = new HashSet<MethodData>();
        Set<FieldData> fields = new HashSet<FieldData>();
        Map<MethodData, Set<FieldData>> fieldRefs = new HashMap<MethodData, Set<FieldData>>();
        try {
            FileInputStream fis = new FileInputStream(pathToClassFile);
            ClassReader cr = new ClassReader(fis);
            ASMClassVisitor cv = new ASMClassVisitor(Opcodes.ASM5, classes, methods, fields, fieldRefs);
            cr.accept(cv, ClassReader.SKIP_DEBUG);
            MethodData method1 = new MethodData("main", "Main", "void", new String[] {"java.lang.String[]"}, true, true);
            Set<FieldData> fieldReferences1 = fieldRefs.get(method1);
            assertNotNull(fieldReferences1);
            assertEquals(2, fieldReferences1.size());
            FieldData f1 = null;
            FieldData f2 = null;
            for(FieldData field : fieldReferences1) {
                if(field.getName().equals("f1")) {
                    f1 = field;
                } else if (field.getName().equals("f2")) {
                    f2 = field;
                }
            }
            assertNotNull(f1);
            assertFalse(f1.isStatic());
            assertNotNull(f2);
            assertTrue(f2.isStatic());

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testVisitFieldReferences2() {
        ClassLoader classLoader = ASMClassVisitorTest.class.getClassLoader();
        String pathToClassFile = classLoader.getResource("simple-test-project"
                + File.separator + "target" + File.separator + "classes" + File.separator + "StandardStuff.class").getFile();
        Set<String> classes = new HashSet<String>();
        Set<MethodData> methods = new HashSet<MethodData>();
        Set<FieldData> fields = new HashSet<FieldData>();
        Map<MethodData, Set<FieldData>> fieldRefs = new HashMap<MethodData, Set<FieldData>>();
        try {
            FileInputStream fis = new FileInputStream(pathToClassFile);
            ClassReader cr = new ClassReader(fis);
            ASMClassVisitor cv = new ASMClassVisitor(Opcodes.ASM5, classes, methods, fields, fieldRefs);
            cr.accept(cv, ClassReader.SKIP_DEBUG);
            MethodData method1 = new MethodData("getStringStatic", "StandardStuff", "java.lang.String", new String[] {"int"}, false, true);
            Set<FieldData> fieldReferences1 = fieldRefs.get(method1);
            assertNotNull(fieldReferences1);
            assertEquals(1, fieldReferences1.size());
            FieldData f1 = null;
            FieldData f2 = null;
            FieldData f3 = null;
            for(FieldData field : fieldReferences1) {
                if(field.getName().equals("HELLO_WORLD_STRING")) {
                    f1 = field;
                } else if (field.getName().equals("GOODBYE_STRING")) {
                    f2 = field;
                } else if (field.getName().equals("out")) {
                    f3 = field;
                }
            }
            assertNull(f1); // this private static final field is inlined by the Java compiler
            assertNull(f2);
            assertNotNull(f3);
            assertTrue(f3.isStatic());

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
