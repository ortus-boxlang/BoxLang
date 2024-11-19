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

import java.util.Set;

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.DumpUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent
public class Dump extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Dump() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.var, "any" ),
		    new Attribute( Key.label, "string", "" ),
		    new Attribute( Key.top, "numeric", 0 ),
		    new Attribute( Key.expand, "boolean" ),
		    new Attribute( Key.abort, "any", false ),
		    new Attribute( Key.output, "string", "buffer", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.format, "string", Set.of( Validator.valueOneOf( "html", "text" ), Validator.NON_EMPTY ) ),
		    new Attribute( Key.showUDFs, "boolean", true )
		};
	}

	/**
	 * Outputs the contents of a variable (simple or complex) of any type for debugging purposes to a specific output location.
	 * <p>
	 * The available {@code output} locations are:
	 * - <strong>buffer<strong>: The output is written to the buffer, which is the default location. If running on a web server, the output is written to the browser.
	 * - <strong>console</strong>: The output is printed to the System console.
	 * - <strong>{absolute_file_path}</strong> The output is written to a file with the specified filename path.
	 * <p>
	 * The output {@code format} can be either HTML or plain text.
	 * The default format is HTML if the output location is the buffer or a web server or a file, otherwise it is plain text for the console.
	 *
	 * @attributes.var The variable to dump, can be any type
	 *
	 * @attributes.label A custom label to display above the dump (Only in HTML output)
	 *
	 * @attributes.top The number of levels to display when dumping collections. Great to avoid dumping the entire world! Default is inifinity. (Only in HTML output)
	 *
	 * @attributes.expand Whether to expand the dump. Be default, we try to expand as much as possible. (Only in HTML output)
	 *
	 * @attributes.abort Whether to do a hard abort the request after dumping. Default is false
	 *
	 * @attributes.output The output format which can be "buffer", "console", or "{absolute file path}". The default is "buffer".
	 *
	 * @attributes.format The format of the output to a <strong>filename</strong>. Can be "html" or "text". The default is according to the output location.
	 *
	 * @attributes.showUDFs Show UDFs or not. Default is true. (Only in HTML output)
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		// Abort as String and empty means true <bx:dump var="" abort>
		Object abort = attributes.get( Key.abort );
		if ( abort instanceof String castedAbort && castedAbort.isEmpty() ) {
			attributes.put( Key.abort, true );
		}

		DumpUtil.dump(
		    context,
		    DynamicObject.unWrap( attributes.get( Key.var ) ),
		    attributes.getAsString( Key.label ),
		    IntegerCaster.cast( attributes.get( Key.top ) ),
		    attributes.getAsBoolean( Key.expand ),
		    BooleanCaster.cast( attributes.get( Key.abort ) ),
		    attributes.getAsString( Key.output ).toLowerCase(),
		    attributes.getAsString( Key.format ),
		    attributes.getAsBoolean( Key.showUDFs )
		);
		return DEFAULT_RETURN;
	}
}
