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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Parse XML from a string" )
public class XMLParse extends BIF {

	/**
	 * Constructor
	 */
	public XMLParse() {
		super();
		this.declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.XML ),
		    new Argument( false, "boolean", Key.caseSensitive, true ),
		    new Argument( false, "any", Key.validator ),
		    new Argument( false, "boolean", Key.lenient )
		};
	}

	/**
	 * Return new array
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.XML The XML string to parse.
	 * 
	 * @argument.caseSensitive Whether the XML parsing should be case-sensitive.
	 * 
	 * @argument.validator An optional validator to apply to the parsed XML. This can be a string containing a path or URL to an XSD schema file, or a struct of XML security settings
	 * 
	 * @argument.lenient Whether the XML parsing should be lenient.
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
			xml = FileSystemUtil.readString( xml );
		}

		Boolean	caseSensitive	= arguments.getAsBoolean( Key.caseSensitive );
		Object	validator		= arguments.get( Key.validator );
		Boolean	lenient			= arguments.getAsBoolean( Key.lenient );
		String	validatorString	= validator instanceof String vstr ? vstr : null;
		IStruct	XMLSettings		= validator instanceof IStruct vstr ? vstr : null;

		// If the validator is a local file path (not an HTTP/HTTPS URL), expand it
		if ( validatorString != null && !validatorString.trim().isEmpty() && !validatorString.toLowerCase().startsWith( "http" ) ) {
			validatorString = FileSystemUtil.expandPath( context, validatorString ).absolutePath().toString();
		}

		// If lenient is explicitly passed, inject it as an override into the validator struct.
		if ( lenient != null ) {
			XMLSettings = XMLSettings != null ? XMLSettings : Struct.of();
			XMLSettings.put( Key.lenientProcessing, lenient );
		}

		return new XML( xml, caseSensitive, XMLSettings, validatorString, context );
	}

}
