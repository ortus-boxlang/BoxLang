/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF
public class BoxAnnounceAsync extends BIF {

	/**
	 * Constructor
	 */
	public BoxAnnounceAsync() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.state ),
		    new Argument( false, "struct", Key.data, new Struct() )
		};
	}

	/**
	 * Announce a BoxLang event to the system asynchronously
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.state The event to announce
	 *
	 * @argument.data The data to send with the event
	 *
	 * @return A CompletableFuture that will be completed when the event has been announced, or null if the state doesn't exist
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return runtime
		    .getInterceptorService()
		    .announceAsync(
		        Key.of( arguments.getAsString( Key.state ) ),
		        arguments.getAsStruct( Key.data ),
		        context
		    );
	}

}
