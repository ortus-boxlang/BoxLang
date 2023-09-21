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
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Lambda;

/**
 * Phase 2 BoxLang
 * Example of Lambda delcaration and execution
 */
public class Phase2Lambda$lambda1 extends Lambda {

	private static Phase2Lambda$lambda1		instance;
	/**
	 * The name of the function
	 */
	private final static Key				name		= Lambda.defaultName;

	/**
	 * The arguments of the function
	 */
	private static final Argument[]			arguments	= new Argument[] {
	    new Argument( true, "String", Key.of( "name" ), "Brad", "" )
	};

	/**
	 * The return type of the function
	 */
	private static final String				returnType	= "any";

	/**
	 * The hint of the function
	 */
	private static final String				hint		= "";

	/**
	 * Whether the function outputs
	 * TODO: Break CFML compat here?
	 */
	private static final boolean			output		= true;

	// TODO: cachedwithin, modifier, localmode, return format

	/**
	 * Additional abitrary metadata about this function.
	 */
	private static final Map<Key, Object>	metadata	= new HashMap<Key, Object>();

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

	private Phase2Lambda$lambda1() {
		super();
	}

	public static synchronized Phase2Lambda$lambda1 getInstance() {
		if ( instance == null ) {
			instance = new Phase2Lambda$lambda1();
		}
		return instance;
	}

	/*
	 * <pre>
	 * ( required string name='Brad' ) -> {
	 * var greeting = "Hello " & name;
	 * return greeting;
	 * }
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

		return context.scopeFindNearby( Key.of( "greeting" ), null ).value();
	}

}
