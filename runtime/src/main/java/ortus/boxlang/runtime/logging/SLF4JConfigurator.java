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
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

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
public class SLF4JConfigurator {

	/**
	 * The default logging file to load
	 */
	private static final String DEFAULT_CONFIG_FILE = "config/logging.properties";

	/**
	 * Read and apply configuration for the currently installed SLF4J provider
	 *
	 * @param debugMode Whether or not to enable debug mode
	 */
	public static void configure( Boolean debugMode ) {
		try {
			LogManager.getLogManager().readConfiguration( loadFromPropertiesFile() );

			if ( debugMode ) {
				// TODO: @michaelborn debug mode
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Read logging configuration from the `logging.properties` file
	 */
	private static InputStream loadFromPropertiesFile() {
		return SLF4JConfigurator.class.getClassLoader().getResourceAsStream( DEFAULT_CONFIG_FILE );
	}
}
