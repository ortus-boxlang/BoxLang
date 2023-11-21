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
package ortus.boxlang.transpiler.transformer.statement;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Source;
import ortus.boxlang.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a Function Declaration in the equivalent Java Parser AST nodes
 */
public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	Logger			logger		= LoggerFactory.getLogger( BoxFunctionDeclarationTransformer.class );
	// @formatter:off
	private String template = """
		package ${packageName};

		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.context.IBoxContext;
		import ortus.boxlang.runtime.context.ScriptingBoxContext;
		import ortus.boxlang.runtime.scopes.ArgumentsScope;
		import ortus.boxlang.runtime.scopes.IScope;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.types.Function.Argument;
		import ortus.boxlang.runtime.types.UDF;
		import ortus.boxlang.runtime.runnables.IBoxRunnable;
		import ortus.boxlang.runtime.operators.*;
		import ortus.boxlang.runtime.dynamic.casters.*;

		// Classes Auto-Imported on all Templates and Classes by BoxLang
		import java.time.LocalDateTime;
		import java.time.Instant;
		import java.lang.System;
		import java.lang.String;
		import java.lang.Character;
		import java.lang.Boolean;
		import java.lang.Double;
		import java.lang.Integer;
		import java.util.*;
		import java.nio.file.*;

		public class ${classname} extends UDF {
			private static ${classname}				instance;
			private final static Key				name		= Key.of( "${functionName}" );
			private final static Argument[]			arguments	= new Argument[] {
				${arguments}
			};
			private final static String				returnType	= "string";
			private              Access		    access		= Access.PUBLIC;

			public Key getName() {
				return name;
			}
			public Argument[] getArguments() {
				return arguments;
			}
			public String getReturnType() {
				return returnType;
			}

			public Access getAccess() {
   				return access;
   			}

			public  boolean isOutput() {
				return false;
			}

			public String getHint() {
				return "";
			}

   			public  Map<Key, Object> getAdditionalMetadata() {
   				return null;
   			}

			public  long getRunnableCompileVersion() {
				return 0L;
			}

			public LocalDateTime getRunnableCompiledOn() {
				return null;
			}

			public IBoxRunnable getDeclaringRunnable() {
				return null;
			}

			public Object getRunnableAST() {
				return null;
			}

			private ${classname}() {
				super();
			}

			public static synchronized ${classname} getInstance() {
				if ( instance == null ) {
					instance = new ${classname}();
				}
				return instance;
			}

			@Override
			public Object _invoke( FunctionBoxContext context ) {
				IScope variablesScope = ${contextName}.getScopeNearby( Key.of( "variables" ));

			}
		}
 	""";
	public BoxFunctionDeclarationTransformer(JavaTranspiler transpiler) {
    	super(transpiler);
    }
	// @formatter:on
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxFunctionDeclaration	function	= ( BoxFunctionDeclaration ) node;
		Source					source		= function.getPosition().getSource();
		String					packageName	= JavaTranspiler.getPackageName( source );
		String					className	= JavaTranspiler.getClassName( source ) + "$" + function.getName();
		String					arguments	= "";

		if ( context == TransformerContext.REGISTER ) {
			Map<String, String> values = Map.ofEntries(
			    Map.entry( "className", className )
			);
			template = "${contextName}.registerUDF( ${className}.getInstance() );";
			Node javaStmt = parseStatement( template, values );
			logger.info( node.getSourceText() + " -> " + javaStmt );
			addIndex( javaStmt, node );
			return javaStmt;

		} else {

			Map<String, String>				values	= Map.ofEntries(
			    Map.entry( "packageName", packageName ),
			    Map.entry( "className", className ),
			    Map.entry( "functionName", function.getName() ),
			    Map.entry( "arguments", arguments )
			);
			String							code	= PlaceholderHelper.resolve( template, values );
			ParseResult<CompilationUnit>	result;
			try {
				result = javaParser.parse( code );
			} catch ( Exception e ) {
				// Temp debugging to see generated Java code
				throw new ApplicationException( code, e );
			}
			if ( !result.isSuccessful() ) {
				// Temp debugging to see generated Java code
				throw new ApplicationException( result + "\n" + code );
			}
			CompilationUnit		javaClass		= result.getResult().get();
			MethodDeclaration	invokeMethod	= javaClass.findCompilationUnit().orElseThrow()
			    .getClassByName( className ).orElseThrow()
			    .getMethodsByName( "_invoke" ).get( 0 );

			for ( BoxStatement statement : function.getBody() ) {
				Node javaStmt = transpiler.transform( statement );
				if ( javaStmt instanceof BlockStmt stmt ) {
					stmt.getStatements().forEach( it -> invokeMethod.getBody().get().addStatement( it ) );
				} else {
					invokeMethod.getBody().get().addStatement( ( Statement ) javaStmt );
				}
			}
			return javaClass;
		}
	}
}
