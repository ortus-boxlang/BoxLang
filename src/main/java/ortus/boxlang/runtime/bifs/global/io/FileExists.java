
package ortus.boxlang.runtime.bifs.global.io;

import java.nio.file.Files;
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

public class FileExists extends BIF {

	/**
	 * Constructor
	 */
	public FileExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.source ),
		    new Argument( true, "boolean", Key.allowRealPath, true )
		};
	}

	/**
	 * Determines whether a file exists
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The file path
	 *
	 * @arguments.allowRealPath Whether to allow an absolute path as the path argument
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	filePath		= arguments.getAsString( Key.source );
		Boolean	allowRealPath	= arguments.getAsBoolean( Key.allowRealPath );

		if ( !allowRealPath && Path.of( filePath ).isAbsolute() ) {
			throw new BoxRuntimeException(
			    "The file or path argument [" + filePath + "] is an absolute path. This is disallowed when the allowRealPath argument is set to false."
			);
		}

		return ( Boolean ) FileSystemUtil.exists( filePath ) && !Files.isDirectory( Path.of( filePath ) );
	}

}
