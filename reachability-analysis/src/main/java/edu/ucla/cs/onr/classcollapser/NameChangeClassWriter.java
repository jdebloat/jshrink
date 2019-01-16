package edu.ucla.cs.onr.classcollapser;

import edu.ucla.cs.onr.util.ASMUtils;
import org.objectweb.asm.*;
import org.omg.CosNaming.NameComponentHelper;

import java.util.ArrayList;


public class NameChangeClassWriter extends ClassVisitor implements NameChangeWriter {
    private boolean changed = false;

    private final String changeFrom;
    private final String changeTo;

    private ArrayList<NameChangeWriter> childrenWriters;

    public NameChangeClassWriter(final int api, final ClassVisitor cv, final String changeFromClassName, final String changeToClassName) {
        super(api, cv);
        changeFrom = changeFromClassName;
        changeTo = changeToClassName;
        childrenWriters = new ArrayList<NameChangeWriter>();
    }

    public boolean isChanged() {
        if (changed) {
            return true;
        }
        for (NameChangeWriter ncw: childrenWriters) {
            if (ncw.isChanged()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {

        String newSuperName = superName;
        if (superName.equals(changeFrom)) {
            changed = true;
            newSuperName = changeTo;
        }

        String newInterfaces[] = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            if (interfaces[i].equals(changeFrom)) {
                changed = true;
                newInterfaces[i] = changeTo;
            } else {
                newInterfaces[i] = interfaces[i];
            }
        }

        super.visit(version, access, name, signature, newSuperName, newInterfaces);
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        if (owner.equals(changeFrom)) {
            changed = true;
            super.visitOuterClass(changeTo, name, descriptor);
        } else {
            super.visitOuterClass(owner, name, descriptor);
        }
    }

    @Override
    public void visitInnerClass(
            final String name, final String outerName, final String innerName, final int access) {
        String newName = name;
        String newOuterName = outerName;
        String newInnerName = innerName;
        if (name != null && name.equals(changeFrom)) {
            changed = true;
            newName = changeTo;
            String[] innerNameList = changeTo.split("/");
            newInnerName = innerNameList[innerNameList.length-1];
        }
        if (outerName != null && outerName.equals(changeFrom)) {
            changed = true;
            newOuterName = changeTo;
        }
        super.visitInnerClass(newName, newOuterName, newInnerName, access);
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
        String newDescriptor = ASMUtils.changeType(descriptor, changeFrom, changeTo);
        if (!newDescriptor.equals(descriptor)) {
            changed = true;
        }
        NameChangeFieldWriter toReturn = new NameChangeFieldWriter(api, cv.visitField(access, name, newDescriptor, signature, value));
        childrenWriters.add(toReturn);
        return toReturn;
    }


    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        Type returnType = Type.getReturnType(descriptor);
        Type newReturnType = Type.getType(ASMUtils.changeType(returnType.getDescriptor(),changeFrom, changeTo));
        Type[] arguTypes = Type.getArgumentTypes(descriptor);
        Type[] newArguTypes = new Type[arguTypes.length];
        for (int i = 0; i < arguTypes.length; ++i) {
            newArguTypes[i] = Type.getType(ASMUtils.changeType(arguTypes[i].getDescriptor(), changeFrom, changeTo));
        }
        String newDescriptor = Type.getMethodDescriptor(newReturnType, newArguTypes);
        if (!newDescriptor.equals(descriptor)) {
            changed = true;
        }
        NameChangeMethodWriter toReturn = new NameChangeMethodWriter(api, cv.visitMethod(access, name, newDescriptor, signature, exceptions), changeFrom, changeTo);
        childrenWriters.add(toReturn);
        return toReturn;
    }
}
