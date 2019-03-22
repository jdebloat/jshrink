package edu.ucla.cs.jshrinklib.reachability;

import edu.ucla.cs.jshrinklib.reachability.MethodData;
import edu.ucla.cs.jshrinklib.reachability.ASMClassVisitor;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class MethodDataTest {

	@Test
	public void methodDataBasicTest(){
		String methodName = "method_1";
		String className = "com.example.Class1";
		String methodReturnType = "void";
		String[] args = new String[3];
		args[0] = "java.util.String";
		args[1] = "com.example.Class2";
		args[2] = "int";
		boolean isPublic = true;
		boolean isStatic = true;
		MethodData methodData = new MethodData(methodName, className, methodReturnType, args, isPublic, isStatic);

		assertFalse(methodData.getAnnotation().isPresent());
		assertEquals(methodName, methodData.getName());
		assertEquals(className, methodData.getClassName());
		assertEquals(methodReturnType, methodData.getReturnType());
		assertEquals(args.length, methodData.getArgs().length);
		for(int i=0; i< args.length; i++){
			assertEquals(args[i], methodData.getArgs()[i]);
		}
		assertEquals(isPublic, methodData.isPublic());
		assertEquals(isStatic, methodData.isStatic());
	}

	@Test
	public void methodDataToStringTest(){
		String methodName = "method_1";
		String className = "com.example.Class1";
		String methodReturnType = "void";
		String[] args = new String[3];
		args[0] = "java.util.String";
		args[1] = "com.example.Class2";
		args[2] = "int";
		boolean isPublic = true;
		boolean isStatic = true;
		MethodData methodData = new MethodData(methodName, className, methodReturnType, args, isPublic, isStatic);

		String expected = "<com.example.Class1: public static void method_1(java.util.String,com.example.Class2,int)>";
		assertEquals(expected, methodData.toString());
	}

	@Test
	public void methodDataAnnotationTest(){
		String methodName = "method_1";
		String className = "com.example.Class1";
		String methodReturnType = "void";
		String[] args = new String[3];
		args[0] = "java.util.String";
		args[1] = "com.example.Class2";
		args[2] = "int";
		boolean isPublic = true;
		boolean isStatic = true;
		MethodData methodData = new MethodData(methodName, className, methodReturnType, args, isPublic, isStatic);
		String annotation = "org.junit.Test";
		methodData.setAnnotation(annotation);

		assertTrue(methodData.getAnnotation().isPresent());
		assertEquals(annotation, methodData.getAnnotation().get());
	}

	@Test
	public void methodDataSignatureBasic() throws IOException {
		String signature =
			"<com.example.Class1: public static void method_1(java.util.String, com.example.Class2, int)>";
		MethodData methodData = new MethodData(signature);

		assertFalse(methodData.getAnnotation().isPresent());
		assertEquals("method_1", methodData.getName());
		assertEquals("com.example.Class1", methodData.getClassName());
		assertEquals("void", methodData.getReturnType());
		assertEquals(3, methodData.getArgs().length);
		assertEquals("java.util.String", methodData.getArgs()[0]);
		assertEquals("com.example.Class2", methodData.getArgs()[1]);
		assertEquals("int", methodData.getArgs()[2]);
		assertTrue(methodData.isPublic());
		assertTrue(methodData.isStatic());

	}

	@Test
	public void methodDataSignatureInit() throws IOException {
		String signature =
				"<com.example.Class1: void <init>(java.util.String, com.example.Class2, int)>";
		MethodData methodData = new MethodData(signature);

		assertFalse(methodData.getAnnotation().isPresent());
		assertEquals("<init>", methodData.getName());
		assertEquals("com.example.Class1", methodData.getClassName());
		assertEquals("void", methodData.getReturnType());
		assertEquals(3, methodData.getArgs().length);
		assertEquals("java.util.String", methodData.getArgs()[0]);
		assertEquals("com.example.Class2", methodData.getArgs()[1]);
		assertEquals("int", methodData.getArgs()[2]);
		assertFalse(methodData.isPublic());
		assertFalse(methodData.isStatic());
	}

	@Test
	public void testJUnit3Test() throws FileNotFoundException, IOException {
		String testClassPath = "src/test/resources/RepeatedTestTest.class";
		ClassReader cr = new ClassReader(new FileInputStream(testClassPath));
		Set<String> classes = new HashSet<String>();
		Set<MethodData> methods = new HashSet<MethodData>();
		cr.accept(new ASMClassVisitor(Opcodes.ASM5, classes, methods, null, null), ClassReader.SKIP_DEBUG);
		for(MethodData md : methods) {
			if(md.getName().startsWith("test")) {
				assertTrue(md.isJUnit3Test());
			}
		}
	}
}
