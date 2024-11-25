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
package ortus.boxlang.runtime.logging;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;

/**
 * This service allows BoxLang to leverage logging facilities and interact with the logging system.
 * <p>
 * It also manages all custom logging events, appenders and loggers.
 * <p>
 * It's not a true BoxLang service, due to the chicken and egg problem of logging being needed before the runtime starts.
 */
public class LoggingService {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	public static final String							DEFAULT_LOG_LEVEL		= "info";
	public static final String							DEFAULT_LOG_TYPE		= "Application";
	public static final String							DEFAULT_LOG_CATEGORY	= "boxruntime";

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton
	 */
	private static LoggingService						instance;

	/**
	 * The linked runtime
	 */
	private BoxRuntime									runtime;

	/**
	 * The logging configuration for the runtime
	 * This is set by the first call to `getLogger()` which happens in the
	 *
	 * <pre>
	 * startup()
	 * </pre>
	 */
	private LoggingConfigurator							loggingConfigurator;

	/**
	 * The root logger for the runtime
	 */
	private Logger										rootLogger;

	/**
	 * The logger context for the runtime
	 */
	private LoggerContext								loggerContext;

	/**
	 * The BoxLang pattern encoder we use for logging
	 */
	private PatternLayoutEncoder						encoder;

	/**
	 * A map of appenders
	 */
	private Map<String, FileAppender<ILoggingEvent>>	appendersMap			= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	private LoggingService( BoxRuntime runtime ) {
		// Private constructor to prevent instantiation
		this.runtime = runtime;
	}

	/**
	 * Get the singleton instance of the LoggingService and initializing it with the runtime
	 * This is called by the Runtime only!
	 *
	 * @return The LoggingService instance
	 */
	public static LoggingService getInstance( BoxRuntime runtime ) {
		if ( instance == null ) {
			synchronized ( LoggingService.class ) {
				if ( instance == null ) {
					instance = new LoggingService( runtime );
				}
			}
		}
		return instance;
	}

	public LoggingService loadConfiguration() {
		LoggerContext context = ( LoggerContext ) LoggerFactory.getILoggerFactory();
		return instance;
	}

	/**
	 * Get the singleton instance of the LoggingService
	 *
	 * @throws IllegalStateException If the LoggingService has not been initialized yet
	 *
	 * @return The LoggingService instance
	 */
	public static LoggingService getInstance() {
		// Check if null and throw an exception
		if ( instance == null ) {
			throw new IllegalStateException( "LoggingService has not been initialized yet" );
		}
		return instance;
	}

