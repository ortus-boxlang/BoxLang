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
package ortus.boxlang.runtime.bifs.global.format;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Format a number using a specified mask" )
@BoxMember( type = BoxLangType.NUMERIC )
@BoxBIF( alias = "LSNumberFormat" )

public class NumberFormat extends BIF {

	/**
	 * Static regex patterns for performance optimization
	 */
	private static final Pattern	AFTER_COMMA_PATTERN						= Pattern.compile( "[90_#]*\\.?[90_#]*" );
	private static final Pattern	DECIMAL_SPLIT_PATTERN					= Pattern.compile( "\\." );
	private static final Pattern	NINE_REPLACEMENT_PATTERN				= Pattern.compile( "9" );
	private static final Pattern	ALL_ZEROS_GROUPED_PATTERN				= Pattern.compile( "0+(,0+)+" );
	private static final Pattern	ALL_ZEROS_GROUPED_WITH_DECIMAL_PATTERN	= Pattern.compile( "0+(,0+)+\\..*" );

	/**
	 * Constructor
	 */
	public NumberFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "string", Key.mask ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Formats a number with an optional format mask
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to be formatted, or an empty string which will be treated as 0.
	 *
	 * @argument.mask The formatting mask to apply using the {@link java.text.DecimalFormat} patterns.
	 *
	 * @argument.locale An optional locale string to apply to the format
	 *
	 * @function.currencyFormat Formats a number as a currency value
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object oValue = arguments.get( Key.number );
		if ( oValue == null ) {
			oValue = 0;
		}
		// Turn "" into 0
		if ( oValue instanceof String sValue && sValue.isEmpty() ) {
			oValue = 0;
		}
		// "" is the only valid string valie. Any other non-numbers will error out here
		Number								value				= NumberCaster.cast( oValue );
		String								format				= arguments.getAsString( Key.mask );
		Locale								locale				= LocalizationUtil.parseLocaleFromContext( context, arguments );
		java.text.NumberFormat				formatter			= LocalizationUtil.localizedDecimalFormatter(
		    locale,
		    LocalizationUtil.NUMBER_FORMAT_PATTERNS.get( LocalizationUtil.DEFAULT_NUMBER_FORMAT_KEY )
		);
		final LinkedHashMap<String, String>	formatReplacements	= new LinkedHashMap<>() {

																	{
																		put( "9", "0" );
																		// Fix incorrect number positionals with dollar notation
																		put( "_$", "$_" );
																		// This is a special case to ensure preceeding zeroes before the decimal. Using `#` will leave those blank
																		put( "_.", "0." );
																		// Standard replacement
																		put( "_", "#" );
																		put( "#,.", "#,##0." );
																		put( "$#,0", "$#,##0" ); // Fix currency with single digit grouping
																		put( "#$,0", "$#,##0" );
																	}
																};

		// Currency-specific arguments
		String								type				= arguments.getAsString( Key.type );
		if ( type != null ) {
			formatter = ( java.text.NumberFormat ) LocalizationUtil.localizedCurrencyFormatter( locale, type );
			// Format parsing
		} else if ( format != null ) {
			if ( format.equals( "$" ) ) {
				format = "USD";
			}
			Key formatKey = Key.of( format );
			if ( LocalizationUtil.COMMON_NUMBER_FORMATTERS.containsKey( formatKey ) ) {
				formatter = ( java.text.NumberFormat ) LocalizationUtil.COMMON_NUMBER_FORMATTERS.get( formatKey );
			} else if ( LocalizationUtil.NUMBER_FORMAT_PATTERNS.containsKey( formatKey ) ) {
				formatter = LocalizationUtil.localizedDecimalFormatter( locale, format );
			} else if ( format.equals( "ls$" ) ) {
				formatter = LocalizationUtil.localizedCurrencyFormatter( locale );
			} else {
				// A leading comma (e.g. ",9") is incorrect but is parsed in other engines"
				if ( format.startsWith( "," ) ) {
					String afterComma = format.substring( 1 );
					if ( afterComma.isEmpty() || AFTER_COMMA_PATTERN.matcher( afterComma ).matches() ) {
						format = "#,##0" + ( afterComma.contains( "." ) ? afterComma.substring( afterComma.indexOf( '.' ) ) : "" );
					}
				}

				// Pre-replace any 9's before the decimal to ensure no leading blanks
				if ( format.contains( "." ) ) {
					String[] parts = DECIMAL_SPLIT_PATTERN.split( format );
					if ( !parts[ 0 ].contains( "," ) ) {
						parts[ 0 ]	= NINE_REPLACEMENT_PATTERN.matcher( parts[ 0 ] ).replaceAll( "_" );
						format		= String.join( ".", parts );
					}
				}

				for ( Map.Entry<String, String> entry : formatReplacements.entrySet() ) {
					format = format.replace( entry.getKey(), entry.getValue() );
				}

				// Collapse patterns like "000,000,000" (all-zero groups) into "#,##0" to suppress leading zeros.
				// This handles incorrect masks like "999,999,999" where 9 means optional digit, not forced zero.
				if ( ALL_ZEROS_GROUPED_PATTERN.matcher( format ).matches() ) {
					format = "#,##0";
				} else if ( ALL_ZEROS_GROUPED_WITH_DECIMAL_PATTERN.matcher( format ).matches() ) {
					format = "#,##0" + format.substring( format.indexOf( '.' ) );
				}

				if ( format.substring( 0, 1 ).equals( "L" ) ) {
					format = format.substring( 1, format.length() );
				} else if ( format.substring( 0, 1 ).equals( "C" ) ) {
					format = format.substring( 1, format.length() ).replace( "0", "#" );
				}
				try {
					formatter = LocalizationUtil.localizedDecimalFormatter( locale, format );
				} catch ( IllegalArgumentException e ) {
					throw new RuntimeException( "Invalid number format pattern mask: " + arguments.getAsString( Key.mask ), e );
				}
			}
		}

		return formatter.format( value );
	}

}
