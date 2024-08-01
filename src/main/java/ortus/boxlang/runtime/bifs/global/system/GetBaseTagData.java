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

import java.util.Arrays;
import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class GetBaseTagData extends BIF {

	/**
	 * Constructor
	 */
	public GetBaseTagData() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.tagName, Set.of( Validator.NON_EMPTY ) ),
		    new Argument( true, "integer", Key.ancestorLevels, 1, Set.of( Validator.min( 1 ) ) )
		};
	}

	/**
	 * Used within a custom tag. Finds calling (ancestor) tag by name and accesses its data.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	tagName			= arguments.getAsString( Key.tagName );
		Integer	ancestorLevels	= arguments.getAsInteger( Key.ancestorLevels );

		// if tagname as _, strip token before first _
		if ( tagName.contains( "_" ) ) {
			tagName = tagName.substring( tagName.indexOf( "_" ) + 1 );
		}
		Key	tagKey		= Key.of( tagName );

		var	components	= Arrays
		    .asList( context.getComponents() )
		    .stream()
		    .filter( s -> s.get( Key._NAME ).equals( Key.module ) && s.get( Key.customTagName ).equals( tagKey ) )
		    .toList();
		if ( components.isEmpty() ) {
			throw new BoxValidationException( tagName + " not found" );
		}
		if ( ancestorLevels > components.size() ) {
			throw new BoxValidationException( "ancestorLevels is greater than the number of ancestor tags" );
		}
		IStruct component = components.get( ancestorLevels - 1 );
		return Struct.of(
		    Key.caller, component.get( Key.caller ),
		    Key.thisTag, component.get( Key.thisTag ),
		    Key.attributes, component.getAsStruct( Key.attributes ).get( Key.attributes )
		);
	}
}
