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
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Configures the bundled SLF4J provider.
 *
 * This class serves as a single endpoint for configuring the slf4j logging provider. Currently that is logback, but in the future it may be another
 * provider.
 *
 * See https://logback.qos.ch/manual/configuration.html for more information on logback configuration.
 */
public class LoggingConfigurator extends ContextAwareBase implements Configurator {

	/**
	 * Logback-specific encoder pattern. Thankfully, this is fairly legible compared to the JUL pattern.
	 * https://logback.qos.ch/manual/layouts.html#conversionWord
	 *
	 * @see https://logback.qos.ch/manual/layouts.html#conversionWord
	 */
	private static final String	LOG_FORMAT	= "%date %logger{0} [%level] %kvp %message%n";

	/**
	 * Passed runtime debugger flag
	 */
	private Boolean				debugMode	= false;

	/**
	 * Default constructor.
	 */
	public LoggingConfigurator( Boolean debugMode ) {
		this.debugMode = debugMode;
	}

	/**
	 * Configure the logging provider.
	 *
	 * @param loggerContext The logger context to configure
	 *
	 * @return The status of the configuration
	 */
	public ExecutionStatus configure( LoggerContext loggerContext ) {
		// Base log level depending on debug mode
		Level					logLevel	= Boolean.TRUE.equals( this.debugMode ) ? Level.DEBUG : Level.INFO;

		// Setup the Pattern Layout Encoder
		// See: https://logback.qos.ch/manual/layouts.html#ClassicPatternLayout
		PatternLayoutEncoder	encoder		= new PatternLayoutEncoder();
		encoder.setContext( loggerContext );
		encoder.setPattern( LOG_FORMAT );
		encoder.start();

		// Configure a Console Appender
		// See: https://logback.qos.ch/manual/appenders.html
		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
		appender.setContext( loggerContext );
		appender.setEncoder( encoder );
		appender.start();

		// Configure Root Logger
		Logger rootLogger = loggerContext.getLogger( Logger.ROOT_LOGGER_NAME );
		rootLogger.setLevel( logLevel );
		rootLogger.addAppender( appender );

		// We should be the last configurator to run, so stop searching for further configurators.
		return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
	}

	/**
	 * Reset configuration according to the provided debug mode.
	 *
	 * @param debugMode Whether or not to enable debug mode
	 */
	public static void loadConfiguration( Boolean debugMode ) {
		LoggerContext		loggerContext	= ( LoggerContext ) LoggerFactory.getILoggerFactory();
		LoggingConfigurator	configurator	= new LoggingConfigurator( debugMode );
		configurator.setContext( loggerContext );
		loggerContext.reset();
		configurator.configure( loggerContext );
	}
}
