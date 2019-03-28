package edu.ucla.cs.jshrinklib.classcollapser;

import edu.ucla.cs.jshrinklib.TestUtils;
import edu.ucla.cs.jshrinklib.reachability.MethodData;
import org.junit.After;
import org.junit.Test;
import soot.*;
import soot.jimple.InvokeStmt;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class ClassCollapserTest {

    @After
    public void after(){
        G.reset();
    }

    @Test
    public void classClassifierTest() throws IOException {
        String overridePath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser"
                    + File.separator + "override" + File.separator + "original").getFile()).getAbsolutePath();
        SootClass A = TestUtils.getSootClass(overridePath,"A");
        SootClass B = TestUtils.getSootClass(overridePath,"B");

        Set<String> appClasses = new HashSet<String>();
        appClasses.add(A.getName());
        appClasses.add(B.getName());

        Set<String> usedAppClasses = new HashSet<String>();
        usedAppClasses.add(B.getName());

        Set<MethodData> usedAppMethodData = new HashSet<MethodData>();
        for (SootMethod m : B.getMethods()) {
            usedAppMethodData.add(new MethodData(m.getSignature()));
        }

        ClassCollapserAnalysis classCollapserAnalysis
                = new ClassCollapserAnalysis(appClasses,usedAppClasses,usedAppMethodData);
        classCollapserAnalysis.run();

        ClassCollapser classCollapser = new ClassCollapser();
        classCollapser.run(classCollapserAnalysis);

        ClassCollapserData classCollapserData = classCollapser.getClassCollapserData();
        assertTrue(classCollapserData.getClassesToRemove().contains(B.getName()));
        assertEquals(1, classCollapserData.getClassesToRemove().size());
        assertTrue(classCollapserData.getClassesToRewrite().contains(A.getName()));
        assertEquals(1, classCollapserData.getClassesToRewrite().size());
    }

    @Test
    public void mergeTwoClassesTest_override() {
        String overridePath
            = new File(ClassCollapser.class.getClassLoader()
            .getResource("classcollapser"
                + File.separator + "override" + File.separator + "original").getFile()).getAbsolutePath();
        SootClass A = TestUtils.getSootClass(overridePath,"A");
        SootClass B = TestUtils.getSootClass(overridePath,"B");

        assertEquals(2, A.getMethods().size());
        assertEquals(2, B.getMethodCount());

        HashMap<String, Set<String>> usedMethods = new HashMap<String, Set<String>>();
        usedMethods.put("B", new HashSet<String>());
        for (SootMethod m : B.getMethods()) {
            usedMethods.get("B").add(m.getSubSignature());
        }

        System.out.println(usedMethods);

        ClassCollapser.mergeTwoClasses(B, A, usedMethods);

        assertEquals(2, A.getMethodCount());
        for (Unit u: A.getMethodByName("foo").retrieveActiveBody().getUnits()) {
            if (u instanceof InvokeStmt) {
                assertEquals("\"class B\"", ((InvokeStmt)u).getInvokeExpr().getArg(0).toString());
            }
        }
    }

    @Test
    public void mergeTwoClassesTest_field() {
        String fieldPath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser" + File.separator
                    + "field" + File.separator + "original").getFile()).getAbsolutePath();
        SootClass A = TestUtils.getSootClass(fieldPath,"A");
        SootClass B = TestUtils.getSootClass(fieldPath,"B");

        assertEquals(1, A.getFieldCount());
        assertEquals(1, B.getFieldCount());

        ClassCollapser.mergeTwoClasses(B, A, new HashMap<String, Set<String>>());

        assertEquals(2, A.getFieldCount());
        assertNotNull(A.getFieldByName("a"));
        assertNotNull(A.getFieldByName("b"));
    }

    @Test
    public void changeClassNameTest_override() {
        String overridePath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser" + File.separator
                    + "override" + File.separator + "original").getFile()).getAbsolutePath();
        SootClass A = TestUtils.getSootClass(overridePath,"A");
        SootClass B = TestUtils.getSootClass(overridePath,"B");
        SootClass main = TestUtils.getSootClass(overridePath, "Main");

        ClassCollapser.changeClassNamesInClass(main, B, A);
        for (SootMethod m: main.getMethods()) {
            Body body = m.retrieveActiveBody();
            for (Local l: body.getLocals()) {
                assertNotEquals("B", l.getType().toString());
            }
        }
    }

    @Test
    public void classClassifierTest_simpleCollapseExample() throws IOException{
        String overridePath
                = new File(ClassCollapser.class.getClassLoader()
                .getResource("classcollapser" + File.separator
                    + "simple-collapse-example" + File.separator + "target" + File.separator + "classes").getFile()).getAbsolutePath();
        SootClass A = TestUtils.getSootClass(overridePath,"A");
        SootClass B = TestUtils.getSootClass(overridePath,"B");
        SootClass C = TestUtils.getSootClass(overridePath,"C");
        SootClass Main = TestUtils.getSootClass(overridePath,"Main");

        Set<String> appClasses = new HashSet<String>();
        appClasses.add(A.getName());
        appClasses.add(B.getName());
        appClasses.add(C.getName());
        appClasses.add(Main.getName());

        Set<String> usedAppClasses = new HashSet<String>();
        usedAppClasses.add(B.getName());
        usedAppClasses.add(Main.getName());

        Set<MethodData> usedAppMethodData = new HashSet<MethodData>();
        for (SootMethod m : B.getMethods()) {
            usedAppMethodData.add(new MethodData(m.getSignature()));
        }

        ClassCollapserAnalysis classCollapserAnalysis
                = new ClassCollapserAnalysis(appClasses,usedAppClasses,usedAppMethodData);
        classCollapserAnalysis.run();

        ClassCollapser classCollapser = new ClassCollapser();
        classCollapser.run(classCollapserAnalysis);

        ClassCollapserData classCollapserData = classCollapser.getClassCollapserData();

        assertEquals(1,classCollapserData.getClassesToRemove().size());
        assertTrue(classCollapserData.getClassesToRemove().contains(B.getName()));

        assertEquals(2, classCollapserData.getClassesToRewrite().size());
        assertTrue(classCollapserData.getClassesToRewrite().contains(A.getName()));
        assertTrue(classCollapserData.getClassesToRewrite().contains(Main.getName()));

        assertNotNull(A.getMethodByName("getClassType"));
        assertNotNull(A.getMethodByName("saySomething"));
        assertNotNull(A.getMethodByName("uniqueToA"));
        assertNotNull(A.getMethodByName("uniqueToB"));

        SootMethod saySomething = A.getMethodByName("saySomething");
        assertTrue(saySomething.retrieveActiveBody().toString().contains("\"I am class B\""));
    }
}
