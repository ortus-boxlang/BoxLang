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
package ortus.boxlang.runtime.testing;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Closure;

/**
 * Phase 2 BoxLang
 * Example of UDF delcaration and execution
 */
public class Phase2Closure$closure1 extends Closure {

	// TODO: Was not working.
	public Phase2Closure$closure1( IBoxContext declaringContext ) {
		super( declaringContext );
		// super(
		// new Argument[] {
		// new Argument( true, "String", Key.of( "name" ), "Brad", "" )
		// },
		// declaringContext
		// );
	}

	/**
	 * <pre>
	    ( required string name='Brad' ) => {
	        var greeting = "Hello " & name;
	
	        out.println( "Inside Closure, outside lookup finds: " & outside )
	
	        return greeting;
	    }
	 * </pre>
	 */
	@Override
	public Object invoke( FunctionBoxContext context ) {

		context.getScopeNearby( LocalScope.name ).assign(
		    Key.of( "Greeting" ),
		    Concat.invoke(
		        "Hello ",
		        context.scopeFindNearby( Key.of( "name" ), null ).value()
		    )
		);

		// Reach "into" parent context and get "out" from variables scope
		Referencer.getAndInvoke(
		    context,
		    // Object
		    context.scopeFindNearby( Key.of( "out" ), null ).value(),
		    // Method
		    Key.of( "println" ),
		    // Arguments
		    new Object[] {
		        "Inside Closure, outside lookup finds: " + context.scopeFindNearby( Key.of( "outside" ), null ).value()
		    },
		    false
		);

		return context.scopeFindNearby( Key.of( "greeting" ), null ).value();
	}

}
