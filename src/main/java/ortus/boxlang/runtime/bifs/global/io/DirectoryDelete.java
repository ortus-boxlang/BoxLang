
package ortus.boxlang.runtime.bifs.global.io;

import java.io.IOException;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF

public class DirectoryDelete extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryDelete() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		    new Argument( true, "boolean", Key.recursive, false )
		};
	}

	/**
	 * Deletes a directory
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path the path of the directory to delete
	 *
	 * @argument.recursive [default:false] whether to recursively delete the directory.
	 *                     If falls and the directory is not empty, with throw a runtime exception
	 *
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		try {
			FileSystemUtil.deleteDirectory( arguments.getAsString( Key.path ), arguments.getAsBoolean( Key.recursive ) );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return null;
	}

}
