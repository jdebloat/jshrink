public class LibraryClass {

	public LibraryClass(){}

	public int getNumber(){
		System.out.println("getNumber touched");
		int i=0;
		i++;
		i*=10;
		return i;
	}

	public int untouchedGetNumber(){
		System.out.println("untouchedGetNumber touched");
		int i=0;
		i++;
		i*=10;
		return i;
	}

	private int privateUntouchedGetNumber(){
		System.out.println("privateUntouchedGetNumber touched");
		int i=0;
		i++;
		i*=10;
		return i;
	}
}
