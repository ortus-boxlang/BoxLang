
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

public class FileDelete extends BIF {

	/**
	 * Constructor
	 */
	public FileDelete() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.file )
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
		try {
			FileSystemUtil.deleteFile( arguments.getAsString( Key.file ) );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return null;
	}

}
