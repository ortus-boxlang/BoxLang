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

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * Configures the bundled SLF4J provider.
 *
 * This class serves as a single endpoint for configuring the slf4j logging provider. Currently that is logback, but in the future it may be another
 * provider.
 *
 * See https://logback.qos.ch/manual/configuration.html for more information on logback configuration.
 */
public class LoggingConfigurator {

	/**
	 * Logback-specific encoder pattern. Thankfully, this is fairly legible compared to the JUL pattern.
	 * https://logback.qos.ch/manual/layouts.html#conversionWord
	 *
	 * @see https://logback.qos.ch/manual/layouts.html#conversionWord
	 */
	private static String		logFormat			= "%date %logger{0} [%level] %kvp %message%n";

	/**
	 * The default logging file to load
	 */
	private static final String	DEFAULT_CONFIG_FILE	= "config/logback.xml";

	/**
	 * Read and apply configuration for the currently installed SLF4J provider
	 *
	 * @param debugMode Whether or not to enable debug mode
	 */
	public static void configure( Boolean debugMode ) {
		// Set directory to look for logback.xml
		System.setProperty( "Logback.configurationFile", DEFAULT_CONFIG_FILE );

		Level					logLevel		= debugMode ? Level.DEBUG : Level.INFO;
		Logger					rootLogger		= ( Logger ) LoggerFactory.getLogger( Logger.ROOT_LOGGER_NAME );
		LoggerContext			loggerContext	= rootLogger.getLoggerContext();

		PatternLayoutEncoder	encoder			= new PatternLayoutEncoder();
		encoder.setContext( loggerContext );
		encoder.setPattern( logFormat );
		encoder.start();

		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
		appender.setContext( loggerContext );
		appender.setEncoder( encoder );
		appender.start();

		rootLogger.setLevel( logLevel );
		rootLogger.addAppender( appender );
	}
}
