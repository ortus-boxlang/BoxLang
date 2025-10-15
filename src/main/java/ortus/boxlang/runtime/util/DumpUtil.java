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
package ortus.boxlang.runtime.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.checkerframework.checker.nullness.qual.Nullable;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.components.jdbc.Query;
import ortus.boxlang.runtime.context.ContainerBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.ITemplateRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.util.JSONUtil;

/**
 * This class is responsible for dumping objects in the BoxLang runtime
 * into a human readable format in different formats and to different
 * destinations.
 */
public class DumpUtil {

	/**
	 * This is used to track objects that have been dumped to prevent infinite recursion.
	 */
	private static final ThreadLocal<Set<Integer>>				dumpedObjects			= ThreadLocal.withInitial( HashSet::new );

	/**
	 * This is used to cache the templates for dumping objects.
	 */
	private static final ConcurrentMap<String, DumpTemplate>	dumpTemplateCache		= new ConcurrentHashMap<>();

	/**
	 * This is the base path for the templates when using the HTML format.
	 * This inside of the jar as a resource. {@see resources/dump/html}
	 */
	private static final String									TEMPLATES_BASE_PATH		= "/dump/html/";

	/**
	 * This is the default template name to use when the target object does not have a template.
	 */
	private static final String									DEFAULT_DUMP_TEMPLATE	= "Class.bxm";

	/**
	 *
	 * @param target
	 * @param label
	 * @param top
	 * @param expand
	 * @param abort
	 * @param output
	 * @param format
	 * @param showUDFs
	 */
	public static void dump(
	    IBoxContext context,
	    Object target,
	    String label,
	    @Nullable Integer top,
	    Boolean expand,
	    Boolean abort,
	    String output,
	    String format,
	    Boolean showUDFs ) {

		boolean isScriptContext = context.getRequestContext() instanceof ScriptingRequestBoxContext;

		// Default output param
		if ( output == null ) {
			if ( isScriptContext ) {
				output = "console";
			} else {
				output = "buffer";
			}
		} else {

			// Backwards compat here, could put this in the transpiler if we want
			if ( output.equalsIgnoreCase( "browser" ) ) {
				output = "buffer";
			}
		}
		Path dumpFilePath = null;
		// If the output contains a dot or slash, then take it as a file path
		if ( output.contains( "." ) || output.contains( "/" ) || output.contains( "\\" ) ) {
			try {
				// test if output is an absolute valid path on disk
				dumpFilePath = Paths.get( output );
				// If not absolute, make relative to the temp dir. FileSystemUtil.getTempDirectory()
				if ( !dumpFilePath.isAbsolute() ) {
					dumpFilePath = Paths.get( FileSystemUtil.getTempDirectory() ).resolve( output ).toAbsolutePath().normalize();
				}
				// Set this last so any errors above don't change the output type
				output = "___file___";
			} catch ( Exception e ) {
				// If the path is invalid, simply ignore
				dumpFilePath = null;
			}
		}

		// Don't lowercase this until we've checked if it's a file path
		output = output.toLowerCase();
		// Determine the output format if not passed from the output parameter.
		if ( format == null ) {
			// If the output is console or the parent context is a scripting context, then use text.
			if ( output.equals( "console" ) || isScriptContext ) {
				format = "text";
			} else {
				// Otherwise, use HTML (default for buffer or filename)
				format = "html";
			}
		} else {
			format = format.toLowerCase();
		}

		// Announce it
		final var	outputFinal			= output;
		final var	formatFinal			= format;
		final var	dumpFilePathFinal	= dumpFilePath;
		context
		    .getRuntime()
		    .announce(
		        BoxEvent.ON_BXDUMP,
		        () -> Struct.ofNonConcurrent(
		            Key.context, context,
		            Key.target, target,
		            Key.label, label,
		            Key.top, top,
		            Key.expand, expand,
		            Key.abort, abort,
		            Key.output, outputFinal,
		            Key.format, formatFinal,
		            Key.dumpFilePath, dumpFilePathFinal,
		            Key.showUDFs, showUDFs
		        ) );

		String dumpOutput;
		if ( format.equals( "html" ) ) {
			dumpOutput = generateDumpHTML( context, target, label, top, expand, abort, output, format, showUDFs );
		} else {
			dumpOutput = generateDumpText( context, target, label );
		}
		if ( dumpOutput != null ) {
			// Dump the object based on the output location
			switch ( output ) {
				// SEND TO CONSOLE
				case "console" :
					context.getRequestContext().getOut().println( dumpOutput );
					break;

				// SEND TO BUFFER (HTML or TEXT)
				case "buffer" :
					context.writeToBuffer( dumpOutput );
					context.flushBuffer( false );

					// If we are aborting, then throw an abort exception.
					if ( abort ) {
						context.writeToBuffer(
						    """
						    	<div style="margin-top: 10px; display: inline-block; padding: 8px 12px; background-color: #ff4d4d; color: white; font-weight: bold; border-radius: 5px;">
						    		Dump Aborted
						    	</div>
						    """,
						    true );
						context.flushBuffer( true );
						throw new AbortException( "request", null );
					}
					break;

				// SEND TO FILE
				case "___file___" :
					if ( dumpFilePath != null ) {
						FileSystemUtil.write( dumpFilePath.toString(), dumpOutput, FileSystemUtil.DEFAULT_CHARSET.name(), true );
						break;
					}

					// SEND ANNOUCEMENT
				default :
					// Validate we have something
					if ( output.isEmpty() ) {
						throw new BoxRuntimeException( "The output parameter is required." );
					}

					// Announce the missing dump output
					// So anybody listening can handle it.
					final var dumpOutputFinal = dumpOutput;
					context.getRuntime()
					    .getInterceptorService()
					    .announce(
					        BoxEvent.ON_MISSING_DUMP_OUTPUT,
					        () -> Struct.ofNonConcurrent(
					            Key.context, context,
					            Key.target, target,
					            Key.label, label,
					            Key.top, top,
					            Key.expand, expand,
					            Key.abort, abort,
					            Key.output, outputFinal,
					            Key.format, formatFinal,
					            Key.dumpFilePath, dumpFilePathFinal,
					            Key.dumpOutput, dumpOutputFinal,
					            Key.showUDFs, showUDFs
					        )
					    );
					break;
			}

		}
	}

