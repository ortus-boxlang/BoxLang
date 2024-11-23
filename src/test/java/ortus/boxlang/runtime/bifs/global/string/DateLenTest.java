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

public class DateLenTest {

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

	@DisplayName( "It returns the length of the date using len(date)" )
	@Test
	public void testDateLenFunction() {
		instance.executeSource(
		    """
		    dt = now();
		    result = len(dt);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 26 );
	}

	@DisplayName( "It returns the length of the date using date.len()" )
	@Test
	public void testDateLenMethod() {
		instance.executeSource(
		    """
		    dt = now();
		    result = dt.len();
		    result2 = now().len();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 26 );
		assertThat( variables.get( result2 ) ).isEqualTo( 26 );
	}

	@DisplayName( "It returns the length of custom JVM date classes" )
	@Test
	public void testCustomDateLenFunction() {
		instance.executeSource(
		    """
		      	import java.util.Date;
		    dt = new Date();
		    result = len(dt);
		    actualLen = dt.toString().length();
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( variables.getAsInteger( Key.of( "actualLen" ) ) );
	}

}
