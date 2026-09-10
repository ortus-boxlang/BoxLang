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

package ortus.boxlang.runtime.bifs.global.io;

import java.time.Instant;
import java.time.ZoneId;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxFile;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF( description = "Set the last modified timestamp of a file" )
@BoxMember( type = BoxLangType.FILE )

public class FileSetLastModified extends BIF {

	/**
	 * Constructor
	 */
	public FileSetLastModified() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "boxfile", Key.file ),
		    new Argument( true, "any", Key.date )
		};
	}

	/**
	 * Sets the last modified time of a file
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file A file path or object
	 *
	 * @argument.date A date time object or string
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxFile		file	= arguments.getAsBoxFile( Key.file );
		Object		oDate	= arguments.get( Key.date );
		DateTime	date;
		if ( oDate instanceof Number nDate ) {
			date = new DateTime( Instant.ofEpochMilli( nDate.longValue() ).atZone( ZoneId.systemDefault() ) );
		} else {
			date = DateTimeCaster.cast( oDate, true, ZoneId.systemDefault(), false, context, null );
		}
		file.setLastModifiedTime( date );
		if ( file.implicitlyCast ) {
			return null;
		} else {
			return file;
		}
	}

}
