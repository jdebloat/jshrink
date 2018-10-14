package edu.ucla.cs.onr.reachability;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class ASMMethodAnnotationScanner extends MethodVisitor {
	private final MethodData currentMethod;

	public ASMMethodAnnotationScanner(int api, MethodData method) {
		super(api);
		this.currentMethod = method;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		//System.out.println("visitAnnotation: desc="+desc+" visible="+visible);
		this.currentMethod.setAnnotation(Type.getType(desc).getClassName());
		return null;//super.visitAnnotation(desc, visible);
	}
}