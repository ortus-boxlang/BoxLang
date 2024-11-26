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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * The BoxLang LoggingConfig class is a configuration segment that is used to define the logging settings for the BoxLang runtime.
 */
public class LoggingConfig implements IConfigSegment {

	/**
	 * The default logs directory for the runtime
	 */
	public String				logsDirectory	= Paths.get( BoxRuntime.getInstance().getRuntimeHome().toString(), "/logs" )
	    .normalize()
	    .toString();

	/**
	 * The maximum number of days to keep log files before rotation
	 * Default is 90 days or 3 months
	 * Set to 0 to disable
	 */
	public int					maxLogDays		= 90;

	/**
	 * The maximum file size for a single log file before rotation
	 * You can use the following suffixes: KB, MB, GB
	 * Default is 100MB
	 */
	public String				maxFileSize		= "100MB";

	/**
	 * The total cap size of all log files before rotation
	 * You can use the following suffixes: KB, MB, GB
	 * Default is 5GB
	 */
	public String				totalCapSize	= "5GB";

	/**
	 * The root logger level
	 */
	public String				rootLevel		= "INFO";

	/**
	 * The collection of loggers and their levels
	 */
	public IStruct				loggers			= Struct.of();

	/**
	 * --------------------------------------------------------------------------
	 * Private Props
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger	logger			= LoggerFactory.getLogger( LoggingConfig.class );

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

		// Debug Mode || Debbuging Enabled (cfconfig)
		if ( config.containsKey( Key.logsDirectory ) ) {
			this.logsDirectory = PlaceholderHelper.resolve( config.get( Key.logsDirectory ) );
		}

		if ( config.containsKey( Key.maxLogDays ) ) {
			this.maxLogDays = IntegerCaster.cast( PlaceholderHelper.resolve( config.get( Key.maxLogDays ) ) );
		}

		if ( config.containsKey( Key.maxFileSize ) ) {
			this.maxFileSize = PlaceholderHelper.resolve( config.get( Key.maxFileSize ) );
		}

		if ( config.containsKey( Key.totalCapSize ) ) {
			this.totalCapSize = PlaceholderHelper.resolve( config.get( Key.totalCapSize ) );
		}

		if ( config.containsKey( Key.rootLevel ) ) {
			this.rootLevel = PlaceholderHelper.resolve( config.get( Key.rootLevel ) );
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		return Struct.of(
		    Key.logsDirectory, this.logsDirectory,
		    Key.maxLogDays, this.maxLogDays,
		    Key.maxFileSize, this.maxFileSize,
		    Key.rootLevel, this.rootLevel,
		    Key.totalCapSize, this.totalCapSize
		);
	}

}