	/**
	 * Get the configured Logging Configurator for the runtime
	 *
	 * @return The Configurator
	 */
	public LoggingConfigurator getLoggingConfigurator() {
		return this.loggingConfigurator;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the root logger
	 *
	 * @return The root logger
	 */
	public Logger getRootLogger() {
		return this.rootLogger;
	}

	/**
	 * Set the root logger
	 *
	 * @param logger The logger to set
	 *
	 * @return The logging service
	 */
	public LoggingService setRootLogger( Logger logger ) {
		this.rootLogger = logger;
		return instance;
	}

	/**
	 * Get the BoxLang pattern encoder
	 *
	 * @return The encoder
	 */
	public PatternLayoutEncoder getEncoder() {
		return this.encoder;
	}

	/**
	 * Store the BoxLang pattern encoder
	 *
	 * @param encoder The encoder to store
	 *
	 * @return LoggingService
	 */
	public LoggingService setEncoder( PatternLayoutEncoder encoder ) {
		this.encoder = encoder;
		return instance;
	}

	/**
	 * Set the Logging Configurator
	 *
	 * @param configurator The configurator to set
	 *
	 * @return The Runtime
	 */
	public LoggingService setLoggingConfigurator( LoggingConfigurator configurator ) {
		this.loggingConfigurator = configurator;
		return instance;
	}

	/**
	 * Get the logger context
	 *
	 * @return The logger context
	 */
	public LoggerContext getLoggerContext() {
		if ( this.loggerContext == null ) {
			this.loggerContext = getOrBuildLoggerContext();
		}
		return this.loggerContext;
	}

	/**
	 * Set the logger context
	 *
	 * @param loggerContext The logger context to set
	 *
	 * @return The logging service
	 */
	public LoggingService setLoggerContext( LoggerContext loggerContext ) {
		this.loggerContext = loggerContext;
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Enable debug mode for the runtime's root logger or not.
	 *
	 * This is usually a convenience method for the runtime to enable or disable
	 * debug mode
	 * via configuration overrides
	 *
	 * @param debugMode True to enable debug mode, false to disable
	 */
	public LoggingService reconfigureDebugMode( Boolean debugMode ) {
		this.rootLogger.setLevel( Boolean.TRUE.equals( debugMode ) ? Level.DEBUG : Level.INFO );
		return instance;
	}

	/**
	 * Log a message into a specific log file and a specific type
	 *
	 * @param message         The message to log
	 * @param type            The type of log message (fatal, error, info, warn, debug, trace)
	 * @param applicationName The name of the application requesting the log message
	 * @param logFile         The destination file to log to in the logs directory. If empty, the default log file is used
	 *
	 * @return The logging service
	 */
	public LoggingService logMessage(
	    String message,
	    String type,
	    String applicationName,
	    String logFile ) {

		// Default to info if no log level is passed
		if ( type.isEmpty() ) {
			type = DEFAULT_LOG_LEVEL;
		}
		// Get and Validate log level
		Key logLevel = LogLevel.valueOf( type, false );

		// The application name is used as the logging category.
		// If it is empty, then use the default category
		if ( applicationName.isEmpty() ) {
			applicationName = DEFAULT_LOG_CATEGORY;
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
		String			filePath	= Path.of( logFile ).isAbsolute()
		    ? Path.of( logFile ).normalize().toString()
		    : Paths.get( getLogsDirectory(), "/", logFile ).normalize().toString();

		// Build the logger context or get it if it exists
		// Now that we have a context
		// A logger is based on the {fileName} as the category. This allows multiple loggers
		// for the same file, but different categories
		final Logger	logger		= getOrBuildLoggerContext().getLogger( FilenameUtils.getBaseName( filePath ).toLowerCase() );
		logger.setLevel( Level.TRACE );

		// Create or compute the file appender requested
		// This provides locking also and caching so we don't have to keep creating them
		// Shutdown will stop the appenders
		logger.addAppender( getOrBuildAppender( filePath, getLoggerContext() ) );

		// Log according to the level
		switch ( logLevel.getNameNoCase() ) {
			// No fatal in SL4J
			case "FATAL" -> logger.error( message );
			case "ERROR" -> logger.error( message );
			case "WARN" -> logger.warn( message );
			case "INFO" -> logger.info( message );
			case "DEBUG" -> logger.debug( message );
			case "TRACE" -> logger.trace( message );
			default -> logger.info( message );
		}

		return instance;
	}

	/**
	 * Get the runtime's log directory as per the configuration
	 */
	public String getLogsDirectory() {
		return this.runtime.getConfiguration().logsDirectory;
	}

	/**
	 * Get the requested file appender according to log location
	 *
	 * @param filePath   The file path to get the appender for
	 * @param logContext The logger context requested for the appender
	 * @param logger     The logger to add the appender to
	 *
	 * @return The file appender, computed or from cache
	 */
	public FileAppender<ILoggingEvent> getOrBuildAppender( String filePath, LoggerContext logContext ) {
		return this.appendersMap.computeIfAbsent( filePath.toLowerCase(), key -> {
			var appender = new FileAppender<ILoggingEvent>();
			appender.setFile( filePath );
			appender.setEncoder( getEncoder() );
			appender.setContext( logContext );
			appender.setAppend( true );
			appender.setImmediateFlush( true );
			appender.setPrudent( true );
			appender.start();
			return appender;
		} );
	}

	/**
	 * Verify if we have the passed in filePath appender
	 *
	 * @return True if the appender exists, false otherwise
	 */
	public boolean hasAppender( String filePath ) {
		return this.appendersMap.containsKey( filePath.toLowerCase() );
	}

	/**
	 * Remove the appender from the cache using the file path
	 *
	 * @param filePath The file path to remove the appender for
	 *
	 * @return True if the appender was removed, false otherwise
	 */
	public boolean removeAppender( String filePath ) {
		return this.appendersMap.remove( filePath.toLowerCase() ) != null;
	}

	/**
	 * Get a list of all registered file appenders
	 *
	 * @return The list of appenders
	 */
	public List<String> getAppendersList() {
		return new ArrayList<>( this.appendersMap.keySet() );
	}

	/**
	 * Shutdown all the appenders
	 *
	 * @return The logging service
	 */
	public LoggingService shutdownAppenders() {
		this.appendersMap.values().forEach( FileAppender::stop );
		return instance;
	}

	/**
	 * Shutdown the logging service
	 */
	public LoggingService shutdown() {
		// Shutdown all the appenders
		shutdownAppenders();
		this.loggerContext.stop();
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

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

		// Do we have a configurator?
		if ( this.loggingConfigurator == null ) {
			synchronized ( this ) {
				if ( this.loggingConfigurator == null ) {
					// In the servlet context we are seeing the configurator configure method is not being run automagically
					loggerFactory
					    .getLogger( getClass().getName() )
					    .warn( "Log context was null, attempting to configure the logger context manually" );

					logContext = new LoggerContext();
					logContext.start();
					var newLoggingConfig = new LoggingConfigurator();
					newLoggingConfig.configure( logContext );
					this.loggingConfigurator = newLoggingConfig;
				}
			}
		}

		return getLoggingConfigurator().getLoggerContext();
		// LoggingConfigurator configurator = ServiceLoader
		// .load( Configurator.class, BoxRuntime.class.getClassLoader() )
		// .stream()
		// .map( ServiceLoader.Provider::get )
		// .map( LoggingConfigurator.class::cast )
		// .findFirst()
		// .orElse( null );
		// logContext = configurator.getLoggerContext();
	}

}
