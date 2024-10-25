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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import ortus.boxlang.compiler.DiskClassUtil;
import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ThrowableCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.operators.InstanceOf;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
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
		if ( type.equalsIgnoreCase( "any" ) ) {
			return true;
		}
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

		if ( ex instanceof String string ) {
			throw new CustomException( string );
		} else {
			Throwable t = ThrowableCaster.cast( ex );
			if ( t instanceof RuntimeException runtimeException ) {
				throw runtimeException;
			} else {
				throw new CustomException( t.getMessage(), t );
			}
		}
	}

	/**
	 * Print the stack trace of an exception to the given PrintStream
	 *
	 * @param e   The exception
	 * @param out The target print stream
	 */
	public static void printBoxLangStackTrace( Throwable e, PrintStream out ) {
		StringWriter		sw			= new StringWriter();
		PrintWriter			pw			= new PrintWriter( sw );
		Array				tagContext	= buildTagContext( e );
		StackTraceElement[]	elements	= e.getStackTrace();

		pw.println( e.getClass().getName() + ": " + e.getMessage() );

		for ( int i = 0; i < elements.length; i++ ) {
			final int j = i;
			tagContext.stream()
			    .filter( ( tc ) -> {
				    Struct context = ( Struct ) tc;

				    return context.containsKey( Key.depth ) && context.getAsInteger( Key.depth ) == j;
			    } )
			    .findFirst()
			    .ifPresentOrElse(
			        ( tc ) -> {
				        Struct context = ( Struct ) tc;
				        pw.println( "\t" + context.getAsString( Key.template ) + ":" + context.get( Key.line ) );
			        },
			        () -> {
				        pw.println( "\t" + elements[ j ].toString() );
			        } );
		}

		out.println( sw.toString() );
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
		Array											tagContext				= new Array();
		boolean											isInComponent			= false;
		String											skipNext				= "";
		boolean											argumentDefaultValue	= false;
		int												i						= -1;
		LinkedHashMap<Throwable, StackTraceElement[]>	stacks					= getMergedStackTrace2( e );
		for ( var stack : stacks.entrySet() ) {
			Array		thisTagContext	= new Array();
			Throwable	cause			= stack.getKey();
			for ( StackTraceElement element : stack.getValue() ) {
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
				    // _pseudoConstructor means we're in a class pseudoconstructor, ._invoke means we're executing the template or function. lambda$_invoke$ means we're in a lambda inside of that same tmeplate for
				    // function. argumentDefaultValue is true when this is next stack AFTER a call to Argument.getDefaultValue()
				    && ( fileName.contains( "._pseudoConstructor(" ) || fileName.contains( "._invoke(" )
				        || ( isInComponent = fileName.contains( ".lambda$_invoke$" ) ) || argumentDefaultValue ) ) {
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
					var		sourceMap	= JavaBoxpiler.getInstance().getSourceMapFromFQN( IBoxpiler.getBaseFQN( element.getClassName() ) );
					if ( sourceMap != null ) {
						lineNo		= sourceMap.convertJavaLineToSourceLine( element.getLineNumber() );
						BLFileName	= sourceMap.getSource();
					}
					String	id	= "";
					Matcher	m	= Pattern.compile( ".*\\$Func_(.*)$" ).matcher( element.getClassName() );
					if ( m.find() ) {
						id = m.group( 1 ) + "()";
					}
					thisTagContext.add( Struct.of(
					    Key.codePrintHTML, getSurroudingLinesOfCode( BLFileName, lineNo, true ),
					    Key.codePrintPlain, getSurroudingLinesOfCode( BLFileName, lineNo, false ),
					    Key.column, -1,
					    Key.id, id,
					    Key.line, lineNo,
					    Key.Raw_Trace, element.toString(),
					    Key.template, BLFileName,
					    Key.type, "BL",
					    Key.depth, i
					) );
					if ( depth > 0 && tagContext.size() >= depth ) {
						break;
					}
				}
				isInComponent = false;
			}
			// If this is a parse exception or Expression Exception, then add one more frame on the context for the line where the parsing error occurred
			Position position = null;
			if ( cause instanceof ParseException pe ) {
				if ( pe.hasIssues() ) {
					position = pe.getIssues().get( 0 ).getPosition();
				}
			} else if ( cause instanceof ExpressionException ee ) {
				position = ee.getPosition();
			}
			if ( position != null ) {
				String	fileName		= "";
				String	codePrintHTML	= "";
				String	codePrintPlain	= "";
				if ( position.getSource() != null ) {
					if ( position.getSource() instanceof SourceFile sf ) {
						fileName = sf.getFile().toString();
					}
					codePrintHTML	= position.getSource().getSurroundingLines( position.getStart().getLine(), true );
					codePrintPlain	= position.getSource().getSurroundingLines( position.getStart().getLine(), false );
				}
				thisTagContext.add( 0, Struct.of(
				    Key.codePrintHTML, codePrintHTML,
				    Key.codePrintPlain, codePrintPlain,
				    Key.column, position.getStart().getColumn(),
				    Key.id, "",
				    Key.line, position.getStart().getLine(),
				    Key.Raw_Trace, "",
				    Key.template, fileName,
				    Key.type, "BL"
				) );
			}
			tagContext.addAll( thisTagContext );
		}
		return tagContext;
	}

	/**
	 * Utility to get the surrounding lines of code for a given line number in a file
	 *
	 * @param fileName The file name
	 * @param lineNo   The line number
	 * @param html     True if the output should be HTML
	 *
	 * @return The surrounding lines of code
	 */
	private static String getSurroudingLinesOfCode( String fileName, int lineNo, boolean html ) {
		// read file, if exists, and return the surrounding lines of code, 2 before and 2 after
		File srcFile = new File( fileName );
		if ( srcFile.exists() ) {

			// If this is a pre-compiled source file, then we can't read it
			if ( new DiskClassUtil( null ).isJavaBytecode( srcFile ) ) {
				return "Precompiled source not available.";
			}

			try {
				List<String>	lines		= Files.readAllLines( srcFile.toPath() );
				int				startLine	= Math.max( 1, lineNo - 2 );
				int				endLine		= Math.min( lines.size(), lineNo + 2 );

				StringBuilder	codeSnippet	= new StringBuilder();
				for ( int i = startLine; i <= endLine; i++ ) {
					String theLine = StringEscapeUtils.escapeHtml4( lines.get( i - 1 ) );
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

	/**
	 * This method will allow you to get the position in code of where an execution of it takes place.
	 *
	 * @return The String representation of the position in code > {@code templatePath:lineNumber}
	 */
	public static String getCurrentPositionInCode() {
		Array tagContext = getTagContext( 1 );
		if ( !tagContext.isEmpty() ) {
			IStruct thisTag = ( IStruct ) tagContext.get( 0 );
			return thisTag.getAsString( Key.template ) + ":" + thisTag.get( Key.line );
		}
		return "[not found]";
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

	/**
	 * Get the stack trace as a string from the Throwable
	 *
	 * @param e The exception
	 *
	 * @return The stack trace as a string
	 */
	public static String getStackTraceAsString( Throwable e ) {
		StringWriter	sw	= new StringWriter();
		PrintWriter		pw	= new PrintWriter( sw );
		e.printStackTrace( pw );
		return sw.toString();
	}

	/**
	 * Get the merged stack trace from the Throwable
	 *
	 * @param cause The exception
	 *
	 * @return The merged stack trace
	 */
	public static StackTraceElement[] getMergedStackTrace( Throwable cause ) {
		List<StackTraceElement>	merged		= new ArrayList<>();
		StackTraceElement[]		elements	= cause.getStackTrace();
		merged.addAll( Arrays.asList( elements ) );

		Throwable parent = cause.getCause();
		while ( parent != null ) {
			elements = parent.getStackTrace();
			int	i	= merged.size() - 1;
			int	j	= elements.length - 1;

			// Find the number of common elements from the end of the stack traces
			while ( i >= 0 && j >= 0 && merged.get( i ).equals( elements[ j ] ) ) {
				i--;
				j--;
			}

			// Add the unique elements to the list
			for ( int k = j; k >= 0; k-- ) {
				merged.add( i + 1, elements[ k ] );
			}

			parent = parent.getCause();
		}

		// Convert the list to an array
		return merged.toArray( new StackTraceElement[ 0 ] );
	}

	/**
	 * Get the merged stack trace from the Throwable
	 *
	 * @param cause The exception
	 *
	 * @return The merged stack trace
	 */
	public static LinkedHashMap<Throwable, StackTraceElement[]> getMergedStackTrace2( Throwable cause ) {
		LinkedHashMap<Throwable, StackTraceElement[]>	map		= new LinkedHashMap<>();

		Throwable										current	= cause;
		while ( current != null ) {
			StackTraceElement[]		elements	= current.getStackTrace();
			List<StackTraceElement>	merged		= new ArrayList<>( Arrays.asList( elements ) );

			Throwable				parent		= current.getCause();
			if ( parent != null ) {
				StackTraceElement[]	parentElements	= parent.getStackTrace();
				int					i				= elements.length - 1;
				int					j				= parentElements.length - 1;

				// Find the number of common elements from the end of the stack traces
				while ( i >= 0 && j >= 0 && elements[ i ].equals( parentElements[ j ] ) ) {
					i--;
					j--;
				}

				// Remove the common elements from the end of the list
				merged = merged.subList( 0, i + 1 );
			}

			// Add the unique elements to the map
			map.put( current, merged.toArray( new StackTraceElement[ 0 ] ) );

			current = parent;
		}

		return map;
	}

	/**
	 * Convert a Throwable to a Struct
	 *
	 * @param target The Throwable
	 *
	 * @return The Struct
	 */
	public static IStruct throwableToStruct( Throwable target ) {
		if ( target == null ) {
			return null;
		}
		IStruct result = Struct.of(
		    Key.message, target.getMessage(),
		    Key.stackTrace, ExceptionUtil.getStackTraceAsString( target ),
		    Key.tagContext, ExceptionUtil.buildTagContext( target ),
		    Key.cause, throwableToStruct( target.getCause() )
		);
		if ( target instanceof BoxLangException ble ) {
			result.addAll( ble.dataAsStruct() );
		}
		return result;
	}
}
