package edu.ucla.cs.onr.classcollapser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import edu.ucla.cs.onr.util.SootUtils;
import org.junit.After;
import org.junit.Test;
import soot.*;
import soot.jimple.InvokeStmt;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClassCollapserTest {
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
    public void mergeTwoClassesTest_override() {
        SootClass A = getSootClassFromResources("src/test/resources/classcollapser/override/original/","A");
        SootClass B = getSootClassFromResources("src/test/resources/classcollapser/override/original/","B");

        assertEquals(2, A.getMethods().size());
        assertEquals(2, B.getMethodCount());

        ClassCollapser.mergeTwoClasses(B, A);

        assertEquals(2, A.getMethodCount());
        for (Unit u: A.getMethodByName("foo").retrieveActiveBody().getUnits()) {
            if (u instanceof InvokeStmt) {
                assertEquals("\"class B\"", ((InvokeStmt)u).getInvokeExpr().getArg(0).toString());
            }
        }
    }

    @Test
    public void mergeTwoClassesTest_field() {
        SootClass A = getSootClassFromResources("src/test/resources/classcollapser/field/original/","A");
        SootClass B = getSootClassFromResources("src/test/resources/classcollapser/field/original/","B");

        assertEquals(1, A.getFieldCount());
        assertEquals(1, B.getFieldCount());

        ClassCollapser.mergeTwoClasses(B, A);

        assertEquals(2, A.getFieldCount());
        assertNotNull(A.getFieldByName("a"));
        assertNotNull(A.getFieldByName("b"));
    }
}
