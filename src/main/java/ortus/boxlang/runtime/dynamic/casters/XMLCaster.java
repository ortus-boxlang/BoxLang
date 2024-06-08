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
package ortus.boxlang.runtime.dynamic.casters;

import org.xml.sax.SAXException;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I handle casting anything to XML
 */
public class XMLCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a XML.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a XML
	 *
	 * @return The XML value
	 */
	public static CastAttempt<XML> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a XML, throwing exception if we fail
	 *
	 * @param object The value to cast to a XML
	 *
	 * @return The XML value
	 */
	public static XML cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a XML
	 *
	 * @param object The value to cast to a XML
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The XML value
	 */
	public static XML cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a XML." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );

		// Is this already an instance of our XML type?
		if ( object instanceof XML xml ) {
			return xml;
		}

		// Is this a native Java DOM object? If so, wrap it.
		if ( object instanceof org.w3c.dom.Node node ) {
			return new XML( node );
		}

		// If we have a string-ish input, let's try to parse it.
		CastAttempt<String> stringCastAttempt = StringCaster.attempt( object );
		if ( stringCastAttempt.wasSuccessful() ) {
			String oString = stringCastAttempt.get();
			if ( XMLSmokeTest( oString ) ) {
				try {
					return new XML( oString );
				} catch ( BoxRuntimeException e ) {
					// If the error we hit didn't seem to be related to the actual parsing, rethrow it
					if ( e.getCause() == null || ! ( e.getCause() instanceof SAXException ) ) {
						throw e;
					}
					// Do nothing, we'll throw below
				}
			}
		}

		if ( fail ) {
			throw new BoxCastException( "Can't cast " + object.getClass().getName() + " to XML." );
		} else {
			return null;
		}
	}

	/**
	 * An attempt to do a basic smoke test on the string before actually parsing it and possibly catching an execption.
	 * If this is problematic, remove it and just take the parsing hit.
	 *
	 * @param xmlData The XML data to test
	 *
	 * @return True if the data is valid XML
	 */
	private static boolean XMLSmokeTest( String xmlData ) {
		String trimmed = xmlData.trim();
		return trimmed.startsWith( "<" ) && trimmed.endsWith( ">" );
	}

}
