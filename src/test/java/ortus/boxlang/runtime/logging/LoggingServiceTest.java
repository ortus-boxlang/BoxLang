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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.LoggerContext;
import ortus.boxlang.runtime.BoxRuntime;

public class LoggingServiceTest {

	static BoxRuntime		runtime;
	static LoggingService	loggingService;

	@BeforeAll
	public static void setup() {
		// This tests loading and configuration
		runtime			= BoxRuntime.getInstance( true );
		loggingService	= LoggingService.getInstance();
	}

	@DisplayName( "Logging service loads, configures, and re-configures the logging" )
	@Test
	public void testLoggingService() {
		assertThat( LoggingService.getInstance() ).isEqualTo( runtime.getLoggingService() );
		assertThat( loggingService.getDefaultEncoder() ).isNotNull();
		assertThat( loggingService.getRootLogger() ).isNotNull();
		assertThat( loggingService.getLoggerContext() ).isInstanceOf( LoggerContext.class );
		assertThat( loggingService.getLogsDirectory() ).isNotNull();
	}

	@DisplayName( "Try logging a message to the default log file" )
	@Test
	public void testLoggingServiceLog() {
		// assert it doesn't fail
		assertDoesNotThrow( () -> loggingService.logMessage( "This is a test message" ) );
	}

}
