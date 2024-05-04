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

public class isInThreadTest {

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

	@DisplayName( "It can verify you are not in a thread" )
	@Test
	public void testNotInThread() {
		// @formatter:off
		instance.executeSource(
		    """
		    result = isInThread();
		    """,
		    context );
		// @formatter:on

		Boolean resultValue = variables.getAsBoolean( result );
		assert ( resultValue == false );
	}

	@DisplayName( "It can verify you are in a thread" )
	@Test
	public void testInThread() {
		// @formatter:off
		instance.executeSource(
		    """
				result = false;
				thread name="testThread"{
					result = isInThread();
				}
				// join
				thread action="join" name="testThread";
		    """,
		    context );
		// @formatter:on

		Boolean resultValue = variables.getAsBoolean( result );
		assert ( resultValue == true );
	}

}
