public class StandardStuff {
	private static final String HELLO_WORLD_STRING = "Hello world";
	private static final String GOODBYE_STRING="Goodbye";
	private final int integer;

	public StandardStuff(){
		this.integer = 6;
	}

	public String getString(){
		return getStringStatic(this.integer);
	}

	private static String getStringStatic(int theInteger){
		System.out.println("getStringStatic touched");
		if(theInteger == 6){
			return HELLO_WORLD_STRING;
		} else if(theInteger == 7){
			return GOODBYE_STRING;
		}

		return "";
	}

	public void publicAndTestedButUntouched(){
		System.out.println("publicAndTestedButUntouched touched");
		publicAndTestedButUntouchedCallee();
	}

	public void publicAndTestedButUntouchedCallee(){
		System.out.println("publicAndTestedButUntouchedCallee touched");
		int i=0;
		i++;
		i=i+10;
	}

	public void publicNotTestedButUntouched(){
		System.out.println("publicNotTestedButUntouched touched");
		publicNotTestedButUntouchedCallee();
	}

	public void publicNotTestedButUntouchedCallee(){
		System.out.println("publicNotTestedButUntouchedCallee touched");
		int i=0;
		i++;
		i=i+10;
	}

	private int privateAndUntouched(){
		System.out.println("privateAndUntouched touched");
		int i=0;
		i++;
		i++;
		return i;
	}
}