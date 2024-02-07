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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

@BoxBIF
public class Throw extends BIF {

	/**
	 * Constructor
	 */
	public Throw() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "String", Key.message ),
		    new Argument( false, "String", Key.type ),
		    new Argument( false, "String", Key.detail ),
		    new Argument( false, "String", Key.errorcode ),
		    new Argument( false, "any", Key.extendedinfo ),
		    new Argument( false, "any", Key.object )
		};
	}

	/**
	 * Throws a developer-specified exception, which can be caught with a catch block.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.message Message that describes exception event
	 * 
	 * @argument.type The type of the exception
	 * 
	 * @argument.detail Description of the event
	 * 
	 * @argument.errorcode A custom error code that you supply
	 * 
	 * @argument.extendedinfo Additional custom error data that you supply
	 * 
	 * @argument.object An instance of an exception object. If there is no message provided, this object will be thrown directly. If there is a message, a
	 *                  CustomException will be thrown and this object will be used as the cause.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Throwable	cause			= null;
		Throwable	exceptionToThrow;
		String		message			= arguments.getAsString( Key.message );
		String		detail			= arguments.getAsString( Key.detail );
		String		errorcode		= arguments.getAsString( Key.errorcode );
		String		type			= arguments.getAsString( Key.type );
		Object		extendedinfo	= arguments.get( Key.extendedinfo );
		Object		oCause			= DynamicObject.unWrap( arguments.get( Key.object ) );
		if ( oCause != null ) {
			if ( oCause instanceof Throwable t ) {
				cause = t;
			} else {
				throw new BoxRuntimeException(
				    "Cannot throw exception object of type " + oCause.getClass().getName() + " as it is not an instance of Throwable" );
			}
		}
		if ( message == null && cause != null ) {
			exceptionToThrow = cause;
		} else {
			exceptionToThrow = new CustomException(
			    message,
			    detail,
			    errorcode,
			    type == null ? "Custom" : type,
			    extendedinfo,
			    cause
			);
		}
		ExceptionUtil.throwException( exceptionToThrow );
		return null;
	}

}
