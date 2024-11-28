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
package ortus.boxlang.runtime.config.segments;

import java.util.Set;

import ortus.boxlang.runtime.config.util.PropertyHelper;
import ortus.boxlang.runtime.logging.LogLevel;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A default configuration for a logger.
 */
public class LoggerConfig implements IConfigSegment {

	/**
	 * The valid appenders we can use in the runtime now
	 */
	public static final Set<String>	VALID_APPENDERS		= Set.of( "console", "file" );
	public static final Key			DEFAULT_APPENDER	= Key.file;
	public static final Key			DEFAULT_LOG_LEVEL	= Key.trace;

	/**
	 * The unique name of the logger.
	 */
	public Key						name;

	/**
	 * The level of the logger.
	 */
	public Key						level				= DEFAULT_LOG_LEVEL;

	/**
	 * The appender for the logger.
	 */
	public Key						appender			= DEFAULT_APPENDER;

	/**
	 * The encoder to use for the logger.
	 */
	public Key						encoder				= LoggingConfig.DEFAULT_ENCODER;

	/**
	 * The appender properties
	 */
	public IStruct					appenderArguments	= Struct.of();

	/**
	 * Default logging configuration
	 */
	private LoggingConfig			loggingConfig;

	/**
	 * Constructor
	 *
	 * @param name          The name of the logger
	 * @param loggingConfig The logging configuration
	 */
	public LoggerConfig( String name, LoggingConfig loggingConfig ) {
		this.name			= new Key( name );
		this.loggingConfig	= loggingConfig;
	}

	@Override
	public IConfigSegment process( IStruct config ) {
		this.level				= LogLevel.valueOf( PropertyHelper.processString( config, Key.level, this.loggingConfig.rootLevel.getName() ), false );
		this.appender			= Key.of( PropertyHelper.processString( config, Key.appender, DEFAULT_APPENDER.getName(), VALID_APPENDERS ) );
		this.encoder			= Key.of(
		    PropertyHelper.processString( config, Key.encoder, LoggingConfig.DEFAULT_ENCODER.getName(), LoggingConfig.VALID_ENCODERS )
		);
		this.appenderArguments	= PropertyHelper.processToStruct( config, Key.appenderArguments );

		return this;
	}

	@Override
	public IStruct asStruct() {
		IStruct argsCopy = new Struct( Struct.KEY_LENGTH_LONGEST_FIRST_COMPARATOR );
		argsCopy.putAll( this.appenderArguments );

		return Struct.of(
		    Key._NAME, this.name.getName(),
		    Key.level, this.level.getName(),
		    Key.appender, this.appender.getName(),
		    Key.encoder, this.encoder.getName(),
		    Key.appenderArguments, argsCopy
		);
	}

}
