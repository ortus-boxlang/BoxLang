package ortus.boxlang.runtime.interceptors;

import java.nio.file.Paths;
import java.util.HashMap;

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

public class Logging extends BaseInterceptor {

	private final String				logsDirectory;

	private Struct						appendersMap	= new Struct();

	private Argument[]					logArguments	= new Argument[] {
	    new Argument( true, "string", Key.text ),
	    new Argument( false, "string", Key.file ),
	    new Argument( false, "string", Key.log, "Application" ),
	    new Argument( false, "string", Key.type, "Information" ),
	};

	private final String				LEVEL_TRACE		= "trace";
	private final String				LEVEL_DEBUG		= "debug";
	private final String				LEVEL_INFO		= "info";
	private final String				LEVEL_WARN		= "warn";
	private final String				LEVEL_ERROR		= "error";

	private final HashMap<Key, String>	levelMap		= new HashMap<Key, String>() {

															{
																put( Key.of( "Trace" ), LEVEL_TRACE );
																put( Key.of( "Debug" ), LEVEL_DEBUG );
																put( Key.of( "Debugging" ), LEVEL_DEBUG );
																put( Key.of( "Info" ), LEVEL_INFO );
																put( Key.of( "Information" ), LEVEL_INFO );
																put( Key.of( "Warning" ), LEVEL_WARN );
																put( Key.of( "Warn" ), LEVEL_WARN );
																put( Key.of( "Error" ), LEVEL_ERROR );
																// There is no FATAL level in SLF4J
																put( Key.of( "Fatal" ), LEVEL_ERROR );
															}
														};

	public Logging( BoxRuntime instance ) {
		this.logsDirectory = instance.getConfiguration().runtime.logsDirectory;
	}

	/**
	 * Logs a message
	 *
	 * @param arguments
	 */
	@InterceptionPoint
	public void logMessage( IStruct arguments ) {

		String	logText		= arguments.getAsString( Key.text );
		String	file		= arguments.getAsString( Key.file );
		String	logCategory	= arguments.getAsString( Key.log );
		String	logLevel	= arguments.getAsString( Key.level );
		// named argument for CFML tags cflog and function writeLog
		String	logType		= arguments.getAsString( Key.type );
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
			fileAppender = new FileAppender<ILoggingEvent>();
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
	@InterceptionPoint
	@SuppressWarnings( { "unchecked" } )
	public void onRuntimeShutdown() {
		appendersMap.keySet().stream().forEach( key -> ( ( FileAppender<ILoggingEvent> ) appendersMap.get( key ) ).stop() );
	}

	/**
	 * Alternate signature
	 * 
	 * @param args
	 */
	public void onRuntimeShutdown( IStruct args ) {
		onRuntimeShutdown();
	}

}
