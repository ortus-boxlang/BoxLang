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
package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.types.UDF;

/**
 * This context represents the initialization of an interface, and is really only here for the registerUDF method
 */
public class InterfaceBoxContext extends BaseBoxContext {

	/**
	 * The interface instance
	 */
	protected BoxInterface thisInterface;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent        The parent context
	 * @param thisInterface The target interface
	 */
	public InterfaceBoxContext( IBoxContext parent, BoxInterface thisInterface ) {
		super( parent );
		this.thisInterface = thisInterface;

	}

	@Override
	public void registerUDF( UDF udf ) {
		thisInterface.getDefaultMethods().put( udf.getName(), udf );
	}

}
