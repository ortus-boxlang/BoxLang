
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
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
@BoxMember( type = BoxLangType.FILE )

public class FileSetAccessMode extends BIF {

	/**
	 * Constructor
	 */
	public FileSetAccessMode() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.file ),
		    new Argument( true, "string", Key.mode )
		};
	}

	/**
	 * Sets the Posix permissions on a file
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file A file path or object
	 *
	 * @argument.mode The three-digit permission designations for the file or directory
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	file	= null;
		File	fileObj	= null;
		if ( arguments.get( Key.file ) instanceof File ) {
			fileObj	= ( File ) arguments.get( Key.file );
			file	= fileObj.filepath;
		} else {
			file = arguments.getAsString( Key.file );
		}

		FileSystemUtil.setPosixPermissions( file, arguments.getAsString( Key.mode ) );

		return fileObj != null ? fileObj : null;
	}

}
