package ortus.boxlang.runtime.bifs.global.async;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class RunAsyncTest {

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

	@DisplayName( "Create an complete future with a lambda" )
	@Test
	public void testCompleteFutureWithLambda() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> 42 );
		""", context);
		// @formatter:on

		// Check the result
		BoxFuture<?> future = ( BoxFuture<?> ) variables.get( result );
		assertThat( future ).isInstanceOf( BoxFuture.class );
		assertThat( future.get() ).isEqualTo( 42 );
	}

	@DisplayName( "Complete exceptionally with my message" )
	@Test
	public void testCompleteExceptionally() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> {
				sleep( 1000 );
				return 42
			} );
			e = result.completeExceptionally( "My exception" ) ;
		""", context);
		// @formatter:on

		// Check the result
		BoxFuture<?> future = ( BoxFuture<?> ) variables.get( result );
		assertThat( future.isCompletedExceptionally() ).isTrue();
	}

	@DisplayName( "Complete on timeout override" )
	@Test
	public void testCompleteOnTimeout() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> {
				sleep( 1000 );
				return 42
			} );
			e = result.completeOnTimeout( 100, 200 ) ;
		""", context);
		// @formatter:on

		// Check the result
		BoxFuture<?> future = ( BoxFuture<?> ) variables.get( result );
		assertThat( ( Integer ) future.get() ).isEqualTo( 100 );

		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> {
				sleep( 1000 );
				return 42
			} );
			e = result.completeOnTimeout( 100, 200, "milliseconds" ) ;
		""", context);
		// @formatter:on

		// Check the result
		BoxFuture<?> future2 = ( BoxFuture<?> ) variables.get( result );
		assertThat( ( Integer ) future2.get() ).isEqualTo( 100 );
	}

	@DisplayName( "Join with a value if absent" )
	@Test
	public void testJoinWith() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> null ).joinOrDefault( 42 );
		""", context);
		// @formatter:on

		// Check the result
		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

	@DisplayName( "Get with a value if absent" )
	@Test
	public void testGetWith() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> null ).getOrDefault( 42 );
		""", context);
		// @formatter:on

		// Check the result
		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

	@DisplayName( "Get with a value and a timeout in default milliseconds" )
	@Test
	public void testGetWithTimeout() throws InterruptedException, ExecutionException {
		assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			instance.executeSource("""
				result = runAsync( () -> {
					sleep( 1000 );
					return 42
				} ).get( 100 );
			""", context);
			// @formatter:on
		} );
	}

	@DisplayName( "Use the orTimeout with a default timeout" )
	@Test
	public void testOrTimeout() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> {
				sleep( 1000 );
				return 42
			} ).orTimeout( 100 );
		""", context);
		// @formatter:on

		// Check the result
		BoxFuture<?> future = ( BoxFuture<?> ) variables.get( result );
		assertThrows( ExecutionException.class, future::get );
	}

	@DisplayName( "use the onError() alias for exceptionally" )
	@Test
	public void testOnError() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> {
				if( true ){
					throw( type: "AsyncException", message: "My Exception" );
				}
			} ).onError( throwable -> {
				println( throwable.getMessage() );
				return "async";
			} );
		""", context);
		// @formatter:on

		// Check the result
		BoxFuture<?> future = ( BoxFuture<?> ) variables.get( result );
		assertThat( future.get() ).isEqualTo( "async" );
		assertThat( future.isCompletedExceptionally() ).isFalse();
	}

	@DisplayName( "use exceptionally" )
	@Test
	public void testExceptionally() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> {
				if( true ){
					throw( type: "AsyncException", message: "My Exception" );
				}
			} ).exceptionally( throwable -> {
				println( throwable.getMessage() );
				return "async";
			} );
		""", context);
		// @formatter:on

		// Check the result
		BoxFuture<?> future = BoxFuture.ofCompletableFuture( ( CompletableFuture<?> ) variables.get( result ) );
		assertThat( future.get() ).isEqualTo( "async" );
		assertThat( future.isCompletedExceptionally() ).isFalse();
	}

	@DisplayName( "use then" )
	@Test
	public void testThen() throws InterruptedException, ExecutionException {
		// @formatter:off
		instance.executeSource("""
			result = runAsync( () -> {
				sleep( 1000 );
				return 42;
			} ).then( value -> {
				return value + 1;
			} );
		""", context);
		// @formatter:on

		// Check the result
		BoxFuture<?> future = BoxFuture.ofCompletableFuture( ( CompletableFuture<?> ) variables.get( result ) );
		assertThat( future.get() ).isEqualTo( 43 );
		assertThat( future.isCompletedExceptionally() ).isFalse();
	}

}
