package edu.ucla.cs.onr.reachability;

import soot.Scene;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodData {
	private String name;
	private String className;
	private String[] args;
	private boolean isPublicMethod;
	private String returnType;
	private boolean isStaticMethod;
	private Optional<String> annotation;

	/*
	Note: We only record whether a method is public or not. We do not record other access information.
	Therefore, the data stored here cannot constitute a full signature of the method. It is, however
	sufficient for our requirements.
	 */

	public MethodData(String methodName, String methodClassName, String methodReturnType,
	                  String[] methodArgs, boolean isPublic, boolean isStatic){
		this.setData(methodName, methodClassName, methodReturnType, methodArgs, isPublic, isStatic);
	}

	private void setData(String methodName, String methodClassName, String methodReturnType,
	                     String[] methodArgs, boolean isPublic, boolean isStatic){

		this.name=methodName;
		this.className = methodClassName;
		this.args = methodArgs;
		this.isPublicMethod = isPublic;
		this.returnType = methodReturnType;
		this.isStaticMethod = isStatic;
		this.annotation = Optional.empty();

	}

	public MethodData(String signature) throws IOException{ //TODO: is this the correct exception to throw?
		signature = signature.trim();
		if(!signature.startsWith("<") || !signature.endsWith(">")){
			throw new IOException("Signature must start with with '<' and end with '>'");
		}

		signature = signature.substring(1,signature.length()-1);
		String[] signatureSplit = signature.split(":");

		if(signatureSplit.length != 2){
			throw new IOException("Method signature must be in format of " +
				"'<[classname]:[public?] [static?] [returnType] [methodName]([args...?])>'");
		}

		String clName = signatureSplit[0];
		String methodString = signatureSplit[1];

		boolean publicMethod = methodString.toLowerCase().contains("public");
		boolean staticMethod = methodString.toLowerCase().contains("static");

		Pattern pattern = Pattern.compile("([a-zA-Z][a-zA-Z0-9_]*)(\\(.*\\))");
		Matcher matcher = pattern.matcher(methodString);

		if(!matcher.find()){
			throw new IOException("Could not find a method matching our regex pattern ('" + pattern.toString() + "')");
		}

		String method = matcher.group();
		String methodName = method.substring(0,method.indexOf('('));
		String[] methodArgs = method.substring(method.indexOf('(')+1, method.lastIndexOf(')'))
			.split(",");

		for(int i=0; i<methodArgs.length; i++){
			methodArgs[i] = methodArgs[i].trim();
		}

		String[] temp = methodString.substring(0, methodString.indexOf(methodName)).trim().split("\\s+");
		String methodReturnType = temp[temp.length-1];

		this.setData(methodName,clName,methodReturnType, methodArgs, publicMethod, staticMethod);
	}

	//I don't like this, but I can't construct MethodData with knowledge of whether it's annotated or not
	/*package*/ void setAnnotation(String annotation){
		this.annotation = Optional.of(annotation);
	}

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

	public String getSubSignature(){
		StringBuilder stringBuilder = new StringBuilder();
		//stringBuilder.append((this.returnType.equals("void") ? "void" : Scene.v().quotedNameOf(this.returnType))
			//+ " " + Scene.v().quotedNameOf(this.name) + "(");
		String returnTypeProcess = this.returnType.contains(".") ? Scene.v().quotedNameOf(this.returnType) : this.returnType;
		stringBuilder.append(returnTypeProcess + " " + Scene.v().quotedNameOf(this.name) + "(");
		for(int i=0; i< this.args.length; i++){
			String argProcess = this.args[i].contains(".") ? Scene.v().quotedNameOf(this.args[i]) : this.args[i];
			stringBuilder.append(argProcess);
			if(i < this.args.length -1){
				stringBuilder.append(",");
			}
		}
		stringBuilder.append(")");

		return stringBuilder.toString();
	}

	public String getSignature(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<" + Scene.v().quotedNameOf(this.className) + ": ");

		if(this.isPublicMethod){
			stringBuilder.append("public ");
		}

		if(this.isStaticMethod){
			stringBuilder.append("static ");
		}

		stringBuilder.append(getSubSignature() + ")>");

		return stringBuilder.toString();
	}

	@Override
	public String toString(){
		return getSignature();
	}

	@Override
	public boolean equals(Object o){
		if(o instanceof MethodData){
			MethodData toCompare = (MethodData)o;
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
