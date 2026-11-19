
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Execute a system command" )

public class SystemExecute extends BIF {

	/**
	 * Grace period in milliseconds to wait for stream reader threads to finish
	 * after the process exits. Handles cases where child processes keep pipes open.
	 */
	private static final long	STREAM_DRAIN_GRACE_MS	= 5000;

	/**
	 * Grace period in milliseconds to wait for stream reader threads after a cancellation.
	 * The process tree has been killed by then, so the pipes close quickly.
	 */
	private static final long	CANCEL_DRAIN_GRACE_MS	= 1000;

	/**
	 * How often, in milliseconds, the live loop checks for cancellation, timeout and process exit.
	 */
	private static final long	POLL_INTERVAL_MS		= 50;

	/**
	 * A single line of process output and the stream it came from.
	 *
	 * @param stream Either "stdout" or "stderr"
	 * @param line   The line, without its line terminator
	 */
	private record OutputLine( String stream, String line ) {
	}

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
		    new Argument( false, "boolean", Key.inheritEnvironment, true ),
		    new Argument( false, "struct", Key.environment, new Struct() ),
		    new Argument( false, Argument.FUNCTION, Key.onLine ),
		    new Argument( false, Argument.FUNCTION, Key.isCancelled ),
		    new Argument( false, "boolean", Key.mergeErrors, false ),
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
	 * exitCode : the exit code, when the process exited or was terminated
	 * cancelled : [ boolean value as to whether isCancelled() ended the process ]
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
	 *
	 * @argument.inheritEnvironment Whether to inherit the parent process environment variables. Defaults to true.
	 *
	 * @argument.environment A struct of environment variables to pass to the process. Merged in after the inherit decision.
	 *
	 * @argument.onLine An optional function called as each output line arrives, before the process exits: onLine( line, stream ), where stream is "stdout" or
	 *                  "stderr". It runs on the calling thread. Output is still captured in the result. A stream redirected to a file gets no callbacks.
	 *                  If the function throws, the process is killed and the error is rethrown.
	 *
	 * @argument.isCancelled An optional function polled while the process runs. When it returns true the process tree is terminated and the output captured so
	 *                       far is returned with cancelled = true and terminated = true. A cancellation is not a timeout. Lines still queued when the
	 *                       process is killed are not delivered to onLine, but remain in the captured output.
	 *
	 * @argument.mergeErrors When true, stderr is combined into stdout, so there is a single stream and every line is reported as "stdout". When false (default)
	 *                       the streams stay separate. Cannot be combined with the error argument.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String				bin					= arguments.getAsString( Key.of( "name" ) );
		Object				args				= arguments.get( Key.arguments );
		String				directory			= arguments.getAsString( Key.directory );
		Long				timeout				= arguments.getAsLong( Key.timeout );
		Boolean				terminateOnTimeout	= arguments.getAsBoolean( Key.terminateOnTimeout );
		String				outputTarget		= arguments.getAsString( Key.output );
		String				errorTarget			= arguments.getAsString( Key.error );
		Boolean				inheritEnvironment	= arguments.getAsBoolean( Key.inheritEnvironment );
		IStruct				environment			= arguments.getAsStruct( Key.environment );
		Function			onLine				= arguments.getAsFunction( Key.onLine );
		Function			isCancelled			= arguments.getAsFunction( Key.isCancelled );
		boolean				mergeErrors			= Boolean.TRUE.equals( arguments.getAsBoolean( Key.mergeErrors ) );
		boolean				live				= onLine != null || isCancelled != null;

		ArrayList<String>	cmd					= new ArrayList<String>( 1 );
		Struct				response			= new Struct( new HashMap<Key, Object>() {

													{
														put( Key.output, null );
														put( Key.error, null );
														put( Key.timeout, false );
														put( Key.terminated, false );
														put( Key.cancelled, false );
														put( Key.pid, null );
													}
												} );

		if ( mergeErrors && errorTarget != null ) {
			throw new BoxRuntimeException(
			    "The mergeErrors argument cannot be combined with the error argument, as stderr cannot be both merged and redirected to a file." );
		}

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

