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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.config.util.RuntimeSettingsUtil;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Get a user-defined runtime setting from boxlang.json or the Application descriptor" )
public class GetRuntimeSetting extends BIF {

	/**
	 * Constructor
	 */
	public GetRuntimeSetting() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "String", Key.key ),
		    new Argument( false, "Any", Key.defaultValue )
		};
	}

	/**
	 * Retrieve a user-defined runtime setting. Settings are defined in the {@code runtimeSettings} struct of
	 * {@code boxlang.json} and can be overridden (deep merged) by {@code this.runtimeSettings} in the Application descriptor.
	 * Values support {@code ${env.X}} placeholders and {@code bxsecret:} values. The returned values are read-only.
	 * <p>
	 * Use a dotted path to reach nested values, e.g. {@code features.beta}. Array elements can be reached with a 1-based index, e.g. {@code limits.1}.
	 * If no key is passed, the whole merged settings struct is returned.
	 * <p>
	 * If the key is not found and no default is provided, an exception is thrown.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.key The name or dotted path of the setting to retrieve. Omit to retrieve all settings.
	 *
	 * @argument.defaultValue The default value to return if the setting is not found
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	path		= arguments.getAsString( Key.key );
		IStruct	settings	= context.getConfigItem( Key.runtimeSettings ) instanceof IStruct s ? s : Struct.EMPTY;

		if ( path == null || path.isEmpty() ) {
			return settings;
		}

		Object value = RuntimeSettingsUtil.get( settings, path );
		if ( value != null ) {
			return value;
		}
		Object defaultValue = arguments.get( Key.defaultValue );
		if ( defaultValue == null ) {
			throw new BoxRuntimeException( "The runtime setting [" + path + "] was not found and no default value was provided." );
		}
		return defaultValue;
	}
}
