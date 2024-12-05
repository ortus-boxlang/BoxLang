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
import ch.qos.logback.classic.encoder.JsonEncoder;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapterSimple;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.LoggerConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This service allows BoxLang to leverage logging facilities and interact with the logging system: LogBack: https://logback.qos.ch/manual/index.html
 * <p>
 * It also manages all custom logging events, appenders and loggers.
 * <p>
 * It's not a true BoxLang service, due to the chicken and egg problem of logging being needed before the runtime starts.
 * <p>
 * The {@link #configureBasic(Boolean)} method is called by the runtime to setup the basic logging system first, then
 * once the runtime is online and has read the configuration file (boxlang.json), it can reconfigure the logging system
 * via the {@link #reconfigure()} method.
 * <p>
 * Please note that in BoxLang you can use the following arguments for logging via the {@link #logMessage(String, String, String, String)} method:
 * <ul>
 * <li><strong>message</strong> - The message to send for logging or a lambda that produces the message</li>
 * <li><strong>type</strong> - The logging level type (error, info, warn, debug, trace)</li>
 * <li><strong>applicationName</strong> - The name of the BoxLang application (if any)</li>
 * <li><strong>logger</strong> - The named logger to emit to. Example: "scheduler, application, orm, etc"</li>
 * </ul>
 * <p>
 * If the named logger does not exist or it's an absolute path, then the logger will be registered as a new logger, with the name of the file as the category.
 */
public class LoggingService {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	public static final String						DEFAULT_LOG_LEVEL	= "info";
	public static final String						DEFAULT_LOG_TYPE	= "Application";
	public static final String						DEFAULT_LOG_FILE	= "runtime.log";
	public static final String						DEFAULT_APPLICATION	= "no-application";
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
	 * The default encoder for the runtime: We can use either a text or JSON encoder
	 */
	private EncoderBase<ILoggingEvent>				defaultEncoder;

	/**
	 * A map of registered appenders
	 * We lazy-load all appenders and cache them here
	 */
	private Map<String, Appender<ILoggingEvent>>	appendersMap		= new ConcurrentHashMap<>();

	/**
	 * A map of registered loggers we can use in the runtime
	 * We lazy-load and cache them here
	 * We use a struct so they are case-insensitive
	 */
	private IStruct									loggersMap			= new Struct();

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
	 * Get the runtime's log directory as per the configuration
	 */
	public String getLogsDirectory() {
		return this.runtime.getConfiguration().logging.logsDirectory;
	}

	/**
	 * Set the root logger for the runtime
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
	public EncoderBase<ILoggingEvent> getDefaultEncoder() {
		return this.defaultEncoder;
	}

	/**
	 * Store the BoxLang pattern encoder
	 *
	 * @param encoder The encoder to store
	 *
	 * @return LoggingService
	 */
	public LoggingService setDefaultEncoder( EncoderBase<ILoggingEvent> encoder ) {
		this.defaultEncoder = encoder;
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
	 * Configuration Methods
	 * --------------------------------------------------------------------------
	 * The runtime is configured initially with a basic logging system, then reconfigured once the configuration file is read.
	 */

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
		this.loggerContext.setMDCAdapter( new LogbackMDCAdapterSimple() );

		// Name it
		this.loggerContext.setName( CONTEXT_NAME );

		// Setup the runtime encoder with the BoxLang format
		PatternLayoutEncoder oEncoder = new PatternLayoutEncoder();
		oEncoder.setContext( loggerContext );
		oEncoder.setPattern( LOG_FORMAT );
		oEncoder.start();
		setDefaultEncoder( oEncoder );

		// Configure a basic Console Appender
		// See: https://logback.qos.ch/manual/appenders.html
		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
		appender.setContext( this.loggerContext );
		appender.setEncoder( oEncoder );
		appender.start();

		// Configure the Root Logger
		this.rootLogger = loggerContext.getLogger( Logger.ROOT_LOGGER_NAME );
		this.rootLogger.setLevel( Boolean.TRUE.equals( debugMode ) ? Level.DEBUG : Level.WARN );
		this.rootLogger.addAppender( appender );

		return instance;
	}

	/**
	 * This method is called by the runtime to reconfigure the logging system
	 * once the configuration file has been read.
	 * <p>
	 * This could change logging levels, add new appenders, etc.
	 */
	public LoggingService reconfigure() {
		// Reconfigure Root Logger from the configuration file
		Level rootLevel = Level.toLevel( this.runtime.getConfiguration().logging.rootLevel.getName() );
		this.rootLogger.setLevel( rootLevel );

		// Change encoder or not to JSON, default is text
		if ( this.runtime.getConfiguration().logging.defaultEncoder.equals( Key.json ) ) {
			setDefaultEncoder( buildJsonEncoder() );
		}

		// Debugging
		if ( this.runtime.inDebugMode() ) {
			StatusPrinter.print( this.loggerContext );
		}

		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Logging Methods
	 * --------------------------------------------------------------------------
	 * Basic logging methods that all BoxLang applications can use
	 */

	/**
	 * Log a message into the default log file and the default log type with no application name
	 *
	 * @param message The message to log
	 *
	 * @return The logging service
	 */
	public LoggingService logMessage( String message ) {
		return logMessage( message, DEFAULT_LOG_LEVEL, "no-application", DEFAULT_LOG_FILE );
	}

	/**
	 * Log a message into the default log file and a custom log type with no application name
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
	 * Log a message into the default log file and a custom log type and a custom application name
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
	 * Log a message with specific arguments
	 *
	 * @param message         The message to log
	 * @param type            The type of log message (fatal, error, info, warn, debug, trace)
	 * @param applicationName The name of the application requesting the log message
	 * @param logger          The logger destination. It can be a named logger or an absolute path
	 *
	 * @return The logging service
	 *
	 * @throws IllegalArgumentException If the log level is not valid
	 */
	public LoggingService logMessage(
	    String message,
	    String type,
	    String applicationName,
	    String logger ) {

		// Default level to info if no log level is passed
		if ( type == null || type.isEmpty() ) {
			type = DEFAULT_LOG_LEVEL;
		}
		// Get and Validate the log level
		Key targetLogLevel = LogLevel.valueOf( type, false );

		// If it is empty, then use the default of no-application
		if ( applicationName == null || applicationName.isEmpty() ) {
			applicationName = DEFAULT_APPLICATION;
		}
		// Include the application name in the message
		message = String.format( "[%s] %s", applicationName, message );

		// If no file or logger is passed, then use the default logger: runtime.log
		if ( logger.isEmpty() ) {
			logger = DEFAULT_LOG_FILE;
		}

		// Compute and get the logger
		Logger oLogger = getLogger( logger );

		// Log according to the level
		switch ( targetLogLevel.getNameNoCase() ) {
			// No fatal in SL4J
			case "FATAL" -> oLogger.error( message );
			case "ERROR" -> oLogger.error( message );
			case "WARN" -> oLogger.warn( message );
			case "INFO" -> oLogger.info( message );
			case "DEBUG" -> oLogger.debug( message );
			case "TRACE" -> oLogger.trace( message );
			default -> oLogger.info( message );
		}

		return instance;
	}

	/**
	 * Get a logger by registered name.
	 * If the logger doesn't exist, it will auto-register it and load it
	 * using the name as the file name in the logs directory.
	 *
	 * @param logger The name of the logger to retrieve.
	 *
	 * @return The logger requested
	 */
	public Logger getLogger( String logger ) {
		// The incoming logger can be:
		// 1. A named logger: "scheduler", "application", "orm", etc
		// 2. A relative path: "scheduler.log", "application.log", "orm.log"
		// 3. An absolute path: "/var/log/boxlang/scheduler.log"

		// Make sure it ends in .log
		if ( !logger.endsWith( ".log" ) ) {
			logger = logger + ".log";
		}

		// If the file is an absolute path, use it, otherwise use the logs directory as the base
		String	loggerFilePath	= Path.of( logger ).normalize().isAbsolute()
		    ? Path.of( logger ).normalize().toString()
		    : Paths.get( getLogsDirectory(), logger.toLowerCase() ).normalize().toString();
		Key		loggerKey		= Key.of( FilenameUtils.getBaseName( loggerFilePath.toLowerCase() ) );

		// Compute it or return it
		return ( Logger ) this.loggersMap.computeIfAbsent( Key.of( loggerFilePath ), key -> createLogger( loggerKey, loggerFilePath ) );
	}

	/**
	 * Verify if a logger with the specified name exists
	 *
	 * @param loggerName The name of the logger to verify
	 */
	public boolean hasLogger( Key loggerName ) {
		return this.loggersMap.containsKey( loggerName );
	}

	/**
	 * Tell me how many loggers have been registered
	 *
	 * @return The number of loggers registered
	 */
	public int getLoggersCount() {
		return this.loggersMap.size();
	}

	/**
	 * Get a list of all the registered loggers so far in the runtime
	 *
	 * @return The list of loggers
	 */
	public List<String> getLoggersList() {
		return this.loggersMap.getKeysAsStrings();
	}

	/**
	 * Get a list of all the registered loggers so far in the runtime
	 *
	 * @return The list of loggers
	 */
	public List<Key> getLoggersKeys() {
		return this.loggersMap.getKeys();
	}

	/**
	 * Remove a logger by name
	 *
	 * @param loggerName The name of the logger to remove
	 *
	 * @return True if the logger was removed, false otherwise
	 */
	public boolean removeLogger( Key loggerName ) {
		Logger oLogger = ( Logger ) this.loggersMap.remove( loggerName );

		if ( oLogger != null ) {
			oLogger.detachAndStopAllAppenders();
			return true;
		}

		return false;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Appender Methods
	 * --------------------------------------------------------------------------
	 * We lazy-load and dynamically create appenders as needed
	 * These methods manage the appenders and their lifecycles
	 */

	/**
	 * Get the requested file appender according to log location
	 *
	 * @param filePath     The file path to get the appender for
	 * @param logContext   The logger context requested for the appender
	 * @param loggerConfig The logger configuration
	 *
	 * @return The file appender, computed or from cache
	 */
	public FileAppender<ILoggingEvent> getOrBuildAppender( String filePath, LoggerContext logContext, LoggerConfig loggerConfig ) {
		return ( FileAppender<ILoggingEvent> ) this.appendersMap.computeIfAbsent( filePath.toLowerCase(), key -> {
			var		appender		= new RollingFileAppender<ILoggingEvent>();
			String	fileName		= FilenameUtils.getBaseName( filePath );
			String	enclosingFolder	= FilenameUtils.getFullPathNoEndSeparator( filePath );

			// Set basics of appender
			appender.setName( fileName );
			appender.setFile( filePath );
			appender.setContext( logContext );
			appender.setEncoder( getDefaultEncoder() );
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
			if ( this.runtime.inDebugMode() ) {
				StatusPrinter.print( logContext );
			}

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

	private JsonEncoder buildJsonEncoder() {
		JsonEncoder targetEncoder = new JsonEncoder();
		targetEncoder.setContext( this.loggerContext );
		targetEncoder.start();
		return targetEncoder;
	}

	/**
	 * Build a logger with the specified name and file path.
	 * This will also look into the configuration file for the logger level and additivity.
	 *
	 * @param loggerKey      The key of the logger to build
	 * @param loggerFilePath The file path to log to
	 *
	 * @return The logger requested
	 */
	private Logger createLogger( Key loggerKey, String loggerFilePath ) {
		LoggerContext	targetContext	= getLoggerContext();
		Logger			oLogger			= targetContext.getLogger( loggerKey.getNameNoCase() );

		// Check if we have the logger configuration or else build a vanilla one
		LoggerConfig	loggerConfig	= ( LoggerConfig ) this.runtime
		    .getConfiguration().logging.loggers
		    .computeIfAbsent( loggerKey, key -> new LoggerConfig( key.getNameNoCase(), this.runtime.getConfiguration().logging ) );
		Level			configLevel		= Level.toLevel( LogLevel.valueOf( loggerConfig.level.getName(), false ).getName() );

		// Seed the properties
		oLogger.setLevel( configLevel );
		oLogger.setAdditive( loggerConfig.additive );
		oLogger.addAppender( getOrBuildAppender( loggerFilePath, targetContext, loggerConfig ) );
		return oLogger;
	}

}
