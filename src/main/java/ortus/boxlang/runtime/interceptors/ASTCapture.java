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

import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.runtime.types.IStruct;

/**
 * An interceptor state is an event state that is used to hold observers that want to listent
 * to that specific state. For example, the "preProcess" state is used to hold observers that
 * listen to "preProcess" events.
 *
 * The {@see InterceptorService} is in charge of managing all states and event registrations in BoxLang.
 */
public class ASTCapture {

	private boolean	toConsole	= false;
	private boolean	toFile		= false;
	// Default to current working directory
	private Path	filePath	= Paths.get( "./grapher/data/" );

	public ASTCapture( boolean toConsole, boolean toFile ) {
		this.toConsole	= toConsole;
		this.toFile		= toFile;
	}

	public void onParse( IStruct data ) {
		ParsingResult result = ( ParsingResult ) data.get( "result" );
		if ( result.getRoot() != null && ( toConsole || toFile ) ) {
			String JSON = result.getRoot().toJSON().toString();
			if ( toConsole ) {
				System.out.println( JSON );
			}
			if ( toFile ) {
				Path file = Paths.get( filePath.toString(), "lastAST.json" );
				try {
					Files.writeString( file, JSON, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
		}
	}
}
