/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.xml;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.util.FileSystemUtil;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class XMLParse extends BIF {

	/**
	 * Constructor
	 */
	public XMLParse() {
		super();
		this.declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.XML )
		};
	}

	/**
	 * Return new array
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String xml = arguments.getAsString( Key.XML );
		if ( xml == null && arguments.containsKey( Key.XMLText ) ) {
			xml = arguments.getAsString( Key.XMLText );
		} else if ( xml == null && arguments.containsKey( Key.XMLString ) ) {
			xml = arguments.getAsString( Key.XMLString );
		} else if ( xml == null ) {
			throw new BoxRuntimeException( "Required argument XML is missing for function xmlParse" );
		}

		// Is not XML. Must be file or URL
		if ( !xml.trim().startsWith( "<" ) ) {
			xml = StringCaster.cast( FileSystemUtil.read( xml ) );
		}
		return new XML( xml );
	}

}
