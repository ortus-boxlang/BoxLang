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
package ortus.boxlang.runtime.bifs.global.cli;

import java.io.IOException;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Clear the CLI console screen" )
public class CLIClear extends BIF {

	/**
	 * Constructor
	 */
	public CLIClear() {
		super();
	}

	/**
	 * Clears the CLI screen.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( System.getProperty( "os.name" ).contains( "Windows" ) ) {
			try {
				new ProcessBuilder( "cmd", "/c", "cls" ).inheritIO().start().waitFor();
			} catch ( InterruptedException | IOException e ) {
				throw new BoxRuntimeException( "Error clearing screen", e );
			}
		} else {
			System.out.print( "\033[H\033[2J" );
			System.out.flush();
		}

		return null;
	}

}
