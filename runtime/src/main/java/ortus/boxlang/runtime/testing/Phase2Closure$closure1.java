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

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Closure;

/**
 * Phase 2 BoxLang
 * Example of Closure delcaration and execution
 */
public class Phase2Closure$closure1 extends Closure {

	/**
	 * The name of the function
	 */
	private final static Key				name		= Closure.defaultName;

	/**
	 * The arguments of the function
	 */
	private final static Argument[]			arguments	= new Argument[] {
	    new Argument( true, "String", Key.of( "name" ), "Brad", "" )
	};

	/**
	 * The return type of the function
	 */
	private final static String				returnType	= "any";

	/**
	 * The hint of the function
	 */
	private final static String				hint		= "";

	/**
	 * Whether the function outputs
	 * TODO: Break CFML compat here?
	 */
	private final static boolean			output		= true;

	// TODO: cachedwithin, modifier, localmode, return format

	/**
	 * Additional abitrary metadata about this function.
	 */
	private final static Map<Key, Object>	metadata	= new HashMap<Key, Object>();

	public Key getName() {
		return name;
	}

	public Argument[] getArguments() {
		return arguments;
	}

	public String getReturnType() {
		return returnType;
	}

	public String getHint() {
		return hint;
	}

	public boolean isOutput() {
		return output;
	}

	public Map<Key, Object> getMetadata() {
		return metadata;
	}

	public Phase2Closure$closure1( IBoxContext declaringContext ) {
		super( declaringContext );
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
	public Object _invoke( FunctionBoxContext context ) {

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
