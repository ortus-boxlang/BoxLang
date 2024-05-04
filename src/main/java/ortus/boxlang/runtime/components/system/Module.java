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
import java.util.ArrayList;
import java.util.List;

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
import ortus.boxlang.runtime.util.ResolvedFilePath;

@BoxComponent( allowsBody = true )
public class Module extends Component {

	/**
	 * List of valid class extensions
	 */
	// TODO: Move .cfc extension into CF compat module and contribute it at startup.
	// Need to add a setter or other similar mechanism to allow for dynamic extension
	private static List<String> VALID_EXTENSIONS = List.of( ".bxm", ".cfm" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Module() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.template, "string" ),
		    new Attribute( Key._NAME, "string" )
		};
	}

	/**
	 * Invokes a custom tag.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.template Mutually exclusive with the name attribute. A path to the template that implements the tag.
	 *
	 * @attribute.name Mutually exclusive with the template attribute. A custom tag name, in the form "Name.Name.Name..." Identifies subdirectory, under
	 *                 the CFML tag root directory, that contains custom tag template.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
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

				StringBuffer	buffer		= new StringBuffer();
				BodyResult		bodyResult	= processBody( context, body, buffer );
				// IF there was a return statement inside our body, we early exit now
				if ( bodyResult.isEarlyExit() ) {
					// Output thus far
					context.writeToBuffer( buffer.toString() );
					return bodyResult;
				}
				thisTag.put( Key.generatedContent, buffer.toString() );

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
	private ResolvedFilePath findByName( IBoxContext context, String name ) {
		// Convert dots to file separator in name
		// TODO: include BL extensions
		String					fullName		= name.replace( '.', File.separatorChar );
		List<ResolvedFilePath>	pathToSearch	= new ArrayList<ResolvedFilePath>();
		pathToSearch.addAll(
		    context.getConfig()
		        .getAsStruct( Key.runtime )
		        .getAsArray( Key.customTagsDirectory )
		        .stream()
		        .map( entry -> ResolvedFilePath.of( "", entry.toString(), null, ( Path ) null ) )
		        .toList()
		);
		// Add in mappings to search
		pathToSearch.addAll(
		    context.getConfig()
		        .getAsStruct( Key.runtime )
		        .getAsStruct( Key.mappings )
		        .entrySet()
		        .stream()
		        .map( entry -> ResolvedFilePath.of( entry.getKey().getName(), entry.getValue().toString(), null, ( Path ) null ) )
		        .toList()
		);

		// TODO: Case insensitive search.
		ResolvedFilePath foundPath = pathToSearch
		    .stream()
		    // Map it to a Stream<File> object representing the Files to the files
		    .flatMap( entry -> {
			    // Generate multiple paths here
			    List<ResolvedFilePath> files = new ArrayList<ResolvedFilePath>();
			    for ( String extension : VALID_EXTENSIONS ) {
				    files.add(
				        ResolvedFilePath.of(
				            entry.mappingName(),
				            entry.mappingPath(),
				            fullName + extension,
				            new File( entry.mappingPath(), fullName + extension ).toPath()
				        )
				    );
			    }

			    return files.stream();
		    } )
		    .filter( possibleMatch -> possibleMatch.absolutePath().toFile().exists() )
		    .findFirst()
		    .orElseThrow( () -> new BoxRuntimeException( "Could not find custom tag [" + name + "]" ) );

		return foundPath;
	}
}
