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
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.UDF;
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
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IsValidType type = IsValidType.fromString( arguments.getAsString( Key.type ) );
		return switch ( type ) {
			/*
			 * Implemented in GenericCaster, mostly non-string types.
			 */
			case ANY -> GenericCaster.attempt( context, arguments.get( Key.value ), "any" ).wasSuccessful();
			case ARRAY -> GenericCaster.attempt( context, arguments.get( Key.value ), "array" ).wasSuccessful();
			case BOOLEAN -> GenericCaster.attempt( context, arguments.get( Key.value ), "boolean" ).wasSuccessful();
			case DATE -> GenericCaster.attempt( context, arguments.get( Key.value ), "datetime" ).wasSuccessful();
			case STRING -> GenericCaster.attempt( context, arguments.get( Key.value ), "string" ).wasSuccessful();
			case STRUCT -> GenericCaster.attempt( context, arguments.get( Key.value ), "struct" ).wasSuccessful();
			case TIME -> GenericCaster.attempt( context, arguments.get( Key.value ), "datetime" ).wasSuccessful();

			/*
			 * Implemented in ValidationUtil
			 */
			case CREDITCARD -> ValidationUtil.isValidCreditCard( arguments.getAsString( Key.value ) );
			case COMPONENT -> arguments.get( Key.value ) instanceof IClassRunnable;
			case GUID -> ValidationUtil.isValidGUID( arguments.getAsString( Key.value ) );
			case INTEGER -> ValidationUtil.isValidInteger( arguments.get( Key.value ) );
			case NUMERIC -> ValidationUtil.isValidNumeric( arguments.get( Key.value ) );
			case SSN, SOCIAL_SECURITY_NUMBER -> ValidationUtil.isValidSSN( arguments.getAsString( Key.value ) );
			case TELEPHONE -> ValidationUtil.isValidTelephone( arguments.getAsString( Key.value ) );
			case URL -> ValidationUtil.isValidURL( arguments.getAsString( Key.value ) );
			case UUID -> ValidationUtil.isValidUUID( arguments.getAsString( Key.value ) ) || ValidationUtil.isValidGUID( arguments.getAsString( Key.value ) );
			case USDATE -> context.invokeFunction( Key.of( "LSIsDate" ),
			    java.util.Map.of( Key.date, arguments.getAsString( Key.value ), Key.locale, "en_US" ) );
			case ZIPCODE -> ValidationUtil.isValidZipCode( arguments.getAsString( Key.value ) );

			/*
			 * @TODO: Implement these!
			 */
			// case BINARY -> ValidationUtil.isValidBINARY( arguments.get( Key.value ) );
			// case EMAIL -> ValidationUtil.isValidEMAIL( arguments.getAsString( Key.value ) );
			// case FLOAT -> value instanceof Float || ???
			// case QUERY -> ValidationUtil.isValidQUERY( value );
			// case RANGE -> ValidationUtil.isValidRANGE( value );
			// case REGEX, REGULAR_EXPRESSION -> ValidationUtil.isValidREGEX( arguments.getAsString( Key.value ) );
			// case VARIABLENAME -> ValidationUtil.isValidVARIABLENAME( value );
			// case XML -> ValidationUtil.isValidXML( value );

			/*
			 * Lucee Only:
			 */
			case LAMBDA -> arguments.get( Key.value ) instanceof Lambda;
			case FUNCTION -> arguments.get( Key.value ) instanceof UDF;
			case CLOSURE -> arguments.get( Key.value ) instanceof Closure;
		};
	}

	private enum IsValidType {

		ANY,
		ARRAY,
	    // BINARY,
		BOOLEAN,
		CREDITCARD,
		COMPONENT,
		DATE,
	    // EMAIL,
	    // FLOAT,
		GUID,
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
		URL,
		UUID,
		USDATE,
	    // VARIABLENAME,
	    // XML,
		ZIPCODE,
	    // Lucee Only:
		LAMBDA,
		FUNCTION,
		CLOSURE;

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