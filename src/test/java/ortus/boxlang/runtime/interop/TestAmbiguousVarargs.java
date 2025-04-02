package ortus.boxlang.runtime.interop;

public class TestAmbiguousVarargs {

	static public String foo( int x, String y ) {
		return "Non-varargs method";
	}

	static public String foo( int x, String y, Object... rest ) {
		return "Varargs method";
	}
}
