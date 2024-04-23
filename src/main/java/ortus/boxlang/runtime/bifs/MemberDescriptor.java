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
package ortus.boxlang.runtime.bifs;

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This describes a member method and how to invoke it
 */
public class MemberDescriptor {

	/**
	 * The name of the member method : "insert", "update", "delete", etc
	 */
	public final Key			name;

	/**
	 * The type this method can be attached to. {@link BoxLangType}
	 */
	public final BoxLangType	type;
	public final Class<?>		customClass;

	/**
	 * The name of the argument that is the object this method is attached to
	 * When calling BIFS. This is the first argument by default.
	 */
	public final Key			objectArgument;

	/**
	 * The BIFDescriptor that describes how to invoke this method
	 */
	public final BIFDescriptor	BIFDescriptor;

	/**
	 * Constructor for a member method
	 *
	 * @param name           The name of the member method
	 * @param type           The type this method can be attached to
	 * @param objectArgument The name of the argument that is the object this method is attached to
	 * @param BIFDescriptor  The BIFDescriptor that describes how to invoke this method
	 */
	public MemberDescriptor(
	    Key name,
	    BoxLangType type,
	    Class<?> customClass,
	    Key objectArgument,
	    BIFDescriptor BIFDescriptor ) {
		this.name			= name;
		this.type			= type;
		this.customClass	= customClass;
		this.objectArgument	= objectArgument;
		this.BIFDescriptor	= BIFDescriptor;
	}

	/**
	 * Invoke the BIF with no arguments
	 *
	 * @param context The context to invoke the BIF in
	 * @param object  The object to invoke the BIF on
	 *
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Object object ) {
		return BIFDescriptor.invoke( context, new Object[] { object }, true, name );
	}

	/**
	 * Invoke the BIF with positional arguments
	 *
	 * @param context             The context to invoke the BIF in
	 * @param object              The object to invoke the BIF on
	 * @param positionalArguments The positional arguments to pass to the BIF
	 *
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Object object, Object[] positionalArguments ) {
		if ( objectArgument != null ) {
			Argument[]			bifArgs		= BIFDescriptor.getBIF().getDeclaredArguments();
			Map<Key, Object>	namedArgs	= new HashMap<Key, Object>();
			int					p			= 0;
			// loop over bif args, and add in any matching positional arguments
			for ( int i = 0; i < bifArgs.length; i++ ) {
				// break if we have no more positional arguments
				if ( positionalArguments.length < p + 1 ) {
					break;
				}
				// Skip the object argument
				if ( !bifArgs[ i ].name().equals( objectArgument ) ) {
					// Note we're adding the bif arg from the loop position, but we're adding the NEXT positional argument
					namedArgs.put( bifArgs[ i ].name(), positionalArguments[ p++ ] );
				}
			}
			return invoke( context, object, namedArgs );
		} else {
			Object[] args = new Object[ positionalArguments.length + 1 ];
			args[ 0 ] = object;
			System.arraycopy( positionalArguments, 0, args, 1, positionalArguments.length );

			// for ( int i = 0; i < args.length; i++ ) {
			// System.out.println( "args i: " + i );
			// System.out.println( args[ i ] );
			// }

			return BIFDescriptor.invoke( context, args, true, name );
		}
	}

	/**
	 * Invoke the BIF with named arguments
	 *
	 * @param context        The context to invoke the BIF in
	 * @param object         The object to invoke the BIF on
	 * @param namedArguments The named arguments to pass to the BIF
	 *
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Object object, Map<Key, Object> namedArguments ) {
		if ( objectArgument != null ) {
			namedArguments.put( objectArgument, object );
		} else {
			Argument[] args = BIFDescriptor.getBIF().getDeclaredArguments();
			if ( args.length == 0 ) {
				throw new BoxRuntimeException(
				    "Function " + BIFDescriptor.name.getName() + " does not accept any arguments and can't be used as a member method." );
			}
			namedArguments.put( args[ 0 ].name(), object );
		}
		return BIFDescriptor.invoke( context, namedArguments, true, name );
	}

}
