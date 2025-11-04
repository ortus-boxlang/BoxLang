
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Generates the AST for BoxLang code." )
public class BoxAST extends BIF {

	/**
	 * Constructor
	 */
	public BoxAST() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.source ),
		    new Argument( false, Argument.STRING, Key.filepath ),
		    new Argument( false, Argument.STRING, Key.returnType, "struct", Set.of( Validator.valueOneOf( "struct", "json", "text" ) ) )
		};
	}

	/**
	 * This generates the AST for a particular source passed or a file path.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	source		= arguments.getAsString( Key.source );
		String	filePath	= arguments.getAsString( Key.filepath );
		String	returnType	= arguments.getAsString( Key.returnType );

		// Validate that at least one argument is provided
		if ( ( source == null || source.trim().isEmpty() ) &&
		    ( filePath == null || filePath.trim().isEmpty() ) ) {
			throw new BoxRuntimeException( "Either 'source' or 'filepath' argument must be provided" );
		}

		BoxNode root = null;
		try {
			if ( source != null && !source.trim().isEmpty() ) {
				root = new Parser().parseStatement( source ).getRoot();
			} else {
				root = new Parser().parse( Path.of( filePath ).toFile() ).getRoot();
			}
		} catch ( IOException e ) {
			throw new BoxIOException( "Error parsing source code", e );
		}

		switch ( returnType ) {
			case "struct" :
				return root.toMap();
			case "json" :
				return root.toJSON();
			case "text" :
				return root.toString();
			default :
				throw new BoxRuntimeException( "Invalid return type: " + returnType );
		}

	}
}
