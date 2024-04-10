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

import java.nio.file.Paths;
import java.util.HashMap;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.logging.LoggingConfigurator;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class WriteLog extends BIF {

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
																put( Key.of( "Fatal" ), LEVEL_ERROR );
															}
														};

	private final BoxRuntime			instance		= BoxRuntime.getInstance();

	private final String				logsDirectory	= instance.getConfiguration().runtime.logsDirectory;

	/**
	 * Constructor
	 */
	public WriteLog() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.text ),
		    new Argument( false, "string", Key.file ),
		    new Argument( false, "string", Key.log, "Application" ),
		    new Argument( false, "string", Key.type, "Information" ),
		};
	}

	/**
	 *
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	logText		= arguments.getAsString( Key.text );
		String	file		= arguments.getAsString( Key.file );
		String	logCategory	= arguments.getAsString( Key.log );
		String	logLevel	= arguments.getAsString( Key.type );
		Key		levelKey	= Key.of( logLevel );

		if ( !levelMap.containsKey( levelKey ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "[%s] is not a valid logging level type.",
			        logLevel
			    )
			);
		}

		Logger						logger			= ( ch.qos.logback.classic.Logger ) LoggerFactory.getLogger( logCategory );
		FileAppender<ILoggingEvent>	fileAppender	= null;
		try {
			if ( file != null ) {
				String					filePath		= Paths.get( logsDirectory, "/", file ).normalize().toString();
				LoggerContext			logContext		= ( LoggerContext ) LoggerFactory.getILoggerFactory();
				PatternLayoutEncoder	layoutEncoder	= new PatternLayoutEncoder();
				layoutEncoder.setPattern( LoggingConfigurator.LOG_FORMAT );
				layoutEncoder.setContext( logContext );
				layoutEncoder.start();
				fileAppender = new FileAppender<ILoggingEvent>();
				fileAppender.setFile( filePath );
				fileAppender.setEncoder( layoutEncoder );
				fileAppender.setContext( logContext );
				fileAppender.start();

				logger.addAppender( fileAppender );
				logger.setAdditive( false );
			} else {
				logger = ( ch.qos.logback.classic.Logger ) LoggerFactory.getLogger( logCategory );
			}

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

		return null;
	}
}
