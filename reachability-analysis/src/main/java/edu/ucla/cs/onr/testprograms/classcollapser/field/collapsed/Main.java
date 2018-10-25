package edu.ucla.cs.onr.testprograms.classcollapser.field.collapsed;

public class Main {
    public static void main(String[] args) {
        A a = new A(1, 2);
        A b = new A(3, 4);

        a.foo();
        b.foo();
    }
}
