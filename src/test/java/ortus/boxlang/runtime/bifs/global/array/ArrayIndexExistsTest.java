package ortus.boxlang.runtime.bifs.global.array;

import static com.google.common.truth.Truth.assertThat;

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

public class ArrayIndexExistsTest {

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

	@DisplayName( "It should return true if the index has a value" )
	@Test
	public void testHasIndex() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = ArrayIndexExists( arr, 3 );
		    """,
		    context );
		Boolean res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( true );

		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = ArrayIndexExists( arr, 1 );
		    """,
		    context );
		res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( true );
	}

	@DisplayName( "It should return false if the index is 0" )
	@Test
	public void testIndexIsZero() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = result = ArrayIndexExists( arr, 0 );
		    """,
		    context );
		Boolean res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( false );
	}

	@DisplayName( "It should return false if the index is negative" )
	@Test
	public void testIndexIsNegative() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = ArrayIndexExists( arr, -3 );
		    """,
		    context );
		Boolean res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( false );
	}

	@DisplayName( "It should return false if the index is greater than the length of the array" )
	@Test
	public void testIndexIsGreaterThanLength() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = ArrayIndexExists( arr, 4 );
		    """,
		    context );
		Boolean res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( false );
	}

	@DisplayName( "It should return false if the index is a null value" )
	@Test
	public void testIndexIsNull() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, null ];
		    result = ArrayIndexExists( arr, 3 );
		    """,
		    context );
		Boolean res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( false );
	}

	@DisplayName( "It should be invocable as a member function" )
	@Test
	public void testMemberInvocation() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = arr.indexExists( 3 );
		    """,
		    context );
		Boolean res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( true );
	}

	@DisplayName( "It should be aliased as ArrayIsDefined" )
	@Test
	public void testArrayIsDefinedAlias() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = ArrayIsDefined( arr, 3 )
		    """,
		    context );
		Boolean res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( true );
	}

	@DisplayName( "It should be invocable as a member function using the isDefined alias" )
	@Test
	public void testIsDefindAliasMemberInvocation() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = arr.isDefined( 3 );
		    """,
		    context );
		Boolean res = ( Boolean ) variables.get( result );
		assertThat( res ).isEqualTo( true );
	}
}
