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

import java.math.BigDecimal;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class JavaCast extends BIF {

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

		switch ( type.toLowerCase() ) {
			case "boolean" :
				return convertToBoolean( variable );
			case "double" :
				return convertToDouble( variable );
			case "float" :
				return convertToFloat( variable );
			case "int" :
				return convertToInt( variable );
			case "long" :
				return convertToLong( variable );
			case "string" :
				return variable.toString();
			case "null" :
				return null;
			case "byte" :
				return convertToByte( variable );
			case "bigdecimal" :
				return convertToBigDecimal( variable );
			case "char" :
				return convertToChar( variable );
			case "short" :
				return convertToShort( variable );
			default :
				throw new BoxRuntimeException( "Unsupported Java cast type: " + type );
		}
	}

	private boolean convertToBoolean( Object variable ) {
		if ( variable instanceof String ) {
			String stringValue = ( ( String ) variable ).toLowerCase();
			return "yes".equalsIgnoreCase( stringValue ) || "true".equalsIgnoreCase( stringValue );
		} else if ( variable instanceof Number ) {
			return ( ( Number ) variable ).doubleValue() != 0;
		} else {
			return false;
		}
	}

	private double convertToDouble( Object variable ) {
		if ( variable instanceof Number ) {
			return ( ( Number ) variable ).doubleValue();
		} else if ( variable instanceof String ) {
			return Double.parseDouble( ( String ) variable );
		} else {
			return ( double ) variable;
		}
	}

	private float convertToFloat( Object variable ) {
		if ( variable instanceof String ) {
			try {
				return Float.parseFloat( ( String ) variable );
			} catch ( NumberFormatException e ) {
				throwConversionException( "float", variable );
			}
		} else if ( variable instanceof Number ) {
			return ( ( Number ) variable ).floatValue();
		} else {
			throwConversionException( "float", variable );
		}
		return 0;
	}

	private BigDecimal convertToBigDecimal( Object variable ) {
		if ( variable instanceof String ) {
			try {
				return new BigDecimal( ( String ) variable );
			} catch ( NumberFormatException e ) {
				throwConversionException( "BigDecimal", variable );
			}
		} else if ( variable instanceof Number ) {
			return new BigDecimal( ( ( Number ) variable ).doubleValue() );
		} else {
			throwConversionException( "BigDecimal", variable );
		}
		return null;
	}

	private int convertToInt( Object variable ) {
		if ( variable instanceof String ) {
			try {
				return Integer.parseInt( ( String ) variable );
			} catch ( NumberFormatException e ) {
				throwConversionException( "int", variable );
			}
		} else if ( variable instanceof Number ) {
			return ( ( Number ) variable ).intValue();
		} else {
			throwConversionException( "int", variable );
		}
		return 0;
	}

	private long convertToLong( Object variable ) {
		if ( variable instanceof String ) {
			try {
				return Long.parseLong( ( String ) variable );
			} catch ( NumberFormatException e ) {
				throwConversionException( "long", variable );
			}
		} else if ( variable instanceof Number ) {
			return ( ( Number ) variable ).longValue();
		} else {
			throwConversionException( "long", variable );
		}
		return 0;
	}

	private byte convertToByte( Object variable ) {
		if ( variable instanceof String ) {
			try {
				return Byte.parseByte( ( String ) variable );
			} catch ( NumberFormatException e ) {
				throwConversionException( "byte", variable );
			}
		} else if ( variable instanceof Number ) {
			return ( ( Number ) variable ).byteValue();
		} else {
			throwConversionException( "byte", variable );
		}
		return 0;
	}

	private char convertToChar( Object variable ) {
		if ( variable instanceof String ) {
			return ( ( String ) variable ).charAt( 0 );
		} else if ( variable instanceof Character ) {
			return ( Character ) variable;
		} else {
			throwConversionException( "char", variable );
		}
		return 0;
	}

	private short convertToShort( Object variable ) {
		if ( variable instanceof String ) {
			try {
				return Short.parseShort( ( String ) variable );
			} catch ( NumberFormatException e ) {
				throwConversionException( "short", variable );
			}
		} else if ( variable instanceof Number ) {
			return ( ( Number ) variable ).shortValue();
		} else {
			throwConversionException( "short", variable );
		}
		return 0;
	}

	private void throwConversionException( String type, Object variable ) {
		throw new BoxRuntimeException( "Failed to convert " + variable.getClass().getSimpleName() + " to " + type + ": " + variable );
	}
}
