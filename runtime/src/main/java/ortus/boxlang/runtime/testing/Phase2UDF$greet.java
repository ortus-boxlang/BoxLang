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
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.UDF;

/**
 * Phase 2 BoxLang
 * Example of UDF delcaration and execution
 */
public class Phase2UDF$greet extends UDF {

	private static Phase2UDF$greet			instance;

	/**
	 * The name of the function
	 */
	private final static Key				name		= Key.of( "greet" );

	/**
	 * The arguments of the function
	 */
	private final static Argument[]			arguments	= new Argument[] {
	    new Argument( true, "String", Key.of( "name" ), "Brad", "" )
	};

	/**
	 * The return type of the function
	 */
	private final static String				returnType	= "String";

	/**
	 * The hint of the function
	 */
	private final static String				hint		= "My Function Hint";

	/**
	 * Whether the function outputs
	 * TODO: Break CFML compat here?
	 */
	private final static boolean			output		= true;

	/**
	 * The access modifier of the function
	 */
	private Access							access		= Access.PUBLIC;

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

	public Access getAccess() {
		return access;
	}

	private Phase2UDF$greet() {
		super();
	}

	public static synchronized Phase2UDF$greet getInstance() {
		if ( instance == null ) {
			instance = new Phase2UDF$greet();
		}
		return instance;
	}

	/**
	 * <pre>
	    string function greet( required string name='Brad' ) hint="My Function Hint" {
	        local.race = "Local scope value";
	        arguments.race = "Arguments scope value";
	
	        var greeting = "Hello " & name;
	
	        // Reach "into" parent context and get "out" from variables scope
	        out.println( "Inside UDF, race scope lookup finds: " & race )
	
	        return greeting;
	    }
	 * </pre>
	 */
	@Override
	public Object _invoke( FunctionBoxContext context ) {

		// Create local.race and arguments.race to show scope lookup
		context.getScopeNearby( LocalScope.name ).put(
		    Key.of( "race" ),
		    "Local scope value"
		);

		context.getScopeNearby( ArgumentsScope.name ).put(
		    Key.of( "race" ),
		    "Arguments scope value"
		);

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
		        "Inside UDF, race scope lookup finds: " + context.scopeFindNearby( Key.of( "race" ), null ).value()
		    },
		    false
		);

		return context.scopeFindNearby( Key.of( "greeting" ), null ).value();
	}

}
