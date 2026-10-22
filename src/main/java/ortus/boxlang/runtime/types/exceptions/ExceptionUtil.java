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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import ortus.boxlang.compiler.DiskClassUtil;
import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.SourceMap;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCasterLoose;
import ortus.boxlang.runtime.dynamic.casters.ThrowableCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.operators.InstanceOf;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This exception is thrown when a cast can't be done on any type
 */
public class ExceptionUtil {

	private static final int	MAX_CAUSE_DEPTH				= 50;
	private static BoxRuntime	runtime						= BoxRuntime.getInstance();

	/**
	 * Name of the runtime cache that holds the rendered code snippets a tag context shows
	 */
	private static final Key	CODE_SNIPPETS_CACHE			= Key.of( "bxCodeSnippets" );

	/**
	 * Message rendered in place of the source snippet when we can't read the source of a precompiled file
	 */
	private static final String	PRECOMPILED_SOURCE_MESSAGE	= "Precompiled source not available.";

	public static final String	LICENSE_MODULE_NAME			= "bx-plus";
	public static final String	LICENSE_SUBSCRIPTION_NAME	= "BoxLang+";

	/**
	 * Checks if an exception is of a given type using loose rules based on the type as a string.
	 * This takes into account the "type" string of BoxLang exceptions as well as the class name of native exceptions.
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
		// Check the exception and all of its causes
		Throwable	firstCause	= null;
		int			causeDepth	= 0;
		while ( e != null && causeDepth++ < MAX_CAUSE_DEPTH && e != firstCause ) {
			if ( firstCause == null ) {
				firstCause = e;
			}
			// BoxLangExceptions check the type
			if ( e instanceof BoxLangException ble ) {
				// Either direct match to type, or "foo.bar" matches "foo.bar.baz
				if ( ble.type.equalsIgnoreCase( type ) || ble.type.toLowerCase().startsWith( type.toLowerCase() + "." ) )
					return true;
			}

			// Native exceptions just check the class hierarchy
			if ( InstanceOf.invoke( context, e, type ) ) {
				return true;
			}
			e = e.getCause();
		}
		return false;
	}

	/**
	 * Check if an exception, or its cause is an InterruptedException.
	 * This is a pure Java check and does not take BoxLang runtime exception types into account; it only checks for Java's InterruptedException.
	 * 
	 * @param exception The exception
	 */
	public static boolean isInterruptedException( Throwable exception ) {
		Throwable	e			= exception;
		Throwable	firstCause	= null;
		int			causeDepth	= 0;
		while ( e != null && causeDepth++ < MAX_CAUSE_DEPTH && e != firstCause ) {
			if ( firstCause == null ) {
				firstCause = e;
			}
			if ( e instanceof InterruptedException ) {
				return true;
			}
			e = e.getCause();
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
		StackTraceElement[]	elements	= getMergedStackTrace( e );

		Throwable			current		= e;
		String				indent		= "";
		Throwable			firstCause	= null;
		int					causeDepth	= 0;
		while ( current != null && causeDepth++ < MAX_CAUSE_DEPTH && current != firstCause ) {
			if ( firstCause == null ) {
				firstCause = current;
			}
			pw.println( indent + current.getClass().getName() + ": " + current.getMessage() );
			indent	+= "  ";
			current	= current.getCause();
		}
		pw.println();
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
		boolean											debugMode				= runtime.inDebugMode();
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

				String	fileName	= element.toString();
				String	methodName	= element.getMethodName();
				if ( isCompiledSource( fileName )
				    // _pseudoConstructor means we're in a class pseudoconstructor, ._invoke means we're executing the template or function.
				    // invokeFunction_Xxx, invokeLambda_N, invokeClosure_N are the static method patterns for functions/lambdas/closures.
				    // componentBody_N is the static method pattern for component bodies.
				    // componentIfBranch_N and _split_N are ASM helper methods generated from source nodes.
				    // lambda$_invoke$ means we're in a lambda inside of that same template (Java boxpiler).
				    // argumentDefaultValue is true when this is next stack AFTER a call to Argument.getDefaultValue()
				    && ( fileName.contains( "._pseudoConstructor(" ) || fileName.contains( "._invoke(" )
				        || methodName.startsWith( IBoxpiler.INVOKE_FUNCTION_PREFIX ) || methodName.startsWith( "invokeLambda_" )
				        || methodName.startsWith( "invokeClosure_" )
				        || ( isInComponent = isComponentBody( methodName ) ) || isGeneratedSourceHelper( methodName ) || argumentDefaultValue ) ) {

					// If we're just inside the nested lambda for a component, skip subssequent lines of the stack trace
					if ( !skipNext.isEmpty() ) {
						if ( fileName.startsWith( skipNext ) ) {
							continue;
						}
						skipNext = "";
					}

					// If this stack trace line was inside of a component body lambda (Java boxpiler), skip subsequent lines
					if ( isInComponent && fileName.contains( ".lambda$_invoke$" ) ) {
						skipNext = fileName.substring( 0, fileName.indexOf( ".lambda$_invoke$" ) );
					}

					int		lineNo		= element.getLineNumber();
					String	BLFileName	= element.getClassName();

					if ( element.getFileName() != null ) {
						BLFileName = element.getFileName();
					}

					IBoxpiler	boxpiler	= RunnableLoader.getInstance().getBoxpiler();
					SourceMap	sourceMap	= null;

					if ( boxpiler.getName().equals( Key.java ) ) {
						sourceMap = boxpiler.getSourceMapFromFQN( IBoxpiler.getBaseFQN( element.getClassName() ) );
					}

					if ( sourceMap != null ) {
						lineNo		= sourceMap.convertJavaLineToSourceLine( element.getLineNumber() );
						BLFileName	= sourceMap.getSource();
					}

					String	functionName	= "";
					String	id				= "";

					// Static method pattern: invokeFunction_Xxx, invokeLambda_N, invokeClosure_N
					if ( methodName.startsWith( IBoxpiler.INVOKE_FUNCTION_PREFIX ) ) {
						functionName	= IBoxpiler.unsanitizeFromJavaIdentifier( methodName.substring( IBoxpiler.INVOKE_FUNCTION_PREFIX.length() ) );
						id				= id + "()";
					} else if ( methodName.startsWith( "invokeLambda_" ) || methodName.startsWith( "invokeClosure_" ) ) {
						functionName	= "";
						id				= id + "()";
					}

					// The code snippets are only gathered in debug mode
					CodeSnippet codeSnippet = debugMode ? getSurroundingLinesOfCode( BLFileName, lineNo ) : CodeSnippet.EMPTY;

					thisTagContext.add( Struct.of(
					    Key.codePrintHTML, codeSnippet.html,
					    Key.codePrintPlain, codeSnippet.plain,
					    Key.column, -1,
					    Key.id, id,
					    Key.function, functionName,
					    Key.line, lineNo,
					    Key.lineNumber, lineNo,
					    Key.Raw_Trace, element.toString(),
					    Key.template, BLFileName,
					    Key.type, "BL",
					    Key.depth, i
					) );

					//
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
				    Key.lineNumber, position.getStart().getLine(),
				    Key.Raw_Trace, "",
				    Key.template, fileName == null ? "" : fileName,
				    Key.type, "BL"
				) );
			}
			tagContext.addAll( thisTagContext );
		}
		return tagContext;
	}

	/**
	 * Check if the given file name is a compiled source
	 * 
	 * @param fileName The file name
	 * 
	 * @return True if the file name indicates a compiled source file, false otherwise
	 */
	private static boolean isCompiledSource( String fileName ) {
		return fileName.contains( "$cf" ) || fileName.contains( "$bx" ) || fileName.startsWith( "boxgenerated.scripts.Script__" )
		    || fileName.startsWith( "boxgenerated.scripts.Script.Statement__" );
	}

	/**
	 * Check if the given method name is a component body invocation.
	 * Java boxpiler uses lambda$_invoke$N, ASM boxpiler uses componentBody_N static methods.
	 * 
	 * @param methodName The method name
	 * 
	 * @return True if the method name indicates a component body invocation, false otherwise
	 */
	private static boolean isComponentBody( String methodName ) {
		return methodName.startsWith( "componentBody_" ) || methodName.startsWith( "lambda$_invoke$" );
	}

	private static boolean isGeneratedSourceHelper( String methodName ) {
		return methodName.startsWith( "componentIfBranch_" ) || methodName.startsWith( "_split_" );
	}

	/**
	 * Utility to get the surrounding lines of code for a given line number in a file, 2 before and 2 after,
	 * rendered as both HTML and plain text. Only the lines being rendered are ever read from disk, and the
	 * result is cached per file + line so repeated dumps of the same exception don't touch the file system.
	 *
	 * @param fileName The file name
	 * @param lineNo   The line number
	 *
	 * @return The rendered code snippet
	 */
	static CodeSnippet getSurroundingLinesOfCode( String fileName, int lineNo ) {
		if ( fileName == null || fileName.isEmpty() ) {
			return CodeSnippet.EMPTY;
		}

		File			srcFile			= new File( fileName );
		long			lastModified	= srcFile.lastModified();
		String			cacheKey		= fileName + ":" + lineNo;
		ICacheProvider	cache			= getCodeSnippetCache();

		if ( cache != null ) {
			Object cached = cache.get( cacheKey ).orElse( null );
			if ( cached instanceof CodeSnippet snippet ) {
				// The entry is good while the local file isn't newer than the file it was rendered from. Only
				// requiring that the file isn't newer lets a shared cache be used between servers whose files
				// were deployed at different times.
				if ( lastModified > 0 && lastModified <= snippet.lastModified ) {
					return snippet;
				}
				// The file changed, so every snippet cached for it is stale, not just this line. Discard them.
				purgeCachedSnippets( cache, fileName );
			}
		}

		CodeSnippet rendered = renderSnippet( srcFile, lineNo );

		// Only snippets that came from a file we could read are worth keeping around
		if ( cache != null && rendered.lastModified > 0 ) {
			cache.set( cacheKey, rendered );
		}

		return rendered;
	}

	/**
	 * Actively discard every snippet cached for a source file. This runs when a file is detected as changed,
	 * so the other line numbers cached for it don't linger as stale entries.
	 *
	 * @param cache    The snippet cache
	 * @param fileName The source file name
	 */
	private static void purgeCachedSnippets( ICacheProvider cache, String fileName ) {
		String		prefix		= fileName + ":";
		String[]	staleKeys	= cache.getKeysStream()
		    .filter( key -> key.startsWith( prefix ) )
		    .toArray( String[]::new );

		if ( staleKeys.length > 0 ) {
			cache.clear( staleKeys );
		}
	}

	/**
	 * Read a source file from disk and render the 5 lines around the given line number. The file is opened a
	 * single time: the header is checked for Java bytecode and the same channel is then rewound to read the lines.
	 *
	 * @param srcFile The file to read
	 * @param lineNo  The line number
	 *
	 * @return The rendered code snippet, empty when the file can't be read
	 */
	static CodeSnippet renderSnippet( File srcFile, int lineNo ) {
		int		startLine		= Math.max( 1, lineNo - 2 );
		int		endLine			= lineNo + 2;

		// Stat the file before reading it, a change that lands after this point is caught on the next lookup
		long	lastModified	= srcFile.lastModified();

		try ( FileChannel channel = FileChannel.open( srcFile.toPath(), StandardOpenOption.READ ) ) {
			// Read the header from the same channel that reads the lines so the file is only opened once
			ByteBuffer header = ByteBuffer.allocate( 4 );
			while ( header.hasRemaining() && channel.read( header ) > 0 ) {
			}
			// Only a complete header can be checked, truncated files are treated as source
			if ( header.position() == 4 && DiskClassUtil.isJavaByteCode( header.array() ) ) {
				return new CodeSnippet( PRECOMPILED_SOURCE_MESSAGE, PRECOMPILED_SOURCE_MESSAGE, lastModified );
			}

			channel.position( 0 );
			try ( BufferedReader reader = new BufferedReader( Channels.newReader( channel, StandardCharsets.UTF_8.name() ) ) ) {
				StringBuilder	html	= new StringBuilder();
				StringBuilder	plain	= new StringBuilder();

				// Read up to the last line we render and stop, there is no reason to read the rest of the file
				for ( int i = 1; i <= endLine; i++ ) {
					String theLine = reader.readLine();
					if ( theLine == null ) {
						break;
					}
					if ( i < startLine ) {
						continue;
					}
					String escaped = StringEscapeUtils.escapeHtml4( theLine );
					if ( i == lineNo ) {
						html.append( "<b>" ).append( i ).append( ": " ).append( escaped ).append( "</b>" ).append( "<br>" );
					} else {
						html.append( i ).append( ": " ).append( escaped ).append( "<br>" );
					}
					plain.append( i ).append( ": " ).append( theLine ).append( "\n" );
				}

				return new CodeSnippet( html.toString(), plain.toString(), lastModified );
			}
		} catch ( IOException e ) {
			// The file is missing or can't be read. We don't have anything useful to do here, and logging
			// while an exception is being formatted has a habit of making things worse.
			return CodeSnippet.EMPTY;
		}
	}

	/**
	 * Get the cache that holds the rendered code snippets. The runtime ships a <code>bxCodeSnippets</code>
	 * cache, but a runtime home config that predates it won't have one registered, so create it on first use.
	 *
	 * @return The code snippet cache or null if one can't be resolved
	 */
	private static ICacheProvider getCodeSnippetCache() {
		CacheService cacheService = runtime.getCacheService();
		if ( cacheService == null ) {
			return null;
		}

		if ( !cacheService.hasCache( CODE_SNIPPETS_CACHE ) ) {
			try {
				cacheService.createCacheIfAbsent(
				    CODE_SNIPPETS_CACHE,
				    Key.boxCacheProvider,
				    Struct.of(
				        "evictionPolicy", "LRU",
				        "maxObjects", 10_000,
				        "objectStore", "ConcurrentSoftReferenceStore"
				    )
				);
			} catch ( Exception e ) {
				// Another thread beat us to it, or the runtime does not allow it. Either way, no caching.
				return null;
			}
		}

		return cacheService.getCache( CODE_SNIPPETS_CACHE );
	}

	/**
	 * Clear the cached code snippets used to render tag contexts. Use this when source files have changed on
	 * disk and stale snippets need to go away before the file timestamps catch up.
	 */
	public static void clearCodeSnippetCache() {
		ICacheProvider cache = getCodeSnippetCache();
		if ( cache != null ) {
			cache.clearAll();
		}
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

		Throwable	parent		= cause.getCause();
		Throwable	firstCause	= null;
		int			causeDepth	= 0;
		while ( parent != null && causeDepth++ < MAX_CAUSE_DEPTH && parent != firstCause ) {
			if ( firstCause == null ) {
				firstCause = parent;
			}
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
		LinkedHashMap<Throwable, StackTraceElement[]>	map			= new LinkedHashMap<>();

		Throwable										current		= cause;
		Throwable										firstCause	= null;
		int												causeDepth	= 0;
		while ( current != null && causeDepth++ < MAX_CAUSE_DEPTH && current != firstCause ) {
			if ( firstCause == null ) {
				firstCause = current;
			}
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
		return throwableToStruct( target, target, 0 );
	}

	private static IStruct throwableToStruct( Throwable target, Throwable firstCause, int causeDepth ) {
		if ( target == null ) {
			return null;
		}
		if ( causeDepth >= MAX_CAUSE_DEPTH || ( causeDepth > 0 && target == firstCause ) ) {
			return null;
		}

		// All getter methods will be called on the target, which will get all custom fields
		// regardless of what the actual class is
		IStruct result = StructCasterLoose.cast( target );
		result.put( Key.tagContext, ExceptionUtil.buildTagContext( target ) );
		result.put( Key.stackTrace, ExceptionUtil.getStackTraceAsString( target ) );
		result.put( Key.cause, throwableToStruct( target.getCause(), firstCause, causeDepth + 1 ) );

		// Ensure we have a type field
		if ( !result.containsKey( Key.type ) ) {
			result.put( Key.type, target.getClass().getName() );
		}

		if ( result.containsKey( Key.suppressed ) ) {
			Object oSuppressed = result.get( Key.suppressed );
			if ( oSuppressed != null && oSuppressed.getClass().isArray() ) {
				if ( ( ( Object[] ) oSuppressed ).length == 0 ) {
					result.remove( Key.suppressed );
				}
			}
		}
		if ( result.containsKey( Key.cause ) && result.get( Key.cause ) == null ) {
			result.remove( Key.cause );
		}
		if ( result.containsKey( Key.localizedMessage ) ) {
			result.remove( Key.localizedMessage );
		}

		return result;
	}

	/**
	 * Check if the current runtime is executing a license action in CLI mode
	 * 
	 * @param instance The BoxRuntime instance
	 * @param t        The Throwable
	 * 
	 * @return True if executing a license action in CLI mode, false otherwise
	 */
	public static boolean isValidLicenseAction( BoxRuntime instance, Throwable t ) {
		return ( t instanceof BoxLicenseException
		    ||
		    ( t.getMessage() != null && t.getMessage().contains( ExceptionUtil.LICENSE_MODULE_NAME ) ) )
		    && instance.inCLIMode()
		    && instance.getCliOptions().targetModule() != null
		    && instance.getCliOptions().targetModule().equalsIgnoreCase( ExceptionUtil.LICENSE_MODULE_NAME );
	}

	/**
	 * A rendered snippet of source code, in both the HTML and the plain text flavor a tag context exposes.
	 * The timestamp of the file it was rendered from travels with it, so a cache entry can be discarded the
	 * moment that file changes.
	 */
	static final class CodeSnippet {

		private static final CodeSnippet	EMPTY	= new CodeSnippet( "", "", 0L );

		final String						html;
		final String						plain;
		final long							lastModified;

		private CodeSnippet( String html, String plain, long lastModified ) {
			this.html			= html;
			this.plain			= plain;
			this.lastModified	= lastModified;
		}
	}
}
