
package ortus.boxlang.runtime.bifs.global.io;

import java.io.IOException;
import java.nio.file.Path;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;

@BoxBIF

public class GetCanonicalPath extends BIF {

	/**
	 * Constructor
	 */
	public GetCanonicalPath() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path )
		};
	}

	/**
	 * Returns the canonical path of a file, resolving all relative path elements and symlinks
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The file or directory path string
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		try {
			return Path.of( arguments.getAsString( Key.path ) ).toRealPath().toString();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

}
