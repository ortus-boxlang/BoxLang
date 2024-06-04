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

import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )

public class NumberFormat extends BIF {

	/**
	 * Constructor
	 */
	public NumberFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "string", Key.mask )
		};
	}

	/**
	 * Formats a number with an optional format mask
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to be formatted
	 *
	 * @argument.mask The formatting mask to apply
	 *
	 * @argument.locale Note used by standard NumberFormat but used by LSNumberFormat
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		double					value		= DoubleCaster.cast( arguments.get( Key.number ), true );
		String					format		= arguments.getAsString( Key.mask );
		Locale					locale		= LocalizationUtil.parseLocaleFromContext( context, arguments );
		java.text.NumberFormat	formatter	= LocalizationUtil.localizedDecimalFormatter(
		    locale,
		    LocalizationUtil.NUMBER_FORMAT_PATTERNS.get( LocalizationUtil.DEFAULT_NUMBER_FORMAT_KEY )
		);

		// Currency-specific arguments
		String					type		= arguments.getAsString( Key.type );
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
				format = format.replaceAll( "9", "0" )
				    .replaceAll( "_", "#" );
				if ( format.substring( 0, 1 ).equals( "L" ) ) {
					format = format.substring( 1, format.length() );
				} else if ( format.substring( 0, 1 ).equals( "C" ) ) {
					format = format.substring( 1, format.length() ).replace( "0", "#" );
				}
				formatter = LocalizationUtil.localizedDecimalFormatter( locale, format );
			}
		}

		return formatter.format( value );
	}

}
