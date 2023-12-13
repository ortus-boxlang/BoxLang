package TestCases.phase1;

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
import ortus.boxlang.runtime.types.Struct;

public class DereferenceTest {

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

	@DisplayName( "Single identifier dot access" )
	@Test
	public void testSingleIdentifierReference() {
		variables.assign( new Key( "foo" ), "test" );
		instance.executeSource(
		    """
		    foo;
		    """,
		    context );
	}

	@DisplayName( "Multi identifier dot access" )
	@Test
	public void testmultiIdentifierReference() {
		Struct s = new Struct();
		s.assign( new Key( "bar" ), "test" );
		variables.assign( new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo.bar;
		    """,
		    context );
	}

	@DisplayName( "Multi multi identifier dot access" )
	@Test
	public void testmultimultiIdentifierReference() {
		Struct x = new Struct();
		x.assign( new Key( "baz" ), "test" );
		Struct s = new Struct();
		s.assign( new Key( "bar" ), x );
		variables.assign( new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo.bar.baz;
		    """,
		    context );
	}

	@DisplayName( "Bracket string access" )
	@Test
	public void testBracketStringAccess() {
		Struct s = new Struct();
		s.assign( new Key( "bar" ), "test" );
		variables.assign( new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo["bar"];
		    """,
		    context );
	}

	@DisplayName( "Bracket string concat access" )
	@Test
	public void testBracketStringConcatAccess() {
		Struct s = new Struct();
		s.assign( new Key( "bar" ), "test" );
		variables.assign( new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo["b" & "ar"]
		    """,
		    context );
	}

	@DisplayName( "Bracket number access" )
	@Test
	public void testBracketNumberAccess() {
		Struct s = new Struct();
		s.assign( new Key( "7" ), "test" );
		variables.assign( new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo[ 7 ]
		    """,
		    context );
	}

	@DisplayName( "Bracket number expression access" )
	@Test
	public void testBracketNumberExpressionAccess() {
		Struct s = new Struct();
		s.assign( new Key( "12" ), "test" );
		variables.assign( new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo[ 7 + 5 ]
		    """,
		    context );
	}

	@DisplayName( "Bracket object access" )
	@Test
	public void testBracketObjectExpressionAccess() {
		Struct x = new Struct();
		x.assign( new Key( "bar" ), "baz" );
		Struct s = new Struct();
		s.assign( new Key( "12" ), "test" );
		s.assign( Key.of( x ), "test" );
		variables.assign( new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo[ { bar : "baz" } ];
		    """,
		    context );
	}

	@DisplayName( "Mixed access" )
	@Test
	public void testBracketMixedAccess() {
		Struct	aaa		= new Struct();
		Struct	twelve	= new Struct();
		Struct	other	= new Struct();
		Struct	foo		= new Struct();

		foo.assign( new Key( "aaa" ), aaa );
		aaa.assign( Key.of( 12 ), twelve );
		twelve.assign( Key.of( "other" ), other );
		other.assign( Key.of( 7 ), "test" );

		variables.assign( new Key( "foo" ), foo );
		instance.executeSource(
		    """
		    foo[ "a" & "aa" ][ 12 ].other[ 2 + 5 ];
		    """,
		    context );
	}

}
