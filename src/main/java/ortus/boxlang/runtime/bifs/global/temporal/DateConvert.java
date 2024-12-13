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
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
public class DateConvert extends BIF {

	private static final Key	utc2Local	= Key.of( "utc2Local" );
	private static final ZoneId	utcZone		= ZoneId.of( "UTC" );

	/**
	 * Constructor
	 */
	public DateConvert() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.conversionType ),
		    new Argument( true, "any", Key.date )
		};
	}

	/**
	 * Converts local time to Coordinated Universal Time (UTC), or UTC to local time.
	 * The function uses the daylight savings settings in the executing computer to compute daylight savings time, if required.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.conversionType The conversion type. Valid values are "utc2Local" and "local2Utc".
	 * 
	 * @argument.date The date to convert.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key			conversion	= Key.of( arguments.getAsString( Key.conversionType ) );
		ZoneId		localZone	= LocalizationUtil.parseZoneId( null, context );
		ZoneId		refZone		= conversion.equals( utc2Local ) ? utcZone : localZone;
		Object		dateObject	= arguments.get( Key.date );
		DateTime	dateRef		= null;
		if ( dateObject instanceof String stringDate ) {
			dateRef = new DateTime( stringDate, refZone );
		} else {
			dateRef = DateTimeCaster.cast( dateObject, context );
		}

		return dateRef.convertToZone( conversion.equals( utc2Local ) ? localZone : utcZone );

	}

}
