package edu.ucla.cs.onr.methodwiper;

import soot.*;
import soot.JastAddJ.BooleanLiteral;
import soot.JastAddJ.CharacterLiteral;
import soot.JastAddJ.Literal;
import soot.JastAddJ.NullLiteral;
import soot.jimple.*;
import soot.tagkit.IntegerConstantValueTag;
import soot.util.Switch;

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
 *
 * @author Bobby R. Bruce
 */
public class MethodWiper {

	private static final String RUNTIME_EXCEPTION_REF = "java.lang.RuntimeException";
	private static final String RUNTIME_EXCEPTION_INIT = "<java.lang.RuntimeException: void <init>()>";
	private static final String RUNTIME_EXCEPTION_INIT_WITH_MESSAGE =
		"<java.lang.RuntimeException: void <init>(java.lang.String)>";

	private static void wipeMethodStart(SootMethod sootMethod) {

		//Retrieve the active body
		sootMethod.retrieveActiveBody();

		//wipe contents
		sootMethod.getActiveBody().getLocals().clear();
		sootMethod.getActiveBody().getUnits().clear();
		sootMethod.getActiveBody().getTraps().clear();
		sootMethod.getActiveBody().getUseAndDefBoxes().clear();

		//Need to add 'this', if a non-static method
		if(!sootMethod.isStatic()){

			SootClass declClass = sootMethod.getDeclaringClass();
			Type classType = declClass.getType();
			Local thisLocal = Jimple.v().newLocal("r0",classType);
			sootMethod.getActiveBody().getLocals().add(thisLocal);

			Unit thisIdentityStatement = Jimple.v().newIdentityStmt(thisLocal,
				Jimple.v().newThisRef(RefType.v(declClass)));
			sootMethod.getActiveBody().getUnits().add(thisIdentityStatement);
		}

		//Handle the parameters
		List<Type> parameterTypes = sootMethod.getParameterTypes();
		for (int i = 0; i < parameterTypes.size(); i++) {
			Type type = parameterTypes.get(i);
			Local arg = Jimple.v().newLocal("i" + Integer.toString(i), type);
			sootMethod.getActiveBody().getLocals().add(arg);

			Unit paramIdentifyStatement = Jimple.v().newIdentityStmt(arg, Jimple.v().newParameterRef(type, i));
			sootMethod.getActiveBody().getUnits().add(paramIdentifyStatement);
		}



	}

	private static void wipeMethodEnd(SootMethod sootMethod) {

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

		sootMethod.getActiveBody().getUnits().add(toReturnStatement);

	}

	/**
	 * Wipes the method contents of a method's body and adds the bare minimum to ensure it remains compilable.
	 *
	 * @param sootMethod The method to be wiped
	 */
	public static void wipeMethod(SootMethod sootMethod) {
		if(!sootMethod.isAbstract()) {
			wipeMethodStart(sootMethod);
			wipeMethodEnd(sootMethod);
		}
	}

	/**
	 * Wipes the contents of a method's body and adds a throw call.
	 * WARNING: A bit advanced. Would not recommend unless you know what you're doing
	 *
	 * @param sootMethod The method to be wiped
	 * @param toThrow    The Value to be thrown
	 */
	public static void wipeMethodAndInsertThrow(SootMethod sootMethod, Value toThrow) {
		if(!sootMethod.isAbstract()) {
			wipeMethodStart(sootMethod);
			sootMethod.getActiveBody().getUnits().add(Jimple.v().newThrowStmt(toThrow));
			wipeMethodEnd(sootMethod);
		}
	}

	private static void addThrowRuntimeException(SootMethod sootMethod, Optional<String> message) {
		//Declare the locals
		RefType exceptionRef = RefType.v(RUNTIME_EXCEPTION_REF);
		Local localRuntimeException = Jimple.v().newLocal("r0", exceptionRef);
		sootMethod.getActiveBody().getLocals().add(localRuntimeException);

		//$r0 = new java.lang.RuntimeException;
		AssignStmt assignStmt = Jimple.v().newAssignStmt(localRuntimeException, Jimple.v().newNewExpr(exceptionRef));
		sootMethod.getActiveBody().getUnits().add(assignStmt);

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
		sootMethod.getActiveBody().getUnits().add(initStmt);

		//throw $r0
		sootMethod.getActiveBody().getUnits().add(Jimple.v().newThrowStmt(localRuntimeException));
	}

	/**
	 * Wipes the contents of a methods body and inserts a throw statement; throws a RuntimeException
	 *
	 * @param sootMethod The method to be wiped
	 * @param message    The message to be thrown
	 */
	public static void wipeMethodAndInsertRuntimeException(SootMethod sootMethod, String message) {
		if(!sootMethod.isAbstract()) {
			wipeMethodStart(sootMethod);
			addThrowRuntimeException(sootMethod, Optional.of(message));
			// No 'wipeMethodEnd' --- if an exception is thrown, no return statement is required
		}
	}

	/**
	 * Wipes the contents of a methods body and inserts a throw statement; throws a RuntimeException
	 *
	 * @param sootMethod The method to be wiped
	 */
	public static void wipeMethodAndInsertRuntimeException(SootMethod sootMethod) {
		if(!sootMethod.isAbstract()) {
			wipeMethodStart(sootMethod);
			addThrowRuntimeException(sootMethod, Optional.empty());
			// No 'wipeMethodEnd' --- if an exception is thrown, no return statement is required
		}
	}

}

