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

package ortus.boxlang.runtime.bifs.global.temporal;

import java.math.RoundingMode;
import java.time.ZoneId;

import java.math.BigDecimal;
import java.time.Instant;
import ortus.boxlang.runtime.types.util.DateTimeHelper;
import java.time.LocalDateTime;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxMember( type = BoxLangType.DATETIME, name = "add", objectArgument = "date" )
public class DateAdd extends BIF {

	/**
	 * Constructor
	 */
	public DateAdd() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.datepart ),
		    new Argument( true, "number", Key.number ),
		    new Argument( true, "any", Key.date )
		};
	}

	/**
	 * Modifies a date object by date part and integer time unit
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.datepart The date part to modify
	 * 
	 * @argument.number The number of units to modify by
	 * 
	 * @argument.date The date object to modify
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ZoneId		timezone	= LocalizationUtil.parseZoneId( null, context );
		DateTime	ref			= DateTimeHelper.castIncludeFractionalDays( arguments.get( Key.date ), timezone, context );
		return ref.clone().modify(
		    arguments.getAsString( Key.datepart ),
		    BigDecimalCaster.cast( arguments.get( Key.number ) ).setScale( 0, RoundingMode.HALF_UP ).longValue()
		);
	}

}
