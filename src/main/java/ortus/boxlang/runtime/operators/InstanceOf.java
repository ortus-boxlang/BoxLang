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
package ortus.boxlang.runtime.operators;

import java.util.List;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;

/**
 * Performs instance of check.
 * https://helpx.adobe.com/coldfusion/cfml-reference/coldfusion-functions/functions-in-k/isinstanceof.html
 *
 * - The object specified by the first parameter is an instance of the interface or component specified by the second parameter.
 * - The Java object specified by the first parameter was created by using the object component or CreateObject method and is an instance of the Java
 * class specified by the second parameter.
 * - The object specified by the first parameter is an instance of a component that extends the component specified in the second parameter.
 * - The object specified by the first parameter is an instance of a component that extends a component that implements the interface specified in the
 * second parameter.
 * - The Java object specified by the first parameter was created by using the object component or CreateObject method and is an instance of a Java
 * class
 * that extends the class specified by the second parameter.
 */
public class InstanceOf implements IOperator {

	/**
	 * @param left  The object to perform type check on
	 * @param right The type to check against
	 *
	 * @return The result
	 */
	public static Boolean invoke( IBoxContext context, Object left, Object right ) {
		if ( left == null ) {
			return false;
		}
		IClassRunnable	boxClass	= null;

		String			type		= StringCaster.cast( right );

		left = DynamicObject.unWrap( left );

		// First perform exact boxClass check
		if ( left instanceof IClassRunnable boxClass2 ) {
			boxClass = boxClass2;
			String boxClassName = boxClass.bxGetName().getName();
			if ( looseClassCheck( boxClassName, type ) ) {
				return true;
			}
		}

		// Perform exact Java type check
		if ( looseClassCheck( left.getClass().getName(), type ) ) {
			return true;
		}

		// Perform boxClass inheritance check
		if ( boxClass != null ) {
			IClassRunnable _super = boxClass;
			while ( ( _super = _super.getSuper() ) != null ) {
				// For each super class, check if it's the same as the type
				String boxClassName = _super.bxGetName().getName();
				if ( looseClassCheck( boxClassName, type ) ) {
					return true;
				}
				// For each super class, check if implements an interface of that type
				if ( checkInterfaces( _super.getInterfaces(), type ) ) {
					return true;
				}
			}
			if ( checkInterfaces( boxClass.getInterfaces(), type ) ) {
				return true;
			}

		}

		// Perform Java inheritance check
		Class<?> leftClass = left.getClass();
		if ( isAssignableFromIgnoreCase( type, leftClass ) ) {
			return true;
		}

		return false;
	}

	/**
	 * I check a list of interfaces for a specific type
	 *
	 * @param interfaces The interfaces to check
	 * @param type       The type to check against
	 *
	 * @return true if the interface is assignable from the type
	 */
	private static Boolean checkInterfaces( List<BoxInterface> interfaces, String type ) {
		for ( BoxInterface boxInterface : interfaces ) {
			if ( checkInterface( type, boxInterface ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * I check a single interface for a specific type and recurse into its super interfaces
	 *
	 * @param type         The type to check against
	 * @param boxInterface The interface to check
	 *
	 * @return true if the interface is assignable from the type
	 */
	private static Boolean checkInterface( String type, BoxInterface boxInterface ) {
		// If we match, quit
		if ( looseClassCheck( boxInterface.getName().getName(), type ) ) {
			return true;
		}
		// If one of the super interfaces matches, quit
		if ( boxInterface.getSupers().size() > 0 ) {
			if ( checkInterfaces( boxInterface.getSupers(), type ) ) {
				return true;
			}
		}
		// We give up
		return false;
	}

	/**
	 * Check Java inheritance
	 *
	 * @param targetTypeName The string type to check against
	 * @param leftClass      The class to check
	 *
	 * @return true if the class is assignable from the type
	 */
	private static boolean isAssignableFromIgnoreCase( String targetTypeName, Class<?> leftClass ) {
		while ( leftClass != null ) {
			if ( looseClassCheck( leftClass.getName(), targetTypeName ) ) {
				return true;
			}
			// Check interfaces
			if ( checkJavaInterfaces( leftClass.getInterfaces(), targetTypeName ) ) {
				return true;
			}
			leftClass = leftClass.getSuperclass();
		}
		return false;
	}

	/**
	 * Check Java interfaces including super interfaces
	 *
	 * @param interfaces     The interfaces to check
	 * @param targetTypeName The type to check against
	 *
	 * @return true if the interface is assignable from the type
	 */
	private static boolean checkJavaInterfaces( Class<?>[] interfaces, String targetTypeName ) {
		for ( Class<?> iface : interfaces ) {
			if ( looseClassCheck( iface.getName(), targetTypeName ) ) {
				return true;
			}
			// Recursively check superinterfaces
			if ( checkJavaInterfaces( iface.getInterfaces(), targetTypeName ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Will match java.lang.String or just String, or even java.lang.string or string.
	 *
	 * @param actual   The actual class name
	 * @param expected The expected class name
	 *
	 * @return true if the class names match
	 */
	private static boolean looseClassCheck( String actual, String expected ) {
		return actual.equalsIgnoreCase( expected ) || actual.toLowerCase().endsWith( "." + expected.toLowerCase() );
	}

}
