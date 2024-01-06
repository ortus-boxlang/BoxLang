
package ortus.boxlang.runtime.bifs.global.io;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF

public class FileWrite extends BIF {

	/**
	 * Constructor
	 */
	public FileWrite() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.file ),
		    new Argument( true, "any", Key.data ),
		    new Argument( false, "string", Key.charset, "utf-8" ),
		    new Argument( false, "boolean", Key.ensurePaths, false )
		};
	}

	/**
	 * Writes the contents of a string or binary data to a file
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file The string path of the file - either root relative or absolute
	 *
	 * @argument.data The string or binary byte array of the file content
	 *
	 * @arguments.charset The charset encoding ( ignored for binary data )
	 *
	 * @aguments.ensurePaths [false] ( Boxlang only ) When true will ensure all directories to file destination are created
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	filePath	= arguments.getAsString( Key.file );
		Object	fileContent	= arguments.get( Key.data );
		String	charset		= arguments.getAsString( Key.charset );
		Boolean	ensurePaths	= arguments.getAsBoolean( Key.ensurePaths );
		try {
			if ( fileContent instanceof String ) {
				FileSystemUtil.write( filePath, arguments.getAsString( Key.data ), charset, ensurePaths );
			} else {
				FileSystemUtil.write( filePath, ( byte[] ) fileContent, ensurePaths );
			}
		} catch ( NoSuchFileException e ) {
			throw new BoxRuntimeException(
			    "The file [" + filePath + "] could not be writtent. The directory [" + Path.of( filePath ).getParent().toString() + "] does not exist." );
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}

		return null;

	}

}
