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
 * I handle casting anything to a Double
 */
public class DoubleCaster {

	/**
	 * Tests to see if the value can be cast to a Double.
	 * Returns a CastAttempt<T> which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param value The value to cast to a Double
	 * @return The Double value
	 */
	public static CastAttempt<Double> attempt( Object object ) {
	   return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Double, throwing exception if we fail
	 *
	 * @param value The value to cast to a Double
	 * @return The Double value
	 */
	public static Double cast( Object object ) {
	   return cast( object, true );
	}


	 /**
	  * Used to cast anything to a double
	  *
	  * @param value The value to cast to a double
	  * @return The double value
	  */
	 public static Double cast( Object object, Boolean fail ) {
		if( object == null ) {
			return Double.valueOf( 0 );
		}
		if( object instanceof Double ) {
			return (Double)object;
		}
		if( object instanceof Number ) {
			return ((Number)object).doubleValue();
		}

		if( object instanceof Boolean ) {
			return (Boolean)object ? 1D : 0D;
		}

		if( object instanceof String ) {
			String o = (String)object;
			// String true and yes are truthy
			if( o.equalsIgnoreCase( "true" ) || o.equalsIgnoreCase( "yes" ) ) {
				return 1D;
			// String false and no are truthy
			} else if( o.equalsIgnoreCase( "false" ) || o.equalsIgnoreCase( "no" ) ) {
				return 0D;
			}
		}
		// TODO: Find a way to check if the string can be cast without throwing an exception here
		try {
			return Double.valueOf( StringCaster.cast( object ) );
		} catch( NumberFormatException e ) {
			if( fail ) {
				throw e;
			} else {
				return null;
			}
		}


	 }

}
