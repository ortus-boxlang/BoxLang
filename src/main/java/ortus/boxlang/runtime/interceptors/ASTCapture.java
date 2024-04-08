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
package ortus.boxlang.runtime.interceptors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.types.IStruct;

/**
 * An interceptor that captures the AST and outputs it to the console or a file
 */
public class ASTCapture extends BaseInterceptor {

	private boolean	toConsole	= false;
	private boolean	toFile		= false;
	// Default to current working directory
	private Path	filePath	= Paths.get( "./grapher/data/" );

	/**
	 * Constructor
	 *
	 * @param toConsole Whether to output to the console
	 * @param toFile    Whether to output to a file
	 */
	public ASTCapture( boolean toConsole, boolean toFile ) {
		this.toConsole	= toConsole;
		this.toFile		= toFile;
	}

	/**
	 * Listen to the "onParse" event
	 */
	@InterceptionPoint
	public void onParse( IStruct data ) {
		ParsingResult result = ( ParsingResult ) data.get( "result" );
		if ( result.getRoot() != null && ( this.toConsole || this.toFile ) ) {
			String JSON = result.getRoot().toJSON();

			if ( this.toConsole ) {
				System.out.println( "==================== AST ====================" );
				System.out.println( JSON );
				System.out.println( "=============================================" );
			}

			if ( this.toFile ) {
				Path file = Paths.get( filePath.toString(), "lastAST.json" );

				// Ensure path exists
				try {
					Files.createDirectories( file.getParent() );
				} catch ( IOException e ) {
					e.printStackTrace();
				}

				try {
					Files.writeString( file, JSON, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
		}
	}
}
