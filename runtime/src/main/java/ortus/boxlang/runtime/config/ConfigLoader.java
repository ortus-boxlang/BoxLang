package ortus.boxlang.runtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import ortus.boxlang.runtime.BoxPiler;

import java.io.IOException;
import java.io.ObjectInputFilter.Config;

public class ConfigLoader {

	/**
	 * Path to the config file in the `resources` folder
	 */
	private static final String		CONFIG_FILE	= "config/config.json";

	private static ConfigLoader		instance;
	private static Configuration	configuration;

	/**
	 * Constructor
	 */
	private ConfigLoader() {
		// Any initialization code can be placed here
		getInstance();
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
	 * Load the config file
	 *
	 * @return Config
	 */
	public static synchronized Configuration load() {
		// Create ObjectMapper
		ObjectMapper objectMapper = new ObjectMapper();

		// Read JSON file into Configuration class
		try {
			instance.configuration = objectMapper.readValue(
			    ConfigLoader.class.getResourceAsStream( CONFIG_FILE ),
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
