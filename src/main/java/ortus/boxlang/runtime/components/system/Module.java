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
	public void _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String	template	= attributes.getAsString( Key.template );
		String	name		= attributes.getAsString( Key._NAME );
		String	actualFilePath;

		if ( template != null && !template.isEmpty() ) {
			actualFilePath = template;
		} else if ( name != null && !name.isEmpty() ) {
			throw new BoxRuntimeException( "name not implemented yet" );
		} else {
			throw new CustomException( "Either the template or name attribute must be specified." );
		}

		VariablesScope		caller		= ( VariablesScope ) context.getScopeNearby( VariablesScope.name );
		CustomTagBoxContext	ctContext	= new CustomTagBoxContext( context );
		VariablesScope		variables	= ( VariablesScope ) ctContext.getScopeNearby( VariablesScope.name );
		variables.put( Key.attributes, attributes );
		variables.put( Key.caller, caller );
		IStruct thisTag = new Struct();
		thisTag.put( Key.executionMode, "start" );
		thisTag.put( Key.hasEndTag, body != null );
		thisTag.put( Key.generatedContent, "" );
		thisTag.put( Key.assocAttribs, new Array() );
		variables.put( Key.thisTag, thisTag );

		// Load template class, compiling if neccessary
		BoxTemplate bTemplate = RunnableLoader.getInstance().loadTemplateRelative( context, actualFilePath );
		try {
			bTemplate.invoke( ctContext );
			ctContext.flushBuffer( false );

			if ( body != null ) {

				thisTag.put( Key.executionMode, "inactive" );

				thisTag.put( Key.generatedContent, processBody( context, body ) );

				thisTag.put( Key.executionMode, "end" );

				bTemplate.invoke( ctContext );

				context.writeToBuffer( thisTag.getAsString( Key.generatedContent ) );
			}
		} finally {
			ctContext.flushBuffer( false );
		}

	}
}
