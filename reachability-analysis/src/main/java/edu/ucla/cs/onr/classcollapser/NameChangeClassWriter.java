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
//        System.out.println(signature);
//        System.out.println(name);
//        System.out.println(superName);

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
//        if (cv != null) {
//            cv.visit(version, access, name, signature, superName, interfaces);
//        }
    }

//    /**
//     * Visits the source of the class.
//     *
//     * @param source the name of the source file from which the class was compiled. May be {@literal
//     *     null}.
//     * @param debug additional debug information to compute the correspondence between source and
//     *     compiled elements of the class. May be {@literal null}.
//     */
//    public void visitSource(final String source, final String debug) {
//        if (cv != null) {
//            cv.visitSource(source, debug);
//        }
//    }

//    /**
//     * Visit the module corresponding to the class.
//     *
//     * @param name the fully qualified name (using dots) of the module.
//     * @param access the module access flags, among {@code ACC_OPEN}, {@code ACC_SYNTHETIC} and {@code
//     *     ACC_MANDATED}.
//     * @param version the module version, or {@literal null}.
//     * @return a visitor to visit the module values, or {@literal null} if this visitor is not
//     *     interested in visiting this module.
//     */
//    public ModuleVisitor visitModule(final String name, final int access, final String version) {
//        if (api < Opcodes.ASM6) {
//            throw new UnsupportedOperationException("This feature requires ASM6");
//        }
//        if (cv != null) {
//            return cv.visitModule(name, access, version);
//        }
//        return null;
//    }

//    /**
//     * Visits the nest host class of the class. A nest is a set of classes of the same package that
//     * share access to their private members. One of these classes, called the host, lists the other
//     * members of the nest, which in turn should link to the host of their nest. This method must be
//     * called only once and only if the visited class is a non-host member of a nest. A class is
//     * implicitly its own nest, so it's invalid to call this method with the visited class name as
//     * argument.
//     *
//     * @param nestHost the internal name of the host class of the nest.
//     */
//    public void visitNestHost(final String nestHost) {
//        if (api < Opcodes.ASM7) {
//            throw new UnsupportedOperationException("This feature requires ASM7");
//        }
//        if (cv != null) {
//            cv.visitNestHost(nestHost);
//        }
//    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
//        System.out.printf("visit outer class: owner: %s, name: %s, descriptor: %s\n", owner, name, descriptor);
        if (owner.equals(changeFrom)) {
            changed = true;
            super.visitOuterClass(changeTo, name, descriptor);
        } else {
            super.visitOuterClass(owner, name, descriptor);
        }
//        if (cv != null) {
//            cv.visitOuterClass(owner, name, descriptor);
//        }
    }

//    /**
//     * Visits an annotation of the class.
//     *
//     * @param descriptor the class descriptor of the annotation class.
//     * @param visible {@literal true} if the annotation is visible at runtime.
//     * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
//     *     interested in visiting this annotation.
//     */
//    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
//        if (cv != null) {
//            return cv.visitAnnotation(descriptor, visible);
//        }
//        return null;
//    }

//    /**
//     * Visits an annotation on a type in the class signature.
//     *
//     * @param typeRef a reference to the annotated type. The sort of this type reference must be
//     *     {@link TypeReference#CLASS_TYPE_PARAMETER}, {@link
//     *     TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link TypeReference#CLASS_EXTENDS}. See
//     *     {@link TypeReference}.
//     * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
//     *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
//     *     'typeRef' as a whole.
//     * @param descriptor the class descriptor of the annotation class.
//     * @param visible {@literal true} if the annotation is visible at runtime.
//     * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
//     *     interested in visiting this annotation.
//     */
//    public AnnotationVisitor visitTypeAnnotation(
//            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
//        if (api < Opcodes.ASM5) {
//            throw new UnsupportedOperationException("This feature requires ASM5");
//        }
//        if (cv != null) {
//            return cv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
//        }
//        return null;
//    }

//    /**
//     * Visits a non standard attribute of the class.
//     *
//     * @param attribute an attribute.
//     */
//    public void visitAttribute(final Attribute attribute) {
//        System.out.println("visit attribute: ");
//        System.out.println(attribute);
//        if (cv != null) {
//            cv.visitAttribute(attribute);
//        }
//    }

//    /**
//     * Visits a member of the nest. A nest is a set of classes of the same package that share access
//     * to their private members. One of these classes, called the host, lists the other members of the
//     * nest, which in turn should link to the host of their nest. This method must be called only if
//     * the visited class is the host of a nest. A nest host is implicitly a member of its own nest, so
//     * it's invalid to call this method with the visited class name as argument.
//     *
//     * @param nestMember the internal name of a nest member.
//     */
//    public void visitNestMember(final String nestMember) {
//        if (api < Opcodes.ASM7) {
//            throw new UnsupportedOperationException("This feature requires ASM7");
//        }
//        if (cv != null) {
//            cv.visitNestMember(nestMember);
//        }
//    }

    @Override
    public void visitInnerClass(
            final String name, final String outerName, final String innerName, final int access) {
//        System.out.printf("innerclass: name: %s, outerName: %s, innerName: %s\n", name, outerName, innerName);
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
//        if (cv != null) {
//            cv.visitInnerClass(name, outerName, innerName, access);
//        }
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
//        System.out.printf("visit filed: name: %s, desc: %s, sig: %s\n", name, descriptor, signature);
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
//        System.out.printf("visit method: name:%s, desc:%s, sig: %s\n", name, descriptor, signature);
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

//    /**
//     * Visits the end of the class. This method, which is the last one to be called, is used to inform
//     * the visitor that all the fields and methods of the class have been visited.
//     */
//    public void visitEnd() {
//        if (cv != null) {
//            cv.visitEnd();
//        }
//    }

}
