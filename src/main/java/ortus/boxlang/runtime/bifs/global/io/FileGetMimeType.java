
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

@BoxBIF

public class FileGetMimeType extends BIF {

	/**
	 * Constructor
	 */
	public FileGetMimeType() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.file ),
		    new Argument( false, "boolean", Key.strict, true )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Path	filePath	= Path.of( arguments.getAsString( Key.file ) );
		Boolean	strict		= arguments.getAsBoolean( Key.strict );

		String	mimeType	= null;

		if ( strict ) {
			try {
				if ( !Files.exists( filePath ) ) {
					throw new BoxRuntimeException(
					    "The file ["
					        + arguments.getAsString( Key.file )
					        + "] does not exist. To retrieve the mimetype of a non-existent file set the strict argument to false."
					);
				} else if ( Files.size( filePath ) == 0 ) {
					throw new BoxRuntimeException(
					    "The file ["
					        + arguments.getAsString( Key.file )
					        + "] is empty. To retrieve the mimetype of a empty file set the strict argument to false."
					);
				}
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}

		}

		try {
			mimeType = Files.probeContentType( filePath );
			if ( mimeType == null ) {
				mimeType = "application/octet-stream";
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		return mimeType;

	}

}
