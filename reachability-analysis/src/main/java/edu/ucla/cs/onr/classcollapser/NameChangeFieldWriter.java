package edu.ucla.cs.onr.classcollapser;

import org.objectweb.asm.*;

public class NameChangeFieldWriter extends FieldVisitor implements NameChangeWriter {
    private boolean changed;

    public NameChangeFieldWriter(final int api, final FieldVisitor fv) {
        super(api, fv);
//        System.out.println(fv == null);
        changed = false;
    }

    public boolean isChanged() {
        return changed;
    }

    /**
     * Visits an annotation of the field.
     *
     * @param descriptor the class descriptor of the annotation class.
     * @param visible {@literal true} if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
     *     interested in visiting this annotation.
     */
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
//        System.out.println("visit annotation");
        if (fv != null) {
            return fv.visitAnnotation(descriptor, visible);
        }
        return null;
    }

    /**
     * Visits an annotation on the type of the field.
     *
     * @param typeRef a reference to the annotated type. The sort of this type reference must be
     *     {@link TypeReference#FIELD}. See {@link TypeReference}.
     * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
     *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
     *     'typeRef' as a whole.
     * @param descriptor the class descriptor of the annotation class.
     * @param visible {@literal true} if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
     *     interested in visiting this annotation.
     */
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
//        System.out.printf("visiteTypeAnnotation, typeRef: %s, typePath: %s, descriptor: %s, visible: %d\n", typeRef, typePath, descriptor, visible? 1: 0);
        if (api < Opcodes.ASM5) {
            throw new UnsupportedOperationException("This feature requires ASM5");
        }
        if (fv != null) {
            return fv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }
        return null;
    }

    /**
     * Visits a non standard attribute of the field.
     *
     * @param attribute an attribute.
     */
    public void visitAttribute(final Attribute attribute) {
//        System.out.println("visit attribute: " + attribute);
        if (fv != null) {
            fv.visitAttribute(attribute);
        }
    }

    /**
     * Visits the end of the field. This method, which is the last one to be called, is used to inform
     * the visitor that all the annotations and attributes of the field have been visited.
     */
    public void visitEnd() {
//        System.out.println("field visit end");
        if (fv != null) {
            fv.visitEnd();
        }
    }

}
