package edu.ucla.cs.onr.reachability;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ASMClassVisitor extends ClassVisitor{
	private String currentClass;
	private Set<String> classes;
	private Set<MethodData> methods;
	private boolean isJUnit3Test;
	
	public ASMClassVisitor(int api, Set<String> classes, Set<MethodData> methods) {
		super(api);
		this.classes = classes;
		this.methods = methods; 
		isJUnit3Test = false;
	}
	
	@Override
	public void visit(int version, int access, String name,
            String signature, String superName, String[] interfaces) {
		String name2 = name.replaceAll(Pattern.quote("/"), ".");
		currentClass = name2;
		classes.add(name2);
		if(superName.equals("junit/framework/TestCase")) {
			// this is a test case written in JUnit 3
			isJUnit3Test = true;
		}
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, 
			String desc, String signature, String[] exceptions) {
		String returnType = Type.getReturnType(desc).getClassName();
		Type[] ts = Type.getArgumentTypes(desc);
		
		// No idea why this works, but it do.
		String args = "";
		for(int i = 0; i < ts.length - 1; i++) {
				args += ts[i].getClassName() + ",";
		}
		if(ts.length > 0 && !ts[ts.length - 1].getClassName().equals("")) {
			args += ts[ts.length - 1].getClassName();
		}

		List<String> argsList = new ArrayList<String>();
		for(String arg: args.split(",")){
			if(!arg.trim().isEmpty()){
				argsList.add(arg);
			}
		}
		
		MethodData methodData = new MethodData(name, currentClass,returnType,
			argsList.toArray(new String[argsList.size()]),
			(access & 1) !=0, (access & 8) != 0); //1 == public; 8 == Static
		if(isJUnit3Test && 
				(name.startsWith("test") || name.equals("setup") || name.equals("tearDown"))) {
			methodData.setAsJUnit3Test();
		}
		methods.add(methodData);

		return new ASMMethodAnnotationScanner(this.api, methodData);
	}
}
