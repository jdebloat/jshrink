import org.objectweb.asm.*;

public class NClassVisitor extends ClassVisitor implements Opcodes {
    private ClassInfo classInfo;

    public NClassVisitor(ClassInfo cInfo) {
        super(ASM5);
        classInfo = cInfo;
    }

    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces) {
        classInfo.name = name;
        if (!classInfo.classUsed.contains(superName)) {
            classInfo.classUsed.add(superName);
        }
//        System.out.println(name + " extends " + superName + " {");
    }

    public void visitSource(String source, String debug) {

    }

    public void visitOuterClass(String owner, String name, String desc) {

    }

    public AnnotationVisitor visitAnnotation(String desc,
                                             boolean visible) {
        return null;
    }

    public void visitAttribute(Attribute attr) {

    }

    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
    }

    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
//        System.out.println("field " + desc + " " + name);
        return null;
    }

    public MethodVisitor visitMethod(int access, String name,
                                     String desc, String signature, String[] exceptions) {
//        System.out.println("method " + name + desc);
        return new NMethodVisitor(classInfo.classUsed);
    }

    public void visitEnd() {
//        System.out.println("}");
    }
}