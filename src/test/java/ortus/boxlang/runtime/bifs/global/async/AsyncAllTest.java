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
import ortus.boxlang.runtime.types.Array;

public class AsyncAllTest {

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
	@DisplayName( "Test asyncAll() with multiple futures" )
	@Test
	public void testAsyncAllWithMultipleFutures() throws Throwable, ExecutionException {
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

			result = asyncAll( [f1, f2] );

		""", context);
		// @formatter:on

		BoxFuture<Array>	results	= ( BoxFuture<Array> ) variables.get( result );
		Array				array	= ( Array ) results.get();
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( array.get( 0 ) ).isEqualTo( "Result from f1" );
		assertThat( array.get( 1 ) ).isEqualTo( "Result from f2" );
	}

	@DisplayName( "Test with some closures" )
	@Test
	public void testAsyncAllWithClosures() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = asyncAll([
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

		BoxFuture<Array>	results	= ( BoxFuture<Array> ) variables.get( result );
		Array				array	= ( Array ) results.get();
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( array.get( 0 ) ).isEqualTo( "Closure Result 1" );
		assertThat( array.get( 1 ) ).isEqualTo( "Closure Result 2" );
	}

	@DisplayName( "Test with some lambdas" )
	@Test
	public void testAsyncAllWithLambdas() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = asyncAll([
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

		BoxFuture<Array>	results	= ( BoxFuture<Array> ) variables.get( result );
		Array				array	= ( Array ) results.get();
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( array.get( 0 ) ).isEqualTo( "Lambda Result 1" );
		assertThat( array.get( 1 ) ).isEqualTo( "Lambda Result 2" );
	}

	@DisplayName( "Test with some closures and some scopes" )
	@Test
	public void testAsyncAllWithClosuresAndScopes() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			request.value = 2
			application.value = 2

			multiply = ( a, b ) => {
				return a * b
			}

			// Using lambdas with scopes
			result = asyncAll([
				() => {
					var test = multiply( 1, request.value )
					sleep( 1000 )
					return "Lambda Result #test#"
				},
				() => {
					var test = multiply( 1, application.value )
					sleep( 500 )
					return "Lambda Result #test#"
				}
			]);

			""", context);
		// @formatter:on

		BoxFuture<Array>	results	= ( BoxFuture<Array> ) variables.get( result );
		Array				array	= ( Array ) results.get();
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( array.get( 0 ) ).isEqualTo( "Lambda Result 2" );
		assertThat( array.get( 1 ) ).isEqualTo( "Lambda Result 2" );
	}

	@DisplayName( "Test with some lambdas and a custom executor record" )
	@Test
	public void testAsyncAllWithLambdasAndCustomExecutor() throws Throwable, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = asyncAll([
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

		BoxFuture<Array>	results	= ( BoxFuture<Array> ) variables.get( result );
		Array				array	= ( Array ) results.get();
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( array.get( 0 ) ).isEqualTo( "Lambda Result 1" );
		assertThat( array.get( 1 ) ).isEqualTo( "Lambda Result 2" );
	}
}
