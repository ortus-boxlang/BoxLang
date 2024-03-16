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
import java.util.Scanner;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.ContainerBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

@BoxBIF
@BoxBIF( alias = "writeDump" )
public class Dump extends BIF {

	BoxRuntime runtime = BoxRuntime.getInstance();

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
		Array	tagContext		= ExceptionUtil.getTagContext( 1 );
		String	posInCode		= "";
		Key		posInCodeKey	= Key.of( "posInCode" );
		if ( tagContext.size() > 0 ) {
			IStruct thisTag = ( IStruct ) tagContext.get( 0 );
			posInCode = thisTag.getAsString( Key.template ) + ":" + thisTag.get( Key.line );

		}
		String		templateBasePath	= "/dump/html/";
		Object		target				= arguments.get( Key.var );
		InputStream	dumpTemplate		= null;
		String		name				= "Class.cfm";

		if ( target == null ) {
			name = "Null.cfm";
		} else if ( target instanceof Throwable ) {
			name = "Throwable.cfm";
		} else if ( target instanceof IScope ) {
			name = "Struct.cfm";
		} else if ( target instanceof Key ) {
			name = "Key.cfm";
		} else if ( target instanceof DateTime ) {
			name = "DateTime.cfm";
		} else if ( target instanceof IClassRunnable ) {
			name = "BoxClass.cfm";
		} else if ( target instanceof ITemplateRunnable itr ) {
			target	= itr.getRunnablePath();
			name	= "ITemplateRunnable.cfm";
		} else if ( target instanceof IStruct ) {
			name = "Struct.cfm";
		} else if ( target instanceof IType ) {
			name = target.getClass().getSimpleName() + ".cfm";
		} else if ( target instanceof String ) {
			name = "String.cfm";
		} else if ( target instanceof Number ) {
			name = "Number.cfm";
		} else if ( target instanceof Boolean ) {
			name = "Boolean.cfm";
		} else if ( target.getClass().isArray() ) {
			name = "NativeArray.cfm";
		}
		URL		url					= this.getClass().getResource( "" );
		boolean	runningFromJar		= url.getProtocol().equals( "jar" );

		String	dumpTemplatePath	= templateBasePath + name;
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
			dumpTemplatePath = templateBasePath + "Class.cfm";

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

		// Just using this so I can have my own variables scope to use.
		IBoxContext dumpContext = new ContainerBoxContext( context );
		dumpContext.getScopeNearby( VariablesScope.name ).put( posInCodeKey, posInCode );
		dumpContext.getScopeNearby( VariablesScope.name ).put( Key.var, target );
		try ( Scanner s = new Scanner( dumpTemplate ).useDelimiter( "\\A" ) ) {
			String fileContents = s.hasNext() ? s.next() : "";
			runtime.executeSource( fileContents, dumpContext, BoxScriptType.CFMARKUP );
		}

		return null;
	}
}
