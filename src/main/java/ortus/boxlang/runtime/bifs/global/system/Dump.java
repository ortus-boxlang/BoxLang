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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.ContainerBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.ITemplateRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

@BoxBIF
@BoxBIF( alias = "writeDump" )
@BoxMember( type = BoxLangType.ANY, name = "dump" )
@BoxMember( type = BoxLangType.ARRAY, name = "dump" )
@BoxMember( type = BoxLangType.BOOLEAN, name = "dump" )
@BoxMember( type = BoxLangType.DATE, name = "dump" )
@BoxMember( type = BoxLangType.DATETIME, name = "dump" )
@BoxMember( type = BoxLangType.FILE, name = "dump" )
@BoxMember( type = BoxLangType.LIST, name = "dump" )
@BoxMember( type = BoxLangType.NUMERIC, name = "dump" )
@BoxMember( type = BoxLangType.QUERY, name = "dump" )
@BoxMember( type = BoxLangType.STRUCT, name = "dump" )
@BoxMember( type = BoxLangType.STRING, name = "dump" )
@BoxMember( type = BoxLangType.UDF, name = "dump" )
@BoxMember( type = BoxLangType.CLOSURE, name = "dump" )
@BoxMember( type = BoxLangType.LAMBDA, name = "dump" )
@BoxMember( type = BoxLangType.XML, name = "dump" )
@BoxMember( type = BoxLangType.CUSTOM, name = "dump" )
public class Dump extends BIF {

	private static final ThreadLocal<Set<Integer>>		dumpedObjects		= ThreadLocal.withInitial( HashSet::new );

	private static final ConcurrentMap<String, String>	dumpTemplateCache	= new ConcurrentHashMap<>();

	private static final Logger							logger				= LoggerFactory.getLogger( Dump.class );

	// This is hard-coded for now. Needs to be dynamic for other dump formats like plain text, etc
	private static final String							TEMPLATES_BASE_PATH	= "/dump/html/";

