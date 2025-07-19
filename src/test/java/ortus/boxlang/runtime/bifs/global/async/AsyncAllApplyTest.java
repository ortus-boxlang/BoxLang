package ortus.boxlang.runtime.bifs.global.async;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.ExecutionException;

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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class AsyncAllApplyTest {

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

	@DisplayName( "Test asyncAllApply() with the basics using an array" )
	@Test
	public void testAsyncApplyWithBasicsArray() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			a = [1, 2, 3];
			result = asyncAllApply( a, ( item ) => item * 2 )
			println( result )
			assert result[ 1 ] == 2
			assert result[ 2 ] == 4
			assert result[ 3 ] == 6
		""", context);
		// @formatter:on

		Array aResult = variables.getAsArray( result );
		assertThat( aResult ).isNotNull();
	}

	@DisplayName( "Test asyncAllApply() with array and a timeout" )
	@Test
	public void testAsyncApplyWithTimeout() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			a = [1, 2, 3];
			result = asyncAllApply(
				items: a,
				mapper: ( item ) => {
					sleep( 1000 );
					return item * 2;
				},
				timeout: 500,
				timeUnit: "MILLISECONDS"
			)
			println( result )
		""", context);
		// @formatter:on
		Array aResult = variables.getAsArray( result );
		assertThat( aResult ).isNotNull();
	}

	@DisplayName( "Test asyncAllApply() with the basics using a struct" )
	@Test
	public void testAsyncApplyWithBasicsStruct() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			a = { "one": 1, "two": 2, "three": 3 };
			result = asyncAllApply( a, item =>{
				println( "Processing item: " & item.toString() )
				 item.value = item.value * 2
				return item
			})
			println( result )
			assert result.one == 2
			assert result.two == 4
			assert result.three == 6
		""", context);
		// @formatter:on
		IStruct aResult = variables.getAsStruct( result );
		assertThat( aResult ).isNotNull();
	}

}
