package ortus.boxlang.runtime.bifs.global.async;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class IsThreadInterruptedTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can verify a thread is not interrupted" )
	@Test
	public void testNotInterrupted() {
		// @formatter:off
		instance.executeSource(
		    """
				thread name="testThread"{
					sleep( 1000 );
				};
				result = isThreadInterrupted( "testThread" );
		    """,
		    context );
		// @formatter:on

		Boolean resultValue = variables.getAsBoolean( result );
		assert ( resultValue == false );
	}

	@DisplayName( "It can verify a thread is interrupted" )
	@Test
	public void testInterrupted() {
		// @formatter:off
		instance.executeSource(
		    """
				thread name="testThread"{
					assert isThreadInterrupted() == false
					sleep( 1000 );
				};
				threadInterrupt( "testThread" );
				result = isThreadInterrupted( "testThread" );
				println( testThread );
		    """,
		    context );
		// @formatter:on

		Boolean resultValue = variables.getAsBoolean( result );
		assert ( resultValue == true );
	}
}