		// If inheritEnvironment is false, start with a clean slate; otherwise keep the JVM's inherited env.
		if ( !Boolean.TRUE.equals( inheritEnvironment ) ) {
			processBuilder.environment().clear();
		}

		// Merge any caller-supplied environment variables on top (works regardless of inherit flag).
		if ( environment != null && !environment.isEmpty() ) {
			environment.entrySet().forEach( entry -> processBuilder.environment().put( entry.getKey().getName(), StringCaster.cast( entry.getValue() ) ) );
		}

		if ( directory != null ) {
			processBuilder.directory( Path.of( directory ).toFile() );
		}

		if ( outputTarget != null ) {
			processBuilder.redirectOutput( Path.of( outputTarget ).toFile() );
		}
		if ( errorTarget != null ) {
			processBuilder.redirectError( Path.of( errorTarget ).toFile() );
		}
		if ( mergeErrors ) {
			processBuilder.redirectErrorStream( true );
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
			// When live callbacks are used, completed lines are also queued so the calling thread can deliver them.
			final Process				finalProcess	= process;
			StringBuilder				stdoutBuilder	= new StringBuilder();
			StringBuilder				stderrBuilder	= new StringBuilder();
			BlockingQueue<OutputLine>	lineQueue		= live ? new LinkedBlockingQueue<>() : null;
			Thread						stdoutThread	= null;
			Thread						stderrThread	= null;

			if ( outputTarget == null ) {
				StreamPump pump = new StreamPump( finalProcess.inputReader(), "stdout", stdoutBuilder, lineQueue );
				stdoutThread = new Thread( pump, "SystemExecute-stdout" );
				stdoutThread.setDaemon( true );
				stdoutThread.start();
			}

			// When errors are merged there is no separate stderr stream to read
			if ( errorTarget == null && !mergeErrors ) {
				StreamPump pump = new StreamPump( finalProcess.errorReader(), "stderr", stderrBuilder, lineQueue );
				stderrThread = new Thread( pump, "SystemExecute-stderr" );
				stderrThread.setDaemon( true );
				stderrThread.start();
			}

			if ( live ) {
				// Poll the line queue, the cancel callback, the timeout and the process on the calling thread
				exitCode = waitLive( context, process, onLine, isCancelled, lineQueue, timeout, terminateOnTimeout, response );
			} else if ( timeout != null && timeout != 0L ) {
				// Wait for the process to complete
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
			long grace = response.getAsBoolean( Key.cancelled ) ? CANCEL_DRAIN_GRACE_MS : STREAM_DRAIN_GRACE_MS;
			joinStreamThread( stdoutThread, finalProcess.getInputStream(), grace );
			joinStreamThread( stderrThread, finalProcess.getErrorStream(), grace );

			// Deliver any lines that arrived while we were waiting for the readers to finish, including a final partial line.
			// After a cancellation or a terminated timeout the backlog is dropped, so a slow callback cannot delay the return.
			// The text is still part of the captured output.
			if ( live ) {
				if ( response.getAsBoolean( Key.terminated ) ) {
					lineQueue.clear();
				} else {
					try {
						OutputLine pending;
						while ( ( pending = lineQueue.poll() ) != null ) {
							deliver( context, onLine, pending );
						}
					} catch ( RuntimeException | Error e ) {
						// A timeout without terminateOnTimeout leaves the child running: do not leave it with nobody listening
						if ( process.isAlive() ) {
							killProcessTree( process );
						}
						throw e;
					}
				}
			}

			// A cancellation returns whatever was captured so far; a terminated timeout keeps its historical null output
			if ( !response.getAsBoolean( Key.terminated ) || response.getAsBoolean( Key.cancelled ) ) {
				if ( outputTarget == null ) {
					response.put( Key.output, snapshot( stdoutBuilder ) );
				}
				if ( errorTarget == null ) {
					response.put( Key.error, snapshot( stderrBuilder ) );
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
			if ( live && process != null ) {
				killProcessTree( process );
			}
			throw new BoxRuntimeException(
			    "The process was interrupted while waiting for the command to complete",
			    "InterruptedException",
			    ie
			);
		}

	}

	/**
	 * Waits for the process while delivering output lines, polling the cancel callback and enforcing the timeout.
	 * All callbacks run on the calling thread. If a callback throws, the process tree is killed and the error is rethrown.
	 *
	 * @param context            The BoxLang context used to invoke the callbacks
	 * @param process            The running process
	 * @param onLine             The line callback, may be null
	 * @param isCancelled        The cancel callback, may be null
	 * @param lineQueue          The queue the stream readers add completed lines to
	 * @param timeout            The timeout in seconds, null or 0 for none
	 * @param terminateOnTimeout Whether to kill the process when the timeout is reached
	 * @param response           The response struct, updated with timeout, terminated and cancelled
	 *
	 * @return The exit code, or null if the process was left running after a timeout
	 *
	 * @throws InterruptedException If the calling thread is interrupted
	 */
	private Integer waitLive(
	    IBoxContext context,
	    Process process,
	    Function onLine,
	    Function isCancelled,
	    BlockingQueue<OutputLine> lineQueue,
	    Long timeout,
	    boolean terminateOnTimeout,
	    Struct response ) throws InterruptedException {
		boolean	hasDeadline		= timeout != null && timeout != 0L;
		long	deadlineNanos	= hasDeadline ? System.nanoTime() + TimeUnit.SECONDS.toNanos( timeout ) : 0L;

		try {
			while ( true ) {
				// Block briefly for the next line, then drain what else is ready without starving the cancel and timeout checks
				OutputLine	line		= lineQueue.poll( POLL_INTERVAL_MS, TimeUnit.MILLISECONDS );
				long		drainUntil	= System.nanoTime() + TimeUnit.MILLISECONDS.toNanos( POLL_INTERVAL_MS );
				while ( line != null ) {
					deliver( context, onLine, line );
					if ( System.nanoTime() - drainUntil >= 0 ) {
						break;
					}
					line = lineQueue.poll();
				}

				// A process that already finished keeps its normal completion state, whatever the cancel callback would say
				if ( !process.isAlive() ) {
					return process.exitValue();
				}

				if ( isCancelled != null && checkCancelled( context, isCancelled ) ) {
					response.put( Key.cancelled, true );
					response.put( Key.terminated, true );
					killProcessTree( process );
					return process.exitValue();
				}

				if ( hasDeadline && System.nanoTime() - deadlineNanos >= 0 ) {
					response.put( Key.timeout, true );
					if ( terminateOnTimeout ) {
						killProcessTree( process );
						response.put( Key.terminated, true );
						return process.exitValue();
					}
					return null;
				}
			}
		} catch ( RuntimeException | Error e ) {
			// A callback failed: do not leave the child running with nobody listening
			killProcessTree( process );
			throw e;
		}
	}

	/**
	 * Hands a line to the onLine callback, if there is one.
	 *
	 * @param context The BoxLang context used to invoke the callback
	 * @param onLine  The callback, may be null
	 * @param line    The line and the stream it came from
	 */
	private void deliver( IBoxContext context, Function onLine, OutputLine line ) {
		if ( onLine != null ) {
			context.invokeFunction( onLine, new Object[] { line.line(), line.stream() } );
		}
	}

	/**
	 * Calls the isCancelled callback and interprets its result. A null result means not cancelled.
	 *
	 * @param context     The BoxLang context used to invoke the callback
	 * @param isCancelled The callback
	 *
	 * @return True if the callback asked for cancellation
	 */
	private boolean checkCancelled( IBoxContext context, Function isCancelled ) {
		Object result = context.invokeFunction( isCancelled );
		return result != null && Boolean.TRUE.equals( BooleanCaster.cast( result ) );
	}

	/**
	 * Terminates a process and all of its descendants. Descendants are captured first, because they are
	 * reparented once the parent dies. Polite destroy first, then a forced kill after a short grace period.
	 *
	 * @param process The process to terminate
	 */
	private void killProcessTree( Process process ) {
		List<ProcessHandle> descendants = process.descendants().toList();
		process.destroy();
		descendants.forEach( ProcessHandle::destroy );
		try {
			if ( !process.waitFor( 2, TimeUnit.SECONDS ) ) {
				process.destroyForcibly();
				descendants.forEach( ProcessHandle::destroyForcibly );
				process.waitFor();
			} else {
				descendants.stream().filter( ProcessHandle::isAlive ).forEach( ProcessHandle::destroyForcibly );
			}
		} catch ( InterruptedException e ) {
			process.destroyForcibly();
			descendants.forEach( ProcessHandle::destroyForcibly );
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Reads a captured stream's content safely while a reader thread may still be appending to it.
	 *
	 * @param builder The capture builder
	 *
	 * @return The captured text
	 */
	private String snapshot( StringBuilder builder ) {
		synchronized ( builder ) {
			return builder.toString();
		}
	}

	/**
	 * Waits for a stream-reader thread to finish within a grace period.
	 * If it doesn't finish in time (e.g. child processes keep the pipe open),
	 * force-closes the underlying stream to unblock the reader.
	 *
	 * @param thread  The stream reader thread (may be null)
	 * @param stream  The underlying input stream to force-close if needed (may be null)
	 * @param graceMs How long to wait for the thread before closing the stream, in milliseconds
	 */
	private void joinStreamThread( Thread thread, InputStream stream, long graceMs ) {
		if ( thread == null ) {
			return;
		}
		try {
			thread.join( graceMs );
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

	/**
	 * Reads one process stream character by character so a line that stalls half way is never lost.
	 * Each completed line is appended to the shared capture and, when live callbacks are in use, queued for delivery.
	 * Whatever is buffered when the stream ends or is closed is flushed as a final line.
	 */
	private static final class StreamPump implements Runnable {

		private final BufferedReader			reader;
		private final String					stream;
		private final StringBuilder				capture;
		private final BlockingQueue<OutputLine>	queue;
		private final StringBuilder				pending	= new StringBuilder();
		private boolean							skipLineFeed;

		/**
		 * Creates a pump.
		 *
		 * @param reader  The stream to read
		 * @param stream  The stream name reported to callbacks, "stdout" or "stderr"
		 * @param capture The builder completed lines are appended to
		 * @param queue   The queue completed lines are added to, or null when no callbacks are in use
		 */
		StreamPump( BufferedReader reader, String stream, StringBuilder capture, BlockingQueue<OutputLine> queue ) {
			this.reader		= reader;
			this.stream		= stream;
			this.capture	= capture;
			this.queue		= queue;
		}

		/**
		 * Reads until the stream ends or is closed, then flushes any partial line.
		 */
		@Override
		public void run() {
			char[] buffer = new char[ 4096 ];
			try {
				int count;
				while ( ( count = this.reader.read( buffer ) ) != -1 ) {
					for ( int i = 0; i < count; i++ ) {
						accept( buffer[ i ] );
					}
				}
			} catch ( IOException e ) {
				// Stream was closed or errored - stop reading silently.
				// This is expected when we force-close the stream after the grace period.
			} finally {
				if ( this.pending.length() > 0 ) {
					emit();
				}
			}
		}

		/**
		 * Handles one character, treating \n, \r and \r\n as line terminators.
		 *
		 * @param c The character read
		 */
		private void accept( char c ) {
			if ( this.skipLineFeed ) {
				this.skipLineFeed = false;
				if ( c == '\n' ) {
					return;
				}
			}
			if ( c == '\n' ) {
				emit();
			} else if ( c == '\r' ) {
				this.skipLineFeed = true;
				emit();
			} else {
				this.pending.append( c );
			}
		}

		/**
		 * Completes the buffered line: appends it to the capture and queues it for delivery.
		 */
		private void emit() {
			String line = this.pending.toString();
			this.pending.setLength( 0 );
			synchronized ( this.capture ) {
				if ( this.capture.length() > 0 ) {
					this.capture.append( "\n" );
				}
				this.capture.append( line );
			}
			if ( this.queue != null ) {
				this.queue.add( new OutputLine( this.stream, line ) );
			}
		}
	}

}
