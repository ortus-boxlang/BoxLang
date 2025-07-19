package ortus.boxlang.runtime.bifs.global.async;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.BoxFuture;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class AsyncAnyTest {

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

	@SuppressWarnings( "unchecked" )
	@DisplayName( "Test asyncAny() with multiple futures" )
	@Test
	public void testAsyncAnyWithMultipleFutures() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			f1 = futureNew( () => {
				sleep( 1000 );
				return "Result from f1";
			} );
			f2 = futureNew( () => {
				sleep( 500 );
				return "Result from f2";
			} );

			result = asyncAny( [f1, f2] );

		""", context);
		// @formatter:on

		BoxFuture<Object>	results			= ( BoxFuture<Object> ) variables.get( result );
		String				futureResult	= ( String ) results.get();
		assertThat( futureResult ).isEqualTo( "Result from f2" );
	}

	@DisplayName( "Test with some closures" )
	@Test
	public void testAsyncAnyWithClosures() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = asyncAny([
				() => {
					sleep( 1000 );
					return "Closure Result 1";
				},
				() => {
					sleep( 500 );
					return "Closure Result 2";
				}
			]);
			""", context);
		// @formatter:on

		BoxFuture<Object>	results			= ( BoxFuture<Object> ) variables.get( result );
		String				futureResult	= ( String ) results.get();
		assertThat( futureResult ).isEqualTo( "Closure Result 2" );
	}

	@DisplayName( "Test with some lambdas" )
	@Test
	public void testAsyncAnyWithLambdas() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = asyncAny([
				() -> {
					sleep( 1000 );
					return "Lambda Result 1";
				},
				() -> {
					sleep( 500 );
					return "Lambda Result 2";
				}
			]);
			""", context);
		// @formatter:on

		BoxFuture<Object>	results			= ( BoxFuture<Object> ) variables.get( result );
		String				futureResult	= ( String ) results.get();
		assertThat( futureResult ).isEqualTo( "Lambda Result 2" );
	}

	@DisplayName( "Test with some lambdas and a custom executor record" )
	@Test
	public void testAsyncAnyWithLambdasAndCustomExecutor() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = asyncAny([
				() -> {
					sleep( 1000 );
					return "Lambda Result 1";
				},
				() -> {
					sleep( 500 );
					return "Lambda Result 2";
				}
			], "io-tasks" );
			""", context);
		// @formatter:on

		BoxFuture<Object>	results			= ( BoxFuture<Object> ) variables.get( result );
		String				futureResult	= ( String ) results.get();
		assertThat( futureResult ).isEqualTo( "Lambda Result 2" );
	}
}
