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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.global.decision.IsObject;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything to a Struct, except I'll also cast any Java classes which are not a built in datatype into a struct, using the public
 * fields as the keys and the values as the values.
 */
public class StructCasterLoose implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a Struct.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Struct
	 *
	 * @return The Struct value
	 */
	public static CastAttempt<IStruct> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Struct, throwing exception if we fail
	 *
	 * @param object The value to cast to a Struct
	 *
	 * @return The Struct value
	 */
	public static IStruct cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Struct
	 *
	 * @param object The value to cast to a Struct
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Struct value
	 */
	public static IStruct cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Struct." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );

		// Struct caster calls Exception Util, which calls us, so avoid a stack overflow here
		if ( ! ( object instanceof Throwable ) ) {
			IStruct result = StructCaster.cast( object, false );
			if ( result != null ) {
				return result;
			}
		}

		// Special Productivity Hack: If it's a Query object, take the first row and return it as a struct
		if ( object instanceof Query query ) {
			return query.isEmpty() ? new Struct() : query.getRowAsStruct( 0 );
		}

		// If it's a random Java class, then turn it into a struct!!
		if ( IsObject.isObject( object ) ) {
			IStruct			thisResult	= new Struct();
			DynamicObject	dynObject;

			// Get the fields and methods of the class
			dynObject = DynamicObject.of( object );
			dynObject.getFieldsAsStream()
			    .filter( field -> Modifier.isPublic( field.getModifiers() ) )
			    .forEach( field -> {
				    thisResult.put( field.getName(), dynObject.getField( field.getName() ).get() );
			    } );
			// also add fields for all public methods starting with "get" that take no arguments
			dynObject.getMethodNames( true ).forEach( methodName -> {
				Method m;
				if ( methodName.startsWith( "get" ) && Modifier.isPublic( ( m = dynObject.getMethod( methodName, true ) ).getModifiers() )
				    && m.getParameterCount() == 0 && !methodName.equals( "getClass" ) ) {
					thisResult.put( methodName.substring( 3 ), dynObject.invoke( BoxRuntime.getInstance().getRuntimeContext(), methodName ) );
				}
			} );

			// Force the overloaded method expecting a class.
			// Get the static methods and fields of the Class that the Class represents
			if ( object instanceof Class clazz ) {
				DynamicObject dynObject2 = DynamicObject.of( clazz );
				dynObject2.getFieldsAsStream()
				    // get public, static fields
				    .filter( field -> Modifier.isPublic( field.getModifiers() ) && Modifier.isStatic( field.getModifiers() ) )
				    .forEach( field -> {
					    thisResult.put( field.getName(), dynObject2.getField( field.getName() ).get() );
				    } );
				// also add fields for all public methods starting with "get" that take no arguments
				dynObject2.getMethodNames( true ).forEach( methodName -> {
					if ( methodName.startsWith( "get" ) ) {
						Method	m			= dynObject2.getMethod( methodName, true );
						int		modifiers	= m.getModifiers();
						if ( Modifier.isPublic( modifiers ) && Modifier.isStatic( modifiers ) && m.getParameterCount() == 0 ) {
							thisResult.put( methodName.substring( 3 ), dynObject2.invokeStatic( BoxRuntime.getInstance().getRuntimeContext(), methodName ) );
						}
					}
				} );
			}

			return thisResult;
		}

		if ( fail ) {
			throw new BoxCastException(
			    String.format( "Can't cast [%s] to a Struct.", object.getClass().getName() )
			);
		} else {
			return null;
		}
	}

}
