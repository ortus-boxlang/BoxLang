/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class JavaCast extends BIF {

	public static final class types {

		public static final Key	BOOLEAN		= Key.of( "boolean" );
		public static final Key	DOUBLE		= Key.of( "double" );
		public static final Key	FLOAT		= Key.of( "float" );
		public static final Key	INT			= Key.of( "int" );
		public static final Key	LONG		= Key.of( "long" );
		public static final Key	STRING		= Key.of( "string" );
		public static final Key	NULL		= Key.of( "null" );
		public static final Key	BYTE		= Key.of( "byte" );
		public static final Key	BIGDECIMAL	= Key.of( "bigdecimal" );
		public static final Key	CHAR		= Key.of( "char" );
		public static final Key	SHORT		= Key.of( "short" );
	}

	/**
	 * Constructor
	 */
	public JavaCast() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.type ),
		    new Argument( true, "any", Key.variable )
		};
	}

	/**
	 * Cast a variable to a specified Java type
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The name of a Java primitive or a Java class name.
	 * 
	 * @argument.variable The variable, Java object, or array to cast.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	type		= arguments.getAsString( Key.type );
		Object	variable	= arguments.get( Key.variable );

		if ( variable == null ) {
			return null;
		}

		Key typeKey = Key.of( type );

		if ( typeKey.equals( types.BOOLEAN ) ) {
			return GenericCaster.cast( variable, "boolean" );
		} else if ( typeKey.equals( types.DOUBLE ) ) {
			return GenericCaster.cast( variable, "double" );
		} else if ( typeKey.equals( types.FLOAT ) ) {
			return GenericCaster.cast( variable, "float" );
		} else if ( typeKey.equals( types.INT ) ) {
			return GenericCaster.cast( variable, "int" );
		} else if ( typeKey.equals( types.LONG ) ) {
			return GenericCaster.cast( variable, "long" );
		} else if ( typeKey.equals( types.STRING ) ) {
			return GenericCaster.cast( variable, "string" );
		} else if ( typeKey.equals( types.NULL ) ) {
			return GenericCaster.cast( variable, "null" );
		} else if ( typeKey.equals( types.BYTE ) ) {
			return GenericCaster.cast( variable, "byte" );
		} else if ( typeKey.equals( types.BIGDECIMAL ) ) {
			return GenericCaster.cast( variable, "bigdecimal" );
		} else if ( typeKey.equals( types.CHAR ) ) {
			return GenericCaster.cast( variable, "char" );
		} else if ( typeKey.equals( types.SHORT ) ) {
			return GenericCaster.cast( variable, "short" );
		} else {
			throw new BoxRuntimeException( "Unsupported Java cast type: " + type );
		}
	}
}
