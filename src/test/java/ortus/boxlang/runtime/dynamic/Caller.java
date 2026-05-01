package ortus.boxlang.runtime.dynamic;

public class Caller {

	public String doStuff( TestFunctionalInterface i ) {
		return i.name() + i.doStuff( "A", "B" );
	}
}
