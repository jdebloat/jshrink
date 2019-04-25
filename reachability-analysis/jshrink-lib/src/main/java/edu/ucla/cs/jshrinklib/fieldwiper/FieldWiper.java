package edu.ucla.cs.jshrinklib.fieldwiper;

import edu.ucla.cs.jshrinklib.util.SootUtils;
import soot.SootClass;
import soot.SootField;

public class FieldWiper {
    public static boolean removeField(SootField field) {
        SootClass owningClass = field.getDeclaringClass();
        if(SootUtils.modifiableSootClass(owningClass)) {
            owningClass.removeField(field);
            if(!SootUtils.modifiableSootClass(owningClass)){
                owningClass.addField(field);
                return false;
            }
            return true;
        }
        return false;
    }
}
