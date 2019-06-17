import java.util.function.Function;

public class Main {
	public static void main(String[] args){
		Function<String, String> unusedMethod = Main::unusedMethodReference;
		Function<String, String> usedMethod = Main::usedMethodReference;

		System.out.println(usedMethod.apply("main"));
	}

	private static String unusedMethodReference(String input){
		return "unusedMethodReference has been called by " + input;
	}

	private static String usedMethodReference(String input){
		return "usedMethodReference has been called by " + input;
	}
}