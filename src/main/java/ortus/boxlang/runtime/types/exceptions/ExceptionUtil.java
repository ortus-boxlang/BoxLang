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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
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

	/**
	 * Get the tag context from an exception. Passing an depth of -1 will return the entire tag context
	 * Passing a non-zero depth will return that many tags from the tag context
	 * 
	 * @param e     The exception
	 * @param depth The depth of the tag context
	 * 
	 * @return The tag context array
	 */
	public static Array buildTagContext( Throwable e, int depth ) {
		Array		tagContext				= new Array();
		Throwable	cause					= e;
		boolean		isInComponent			= false;
		String		skipNext				= "";
		boolean		argumentDefaultValue	= false;
		int			i						= -1;
		for ( StackTraceElement element : cause.getStackTrace() ) {
			i++;
			argumentDefaultValue = false;
			// test next element in array
			if ( i < cause.getStackTrace().length - 1 ) {
				// check if next element is Argument.getDefaultValue()
				if ( cause.getStackTrace()[ i + 1 ].toString().contains( "ortus.boxlang.runtime.types.Argument.getDefaultValue" ) ) {
					argumentDefaultValue = true;
				}
			}
			String fileName = element.toString();
			if ( ( fileName.contains( "$cf" ) || fileName.contains( "$bx" ) )
			    // ._invoke means we're just executing the template or function. lambda$_invoke$ means we're in a lambda inside of that same tmeplate for
			    // function. argumentDefaultValue is true when this is next stack AFTER a call to Argument.getDefaultValue()
			    && ( fileName.contains( "._invoke(" ) || ( isInComponent = fileName.contains( ".lambda$_invoke$" ) ) || argumentDefaultValue ) ) {
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
				    // TODO: Improve this to read the file once and generate both HTML and plain text at the same time
				    Key.codePrintHTML, getSurroudingLinesOfCode( BLFileName, lineNo, true ),
				    Key.codePrintPlain, getSurroudingLinesOfCode( BLFileName, lineNo, false ),
				    Key.column, -1,
				    Key.id, id,
				    Key.line, lineNo,
				    Key.Raw_Trace, element.toString(),
				    Key.template, BLFileName,
				    Key.type, "CFML"
				) );
				if ( depth > 0 && tagContext.size() >= depth ) {
					break;
				}
			}
			isInComponent = false;
		}
		return tagContext;
	}

	private static String getSurroudingLinesOfCode( String fileName, int lineNo, boolean html ) {
		// read file, if exists, and return the surrounding lines of code, 2 before and 2 after
		File srcFile = new File( fileName );
		if ( srcFile.exists() ) {
			// ...

			try {
				List<String>	lines		= Files.readAllLines( srcFile.toPath() );
				int				startLine	= Math.max( 1, lineNo - 2 );
				int				endLine		= Math.min( lines.size(), lineNo + 2 );

				StringBuilder	codeSnippet	= new StringBuilder();
				for ( int i = startLine; i <= endLine; i++ ) {
					String theLine = escapeHTML( lines.get( i - 1 ) );
					if ( i == lineNo && html ) {
						codeSnippet.append( "<b>" ).append( i ).append( ": " ).append( theLine ).append( "</b>" ).append( "<br>" );
					} else {
						codeSnippet.append( i ).append( ": " ).append( theLine ).append( html ? "<br>" : "\n" );
					}
				}

				return codeSnippet.toString();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
		return "";
	}

	private static String escapeHTML( String s ) {
		if ( s == null ) {
			return "";
		}
		return s.replace( "<", "&lt;" ).replace( ">", "&gt;" );
	}

	/**
	 * Get the tag context from an exception.
	 * 
	 * @param e The exception
	 * 
	 * @return The tag context array
	 */
	public static Array buildTagContext( Throwable e ) {
		return buildTagContext( e, -1 );
	}

	/**
	 * Get the tag context from the current execution context.
	 * 
	 * @return The tag context array
	 */
	public static Array getTagContext() {
		return buildTagContext( new Exception(), -1 );
	}

	/**
	 * Get the tag context from the current execution context. Passing an depth of -1 will return the entire tag context
	 * Passing a non-zero depth will return that many tags from the tag context
	 * 
	 * @param depth The depth of the tag context
	 * 
	 * @return The tag context array
	 */
	public static Array getTagContext( int depth ) {
		return buildTagContext( new Exception(), depth );
	}

	public static String getStackTraceAsString( Throwable e ) {
		StringWriter	sw	= new StringWriter();
		PrintWriter		pw	= new PrintWriter( sw );
		e.printStackTrace( pw );
		return sw.toString();
	}
}
