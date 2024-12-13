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
package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.RegexBuilder;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "ListQualify" )

public class ListQualify extends BIF {

	private static final String ELEMENTS_CHAR = "char";

	/**
	 * Constructor
	 */
	public ListQualify() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "string", Key.qualifier ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "string", Key.elements, "all" ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false )
		};
	}

	/**
	 * Inserts a string at the beginning and end of list elements.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to qualify.
	 *
	 * @argument.qualifier The string to insert at the beginning and end of each element.
	 *
	 * @argument.delimiter The delimiter used in the list.
	 *
	 * @argument.elements The elements to qualify. If set to "char", only elements that are all alphabetic characters will be qualified.
	 *
	 * @argument.includeEmptyFields If true, empty fields will be qualified.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	elements	= arguments.getAsString( Key.elements );
		String	qualifier	= arguments.getAsString( Key.qualifier );

		return ListUtil.asString(
		    ArrayCaster.cast(
		        ListUtil.asList(
		            arguments.getAsString( Key.list ),
		            arguments.getAsString( Key.delimiter ),
		            arguments.getAsBoolean( Key.includeEmptyFields ),
		            true
		        ).stream().map( item -> {

			        if ( elements.equals( ELEMENTS_CHAR ) ? RegexBuilder.of( StringCaster.cast( item ), RegexBuilder.ALPHA ).matches() : true ) {
				        return new StringBuilder( qualifier ).append( item ).append( qualifier ).toString();
			        } else {
				        return item;
			        }
		        } ).toArray()
		    ),
		    arguments.getAsString( Key.delimiter )
		);
	}

}
