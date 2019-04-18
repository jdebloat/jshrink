public class TestStub {
    public static void run() {
        SubB subB = new SubB();
        SubA subA = new SubA(subB);
        subA.print();
    }
}