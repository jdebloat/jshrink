package edu.ucla.cs.onr;

import java.util.HashSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

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
		className = name;
		classes.add(name);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, 
			String desc, String signature, String[] exceptions) {
		String qualifiedName = className + ":" + name;
		qualifiedName += ":" + desc;
		methods.add(qualifiedName);
		return null;
	}
}
