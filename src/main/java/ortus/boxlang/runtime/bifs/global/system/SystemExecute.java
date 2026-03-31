
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

package ortus.boxlang.runtime.bifs.global.system;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Execute a system command" )

public class SystemExecute extends BIF {

	/**
	 * Grace period in milliseconds to wait for stream reader threads to finish
	 * after the process exits. Handles cases where child processes keep pipes open.
	 */
	private static final long STREAM_DRAIN_GRACE_MS = 5000;

	/**
	 * Constructor
	 */
	public SystemExecute() {
		super();
		// Uncomment and define declare argument to this BIF
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.of( "name" ) ),
		    new Argument( false, "any", Key.arguments ),
		    new Argument( false, "long", Key.timeout ),
		    new Argument( false, "boolean", Key.terminateOnTimeout, false ),
		    new Argument( false, "string", Key.directory ),
		    new Argument( false, "string", Key.output ),
		    new Argument( false, "string", Key.error ),
		};
	}

	/**
	 * Executes a system process/command on the underlying OS. Returns a struct with the following keys:
	 *
	 * {
	 * output : [ the command output ]
	 * error : [ any errors emitted by the command ]
	 * timeout : [ boolean value as to whether a timeout was reached ]
	 * terminated : [ boolean value as to whether the process was terminated ]
	 * pid : the PID of the process
	 * }
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The process name or binary path ( e.g. bash or /bin/sh )
	 *
	 * @argument.arguments The process arguments ( e.g. for `java --version` this would be `--version` )
	 *
	 * @argument.timeout The timeout to wait for the command, in seconds ( default unlimited )
	 *
	 * @argument.terminateOnTimeout Whether to terminate the process/command if a timeout is reached
	 *
	 * @argument.directory A working directory to execute the command from
	 *
	 * @argument.ouptut An optional file path to write the command output to
	 *
	 * @argument.error An optional file path to write errors to
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String				bin					= arguments.getAsString( Key.of( "name" ) );
		Object				args				= arguments.get( Key.arguments );
		String				directory			= arguments.getAsString( Key.directory );
		Long				timeout				= arguments.getAsLong( Key.timeout );
		Boolean				terminateOnTimeout	= arguments.getAsBoolean( Key.terminateOnTimeout );
		String				outputTarget		= arguments.getAsString( Key.output );
		String				errorTarget			= arguments.getAsString( Key.error );

		ArrayList<String>	cmd					= new ArrayList<String>( 1 );
		Struct				response			= new Struct( new HashMap<Key, Object>() {

													{
														put( Key.output, null );
														put( Key.error, null );
														put( Key.timeout, false );
														put( Key.terminated, false );
														put( Key.pid, null );
													}
												} );

		cmd.add( bin );
		if ( args != null ) {
			CastAttempt<String> strAttempt = StringCaster.attempt( args );
			// If the args are a simple value, parse out each argument
			if ( strAttempt.wasSuccessful() ) {
				// ensure we preserve any spaces in
				Matcher matches = Pattern.compile( "[^\\s\"']+|\"[^\"]*\"|'[^']*'" ).matcher( strAttempt.get() );
				matches.reset();
				while ( matches.find() ) {
					cmd.add( matches.group() );
				}
			} else {
				// If args are an array, use the array values as arguments directly
				CastAttempt<Array> arrAttempt = ArrayCaster.attempt( args );
				if ( arrAttempt.wasSuccessful() ) {
					arrAttempt.get().stream().forEach( arg -> cmd.add( StringCaster.cast( arg ) ) );
				} else {
					throw new BoxRuntimeException(
					    String.format(
					        "The provided process arguments provided [%s] could not be parsed in to command arguments",
					        args.toString()
					    )
					);
				}
			}
		}

		ProcessBuilder processBuilder = new ProcessBuilder( cmd );

		if ( directory != null ) {
			processBuilder.directory( Path.of( directory ).toFile() );
		}

		if ( outputTarget != null ) {
			processBuilder.redirectOutput( Path.of( outputTarget ).toFile() );
		}
		if ( errorTarget != null ) {
			processBuilder.redirectError( Path.of( errorTarget ).toFile() );
		}

		Process	process		= null;
		Integer	exitCode	= null;

		try {
			process = processBuilder.start();
			response.put( Key.pid, process.pid() );

			// Close stdin since we are not writing to the process
			process.getOutputStream().close();

			// Start draining stdout and stderr concurrently in background threads.
			// Reading both streams in parallel prevents buffer deadlocks (where one
			// full buffer blocks the process while we're stuck reading the other).
			// It also prevents hangs when child processes inherit the pipe file
			// descriptors and keep them open after the main process exits.
			final Process	finalProcess	= process;
			StringBuilder	stdoutBuilder	= new StringBuilder();
			StringBuilder	stderrBuilder	= new StringBuilder();
			Thread			stdoutThread	= null;
			Thread			stderrThread	= null;

			if ( outputTarget == null ) {
				stdoutThread = new Thread( () -> drainStream( finalProcess.inputReader(), stdoutBuilder ), "SystemExecute-stdout" );
				stdoutThread.setDaemon( true );
				stdoutThread.start();
			}

			if ( errorTarget == null ) {
				stderrThread = new Thread( () -> drainStream( finalProcess.errorReader(), stderrBuilder ), "SystemExecute-stderr" );
				stderrThread.setDaemon( true );
				stderrThread.start();
			}

			// Wait for the process to complete
			if ( timeout != null && timeout != 0L ) {
				boolean finished = process.waitFor( timeout, TimeUnit.SECONDS );
				response.put( Key.timeout, !finished );

				if ( finished ) {
					exitCode = process.exitValue();
				} else if ( terminateOnTimeout && process.isAlive() ) {
					// "destroy()" is polite; may not stop the process quickly.
					process.destroy();

					// Optional: wait a short grace period, then force kill if still alive
					if ( !process.waitFor( 2, TimeUnit.SECONDS ) && process.isAlive() ) {
						process.destroyForcibly();
						process.waitFor(); // wait until it actually exits
					}

					response.put( Key.terminated, true );

					// Now that it exited, you can capture its exit code
					exitCode = process.exitValue();
				}
			} else {
				exitCode = process.waitFor(); // this returns int exit code
				response.put( Key.timeout, false );
			}

			// After the process exits, wait for stream reader threads to finish.
			// If child processes keep the pipes open, force-close the streams after a grace period.
			joinStreamThread( stdoutThread, finalProcess.getInputStream() );
			joinStreamThread( stderrThread, finalProcess.getErrorStream() );

			if ( !response.getAsBoolean( Key.terminated ) ) {
				if ( outputTarget == null ) {
					response.put( Key.output, stdoutBuilder.toString() );
				}
				if ( errorTarget == null ) {
					response.put( Key.error, stderrBuilder.toString() );
				}
			}

			if ( exitCode != null ) {
				response.put( Key.exitCode, exitCode );
			}

			return response;

		} catch ( IOException e ) {
			throw new BoxRuntimeException(
			    String.format( "An exception occurred while attempting to execute the command [%s]", StringUtils.join( cmd, " " ) ),
			    e
			);
		} catch ( InterruptedException ie ) {
			throw new BoxRuntimeException(
			    "The process was interrupted while waiting for the command to complete",
			    "InterruptedException",
			    ie
			);
		}

	}

	/**
	 * Drains a BufferedReader line-by-line into a StringBuilder.
	 * Designed to be run in a background thread to prevent blocking the main thread.
	 *
	 * @param reader  The reader to drain
	 * @param builder The StringBuilder to append lines to
	 */
	private void drainStream( java.io.BufferedReader reader, StringBuilder builder ) {
		try {
			String line;
			while ( ( line = reader.readLine() ) != null ) {
				if ( builder.length() > 0 ) {
					builder.append( "\n" );
				}
				builder.append( line );
			}
		} catch ( IOException e ) {
			// Stream was closed or errored - stop reading silently.
			// This is expected when we force-close the stream after the grace period.
		}
	}

	/**
	 * Waits for a stream-reader thread to finish within a grace period.
	 * If it doesn't finish in time (e.g. child processes keep the pipe open),
	 * force-closes the underlying stream to unblock the reader.
	 *
	 * @param thread The stream reader thread (may be null)
	 * @param stream The underlying input stream to force-close if needed (may be null)
	 */
	private void joinStreamThread( Thread thread, java.io.InputStream stream ) {
		if ( thread == null ) {
			return;
		}
		try {
			thread.join( STREAM_DRAIN_GRACE_MS );
			if ( thread.isAlive() && stream != null ) {
				// Force-close the stream to unblock the reader thread
				stream.close();
				thread.join( 1000 );
			}
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		} catch ( IOException e ) {
			// Ignore close errors
		}
	}

}
