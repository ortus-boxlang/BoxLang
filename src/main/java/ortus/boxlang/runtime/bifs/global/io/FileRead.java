
package ortus.boxlang.runtime.bifs.global.io;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
@BoxBIF( alias = "FileReadBinary" )
public class FileRead extends BIF {

	/**
	 * Constructor
	 */
	public FileRead() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.filepath ),
		    new Argument( false, "string", Key.charsetOrBufferSize ),
		    new Argument( false, "string", Key.charset ),
		    new Argument( false, "string", Key.buffersize )
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
		String	charsetOrBufferSize	= arguments.getAsString( Key.charsetOrBufferSize );
		String	charset				= arguments.getAsString( Key.charset );
		Integer	bufferSize			= arguments.getAsInteger( Key.buffersize );

		if ( charsetOrBufferSize != null ) {
			if ( IntegerCaster.isInteger( charsetOrBufferSize ) ) {
				bufferSize = IntegerCaster.cast( charsetOrBufferSize );
			} else {
				charset = charsetOrBufferSize;
			}
		}

		try {
			return FileSystemUtil.read( arguments.getAsString( Key.filepath ), charset, bufferSize );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}

	}

}
