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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class GetClassMetadata extends BIF {

	private static final ClassLocator CLASS_LOCATOR = BoxRuntime.getInstance().getClassLocator();

	/**
	 * Constructor
	 */
	public GetClassMetadata() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.path )
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
		Object path = arguments.get( Key.path );

		// Check if the path is an instance of a class already
		if ( path instanceof IClassRunnable castedObject ) {
			return castedObject.getBoxMeta().getMeta();
		}

		// Else we have a path, let's get the data
		DynamicObject loadedClass = CLASS_LOCATOR.load(
		    context,
		    ( String ) path,
		    ClassLocator.BX_PREFIX,
		    true,
		    context.getCurrentImports()
		);

		// Check if the class is an interface
		if ( DynamicInteropService.isInterface( loadedClass.getTargetClass() ) ) {
			BoxInterface boxInterface = ( BoxInterface ) loadedClass.unWrapBoxLangClass();
			return boxInterface.getMetaData();
		}
		// Else we have a class
		else {
			loadedClass.invokeConstructor( context, Key.noInit );
			IClassRunnable boxClass = ( IClassRunnable ) loadedClass.unWrapBoxLangClass();
			return boxClass.getBoxMeta().getMeta();
		}

	}
}
