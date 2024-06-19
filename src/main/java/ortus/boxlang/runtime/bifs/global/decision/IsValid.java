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
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.ValidationUtil;

@BoxBIF
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
	 * Determine whether the given value is a string, numeric, or date.Arrays, structs, queries, closures, classes and components, and other complex
	 * structures will return false.
	 * <p>
	 * Note we expressly do not support the `eurodate` type, since date formats vary across EU countries. For this, prefer the `LSIsDate( date, locale )`
	 * method instead.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The type to validate the value against
	 *
	 * @argument.value Value to test for validaty on a given type
	 *
	 * @argument.min The minimum value for the range type or a pattern to validate the value against
	 *
	 * @argument.max The maximum value for the range type
	 *
	 * @argument.pattern The pattern to validate the value against
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IsValidType	type	= IsValidType.fromString( arguments.getAsString( Key.type ) );
		Object		min		= arguments.get( Key.min );
		String		pattern	= arguments.getAsString( Key.pattern );

		// if the type is regex or regular_express, but no pattern is provided, throw an error
		if ( ( type == IsValidType.REGEX || type == IsValidType.REGULAR_EXPRESSION ) && pattern.isEmpty() ) {
			// Try to get it from the `min` argument
			if ( min != null ) {
				pattern = min.toString();
			} else {
				throw new IllegalArgumentException( "The pattern argument is required for the regex and regular_expression types." );
			}
		}

		return switch ( type ) {
			/**
			 * Implemented in GenericCaster, mostly non-string types.
			 */
			case ANY -> GenericCaster.attempt( context, arguments.get( Key.value ), "any" ).wasSuccessful();
			case ARRAY -> GenericCaster.attempt( context, arguments.get( Key.value ), "array" ).wasSuccessful();
			case BOOLEAN -> GenericCaster.attempt( context, arguments.get( Key.value ), "boolean" ).wasSuccessful();
			case DATE -> GenericCaster.attempt( context, arguments.get( Key.value ), "datetime" ).wasSuccessful();
			case FLOAT -> ValidationUtil.isFloat( arguments.get( Key.value ) );
			case QUERY -> GenericCaster.attempt( context, arguments.get( Key.value ), "query" ).wasSuccessful();
			case STRING -> GenericCaster.attempt( context, arguments.get( Key.value ), "string" ).wasSuccessful();
			case STRUCT -> GenericCaster.attempt( context, arguments.get( Key.value ), "struct" ).wasSuccessful();
			case TIME -> GenericCaster.attempt( context, arguments.get( Key.value ), "time" ).wasSuccessful();
			case XML -> GenericCaster.attempt( context, arguments.get( Key.value ), "xml" ).wasSuccessful();

			/**
			 * Implemented in ValidationUtil
			 */
			case BINARY -> ValidationUtil.isBinary( arguments.get( Key.value ) );
			case CREDITCARD -> ValidationUtil.isValidCreditCard( castAsStringOrNull( arguments.get( Key.value ) ) );
			case CLOSURE -> ValidationUtil.isClosure( arguments.get( Key.value ) );
			case COMPONENT, CLASS -> ValidationUtil.isBoxClass( arguments.get( Key.value ) );
			case GUID -> ValidationUtil.isValidGUID( castAsStringOrNull( arguments.get( Key.value ) ) );
			case EMAIL -> ValidationUtil.isValidEmail( castAsStringOrNull( arguments.get( Key.value ) ) );
			case FUNCTION -> ValidationUtil.isFunction( arguments.get( Key.value ) );
			case INTEGER -> ValidationUtil.isValidInteger( arguments.get( Key.value ) );
			case LAMBDA -> ValidationUtil.isLambda( arguments.get( Key.value ) );
			case NUMERIC -> ValidationUtil.isValidNumeric( arguments.get( Key.value ) );
			case RANGE -> ValidationUtil.isValidRange(
			    arguments.get( Key.value ),
			    DoubleCaster.cast( arguments.get( Key.min ) ),
			    DoubleCaster.cast( arguments.get( Key.max ) )
			);
			case REGEX, REGULAR_EXPRESSION -> ValidationUtil.isValidPattern(
			    castAsStringOrNull( arguments.get( Key.value ) ),
			    pattern
			);
			case SSN, SOCIAL_SECURITY_NUMBER -> ValidationUtil.isValidSSN( castAsStringOrNull( arguments.get( Key.value ) ) );
			case TELEPHONE -> ValidationUtil.isValidTelephone( castAsStringOrNull( arguments.get( Key.value ) ) );
			case URL -> ValidationUtil.isValidURL( castAsStringOrNull( arguments.get( Key.value ) ) );
			case UDF -> ValidationUtil.isUDF( arguments.get( Key.value ) );
			case UUID -> ValidationUtil.isValidUUID( castAsStringOrNull( arguments.get( Key.value ) ) )
			    || ValidationUtil.isValidGUID( castAsStringOrNull( arguments.get( Key.value ) ) );
			case VARIABLENAME -> ValidationUtil.isValidVariableName( castAsStringOrNull( arguments.get( Key.value ) ) );
			case USDATE -> context.invokeFunction( Key.of( "LSIsDate" ),
			    java.util.Map.of( Key.date, arguments.get( Key.value ), Key.locale, "en_US" ) );
			case ZIPCODE -> ValidationUtil.isValidZipCode( castAsStringOrNull( arguments.get( Key.value ) ) );

			default -> throw new IllegalArgumentException( "Invalid type: " + type + ". Valid types are: " + Arrays.toString( IsValidType.toArray() ) );
		};
	}

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
		INTEGER( "integer" ),
		LAMBDA( "lambda" ),
		NUMERIC( "numeric" ),
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
				    String.format( "Invalid type [%s], must be one of %s", type, Arrays.toString( IsValidType.values() ) )
				);
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
