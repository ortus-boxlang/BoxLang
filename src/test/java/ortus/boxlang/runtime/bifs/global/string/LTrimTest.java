package ortus.boxlang.runtime.bifs.global.string;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class LTrimTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It trims leading whitespace from a string" )
	@Test
	public void testItTrimsLeadingWhitespace() {
		instance.executeSource(
		    """
		    result = lTrim('  Grant  ');
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Grant  " );
	}

	@DisplayName( "It trims leading whitespace from a string as member" )
	@Test
	public void testItTrimsLeadingWhitespaceMember() {
		instance.executeSource(
		    """
		    value = '  Grant  ';
		    result = value.lTrim();
		    result2 = "  Michael  ".lTrim();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Grant  " );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "Michael  " );
	}
}
