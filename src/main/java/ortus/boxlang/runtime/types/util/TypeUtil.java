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
package ortus.boxlang.runtime.types.util;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.IType;

/**
 * Utility class for type related operations
 *
 * @author BoxLang Development Team
 *
 * @since 1.6.0
 */
public class TypeUtil {

	/**
	 * Get the name of an object or class suitable for error messages.
	 * Adjust this further as neccessary.
	 * 
	 * - For null it returns the string "null"
	 * - DynamicObjects are unwrapped
	 * - BL classes use the class name
	 * - Java Class objects are represented as Class<FQN>
	 * - Java arrays are represented as FQN[]
	 * - BoxLang types (IType) use their BoxLang type name
	 * - Java primitive wrappers (String, Integer, Boolean, etc) in the java.lang package use their simple name
	 * - Java Number types are represented as Number<name>
	 * - All other objects, return the Java class name
	 * 
	 * @param obj The object or class
	 * 
	 * @return The name of the object or class
	 */
	public static String getObjectName( Object obj ) {
		if ( obj == null ) {
			return "null";
		}

		obj = DynamicObject.unWrap( obj );

		if ( obj instanceof Class clazz ) {
			return "Class<" + getSimplishName( clazz ) + ">";
		}

		if ( obj instanceof IClassRunnable icr ) {
			return icr.bxGetName().getName();
		}

		if ( obj instanceof IType it ) {
			return it.getBoxTypeName();
		}

		Class<?> clazz = obj.getClass();
		if ( clazz.isArray() ) {
			return getArrayTypeName( clazz );
		}

		String name = getSimplishName( clazz );

		if ( obj instanceof Number ) {
			return "Number<" + name + ">";
		}

		return name;
	}

	/**
	 * Get the type name for an array class
	 * 
	 * @param arrayClass The array class
	 * 
	 * @return The type name
	 */
	private static String getArrayTypeName( Class<?> arrayClass ) {
		int			dimensions		= 0;

		// Count dimensions and get the base component type
		Class<?>	componentType	= arrayClass;
		while ( componentType.isArray() ) {
			dimensions++;
			componentType = componentType.getComponentType();
		}

		String brackets = "[]".repeat( dimensions );
		return getSimplishName( componentType ) + brackets;
	}

	/**
	 * Get a simplified name for a class. If it's in the java.lang package, return just the simple name, otherwise return the full name.
	 * 
	 * @param clazz The class
	 * 
	 * @return The simplified name
	 */
	private static String getSimplishName( Class<?> clazz ) {
		if ( clazz.getPackageName().equals( "java.lang" ) ) {
			return clazz.getSimpleName();
		} else {
			return clazz.getName();
		}
	}
}
