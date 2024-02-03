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
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class SetTimezone extends BIF {

	/**
	 * Constructor
	 */
	public SetTimezone() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.timezone )
		};
	}

	/**
	 * Sets the timezone of the current request context
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.timezone The string representation or three character alias of the timezone
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	timezone	= arguments.getAsString( Key.timezone );
		ZoneId	assigned	= LocalizationUtil.parseZoneId( timezone );
		if ( assigned == null ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The timezone requested, [%s], is not a valid timezone identifier.",
			        timezone
			    )
			);
		}
		context.getParentOfType( RequestBoxContext.class ).setTimezone( assigned );
		return null;
	}

}
