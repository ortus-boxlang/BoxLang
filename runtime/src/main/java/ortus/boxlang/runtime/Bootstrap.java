package ortus.boxlang.runtime;

/**
 * BoxLang bootstrapper. Loads up the engine
 */
public class Bootstrap {

	public String getGreeting() {
		return "Hello World!";
	}

	public String getGreeting( String name ) {
		return "Hello " + name + "!";
	}

	public static void main( String[] args ) {
		BoxRuntime boxRuntime = BoxRuntime.startup();

		ExecutionContext context = new ExecutionContext();
		// Here is where we presumably boostrap a page or class that we are executing in our new context.
		// JIT if neccessary

		System.out.println( new Bootstrap().getGreeting() );

		boxRuntime.shutdown();
	}
}
