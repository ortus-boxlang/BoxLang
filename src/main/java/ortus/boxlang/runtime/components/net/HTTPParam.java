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
package ortus.boxlang.runtime.components.net;

import java.util.Optional;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.validators.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

@BoxComponent
public class HTTPParam extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public HTTPParam() {
	}

	/**
	 * Constructor
	 *
	 * @param name The name of the component
	 */
	public HTTPParam( Key name ) {
		super( name );
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.type, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        ( cxt, comp, attr, attrs ) -> {
			        String type = attrs.getAsString( attr.name() ).toLowerCase();
			        if ( !Set.of( "header", "body", "xml", "cgi", "file", "url", "formfield", "cookie" ).contains( type ) ) {
				        throw new BoxValidationException( comp, attr, "Must be one of [header, body, xml, cgi, file, url, formfield, cookie]" );
			        }
		        }
		    ) ),
		    new Attribute( Key._NAME, "string", Set.of(
		        ( cxt, comp, attr, attrs ) -> {
			        String type = attrs.getAsString( Key.type ).toLowerCase();
			        if ( !Set.of( "body", "xml", "file" ).contains( type )
			            && ( attrs.get( attr.name() ) == null || attrs.getAsString( attr.name() ).isEmpty() ) ) {
				        throw new BoxValidationException( comp, attr, "is required when type is one of [header, cgi, url, formfield, cookie]" );
			        }
		        }
		    ) ),
		    new Attribute( Key.value, "any" ),
		    new Attribute( Key.file, "string", Set.of(
		        ( cxt, comp, attr, attrs ) -> {
			        String type = attrs.getAsString( Key.type ).toLowerCase();
			        if ( type.equals( "file" )
			            && ( attrs.get( attr.name() ) == null || attrs.getAsString( attr.name() ).isEmpty() ) ) {
				        throw new BoxValidationException( comp, attr, "is required when type is [file]" );
			        }
		        }
		    ) ),
		    new Attribute( Key.encoded, "boolean", false ),
		    new Attribute( Key.mimetype, "string", Set.of(
		        ( cxt, comp, attr, attrs ) -> {
			        String type = attrs.getAsString( Key.type ).toLowerCase();
			        if ( !type.equals( "file" ) && attrs.get( attr.name() ) != null ) {
				        throw new BoxValidationException( comp, attr, "is only allowed when type is [file]" );
			        }
		        }
		    ) )
		};
	}

	/**
	 * I add an HTTP param to an HTTP call. Nest me in the body of an HTTP component.
	 *
	 * @param context        The context in which the BIF is being invoked
	 * @param attributes     The attributes to the BIF
	 * @param body           The body of the BIF
	 * @param executionState The execution state of the BIF
	 *
	 */
	public Optional<Object> _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IStruct parentState = context.findClosestComponent( Key.HTTP );
		if ( parentState == null ) {
			throw new RuntimeException( "HTTPParam must be nested in the body of an HTTP component" );
		}
		// Set our data into the HTTP component for it to use
		parentState.getAsArray( Key.HTTPParams ).add( attributes );
		return DEFAULT_RETURN;
	}
}
