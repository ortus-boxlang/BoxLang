package ortus.boxlang.runtime.interceptors;

import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Web request based interceptions
 * TODO: Move this to web runtime when broken out
 */
public class WebRequest {

	/**
	 * Listens for the file component actions around web uploaads
	 *
	 * @param interceptData The data to be intercepted
	 */
	@InterceptionPoint
	public void onFileComponentAction( IStruct interceptData ) {
		IStruct	arguments	= interceptData.getAsStruct( Key.arguments );
		Key		action		= Key.of( arguments.getAsString( Key.action ) );

		if ( action.equals( Key.upload ) ) {
			throw new BoxRuntimeException( "The file action [" + action.getName() + "] is yet implemented in the web runtime" );
		} else if ( action.equals( Key.uploadAll ) ) {
			throw new BoxRuntimeException( "The file action [" + action.getName() + "] is not yet implemented in the web runtime" );
		}
	}

}
