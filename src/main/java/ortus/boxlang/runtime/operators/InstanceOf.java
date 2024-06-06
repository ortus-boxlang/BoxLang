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
import java.util.Optional;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
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
			String boxClassName = boxClass.getName().getName();
			if ( boxClassName.equalsIgnoreCase( type )
			    || boxClassName.toLowerCase().endsWith( "." + type.toLowerCase() ) ) {
				return true;
			}
		}

		// Perform exact Java type check
		if ( left.getClass().getName().equalsIgnoreCase( type )
		    // Lucee does some loose typing here, not sure exactly how it works, but it's along these lines
		    || left.getClass().getName().toLowerCase().endsWith( "." + type.toLowerCase() ) ) {

			// System.out.println( "java exact check true" );
			return true;
		}

		// Perform boxClass inheritance check
		if ( boxClass != null ) {
			IClassRunnable _super = boxClass;
			while ( ( _super = _super.getSuper() ) != null ) {
				// For each super class, check if it's the same as the type
				String boxClassName = _super.getName().getName();
				if ( boxClassName.equalsIgnoreCase( type )
				    || boxClassName.toLowerCase().endsWith( "." + type.toLowerCase() ) ) {
					return true;
				}
				// For each super class, check if implements an interface of that type
				if ( checkInterfaces( _super, type ) ) {
					return true;
				}
			}
			if ( checkInterfaces( boxClass, type ) ) {
				return true;
			}

		}

		// Perform Java inheritance check
		Optional<DynamicObject> loadResult = ClassLocator.getInstance().safeLoad( context, type, "java" );
		// true if left's class is the same as, or a superclass or superinterface of javaType
		if ( loadResult.isPresent() && loadResult.get().getTargetClass().isAssignableFrom( left.getClass() ) ) {
			return true;
		}

		return false;
	}

	// TODO: If we allow interfaces to extend another interface, check the full chain of each interface
	private static Boolean checkInterfaces( IClassRunnable boxClass, String type ) {
		List<BoxInterface> interfaces = boxClass.getInterfaces();
		for ( BoxInterface boxInterface : interfaces ) {
			if ( boxInterface.getName().getName().equalsIgnoreCase( type )
			    || boxInterface.getName().getName().toLowerCase().endsWith( "." + type.toLowerCase() ) ) {
				return true;
			}
		}
		return false;
	}
}
