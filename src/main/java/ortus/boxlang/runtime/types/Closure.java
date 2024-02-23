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
package ortus.boxlang.runtime.types;

import java.util.Objects;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Represents a closure, which is a function, but has less data than a UDF and also retains a reference to the declaring context.
 */
public abstract class Closure extends Function {

	public static final Key	defaultName	= Key.of( "Closure" );

	/**
	 * The context in which this closure was declared.
	 */
	private IBoxContext		declaringContext;

	/**
	 * Constructor
	 */
	protected Closure( IBoxContext declaringContext ) {
		super();
		Objects.requireNonNull( declaringContext, "A Closure's declaring context cannot be null." );
		this.declaringContext = declaringContext;
	}

	/**
	 * Get the context in which this closure was declared.
	 *
	 * @return the context.
	 */
	public IBoxContext getDeclaringContext() {
		return declaringContext;
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 *
	 * @return true if there is an IClassRunnable at the top of the template stack
	 */
	public boolean isInClass() {
		return false;
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 *
	 * @return the IClassRunnable this context is executing in, or null if not in a class
	 */
	public IClassRunnable getThisClass() {
		return null;
	}

}
