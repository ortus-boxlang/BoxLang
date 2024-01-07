
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
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF

public class DirectoryCreate extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryCreate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		    new Argument( false, "boolean", Key.createPath, true ),
		    new Argument( false, "boolean", Key.ignoreExists, false )
		};
	}

	/**
	 * Creates a directory
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The directory path to create
	 *
	 * @argument.createPath [true] Whether to create all paths necessary to create the directory path
	 *
	 * @argument.ignoreExists [false] Whether to ignore if a directory already exists
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	targetDirectory	= arguments.getAsString( Key.path );
		Path	targetPath		= Path.of( targetDirectory );
		Boolean	createPath		= arguments.getAsBoolean( Key.createPath );
		Boolean	ignoreExists	= arguments.getAsBoolean( Key.ignoreExists );
		if ( !ignoreExists && FileSystemUtil.exists( targetDirectory ) ) {
			throw new BoxRuntimeException( "The directory [" + targetPath.toAbsolutePath().toString()
			    + "] already exists. Set the boolean argument ignoreExists to true to prevent this error" );
		}
		try {
			if ( createPath ) {
				Files.createDirectories( targetPath );
			} else {
				Files.createDirectory( targetPath );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		return null;
	}

}
