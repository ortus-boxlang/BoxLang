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
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF
public class GetApplicationMetadata extends BIF {

	/**
	 * Constructor
	 */
	public GetApplicationMetadata() {
		super();
		// TODO: Lucee has suppressFunction and onlySupported booleans which filter out the things returned.
	}

	/**
	 * Print a message with line break to the console
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.message The message to print
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct settings = new Struct();
		settings.putAll( context.getParentOfType( RequestBoxContext.class ).getSettings() );
		if ( settings.get( Key.invokeImplicitAccessor ) == null ) {
			// We don't actually know the value if it's not set since it's based on the source type
			// BX files default this to true, but CFCs default this to false
			// If not explicitly set, we keep the app setting null so we know it's not actually set
			settings.put( Key.invokeImplicitAccessor, true );
		}
		return settings;
	}
}
