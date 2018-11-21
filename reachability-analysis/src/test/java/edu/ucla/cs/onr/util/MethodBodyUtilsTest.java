package edu.ucla.cs.onr.util;

import org.junit.After;
import org.junit.Test;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class MethodBodyUtilsTest {
    private static SootClass getSootClassFromResources(String pathName, String className){
//		ClassLoader classLoader = MethodWiperTest.class.getClassLoader();
//		File classFile = new File(classLoader.getResource(className + ".class").getFile());
        // the code above throws an exception about unfound resources
        // below is a temporary patch
        //TODO: Fix this --- cannot get load resources working across eclipse version. (Copied from MethodWiperTest)
        File classFile = new File(pathName + className + ".class");

        final String workingClasspath=classFile.getParentFile().getAbsolutePath();
        Options.v().set_soot_classpath(SootUtils.getJREJars() + File.pathSeparator + workingClasspath);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);

        List<String> processDirs = new ArrayList<String>();
        processDirs.add(workingClasspath);
        Options.v().set_process_dir(processDirs);

        SootClass sClass = Scene.v().loadClassAndSupport(className);
        Scene.v().loadNecessaryClasses();


        return sClass;
    }


    @After
    public void before(){
        G.reset();
    }

    @Test
    public void isEmptyConstructorTest_noConstructor() {
        SootClass A = getSootClassFromResources("src/test/resources/methodbodyutils/no_constructor/", "A");
        SootClass B = getSootClassFromResources("src/test/resources/methodbodyutils/no_constructor/", "B");

        assertTrue(MethodBodyUtils.isEmptyConstructor(A.getMethodByName("<init>")));
        assertTrue(MethodBodyUtils.isEmptyConstructor(B.getMethodByName("<init>")));
    }

    @Test
    public void isEmptyConstructorTest_emptyConstructor() {
        SootClass A = getSootClassFromResources("src/test/resources/methodbodyutils/empty_constructor/", "A");
        SootClass B = getSootClassFromResources("src/test/resources/methodbodyutils/empty_constructor/", "B");

        assertTrue(MethodBodyUtils.isEmptyConstructor(A.getMethodByName("<init>")));
        assertTrue(MethodBodyUtils.isEmptyConstructor(B.getMethodByName("<init>")));
    }

    @Test
    public void isEmptyConstructorTest_constructorWithBody() {
        SootClass A = getSootClassFromResources("src/test/resources/methodbodyutils/constructor_with_body/", "A");

        assertFalse(MethodBodyUtils.isEmptyConstructor(A.getMethodByName("<init>")));

    }

    @Test
    public void isEmptyConstructorTest_parameterMismatch() {
        SootClass A = getSootClassFromResources("src/test/resources/methodbodyutils/constructor_with_parameter/", "A");
        SootClass B = getSootClassFromResources("src/test/resources/methodbodyutils/constructor_with_parameter/", "B");

        assertFalse(MethodBodyUtils.isEmptyConstructor(B.getMethodByName("<init>")));
    }
}