	/**
	 * Return the dump text for the object.
	 *
	 * @param context The context
	 * @param target  The target object
	 * @param label   The label for the object
	 */
	private static String generateDumpText( IBoxContext context, Object target, String label ) {
		StringBuffer buffer = new StringBuffer();

		// Do we have a console label?
		if ( label != null && !label.isEmpty() ) {
			buffer.append( "============================================\n" );
			buffer.append( label + "\n" );
			buffer.append( "============================================\n" );
		}

		// Dump the object(s) to the console
		if ( target instanceof IClassRunnable castedTarget ) {
			try {
				buffer.append( "> " + castedTarget.getBoxMeta().getMeta().getAsString( Key._NAME ) + "\n" );
				buffer.append( JSONUtil.getJSONBuilder().asString( castedTarget ) + "\n" );
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Error serializing class to JSON", e );
			}
		} else {
			// buffer.append( "> " + target.getClass().getName() );
			if ( target == null ) {
				buffer.append( "[null]" );
			} else {
				buffer.append( target.toString() );
			}
		}
		return buffer.toString();
	}

	/**
	 * Dump the object to an HTML buffer.
	 *
	 * @param context  The context
	 * @param target   The target object
	 * @param label    The label for the object
	 * @param top      The number of levels to dump
	 * @param expand   Whether to expand the object
	 * @param abort    Whether to abort on error
	 * @param output   The output location
	 * @param format   The format for the output
	 * @param showUDFs Whether to show UDFs
	 */
	private static String generateDumpHTML(
	    IBoxContext context,
	    Object target,
	    String label,
	    @Nullable Integer top,
	    Boolean expand,
	    Boolean abort,
	    String output,
	    String format,
	    Boolean showUDFs ) {
		// Get the set of dumped objects for this thread, so it doesn't recurse forever
		Set<Integer>	dumped			= dumpedObjects.get();
		boolean			outerDump		= dumped.isEmpty();
		Integer			thisHashCode	= System.identityHashCode( target );

		// The target object has already been dumped in this thread, so return to prevent recursion
		if ( !dumped.add( thisHashCode ) ) {
			context.writeToBuffer( "<div><em>Recursive Reference (Skipping dump)</em></div>", true );
			return null;
		}

		// Reached the top limit, so return to prevent dumping the entire world
		if ( top != null && top <= 0 ) {
			context.writeToBuffer( "<div><em>Top Limit reached (Skipping dump)</em></div>", true );
			return null;
		}

		// Prep variables to use for dumping
		String			posInCode		= "";
		DumpTemplate	dumpTemplate	= null;
		StringBuffer	buffer			= null;
		String			templateName	= discoverTemplateName( target, context );

		try {
			// Get and Compile the Dump template to execute.
			dumpTemplate = getDumpTemplate( context, templateName );
		} catch ( Throwable t ) {
			throw new BoxRuntimeException( "Error loading dump template [" + templateName + "]", t );
		}

		try {
			// Just using this so I can have my own variables scope to use.
			IBoxContext dumpContext = new ContainerBoxContext( context );
			// This is expensive, so only do it on the outer dump
			if ( outerDump ) {
				buffer = new StringBuffer();
				context.pushBuffer( buffer );
				posInCode = ExceptionUtil.getCurrentPositionInCode();
				// This assumes HTML output. Needs to be dynamic as XML or plain text output wouldn't have CSS
				dumpContext.writeToBuffer( "<style>" + getDumpTemplate( context, "Dump.css" ).source() + "</style>", true );
				dumpContext.writeToBuffer( "<script>" + getDumpTemplate( context, "Dump.js" ).source() + "</script>", true );
			}

			// Place the variables in the scope
			dumpContext.getScopeNearby( VariablesScope.name )
			    .putAll( Struct.of(
			        Key.posInCode, posInCode,
			        Key.var, target,
			        Key.label, label,
			        Key.top, top,
			        Key.expand, expand,
			        Key.abort, abort,
			        Key.showUDFs, showUDFs,
			        Key.format, format
			    ) );

			// Execute the dump template
			context.getRuntime().executeSource( dumpTemplate.source(), dumpContext, BoxSourceType.BOXTEMPLATE );
		} catch ( Throwable t ) {
			throw new BoxRuntimeException( "Error executing dump template [" + dumpTemplate.path() + "]", t );
		} finally {
			// Clean up the dumped objects
			dumped.remove( thisHashCode );
			if ( outerDump ) {
				dumpedObjects.remove();
				context.popBuffer();
			}
		}

		// If this is the outer dump, then return the buffer contents
		if ( outerDump && buffer != null ) {
			return buffer.toString();
		}

		// Otherwise, return empty string
		return null;

	}

