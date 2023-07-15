package ortus.boxlang.runtime;

public class Bootstrap {

	public String getGreeting() {
		return "Hello World!";
	}

	public static void main( String[] args ) {
		System.out.println( new Bootstrap().getGreeting() );
	}
}
