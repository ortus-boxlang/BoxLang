/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.async;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.DateTimeHelper;

/**
 * This is the BoxLang version of a CompletableFuture to allow for more
 * dynamic goodness and fluent features.
 *
 * This class extends CompletableFuture and adds some additional methods.
 */
public class BoxFuture<T> extends CompletableFuture<T> {

	// The logger for this class
	private BoxLangLogger logger;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default constructor
	 */
	public BoxFuture() {
		super();
		setupLogger();
	}

	/**
	 * Constructor that completes the future with the given value
	 *
	 * @param value The value to complete the future with
	 */
	private BoxFuture( T value ) {
		super();
		setupLogger();
		super.complete( value );
	}

	/**
	 * Constructs a new BoxFuture wrapping an incoming CompletableFuture
	 *
	 * @param future The CompletableFuture to wrap
	 */
	private BoxFuture( CompletableFuture<T> future ) {
		super();
		setupLogger();
		future.whenComplete( ( result, error ) -> {
			if ( error != null ) {
				this.completeExceptionally( error );
			} else {
				this.complete( result );
			}
		} );
	}

	/**
	 * Setup the logger for this class
	 */
	private void setupLogger() {
		this.logger = BoxRuntime.getInstance().getLoggingService().ASYNC_LOGGER;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Fluent Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Completes exceptionally with a BoxLang exception and the passed message.
	 * If not already completed, causes invocations of get() and related methods to throw the given exception.
	 *
	 * @param message The message to include in the exception
	 *
	 * @return true if this invocation caused this CompletableFuture to transition to a completed state, else false
	 */
	public Boolean completeExceptionally( String message ) {
		return completeExceptionally( new BoxRuntimeException( message ) );
	}

	/**
	 * Completes this CompletableFuture with the given value if not otherwise completed before the given timeout
	 * in milliseconds as the default.
	 *
	 * @param value   The value to complete the future with
	 * @param timeout The maximum time to wait in milliseconds
	 *
	 * @return A stage that will complete with the result of the given stage
	 */
	public BoxFuture<T> completeOnTimeout( T value, long timeout ) {
		return completeOnTimeout( value, timeout, "MILLISECONDS" );
	}

	/**
	 * Completes this CompletableFuture with the given value if not otherwise completed before the given timeout.
	 *
	 * @param value   The value to complete the future with
	 * @param timeout The maximum time to wait
	 * @param unit    The time unit of the timeout argument. This can be a TimeUnit or a string representation of a TimeUnit
	 *
	 * @return A stage that will complete with the result of the given stage
	 */
	public BoxFuture<T> completeOnTimeout( T value, long timeout, Object unit ) {
		TimeUnit timeUnit = DateTimeHelper.toTimeUnit( unit );
		return ( BoxFuture<T> ) super.completeOnTimeout( value, timeout, timeUnit );
	}

	/**
	 * Returns the result value when complete, or throws an (unchecked) exception if completed exceptionally.
	 * To better conform with the use of common functional forms, if a computation involved in the completion of this CompletableFuture
	 * threw an exception, this method throws an (unchecked) CompletionException with the underlying exception as its cause.
	 *
	 * @param valueIfAbsent If the returned value is null, then we can pass a default value to return
	 *
	 * @return The result value or the default value if the result is null
	 *
	 * @throws CompletionException   - if this future completed exceptionally or a completion computation threw an exception
	 * @throws CancellationException - if the computation was cancelled
	 */
	public T joinOrDefault( T valueIfAbsent ) {
		T results = super.join();
		return ( results == null ? valueIfAbsent : results );
	}

	/**
	 * Waits if necessary for this future to complete, and then returns its result with
	 * the option to return a default value if the result is null.
	 *
	 * @param valueIfAbsent If the returned value is null, then we can pass a default value to return
	 *
	 * @return The result value or the default value if the result is null
	 *
	 * @throws ExecutionException   - if this future completed exceptionally or a completion computation threw an exception
	 * @throws InterruptedException - if the current thread was interrupted while waiting
	 */
	public T getOrDefault( T valueIfAbsent ) throws InterruptedException, ExecutionException, CancellationException {
		T results = get();
		return ( results == null ? valueIfAbsent : results );
	}

	/**
	 * Get the result of the future as an Attempt object.
	 *
	 * If the future completed exceptionally, then the exception will be returned as an Attempt.
	 *
	 * @return The result as an Attempt object
	 */
	public Attempt<?> getAsAttempt() {
		return getAsAttempt( 0 );
	}

	/**
	 * Waits if necessary for at most the given time for this future to complete, and then returns its result, if available.
	 *
	 * If the future completed exceptionally, then the exception will be returned as an Attempt.
	 *
	 * @param timeout The maximum time to wait in milliseconds
	 *
	 * @return The result as an Attempt object
	 */
	public Attempt<?> getAsAttempt( long timeout ) {
		return getAsAttempt( timeout, "MILLISECONDS" );
	}

	/**
	 * Waits if necessary for at most the given time for this future to complete, and then returns its result, if available.
	 *
	 * If the future completed exceptionally, then the exception will be returned as an Attempt.
	 *
	 * @param timeout The maximum time to wait
	 * @param unit    The time unit of the timeout argument. This can be a TimeUnit or a string representation of a TimeUnit
	 *
	 * @return The result as an Attempt object
	 */
	public Attempt<?> getAsAttempt( long timeout, Object unit ) {
		try {
			return Attempt.of( get( timeout, DateTimeHelper.toTimeUnit( unit ) ) );
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			this.logger.error( "Error executing get() on a future", e );
			return Attempt.of( e );
		}
	}

	/**
	 * Waits if necessary for this future to complete, and then returns its result. If the timeout occurs, then a TimeoutException is thrown.
	 *
	 * @param timeout The maximum time to wait in milliseconds
	 *
	 * @return The result value or the default value if the result is null
	 *
	 * @throws ExecutionException   - if this future completed exceptionally or a completion computation threw an exception
	 * @throws InterruptedException - if the current thread was interrupted while waiting
	 * @throws TimeoutException     - if the wait timed out
	 */
	public T get( long timeout ) throws InterruptedException, ExecutionException, CancellationException, TimeoutException {
		return super.get( timeout, TimeUnit.MILLISECONDS );
	}

	/**
	 * Waits if necessary for this future to complete, and then returns its result. If the timeout occurs, then a TimeoutException is thrown.
	 *
	 * @param timeout The maximum time to wait
	 * @param unit    The time unit of the timeout argument. This can be a TimeUnit or a string representation of a TimeUnit
	 *
	 * @return The result value or the default value if the result is null
	 *
	 * @throws ExecutionException   - if this future completed exceptionally or a completion computation threw an exception
	 * @throws InterruptedException - if the current thread was interrupted while waiting
	 * @throws TimeoutException     - if the wait timed out
	 */
	public T get( long timeout, Object unit ) throws InterruptedException, ExecutionException, CancellationException, TimeoutException {
		return super.get( timeout, DateTimeHelper.toTimeUnit( unit ) );
	}

	/**
	 * Exceptionally completes this CompletableFuture with a TimeoutException if not otherwise completed before the given timeout.
	 *
	 * @param timeout The maximum time to wait in milliseconds
	 *
	 * @return A stage that will complete with the result of the given stage
	 */
	public BoxFuture<T> orTimeout( long timeout ) {
		return orTimeout( timeout, "MILLISECONDS" );
	}

	/**
	 * Exceptionally completes this CompletableFuture with a TimeoutException if not otherwise completed before the given timeout.
	 *
	 * @param timeout The maximum time to wait
	 * @param unit    The time unit of the timeout argument. This can be a TimeUnit or a string representation of a TimeUnit
	 *
	 * @return A stage that will complete with the result of the given stage
	 */
	public BoxFuture<T> orTimeout( long timeout, Object unit ) {
		TimeUnit timeUnit = DateTimeHelper.toTimeUnit( unit );
		return ( BoxFuture<T> ) super.orTimeout( timeout, timeUnit );
	}

	/**
	 * Alias to {@link #exceptionally} for fluency
	 *
	 * @param errorFunction The function to apply if an exception occurs
	 *
	 * @return The future
	 */
	public BoxFuture<T> onError( Function<Throwable, T> errorFunction ) {
		return new BoxFuture<>( exceptionally( errorFunction ) );
	}

	/**
	 * Alias to thenApply for fluency
	 *
	 * @param function The function to apply
	 *
	 * @return The future
	 */
	public <U> BoxFuture<U> then( Function<T, U> function ) {
		return new BoxFuture<>( this.thenApply( function ) );
	}

	/**
	 * Alias to thenApplyAsync for fluency
	 *
	 * @param function The function to apply
	 *
	 * @return The future
	 */
	public <U> BoxFuture<U> thenAsync( Function<T, U> function ) {
		return new BoxFuture<>( this.thenApplyAsync( function ) );
	}

	/**
	 * Alias to thenApply for fluency
	 *
	 * @param function The function to apply
	 * @param executor The executor to run the function on
	 *
	 * @return The future
	 */
	public <U> BoxFuture<U> then( Function<T, U> function, Executor executor ) {
		return new BoxFuture<>( this.thenApplyAsync( function, executor ) );
	}

	/**
	 * Alias to thenApplyAsync for fluency
	 *
	 * @param function The function to apply
	 * @param executor The executor to run the function on
	 *
	 * @return The future
	 */
	public <U> BoxFuture<U> thenAsync( Function<T, U> function, Executor executor ) {
		return new BoxFuture<>( this.thenApplyAsync( function, executor ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns a new BoxFuture that is already completed exceptionally with the given exception message.
	 * The type will be BoxRuntimeException.
	 */
	public static BoxFuture<?> failedFuture( String message ) {
		return new BoxFuture<>( CompletableFuture.failedFuture( new BoxRuntimeException( message ) ) );
	}

	/**
	 * Alias to supplyAsync for fluency, mostly used by BoxLang directly
	 *
	 * @param supplier The supplier to run
	 *
	 * @return The future of the supplier
	 */
	public static <T> BoxFuture<T> run( Supplier<T> supplier ) {
		return new BoxFuture<>( CompletableFuture.supplyAsync( supplier ) );
	}

	/**
	 * Alias to supplyAsync for fluency, mostly used by BoxLang directly using a specific executor
	 *
	 * @param supplier The supplier to run
	 * @param executor The executor to run the supplier on
	 *
	 * @return The future of the supplier
	 */
	public static <T> BoxFuture<T> run( Supplier<T> supplier, Executor executor ) {
		return new BoxFuture<>( CompletableFuture.supplyAsync( supplier, executor ) );
	}

	/**
	 * Returns a new Executor that submits a task to the default executor after the given delay (or no delay if non-positive).
	 *
	 * @param delay The time from now to delay execution
	 * @param unit  The time unit of the delay argument. This can be a TimeUnit or a string representation of a TimeUnit
	 *
	 * @return A new Executor that submits a task to the default executor after the given delay
	 */
	public static Executor delayedExecutor( long delay, Object unit ) {
		TimeUnit timeUnit = DateTimeHelper.toTimeUnit( unit );
		return delayedExecutor( delay, timeUnit );
	}

	/**
	 * Returns a new Executor that submits a task to the given base executor after the given delay (or no delay if non-positive).
	 *
	 * @param delay    The time from now to delay execution
	 * @param unit     The time unit of the delay argument. This can be a TimeUnit or a string representation of a TimeUnit
	 * @param executor The executor to run the task on
	 *
	 * @return A new Executor that submits a task to the given base executor after the given delay
	 */
	public static Executor delayedExecutor( long delay, Object unit, Executor executor ) {
		TimeUnit timeUnit = DateTimeHelper.toTimeUnit( unit );
		return delayedExecutor( delay, timeUnit, executor );
	}

	/**
	 * --------------------------------------------------------------------------
	 * BoxLang Enhanced Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method accepts an array of future objects, closures or an array of future objects/closures
	 * in order to execute them in parallel. It will return back to you a future that will return back an array
	 * of results from every future that was executed. This way you can further attach processing and pipelining
	 * on the constructed array of values.
	 * <p>
	 * This means that the futures will be executed in parallel and the results will be returned in the order
	 * that they were passed in. This also means that this operation is non-blocking and will return immediately
	 * until you call get() on the future.
	 * <p>
	 * Each future can be a BoxFuture or a CompletableFuture or a BoxLang Function that will be treated as a future.
	 * <p>
	 * This uses the default executor (ForkJoinPool.commonPool()).
	 *
	 * @param context The context of the current execution
	 * @param futures The array of futures to execute
	 *
	 * @return A future that will return the results in an array
	 */
	public static BoxFuture<Array> all( IBoxContext context, Array futures ) {
		return all( context, futures, BoxRuntime.getInstance().getAsyncService().getCommonForkJoinPool() );
	}

	/**
	 * This method accepts an array of future objects, closures or an array of future objects/closures
	 * in order to execute them in parallel. It will return back to you a future that will return back an array
	 * of results from every future that was executed. This way you can further attach processing and pipelining
	 * on the constructed array of values.
	 * <p>
	 * This means that the futures will be executed in parallel and the results will be returned in the order
	 * that they were passed in. This also means that this operation is non-blocking and will return immediately
	 * until you call get() on the future.
	 * <p>
	 * Each future can be a BoxFuture or a CompletableFuture or a BoxLang Function that will be treated as a future.
	 * <p>
	 * You can also pass a custom ExecutorRecord to use for the execution of the futures if and ONLY if
	 * the incoming array of futures is an array of closures/lambdas or functions.
	 *
	 * <pre>
	 * results = all( [f1, f2, f3] ).get()
	 * all( [f1, f2, f3] ).then( (values) => logResults( values ) );
	 * </pre>
	 *
	 * @param context        The context of the current execution
	 * @param futures        The array of futures to execute
	 * @param executorRecord The executor to use
	 *
	 * @return A future that will return the results in an array
	 */
	public static BoxFuture<Array> all( IBoxContext context, Array futures, ExecutorRecord executorRecord ) {
		// If futures is null or empty, return an empty array future
		if ( futures == null || futures.isEmpty() ) {
			return BoxFuture.completedFuture( new Array() );
		}

		// Process the incoming array to make sure it complies to our expectations
		BoxFuture<?>[] aFutures = futuresWrap( context, futures, executorRecord );

		// Send to allOf() for execution
		return new BoxFuture<>( allOf( aFutures )
		    .thenApplyAsync( v -> {
			    // All futures are now complete, so join() won't block or throw checked exceptions
			    return Arrays
			        .stream( aFutures )
			        .map( BoxFuture::join )
			        .collect( BLCollector.toArray() );
		    } ) );
	}

	/**
	 * This method accepts an array of future objects, closures or an array of future objects/closures
	 * in order to execute them in parallel. It will return back to you a future that will return back the first
	 * result from the futures that was executed. This way you can further attach processing and pipelining
	 * on the constructed value.
	 * <p>
	 * This means that the futures will be executed in parallel and the result will be returned as soon as one of
	 * the futures completes. This also means that this operation is non-blocking and will return immediately
	 * until you call get() on the future.
	 *
	 * @param context The context of the current execution
	 * @param futures The array of futures to execute
	 *
	 * @return A future that will return the first result from the futures
	 */
	public static BoxFuture<Object> any( IBoxContext context, Array futures ) {
		return any( context, futures, BoxRuntime.getInstance().getAsyncService().getCommonForkJoinPool() );
	}

	/**
	 * This method accepts an array of future objects, closures or an array of future objects/closures
	 * in order to execute them in parallel. It will return back to you a future that will return back the first
	 * result from the futures that was executed. This way you can further attach processing and pipelining
	 * on the constructed value.
	 * <p>
	 * This means that the futures will be executed in parallel and the result will be returned as soon as one of
	 * the futures completes. This also means that this operation is non-blocking and will return immediately
	 * until you call get() on the future.
	 *
	 * @param context        The context of the current execution
	 * @param futures        The array of futures to execute
	 * @param executorRecord The executor to use for the execution of the futures if they are functions or closures
	 *
	 * @return A future that will return the first result from the futures
	 */
	public static BoxFuture<Object> any( IBoxContext context, Array futures, ExecutorRecord executorRecord ) {
		// If futures is null or empty, return an empty future
		if ( futures == null || futures.isEmpty() ) {
			return BoxFuture.completedFuture( null );
		}

		// Process the incoming array to make sure it complies to our expectations
		BoxFuture<?>[] aFutures = futuresWrap( context, futures, executorRecord );

		// Send to any() for execution
		return new BoxFuture<>( anyOf( aFutures ) );
	}

	/**
	 * Creates a new BoxFuture that is already completed with the given value.
	 *
	 * @param value The value to complete the future with
	 *
	 * @return A future that is already completed with the given value
	 */
	public static BoxFuture<?> ofValue( Object value ) {
		return new BoxFuture<>( value );
	}

	/**
	 * Creates a new BoxFuture from a CompletableFuture
	 *
	 * @param value The CompletableFuture to wrap
	 *
	 * @return A future that is already completed with the given value
	 */
	public static <U> BoxFuture<U> completedFuture( U value ) {
		return new BoxFuture<>( CompletableFuture.completedFuture( value ) );
	}

	/**
	 * Creates a new BoxFuture from a CompletableFuture
	 *
	 * @param future The CompletableFuture to wrap
	 *
	 * @return A future that is already completed with the given value
	 */
	public static BoxFuture<?> ofCompletableFuture( CompletableFuture<?> future ) {
		return new BoxFuture<>( future );
	}

	/**
	 * Creates a new BoxFuture that is completed by executing the supplier on the default executor.
	 *
	 * @param context  The context of the current execution
	 * @param function The BoxLang function to execute
	 *
	 * @return A future that is completed by executing the supplier on the default executor
	 */
	public static BoxFuture<?> ofFunction( IBoxContext context, ortus.boxlang.runtime.types.Function function ) {
		return run( new ortus.boxlang.runtime.interop.proxies.Supplier<>( function, context, null ) );
	}

	/**
	 * Creates a new BoxFuture that is completed by executing the supplier on passed in executor.
	 *
	 * @param context  The context of the current execution
	 * @param function The BoxLang function to execute
	 * @param executor The executor to run the function on
	 *
	 * @return A future that is completed by executing the supplier on the default executor
	 */
	public static BoxFuture<?> ofFunction( IBoxContext context, ortus.boxlang.runtime.types.Function function, Executor executor ) {
		return run( new ortus.boxlang.runtime.interop.proxies.Supplier<>( function, context, null ), executor );
	}

	/**
	 * This function can accept an array of items or a struct of items and apply a function
	 * to each of the item's in parallel. The `mapper` argument receives the appropriate item
	 * and must return a result.
	 * <p>
	 * The timeout will be infinite by default and in the default fork/join pool.
	 *
	 * <pre>
	 * // Array
	 * allApply( items, ( item ) => item.getMemento() )
	 * // Struct: The result object is a struct of `key` and `value`
	 * allApply( data, ( item ) => item.key &amp; item.value.toString() )
	 * </pre>
	 *
	 * @param context      The context of the current execution
	 * @param items        The items to apply the function to, this can be an array or a struct
	 * @param mapper       The function to apply to each item
	 * @param errorHandler The function to handle any errors that occur, this can be null
	 *
	 * @return An array or struct of the results
	 */
	public static Object allApply(
	    IBoxContext context,
	    Object items,
	    ortus.boxlang.runtime.types.Function mapper,
	    ortus.boxlang.runtime.types.Function errorHandler ) {
		return allApply( context, items, mapper, errorHandler, 0, TimeUnit.MILLISECONDS, null );
	}

	/**
	 * This function can accept an array of items or a struct of items and apply a function
	 * to each of the item's in parallel. The `mapper` argument receives the appropriate item
	 * and must return a result.
	 * <p>
	 * The timeout will be infinite by default and in the passed executor.
	 *
	 * <pre>
	 * // Array
	 * allApply( items, ( item ) => item.getMemento() )
	 * // Struct: The result object is a struct of `key` and `value`
	 * allApply( data, ( item ) => item.key &amp; item.value.toString() )
	 * </pre>
	 *
	 * @param context      The context of the current execution
	 * @param items        The items to apply the function to, this can be an array or a struct
	 * @param mapper       The function to apply to each item
	 *
	 * @param errorHandler The function to handle any errors that occur, this can be null
	 * @param executor     The executor to run the function on
	 *
	 * @return An array or struct of the results
	 */
	public static Object allApply(
	    IBoxContext context,
	    Object items,
	    ortus.boxlang.runtime.types.Function mapper,
	    ortus.boxlang.runtime.types.Function errorHandler,
	    ExecutorRecord executor ) {
		return allApply( context, items, mapper, errorHandler, 0, TimeUnit.MILLISECONDS, executor );
	}

	/**
	 * This function can accept an array of items or a struct of items and apply a function
	 * to each of the item's in parallel. The `mapper` argument receives the appropriate item
	 * and must return a result.
	 *
	 * <pre>
	 * // Array
	 * allApply( items, ( item ) => item.getMemento() )
	 * // Struct: The result object is a struct of `key` and `value`
	 * allApply( data, ( item ) => item.key &amp; item.value.toString() )
	 * </pre>
	 *
	 * @param context      The context of the current execution
	 * @param items        The items to apply the function to, this can be an array or a struct
	 * @param mapper       The function to apply to each item
	 * @param errorHandler The function to handle any errors that occur, this can be null
	 * @param timeout      The maximum time to wait
	 * @param unit         The time unit of the timeout argument. This can be a TimeUnit or a string representation of a TimeUnit
	 *
	 * @return An array or struct of the results
	 */
	public static Object allApply(
	    IBoxContext context,
	    Object items,
	    ortus.boxlang.runtime.types.Function mapper,
	    ortus.boxlang.runtime.types.Function errorHandler,
	    long timeout,
	    Object unit ) {
		return allApply( context, items, mapper, errorHandler, timeout, unit, null );
	}

	/**
	 * This function can accept an array of items or a struct of items and apply a function
	 * to each of the item's in parallel. The `mapper` argument receives the appropriate item
	 * and must return a result.
	 *
	 * <pre>
	 * // Array
	 * allApply( items, ( item ) => item.getMemento() )
	 * // Struct: The result object is a struct of `key` and `value`
	 * allApply( data, ( item ) => item.key &amp; item.value.toString() )
	 * </pre>
	 *
	 * @param context      The context of the current execution
	 * @param items        The items to apply the function to, this can be an array or a struct
	 * @param mapper       The function to apply to each item
	 * @param errorHandler The function to handle any errors that occur, this can be null
	 * @param timeout      The maximum time to wait
	 * @param unit         The time unit of the timeout argument. This can be a TimeUnit or a string representation of a TimeUnit
	 * @param executor     The executor to run the function on
	 *
	 * @return An array or struct of the results
	 */
	public static Object allApply(
	    IBoxContext context,
	    Object items,
	    ortus.boxlang.runtime.types.Function mapper,
	    ortus.boxlang.runtime.types.Function errorHandler,
	    long timeout,
	    Object unit,
	    ExecutorRecord executor ) {
		// If items is null, return an empty array
		if ( items == null ) {
			return Array.EMPTY;
		}

		// Array Processing
		if ( items instanceof Array castedArray ) {
			return allApplyArray( context, castedArray, mapper, errorHandler, timeout, unit, executor );
		}
		// Process a Struct
		else if ( items instanceof IStruct castedStruct ) {
			return allApplyStruct( context, castedStruct, mapper, errorHandler, timeout, unit, executor );
		}
		// Add other types here if needed
		// If we get here, then the items argument is not an array or a struct
		// This is an error, so we throw an exception
		else {
			throw new BoxRuntimeException( "The items argument must be an array or a struct" );
		}
	}

	/**
	 * Private helper method to apply a function to each item in an array in parallel
	 *
	 * @param context      The context of the current execution
	 * @param array        The array to process
	 * @param mapper       The function to apply to each item
	 * @param errorHandler The function to handle any errors that occur, this can be null
	 * @param timeout      The maximum time to wait
	 * @param unit         The time unit of the timeout argument
	 * @param executor     The executor to run the function on
	 *
	 * @return An array of the results
	 */
	private static Array allApplyArray(
	    IBoxContext context,
	    Array array,
	    ortus.boxlang.runtime.types.Function mapper,
	    ortus.boxlang.runtime.types.Function errorHandler,
	    long timeout,
	    Object unit,
	    ExecutorRecord executor ) {
		TimeUnit						timeUnit	= DateTimeHelper.toTimeUnit( unit );
		final BoxLangLogger				allLogger	= BoxRuntime.getInstance().getLoggingService().ASYNC_LOGGER;

		// Create all futures in parallel (non-blocking)
		// @formatter:off
		List<CompletableFuture<Object>>	futures		= array
		    .stream()
		    .map( item -> {
				return CompletableFuture.supplyAsync( () -> {
						try {
							// Apply the mapper function directly to each item
							return new ortus.boxlang.runtime.interop.proxies.Function<>( mapper, context, null ).apply( item );
						} catch ( Exception e ) {
							allLogger.error( "Error executing mapper function on item", e );
							// Handle error with error handler if provided, otherwise return exception struct
							if ( errorHandler != null ) {
								try {
									return new ortus.boxlang.runtime.interop.proxies.Function<>( errorHandler, context, null ).apply( e );
								} catch ( Exception handlerError ) {
									allLogger.error( "Error in error handler", handlerError );
									return ExceptionUtil.throwableToStruct( handlerError );
								}
							}
							// No handler provided, return the exception as a struct
							else {
								return ExceptionUtil.throwableToStruct( e );
							}
						}
					},
						// Bound the executor to the CompletableFuture
						executor != null ? executor.executor() : ForkJoinPool.commonPool()
					);
			} )
		    .collect( Collectors.toList() );
		// @formatter:on

		// Wait for all futures to complete with timeout handling
		try {
			CompletableFuture<Void> allFutures = CompletableFuture.allOf( futures.toArray( new CompletableFuture[ 0 ] ) );

			// Apply timeout if specified and wait for completion
			if ( timeout > 0 ) {
				allFutures.get( timeout, timeUnit );
			} else {
				allFutures.get();
			}

			// All completed successfully, collect results
			return futures
			    .stream()
			    .map( CompletableFuture::join )  // Won't block since all are complete
			    .collect( BLCollector.toArray() );
		} catch ( TimeoutException e ) {
			allLogger.error( "Timeout waiting for all array items to complete", e );
			// Cancel remaining futures, not guaranteed to stop them but will prevent further processing
			futures.forEach( f -> f.cancel( true ) );
			// It's up to the user to shut down the executor if needed
			throw new BoxRuntimeException(
			    "Array processing timed out after " + timeout + " " + timeUnit,
			    "java.lang.TimeoutException", // type
			    e // cause
			);
		} catch ( Exception e ) {
			allLogger.error( "Error waiting for array processing", e );
			throw new BoxRuntimeException( "Error during array parallel processing", e );
		}
	}

	/**
	 * Private helper method to apply a function to each item in a struct in parallel
	 *
	 * @param context      The context of the current execution
	 * @param struct       The struct to process
	 * @param mapper       The function to apply to each item
	 * @param errorHandler The function to handle any errors that occur, this can be null
	 * @param timeout      The maximum time to wait
	 * @param unit         The time unit of the timeout argument
	 * @param executor     The executor to run the function on
	 *
	 * @return A struct of the results
	 */
	private static IStruct allApplyStruct(
	    IBoxContext context,
	    IStruct struct,
	    ortus.boxlang.runtime.types.Function mapper,
	    ortus.boxlang.runtime.types.Function errorHandler,
	    long timeout,
	    Object unit,
	    ExecutorRecord executor ) {
		TimeUnit										timeUnit	= DateTimeHelper.toTimeUnit( unit );
		final BoxLangLogger								allLogger	= BoxRuntime.getInstance().getLoggingService().ASYNC_LOGGER;
		IStruct											result		= new Struct();

		// Create all futures in parallel (non-blocking)
		// @formatter:off
		List<CompletableFuture<Map.Entry<Key, Object>>>	futures		= struct
		    .entrySet()
		    .stream()
			// Every item in the collection becomes a CompletableFuture
			// that will execute the mapper function asynchronously
			// and return a key-value pair with the processed value
			// If an error occurs, it will return the key with an error struct
		    .map( entry -> {
				CompletableFuture<Map.Entry<Key, Object>> future = CompletableFuture
					.supplyAsync( () -> {
							try {
								// Create key-value struct for the mapper function
								IStruct itemStruct = Struct.of( Key.key, entry.getKey(), Key.value, entry.getValue() );
								// Apply the mapper function to the itemStruct
								Object mappedResult = (IStruct) new ortus.boxlang.runtime.interop.proxies.Function<>( mapper, context, null ).apply( itemStruct );
								if( !( mappedResult instanceof IStruct ) ) {
									allLogger.error("Mapper function did not return an instance of IStruct. Returned: " + mappedResult.getClass().getName());
									throw new BoxRuntimeException( "Mapper function must return a struct, but it returned a: " + mappedResult.getClass().getName() );
								}
								// Return the key with the processed value
								return new AbstractMap.SimpleEntry<>(
									entry.getKey(),
									((IStruct) mappedResult).get( Key.value )
								);
							} catch ( Exception e ) {
								allLogger.error( "Error executing mapper function on struct entry", e );
								Object errorResult;
								// Handle error with error handler if provided
								if ( errorHandler != null ) {
									try {
										errorResult = new ortus.boxlang.runtime.interop.proxies.Function<>( errorHandler, context, null ).apply( e );
									} catch ( Exception handlerError ) {
										allLogger.error( "Error in error handler", handlerError );
										errorResult = ExceptionUtil.throwableToStruct( handlerError );
									}
								} else {
									errorResult = ExceptionUtil.throwableToStruct( e );
								}
								// Return the key-error pair
								return new AbstractMap.SimpleEntry<>( entry.getKey(), errorResult );
							}
						},
					executor != null ? executor.executor() : ForkJoinPool.commonPool()
				);
				return future;
			} )
		    .collect( Collectors.toList() );
		// @formatter:on

		// Wait for all futures to complete with timeout handling
		try {
			CompletableFuture<Void> allFutures = CompletableFuture.allOf( futures.toArray( new CompletableFuture[ 0 ] ) );

			// Apply timeout if specified, wait for completion
			if ( timeout > 0 ) {
				allFutures.get( timeout, timeUnit );
			} else {
				allFutures.get();
			}

			// All completed successfully, collect results into the struct
			futures
			    .stream()
			    .map( CompletableFuture::join )  // Won't block since all are complete
			    .forEach( entry -> result.put( entry.getKey(), entry.getValue() ) );
			return result;

		} catch ( TimeoutException e ) {
			allLogger.error( "Timeout waiting for all struct entries to complete", e );
			// Cancel remaining futures
			futures.forEach( f -> f.cancel( true ) );
			throw new BoxRuntimeException(
			    "Struct processing timed out after " + timeout + " " + timeUnit, // message
			    "java.lang.TimeoutException", // type
			    e // cause
			);
		} catch ( Exception e ) {
			allLogger.error( "Error waiting for struct processing", e );
			throw new BoxRuntimeException( "Error during struct parallel processing", e );
		}
	}

	/**
	 * This method accepts an array of future objects, closures or an array of future objects/closures
	 *
	 * @param context        The context of the current execution
	 * @param futures        The futures to execute. This can be one or more futures or an array of futures
	 * @param executorRecord The executor to use for running the futures
	 *
	 * @return An array of BoxFuture objects
	 */
	public static BoxFuture<?>[] futuresWrap( IBoxContext context, Array futures, ExecutorRecord executorRecord ) {
		// Wrap all the futures in a BoxFuture
		return futures
		    .stream()
		    .map( future -> {

			    if ( future == null ) {
				    throw new BoxRuntimeException( "Null future found in futures array" );
			    }

			    BoxFuture<?> targetFuture = null;

			    // Already a BoxFuture
			    if ( future instanceof BoxFuture ) {
				    targetFuture = ( BoxFuture<?> ) future;
			    }
			    // Convert CompletableFuture to BoxFuture
			    else if ( future instanceof CompletableFuture ) {
				    targetFuture = BoxFuture.ofCompletableFuture( ( CompletableFuture<?> ) future );
			    }
			    // If it's a function, then wrap it in a Proxy Supplier
			    else if ( future instanceof ortus.boxlang.runtime.types.Function castedFunction ) {
				    targetFuture = run( new ortus.boxlang.runtime.interop.proxies.Supplier<>( castedFunction, context, null ), executorRecord.executor() );
			    } else {
				    throw new BoxRuntimeException(
				        "Invalid future type: " + future.getClass().getSimpleName() +
				            ". Expected BoxFuture, CompletableFuture, or Function"
				    );
			    }

			    // Create a new BoxFuture to handle both success and error cases
			    BoxFuture<Object> resultFuture = new BoxFuture<>();
			    targetFuture.whenComplete( ( result, throwable ) -> {
				    if ( throwable != null ) {
					    // Log the error
					    BoxRuntime.getInstance().getLoggingService().ASYNC_LOGGER.error( "Error executing future", throwable );
					    // Complete with error struct
					    resultFuture.complete(
					        ExceptionUtil.throwableToStruct( throwable )
					    );
				    } else {
					    resultFuture.complete( result );
				    }
			    } );
			    return resultFuture;
		    } )
		    .toArray( BoxFuture[]::new );
	}

}
