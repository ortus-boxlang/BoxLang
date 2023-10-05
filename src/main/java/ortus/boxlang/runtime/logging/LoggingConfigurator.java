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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.LogManager;

import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * Configures the bundled SLF4J provider.
 *
 * This class serves as a single endpoint for configuring the slf4j logging provider, whether it is:
 *
 * <ul>
 * <li>java.util.logging</li>
 * <li>logback</li>
 * <li>Apache Commons Logging</li>
 * </ul>
 *
 * or anything else, this class will ensure the provider logs according to the defined configuration.
 */
public class LoggingConfigurator {

	/**
	 * Please see the Java docs for this obtuse logger format...
	 * ... but in a nutshell:
	 * <li>`1$` inserts the date
	 * <li>`3$` inserts the logger name
	 * <li>`4$` inserts the log level
	 *
	 * Formats:
	 * <li>`tF` formats timestamp to `"%tY-%tm-%td"`
	 * <li>`tT` formats timestamp to 24-hour `"%tH:%tM:%tS`
	 * <li>`s` formats string as, uh, string.
	 * <li>`%n` inserts line separator
	 *
	 * @see https://docs.oracle.com/en/java/javase/17/docs/api/java.logging/java/util/logging/SimpleFormatter.html#format(java.util.logging.LogRecord)
	 */
	private static String		logFormat			= "[%1$tF %1$tT] [%3$s] [%4$s] %5$s %n";

	/**
	 * The default logging file to load
	 */
	private static final String	DEFAULT_CONFIG_FILE	= "config/logging.properties";

	/**
	 * Read and apply configuration for the currently installed SLF4J provider
	 *
	 * @param debugMode Whether or not to enable debug mode
	 */
	public static void configure( Boolean debugMode ) {
		try {
			LogManager.getLogManager().readConfiguration( debugMode
			    ? loadDynamicConfig( java.util.logging.Level.FINE )
			    : loadFromPropertiesFile()
			);
		} catch ( IOException e ) {
			// use logger for this, or rethrow
			e.printStackTrace();
		}
	}

	/**
	 * Read logging configuration from the `logging.properties` file
	 */
	private static InputStream loadFromPropertiesFile() {
		InputStream configFile = LoggingConfigurator.class.getClassLoader().getResourceAsStream( DEFAULT_CONFIG_FILE );
		if ( configFile == null ) {
			throw new ApplicationException( "Unable to load logging configuration from classpath resource: " + DEFAULT_CONFIG_FILE );
		}
		return configFile;
	}

	/**
	 * Build JDK logging configuration dynamically using the provided parameters.
	 *
	 * @param rootLogLevel Default log level for root loggers.
	 *
	 * @return an InputStream safe for feeding to the JDK LogManager's `readConfiguration()` method.
	 */
	private static InputStream loadDynamicConfig( java.util.logging.Level rootLogLevel ) {
		String logConfig = """
		                   .level=%s
		                   handlers=java.util.logging.ConsoleHandler
		                   java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
		                   java.util.logging.SimpleFormatter.format=%s
		                   """
		    .formatted( rootLogLevel, logFormat );
		return new java.io.ByteArrayInputStream( logConfig.getBytes( StandardCharsets.UTF_8 ) );
	}
}
