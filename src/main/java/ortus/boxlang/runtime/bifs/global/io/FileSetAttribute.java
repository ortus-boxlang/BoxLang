
package ortus.boxlang.runtime.bifs.global.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
@BoxMember( type = BoxLangType.FILE )

public class FileSetAttribute extends BIF {

	/**
	 * Constructor
	 */
	public FileSetAttribute() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.file ),
		    new Argument( true, "string", Key.attribute )
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
		String	file	= null;
		File	fileObj	= null;
		if ( arguments.get( Key.file ) instanceof File ) {
			fileObj	= ( File ) arguments.get( Key.file );
			file	= fileObj.filepath;
		} else {
			file = arguments.getAsString( Key.file );
		}

		String	attribute		= arguments.getAsString( Key.attribute );
		String	permissionSet	= null;
		Path	filePath		= Path.of( file );
		Boolean	isPosix			= filePath.getFileSystem().supportedFileAttributeViews().contains( "posix" );
		boolean	isWindows		= FileSystemUtil.isWindows;
		Object	returnItem		= fileObj != null ? fileObj : null;
		switch ( attribute.toLowerCase() ) {
			case "normal" :
			case "default" : {
				permissionSet = isPosix ? "rw-rw-r--" : "dos:readonly";
				break;
			}
			case "readonly" : {
				permissionSet = isWindows ? "dos:readonly" : "r--r--r--";
				break;
			}
			case "archive" : {
				if ( !isWindows ) {
					return returnItem;
				}
				permissionSet = "dos:archive";
				break;
			}
			case "hidden" : {
				if ( !isWindows ) {
					return returnItem;
				}
				permissionSet = "dos:hidden";
				break;
			}
			case "system" : {
				if ( !isWindows ) {
					return returnItem;
				}
				permissionSet = "dos:system";
				break;
			}
			default : {
				throw new BoxRuntimeException( "The attribute provided [" + attribute + "] is not valid for this method." );
			}
		}
		if ( permissionSet.contains( ":" ) ) {
			try {
				Files.setAttribute( filePath, permissionSet, true, LinkOption.NOFOLLOW_LINKS );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		} else {
			try {
				Files.setPosixFilePermissions( filePath, PosixFilePermissions.fromString( permissionSet ) );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}

		return returnItem;
	}

}
