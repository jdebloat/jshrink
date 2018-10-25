public class Test15 {

	public Test15(){
		int i=0;
		i++;
	}

	public static void main(String[] args) {
		Test15.staticVoidMethodNoParams();
		int i1 = Test15.staticIntMethodNoParams();
		String s1 = Test15.staticStringMethodNoParams();
		Double d1 = Test15.staticDoubleMethodNoParams();
		Test15.staticVoidMethodTwoParams(10, 20);
		int i2 = Test15.staticIntMethodTwoParams(10, 20);

		Test15 test1 = new Test15();
		test1.methodNoParams();
		int i3 = test1.intMethodNoParams();
		int i4 = test1.intMethodTwoParams(10, 20);

		Test15.staticBooleanMethodNoParams();
		Test15.staticCharMethodNoParams();
		Test15.staticByteMethodNoParams();
		Test15.staticShortMethodNoParams();
	}

	public static void staticVoidMethodNoParams() {
		System.out.println("staticVoidMethodNoParams touched");
	}

	public static int staticIntMethodNoParams() {
		System.out.println("staticIntMethodNoParams touched");
		return 10;
	}

	public static String staticStringMethodNoParams() {
		System.out.println("staticStringMethodNoParams touched");
		return "FooBar";
	}

	public static Double staticDoubleMethodNoParams() {
		System.out.println("staticDoubleMethodNoParams touched");
		return new Double(10.10);
	}

	public static void staticVoidMethodTwoParams(int one, int two) {
		System.out.println("staticVoidMethodTwoParams touched");
	}

	public static int staticIntMethodTwoParams(int one, int two) {
		System.out.println("staticIntMethodTwoParams touched");
		return one + two;
	}

	public void methodNoParams() {
		System.out.println("methodNoParams touched");
	}

	public int intMethodNoParams() {
		System.out.println("intMethodNoParams touched");
		return 20;
	}

	public int intMethodTwoParams(int one, int two) {
		String temp = "Lorem ipsum dolor sit amet, prima adipisci et est, mel et purto duis ludus. Vix mollis ancillae te. Eu pro purto soleat consetetur. Vix eripuit reprehendunt id. Audire vidisse aperiri eu sed, incorrupte scripserit signiferumque ad est, omnes platonem ex sea. His libris invenire eu. Falli tractatos qui ea, officiis recusabo convenire ea eos, pri hinc oratio delenit an.Omnesque conceptam appellantur ei vel, an quo possim audiam. Consulatu vituperatoribus nam ea, eos an paulo copiosae. Paulo dolor ei his, eam eu minim partem, saepe putent concludaturque vis ex. Nibh consulatu interpretaris pri id, ut urbanitas delicatissimi mei. Te vim aperiam principes assueverit, ea purto imperdiet dissentiunt eos, ex autem mucius iuvaret quo.In his nibh partiendo ocurreret. Probatus corrumpit molestiae ei ius. In qui dictas doctus atomorum, illum vocent cotidieque no sit, ne mei discere facilis lucilius. Mei efficiendi reformidans theophrastus ea, no vis erat novum laoreet, atqui euripidis mea cu. Ex omnes omnesque cum.An eam prima dicta eligendi, dictas option repudiandae no nam. Nisl vero ei duo. Pericula posidonium eu pri, et ius tale constituam. Rebum veritus in ius. Ut mei dicit repudiandae, vim an primis propriae efficiendi, et quas debitis laboramus eam. Cum elitr principes ei.Prima nulla eligendi ex eum, saperet debitis ullamcorper et cum, ut ius autem denique expetendis. Nobis adversarium an qui, nec no melius iuvaret. Eum mundi tantas eu, novum aperiam pri ei. Eam reprimique neglegentur delicatissimi eu, molestie iudicabit ius ne, ullum dolore animal ei cum. Dolorum nusquam eleifend et pri, in errem mentitum sed.";
		System.out.println("intMethodTwoParams touched");
		return one + two;
	}

	public static boolean staticBooleanMethodNoParams(){
		System.out.println("staticBooleanMethodNoParams touched");
		return true;
	}

	public static char staticCharMethodNoParams(){
		System.out.println("staticCharMethodNoParams touched");
		return 'c';
	}

	public static Byte staticByteMethodNoParams(){
		System.out.println("staticByteMethodNoParams touched");
		return 1;
	}

	public static Short staticShortMethodNoParams(){
		System.out.println("staticShortMethodNoParams touched");
		return 3;
	}
}
