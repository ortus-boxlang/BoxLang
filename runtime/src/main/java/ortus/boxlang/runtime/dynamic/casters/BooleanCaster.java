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

/**
 * I handle de
 */
public class BooleanCaster {

	 /**
	  * Used to cast anything to a boolean
	  *
	  * @param value The value to cast to a boolean
	  * @return The boolean value
	  */
	 public static Boolean cast( Object object ) {
		if( object == null ) {
			return false;
		}
		if( object instanceof Boolean ) {
			return (Boolean)object;
		}
		if( object instanceof Number ) {
			// Positive and negative numbers are true, zero is false
			return ((Number)object).doubleValue() != 0;
		}
		if( object instanceof String ) {
			String o = (String)object;
			// String true and yes are truthy
			if( o.equalsIgnoreCase( "true" ) || o.equalsIgnoreCase( "yes" ) ) {
				return true;
			// String false and no are truthy
			} else if( o.equalsIgnoreCase( "false" ) || o.equalsIgnoreCase( "no" ) ) {
				return false;
			}
			throw new RuntimeException(
				String.format( "String [%s] cannot be cast to a boolean", o )
			 );
		}
		throw new RuntimeException(
			String.format( "Value [%s] cannot be cast to a boolean", object.getClass().getName() )
		);
	}

}
