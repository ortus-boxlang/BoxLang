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
package ortus.boxlang.runtime.components.system;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.CustomTagBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;

@BoxComponent( allowsBody = true )
public class Module extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Required by SLI
	 */
	public Module() {
	}

	public Module( Key name ) {
		super( name );
		declaredAttributes	= new Attribute[] {
		    new Attribute( Key.template, "string" ),
		    new Attribute( Key._NAME, "string" )
		};
		captureBodyOutput	= true;
	}

	/**
	 * Invokes a custom tag.
	 *
	 * @param context        The context in which the BIF is being invoked
	 * @param attributes     The attributes to the BIF
	 * @param body           The body of the BIF
	 * @param executionState The execution state of the BIF
	 *
	 * @argument.template Mutually exclusive with the name attribute. A path to the template that implements the tag.
	 *
	 * @argument.name Mutually exclusive with the template attribute. A custom tag name, in the form "Name.Name.Name..." Identifies subdirectory, under
	 *                the CFML tag root directory, that contains custom tag template.
	 *
	 */
	public Optional<Object> _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String		template			= attributes.getAsString( Key.template );
		String		name				= attributes.getAsString( Key._NAME );
		IStruct		actualAttributes	= attributes.getAsStruct( Key.attributes );
		BoxTemplate	bTemplate;

		// Load template class, compiling if neccessary
		if ( template != null && !template.isEmpty() ) {
			// This method already takes into account looking
			// - relative to the current template
			// - relative to a mapping
			bTemplate = RunnableLoader.getInstance().loadTemplateRelative( context, template );
		} else if ( name != null && !name.isEmpty() ) {
			bTemplate = RunnableLoader.getInstance().loadTemplateAbsolute( context, findByName( context, name ) );
		} else {
			throw new CustomException( "Either the template or name attribute must be specified." );
		}

		VariablesScope		caller		= ( VariablesScope ) context.getScopeNearby( VariablesScope.name );
		CustomTagBoxContext	ctContext	= new CustomTagBoxContext( context );
		VariablesScope		variables	= ( VariablesScope ) ctContext.getScopeNearby( VariablesScope.name );
		variables.put( Key.attributes, actualAttributes );
		variables.put( Key.caller, caller );
		IStruct thisTag = new Struct();
		thisTag.put( Key.executionMode, "start" );
		thisTag.put( Key.hasEndTag, body != null );
		thisTag.put( Key.generatedContent, "" );
		// TODO: This requires cfassociate module to be implemented.
		// Should be able to use our executionState to accomplish this.
		thisTag.put( Key.assocAttribs, new Array() );
		variables.put( Key.thisTag, thisTag );

		try {
			bTemplate.invoke( ctContext );
			ctContext.flushBuffer( false );

			if ( body != null ) {

				thisTag.put( Key.executionMode, "inactive" );

				BodyResult bodyResult = processBody( context, body );
				// IF there was a return statement inside our body, we early exit now
				if ( bodyResult.returnValue().isPresent() ) {
					// Output thus far
					context.writeToBuffer( bodyResult.buffer() );
					return bodyResult.returnValue();
				}
				thisTag.put( Key.generatedContent, bodyResult.buffer() );

				thisTag.put( Key.executionMode, "end" );

				bTemplate.invoke( ctContext );

				context.writeToBuffer( thisTag.getAsString( Key.generatedContent ) );
			}
		} finally {
			ctContext.flushBuffer( false );
		}

		return DEFAULT_RETURN;
	}

	/**
	 * Lookup a custom tag by name based on our lookup rules.
	 * An error is thrown is the name could not be found.
	 *
	 * - Directories specified in the this.customTagPaths page variable, if it exists.
	 * - Directories specified in the engine config Custom Tag Paths
	 * - The /boxlang/CustomTags directory, and its subdirectories.
	 * - Directories specified in the mappings in the engine config
	 *
	 * @param name The name of the custom tag in the format "foo.bar.baz". If the tag was called in the format <cf_brad>, then the name would be "brad"
	 *
	 * @return The absolute path found
	 */
	private Path findByName( IBoxContext context, String name ) {
		// Convert dots to file separator in name
		// TODO: include BL extensions
		String	fullName		= name.replace( '.', File.separatorChar ) + ".cfm";
		Array	pathToSearch	= new Array();
		pathToSearch.addAll( context.getConfig().getAsStruct( Key.runtime ).getAsArray( Key.customTagsDirectory ) );
		// Add in mappings to search
		pathToSearch.addAll( context.getConfig().getAsStruct( Key.runtime ).getAsStruct( Key.mappings ).values() );

		// TODO: Case insensitive search.
		Path foundPath = pathToSearch
		    .stream()
		    .map( p -> new File( p.toString(), fullName ) )
		    .filter( f -> f.exists() )
		    .findFirst()
		    .orElseThrow( () -> new BoxRuntimeException( "Could not find custom tag [" + name + "]" ) )
		    .toPath();

		return foundPath;
	}
}
