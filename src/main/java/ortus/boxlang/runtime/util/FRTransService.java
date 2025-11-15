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
package ortus.boxlang.runtime.util;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.interop.DynamicObject;

/**
 * FusionReactor Transaction Service for integrating BoxLang with FusionReactor APM.
 * <p>
 * This service provides a bridge between BoxLang and FusionReactor's transaction tracking
 * and profiling capabilities. It allows BoxLang applications to create, manage, and report
 * on transactions for performance monitoring and debugging purposes.
 * <p>
 * FusionReactor is an Application Performance Monitoring (APM) tool that provides:
 * <ul>
 * <li>Transaction tracking and timing</li>
 * <li>Error and exception reporting</li>
 * <li>Performance profiling</li>
 * <li>Resource monitoring</li>
 * </ul>
 * <p>
 * This service uses dynamic loading to integrate with FusionReactor's API when available,
 * gracefully degrading to a no-op mode when FusionReactor is not present in the classpath.
 * <p>
 * <b>Usage Example:</b>
 *
 * <pre>
 * // Initialize the service (typically done once at startup)
 * FRTransService frService = FRTransService.getInstance( true );
 *
 * if ( frService.isEnabled() ) {
 *     // Start a tracked transaction
 *     DynamicObject transaction = frService.startTransaction(
 *         "MyOperation",
 *         "Processing user request"
 *     );
 *
 *     try {
 *         // ... perform operation ...
 *         frService.endTransaction( transaction );
 *     } catch ( Exception e ) {
 *         frService.errorTransaction( transaction, e );
 *         throw e;
 *     }
 * }
 * </pre>
 * <p>
 * The service implements a singleton pattern with lazy initialization and double-checked
 * locking for thread safety. It automatically waits for FusionReactor to fully initialize
 * before considering itself enabled.
 *
 * @see DynamicObject
 * @see BoxRuntime
 */
public class FRTransService {

	/**
	 * Singleton instance of the FRTransService.
	 */
	private static FRTransService	instance	= null;

	/**
	 * Flag indicating whether FusionReactor integration is enabled and available.
	 * Set to true only if FusionReactor classes are found and successfully initialized.
	 */
	private boolean					FREnabled	= false;

	/**
	 * Dynamic wrapper around the FusionReactor API instance.
	 * Provides access to FusionReactor's transaction tracking methods.
	 */
	private DynamicObject			FRAPI;

	/**
	 * Private constructor for singleton pattern.
	 * <p>
	 * Attempts to initialize FusionReactor integration by:
	 * <ol>
	 * <li>Dynamically loading the FusionReactor API class (com.intergral.fusionreactor.api.FRAPI)</li>
	 * <li>Obtaining the FusionReactor API singleton instance</li>
	 * <li>Waiting for FusionReactor to complete initialization (polls every 200ms)</li>
	 * <li>Wrapping the API in a DynamicObject for type-safe invocation</li>
	 * </ol>
	 * <p>
	 * If FusionReactor is not available in the classpath or initialization fails,
	 * the service gracefully disables itself and all subsequent method calls become no-ops.
	 *
	 * @param enabled whether to attempt FusionReactor initialization (false = always disabled)
	 */
	private FRTransService( Boolean enabled ) {
		if ( !enabled ) {
			FREnabled = false;
			return;
		}
		try {
			// Dynamically load FusionReactor API class
			DynamicObject	frapiClass	= DynamicObject.of( Class.forName( "com.intergral.fusionreactor.api.FRAPI" ) );
			Object			FRAPIObject	= frapiClass.invokeStatic( BoxRuntime.getInstance().getRuntimeContext(), "getInstance" );

			// Wait for FusionReactor to fully initialize (with 200ms polling interval)
			while ( FRAPIObject == null
			    || ! ( ( Boolean ) DynamicObject.of( FRAPIObject ).invoke( BoxRuntime.getInstance().getRuntimeContext(), "isInitialized" ) ) ) {
				Thread.sleep( 200 );
				if ( FRAPIObject == null ) {
					FRAPIObject = frapiClass.invokeStatic( BoxRuntime.getInstance().getRuntimeContext(), "getInstance" );
				}
			}

			FRAPI		= DynamicObject.of( FRAPIObject );
			FREnabled	= true;
		} catch ( Throwable e ) {
			// FusionReactor not available or initialization failed - gracefully disable
			FREnabled = false;
		}
	}

	/**
	 * Returns the singleton instance of FRTransService.
	 * <p>
	 * This method implements lazy initialization with double-checked locking
	 * for thread-safe singleton creation. The instance is created only once,
	 * even in multi-threaded environments.
	 * <p>
	 * <b>Note:</b> Once created with a particular enabled flag, subsequent calls
	 * with different flags will return the existing instance without re-initialization.
	 *
	 * @param enabled whether FusionReactor integration should be enabled (only affects first call)
	 *
	 * @return the singleton FRTransService instance
	 */
	public static FRTransService getInstance( Boolean enabled ) {
		if ( instance == null ) {
			synchronized ( FRTransService.class ) {
				if ( instance == null ) {
					instance = new FRTransService( enabled );
				}
			}
		}
		return instance;
	}

