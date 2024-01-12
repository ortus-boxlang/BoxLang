
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
@BoxBIF( alias = "FileSkipBytes" )
@BoxMember( type = BoxLangType.FILE )
@BoxMember( type = BoxLangType.FILE, name = "skipBytes" )

public class FileSeek extends BIF {

	/**
	 * Constructor
	 */
	public FileSeek() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.file ),
		    new Argument( true, "integer", Key.position )
		};
	}

	/**
	 * Moves the buffer cursor position forward the number of characters specified by the position argument
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file The File instance
	 *
	 * @argument.position The cursor position to move forward in the file
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		File file = null;
		if ( arguments.get( Key.file ) instanceof File ) {
			file = ( File ) arguments.get( Key.file );
		} else {
			throw new BoxRuntimeException( "The file argumennt [" + arguments.getAsString( Key.file ) + "] is not an open file stream." );
		}
		file.seek( arguments.getAsInteger( Key.position ) );
		return null;
	}

}
