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
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;

/**
 * This service allows BoxLang to leverage logging facilities and interact with the logging system.
 * <p>
 * It also manages all custom logging events, appenders and loggers.
 * <p>
 * It's not a true BoxLang service, due to the chicken and egg problem of logging being needed before the runtime starts.
 * <p>
 * The <code>configureBasic()</code> method is called by the runtime to setup the basic logging system.
 * <p>
 * Once the runtime is online and has read the configuration file (boxlang.json), it can reconfigure the logging system.
 * This could change logging levels, add new appenders, etc.
 */
public class LoggingService {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	public static final String						DEFAULT_LOG_LEVEL	= "info";
	public static final String						DEFAULT_LOG_TYPE	= "Application";
	public static final String						DEFAULT_LOG_FILE	= "boxruntime.log";
	public static final String						CONTEXT_NAME		= "BoxLang";

	/**
	 * The log format for the BoxLang runtime
	 *
	 * @see https://logback.qos.ch/manual/layouts.html#conversionWord
	 */
	public static final String						LOG_FORMAT			= "[%date{STRICT}] [%thread] [%-5level] [%logger{0}] %message %ex%n";

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton
	 */
	private static LoggingService					instance;

	/**
	 * The linked runtime
	 */
	private BoxRuntime								runtime;

	/**
	 * The root logger for the runtime
	 */
	private Logger									rootLogger;

	/**
	 * The logger context for the runtime
	 */
	private LoggerContext							loggerContext;

	/**
	 * The BoxLang pattern encoder we use for logging
	 */
	private PatternLayoutEncoder					encoder;

	/**
	 * A map of registered appenders
	 */
	private Map<String, Appender<ILoggingEvent>>	appendersMap		= new ConcurrentHashMap<>();

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

