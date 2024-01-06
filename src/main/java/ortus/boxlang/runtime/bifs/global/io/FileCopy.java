
package ortus.boxlang.runtime.bifs.global.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF

public class FileCopy extends BIF {

	/**
	 * Constructor
	 */
	public FileCopy() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.source ),
		    new Argument( true, "string", Key.destination ),
		    new Argument( false, "boolean", Key.createPath, true )
		};
	}

	/**
	 * Copies a file from one location to another
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The source file
	 *
	 * @argument.destination The destination file
	 *
	 * @argument.createPath [ true ] whether to create any nested paths required to the new file
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Path	sourcePath				= Path.of( arguments.getAsString( Key.source ) );
		Path	destinationPath			= Path.of( arguments.getAsString( Key.destination ) );
		Path	destinationDirectory	= destinationPath.getParent();
		Boolean	createPaths				= arguments.getAsBoolean( Key.createPath );

		if ( createPaths && !Files.exists( destinationDirectory ) ) {
			try {
				Files.createDirectories( destinationDirectory );
			} catch ( IOException e ) {
				throw new RuntimeException( e );
			}
		}

		try {
			Files.copy( sourcePath, destinationPath );
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}

		return null;

	}

}
