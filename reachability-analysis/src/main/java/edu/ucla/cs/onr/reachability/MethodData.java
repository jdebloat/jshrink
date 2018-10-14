package edu.ucla.cs.onr.reachability;

import jdk.internal.org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class MethodData {
	private final String name;
	private final String className;
	private final String[] args;
	private final boolean isPublicMethod;
	private final String returnType;
	private final boolean isStaticMethod;
	private Optional<String> annotation;

	/*
	Note: We only record whether a method is public or not. We do not record other access information.
	Therefore, the data stored here cannot constitute a full signature of the method. It is, however
	sufficient for our requirements.
	 */

	public MethodData(String methodName, String methodClassName, String methodReturnType,
	                  String[] methodArgs, boolean isPublic, boolean isStatic){
		this.name=methodName;
		this.className = methodClassName;
		this.args = methodArgs;
		this.isPublicMethod = isPublic;
		this.returnType = methodReturnType;
		this.isStaticMethod = isStatic;
		this.annotation = Optional.empty();
	}

	//I don't like this, but I can't construct MethodData with knowledge of whether it's annotated or not
	/*package*/ void setAnnotation(String annotation){
		this.annotation = Optional.of(annotation);
	}


	/*public static int determineAccessLevel(boolean isPrivate, boolean isPublic, boolean isProtected,
	                                       boolean isAbstract, boolean isFinal, boolean isStatic){
		//TODO: This is not fully tested, not sure if it accounts for all cases.

		int accessLevel = 0;

		if(isPrivate){
			accessLevel += Opcodes.ACC_PRIVATE;
		} else if(isPublic){
			accessLevel += Opcodes.ACC_PUBLIC;
		} else if(isProtected){
			accessLevel += Opcodes.ACC_PROTECTED;
		}

		if(isAbstract){
			accessLevel += Opcodes.ACC_ABSTRACT;
		} else if(isFinal){
			accessLevel += Opcodes.ACC_FINAL;
		}

		if(isStatic){
			accessLevel += Opcodes.ACC_STATIC;
		}

		return accessLevel;
	}*/

	public String getName(){
		return this.name;
	}

	public String getClassName(){
		return this.className;
	}

	public String[] getArgs(){
		return this.args;
	}

	public String getReturnType(){
		return this.returnType;
	}

	public boolean isPublic(){
		return this.isPublicMethod;
	}

	public boolean isStatic(){
		return this.isStaticMethod;
	}

	public Optional<String> getAnnotation(){
		return this.annotation;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<" + this.className + ": ");

		if(this.isPublicMethod){
			stringBuilder.append("public ");
		}

		if(this.isStaticMethod){
			stringBuilder.append("static ");
		}

		stringBuilder.append(this.returnType +" " + this.name + "(");
		for(int i=0; i< this.args.length; i++){
			stringBuilder.append(this.args[i]);
			if(i < this.args.length -1){
				stringBuilder.append(", ");
			}
		}
		stringBuilder.append(")>");

		return stringBuilder.toString();
	}

	@Override
	public boolean equals(Object o){
		if(o instanceof MethodData){
			MethodData toCompare = (MethodData)o;
			if(toCompare.getName().equals("getString") && this.getName().equals(toCompare.getName())
				&& toCompare.getClassName().equals("StandardStuff")){
				System.out.println();
			}
			if(this.name.equals(toCompare.name) && this.className.equals(toCompare.className)
				&& this.isPublicMethod == toCompare.isPublicMethod && this.args.length == toCompare.args.length
				&& this.returnType.equals(toCompare.returnType) && this.isStaticMethod == toCompare.isStaticMethod){
				for(int i=0; i<this.args.length; i++){
					if(!this.args[i].equals(toCompare.args[i])){
						return false;
					}
				}
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode(){
		int toReturn = this.name.length() * 1 + this.className.length() * 2 + (this.isPublicMethod? 1 : 0) * 4
			+ (this.isStaticMethod ? 1 :0) *8 + this.returnType.length() * 16;

		for(int i=0; i<this.args.length; i++){
			toReturn += this.args[i].length() * Math.pow(2.0, (i+5));
		}

		return toReturn;
	}
}
