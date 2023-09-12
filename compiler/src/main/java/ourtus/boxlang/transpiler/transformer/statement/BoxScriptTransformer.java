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
package ourtus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.apache.commons.text.StringSubstitutor;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.BoxScript;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxScriptTransformer extends AbstractTransformer {

	private final String template = """
	                                	// Auto package creation according to file path on disk
	                                	package ${packageName};

	                                	import ortus.boxlang.runtime.BoxRuntime;
	                                	import ortus.boxlang.runtime.context.IBoxContext;

	                                	// BoxLang Auto Imports
	                                	import ortus.boxlang.runtime.dynamic.BaseTemplate;
	                                	import ortus.boxlang.runtime.dynamic.Referencer;
	                                	import ortus.boxlang.runtime.interop.DynamicObject;
	                                	import ortus.boxlang.runtime.loader.ClassLocator;
	                                	import ortus.boxlang.runtime.operators.*;
	                                	import ortus.boxlang.runtime.scopes.Key;
	                                	import ortus.boxlang.runtime.scopes.IScope;

	                                	// Classes Auto-Imported on all Templates and Classes by BoxLang
	                                	import java.time.LocalDateTime;
	                                	import java.time.Instant;
	                                	import java.lang.System;
	                                	import java.lang.String;
	                                	import java.lang.Character;
	                                	import java.lang.Boolean;
	                                	import java.lang.Double;
	                                	import java.lang.Integer;

	                                	public class ${className} extends BaseTemplate {

	                                		// Auto-Generated Singleton Helpers
	                                		private static ${className} instance;

	                                		public $className() {
	                                			this.name         = "${fileName}";
	                                			this.extension    = "${fileExtension}";
	                                			this.path         = "{$fileFolderPath}";
	                                			// this.lastModified = "$lastModifiedTimestamp";
	                                			// this.compiledOn   = "$compiledOnTimestamp";
	                                			// this.ast = ???
	                                		}

	                                		public static synchronized $className getInstance() {
	                                			if ( instance == null ) {
	                                				instance = new $className();
	                                			}
	                                			return instance;
	                                		}

	                                		/**
	                                		 * Each template must implement the invoke() method which executes the template
	                                		 *
	                                		 * @param context The execution context requesting the execution
	                                		 */
	                                		public void invoke( IBoxContext context ) throws Throwable {
	                                			// Reference to the variables scope
	                                			IScope variablesScope = context.getScopeNearby( Key.of( "variables" ) );

	                                			ClassLocator JavaLoader = ClassLocator.getInstance();
	                                		}

	                                		public static void main( String[] args ) {
	                                			// This is the main method, it will be invoked when the template is executed
	                                			// You can use this
	                                			// Get a runtime going
	                                			BoxRuntime.startup( true );

	                                			try {
	                                				BoxRuntime.executeTemplate( $className.getInstance() );
	                                			} catch ( Throwable e ) {
	                                				e.printStackTrace();
	                                				System.exit( 1 );
	                                			}

	                                			// Bye bye! Ciao Bella!
	                                			BoxRuntime.shutdown();
	                                		}
	                                	}
	                                """;

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxScript						script		= ( BoxScript ) node;
		String							packageName	= "com.ortus"; // TODO
		String							className	= "TestClass"; // TODO

		Map<String, String>				values		= new HashMap<>() {

														{
															put( "packageName", packageName );
															put( "className", className );
														}
													};

		StringSubstitutor				sub			= new StringSubstitutor( values );
		String							code		= sub.replace( template );
		ParseResult<CompilationUnit>	result		= javaParser.parse( code );
		if ( !result.isSuccessful() ) {
			throw new IllegalStateException( result.toString() );
		}

		return result.getResult().get();
	}
}