	/**
	 * This configures LogBack with a basic configuration, so we can use logging before we actually read
	 * the configuration file.
	 * <p>
	 * Once the configuration file is read, we can reconfigure the logging system.
	 *
	 * @param debugMode The flag the runtime was started with
	 */
	public LoggingService configureBasic( Boolean debugMode ) {
		ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

		// Are we in Servlet mode or not? If we are not, then we have to build the logger context
		if ( loggerFactory instanceof LoggerContext ) {
			this.loggerContext = ( LoggerContext ) loggerFactory;
		} else {
			this.loggerContext = new LoggerContext();
		}

		this.loggerContext.reset();

		// Name it
		this.loggerContext.setName( CONTEXT_NAME );

		// Setup the runtime encoder with the BoxLang format
		this.encoder = new PatternLayoutEncoder();
		this.encoder.setContext( loggerContext );
		this.encoder.setPattern( LOG_FORMAT );
		this.encoder.start();

		// Configure a basic Console Appender
		// See: https://logback.qos.ch/manual/appenders.html
		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
		appender.setContext( this.loggerContext );
		appender.setEncoder( this.encoder );
		appender.start();

		// Configure the Root Logger
		this.rootLogger = loggerContext.getLogger( Logger.ROOT_LOGGER_NAME );
		this.rootLogger.setLevel( Boolean.TRUE.equals( debugMode ) ? Level.DEBUG : Level.WARN );
		this.rootLogger.addAppender( appender );

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
	 * Get the logger context
	 *
	 * @return The logger context
	 */
	public LoggerContext getLoggerContext() {
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
	 * This method is called by the runtime to reconfigure the logging system
	 * once the configuration file has been read.
	 * <p>
	 * This could change logging levels, add new appenders, etc.
	 */
	public LoggingService reconfigure() {
		// Reconfigure Root Logger
		Level rootLevel = Level.toLevel( this.runtime.getConfiguration().logging.rootLevel );
		this.rootLogger.setLevel( rootLevel );

		return instance;
	}

	/**
	 * Log a message into the default log file and the default log type
	 *
	 * @param message The message to log
	 *
	 * @return The logging service
	 */
	public LoggingService logMessage( String message ) {
		return logMessage( message, DEFAULT_LOG_LEVEL, "no-application", DEFAULT_LOG_FILE );
	}

	/**
	 * Log a message into the default log file and a custom log type
	 *
	 * @param message The message to log
	 * @param type    The type of log message (fatal, error, info, warn, debug, trace)
	 *
	 * @return The logging service
	 */
	public LoggingService logMessage( String message, String type ) {
		return logMessage( message, type, "no-application", DEFAULT_LOG_FILE );
	}

	/**
	 * Log a message into the default log file and a custom log type
	 *
	 * @param message         The message to log
	 * @param type            The type of log message (fatal, error, info, warn, debug, trace)
	 * @param applicationName The name of the application requesting the log message
	 *
	 * @return The logging service
	 */
	public LoggingService logMessage( String message, String type, String applicationName ) {
		return logMessage( message, type, applicationName, DEFAULT_LOG_FILE );
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
		if ( type == null || type.isEmpty() ) {
			type = DEFAULT_LOG_LEVEL;
		}
		// Get and Validate log level
		Key logLevel = LogLevel.valueOf( type, false );

		// The application name is used as the logging category.
		// If it is empty, then use the default category
		if ( applicationName == null || applicationName.isEmpty() ) {
			applicationName = "no-application";
		}
		// Include the application name in the message
		message = String.format( "[%s] %s", applicationName, message );

		// If no file or log is passed, then use the default log file: boxruntime.log
		if ( logFile.isEmpty() ) {
			logFile = DEFAULT_LOG_FILE;
		}

		// Verify the log file ends in `.log` and if not, append it
		if ( !logFile.toLowerCase().endsWith( ".log" ) ) {
			logFile += ".log";
		}

		// If the file is an absolute path, use it, otherwise use the logs directory as the base
		String			filePath		= Path.of( logFile ).isAbsolute()
		    ? Path.of( logFile ).normalize().toString()
		    : Paths.get( getLogsDirectory(), "/", logFile ).normalize().toString();

		// A BoxLang logger is based on the {fileName} as the category.
		LoggerContext	targetContext	= getLoggerContext();
		Logger			logger			= targetContext.getLogger( FilenameUtils.getBaseName( filePath ).toLowerCase() );
		logger.setLevel( Level.TRACE );
		FileAppender<ILoggingEvent> appender = getOrBuildAppender( filePath, targetContext );

		// Create or compute the file appender requested
		// This provides locking also and caching so we don't have to keep creating them
		// Shutdown will stop the appenders
		if ( !logger.isAttached( appender ) ) {
			logger.addAppender( appender );
		}

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
		return this.runtime.getConfiguration().logging.logsDirectory;
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
		return ( FileAppender<ILoggingEvent> ) this.appendersMap.computeIfAbsent( filePath.toLowerCase(), key -> {
			var		appender		= new RollingFileAppender<ILoggingEvent>();
			String	fileName		= FilenameUtils.getBaseName( filePath );
			String	enclosingFolder	= FilenameUtils.getFullPathNoEndSeparator( filePath );

			// Set basics of appender
			appender.setName( fileName );
			appender.setFile( filePath );
			appender.setContext( logContext );
			appender.setEncoder( getEncoder() );
			appender.setAppend( true );
			appender.setImmediateFlush( true );
			// This is commented as rolling with compression does not allow prudent handling
			// appender.setPrudent( true );

			// Time-based rolling policy with file size constraint
			SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy = new SizeAndTimeBasedRollingPolicy<>();
			policy.setContext( logContext );
			policy.setParent( appender );
			policy.setFileNamePattern( enclosingFolder + "/archives/" + fileName + ".%d{yyyy-MM-dd}.%i.log.zip" );
			policy.setMaxHistory(
			    this.runtime.getConfiguration().logging.maxLogDays
			);
			policy.setMaxFileSize( FileSize.valueOf(
			    this.runtime.getConfiguration().logging.maxFileSize
			) ); // Maximum file size for each log
			policy.setTotalSizeCap( FileSize.valueOf(
			    this.runtime.getConfiguration().logging.totalCapSize
			) ); // Max total cap size
			policy.start();

			// Configure it
			appender.setRollingPolicy( policy );
			appender.start();

			// Uncomment to verify issues
			// StatusPrinter.print( logContext );

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
		this.appendersMap.values().forEach( Appender::stop );
		return instance;
	}

	/**
	 * Shutdown the logging service
	 */
	public LoggingService shutdown() {
		// Shutdown all the appenders
		shutdownAppenders();
		getLoggerContext().stop();
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

}
