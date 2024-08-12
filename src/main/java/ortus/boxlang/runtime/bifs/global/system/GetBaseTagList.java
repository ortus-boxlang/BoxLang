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
package ortus.boxlang.runtime.bifs.global.system;

import java.util.Arrays;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class GetBaseTagList extends BIF {

	/**
	 * Constructor
	 */
	public GetBaseTagList() {
		super();
		declaredArguments = new Argument[] {
		};
	}

	/**
	 * A comma-delimited list of ancestor tag names as a string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return Arrays
		    .asList( context.getComponents() )
		    .stream()
		    .map( s -> {
			    if ( s.get( Key._NAME ).equals( Key.module ) ) {
				    String templatePath	= s.getAsString( Key.customTagPath );
				    // get first two chars of the file extension
				    String type			= templatePath.substring( templatePath.lastIndexOf( '.' ) + 1, templatePath.lastIndexOf( '.' ) + 3 );
				    return type + "_" + s.get( Key.customTagName ).toString();
			    } else {
				    return s.get( Key._NAME ).toString();
			    }
		    } )
		    .collect( Collectors.joining( "," ) );
	}
}