	/**
	 * Constructor
	 */
	public Dump() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "any", Key.var ),
		    new Argument( false, Argument.STRING, Key.label, "" ),
		    new Argument( false, Argument.NUMERIC, Key.top, 0 ),
		    new Argument( false, Argument.BOOLEAN, Key.expand, true ),
		    new Argument( false, Argument.BOOLEAN, Key.abort, false )
			// TODO:
			// output
			// format
			// abort
			// metainfo
			// show
			// hide
			// keys
			// showUDFs
		};
	}

	/**
	 * Outputs the contents of a variable of any type for debugging purposes.
	 * The variable can be as simple as a string or as complex as a class or struct.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.var The variable to dump
	 *
	 * @argument.label A label to display above the dump
	 *
	 * @argument.top The number of levels to display
	 *
	 * @argument.expand Whether to expand the dump
	 *
	 * @argument.abort Whether to abort the request after dumping
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String			posInCode		= "";
		String			dumpTemplate	= null;
		Object			target			= DynamicObject.unWrap( arguments.get( Key.var ) );
		// This discovers the template name based on the target object type and more.
		String			templateName	= discoverTemplateName( target );
		// Get the set of dumped objects for this thread, so it doesn't recurse forever
		Set<Integer>	dumped			= dumpedObjects.get();
		boolean			outerDump		= dumped.isEmpty();
		Integer			thisHashCode	= System.identityHashCode( target );
		if ( !dumped.add( thisHashCode ) ) {
			// The target object has already been dumped in this thread, so return to prevent recursion
			// TODO: Move to template
			context.writeToBuffer( "<div>Recursive reference</div>", true );
			return null;
		}

		try {
			// Compile the dump template if it's not already in the cache
			dumpTemplate = getDumpTemplate( TEMPLATES_BASE_PATH, templateName, "Class.bxm" );
			// Just using this so I can have my own variables scope to use.
			IBoxContext dumpContext = new ContainerBoxContext( context );
			// This is expensive, so only do it on the outer dump
			if ( outerDump ) {
				Array tagContext = ExceptionUtil.getTagContext( 1 );
				if ( !tagContext.isEmpty() ) {
					IStruct thisTag = ( IStruct ) tagContext.get( 0 );
					posInCode = thisTag.getAsString( Key.template ) + ":" + thisTag.get( Key.line );
				}
			}
			if ( outerDump ) {
				// This assumes HTML output. Needs to be dynamic as XML or plain text output wouldn't have CSS
				dumpContext.writeToBuffer( "<style>" + getDumpTemplate( TEMPLATES_BASE_PATH, "Dump.css", null ) + "</style>", true );
			}

			// Place the variables in the scope
			dumpContext.getScopeNearby( VariablesScope.name )
			    .putAll( Struct.of(
			        Key.posInCode, posInCode,
			        Key.var, target,
			        Key.label, arguments.get( Key.label ),
			        Key.top, arguments.get( Key.top ),
			        Key.expand, arguments.get( Key.expand ),
			        Key.abort, arguments.get( Key.abort )
			    ) );

			// Execute the dump template
			runtime.executeSource( dumpTemplate, dumpContext, BoxSourceType.BOXTEMPLATE );
		} finally {
			dumped.remove( thisHashCode );
			if ( outerDump ) {
				dumpedObjects.remove();
			}
		}

		// Do we abort?
		if ( BooleanCaster.cast( arguments.get( Key.abort ) ) ) {
			context.writeToBuffer( "<div style='margin-top 10px'>Dump Aborted</div>", true );
			// Flush the buffer and abort the request
			context.flushBuffer( true );
			throw new AbortException( "request", null );
		}

		return null;
	}

	/**
	 * Discover the template name based on the target object.
	 *
	 * @param target The target object
	 *
	 * @return The template name
	 */
	private String discoverTemplateName( Object target ) {
		if ( target == null ) {
			return "Null.bxm";
		} else if ( target instanceof Throwable ) {
			return "Throwable.bxm";
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
		} else if ( target instanceof IClassRunnable ) {
			return "BoxClass.bxm";
		} else if ( target instanceof ITemplateRunnable castedTarget ) {
			target = castedTarget.getRunnablePath();
			return "ITemplateRunnable.bxm";
		} else if ( target instanceof IStruct ) {
			return "Struct.bxm";
		} else if ( target instanceof IType ) {
			return target.getClass().getSimpleName().replace( "Immutable", "" ) + ".bxm";
		} else if ( target instanceof String ) {
			return "String.bxm";
		} else if ( target instanceof Number ) {
			return "Number.bxm";
		} else if ( target instanceof Boolean ) {
			return "Boolean.bxm";
		} else if ( target.getClass().isArray() ) {
			target = ArrayCaster.cast( target );
			return "Array.bxm";
		} else if ( target instanceof StringBuffer ) {
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
	 *
	 * @param templateBasePath    The base path to the templates
	 * @param dumpTemplateName    The name of the dump template
	 * @param defaultTemplateName The name of the default template
	 * 
	 *                            Throw exception if template is not found and default template is null
	 *
	 * @return The dump template
	 */
	private String getDumpTemplate( String templateBasePath, String dumpTemplateName, String defaultTemplateName ) {
		String dumpTemplatePath = templateBasePath + dumpTemplateName;
		// Bypass caching in debug mode for easier testing
		if ( runtime.inDebugMode() ) {
			// logger.debug( "Dump template [{}] cache bypassed in debug mode", dumpTemplatePath );
			return computeDumpTemplate( dumpTemplatePath, templateBasePath, defaultTemplateName );
		}
		// Normal flow caches dump template on first request.
		return dumpTemplateCache.computeIfAbsent( dumpTemplatePath, key -> computeDumpTemplate( key, templateBasePath, defaultTemplateName ) );
	}

	/**
	 * Compute the dump template from the file system.
	 *
	 * @param dumpTemplatePath The path to the dump template
	 * @param templateBasePath The base path to the templates
	 *
	 * @return The dump template
	 */
	private String computeDumpTemplate( String dumpTemplatePath, String templateBasePath, String defaultTemplateName ) {
		InputStream	dumpTemplate	= null;
		URL			url				= this.getClass().getResource( "" );
		boolean		runningFromJar	= url.getProtocol().equals( "jar" );

		if ( runningFromJar ) {
			dumpTemplate = this.getClass().getResourceAsStream( dumpTemplatePath );
		} else {
			Path filePath = Path.of( "src/main/resources" + dumpTemplatePath );
			if ( Files.exists( filePath ) ) {
				try {
					dumpTemplate = Files.newInputStream( filePath );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( dumpTemplatePath + " not found", e );
				}
			}
		}

		// If not found, try the default template
		if ( dumpTemplate == null && defaultTemplateName != null ) {
			dumpTemplatePath = templateBasePath + defaultTemplateName;

			if ( runningFromJar ) {
				dumpTemplate = this.getClass().getResourceAsStream( dumpTemplatePath );
			} else {
				Path templatePath = Path.of( "src/main/resources" + dumpTemplatePath );

				if ( Files.exists( templatePath ) ) {
					try {
						dumpTemplate = Files.newInputStream( templatePath );
					} catch ( IOException e ) {
						throw new BoxRuntimeException( dumpTemplatePath + " not found", e );
					}
				}
			}
		}

		if ( dumpTemplate == null ) {
			throw new BoxRuntimeException( "Could not load dump template: " + dumpTemplatePath );
		}

		try ( Scanner s = new Scanner( dumpTemplate ).useDelimiter( "\\A" ) ) {
			return s.hasNext() ? s.next() : "";
		}
	}

}
