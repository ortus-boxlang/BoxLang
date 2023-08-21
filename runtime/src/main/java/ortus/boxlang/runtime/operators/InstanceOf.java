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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;

/**
 * Performs instance of check.
 * https://helpx.adobe.com/coldfusion/cfml-reference/coldfusion-functions/functions-in-k/isinstanceof.html
 *
 * - The object specified by the first parameter is an instance of the interface or component specified by the second parameter.
 * - The Java object specified by the first parameter was created by using the cfobject tag or CreateObject method and is an instance of the Java
 * class specified by the second parameter.
 * - The object specified by the first parameter is an instance of a component that extends the component specified in the second parameter.
 * - The object specified by the first parameter is an instance of a component that extends a component that implements the interface specified in the
 * second parameter.
 * - The Java object specified by the first parameter was created by using the cfobject tag or CreateObjectmethod and is an instance of a Java class
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

		String		type		= StringCaster.cast( right );
		Class<?>	javaType	= null;

		left = DynamicObject.unWrap( left );

		// TODO: Determine if type is a known CFC type
		// Right now, just doing Java type checking

		// TODO: First perform exact CFC check

		// Perform exact Java type check
		if ( left.getClass().getName().equalsIgnoreCase( type )
		        // Lucee does some loose typing here, not sure exactly how it works, but it's along these lines
		        || left.getClass().getName().toLowerCase().endsWith( "." + type.toLowerCase() ) ) {
			return true;
		}

		// TODO: Perform CFC inheritance check

		// Perform Java inheritance check
		// TODO: swap this for method that doesn't error when Luis provides
		try {
			javaType = ClassLocator.getInstance().load( context, type ).getTargetClass();
			// true if left's class is the same as, or a superclass or superinterface of javaType
			if ( javaType.isAssignableFrom( left.getClass() ) ) {
				return true;
			}
		} catch ( ClassNotFoundException e ) {
			return false;
		}

		return false;
	}
}
