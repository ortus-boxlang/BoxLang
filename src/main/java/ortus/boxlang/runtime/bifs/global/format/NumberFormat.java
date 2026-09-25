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

import java.math.BigDecimal;
import java.text.DecimalFormat;
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
		Number	value	= NumberCaster.cast( oValue );
		String	format	= arguments.getAsString( Key.mask );
		// Mimic Lucee/ACF: nudge fractional values by a tiny epsilon so that exact half-value ties
		// round up for positive numbers and toward zero for negative numbers ( e.g. -5.5 -> -5 ).
		// Floating-decimal masks ( containing '^' ) preserve natural decimals and are not nudged.
		if ( format == null || format.indexOf( '^' ) == -1 ) {
			value = applyRoundingEpsilon( value );
		}
		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		Locale	locale			= LocalizationUtil.parseLocaleFromContext( context, arguments );
		// In CFML, numberFormat() is locale independent ( always US ), whereas lsNumberFormat() follows the request locale.
		// Force US only when numberFormat is invoked without an explicit locale argument.
		if ( bifMethodKey != null && bifMethodKey.getNameNoCase().equals( "numberformat" ) && arguments.getAsString( Key.locale ) == null ) {
			locale = Locale.US;
		}
		java.text.NumberFormat				formatter			= LocalizationUtil.localizedDecimalFormatter(
		    locale,
		    LocalizationUtil.NUMBER_FORMAT_PATTERNS.get( LocalizationUtil.DEFAULT_NUMBER_FORMAT_KEY )
		);
		final LinkedHashMap<String, String>	formatReplacements	= new LinkedHashMap<>() {

																	{
																		put( "9", "0" );
																		// Fix incorrect number positionals with dollar notation
																		put( "_$", "$_" );
																		// Ensure number optional always comes before the comma
																		put( "$,", "$_," );
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
				// Floating-decimal masks ( containing '^' ) use natural decimals and are padded/justified.
				if ( format.indexOf( '^' ) >= 0 ) {
					return formatJustified( locale, value, format );
				}

				// Capture and strip a leading justification flag ( L = left, C = center, R = right ).
				char	justification	= 'R';
				String	originalMask	= format;
				if ( !format.isEmpty() && ( format.charAt( 0 ) == 'L' || format.charAt( 0 ) == 'C' || format.charAt( 0 ) == 'R' ) ) {
					justification	= format.charAt( 0 );
					format			= format.substring( 1 );
					originalMask	= format;
				}

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

				// Convert fractional underscore placeholders to required zeroes so trailing decimal places are preserved.
				if ( format.contains( "." ) ) {
					int decimalIndex = format.indexOf( '.' );
					format = format.substring( 0, decimalIndex ) + "." + format.substring( decimalIndex + 1 ).replace( "_", "0" );
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

				// Fraction-only masks ( e.g. ".00", ".__" ) must retain their leading zero.
				if ( format.startsWith( "." ) ) {
					format = "0" + format;
				}

				try {
					formatter = LocalizationUtil.localizedDecimalFormatter( locale, format );
				} catch ( IllegalArgumentException e ) {
					throw new RuntimeException( "Invalid number format pattern mask: " + arguments.getAsString( Key.mask ), e );
				}

				// Preserve the whitespace the mask reserves: pad the result to the mask's digit width
				// and honor the requested justification ( L = left, C = center, R / default = right ).
				// The negative sign is placed at the very start, before any padding.
				String	result		= formatter.format( value );
				boolean	negative	= result.startsWith( "-" );
				if ( negative ) {
					result = result.substring( 1 );
				}
				int padding = computeMaskWidth( originalMask ) - countDigitChars( result );
				if ( padding > 0 ) {
					result = applyJustification( result, padding, justification );
				}
				return negative ? "-" + result : result;
			}
		}

		return formatter.format( value );
	}

	/**
	 * Mimics the rounding factor that Adobe CF and Lucee apply before formatting: a tiny positive
	 * epsilon is added to fractional values so that exact half-value ties round up for positive
	 * numbers and toward zero for negative numbers ( e.g. -5.5 -> -5 instead of -6 ).
	 *
	 * @param value The value to nudge
	 *
	 * @return The nudged value, or the original value for integers and non-decimal types
	 */
	private static Number applyRoundingEpsilon( Number value ) {
		if ( value instanceof BigDecimal bd ) {
			// No fractional part to round; leave integers untouched.
			if ( bd.stripTrailingZeros().scale() <= 0 ) {
				return value;
			}
			return bd.add( new BigDecimal( "1E-12" ) );
		}
		if ( value instanceof Double || value instanceof Float ) {
			double doubleValue = value.doubleValue();
			if ( doubleValue == Math.rint( doubleValue ) ) {
				return value;
			}
			return doubleValue + 1E-12;
		}
		return value;
	}

	/**
	 * Counts the digit width a mask reserves: digit placeholders ( '_', '9', '0' ), the decimal
	 * point, and grouping commas. Symbol characters ( '$', '+', '-', '(', ')', 'L', 'C', 'R', '^' )
	 * do not contribute to the width.
	 *
	 * @param mask The format mask
	 *
	 * @return The number of digit positions the mask reserves
	 */
	private static int computeMaskWidth( String mask ) {
		int width = 0;
		for ( int i = 0; i < mask.length(); i++ ) {
			char c = mask.charAt( i );
			if ( c == '_' || c == '9' || c == '0' || c == '.' ) {
				width++;
			}
		}
		return width;
	}

	/**
	 * Counts the digit characters ( digits, decimal point, grouping separators ) in a formatted
	 * result, ignoring any symbol characters ( '$', '+', '-', '(', ')', spaces ).
	 *
	 * @param result The formatted result string
	 *
	 * @return The number of digit characters
	 */
	private static int countDigitChars( String result ) {
		int count = 0;
		for ( int i = 0; i < result.length(); i++ ) {
			char c = result.charAt( i );
			if ( ( c >= '0' && c <= '9' ) || c == '.' || c == ',' ) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Applies the requested justification to pad the result to the mask width.
	 *
	 * @param result        The formatted result
	 * @param padding       The number of spaces to add
	 * @param justification The justification flag ( 'L' = left, 'C' = center, otherwise right )
	 *
	 * @return The justified result
	 */
	private static String applyJustification( String result, int padding, char justification ) {
		if ( justification == 'L' ) {
			return result + " ".repeat( padding );
		} else if ( justification == 'C' ) {
			int leftPad = padding / 2;
			return " ".repeat( leftPad ) + result + " ".repeat( padding - leftPad );
		}
		return " ".repeat( padding ) + result;
	}

	/**
	 * Formats a number using a justification mask. Symbol characters ( '(', ')', '$', '+', '-' ) are
	 * placed at far or near positions relative to the number, and the result is padded to the width
	 * of the mask, then justified ( right by default, or per the L/C/R flag ).
	 *
	 * @param locale The locale to use
	 * @param value  The value to format
	 * @param mask   The original format mask
	 *
	 * @return The padded, justified result
	 */
	private static String formatJustified( Locale locale, Number value, String mask ) {
		char justification = 'R';
		if ( mask.charAt( 0 ) == 'L' || mask.charAt( 0 ) == 'C' || mask.charAt( 0 ) == 'R' ) {
			justification	= mask.charAt( 0 );
			mask			= mask.substring( 1 );
		}

		// Locate the decimal separator: '^' is floating ( natural decimals ), '.' is fixed.
		boolean	floating		= false;
		int		decimalIndex	= mask.indexOf( '^' );
		if ( decimalIndex >= 0 ) {
			floating = true;
		} else {
			decimalIndex = mask.indexOf( '.' );
		}
		String			leftMask		= decimalIndex >= 0 ? mask.substring( 0, decimalIndex ) : mask;
		String			rightMask		= decimalIndex >= 0 ? mask.substring( decimalIndex + 1 ) : "";

		// Parse digit placeholders and far/near symbols from each side.
		StringBuilder	integerPattern	= new StringBuilder();
		StringBuilder	fractionPattern	= new StringBuilder();
		StringBuilder	leftFar			= new StringBuilder();
		StringBuilder	leftNear		= new StringBuilder();
		StringBuilder	rightNear		= new StringBuilder();
		StringBuilder	rightFar		= new StringBuilder();

		boolean			seenLeftDigit	= false;
		for ( char c : leftMask.toCharArray() ) {
			if ( c == '_' || c == '9' ) {
				integerPattern.append( '#' );
				seenLeftDigit = true;
			} else if ( c == '0' ) {
				integerPattern.append( '0' );
				seenLeftDigit = true;
			} else if ( c == ',' ) {
				integerPattern.append( ',' );
			} else if ( isSymbol( c ) ) {
				if ( seenLeftDigit ) {
					leftNear.append( c );
				} else {
					leftFar.append( c );
				}
			}
		}

		boolean seenRightDigit = false;
		for ( char c : rightMask.toCharArray() ) {
			if ( c == '_' || c == '9' ) {
				fractionPattern.append( floating ? '#' : '0' );
				seenRightDigit = true;
			} else if ( c == '0' ) {
				fractionPattern.append( '0' );
				seenRightDigit = true;
			} else if ( isSymbol( c ) ) {
				if ( seenRightDigit ) {
					rightFar.append( c );
				} else {
					rightNear.append( c );
				}
			}
		}

		// Build the DecimalFormat pattern and render the number.
		String			pattern		= integerPattern.toString() + ( decimalIndex >= 0 ? "." + fractionPattern : "" );
		DecimalFormat	formatter	= LocalizationUtil.localizedDecimalFormatter( locale, pattern );
		if ( floating ) {
			int scale = naturalScale( value );
			if ( scale > 0 ) {
				formatter.setMaximumFractionDigits( scale );
			}
		}
		String	numberStr		= formatter.format( value );

		// Render the far/near symbols ( '+'/'-' depend on the sign of the value ).
		boolean	negative		= value.doubleValue() < 0;
		String	leftFarStr		= renderSymbols( leftFar.toString(), negative );
		String	leftNearStr		= renderSymbols( leftNear.toString(), negative );
		String	rightNearStr	= renderSymbols( rightNear.toString(), negative );
		String	rightFarStr		= renderSymbols( rightFar.toString(), negative );
		boolean	hasSymbols		= !leftFarStr.isEmpty() || !leftNearStr.isEmpty() || !rightNearStr.isEmpty() || !rightFarStr.isEmpty() || floating;

		// Compute padding relative to the width of the mask ( every character occupies one column ).
		int		width			= mask.length();
		int		padding			= width - numberStr.length() - leftFarStr.length() - leftNearStr.length() - rightNearStr.length() - rightFarStr.length();
		if ( padding < 0 ) {
			padding = 0;
		}

		int	leftPad		= 0;
		int	rightPad	= 0;
		if ( justification == 'L' ) {
			rightPad = padding;
		} else if ( justification == 'C' ) {
			leftPad		= padding / 2;
			rightPad	= padding - leftPad;
			// Adobe CF centers with a minimum of one space on each side when symbols are present.
			if ( hasSymbols ) {
				leftPad		= Math.max( leftPad, 1 );
				rightPad	= Math.max( rightPad, 1 );
			}
		} else {
			leftPad = padding;
		}

		StringBuilder result = new StringBuilder();
		result.append( leftFarStr ).append( " ".repeat( leftPad ) ).append( leftNearStr );
		result.append( numberStr );
		result.append( rightNearStr ).append( " ".repeat( rightPad ) ).append( rightFarStr );
		return result.toString();
	}

	/**
	 * Determines whether a character is a mask symbol that occupies a fixed column.
	 *
	 * @param c The character
	 *
	 * @return true when the character is a symbol
	 */
	private static boolean isSymbol( char c ) {
		return c == '(' || c == ')' || c == '$' || c == '+' || c == '-';
	}

	/**
	 * Renders mask symbols, resolving the sign for '+' and '-'.
	 *
	 * @param symbols  The symbol characters
	 * @param negative Whether the value is negative
	 *
	 * @return The rendered symbol string
	 */
	private static String renderSymbols( String symbols, boolean negative ) {
		StringBuilder sb = new StringBuilder();
		for ( char c : symbols.toCharArray() ) {
			if ( c == '+' ) {
				sb.append( negative ? '-' : '+' );
			} else if ( c == '-' ) {
				sb.append( negative ? '-' : ' ' );
			} else {
				sb.append( c );
			}
		}
		return sb.toString();
	}

	/**
	 * Determines the number of decimal places a value carries naturally.
	 *
	 * @param value The value
	 *
	 * @return The natural scale ( zero for integers )
	 */
	private static int naturalScale( Number value ) {
		if ( value instanceof BigDecimal bd ) {
			return Math.max( bd.stripTrailingZeros().scale(), 0 );
		}
		return Math.max( BigDecimal.valueOf( value.doubleValue() ).stripTrailingZeros().scale(), 0 );
	}

}
