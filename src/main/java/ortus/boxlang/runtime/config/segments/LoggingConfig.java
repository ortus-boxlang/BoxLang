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

import java.nio.file.Paths;
import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.util.PropertyHelper;
import ortus.boxlang.runtime.logging.LogLevel;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * The BoxLang LoggingConfig class is a configuration segment that is used to define the logging settings for the BoxLang runtime.
 */
public class LoggingConfig implements IConfigSegment {

	public static final Set<String>	VALID_LOG_LEVELS	= Set.of( "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF" );
	public static final Set<String>	VALID_ENCODERS		= Set.of( "text", "json" );
	public static final Key			DEFAULT_ENCODER		= new Key( "text" );

	/**
	 * The default logs directory for the runtime
	 */
	public String					logsDirectory		= Paths.get( BoxRuntime.getInstance().getRuntimeHome().toString(), "/logs" )
	    .normalize()
	    .toString();

	/**
	 * The maximum number of days to keep log files before rotation
	 * Default is 90 days or 3 months
	 * Set to 0 to disable
	 */
	public int						maxLogDays			= 90;

	/**
	 * The maximum file size for a single log file before rotation
	 * You can use the following suffixes: KB, MB, GB
	 * Default is 100MB
	 */
	public String					maxFileSize			= "100MB";

	/**
	 * The total cap size of all log files before rotation
	 * You can use the following suffixes: KB, MB, GB
	 * Default is 5GB
	 */
	public String					totalCapSize		= "5GB";

	/**
	 * The root logger level
	 */
	public Key						rootLevel			= new Key( "INFO" );

	/**
	 * The collection of loggers and their levels
	 */
	public IStruct					loggers				= Struct.of();

	/**
	 * The default encoding for the log files
	 * This can be either "text" or "json". The default is "text"
	 */
	public Key						defaultEncoder		= DEFAULT_ENCODER;

	/**
	 * Status printer on load
	 */
	public boolean					statusPrinterOnLoad	= false;

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default empty constructor
	 */
	public LoggingConfig() {
		// Default all things
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	@Override
	public IConfigSegment process( IStruct config ) {
		this.logsDirectory			= PropertyHelper.processString( config, Key.logsDirectory, this.logsDirectory );
		this.maxLogDays				= PropertyHelper.processInteger( config, Key.maxLogDays, this.maxLogDays );
		this.maxFileSize			= PropertyHelper.processString( config, Key.maxFileSize, this.maxFileSize );
		this.totalCapSize			= PropertyHelper.processString( config, Key.totalCapSize, this.totalCapSize );
		this.statusPrinterOnLoad	= PropertyHelper.processBoolean( config, Key.statusPrinterOnLoad, this.statusPrinterOnLoad );
		this.rootLevel				= LogLevel.valueOf( PropertyHelper.processString( config, Key.rootLevel, this.rootLevel.getName(), VALID_LOG_LEVELS ),
		    false );
		this.defaultEncoder			= Key.of( PropertyHelper.processString( config, Key.defaultEncoder, DEFAULT_ENCODER.getName(), VALID_ENCODERS ) );
		// process loggers now
		PropertyHelper
		    .processToStruct( config, Key.loggers )
		    .entrySet()
		    .forEach( entry -> {
			    if ( entry.getValue() instanceof IStruct castedStruct ) {
				    LoggerConfig loggerConfig = new LoggerConfig( entry.getKey(), this ).process( castedStruct );
				    this.loggers.put( entry.getKey(), loggerConfig );
			    }
		    } );

		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		IStruct loggersCopy = new Struct();
		this.loggers.entrySet()
		    .forEach( entry -> loggersCopy.put( entry.getKey(), ( ( LoggerConfig ) entry.getValue() ).asStruct() ) );

		return Struct.of(
		    Key.defaultEncoder, this.defaultEncoder.getName(),
		    Key.logsDirectory, this.logsDirectory,
		    Key.loggers, loggersCopy,
		    Key.maxLogDays, this.maxLogDays,
		    Key.maxFileSize, this.maxFileSize,
		    Key.rootLevel, this.rootLevel.getName(),
		    Key.statusPrinterOnLoad, this.statusPrinterOnLoad,
		    Key.totalCapSize, this.totalCapSize
		);
	}

}
