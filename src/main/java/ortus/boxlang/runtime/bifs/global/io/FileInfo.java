
package ortus.boxlang.runtime.bifs.global.io;

import java.nio.file.Path;

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
@BoxBIF( alias = "GetFileInfo" )
@BoxMember( type = BoxLangType.FILE )

public class FileInfo extends BIF {

	/**
	 * Constructor
	 */
	public FileInfo() {
		super();
		// Uncomment and define declare argument to this BIF
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.file )
		};
	}

	/**
	 * Returns a struct of file information. Different values are returned for FileInfo and GetFileInfo
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.File The filepath or file object to retrieve info upon
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	file		= arguments.get( Key.file );
		Path	filePath	= null;

		if ( file instanceof File ) {
			File fileObj = ( File ) file;
			filePath = fileObj.getPath();
		} else {
			filePath = Path.of( ( String ) file );
		}

		Key		bifMethodKey	= arguments.getAsKey( __functionName );
		Boolean	verbose			= bifMethodKey.equals( Key.getFileInfo );

		return FileSystemUtil.info( filePath, verbose );

	}

}
