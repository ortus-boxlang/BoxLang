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
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.logging.LoggingConfigurator;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A BoxLang interceptor that provides logging capabilities
 */
public class Logging extends BaseInterceptor {

	public static final String							DEFAULT_LOG_LEVEL		= "Information";
	public static final String							DEFAULT_LOG_TYPE		= "Application";
	public static final String							DEFAULT_LOG_CATEGORY	= "BoxRuntime";

	/**
	 * The directory where logs are stored
	 * This comes from the configuration in the runtime.
	 */
	private final String								logsDirectory;

	/**
	 * A map of appenders
	 */
	private Map<String, FileAppender<ILoggingEvent>>	appendersMap			= new ConcurrentHashMap<>();

	/**
	 * Logging Levels
	 */
	private static final String							LEVEL_TRACE				= "trace";
	private static final String							LEVEL_DEBUG				= "debug";
	private static final String							LEVEL_INFO				= "info";
	private static final String							LEVEL_WARN				= "warn";
	private static final String							LEVEL_ERROR				= "error";

	/**
	 * An Unmodifiable map of logging levels.
	 */
	private static final Map<Key, String>				levelMap				= Map.of(
	    Key.of( "Trace" ), LEVEL_TRACE,
	    Key.of( "Debug" ), LEVEL_DEBUG,
	    Key.of( "Debugging" ), LEVEL_DEBUG,
	    Key.of( "Info" ), LEVEL_INFO,
	    Key.of( "Information" ), LEVEL_INFO,
	    Key.of( "Warning" ), LEVEL_WARN,
	    Key.of( "Warn" ), LEVEL_WARN,
	    Key.of( "Error" ), LEVEL_ERROR,
	    Key.of( "Fatal" ), LEVEL_ERROR
	);

	/**
	 * Constructor
	 *
	 * @param instance The BoxRuntime instance
	 */
	public Logging( BoxRuntime instance ) {
		this.logsDirectory = instance.getConfiguration().logsDirectory;
	}

	/**
	 * Logs a message
	 *
	 * @param data The data to be passed to the interceptor
	 */
	@InterceptionPoint
	public void logMessage( IStruct data ) {
		String	logText		= data.getAsString( Key.text );
		String	file		= data.getAsString( Key.file );
		// The application name is the category, or if not provided, the default
		String	logCategory	= ( String ) data.getOrDefault( Key.application, DEFAULT_LOG_CATEGORY );
		String	logLevel	= data.getAsString( Key.level );
		// named argument for tags bx:log and function writeLog
		String	logType		= data.getAsString( Key.type );

		// Default the category to BoxRuntime if it is empty
		if ( logCategory.isEmpty() ) {
			logCategory = DEFAULT_LOG_CATEGORY;
		}
		// Default the log level to Information if it is empty
		if ( logType != null && !logType.isEmpty() ) {
			logLevel = logType;
		}
		// Prep a default file location, if the file was ommitted
		if ( file == null ) {
			file = logCategory + ".log";
		}
		// If the file is an absolute path, use it, otherwise use the logs directory as the base
		String	filePath	= Path.of( file ).isAbsolute()
		    ? Path.of( file ).normalize().toString()
		    : Paths.get( logsDirectory, "/", file ).normalize().toString();

		// Validate the log level
		Key		levelKey	= Key.of( logLevel );
		if ( !levelMap.containsKey( levelKey ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "[%s] is not a valid logging level.",
			        logLevel
			    )
			);
		}

		try {
			// Build the logger context or get it if it exists
			LoggerContext	logContext	= getOrBuildLoggerContext();
			// Now that we have a context
			// A logger is based on the {logCategory}:{filePath-hash} so we can have multiple loggers
			// for the same file, but different categories
			final Logger	logger		= logContext.getLogger( logCategory + ":" + FilenameUtils.getBaseName( filePath ) );
			logger.setLevel( Level.TRACE );
			// Create or compute the file appender requested
			// This provides locking also and caching so we don't have to keep creating them
			// Shutdown will stop the appenders
			this.appendersMap.computeIfAbsent( filePath.toLowerCase(), key -> {
				var appender = new FileAppender<ILoggingEvent>();
				appender.setFile( filePath );
				appender.setEncoder( getRuntime().getLoggingConfigurator().encoder );
				appender.setContext( logContext );
				appender.start();
				if ( !logger.isAttached( appender ) ) {
					logger.addAppender( appender );
				}
				return appender;
			} );

			// Log according to the level
			switch ( levelMap.get( levelKey ) ) {
				case LEVEL_TRACE -> logger.trace( logText );
				case LEVEL_DEBUG -> logger.debug( logText );
				case LEVEL_INFO -> {
					logContext.getLogger( Logger.ROOT_LOGGER_NAME ).info( logText );
					logger.info( logText );
				}
				case LEVEL_WARN -> logger.warn( logText );
				case LEVEL_ERROR -> logger.error( logText );
				default -> throw new BoxRuntimeException(
				    String.format(
				        "[%s] is not a valid logging level.",
				        logLevel
				    )
				);
			}

		} catch ( Exception e ) {
			throw new BoxRuntimeException( "An error occurred while attempting to log the message", e );
		}

	}

	/**
	 * Gets the logger context or builds one if it doesn't exist (Usually in a servlet context)
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
		// iterate over the appenders and call stop
		this.appendersMap.values().forEach( FileAppender::stop );
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
