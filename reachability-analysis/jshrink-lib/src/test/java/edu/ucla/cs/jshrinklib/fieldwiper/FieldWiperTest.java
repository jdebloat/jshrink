package edu.ucla.cs.jshrinklib.fieldwiper;

import edu.ucla.cs.jshrinklib.TestUtils;
import edu.ucla.cs.jshrinklib.reachability.FieldData;
import edu.ucla.cs.jshrinklib.util.SootUtils;
import org.junit.Test;
import soot.RefType;
import soot.SootClass;
import soot.SootField;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FieldWiperTest {
    private SootClass getSootClassFromResources(String className) {
        ClassLoader classLoader = FieldWiperTest.class.getClassLoader();
        String classPath = new File(classLoader.getResource("fieldwiper" + File.separator + className + ".class").getFile()).getParentFile().getAbsolutePath();
        return TestUtils.getSootClass(classPath, className);
    }

    @Test
    public void testGenericType() {
        SootClass sootClass = getSootClassFromResources("CornerCases");
        SootField field = sootClass.getField("field_with_generic_type", RefType.v("java.util.Set"));
        assertNotNull(field);

        FieldData fieldData = SootUtils.sootFieldToFieldData(field);
        FieldData expected = new FieldData("field_with_generic_type", "CornerCases", false, "java.util.Set");
        assertEquals(expected, fieldData);
    }

    @Test
    public void testSimpleWipeField() {
        SootClass sootClass = getSootClassFromResources("SimpleClass");
        String before = TestUtils.runClass(sootClass);
        // remove the unused field, f2 in SimpleClass.java
        SootField field = sootClass.getField("f2", RefType.v("java.lang.String"));
        sootClass.removeField(field);
        // the output should still be the same
        String after = TestUtils.runClass(sootClass);
        assertEquals(before, after);
    }
}
