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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.ContainerBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String		templateBasePath	= "/dump/html/";
		Object		var					= arguments.get( Key.var );
		InputStream	dumpTemplate		= null;
		String		name				= "Class.cfs";
		if ( var instanceof IType ) {
			name = var.getClass().getSimpleName() + ".cfs";
		} else if ( var instanceof String ) {
			name = "String.cfs";
		} else if ( var instanceof Number ) {
			name = "Number.cfs";
		} else if ( var instanceof Boolean ) {
			name = "Boolean.cfs";
		}
		URL		url					= this.getClass().getResource( "" );
		boolean	runningFromJar		= url.getProtocol().equals( "jar" );

		String	dumpTemplatePath	= templateBasePath + name;
		if ( runningFromJar ) {
			dumpTemplate = this.getClass().getResourceAsStream( dumpTemplatePath );
		} else {
			if ( new File( "src/main/resources" + dumpTemplatePath ).exists() ) {
				try {
					dumpTemplate = new FileInputStream( new File( "src/main/resources" + dumpTemplatePath ) );
				} catch ( FileNotFoundException e ) {
					throw new BoxRuntimeException( dumpTemplatePath + " not found", e );
				}
			}
		}
		if ( dumpTemplate == null ) {
			dumpTemplatePath = templateBasePath + "Class.cfs";
			if ( runningFromJar ) {
				dumpTemplate = this.getClass().getResourceAsStream( dumpTemplatePath );
			} else {
				if ( new File( "src/main/resources" + dumpTemplatePath ).exists() ) {
					try {
						dumpTemplate = new FileInputStream( new File( "src/main/resources" + dumpTemplatePath ) );
					} catch ( FileNotFoundException e ) {
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
		dumpContext.getScopeNearby( VariablesScope.name ).put( Key.var, var );
		Scanner	s				= new Scanner( dumpTemplate ).useDelimiter( "\\A" );
		String	fileContents	= s.hasNext() ? s.next() : "";
		s.close();
		runtime.executeSource( fileContents, dumpContext );
		dumpContext.flushBuffer( false );

		return null;
	}
}
