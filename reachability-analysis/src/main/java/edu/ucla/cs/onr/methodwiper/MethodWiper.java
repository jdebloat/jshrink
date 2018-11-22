package edu.ucla.cs.onr.methodwiper;

import soot.*;
import soot.jimple.*;
import soot.tagkit.JasminAttribute;
import soot.util.JasminOutputStream;

import java.io.*;
import java.util.List;
import java.util.Optional;

/**
 * This class was made to support the complete wiping of methods (while remaining compilable). I have tried to keep this
 * as simple as possible. The API either allows the wiping of methods an exception throw inserted,
 * `wipeMethod(SootMethod sootMethod)`, the wiping of a method, and including a RuntimeException
 * throw, `wipeMethodAndInsertRuntimeException(SootMethod sootMethod)`, with a RuntimeException throw inc. message,
 * `wipeMethodAndInsertRuntimeException(SootMethod sootMethod, String message)`, or with a custom thrown exception
 * (warning --- this is a bit of an advanced feature),
 * `wipeMethodAndInsertThrow(SootMethod scootMethod, Value toThrow)`.
 */
public class MethodWiper {

	private static final String RUNTIME_EXCEPTION_REF = "java.lang.RuntimeException";
	private static final String RUNTIME_EXCEPTION_INIT = "<java.lang.RuntimeException: void <init>()>";
	private static final String RUNTIME_EXCEPTION_INIT_WITH_MESSAGE =
		"<java.lang.RuntimeException: void <init>(java.lang.String)>";

	private static Body getBody(SootMethod sootMethod) {

		//Retrieve the active body
		sootMethod.retrieveActiveBody();

		Body toReturn = new JimpleBody();

		//Need to add 'this', if a non-static method
		if(!sootMethod.isStatic()){

			SootClass declClass = sootMethod.getDeclaringClass();
			Type classType = declClass.getType();
			Local thisLocal = Jimple.v().newLocal("r0",classType);
			toReturn.getLocals().add(thisLocal);

			Unit thisIdentityStatement = Jimple.v().newIdentityStmt(thisLocal,
				Jimple.v().newThisRef(RefType.v(declClass)));
			toReturn.getUnits().add(thisIdentityStatement);
		}

		//Handle the parameters
		List<Type> parameterTypes = sootMethod.getParameterTypes();
		for (int i = 0; i < parameterTypes.size(); i++) {
			Type type = parameterTypes.get(i);
			Local arg = Jimple.v().newLocal("i" + Integer.toString(i), type);
			toReturn.getLocals().add(arg);

			Unit paramIdentifyStatement = Jimple.v().newIdentityStmt(arg, Jimple.v().newParameterRef(type, i));
			toReturn.getUnits().add(paramIdentifyStatement);
		}

		return toReturn;

	}

	private static void addReturnUnit(Body body, SootMethod sootMethod) {

		//Add a return statement
		Type returnType = sootMethod.getReturnType();
		Unit toReturnStatement = null;
		if (returnType == VoidType.v()) {
			toReturnStatement = Jimple.v().newReturnVoidStmt();
		} else if (returnType == LongType.v()) {
			toReturnStatement = Jimple.v().newReturnStmt(LongConstant.v(0));
		} else if (returnType == FloatType.v()) {
			toReturnStatement = Jimple.v().newReturnStmt(FloatConstant.v(0.0f));
		} else if (returnType == DoubleType.v()){
			toReturnStatement = Jimple.v().newReturnStmt(DoubleConstant.v(0.0));
		} else if (returnType == BooleanType.v() || returnType == IntType.v()
			|| returnType == ByteType.v() || returnType == ShortType.v() || returnType == CharType.v()) {
			toReturnStatement = Jimple.v().newReturnStmt(IntConstant.v(0));
		} else { //If not a primitive, must be an object (can therefore be null)
			toReturnStatement = Jimple.v().newReturnStmt(NullConstant.v());
		}

		body.getUnits().add(toReturnStatement);

	}

