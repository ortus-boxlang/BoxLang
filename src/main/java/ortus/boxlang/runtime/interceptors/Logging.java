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

import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.logging.LoggingConfigurator;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A BoxLang interceptor that provides logging capabilities
 */
public class Logging extends BaseInterceptor {

	/**
	 * The directory where logs are stored
	 */
	private final String					logsDirectory;

	/**
	 * A map of appenders
	 */
	private Struct							appendersMap	= new Struct();

	/**
	 * The arguments for the logMessage method
	 */
	private Argument[]						logArguments	= new Argument[] {
	    new Argument( true, "string", Key.text ),
	    new Argument( false, "string", Key.file ),
	    new Argument( false, "string", Key.log, "Application" ),
	    new Argument( false, "string", Key.type, "Information" )
	};

	/**
	 * Logging Levels
	 */
	private static final String				LEVEL_TRACE		= "trace";
	private static final String				LEVEL_DEBUG		= "debug";
	private static final String				LEVEL_INFO		= "info";
	private static final String				LEVEL_WARN		= "warn";
	private static final String				LEVEL_ERROR		= "error";

	// An imutable map of logging levels
	private static final Map<Key, String>	levelMap		= Map.of(
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
		String	logCategory	= data.getAsString( Key.log );
		String	logLevel	= data.getAsString( Key.level );
		// named argument for tags bx:log and function writeLog
		String	logType		= data.getAsString( Key.type );
		if ( logCategory == null ) {
			logCategory = "BoxRuntime";
		}
		if ( logType != null ) {
			logLevel = logType;
		}

		Key levelKey = Key.of( logLevel );

		if ( !levelMap.containsKey( levelKey ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "[%s] is not a valid logging level.",
			        logLevel
			    )
			);
		}

		Logger						logger			= ( ch.qos.logback.classic.Logger ) LoggerFactory.getLogger( logCategory );
		FileAppender<ILoggingEvent>	fileAppender	= null;
		try {
			if ( file == null ) {
				file = logCategory + ".log";
			}
			String			filePath	= Paths.get( logsDirectory, "/", file ).normalize().toString();
			LoggerContext	logContext	= ( LoggerContext ) LoggerFactory.getILoggerFactory();
			fileAppender = new FileAppender<>();
			fileAppender.setFile( filePath );
			fileAppender.setEncoder( LoggingConfigurator.encoder );
			fileAppender.setContext( logContext );
			fileAppender.start();

			logger.addAppender( fileAppender );
			logger.setLevel( Level.ALL );
			logger.setAdditive( false );

			switch ( levelMap.get( levelKey ) ) {
				case LEVEL_TRACE : {
					logger.trace( logText );
					break;
				}
				case LEVEL_DEBUG : {
					logger.debug( logText );
					break;
				}
				default :
				case LEVEL_INFO : {
					logger.info( logText );
					break;
				}
				case LEVEL_WARN : {
					logger.warn( logText );
					break;
				}
				case LEVEL_ERROR : {
					logger.error( logText );
					break;
				}
			}

		} catch ( Exception e ) {
			throw new BoxRuntimeException( "An error occurred while attempting to log the message", e );
		} finally {
			if ( fileAppender != null ) {
				fileAppender.stop();
			}
		}

	}

	/**
	 * Runtime shutdown interception
	 */
	@SuppressWarnings( "unchecked" )
	@InterceptionPoint
	public void onRuntimeShutdown() {
		this.appendersMap.keySet().stream().forEach( key -> ( ( FileAppender<ILoggingEvent> ) this.appendersMap.get( key ) ).stop() );
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
