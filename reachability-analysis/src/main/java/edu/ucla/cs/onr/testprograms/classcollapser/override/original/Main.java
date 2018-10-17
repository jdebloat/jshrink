package edu.ucla.cs.onr.testprograms.classcollapser.override.original;

public class Main {
    public static void main(String[] args) {
        A a = new B();
        a.foo();
        B b = new B();
        b.foo();
    }
}
