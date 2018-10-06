package edu.ucla.cs.onr.reachability;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ASMMethodVisitor extends MethodVisitor {
	public boolean isTestMethod; 

	public ASMMethodVisitor(int api) {
		super(api);
		isTestMethod = true;
	}
	
	@Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        System.out.println("visitAnnotation: desc="+Type.getType(desc).getClassName() +" visible="+visible);
        return null;
    }
}