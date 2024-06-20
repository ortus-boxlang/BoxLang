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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.ContainerBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.ITemplateRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

@BoxBIF
@BoxBIF( alias = "writeDump" )
public class Dump extends BIF {

	private static final ThreadLocal<Set<Integer>>		dumpedObjects		= ThreadLocal.withInitial( HashSet::new );

	private static final ConcurrentMap<String, String>	dumpTemplateCache	= new ConcurrentHashMap<>();

	BoxRuntime											runtime				= BoxRuntime.getInstance();

	/**
	 * Constructor
	 */
	public Dump() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.var )
			// TODO:
			// output
			// format
			// abort
			// label
			// metainfo
			// top
			// show
			// hide
			// keys
			// expand
			// showUDFs
		};
	}

	/**
	 * Pretty print a variable to the buffer
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.var The variable to dump
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	posInCode			= "";
		Key		posInCodeKey		= Key.of( "posInCode" );
		String	templateBasePath	= "/dump/html/";
		Object	target				= DynamicObject.unWrap( arguments.get( Key.var ) );
		String	dumpTemplate		= null;
		String	name				= "Class.bxm";

		if ( target == null ) {
			name = "Null.bxm";
		} else if ( target instanceof Throwable ) {
			name = "Throwable.bxm";
		} else if ( target instanceof Query ) {
			name = "Query.bxm";
		} else if ( target instanceof IScope ) {
			name = "Struct.bxm";
		} else if ( target instanceof Key ) {
			name = "Key.bxm";
		} else if ( target instanceof DateTime ) {
			name = "DateTime.bxm";
		} else if ( target instanceof IClassRunnable ) {
			name = "BoxClass.bxm";
		} else if ( target instanceof ITemplateRunnable itr ) {
			target	= itr.getRunnablePath();
			name	= "ITemplateRunnable.bxm";
		} else if ( target instanceof IStruct ) {
			name = "Struct.bxm";
		} else if ( target instanceof IType ) {
			name = target.getClass().getSimpleName().replaceAll( "Immutable", "" ) + ".bxm";
		} else if ( target instanceof String ) {
			name = "String.bxm";
		} else if ( target instanceof Number ) {
			name = "Number.bxm";
		} else if ( target instanceof Boolean ) {
			name = "Boolean.bxm";
		} else if ( target.getClass().isArray() ) {
			target	= ArrayCaster.cast( target );
			name	= "Array.bxm";
		} else if ( target instanceof StringBuffer ) {
			name = "StringBuffer.bxm";
		} else if ( target instanceof Map ) {
			name = "Map.bxm";
		} else if ( target instanceof List ) {
			name = "List.bxm";
		}
		// Get the set of dumped objects for this thread
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

			dumpTemplate = getDumpTemplate( templateBasePath + name, templateBasePath );

			// Just using this so I can have my own variables scope to use.
			IBoxContext dumpContext = new ContainerBoxContext( context );
			// This is expensive, so only do it on the outer dump
			if ( outerDump ) {
				Array tagContext = ExceptionUtil.getTagContext( 1 );
				if ( tagContext.size() > 0 ) {
					IStruct thisTag = ( IStruct ) tagContext.get( 0 );
					posInCode = thisTag.getAsString( Key.template ) + ":" + thisTag.get( Key.line );

				}
			}
			if ( outerDump ) {
				dumpContext.writeToBuffer( this.styles, true );
			}
			dumpContext.getScopeNearby( VariablesScope.name ).put( posInCodeKey, posInCode );
			dumpContext.getScopeNearby( VariablesScope.name ).put( Key.var, target );
			runtime.executeSource( dumpTemplate, dumpContext, BoxSourceType.BOXTEMPLATE );
		} finally {
			dumped.remove( thisHashCode );
			if ( outerDump ) {
				dumpedObjects.remove();
			}
		}
		return null;
	}

	private String getDumpTemplate( String dumpTemplatePath, String templateBasePath ) {
		// Bypass caching in debug mode for easier testing
		if ( runtime.inDebugMode() ) {
			return computeDumpTemplate( dumpTemplatePath, templateBasePath );
		}
		// Normal flow caches dump template on first request.
		return dumpTemplateCache.computeIfAbsent( dumpTemplatePath, key -> computeDumpTemplate( key, templateBasePath ) );
	}

	private String computeDumpTemplate( String dumpTemplatePath, String templateBasePath ) {
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

		if ( dumpTemplate == null ) {
			dumpTemplatePath = templateBasePath + "Class.bxm";

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

	// @formatter:off
	private final String styles = """
		<style>
			.bx-dump {
				/* Color Pallet Tokens */ 
				--bx-neon-blue-40: #00a4bf;
				--bx-neon-blue-50: #00dbff;
				--bx-neon-blue-80: #bff6ff;
				--bx-blue-gray-10: #050609;
				--bx-blue-gray-70: #8D8E95;
				--bx-blue-gray-95: #F2F2F3;
				--bx-neon-green-40: #00bf5a;
				--bx-neon-green-50: #00ff78;
				--bx-neon-green-80: #bfffdd;
				--bx-neon-lime-40: #8fbf29;
				--bx-neon-lime-50: #bfff36;
				--bx-neon-lime-80: #efffcd;
				--bx-neon-orange-40: #bf7a2a;
				--bx-neon-orange-50: #ffa338;
				--bx-neon-orange-80: #ffe8cd;
				
				/* Color Aliases Tokens */ 
				--bx-color-surface: #ffffff;
				--bx-color-onSurface: var(--bx-blue-gray-10);
				--bx-color-primary: var(--bx-neon-green-50);
				--bx-color-primary-strong: var(--bx-neon-green-40);
				--bx-color-primary-weak: var(--bx-neon-green-80);
				--bx-color-onPrimary: var(--bx-blue-gray-10);
				--bx-color-secondary: var(--bx-neon-blue-50);
				--bx-color-secondary-strong: var(--bx-neon-blue-40);
				--bx-color-secondary-weak: var(--bx-neon-blue-80);
				--bx-color-onSecondary: var(--bx-blue-gray-10);
				--bx-color-tertiary: var(--bx-neon-lime-50);
				--bx-color-tertiary-strong: var(--bx-neon-lime-40);
				--bx-color-tertiary-weak: var(--bx-neon-lime-80);
				--bx-color-onTertiary: var(--bx-blue-gray-10);
				--bx-color-warning: var(--bx-neon-orange-50);
				--bx-color-warning-strong: var(--bx-neon-orange-40);
				--bx-color-warning-weak: var(--bx-neon-orange-80);
				--bx-color-onWarning: var(--bx-blue-gray-10);
				
				/* Text Tokens */ 
				--bx-font-family-sans-serif: system-ui, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, Helvetica, Arial, "Helvetica Neue", sans-serif;
				
				/* Icon Tokens */
				--bx-icon-chevron: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='rgb(0, 0, 0)' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E\");
				
				--bx-table-bg: var(--bx-color-surface);
				--bx-table-color: var(--bx-color-onSurface);

				font-family: var(--bx-font-family-sans-serif);
			}
			@media (prefers-color-scheme: dark) {
				.bx-dump {
					--bx-color-surface: var(--bx-blue-gray-10);
					--bx-color-onSurface: var(--bx-blue-gray-95);
				}
			}
			.bx-dump table {
				border-collapse: collapse;
				border-spacing: 0;
				text-indent: 0;
				border: 1px solid #666;
			}
			.bx-dump table> :not(caption)>*>* {
				border-width: 0 1px 1px;
			}
			.bx-dump caption,
			.bx-dump td,
			.bx-dump th {
				border-color: inherit;
				border-style: solid;
				color: var(--bx-table-color);
				padding: 4px;   
			}
			.bx-dump caption {
				caption-side: top;
				white-space: nowrap;
			}
			.bx-dump caption[role=button] {
				cursor: pointer;
				filter: contrast(0.9);
				padding-left: 1.45rem;
				position: relative;
			}
			.bx-dump caption[role=button]:hover {
				filter: contrast(1);
			}
			.bx-dump caption[role=button][open]{
				filter: contrast(1);
			}
			.bx-dump caption[role=button][open]:hover, .bx-dump caption[role=button][open]:focus {
				filter: contrast(0.9);
			}
			.bx-dump caption[role=button][open]:before {
				transform: rotate(0);
			}
			.bx-dump caption[role=button]:before {
				height: 1.2rem;
				width: 1.2rem;
				position: absolute;
				left: .25rem;
				transform: rotate(-90deg);
				background-image: var(--bx-icon-chevron);
				background-position: right center;
				background-size: 1.2em auto;
				background-repeat: no-repeat;
				content: \"\";
				transition: transform .2s ease-in-out;
			}
			.bx-dump table th {
				text-align: left;
			}
			.bx-dump table th[scope="row"] {
				font-weight: 400;
			}
			.bx-dump caption.bx-dhAy,
			.bx-dump thead th.bx-dhAy {
				background-color: var(--bx-color-secondary);
				color: var(--bx-color-onSecondary );
			}
			.bx-dump table th,
			.bx-dump tbody th.bx-dhAy[scope="row"] {
				background-color: var(--bx-color-secondary-weak);
				color: var(--bx-color-onSecondary );
			}
			.bx-dump tbody td {
				background-color: var(--bx-table-bg);
    			color: var(--bx-table-color);
			}
			.bx-dump .bx-tableCs {
				border-color: var(--bx-color-tertiary-strong)
			}
			.bx-dump .bx-tableAy {
				border-color: var(--bx-color-secondary-strong)
			}
			.bx-dump .bx-tableSt {
				border-color: var(--bx-color-primary-strong)
			}
			.bx-dump caption.bx-dhCs,
			.bx-dump thead th.bx-dhCs {
				background-color: var(--bx-color-tertiary);
				color: var(--bx-color-onTertiary );
			}
			.bx-dump tbody th.bx-dhCs[scope="row"] {
				background-color: var(--bx-color-tertiary-weak);
				color: var(--bx-color-onTertiary );
			}
			.bx-dump caption.bx-dhSt,
			.bx-dump thead th.bx-dhSt {
				background-color: var(--bx-color-primary);
				color: var(--bx-color-onPrimary );
			}
			.bx-dump tbody th.bx-dhSt[scope="row"] {
				background-color: var(--bx-color-primary-weak);
				color: var(--bx-color-onPrimary );
			}
			.bx-dump .bx-dwSv {
				border: 1px solid var(--bx-color-warning-strong);
				display: inline-flex;
			}
			.bx-dump .bx-dwSv span {
				padding: 4px;   
			}
			.bx-dump table .bx-dwSv {
				display: inline;
				border: none;
			}
			.bx-dump .bx-dhSv {
				background-color: var(--bx-color-warning);
				color: var(--bx-color-onWarning );
			}
			.bx-dump table .bx-dhSv {
				background-color: transparent;
				color: var(--bx-color-onSurface );
			}
			.bx-dump .d-none {
				display: none;
			}
		</style>
	""";
	// @formatter:on
}
