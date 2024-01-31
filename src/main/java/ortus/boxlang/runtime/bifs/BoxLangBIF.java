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
package ortus.boxlang.runtime.bifs;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * This is a BIF that is used to proxy from a BoxLang script to a Java method
 */
@BoxBIF
public class BoxLangBIF extends BIF {

	/**
	 * The target BoxLang BIF class we proxy to
	 */
	private IClassRunnable target;

	/**
	 * Constructor
	 *
	 * @param target The target function we proxy to
	 */
	public BoxLangBIF( IClassRunnable target ) {
		this.target = target;
	}

	/**
	 * Constructor
	 */
	public BoxLangBIF() {
	}

	/**
	 * @return the target
	 */
	public IClassRunnable getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget( IClassRunnable target ) {
		this.target = target;
	}

	@Override
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		System.out.println( "BoxLangBIF.invoke() called" );
		System.out.println( "Arguments " + arguments.toString() );
		// Any AOP Stuff HERE if we need to
		return this.target.dereferenceAndInvoke( context, Key.invoke, arguments.asStruct(), false );
	}

}
