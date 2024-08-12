package ortus.boxlang.runtime.bifs.global.async;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ThreadNewTest {

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

	@DisplayName( "It can start a thread using the threadNew() bif" )
	@Test
	public void testCanStartThread() {
		// @formatter:off
		instance.executeSource(
		    """
				result = null;
				threadNew( () => {
					printLn( "thread is done!" );
					sleep( 1000 );
					result = "done";
				} );
				printLn( "thread tag done" );
				sleep( 2000 );
				printLn( "test is done done" );
		    """,
		    context,
		    BoxSourceType.CFSCRIPT
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "done" );
	}

}
