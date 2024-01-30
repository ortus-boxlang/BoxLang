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

import org.apache.commons.lang3.math.NumberUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
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
		    // @TODO: Add support for a `pattern` argument, but positionally it would be in the `min` slot, i.e. the 3rd argument.
		    // See https://docs.lucee.org/reference/functions/isvalid.html
		    new Argument( false, "any", Key.pattern ),
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
	 * @argument.value Value to test for validaty on a given type
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		IsValidType	type	= IsValidType.fromString( arguments.getAsString( Key.type ) );
		Object		value	= arguments.get( Key.value );
		return switch ( type ) {
			/*
			 * Implemented in GenericCaster, mostly non-string types.
			 */
			case ANY -> GenericCaster.attempt( value, "any" ).wasSuccessful();
			case ARRAY -> GenericCaster.attempt( value, "array" ).wasSuccessful();
			case BOOLEAN -> GenericCaster.attempt( value, "boolean" ).wasSuccessful();
			case DATE -> GenericCaster.attempt( value, "datetime" ).wasSuccessful();
			case STRING -> GenericCaster.attempt( value, "string" ).wasSuccessful();
			case STRUCT -> GenericCaster.attempt( value, "struct" ).wasSuccessful();
			case TIME -> GenericCaster.attempt( value, "datetime" ).wasSuccessful();
			/*
			 * Implemented in ValidationUtil
			 */
			// case BINARY -> ValidationUtil.isValidBINARY( value );
			// case CREDITCARD -> ValidationUtil.isValidCREDITCARD( value );
			// case COMPONENT -> ValidationUtil.isValidCOMPONENT( value );
			// case EMAIL -> ValidationUtil.isValidEMAIL( value );
			// case FLOAT -> value instanceof Float || ???
			// case GUID -> ValidationUtil.isValidGUID( value );
			case INTEGER -> value instanceof Integer || ( value instanceof String stringVal && NumberUtils.isDigits( stringVal ) );
			case NUMERIC -> value instanceof Number || ( value instanceof String stringVal && NumberUtils.isCreatable( stringVal ) );
			// case QUERY -> ValidationUtil.isValidQUERY( value );
			// case RANGE -> ValidationUtil.isValidRANGE( value );
			// case REGEX, REGULAR_EXPRESSION -> ValidationUtil.isValidREGEX( value.toString() );
			case SSN, SOCIAL_SECURITY_NUMBER -> ValidationUtil.isValidSSN( value.toString() );
			case TELEPHONE -> ValidationUtil.isValidTelephone( value.toString() );
			// case URL -> ValidationUtil.isValidURL( value );
			case UUID -> ValidationUtil.isValidCFUUID( value.toString() ) || ValidationUtil.isValidUUID( value.toString() );
			case USDATE -> context.invokeFunction( Key.of( "LSIsDate" ), java.util.Map.of( Key.date, value, Key.locale, "en_US" ) );
			// case VARIABLENAME -> ValidationUtil.isValidVARIABLENAME( value );
			// case XML -> ValidationUtil.isValidXML( value );
			case ZIPCODE -> ValidationUtil.isValidZipCode( value.toString() );
			// case LAMBDA -> ValidationUtil.isValidLAMBDA( value );
			// case FUNCTION -> ValidationUtil.isValidFUNCTION( value );
			// case CLOSURE -> ValidationUtil.isValidCLOSURE( value );
		};
	}

	private enum IsValidType {

		ANY,
		ARRAY,
	    // BINARY,
		BOOLEAN,
	    // CREDITCARD,
	    // COMPONENT,
		DATE,
	    // EMAIL,
	    // FLOAT,
	    // GUID,
		INTEGER,
		NUMERIC,
	    // QUERY,
	    // RANGE,
	    // REGEX,
	    // REGULAR_EXPRESSION,
		SSN,
		SOCIAL_SECURITY_NUMBER,
		STRING,
		STRUCT,
		TELEPHONE,
		TIME,
	    // URL,
		UUID,
		USDATE,
	    // VARIABLENAME,
	    // XML,
		ZIPCODE,

		// // possibly Lucee only?
		// LAMBDA,
		// FUNCTION,
		// CLOSURE
		;

		public static IsValidType fromString( String type ) {
			try {
				// @TODO: Consider moving these enum values into the Key class for performance reasons over the string lowercase match.
				return IsValidType.valueOf( type.trim().toUpperCase() );
			} catch ( IllegalArgumentException e ) {
				throw new IllegalArgumentException(
				    String.format( "Invalid type [%s], must be one of %s", type, Arrays.toString( IsValidType.values() ) )
				);
			}
		}
	}
}