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

import java.io.File;
import java.nio.file.Path;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class ExpandPath extends BIF {

	/**
	 * Constructor
	 */
	public ExpandPath() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path Relative or absolute directory reference or filename, within the current directory, (.\ and ..\) to convert to an absolute path. Can
	 *                include forward or backward slashes.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String path = arguments.getAsString( Key.path );
		if ( new File( path ).isAbsolute() ) {
			return path;
		}

		// Determine what this path is relative to
		Path template = context.findClosestTemplate();
		// We our current context is executing a template, then we are relative to that template
		if ( template != null && new File( template.getParent().toString(), path ).exists() ) {
			return new File( template.getParent().toString(), path ).getAbsolutePath();
		}

		if ( !path.startsWith( "/" ) ) {
			path = "/" + path;
		}
		final String	finalPath		= path;
		String			matchingMapping	= context.getConfig().getAsStruct( Key.runtime ).getAsStruct( Key.mappings )
		    .entrySet()
		    .stream()
		    .sorted(
		        ( e1, e2 ) -> Integer.compare( e2.getValue().toString().length(), e1.getValue().toString().length() ) )
		    .filter( e -> finalPath.startsWith( e.getKey().getName() ) )
		    .findFirst()
		    .get()
		    .getValue()
		    .toString();

		return new File( matchingMapping, path ).getAbsolutePath();
	}

}