	/**
	 * Discover the template name based on the target object.
	 *
	 * @param target The target object
	 *
	 * @return The template name found in the resources folder
	 */
	private static String discoverTemplateName( Object target, IBoxContext context ) {
		if ( target == null || target instanceof NullValue ) {
			return "Null.bxm";
		} else if ( target instanceof Throwable ) {
			return "Throwable.bxm";
		} else if ( target instanceof XML ) {
			return "XML.bxm";
		} else if ( target instanceof Query ) {
			return "Query.bxm";
		} else if ( target instanceof Function ) {
			return "Function.bxm";
		} else if ( target instanceof IScope ) {
			return "Struct.bxm";
		} else if ( target instanceof Key ) {
			return "Key.bxm";
		} else if ( target instanceof DateTime ) {
			return "DateTime.bxm";
		} else if ( target instanceof LocalDate || target instanceof LocalDateTime || target instanceof ZonedDateTime ||
		    target instanceof java.sql.Date || target instanceof java.sql.Timestamp || target instanceof java.util.Date ) {
			target = DateTimeCaster.cast( target, context );
			return "DateTime.bxm";
		} else if ( target instanceof Duration ) {
			return "Duration.bxm";
		} else if ( target instanceof ZoneId ) {
			return "ToString.bxm";
		} else if ( target instanceof Instant ) {
			return "Instant.bxm";
		} else if ( target instanceof IClassRunnable ) {
			return "BoxClass.bxm";
		} else if ( target instanceof ITemplateRunnable castedTarget ) {
			target = castedTarget.getRunnablePath();
			return "ITemplateRunnable.bxm";
		} else if ( target instanceof IStruct ) {
			return "Struct.bxm";
		} else if ( target instanceof IType ) {
			return target.getClass().getSimpleName().replace( "Unmodifiable", "" ) + ".bxm";
		} else if ( target instanceof String ) {
			return "String.bxm";
		} else if ( target instanceof Number ) {
			return "Number.bxm";
		} else if ( target instanceof Boolean ) {
			return "Boolean.bxm";
		} else if ( target.getClass().isArray() ) {
			target = ArrayCaster.cast( target );
			return "Array.bxm";
		} else if ( target instanceof StringBuffer || target instanceof StringBuilder ) {
			return "StringBuffer.bxm";
		} else if ( target instanceof Map ) {
			return "Map.bxm";
		} else if ( target instanceof List ) {
			return "List.bxm";
		}
		return "Class.bxm";
	}

