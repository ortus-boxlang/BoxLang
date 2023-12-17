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

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a Function Declaration in the equivalent Java Parser AST nodes
 */
public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	// @formatter:off
	private String registrationTemplate = "${contextName}.registerUDF( ${enclosingClassName}.${className}.getInstance() );";
	private String classTemplate = """
		package ${packageName};

		public class ${classname} extends UDF {
			private static ${classname}				instance;
			private final static Key				name		= Key.of( "${functionName}" );
			private final static Argument[]			arguments	= new Argument[] {};
			private final static String				returnType	= "${returnType}";
			private              Access		    access		= Access.${access};

			private final static Struct	annotations;

			private final static Struct	documentation;

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
			public Struct getAnnotations() {
				return annotations;
			}

			@Override
			public Struct getDocumentation() {
				return documentation;
			}
			
			public List<ImportDefinition> getImports() {
				return null;
			}

			@Override
			public Object _invoke( FunctionBoxContext context ) {

			}
		}
 	""";
	public BoxFunctionDeclarationTransformer(JavaTranspiler transpiler) {
    	super(transpiler);
    }
	// @formatter:on
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxFunctionDeclaration	function			= ( BoxFunctionDeclaration ) node;
		String					packageName			= transpiler.getProperty( "packageName" );
		String					enclosingClassName	= transpiler.getProperty( "classname" );
		String					className			= "Func_" + function.getName();

		if ( context == TransformerContext.REGISTER ) {
			Map<String, String>	values		= Map.ofEntries(
			    Map.entry( "className", className ),
			    Map.entry( "contextName", transpiler.peekContextName() ),
			    Map.entry( "enclosingClassName", enclosingClassName )
			);
			Node				javaStmt	= parseStatement( registrationTemplate, values );
			logger.info( node.getSourceText() + " -> " + javaStmt );
			addIndex( javaStmt, node );
			return javaStmt;

		} else {

			Map<String, String> values = Map.ofEntries(
			    Map.entry( "packageName", packageName ),
			    Map.entry( "className", className ),
			    Map.entry( "access", function.getAccessModifier().toString().toUpperCase() ),
			    Map.entry( "functionName", function.getName() ),
			    Map.entry( "returnType", function.getType().getType().name() ),
			    Map.entry( "enclosingClassName", enclosingClassName )
			);
			transpiler.pushContextName( "context" );

			String							code	= PlaceholderHelper.resolve( classTemplate, values );
			ParseResult<CompilationUnit>	result;
			try {
				result = javaParser.parse( code );
			} catch ( Exception e ) {
				// Temp debugging to see generated Java code
				throw new BoxRuntimeException( code, e );
			}
			if ( !result.isSuccessful() ) {
				// Temp debugging to see generated Java code
				throw new BoxRuntimeException( result + "\n" + code );
			}

			/* Transform the arguments creating the initialization values */
			ArrayInitializerExpr argInitializer = new ArrayInitializerExpr();
			function.getArgs().forEach( arg -> {
				Expression argument = ( Expression ) transpiler.transform( arg );
				argInitializer.getValues().add( argument );
			} );
			result.getResult().orElseThrow().getType( 0 ).getFieldByName( "arguments" ).orElseThrow().getVariable( 0 ).setInitializer( argInitializer );

			/* Transform the annotations creating the initialization value */
			Expression annotationStruct = transformAnnotations( function.getAnnotations() );
			result.getResult().orElseThrow().getType( 0 ).getFieldByName( "annotations" ).orElseThrow().getVariable( 0 ).setInitializer( annotationStruct );

			/* Transform the documentation creating the initialization value */
			Expression documentationStruct = transformDocumentation( function.getDocumentation() );
			result.getResult().orElseThrow().getType( 0 ).getFieldByName( "documentation" ).orElseThrow().getVariable( 0 )
			    .setInitializer( documentationStruct );

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
			// Ensure we have a return statement
			invokeMethod.getBody().get().addStatement( new ReturnStmt( new NullLiteralExpr() ) );
			transpiler.popContextName();
			return javaClass;
		}
	}
}
