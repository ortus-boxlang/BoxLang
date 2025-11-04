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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.context.CustomTagBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

@BoxComponent( description = "Define component properties and functionality", allowsBody = true )
public class Component extends ortus.boxlang.runtime.components.Component {

	/**
	 * List of valid class extensions
	 */
	private static final List<String> VALID_EXTENSIONS = BoxRuntime.getInstance().getConfiguration().getValidTemplateExtensionsList().stream()
	    .filter( ( e ) -> !e.equals( "*" ) ).toList();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Component() {
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
	 *                 the tag root directory, that contains custom tag template.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String		template			= attributes.getAsString( Key.template );
		String		name				= attributes.getAsString( Key._NAME );
		IStruct		actualAttributes	= attributes.getAsStruct( Key.attributes );
		BoxTemplate	bTemplate;
		Key			tagName;

		// Load template class, compiling if neccessary
		if ( template != null && !template.isEmpty() ) {
			// This method already takes into account looking
			// - relative to the current template
			// - relative to a mapping
			String	templateFileName	= new File( template ).getName();
			String	templateName		= templateFileName.substring( 0, templateFileName.lastIndexOf( '.' ) );
			tagName = Key.of( templateName );
			executionState.put( Key.customTagName, tagName );
			bTemplate = RunnableLoader.getInstance().loadTemplateRelative( context, template, false );
		} else if ( name != null && !name.isEmpty() ) {
			tagName = Key.of( name );
			executionState.put( Key.customTagName, tagName );
			bTemplate = RunnableLoader.getInstance().loadTemplateAbsolute( context, findByName( context, name ) );
		} else {
			throw new CustomException( "Either the template or name attribute must be specified." );
		}

		// Register the custom tag in the execution state
		executionState.put( Key.customTagPath, bTemplate.getRunnablePath().absolutePath().toString() );

		// Prepare the variables
		VariablesScope		caller		= ( VariablesScope ) context.getScopeNearby( VariablesScope.name );
		CustomTagBoxContext	ctContext	= new CustomTagBoxContext( context, tagName );
		VariablesScope		variables	= ( VariablesScope ) ctContext.getScopeNearby( VariablesScope.name );

		variables.put( Key.attributes, actualAttributes );
		variables.put( Key.caller, caller );

		// Prepare the thisComponent SCOPE
		IStruct thisComponent = Struct.of(
		    Key.executionMode, "start",
		    Key.hasEndTag, body != null,
		    Key.generatedContent, ""
		);
		variables.put( Key.thisComponent, thisComponent );

		// Place it in the execution state
		executionState.put( Key.caller, caller );
		executionState.put( Key.thisComponent, thisComponent );

		try {
			try {
				bTemplate.invoke( ctContext );
			} catch ( AbortException e ) {
				if ( e.isTag() ) {
					return DEFAULT_RETURN;
				} else if ( e.isTemplate() || e.isPage() ) {
					// Do nothing, we'll contine with the body next like nothing happened
				} else if ( e.isLoop() ) {
					throw new BoxValidationException( "You cannot use the 'loop' method of the exit component in the start of a custom tag." );
				} else {
					// Any other type of abort just keeps going up the stack
					throw e;
				}
			} finally {
				ctContext.flushBuffer( false );
			}

			if ( body != null ) {

				thisComponent.put( Key.executionMode, "inactive" );

				boolean keepLooping = true;
				while ( keepLooping ) {
					// Assume we will only exucute the body once
					keepLooping = false;

					StringBuffer	buffer		= new StringBuffer();
					BodyResult		bodyResult	= processBody( context, body, buffer );
					// IF there was a return statement inside our body, we early exit now
					if ( bodyResult.isEarlyExit() ) {
						// Output thus far
						context.writeToBuffer( buffer.toString() );
						return bodyResult;
					}
					thisComponent.put( Key.generatedContent, buffer.toString() );
					// This will contain data added via the associate component
					thisComponent.putAll( executionState.getAsStruct( Key.dataCollection ) );
					thisComponent.put( Key.executionMode, "end" );

					try {
						bTemplate.invoke( ctContext );
					} catch ( AbortException e ) {
						if ( e.isTag() ) {
							return DEFAULT_RETURN;
						} else if ( e.isTemplate() || e.isPage() ) {
							// Do nothing, we'll contine with the body next like nothing happened
						} else if ( e.isLoop() ) {
							// If the closing tag has exit method="loop", then we will run the tag body again!
							keepLooping = true;
						} else {
							// Any other type of abort just keeps going up the stack
							throw e;
						}
					} finally {
						context.writeToBuffer( thisComponent.getAsString( Key.generatedContent ) );
					}
				}

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
	 * - Directories specified in the this.customComponentPaths page variable, if it exists.
	 * - Directories specified in the engine config boxlang.json file, under the customComponentsDirectory key.
	 * - Directories specified in the mappings in the engine config
	 *
	 * @param name The name of the custom tag in the format "foo.bar.baz". If the tag was called in the format <cf_brad>, then the name would be "brad"
	 *
	 * @return The absolute path found
	 */
	private ResolvedFilePath findByName( IBoxContext context, String name ) {
		// Convert dots to file separator in name
		String					fullName		= name.replace( '.', File.separatorChar );
		List<ResolvedFilePath>	pathToSearch	= new ArrayList<>();

		// Add in the current template path, if it exists
		// This is the path of the template that is invoking this component
		// This is useful for relative paths in the custom component
		Optional.ofNullable( context.findClosestTemplate() )
		    .filter( template -> template.absolutePath() != null && !template.absolutePath().toString().equals( "unknown" )
		        && template.absolutePath().getParent() != null )
		    .ifPresent( template -> pathToSearch.add(
		        ResolvedFilePath.of(
		            template.mappingName(),
		            template.mappingPath(),
		            template.relativePath(),
		            template.absolutePath().getParent()
		        ) )
		    );

		// Add in the custom components directories specified in the config
		pathToSearch.addAll(
		    context.getConfig()
		        .getAsArray( Key.customComponentsDirectory )
		        .stream()
		        .map( entry -> ResolvedFilePath.of(
		            "",
		            entry.toString(),
		            entry.toString(),
		            FileSystemUtil.expandPath( context, entry.toString() ).absolutePath() )
		        )
		        .toList()
		);

		// Add in mappings to search
		pathToSearch.addAll(
		    context.getConfig()
		        .getAsStruct( Key.mappings )
		        .entrySet()
		        .stream()
		        .map( entry -> ResolvedFilePath.of(
		            entry.getKey().getName(),
		            // Mapping.toString() returns the path
		            entry.getValue().toString(),
		            entry.getValue().toString(),
		            entry.getValue().toString() )
		        )
		        .toList()
		);

		// Find the first file that exists
		return pathToSearch
		    .stream()
		    // Map it to a Stream<File> object representing the Files to the files
		    .flatMap( entry -> {
			    // Generate multiple paths here
			    List<ResolvedFilePath> files = new ArrayList<>();
			    for ( String extension : VALID_EXTENSIONS ) {
				    var tagPath = fullName + "." + extension;
				    try {
					    files.add(
					        ResolvedFilePath.of(
					            entry.mappingName(),
					            entry.mappingPath(),
					            tagPath,
					            new File( entry.absolutePath().toString(), tagPath ).toPath()
					        )
					    );
				    } catch ( java.nio.file.InvalidPathException ipe ) {
					    // Skip invalid paths. This can happen if the tag name or extension has invalid chars. No need to blow up, it's simply not found.
					    continue;
				    }
			    }
			    return files.stream();
		    } )
		    .filter( possibleMatch -> FileSystemUtil.pathExistsCaseInsensitive( possibleMatch.absolutePath() ) != null )
		    .findFirst()
		    .orElseThrow( () -> new BoxRuntimeException( "Could not find custom tag [" + name + "]. Paths searched: ["
		        + pathToSearch.stream().map( p -> p.absolutePath().toString() ).collect( java.util.stream.Collectors.joining( ", " ) ) + "]" ) );
	}
}
