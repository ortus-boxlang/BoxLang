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

public class IsThreadAliveTest {

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

	@DisplayName( "It can verify a thread is not alive" )
	@Test
	public void testNotAlive() {
		// @formatter:off
		instance.executeSource(
		    """
				thread name="testThread"{
					sleep( 1000 );
				};
				thread name="testThread" action="join";
				result = isThreadAlive( "testThread" );
		    """,
		    context );
		// @formatter:on

		Boolean resultValue = variables.getAsBoolean( result );
		assert ( resultValue == false );
	}

	@DisplayName( "It can verify a thread is alive" )
	@Test
	public void testAlive() {
		// @formatter:off
		instance.executeSource(
		    """
				thread name="testThread"{
					sleep( 1000 );
				};
				result = isThreadAlive( "testThread" );
		    """,
		    context );
		// @formatter:on

		Boolean resultValue = variables.getAsBoolean( result );
		assert ( resultValue == true );
	}

}
