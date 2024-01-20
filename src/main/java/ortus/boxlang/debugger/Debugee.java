package ortus.boxlang.debugger;

public class Debugee {

	public static void main( String[] args ) {
		System.out.println( args[ 0 ] );
		String jpda = "Java Platform Debugger Architecture";
		System.out.println( "Hi Everyone, Welcome to " + jpda ); // add a break point here
		String	jdi		= "Java Debug Interface"; // add a break point here and also stepping in here
		String	text	= "Today, we'll dive into " + jdi;
		System.out.println( text );
	}
}
