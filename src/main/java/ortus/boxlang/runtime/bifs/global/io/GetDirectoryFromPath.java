
package ortus.boxlang.runtime.bifs.global.io;

import java.nio.file.Path;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF

public class GetDirectoryFromPath extends BIF {

	/**
	 * Constructor
	 */
	public GetDirectoryFromPath() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		};
	}

	/**
	 * Retrieves the directory parent of a path
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The path to extract the parent directory from
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return Path.of( arguments.getAsString( Key.path ) ).getParent().toAbsolutePath().toString();
	}

}
