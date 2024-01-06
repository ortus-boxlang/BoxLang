
package ortus.boxlang.runtime.bifs.global.io;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF

public class DirectoryCopy extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryCopy() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.source ),
		    new Argument( true, "string", Key.destination ),
		    new Argument( false, "boolean", Key.recurse, false ),
		    new Argument( false, "string", Key.filter, "*" ),
		    new Argument( false, "boolean", Key.createPath, true )
		};
	}

	/**
	 * Copies a directory from one location to another
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The source directory
	 *
	 * @argument.destination The destination directory
	 *
	 * @argument.recurse [ false ] whether to recurse in to sub-directories and create paths
	 *
	 * @argument.filter [ "*" ] a file or directory filter to pass
	 *
	 * @argument.createPath [ true ] whether to create any nested paths required to the new directory
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		FileSystemUtil.copyDirectory(
		    arguments.getAsString( Key.source ),
		    arguments.getAsString( Key.destination ),
		    arguments.getAsBoolean( Key.recurse ),
		    arguments.getAsString( Key.filter ),
		    arguments.getAsBoolean( Key.createPath )
		);
		return null;
	}

}
