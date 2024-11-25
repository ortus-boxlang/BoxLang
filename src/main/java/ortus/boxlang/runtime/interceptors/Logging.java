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
package ortus.boxlang.runtime.interceptors;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.logging.LogLevel;
import ortus.boxlang.runtime.logging.LoggingConfigurator;
import ortus.boxlang.runtime.logging.LoggingService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A BoxLang interceptor that provides logging capabilities
 */
public class Logging extends BaseInterceptor {

	private LoggingService	loggingService;
	private BoxRuntime		runtime;

	/**
	 * Constructor
	 *
	 * @param instance The BoxRuntime instance
	 */
	public Logging( BoxRuntime instance ) {
		this.runtime		= instance;
		this.loggingService	= instance.getLoggingService();
	}

	/**
	 * Logs a message to a file or location
	 * <p>
	 * Data should contain the following keys:
	 * <ul>
	 * <li>applicationName: The name of the application requesting the log messasge. Can be empty</li>
	 * <li>text: The text of the log message</li>
	 * <li>type: The severity log level ( fatal, error, info, warn, debug, trace )</li>
	 * <li>file: The file to log to. If empty, the "log" key is used</li>
	 * </ul>
	 *
	 * <p>
	 * The <code>log</code> key is a shortcut to a specific log file. Available log files are: Application, Scheduler, etc.
	 * Which is dumb and should be moved to the CFML compatibility module. Leaving until we move it.
	 *
	 * @param data The data to be passed to the interceptor
	 *
	 * @throws IIllegalArgumentException If the log level is not valid
	 */
	@InterceptionPoint
	public void logMessage( IStruct data ) {
		// The incoming data
		String	logCategory	= ( String ) data.getOrDefault( Key.applicationName, LoggingService.DEFAULT_LOG_CATEGORY );
		String	logText		= ( String ) data.getOrDefault( Key.text, "" );
		String	logType		= ( String ) data.getOrDefault( Key.type, LoggingService.DEFAULT_LOG_LEVEL );
		String	logFile		= ( String ) data.getOrDefault( Key.file, "" );
		String	compatLog	= ( String ) data.getOrDefault( Key.log, "" );

		// If the logText is empty, then don't log anything
		if ( logText.isEmpty() ) {
			return;
		}

		// Default to info if no log level is passed
		if ( logType.isEmpty() ) {
			logType = LoggingService.DEFAULT_LOG_LEVEL;
		}
		// Get and Validate log level
		Key logLevel = LogLevel.valueOf( logType, false );

		// The application name is used as the logging category.
		// If it is empty, then use the default category
		if ( logCategory.isEmpty() ) {
			logCategory = LoggingService.DEFAULT_LOG_CATEGORY;
		}
		if ( logFile == null ) {
			logFile = "";
		}
		if ( compatLog == null ) {
			compatLog = "";
		}

		// COMPAT MODE: If we have an incoming `log` key, then we need to map it to a file.
		// This is a dumb feature and should be moved to the CFML compatibility module
		// As per the CFML docs, if the file is passed, then ignore this
		if ( logFile.isEmpty() && !compatLog.isEmpty() ) {
			logFile = compatLog.toLowerCase();
		}

		// If no file or log is passed, then use the default log file: boxruntime.log
		if ( logFile.isEmpty() ) {
			logFile = LoggingService.DEFAULT_LOG_CATEGORY + ".log";
		}

		// Verify the log file ends in `.log` and if not, append it
		if ( !logFile.toLowerCase().endsWith( ".log" ) ) {
			logFile += ".log";
		}

		// If the file is an absolute path, use it, otherwise use the logs directory as the base
		String filePath = Path.of( logFile ).isAbsolute()
		    ? Path.of( logFile ).normalize().toString()
		    : Paths.get( loggingService.getLogsDirectory(), "/", logFile ).normalize().toString();

		try {
			// Build the logger context or get it if it exists
			LoggerContext	logContext	= getOrBuildLoggerContext();
			// Now that we have a context
			// A logger is based on the {fileName} as the category. This allows multiple loggers
			// for the same file, but different categories
			final Logger	logger		= logContext.getLogger( FilenameUtils.getBaseName( filePath ).toLowerCase() );
			logger.setLevel( Level.TRACE );
			logger.setAdditive( true );

			// Create or compute the file appender requested
			// This provides locking also and caching so we don't have to keep creating them
			// Shutdown will stop the appenders
			logger.addAppender(
			    this.loggingService.getOrBuildAppender( filePath, logContext )
			);

			// Log according to the level
			switch ( logLevel.getNameNoCase() ) {
				// No fatal in SL4J
				case "FATAL" -> logger.error( logText );
				case "ERROR" -> logger.error( logText );
				case "WARN" -> logger.warn( logText );
				case "INFO" -> logger.info( logText );
				case "DEBUG" -> logger.debug( logText );
				case "TRACE" -> logger.trace( logText );
				default -> logger.info( logText );
			}

		} catch ( Exception e ) {
			throw new BoxRuntimeException( "An error occurred while attempting to log the message", e );
		}

	}

	/**
	 * Gets the logger context or builds one if it doesn't exist (Usually in a servlet context)
	 * TODO: Move to the service
	 *
	 * @return The logger context
	 */
	private LoggerContext getOrBuildLoggerContext() {
		LoggerContext				logContext		= null;
		org.slf4j.ILoggerFactory	loggerFactory	= LoggerFactory.getILoggerFactory();

		// If our core SLF4J logger factory is returning a logback instance use that
		if ( loggerFactory instanceof LoggerContext ) {
			return ( LoggerContext ) loggerFactory;
		}

		// Log the issue and try to get the context from the configurator
		loggerFactory
		    .getLogger( getClass().getName() )
		    .warn( "The LoggerFactory context is not an instance of Logback LoggerContext. Received class: {}", loggerFactory.getClass().getName() );

		// otherwise grab the context from the configurator
		LoggingConfigurator configurator = ServiceLoader
		    .load( Configurator.class, BoxRuntime.class.getClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .map( LoggingConfigurator.class::cast )
		    .findFirst()
		    .orElse( null );

		logContext = configurator.getLoggerContext();

		// In the servlet context we are seeing the configurator configure method is not being run automagically
		if ( logContext == null ) {
			logContext = new LoggerContext();
			logContext.start();
			configurator.configure( logContext );
		}

		return logContext;
	}

	/**
	 * Runtime shutdown interception
	 */
	@InterceptionPoint
	public void onRuntimeShutdown() {
		this.loggingService.shutdown();
	}

	/**
	 * Alternate signature for onRuntimeShutdown
	 *
	 * @param data The data to be passed to the interceptor
	 */
	public void onRuntimeShutdown( IStruct data ) {
		onRuntimeShutdown();
	}

}
