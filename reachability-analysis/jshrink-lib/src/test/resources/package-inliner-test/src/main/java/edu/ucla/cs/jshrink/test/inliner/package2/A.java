package edu.ucla.cs.jshrink.test.inliner.package2;

public class A {
	public A(){}

	public int toInline(){
		int toReturn = 0;
		toReturn += packagePrivateMethod();
		toReturn += packagePrivateMethod();
		return toReturn;
	}

	/*package*/ int packagePrivateMethod(){
		return 1;
	}
}
