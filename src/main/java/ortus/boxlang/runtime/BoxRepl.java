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
package ortus.boxlang.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.runnables.BoxScript;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * BoxLang Read-Eval-Print-Loop (REPL) implementation.
 *
 * This class provides an interactive environment for executing BoxLang code
 * from an input stream, typically System.in for command-line interaction.
 *
 * Features:
 * - Interactive prompt with BoxLang ASCII art banner
 * - Line-by-line code execution with immediate feedback
 * - Automatic result display for expressions
 * - Buffer output handling
 * - Exit commands (exit/quit)
 * - Exception handling and display
 *
 * The REPL is designed purely for interactive use and always displays
 * prompts and banners. For non-interactive execution, use other BoxLang
 * execution methods like BoxRuntime.executeSource().
 *
 * Usage:
 *
 * <pre>
 * BoxRuntime runtime = BoxRuntime.getInstance();
 * BoxRepl repl = new BoxRepl( runtime );
 * repl.start(); // Uses System.in for interactive session
 *
 * // Or with custom input stream (still interactive)
 * repl.start( customInputStream );
 * </pre>
 */
public class BoxRepl {

	/**
	 * The BoxLang runtime instance
	 */
	private final BoxRuntime runtime;

	/**
	 * Constructor
	 *
	 * @param runtime The BoxLang runtime instance to use for code execution
	 */
	public BoxRepl( BoxRuntime runtime ) {
		this.runtime = runtime;
	}

	/**
	 * Start the REPL using System.in as the input source.
	 * This is the most common usage for interactive command-line sessions.
	 */
	public void start() {
		start( System.in );
	}

	/**
	 * Start the REPL with a custom input stream.
	 *
	 * @param sourceStream The input stream to read BoxLang code from
	 */
	public void start( InputStream sourceStream ) {
		start( sourceStream, runtime.getRuntimeContext() );
	}

	/**
	 * Start the REPL with a custom input stream and execution context.
	 *
	 * @param sourceStream The input stream to read BoxLang code from
	 * @param context      The BoxLang context to use for code execution
	 */
	public void start( InputStream sourceStream, IBoxContext context ) {
		// Create a scripting context for REPL execution
		IBoxContext		scriptingContext	= new ScriptingRequestBoxContext( context );
		BufferedReader	reader				= new BufferedReader( new InputStreamReader( sourceStream ) );
		String			source;
		RequestBoxContext.setCurrent( scriptingContext.getParentOfType( RequestBoxContext.class ) );
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			// Show the interactive banner
			showBanner();

			// Main REPL loop
			while ( ( source = reader.readLine() ) != null ) {
				// Check for exit commands
				if ( isExitCommand( source ) ) {
					break;
				}

				// Execute the source code
				executeReplLine( source, scriptingContext );
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error reading source stream", e );
		} finally {
			RequestBoxContext.removeCurrent();
			Thread.currentThread().setContextClassLoader( oldClassLoader );
		}
	}

	/**
	 * Display the BoxLang REPL banner and instructions.
	 */
	private void showBanner() {
		System.out.println( "██████   ██████  ██   ██ ██       █████  ███    ██  ██████ " );
		System.out.println( "██   ██ ██    ██  ██ ██  ██      ██   ██ ████   ██ ██      " );
		System.out.println( "██████  ██    ██   ███   ██      ███████ ██ ██  ██ ██   ███" );
		System.out.println( "██   ██ ██    ██  ██ ██  ██      ██   ██ ██  ██ ██ ██    ██" );
		System.out.println( "██████   ██████  ██   ██ ███████ ██   ██ ██   ████  ██████ " );
		System.out.println( "" );
		System.out.println( "Enter an expression, then hit enter" );
		System.out.println( "Press Ctrl-C to exit" );
		System.out.println( "" );
		System.out.print( "BoxLang> " );
	}

	/**
	 * Check if the input is an exit command.
	 *
	 * @param input The user input to check
	 *
	 * @return true if the input is an exit command, false otherwise
	 */
	private boolean isExitCommand( String input ) {
		return input != null &&
		    ( input.toLowerCase().equals( "exit" ) || input.toLowerCase().equals( "quit" ) );
	}

	/**
	 * Execute a single line of BoxLang code in the REPL context.
	 *
	 * @param source           The BoxLang source code to execute
	 * @param scriptingContext The execution context
	 */
	private void executeReplLine( String source, IBoxContext scriptingContext ) {
		try {
			// Compile and load the statement
			BoxScript	scriptRunnable		= RunnableLoader.getInstance().loadStatement(
			    scriptingContext,
			    source,
			    BoxSourceType.BOXSCRIPT
			);

			// Execute the code
			Object		result				= scriptRunnable.invoke( scriptingContext );
			boolean		hadBufferContent	= scriptingContext.getBuffer().length() > 0;

			// Flush any buffered output
			scriptingContext.flushBuffer( false );

			// Display result if there was no buffer content and we have a result
			if ( !hadBufferContent && result != null ) {
				displayResult( result );
			} else {
				System.out.println();
			}
		} catch ( AbortException e ) {
			// Handle abort exceptions (like <cfabort>)
			scriptingContext.flushBuffer( true );
			if ( e.getCause() != null ) {
				System.out.println( "Abort: " + e.getCause().getMessage() );
			}
		} catch ( Exception e ) {
			// Handle any other exceptions
			e.printStackTrace();
		}

		// Show prompt for next input
		System.out.print( "BoxLang> " );
	}

	/**
	 * Display the result of executing a BoxLang expression.
	 *
	 * @param result The result object to display
	 */
	private void displayResult( Object result ) {
		// Try to convert to string first
		CastAttempt<String> stringAttempt = StringCaster.attempt( result );
		if ( stringAttempt.wasSuccessful() ) {
			System.out.println( stringAttempt.get() );
		} else {
			// Handle Java arrays by converting to BoxLang Array
			if ( result.getClass().isArray() ) {
				result = Array.fromArray( ( Object[] ) result );
			}
			// Display the object's toString representation
			System.out.println( result );
		}
	}

	/**
	 * Get the runtime instance used by this REPL.
	 *
	 * @return The BoxRuntime instance
	 */
	public BoxRuntime getRuntime() {
		return runtime;
	}
}