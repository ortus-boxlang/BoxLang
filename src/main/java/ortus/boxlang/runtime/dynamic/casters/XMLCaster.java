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

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything to XML
 */
public class XMLCaster {

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

		if ( fail ) {
			throw new BoxCastException( "Can't cast " + object.getClass().getName() + " to XML." );
		} else {
			return null;
		}
	}

}
