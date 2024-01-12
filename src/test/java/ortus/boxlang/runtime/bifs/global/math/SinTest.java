package ortus.boxlang.runtime.bifs.global.math;

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

public class SinTest {

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

	@DisplayName( "It returns sine" )
	@Test
	public void testItReturnsSine() {
		instance.executeSource(
		    """
		    result = sin(0);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0.0 );
		instance.executeSource(
		    """
		    result = sin(0.5);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( Math.sin( 0.5 ) );
	}

	@DisplayName( "It returns sine member" )
	@Test
	public void testItReturnsSineMember() {
		instance.executeSource(
		    """
		    result = (0).sin();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0.0 );
		instance.executeSource(
		    """
		    result = (0.5).sin();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( Math.sin( 0.5 ) );
	}
}