	private static void addThrowRuntimeException(Body body, Optional<String> message) {

		//Declare the locals
		RefType exceptionRef = RefType.v(RUNTIME_EXCEPTION_REF);
		Local localRuntimeException = Jimple.v().newLocal("r0", exceptionRef);
		body.getLocals().add(localRuntimeException);

		//$r0 = new java.lang.RuntimeException;
		AssignStmt assignStmt = Jimple.v().newAssignStmt(localRuntimeException, Jimple.v().newNewExpr(exceptionRef));
		body.getUnits().add(assignStmt);

		SpecialInvokeExpr sie;

		if (message.isPresent()) {
			//specialinvoke $r0.<java.lang.RuntimeException: void <init>(java.lang.String)>("ERROR");
			SootMethod runTimeExceptionMethod = Scene.v().getMethod(RUNTIME_EXCEPTION_INIT_WITH_MESSAGE);
			sie = Jimple.v().newSpecialInvokeExpr(localRuntimeException,
				runTimeExceptionMethod.makeRef(), StringConstant.v(message.get()));
		} else {
			//specialinvoke $r0.<java.lang.RuntimeException: void <init>(java.lang.String)>();
			SootMethod runTimeExceptionMethod = Scene.v().getMethod(RUNTIME_EXCEPTION_INIT);
			sie = Jimple.v().newSpecialInvokeExpr(localRuntimeException,
				runTimeExceptionMethod.makeRef());
		}

		InvokeStmt initStmt = Jimple.v().newInvokeStmt(sie);
		body.getUnits().add(initStmt);

		//throw $r0
		body.getUnits().add(Jimple.v().newThrowStmt(localRuntimeException));
	}

	private static long getSize(SootClass sootClass){

		for(SootMethod m: sootClass.getMethods()){
			if(m.isConcrete()) {
				m.retrieveActiveBody();
			}
		}

		StringWriter stringWriter = new StringWriter();
		PrintWriter writerOut = new PrintWriter(stringWriter);
		JasminClass jasminClass = new JasminClass(sootClass);
		jasminClass.print(writerOut);
		writerOut.flush();

		return stringWriter.getBuffer().length();
	}

	private static boolean wipeMethod(SootMethod sootMethod, Optional<Optional<String>> exception){



		//The "lambda$" is to avoid removal of Lambda "methods". We cannot handle these right now.
		if(sootMethod.isAbstract() || sootMethod.isNative() || sootMethod.getName().startsWith("lambda$")){
			return false;
		}

		SootClass sootClass = sootMethod.getDeclaringClass();

		long originalSize = getSize(sootClass);

		Body body = getBody(sootMethod);
		if(exception.isPresent()){
			addThrowRuntimeException(body, exception.get());
		} else {
			addReturnUnit(body, sootMethod);
		}

		Body oldBody = sootMethod.getActiveBody();
		body.setMethod(sootMethod);
		sootMethod.setActiveBody(body);

		long newSize = getSize(sootClass);

		if(newSize >= originalSize){
			sootMethod.setActiveBody(oldBody);
			return false;
		}

		return true;
	}

	/**
	 * Wipes the method contents of a method's body and adds the bare minimum to ensure it remains compilable.
	 *
	 * @param sootMethod The method to be wiped
	 * @return a boolean specifying whether the method was wiped or not
	 */
	public static boolean wipeMethod(SootMethod sootMethod) {
		return wipeMethod(sootMethod, Optional.empty());
	}

	/**
	 * Wipes the contents of a methods body and inserts a throw statement; throws a RuntimeException
	 *
	 * @param sootMethod The method to be wiped
	 * @param message    The message to be thrown
	 * @return a boolean specifying whether the method was wiped or not
	 */
	public static boolean wipeMethodAndInsertRuntimeException(SootMethod sootMethod, String message) {
		return wipeMethod(sootMethod,Optional.of(Optional.of(message)));
	}

	/**
	 * Wipes the contents of a methods body and inserts a throw statement; throws a RuntimeException
	 *
	 * @param sootMethod The method to be wiped
	 * @return a boolean specifying whether the method was wiped or not
	 */
	public static boolean wipeMethodAndInsertRuntimeException(SootMethod sootMethod) {
		return wipeMethod(sootMethod, Optional.of(Optional.empty()));
	}

}

