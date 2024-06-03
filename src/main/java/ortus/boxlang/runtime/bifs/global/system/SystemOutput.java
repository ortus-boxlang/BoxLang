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
package ortus.boxlang.runtime.bifs.global.system;

import java.io.PrintStream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IType;

// TODO: Move to compat module
@BoxBIF
public class SystemOutput extends BIF {

	private static final Key	addNewLineKey		= Key.of( "addNewLine" );
	private static final Key	doErrorStreamKey	= Key.of( "doErrorStream" );

	/**
	 * Constructor
	 */
	public SystemOutput() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.obj ),
		    new Argument( true, "boolean", addNewLineKey, false ),
		    new Argument( false, "boolean", doErrorStreamKey, false )
		};
	}

	/**
	 * Writes the given object to the output stream
	 *
	 * 
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.obj The object to write to the output stream
	 * 
	 * @argument.addNewLine If true, a new line will be added to the output stream
	 * 
	 * @argument.doErrorStream If true, the object will be written to the error stream
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	obj				= arguments.get( Key.obj );
		Boolean	addNewLine		= arguments.getAsBoolean( addNewLineKey );
		Boolean	doErrorStream	= arguments.getAsBoolean( doErrorStreamKey );
		if ( obj instanceof IType t ) {
			obj = t.asString();
		}
		PrintStream			stream;
		RequestBoxContext	rCon	= context.getParentOfType( RequestBoxContext.class );
		if ( rCon != null ) {
			stream = rCon.getOut();
		} else {
			stream = System.out;
		}
		if ( doErrorStream ) {
			stream = System.err;
		}
		if ( addNewLine ) {
			stream.println( obj );
		} else {
			stream.print( obj );
		}
		return null;
	}
}