	/**
	 * Checks whether FusionReactor integration is enabled.
	 * <p>
	 * Returns true only if:
	 * <ul>
	 * <li>The service was initialized with enabled=true</li>
	 * <li>FusionReactor classes were successfully loaded</li>
	 * <li>FusionReactor API initialization completed successfully</li>
	 * </ul>
	 *
	 * @return true if FusionReactor is available and ready, false otherwise
	 */
	public boolean isEnabled() {
		return FREnabled;
	}

	/**
	 * Starts a new tracked transaction in FusionReactor.
	 * <p>
	 * Creates a transaction that will be tracked in FusionReactor's APM system,
	 * allowing you to monitor execution time, resource usage, and errors for
	 * a specific operation or code block.
	 * <p>
	 * The transaction is automatically associated with the "BL" (BoxLang) application.
	 * You must call {@link #endTransaction(DynamicObject)} when the operation completes,
	 * or {@link #errorTransaction(DynamicObject, Exception)} if an error occurs.
	 * <p>
	 * <b>Example:</b>
	 *
	 * <pre>
	 * DynamicObject transaction = frService.startTransaction(
	 *     "UserLogin",
	 *     "Authenticating user credentials"
	 * );
	 * try {
	 *     // ... perform login operation ...
	 *     frService.endTransaction( transaction );
	 * } catch ( Exception e ) {
	 *     frService.errorTransaction( transaction, e );
	 *     throw e;
	 * }
	 * </pre>
	 *
	 * @param name        the transaction name (e.g., "UserLogin", "DataQuery")
	 * @param description a detailed description of what this transaction does
	 *
	 * @return a DynamicObject wrapping the FusionReactor transaction, or null if FR is disabled
	 */
	public DynamicObject startTransaction( String name, String description ) {
		if ( !FREnabled ) {
			return null;
		}

		DynamicObject FRTransaction = DynamicObject.of( FRAPI.invoke( BoxRuntime.getInstance().getRuntimeContext(), "createTrackedTransaction", name ) );
		FRAPI.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setTransactionApplicationName", "BL" );
		FRTransaction.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setDescription", description );
		return FRTransaction;
	}

	/**
	 * Ends a tracked transaction successfully.
	 * <p>
	 * Closes the transaction in FusionReactor, recording its total execution time
	 * and marking it as completed successfully. This should be called when an
	 * operation completes without errors.
	 * <p>
	 * If FusionReactor is disabled, this method is a no-op.
	 *
	 * @param FRTransaction the transaction object returned by {@link #startTransaction(String, String)}
	 */
	public void endTransaction( DynamicObject FRTransaction ) {
		if ( !FREnabled ) {
			return;
		}
		FRTransaction.invoke( BoxRuntime.getInstance().getRuntimeContext(), "close" );
	}

	/**
	 * Marks a transaction as having encountered an error.
	 * <p>
	 * Associates an exception with the transaction, which will be recorded in
	 * FusionReactor's error tracking system. This allows you to see stack traces,
	 * error rates, and correlate errors with specific transactions in the APM dashboard.
	 * <p>
	 * Typically called in a catch block before re-throwing or handling an exception.
	 * You should still call {@link #endTransaction(DynamicObject)} after this method
	 * to properly close the transaction.
	 * <p>
	 * If FusionReactor is disabled, this method is a no-op.
	 *
	 * @param FRTransaction the transaction object returned by {@link #startTransaction(String, String)}
	 * @param javaException the exception that occurred during transaction execution
	 */
	public void errorTransaction( DynamicObject FRTransaction, Exception javaException ) {
		if ( !FREnabled ) {
			return;
		}
		FRTransaction.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setTrappedThrowable", javaException );
	}

	/**
	 * Sets the name of the current transaction in the thread context.
	 * <p>
	 * Updates the transaction name for the currently executing transaction in the
	 * current thread. This is useful when the transaction name needs to be changed
	 * dynamically based on runtime conditions (e.g., after determining the actual
	 * operation being performed).
	 * <p>
	 * If FusionReactor is disabled, this method is a no-op.
	 *
	 * @param name the new transaction name
	 */
	public void setCurrentTransactionName( String name ) {
		if ( !FREnabled ) {
			return;
		}
		FRAPI.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setTransactionName", name );
	}

	/**
	 * Sets the application name for the current transaction in the thread context.
	 * <p>
	 * Updates the application name associated with the currently executing transaction.
	 * This allows grouping transactions by application or module in FusionReactor's
	 * reporting interface.
	 * <p>
	 * By default, transactions created by {@link #startTransaction(String, String)}
	 * use "BL" (BoxLang) as the application name.
	 * <p>
	 * If FusionReactor is disabled, this method is a no-op.
	 *
	 * @param name the application name (e.g., "MyApp", "API", "BackgroundWorker")
	 */
	public void setCurrentTransactionApplicationName( String name ) {
		if ( !FREnabled ) {
			return;
		}
		FRAPI.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setTransactionApplicationName", name );
	}
}
