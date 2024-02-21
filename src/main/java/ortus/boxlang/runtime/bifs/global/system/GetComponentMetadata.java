/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class GetComponentMetadata extends BIF {

	ClassLocator classLocator = ClassLocator.getInstance();

	/**
	 * Constructor
	 */
	public GetComponentMetadata() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path )
		};
	}

	/**
	 * Returns the current value of an internal millisecond timer.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String			path	= arguments.getAsString( Key.path );
		IClassRunnable	cfc		= ( IClassRunnable ) classLocator
		    .load( context, "bx:" + path, context.getCurrentImports() )
		    .invokeConstructor( context, Key.noInit )
		    .unWrapBoxLangClass();
		return cfc.getMetaData();
	}
}
