
package ortus.boxlang.runtime.bifs.global.io;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.FILE )

public class FileWriteLine extends BIF {

	/**
	 * Constructor
	 */
	public FileWriteLine() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.file ),
		    new Argument( true, "string", Key.data )
		};
	}

	/**
	 * Writes a line of data to a file
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file An existing file object or a path to a file
	 *
	 * @argument.data The line of data to be written
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		File file = null;
		if ( arguments.get( Key.file ) instanceof File ) {
			file = ( File ) arguments.get( Key.file );
		} else if ( arguments.get( Key.file ) instanceof String ) {
			file = new File( arguments.getAsString( Key.file ), "append", arguments.getAsString( Key.charset ), false );
		} else {
			throw new BoxRuntimeException( "The file argumennt [" + arguments.getAsString( Key.file ) + "] is not an open file stream or string path." );
		}
		file.writeLine( arguments.getAsString( Key.data ) );
		// For strings file args we need to close the buffer
		if ( arguments.get( Key.file ) instanceof String ) {
			file.close();
		}
		return file;
	}

}