	/**
	 * Get the dump template from the cache or load it from the file system.
	 * Throw exception if template is not found and default template is null
	 *
	 * @param context          The context
	 * @param dumpTemplateName The name of the dump template
	 *
	 * @return The compiled dump template
	 */
	private static DumpTemplate getDumpTemplate( IBoxContext context, String dumpTemplateName ) {
		String dumpTemplatePath = TEMPLATES_BASE_PATH + dumpTemplateName;

		// Bypass caching in debug mode for easier testing
		if ( context.getRuntime().inDebugMode() ) {
			// logger.debug( "Dump template [{}] cache bypassed in debug mode", dumpTemplatePath );
			return computeDumpTemplate( dumpTemplatePath, context );
		}

		// Normal flow caches dump template on first request.
		return dumpTemplateCache.computeIfAbsent(
		    dumpTemplatePath,
		    key -> computeDumpTemplate( dumpTemplatePath, context )
		);
	}

	/**
	 * Compute the dump template from the file system by compiling the template.
	 *
	 * @param dumpTemplatePath The path to the dump template
	 * @param context          The context
	 *
	 * @return The dump template
	 */
	private static DumpTemplate computeDumpTemplate( String dumpTemplatePath, IBoxContext context ) {
		Objects.requireNonNull( dumpTemplatePath, "dumpTemplatePath cannot be null" );

		// Try by resource first
		InputStream dumpTemplate = null;
		dumpTemplate = DumpUtil.class.getResourceAsStream( dumpTemplatePath );

		// If we are NOT in jar mode and in debug mode, then re-read the file from the file system
		if ( !context.getRuntime().inJarMode() && context.getRuntime().inDebugMode() ) {
			Path filePath = Paths.get( "src", "main", "resources", dumpTemplatePath ).toAbsolutePath();
			if ( Files.exists( filePath ) ) {
				try {
					dumpTemplate = Files.newInputStream( filePath );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( dumpTemplatePath + " not found", e );
				}
			}
		}

		if ( dumpTemplate == null ) {
			throw new BoxRuntimeException( "Could not load dump template from class path or filesystem: " + dumpTemplatePath );
		}

		// \\A is the beginning of the input boundary so it reads the entire file in one go.
		try ( Scanner s = new Scanner( dumpTemplate ).useDelimiter( "\\A" ) ) {
			return new DumpTemplate( dumpTemplatePath, s.hasNext() ? s.next() : "" );
		}
	}

	public record DumpTemplate( String path, String source ) {
	}

}
