
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

package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructAppend extends BIF {

	/**
	 * Constructor
	 */
	public StructAppend() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct1 ),
		    new Argument( true, "struct", Key.struct2 ),
		    new Argument( false, "boolean", Key.overwrite, true )
		};
	}

	/**
	 * Appends the contents of a second struct to the first struct either with or without overwrite
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct1 The target struct which will be the recipient of the appending
	 * 
	 * @argument.struct2 The struct containing the values to be appended
	 * 
	 * @argument.overwrite Default true. Whether to overwrite existing values found in struct1 from the values in struct2
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Boolean	overwrite	= arguments.getAsBoolean( Key.overwrite );
		IStruct	recipient	= arguments.getAsStruct( Key.struct1 );
		IStruct	assignments	= arguments.getAsStruct( Key.struct2 );

		if ( overwrite ) {
			recipient.putAll( assignments.getWrapped() );
		} else {
			assignments.entrySet().stream().forEach( entry -> recipient.putIfAbsent( entry.getKey(), entry.getValue() ) );
		}

		return arguments.getAsBoolean( __isMemberExecution )
		    ? recipient
		    : true;
	}

}
