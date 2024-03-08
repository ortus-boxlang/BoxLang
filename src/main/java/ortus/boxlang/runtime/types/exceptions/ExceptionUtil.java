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
package ortus.boxlang.runtime.types.exceptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.operators.InstanceOf;
import ortus.boxlang.runtime.runnables.compiler.JavaBoxpiler;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

/**
 * This exception is thrown when a cast can't be done on any type
 */
public class ExceptionUtil {

	/**
	 * Checks if an exception is of a given type
	 *
	 * @param context The context
	 * @param e       The exception
	 * @param type    The type
	 *
	 * @return True if the exception is of the given type
	 */
	public static Boolean exceptionIsOfType( IBoxContext context, Throwable e, String type ) {
		// BoxLangExceptions check the type
		if ( e instanceof BoxLangException ble ) {
			// Either direct match to type, or "foo.bar" matches "foo.bar.baz
			if ( ble.type.equalsIgnoreCase( type ) || ble.type.toLowerCase().startsWith( type + "." ) )
				return true;
		}

		// Native exceptions just check the class hierarchy
		if ( InstanceOf.invoke( context, e, type ) ) {
			return true;
		}
		return false;
	}

	/**
	 * Throws a BoxLang exception or a passed in exception
	 *
	 * @param exception The exception to throw
	 */
	public static void throwException( Object exception ) {
		Object ex = DynamicObject.unWrap( exception );

		if ( ex instanceof RuntimeException runtimeException ) {
			throw runtimeException;
		} else if ( ex instanceof Throwable throwable ) {
			throw new CustomException( throwable.getMessage(), throwable );
		}

		if ( ex instanceof String string ) {
			throw new CustomException( string );
		} else {
			throw new BoxRuntimeException( "Cannot throw object of type [" + ex.getClass().getName() + "].  Must be a Throwable." );
		}
	}

	public static Array buildTagContext( Throwable e ) {
		Array		tagContext		= new Array();
		Throwable	cause			= e;
		boolean		isInComponent	= false;
		String		skipNext		= "";
		for ( StackTraceElement element : cause.getStackTrace() ) {
			String fileName = element.toString();
			if ( ( fileName.contains( "$cf" ) || fileName.contains( "$bx" ) )
			    // ._invoke means we're just executing the template or function. lambda$_invoke$ means we're in a lambda inside of that same tmeplate for
			    // function
			    && ( fileName.contains( "._invoke(" ) || ( isInComponent = fileName.contains( ".lambda$_invoke$" ) ) ) ) {
				// If we're just inside the nested lambda for a component, skip subssequent lines of the stack trace
				if ( !skipNext.isEmpty() ) {
					if ( fileName.startsWith( skipNext ) ) {
						continue;
					}
					skipNext = "";
				}
				// If this stack trace line was inside of a lambda, skip the next line(s) starting with this
				if ( isInComponent ) {
					// take entire string up until ".lambda$_invoke$"
					skipNext = fileName.substring( 0, fileName.indexOf( ".lambda$_invoke$" ) );
				}
				int		lineNo		= -1;
				String	BLFileName	= element.getClassName();
				var		sourceMap	= JavaBoxpiler.getInstance().getSourceMapFromFQN( element.getClassName() );
				if ( sourceMap != null ) {
					lineNo		= sourceMap.convertJavaLineToSourceLine( element.getLineNumber() );
					BLFileName	= sourceMap.getSource();
				}
				String	id	= "";
				Matcher	m	= Pattern.compile( ".*\\$Func_(.*)$" ).matcher( element.getClassName() );
				if ( m.find() ) {
					id = m.group( 1 ) + "()";
				}
				tagContext.add( Struct.of(
				    Key.codePrintHTML, "",
				    Key.codePrintPlain, "",
				    Key.column, -1,
				    Key.id, id,
				    Key.line, lineNo,
				    Key.Raw_Trace, element.toString(),
				    Key.template, BLFileName,
				    Key.type, "CFML"
				) );
			}
			isInComponent = false;
		}
		return tagContext;
	}
}
