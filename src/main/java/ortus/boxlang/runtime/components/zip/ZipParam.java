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
package ortus.boxlang.runtime.components.zip;

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( description = "Define entries for ZIP file operations", allowsBody = false )
public class ZipParam extends Component {

	/**
	 * Constructor
	 */
	public ZipParam() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.charset, Argument.STRING ),
		    new Attribute( Key.content, Argument.ANY ),
		    new Attribute( Key.entryPath, Argument.STRING, Set.of(
		        ( cxt, comp, attr, attrs ) -> {
			        Object type = attrs.get( Key.content );
			        if ( type != null && ( attrs.get( attr.name() ) == null || attrs.getAsString( attr.name() ).isEmpty() ) ) {
				        throw new BoxValidationException( comp, attr, "is required when content attribute is specified" );
			        }
		        }
		    ) ),
		    new Attribute( Key.filter, Argument.ANY ),
		    new Attribute( Key.filterDelimiters, Argument.STRING, Validator.NOT_IMPLEMENTED ),
		    new Attribute( Key.prefix, Argument.STRING ),
		    new Attribute( Key.source, Argument.STRING ),
		    new Attribute( Key.recurse, Argument.BOOLEAN, true ),
		    new Attribute( Key.password, Argument.STRING, Validator.NOT_IMPLEMENTED ),
		    new Attribute( Key.encryptionAlgorithm, Argument.STRING, Validator.NOT_IMPLEMENTED )
		};
	}

	/**
	 * Adds a param to a zip component. This component must always be a child of a Zip component
	 *
	 * @attribute.source The source of the Zip entry - either a file or a directory
	 *
	 * @attribute.recurse Whether to recurse into subdirectories when zipping
	 *
	 * @attribute.content The content of the Zip entry - can be binary or text content
	 *
	 * @attribute.filter The filter to apply to the content of the Zip entry - applies for source directories
	 *
	 * @attribute.prefix The prefix to use for the created zip entries
	 *
	 * @attribute.entryPath The path of the Zip entry - required if content is specified
	 *
	 * @attribute.charset The charset to use for the content of the Zip entry - used only when the content attribute is provided with text content
	 *
	 * @attribute.filterDelimiters The delimiters to use for the filter - not implemented in the current release
	 *
	 * @attribute.password The password to use for the Zip entry - not implemented in the current release
	 *
	 * @attribute.encryptionAlgorithm The encryption algorithm to use for the Zip entry - not implemented in the current release
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IStruct parentState = context.findClosestComponent( Key.Zip );
		if ( parentState == null ) {
			throw new RuntimeException( "ZipParam must be nested in the body of an Zip component" );
		}
		// Set our data into the ZIP component for it to use
		parentState.getAsArray( Key.zipParams ).add( attributes );
		return DEFAULT_RETURN;
	}
}
