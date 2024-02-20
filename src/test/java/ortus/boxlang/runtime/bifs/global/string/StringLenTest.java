package ortus.boxlang.runtime.bifs.global.string;

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

public class StringLenTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			result2	= new Key( "result2" );

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

	@DisplayName( "It returns the length of the string using len(string)" )
	@Test
	public void testStringLenFunction() {
		instance.executeSource(
		    """
		    str = "BoxLang";
		    result = len(str);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
	}

	@DisplayName( "It returns the length of the string using string.len()" )
	@Test
	public void testStringLenMethod() {
		instance.executeSource(
		    """
		    str = "BoxLang";
		    result = str.len();
		    result2 = "BoxLang".len();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
		assertThat( variables.get( result2 ) ).isEqualTo( 7 );
	}

	@DisplayName( "It returns the length of the string using stringlen(string)" )
	@Test
	public void testStringLenFunctionAlternative() {
		instance.executeSource(
		    """
		    str = "BoxLang";
		    result = stringlen(str);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
	}
}
