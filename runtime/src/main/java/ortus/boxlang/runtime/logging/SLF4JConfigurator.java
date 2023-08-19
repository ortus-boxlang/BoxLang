package ortus.boxlang.runtime.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

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
	 * Read and apply configuration for the currently installed SLF4J provider
	 */
	public static void configure() {
		try {
			LogManager.getLogManager().readConfiguration( loadFromPropertiesFile() );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Read logging configuration from the `logging.properties` file
	 */
	private static InputStream loadFromPropertiesFile() {
		return SLF4JConfigurator.class.getClassLoader().getResourceAsStream( "logging.properties" );
	}
}
