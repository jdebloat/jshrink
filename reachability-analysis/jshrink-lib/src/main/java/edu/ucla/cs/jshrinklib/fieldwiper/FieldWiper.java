package edu.ucla.cs.jshrinklib.fieldwiper;

import edu.ucla.cs.jshrinklib.util.SootUtils;
import soot.SootClass;
import soot.SootField;

public class FieldWiper {
    public static void removeField(SootField field) {
        SootClass owningClass = field.getDeclaringClass();
        owningClass.removeField(field);
    }
}
