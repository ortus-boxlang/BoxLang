package tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.loader.util.ClassDiscovery;

public class ComponentServiceProviderGenerator {

	/**
	 * A main method to create a text file with all the classes under the package: ortus.boxlang.runtime.bifs.global
	 */
	public static void main( String[] args ) {

		String	targetPackage	= "ortus.boxlang.runtime.components";
		String	targetPath		= "src/main/resources/META-INF/services/ortus.boxlang.runtime.components.Component";

		// Delete the file if it exists
		try {
			java.nio.file.Files.delete( java.nio.file.Paths.get( targetPath ) );
		} catch ( IOException e ) {
			// Do nothing
		}

		try ( BufferedWriter writer = new BufferedWriter( new FileWriter( targetPath ) ) ) {
			// Get a sstream of the classes found
			ClassDiscovery.findAnnotatedClasses(
			    targetPackage.replace( ".", "/" ),
			    BoxComponent.class
			)
			    .sorted( ( a, b ) -> a.getName().compareTo( b.getName() ) )
			    .forEach( ( clazz ) -> {
				    try {
					    writer.write( clazz.getName() );
					    writer.newLine();
				    } catch ( IOException e ) {
					    e.printStackTrace();
				    }
			    } );
		} catch ( IOException e ) {
			e.printStackTrace();
		}

	}
}
