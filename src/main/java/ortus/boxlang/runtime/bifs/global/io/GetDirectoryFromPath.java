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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF

public class GetDirectoryFromPath extends BIF {

	/**
	 * Constructor
	 */
	public GetDirectoryFromPath() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		};
	}

	/**
	 * Retrieves the directory parent of a path
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The path to extract the parent directory from
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Note: I can't trust the Path object as it won't treat a drive root as a folder, and returns
		// null for some parents that are valid. So we're just doing string parsing for now.
		String path = arguments.getAsString( Key.path );
		if ( path.endsWith( "/" ) || path.endsWith( "\\" ) ) {
			return path;
		}

		int lastSeparator = path.lastIndexOf( "/" );
		// find last index of
		if ( lastSeparator == -1 ) {
			lastSeparator = path.lastIndexOf( "\\" );
		}
		// if path doesn't contain \ or /, return /
		if ( lastSeparator == -1 ) {
			return "/";
		}
		// strip last segment after last \ or / and append a final File.separator
		return path.substring( 0, lastSeparator + 1 );
	}

}
