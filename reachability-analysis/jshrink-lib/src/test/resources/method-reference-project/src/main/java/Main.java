import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.IntStream.builder;

public class Main {
	public static void main(String[] args){
		Function<String, String> unusedMethod = Main::unusedMethodReference;
		Function<String, String> usedMethod = Main::usedMethodReference;

		getCollection();

		Function<String, String> innerClassMethod = InnerClass::innerClassMethodReference;

		System.out.println(usedMethod.apply("main"));
	}

	private static String unusedMethodReference(String input){
		return "unusedMethodReference has been called by " + input;
	}

	private static String usedMethodReference(String input){
		return "usedMethodReference has been called by " + input;
	}

	private static String usedInCollectionMethodReference(String input){
		return "usedInJoiningReference has been called by " + input;
	}

	public static Collection getCollection() {
		Set<Function<String, String>> toReturn = new HashSet<Function<String, String>>();
		toReturn.add(Main::usedInCollectionMethodReference);
		return toReturn;
	}

	public static class InnerClass{
		private static String innerClassMethodReference(String input){
			return "innerClassMethodReference " + input;
		}
	}

}