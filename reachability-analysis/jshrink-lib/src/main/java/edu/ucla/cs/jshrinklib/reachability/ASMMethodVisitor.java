package edu.ucla.cs.jshrinklib.reachability;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

class ASMMethodVisitor extends MethodVisitor {
	private final MethodData currentMethod;
	public Set<FieldData> fieldReferences;

	public ASMMethodVisitor(int api, MethodData method, Set<FieldData> fieldReferences) {
		super(api);
		this.currentMethod = method;
		this.fieldReferences = fieldReferences;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		//System.out.println("visitAnnotation: desc="+desc+" visible="+visible);
		this.currentMethod.setAnnotation(Type.getType(desc).getClassName());
		return null;//super.visitAnnotation(desc, visible);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(fieldReferences == null) {
			// no need to collect field references
			return;
		}

		boolean isStatic;
		if(opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
			isStatic = true;
		} else {
			isStatic = false;
		}

		String className = owner.replaceAll(Pattern.quote("/"), ".");

		// note that for parameterized types we can only get the generic type from the type description
		// due to type erasure in Java
		String type = Type.getType(desc).getClassName();
		FieldData field = new FieldData(name, className, isStatic, type);
		fieldReferences.add(field);
	}
}