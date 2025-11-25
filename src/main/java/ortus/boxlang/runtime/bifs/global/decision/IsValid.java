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
package ortus.boxlang.runtime.bifs.global.decision;

import java.util.Arrays;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.ValidationUtil;

@BoxBIF( description = "Validate data against specified criteria" )
public class IsValid extends BIF {

	/**
	 * Constructor
	 */
	public IsValid() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.type ),
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "any", Key.min ),
		    new Argument( false, "any", Key.max ),
		    new Argument( false, "any", Key.pattern, "" ),
		};
	}

	/**
	 * Validates the incoming <code>value</code> against the given
	 * <code>type</code>. If the type is a range, the value is
	 * validated against the range. If the type is a pattern, the value is validated
	 * against the pattern. If the type is a
	 * date, the value is validated against the date format. If the type is a locale
	 * date, the value is validated against the
	 * locale date format. If the type is a regular expression, the value is
	 * validated against the regular expression.
	 * <p>
	 * <strong>
	 * Note we expressly do not support the `eurodate` type, since date formats vary
	 * across EU countries. For this, prefer the `LSIsDate( date, locale )`
	 * method instead.
	 * </strong>
	 * <p>
	 * <h2>Valid Types</h2>
	 * <ul>
	 * <li>array</li>
	 * <li>binary</li>
	 * <li>boolean</li>
	 * <li>component</li>
	 * <li>creditcard</li>
	 * <li>date</li>
	 * <li>email</li>
	 * <li>float</li>
	 * <li>function</li>
	 * <li>guid</li>
	 * <li>hex</li>
	 * <li>integer</li>
	 * <li>numeric</li>
	 * <li>number</li>
	 * <li>query</li>
	 * <li>range</li>
	 * <li>regex</li>
	 * <li>regular_expression</li>
	 * <li>social_security_number</li>
	 * <li>ssn</li>
	 * <li>string</li>
	 * <li>struct</li>
	 * <li>telephone</li>
	 * <li>time</li>
	 * <li>time</li>
	 * <li>url</li>
	 * <li>usdate</li>
	 * <li>uuid</li>
	 * <li>variablename</li>
	 * <li>xml</li>
	 * <li>zipcode</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The type to validate the value against
	 *
	 * @argument.value Value to test for validaty on a given type
	 *
	 * @argument.min The minimum value for the range type or a pattern to validate
	 *               the value against
	 *
	 * @argument.max The maximum value for the range type
	 *
	 * @argument.pattern The pattern to validate the value against
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IsValidType	type	= IsValidType.fromString( arguments.getAsString( Key.type ) );
		Object		min		= arguments.get( Key.min );
		String		pattern	= arguments.getAsString( Key.pattern );

		// if the type is regex or regular_express, but no pattern is provided, throw an
		// error
		if ( ( type == IsValidType.REGEX || type == IsValidType.REGULAR_EXPRESSION ) && pattern.isEmpty() ) {
			// Try to get it from the `min` argument
			if ( min != null ) {
				pattern = min.toString();
			} else {
				throw new IllegalArgumentException(
				    "The pattern argument is required for the regex and regular_expression types." );
			}
		}

		Object value = arguments.get( Key.value );
		return switch ( type ) {
			/**
			 * Implemented in GenericCaster, mostly non-string types.
			 */
			case ANY -> true;
			case ARRAY -> GenericCaster.cast( context, value, "array", false ) != null;
			// BooleanCaster will cast null to false, but we don't want null to be considered valid for the sake of this BIF.
			case BOOLEAN -> value != null && BooleanCaster.cast( value, false, false ) != null;
			case DATE -> GenericCaster.cast( context, value, "datetime", false ) != null;
			case FLOAT -> ValidationUtil.isFloat( value );
			case QUERY -> GenericCaster.cast( context, value, "query", false ) != null;
			case STRING -> GenericCaster.cast( context, value, "string", false ) != null;
			case STRUCT -> GenericCaster.cast( context, value, "struct", false ) != null;
			case TIME -> GenericCaster.cast( context, value, "time", false ) != null;
			case XML -> GenericCaster.cast( context, value, "xml", false ) != null;

			/**
			 * Implemented in ValidationUtil
			 */
			case BINARY -> ValidationUtil.isBinary( value );
			case CREDITCARD -> ValidationUtil.isValidCreditCard( castAsStringOrNull( value ) );
			case CLOSURE -> ValidationUtil.isClosure( value );
			case COMPONENT, CLASS -> ValidationUtil.isBoxClass( value );
			case GUID -> ValidationUtil.isValidGUID( castAsStringOrNull( value ) );
			case EMAIL -> ValidationUtil.isValidEmail( castAsStringOrNull( value ) );
			case FUNCTION -> ValidationUtil.isFunction( value );
			case HEX -> ValidationUtil.isValidHexString( castAsStringOrNull( value ) );
			case INTEGER -> ValidationUtil.isValidInteger( value );
			case LAMBDA -> ValidationUtil.isLambda( value );
			case NUMERIC -> ValidationUtil.isValidNumeric( value );
			case NUMBER -> ValidationUtil.isValidNumeric( value );
			case RANGE -> ValidationUtil.isValidRange(
			    value,
			    NumberCaster.cast( arguments.get( Key.min ) ),
			    NumberCaster.cast( arguments.get( Key.max ) ) );
			case REGEX, REGULAR_EXPRESSION -> ValidationUtil.isValidPattern(
			    castAsStringOrNull( value ),
			    pattern );
			case SSN, SOCIAL_SECURITY_NUMBER -> ValidationUtil.isValidSSN( castAsStringOrNull( value ) );
			case TELEPHONE -> ValidationUtil.isValidTelephone( castAsStringOrNull( value ) );
			case URL -> ValidationUtil.isValidURL( castAsStringOrNull( value ) );
			case UDF -> ValidationUtil.isUDF( value );
			case UUID -> ValidationUtil.isValidUUID( castAsStringOrNull( value ) )
			    || ValidationUtil.isValidGUID( castAsStringOrNull( value ) );
			case VARIABLENAME -> ValidationUtil.isValidVariableName( castAsStringOrNull( value ) );
			case USDATE -> context.invokeFunction( Key.of( "IsDate" ),
			    java.util.Map.of( Key.date, value, Key.locale, "en_US" ) );
			case ZIPCODE -> ValidationUtil.isValidZipCode( castAsStringOrNull( value ) );

			default -> throw new IllegalArgumentException(
			    "Invalid type: " + type + ". Valid types are: " + Arrays.toString( IsValidType.toArray() ) );
		};
	}

	/**
	 * Helper method to cast the value to a string or null if the value is null.
	 *
	 * @param value The value to cast to a string or null.
	 *
	 * @return The value as a string or null if the value is null.
	 */
	public String castAsStringOrNull( Object value ) {
		return StringCaster.cast( value, false );
	}

	/**
	 * Enum for the various types that can be validated.
	 */
	private enum IsValidType {

		ANY( "any" ),
		ARRAY( "array" ),
		BINARY( "binary" ),
		BOOLEAN( "boolean" ),
		CLASS( "class" ),
		CLOSURE( "closure" ),
		COMPONENT( "component" ),
		CREDITCARD( "creditcard" ),
		EMAIL( "email" ),
		DATE( "date" ),
		FLOAT( "float" ),
		FUNCTION( "function" ),
		GUID( "guid" ),
		HEX( "hex" ),
		INTEGER( "integer" ),
		LAMBDA( "lambda" ),
		NUMERIC( "numeric" ),
		NUMBER( "number" ),
		QUERY( "query" ),
		RANGE( "range" ),
		REGEX( "regex" ),
		REGULAR_EXPRESSION( "regular_expression" ),
		SOCIAL_SECURITY_NUMBER( "social_security_number" ),
		SSN( "ssn" ),
		STRING( "string" ),
		STRUCT( "struct" ),
		TELEPHONE( "telephone" ),
		TIME( "time" ),
		UDF( "udf" ),
		URL( "url" ),
		USDATE( "usdate" ),
		UUID( "uuid" ),
		VARIABLENAME( "variablename" ),
		XML( "xml" ),
		ZIPCODE( "zipcode" );

		/**
		 * The key representing the type
		 */
		private final Key key;

		/**
		 * Constructor
		 *
		 * @param type The name of the event.
		 */
		IsValidType( String type ) {
			this.key = Key.of( type );
		}

		/**
		 * Returns the key representing the type
		 *
		 * @return The key representing the type
		 */
		public Key getKey() {
			return key;
		}

		/**
		 * Returns an array of all the valid type keys.
		 *
		 * @return An array of all the valid type keys.
		 */
		public static String[] toArray() {
			return Arrays.stream( values() )
			    .map( val -> val.key.getName() )
			    .sorted()
			    .toArray( String[]::new );

		}

		/**
		 * Validate if the incoming value is a valid validation type.
		 *
		 * @param value The key value to validate
		 *
		 * @return True if the value is a valid validation type, false otherwise
		 */
		public static boolean isValid( Key value ) {
			for ( IsValidType type : IsValidType.values() ) {
				if ( type.getKey().equals( value ) ) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Validate if the incoming value is a valid valid type.
		 *
		 * @param value The key value to validate
		 *
		 * @return True if the value is a valid valid type, false otherwise
		 */
		@SuppressWarnings( "unused" )
		public static boolean isValid( String value ) {
			return isValid( Key.of( value ) );
		}

		/**
		 * Returns the IsValidType enum value from the given string.
		 *
		 * @param type The string to convert to an IsValidType enum value.
		 *
		 * @return The IsValidType enum value.
		 */
		public static IsValidType fromString( String type ) {
			try {
				return IsValidType.valueOf( type.trim().toUpperCase() );
			} catch ( IllegalArgumentException e ) {
				throw new IllegalArgumentException(
				    String.format( "Invalid type [%s], must be one of %s", type,
				        Arrays.toString( IsValidType.values() ) ) );
			}
		}

		/**
		 * Returns the valid type for the given key as a string.
		 */
		@Override
		public String toString() {
			return this.key.getName();
		}
	}
}
