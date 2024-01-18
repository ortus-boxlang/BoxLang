import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class InsertTest {

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

	@DisplayName( "It inserts a substring at the specified position" )
	@Test
	public void testInsertSubstring() {
		instance.executeSource(
		    """
		    result = insert(" is cool", "BoxLang", 7);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "BoxLang is cool" );
	}

	@DisplayName( "It inserts a substring at the beginning of the string when position is 0" )
	@Test
	public void testInsert() {
		instance.executeSource(
		    """
		    result = insert("BoxLang ", "is cool", 0);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "BoxLang is cool" );
	}

	@DisplayName( "It inserts a substring using the member function" )
	// @Test
	public void testInsertMember() {
		instance.executeSource(
		    """
		    result = "BoxLang".insert(" is cool", 7);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "BoxLang is cool" );
	}

	@DisplayName( "It throws an exception if the position is greater than the string length" )
	@Test
	public void testThrowsExceptionForPositionGreaterThanStringLength() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = insert("BoxLang", " is cool", 10);
		        """,
		        context )
		);
	}

	@DisplayName( "It throws an exception for a negative position" )
	@Test
	public void testThrowsExceptionForNegativePosition() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = insert("is cool", "BoxLang", -1);
		        """,
		        context )
		);
	}

	@DisplayName( "It throws an exception for a position greater than string length + 1" )
	@Test
	public void testThrowsExceptionForInvalidPosition() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = insert("is cool", "BoxLang", 8);
		        """,
		        context )
		);
	}
}
