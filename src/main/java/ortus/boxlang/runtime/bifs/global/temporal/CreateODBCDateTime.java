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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxBIF( alias = "CreateODBCDate" )
@BoxBIF( alias = "CreateODBCTime" )
@BoxMember( type = BoxLangType.DATETIME, name = "toODBCDateTime" )
@BoxMember( type = BoxLangType.DATETIME, name = "toODBCDate" )
@BoxMember( type = BoxLangType.DATETIME, name = "toODBCTime" )
public class CreateODBCDateTime extends BIF {

	private static final IStruct formatters = Struct.of(
	    "CreateODBCDateTime", DateTime.ODBC_DATE_TIME_FORMAT_MASK,
	    "CreateODBCDate", DateTime.ODBC_DATE_FORMAT_MASK,
	    "CreateODBCTime", DateTime.ODBC_TIME_FORMAT_MASK,
	    "toODBCDateTime", DateTime.ODBC_DATE_TIME_FORMAT_MASK,
	    "toODBCDate", DateTime.ODBC_DATE_FORMAT_MASK,
	    "toODBCTime", DateTime.ODBC_TIME_FORMAT_MASK
	);

	/**
	 * Constructor
	 */
	public CreateODBCDateTime() {
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Creates a DateTime object with the format set to ODBC Implicit format
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date The date string or object
	 *
	 * @argument.timezone An optional timezone to apply
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		DateTime dateRef = DateTimeCaster.cast(
		    arguments.get( Key.date ),
		    true,
		    LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context )
		);
		return new DateTime( dateRef.getWrapped() ).setFormat( formatters.getAsString( arguments.getAsKey( BIF.__functionName ) ) );
	}

}
