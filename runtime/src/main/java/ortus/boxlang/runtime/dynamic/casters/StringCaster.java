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

import java.text.DecimalFormat;

/**
 * I handle de
 */
public class StringCaster {

	 /**
	  * Used to cast anything to a string
	  *
	  * @param value The value to cast to a string
	  * @return The String value
	  */
	 public static String cast( Object object ) {
		if( object == null ) {
			return "";
		}
		if( object instanceof Number ) {
			// This removes the ".0" that Doubles' .toString() method adds
			return new DecimalFormat("#.##########################################").format(object);
		}
		if( object instanceof byte[] ) {
			return new String( (byte[])object );
		}
		// TODO: Figure out which types need specific casting
		return object.toString();
	 }

}
