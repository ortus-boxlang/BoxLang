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
package com.ortussolutions.components;

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = false )
public class ExampleComponent extends Component {

	static Key	locationKey	= Key.of( "location" );
	static Key	shoutKey	= Key.of( "shout" );

	public ExampleComponent() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string", Set.of( Validator.REQUIRED ) ),
		    new Attribute( locationKey, "string", "world", Set.of( Validator.REQUIRED, Validator.valueOneOf( "world", "universe" ) ) ),
		    new Attribute( shoutKey, "boolean", false, Set.of( Validator.REQUIRED ) ),
		};
	}

	/**
	 * An example component that says hello
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.name The name of the person greeting us.
	 * 
	 * @attribute.location The location of the person.
	 * 
	 * @attribute.shout Whether the person is shouting or not.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String			name		= attributes.getAsString( Key._NAME );
		String			location	= attributes.getAsString( locationKey );
		Boolean			shout		= attributes.getAsBoolean( shoutKey );

		StringBuilder	sb			= new StringBuilder();
		String			greeting	= sb.append( "Hello, " ).append( location ).append( " - from " ).append( name ).append( "." ).toString();
		context.writeToBuffer( shout ? greeting.toUpperCase() : greeting );

		return DEFAULT_RETURN;
	}
}
