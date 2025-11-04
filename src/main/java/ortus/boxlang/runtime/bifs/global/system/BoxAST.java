
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
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Parses BoxLang source code or a file and generates the Abstract Syntax Tree (AST) representation in various formats" )
@BoxMember( type = BoxLangType.STRING, name = "toAST" )
public class BoxAST extends BIF {

	/**
	 * Constructor
	 */
	public BoxAST() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.source ),
		    new Argument( false, Argument.STRING, Key.filepath ),
		    new Argument( false, Argument.STRING, Key.returnType, "struct", Set.of( Validator.valueOneOf( "struct", "json", "text" ) ) ),
		    new Argument( false, Argument.STRING, Key.sourceType, "script",
		        Set.of( Validator.valueOneOf( "script", "template", "cfscript", "cftemplate" ) ) )
		};
	}

	/**
	 * Generates the Abstract Syntax Tree (AST) for BoxLang source code or a file.
	 * The AST represents the syntactic structure of the code and can be used for
	 * code analysis, transformation, or generation.
	 *
	 * <p>
	 * <strong>Usage Examples:</strong>
	 * </p>
	 *
	 * <pre>
	 * // Parse source code and return as struct (default)
	 * ast = boxAST( source = "x = 1 + 2" );
	 * println( ast.ASTType ); // Outputs: BoxAssignment
	 *
	 * // Parse source code and return as JSON
	 * json = boxAST( source = "function add(a, b) { return a + b; }", returnType = "json" );
	 * println( json ); // Outputs: JSON representation of the AST
	 *
	 * // Parse source code and return as text
	 * text = boxAST( source = "if (x > 5) { println('yes'); }", returnType = "text" );
	 * println( text ); // Outputs: Human-readable text representation
	 *
	 * // Parse a file
	 * ast = boxAST( filepath = "src/MyClass.bx" );
	 *
	 * // Use as a member function on a string
	 * source = "a = [1, 2, 3]";
	 * ast = source.toAST(); // Returns AST as struct
	 * ast = source.toAST( returnType = "json" ); // Returns AST as JSON string
	 *
	 * // Parse CFML/ColdFusion syntax
	 * ast = boxAST( source = "cfset x = 1", sourceType = "cfscript" );
	 *
	 * // Parse template syntax
	 * ast = boxAST( source = "<bx:output>#now()#</bx:output>", sourceType = "template" );
	 * </pre>
	 *
	 * <p>
	 * The returned AST structure contains nodes with the following key properties:
	 * </p>
	 * <ul>
	 * <li><strong>ASTType</strong> - The type of AST node (e.g., BoxAssignment, BoxFunctionDeclaration, BoxClass)</li>
	 * <li><strong>ASTPackage</strong> - The package name of the AST node class</li>
	 * <li>Additional properties specific to each node type (e.g., name, value, children, etc.)</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The BoxLang source code to parse. Either source or filepath must be provided.
	 *                  When used as a member function, this is automatically set to the string value.
	 *
	 * @argument.filepath The path to a BoxLang file to parse. Either source or filepath must be provided.
	 *                    Can be relative (to the current working directory) or absolute.
	 *
	 * @argument.returnType The format of the returned AST. Valid values are "struct" (default), "json", or "text".
	 *                      <ul>
	 *                      <li><strong>struct</strong> - Returns a nested structure (Map) representing the AST hierarchy</li>
	 *                      <li><strong>json</strong> - Returns a JSON string representation of the AST</li>
	 *                      <li><strong>text</strong> - Returns a human-readable text representation of the AST</li>
	 *                      </ul>
	 *
	 * @argument.sourceType The type of source code being parsed. Valid values are "script" (default), "template", "cfscript", or "cftemplate".
	 *                      <ul>
	 *                      <li><strong>script</strong> - BoxLang script syntax (BOXSCRIPT)</li>
	 *                      <li><strong>template</strong> - BoxLang template syntax (BOXTEMPLATE)</li>
	 *                      <li><strong>cfscript</strong> - ColdFusion script syntax (CFSCRIPT)</li>
	 *                      <li><strong>cftemplate</strong> - ColdFusion template syntax (CFTEMPLATE)</li>
	 *                      </ul>
	 *
	 * @return The AST in the requested format: a struct (Map), JSON string, or text representation
	 *
	 * @throws BoxRuntimeException If neither source nor filepath is provided, or if both are empty
	 * @throws BoxIOException      If there is an error reading or parsing the file
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	source		= arguments.getAsString( Key.source );
		String	filePath	= arguments.getAsString( Key.filepath );
		String	returnType	= arguments.getAsString( Key.returnType );
		String	sourceType	= arguments.getAsString( Key.of( "sourceType" ) );

		// Validate that at least one argument is provided
		if ( ( source == null || source.trim().isEmpty() ) &&
		    ( filePath == null || filePath.trim().isEmpty() ) ) {
			throw new BoxRuntimeException(
			    "Either 'source' or 'filepath' argument must be provided to boxAST(). "
			        + "Please provide BoxLang source code via 'source' or a file path via 'filepath'."
			);
		}

		// Convert sourceType string to BoxSourceType enum
		BoxSourceType	boxSourceType	= switch ( sourceType.toLowerCase() ) {
											case "script" -> BoxSourceType.BOXSCRIPT;
											case "template" -> BoxSourceType.BOXTEMPLATE;
											case "cfscript" -> BoxSourceType.CFSCRIPT;
											case "cftemplate" -> BoxSourceType.CFTEMPLATE;
											default -> BoxSourceType.BOXSCRIPT; // Should never happen due to validator
										};

		BoxNode			root			= null;
		try {
			if ( source != null && !source.trim().isEmpty() ) {
				// Parse source string directly with specified source type
				root = new Parser().parse( source, boxSourceType ).getRoot();
			} else {
				// Parse file from filesystem
				root = new Parser().parse( Path.of( filePath ).toFile() ).getRoot();
			}
		} catch ( IOException e ) {
			throw new BoxIOException(
			    "Error parsing BoxLang code" + ( filePath != null ? " from file: " + filePath : "" ),
			    e
			);
		}

		// Return AST in requested format
		switch ( returnType ) {
			case "struct" :
				return root.toMap();
			case "json" :
				return root.toJSON();
			case "text" :
				return root.toString();
			default :
				// This should never happen due to validator, but included for completeness
				throw new BoxRuntimeException( "Invalid return type: " + returnType + ". Valid values are: struct, json, text" );
		}

	}
}
