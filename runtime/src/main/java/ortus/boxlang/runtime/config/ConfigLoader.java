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
package ortus.boxlang.runtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class ConfigLoader {

	/**
	 * Path to the core config file in the `resources` folder
	 */
	private static final String		CONFIG_FILE	= "config/config.json";

	/**
	 * The ConfigLoader instance
	 */
	private static ConfigLoader		instance;

	/**
	 * The parsed configuration
	 */
	private static Configuration	configuration;

	/**
	 * Constructor
	 */
	private ConfigLoader() {
		// Any initialization code can be placed here
	}

	/**
	 * Get an instance of the ConfigLoader
	 *
	 * @return The ConfigLoader instance
	 */
	public static synchronized ConfigLoader getInstance() {
		if ( instance == null ) {
			instance = new ConfigLoader();
		}
		return instance;
	}

	/**
	 * Get the parsed configuration
	 *
	 * @return The parsed configuration
	 */
	public static Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Load the config file into the Configuration class from the `resources` folder
	 *
	 * @throws RuntimeException If the config file cannot be loaded
	 *
	 * @return The parsed configuration
	 */
	public static synchronized Configuration load() {
		// Read JSON file into Configuration class
		try {
			instance.configuration = new ObjectMapper().readValue(
			    ClassLoader.getSystemClassLoader().getResourceAsStream( CONFIG_FILE ),
			    Configuration.class
			);
		} catch ( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException( "Unable to load core resources config file", e );
		}

		return instance.configuration;
	}

	public static void main( String[] args ) {
		Configuration config = ConfigLoader.getInstance().load();
		// Access the parsed configuration
		System.out.println( "Compiler Directory: " + config.getCompiler().getClassGenerationDirectory() );
		System.out.println( "Modules Directory: " + config.getRuntime().getModulesDirectory() );
		System.out.println( "Cache Type: " + config.getRuntime().getCaches().getType() );
		System.out.println( "Cache Properties: " + config.getRuntime().getCaches().getProperties() );
	}

}
