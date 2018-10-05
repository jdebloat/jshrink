package edu.ucla.cs.onr;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ASMClassVisitor extends ClassVisitor{
	private String className;
	HashSet<String> classes; 
	HashSet<String> methods;
	
	public ASMClassVisitor(int api, HashSet<String> classes, HashSet<String> methods) {
		super(api);
		this.classes = classes;
		this.methods = methods;  
	}
	
	@Override
	public void visit(int version, int access, String name,
            String signature, String superName, String[] interfaces) {
		String name2 = name.replaceAll(Pattern.quote("/"), ".");
		className = name2;
		classes.add(name2);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, 
			String desc, String signature, String[] exceptions) {
		String returnType = Type.getReturnType(desc).getClassName();
		Type[] ts = Type.getArgumentTypes(desc);
		String args = "";
		for(int i = 0; i < ts.length - 1; i++) {
			args += ts[i].getClassName() + ",";
		}
		if(ts.length > 0) {
			args += ts[ts.length - 1].getClassName();
		}
		String qualifiedName = className + ": " + returnType + " " + name + "(" + args + ")";
		methods.add(qualifiedName);
		// return new ASMMethodVisitor(Opcodes.ASM5);
		return null;
	}
}
