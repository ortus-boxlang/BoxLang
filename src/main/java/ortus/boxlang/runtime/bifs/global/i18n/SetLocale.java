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
package ortus.boxlang.runtime.bifs.global.i18n;

import java.util.Locale;

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

public class SetLocale extends BIF {

	/**
	 * Constructor
	 */
	public SetLocale() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.locale )
		};
	}

	/**
	 * Sets the current request-level locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.locale The locale ISO directive, common name or alias
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Locale locale = LocalizationUtil.parseLocale( arguments.getAsString( Key.locale ) );
		if ( locale == null || !LocalizationUtil.isValidLocale( locale ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The Locale requested, [%s], is not a valid Locale for this runtime.",
			        locale
			    )
			);
		}
		context.getParentOfType( RequestBoxContext.class ).setLocale( locale );
		return null;
	}

}
