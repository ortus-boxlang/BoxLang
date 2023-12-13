package TestCases.phase1;

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
import ortus.boxlang.runtime.types.Struct;

public class AssignmentTest {

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

	@DisplayName( "Unscoped assignment" )
	@Test
	public void testUnscopedAssignment() {
		instance.executeSource(
		    """
		    foo = "test";
		    """,
		    context );
		assertThat( variables.dereference( Key.of( "foo" ), false ) ).isEqualTo( "test" );
	}

	@DisplayName( "Nested dot assignment" )
	@Test
	public void testNestedDotAssignment() {
		instance.executeSource(
		    """
		    foo.bar = "test";
		    """,
		    context );
		assertThat( ( ( Struct ) variables.dereference( Key.of( "foo" ), false ) ).dereference( Key.of( "bar" ), false ) ).isEqualTo( "test" );
	}

	@DisplayName( "Multi multi identifier dot assignment" )
	@Test
	public void testmultimultiIdentifierAssignment() {
		instance.executeSource(
		    """
		    foo.bar.baz = "test";
		    """,
		    context );

		assertThat( ( ( Struct ) ( ( Struct ) variables.dereference( Key.of( "foo" ), false ) ).dereference( Key.of( "bar" ), false ) )
		    .dereference( Key.of( "baz" ), false ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket string assignment" )
	@Test
	public void testBracketStringAssignment() {
		instance.executeSource(
		    """
		    foo["bar"] = "test";
		    """,
		    context );
		assertThat( ( ( Struct ) variables.dereference( Key.of( "foo" ), false ) ).dereference( Key.of( "bar" ), false ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket string concat assignment" )
	@Test
	public void testBracketStringConcatAssignement() {
		instance.executeSource(
		    """
		    foo["b" & "ar"] = "test";
		    """,
		    context );
		assertThat( ( ( Struct ) variables.dereference( Key.of( "foo" ), false ) ).dereference( Key.of( "bar" ), false ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket number assignment" )
	@Test
	public void testBracketNumberAssignment() {
		instance.executeSource(
		    """
		    foo[ 7 ] = "test";
		    """,
		    context );
		assertThat( ( ( Struct ) variables.dereference( Key.of( "foo" ), false ) ).dereference( Key.of( "7" ), false ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket number expression assignment" )
	@Test
	public void testBracketNumberExpressionAssignment() {
		instance.executeSource(
		    """
		    foo[ 7 + 5 ] = "test";
		    """,
		    context );
		assertThat( ( ( Struct ) variables.dereference( Key.of( "foo" ), false ) ).dereference( Key.of( "12" ), false ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket object assignment" )
	@Test
	public void testBracketObjectExpressionAssignment() {
		Struct x = new Struct();
		x.assign( new Key( "bar" ), "baz" );
		instance.executeSource(
		    """
		    foo[ { bar : "baz" } ] = "test";
		    """,
		    context );
		assertThat( ( ( Struct ) variables.dereference( Key.of( "foo" ), false ) ).dereference( Key.of( x ), false ) ).isEqualTo( "test" );
	}

	@DisplayName( "Mixed assignment" )
	@Test
	public void testBracketMixedAssignment() {
		instance.executeSource(
		    """
		    foo[ "a" & "aa" ][ 12 ].other[ 2 + 5 ] = "test";
		    """,
		    context );

		Struct	foo		= ( Struct ) variables.dereference( Key.of( "foo" ), false );
		Struct	aaa		= ( Struct ) foo.dereference( Key.of( "aaa" ), false );
		Struct	twelve	= ( Struct ) aaa.dereference( Key.of( "12" ), false );
		Struct	other	= ( Struct ) twelve.dereference( Key.of( "other" ), false );

		assertThat( other.dereference( Key.of( "7" ), false ) ).isEqualTo( "test" );
	}

}
