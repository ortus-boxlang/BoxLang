
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

package ortus.boxlang.runtime.bifs.global.string;

import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "ReplaceList" )

public class ReplaceList extends BIF {

	private static final Key	list1Key	= Key.of( "list1" );
	private static final Key	list2Key	= Key.of( "list2" );
	private static final Key	delims1Key	= Key.of( "delimiter_list1" );
	private static final Key	delims2Key	= Key.of( "delimiter_list2" );

	/**
	 * Constructor
	 */
	public ReplaceList() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "string", list1Key ),
		    new Argument( true, "string", list2Key ),
		    new Argument( false, "string", delims1Key, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "string", delims2Key, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false )
		};
	}

	/**
	 * Replaces occurrences of the elements from a delimited list, in a string with corresponding elements from another delimited list.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to operate on
	 *
	 * @argument.list1 The first delimited list of search values
	 *
	 * @argument.list2 The second delimited list of replacement values
	 * 
	 * @argument.delimiter_list1 The delimiters for list 1
	 * 
	 * @argument.delimiter_list2 The delimiters for list 2
	 * 
	 * @argument.includeEmptyFields Whether to include empty fields in the final result
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String								ref			= arguments.getAsString( Key.string );
		Array								list1		= ListUtil.asList(
		    arguments.getAsString( list1Key ),
		    arguments.getAsString( delims1Key ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    true
		);
		Array								list2		= ListUtil.asList(
		    arguments.getAsString( list2Key ),
		    arguments.getAsString( delims1Key ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    true
		);

		BiFunction<Object, Integer, Object>	reduction	= ( acc, idx ) -> {
															if ( list2.size() >= idx + 1 ) {
																return StringUtils.replace( StringCaster.cast( acc ), StringCaster.cast( list1.get( idx ) ),
																    StringCaster.cast( list2.get( idx ) ) );
															} else if ( list2.size() > 0 ) {
																return StringUtils.replaceChars(
																    StringCaster.cast( acc ),
																    ListUtil.asString( list1, arguments.getAsString( delims1Key ) ),
																    ListUtil.asString( list2, arguments.getAsString( delims2Key ) )
																);
															} else {
																return acc;
															}
														};

		return list1.intStream().boxed().reduce(
		    ref,
		    reduction,
		    ( acc, intermediate ) -> acc
		);
	}

}
