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
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
public class PropertyFile extends BIF {

	/**
	 * Constructor
	 */
	public PropertyFile() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.path )
		};
	}

	/**
	 * Get a fluent PropertyFile object that can be used to read or write properties to a loaded property file or a new one.
	 * If the path is not specified, it will return a blank property file object you can use to write properties to a new file.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The path to the property file to load or the path where the property file will be stored at
	 *
	 * @return A fluent PropertyFile object that can be used to read or write properties.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Note: I can't trust the Path object as it won't treat a drive root as a folder, and returns
		// null for some parents that are valid. So we're just doing string parsing for now.
		String									path			= arguments.getAsString( Key.path );
		ortus.boxlang.runtime.util.PropertyFile	propertyFile	= new ortus.boxlang.runtime.util.PropertyFile();

		// If the path is null or empty, we return a blank PropertyFile object
		if ( path == null || path.isEmpty() ) {
			return propertyFile;
		}

		// Let's see if we have a valid path or a potentially valid path
		path = FileSystemUtil.expandPath( context, path ).absolutePath().toString();
		// If valid, we load the property file, otherwise we set the path
		if ( FileSystemUtil.isValidFilePath( path ) ) {
			return propertyFile.load( path );
		}
		return propertyFile.setPath( path );
	}

}
