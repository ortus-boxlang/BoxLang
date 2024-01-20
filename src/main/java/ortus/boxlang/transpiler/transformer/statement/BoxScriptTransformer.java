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

import java.time.LocalDateTime;
import java.util.Map;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Source;
import ortus.boxlang.ast.SourceFile;
import ortus.boxlang.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxExpression;
import ortus.boxlang.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.ast.statement.BoxImport;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxScriptTransformer extends AbstractTransformer {

	// @formatter:off
	private final String template = """
		package ${packageName};

		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.context.*;

		// BoxLang Auto Imports
		import ortus.boxlang.runtime.runnables.BoxTemplate;
		import ortus.boxlang.runtime.runnables.BoxScript;
		import ortus.boxlang.runtime.dynamic.Referencer;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.loader.ClassLocator;
		import ortus.boxlang.runtime.loader.ImportDefinition;
		import ortus.boxlang.runtime.operators.*;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.scopes.*;
		import ortus.boxlang.web.scopes.*;
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
		import ortus.boxlang.runtime.types.*;
		import ortus.boxlang.runtime.runnables.IBoxRunnable;
		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.context.ClassBoxContext;
		import ortus.boxlang.runtime.interop.DynamicObject;

		import java.nio.file.Path;
		import java.nio.file.Paths;
		import java.time.LocalDateTime;
		import java.util.List;
		import java.util.Iterator;
		import java.util.Map;
		import java.util.HashMap;
		import java.util.LinkedHashMap;

		public class ${className} extends ${baseclass} {

			private static ${className} instance;

			private static final List<ImportDefinition>	imports			= List.of();
			private static final Path					path			= Paths.get( "${fileFolderPath}" );
			private static final long					compileVersion	= ${compileVersion};
			private static final LocalDateTime			compiledOn		= ${compiledOnTimestamp};
			private static final Object					ast				= null;
			public static final Key[]					keys			= new Key[] {};

			public ${className}() {
			}

			public static synchronized ${className} getInstance() {
				if ( instance == null ) {
					instance = new ${className}();
				}
				return instance;
			}
			/**
				* Each template must implement the invoke() method which executes the template
				*
				* @param context The execution context requesting the execution
				*/
			public ${returnType} _invoke( IBoxContext context ) {
				ClassLocator classLocator = ClassLocator.getInstance();
			}

			// ITemplateRunnable implementation methods

			/**
				* The version of the BoxLang runtime
			*/
			public long getRunnableCompileVersion() {
				return ${className}.compileVersion;
			}

			/**
				* The date the template was compiled
			*/
			public LocalDateTime getRunnableCompiledOn() {
				return ${className}.compiledOn;
			}

			/**
				* The AST (abstract syntax tree) of the runnable
			*/
			public Object getRunnableAST() {
				return ${className}.ast;
			}

			/**
				* The path to the template
			*/
			public Path getRunnablePath() {
			return ${className}.path;
			}

			/**
			 * The imports for this runnable
			 */
			public List<ImportDefinition> getImports() {
				return imports;
			}

		}
	""";
	// @formatter:on

	/**
	 * Constructor
	 *
	 * @param transpiler parent transpiler
	 */
	public BoxScriptTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxScript	script		= ( BoxScript ) node;
		Source		source		= script.getPosition().getSource();
		String		packageName	= transpiler.getProperty( "packageName" );
		String		className	= transpiler.getProperty( "classname" );
		String		fileName	= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String		fileExt		= fileName.substring( fileName.lastIndexOf( "." ) + 1 );
		String		filePath	= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath() : "unknown";

		//
		className	= transpiler.getProperty( "classname" ) != null ? transpiler.getProperty( "classname" ) : className;
		packageName	= transpiler.getProperty( "packageName" ) != null ? transpiler.getProperty( "packageName" ) : packageName;
		String	baseClass	= transpiler.getProperty( "baseclass" ) != null ? transpiler.getProperty( "baseclass" ) : "BoxScript";
		String	returnType	= baseClass.equals( "BoxScript" ) ? "Object" : "void";
		returnType = transpiler.getProperty( "returnType" ) != null ? transpiler.getProperty( "returnType" ) : returnType;

		Map<String, String>				values	= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "fileName", fileName ),
		    Map.entry( "baseclass", baseClass ),
		    Map.entry( "returnType", returnType ),
		    Map.entry( "fileExtension", fileExt ),
		    Map.entry( "fileFolderPath", filePath.replaceAll( "\\\\", "\\\\\\\\" ) ),
		    Map.entry( "compiledOnTimestamp", transpiler.getDateTime( LocalDateTime.now() ) ),
		    Map.entry( "compileVersion", "1L" )
		);
		String							code	= PlaceholderHelper.resolve( template, values );
		ParseResult<CompilationUnit>	result;

		try {
			result = javaParser.parse( code );
		} catch ( Exception e ) {
			// Temp debugging to see generated Java code
			throw new BoxRuntimeException( code, e );
		}
		if ( !result.isSuccessful() ) {
			// Temp debugging to see generated Java code
			throw new BoxRuntimeException( result.toString() + "\n" + code );
		}

		CompilationUnit		entryPoint		= result.getResult().get();

		MethodDeclaration	invokeMethod	= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "_invoke" ).get( 0 );

		FieldDeclaration	imports			= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "imports" ).orElseThrow();

		FieldDeclaration	keys			= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "keys" ).orElseThrow();

		transpiler.pushContextName( "context" );
		// Track if the latest BL AST node we encountered was a returnable expression
		boolean lastStatementIsReturnable = false;
		for ( BoxStatement statement : script.getStatements() ) {
			// Expressions are returnable
			lastStatementIsReturnable = statement instanceof BoxExpression;

			Node javaASTNode = transpiler.transform( statement );
			// For Function declarations, we add the transformed function itself as a compilation unit
			// and also hoist the declaration itself to the top of the _invoke() method.
			if ( statement instanceof BoxFunctionDeclaration BoxFunc ) {
				// a function declaration generate
				( ( JavaTranspiler ) transpiler ).getUDFcallables().put( Key.of( BoxFunc.getName() ), ( CompilationUnit ) javaASTNode );
				Node registrer = transpiler.transform( statement, TransformerContext.REGISTER );
				invokeMethod.getBody().orElseThrow().addStatement( 0, ( Statement ) registrer );

			} else {
				// Java block get each statement in their block added
				if ( javaASTNode instanceof BlockStmt ) {
					BlockStmt stmt = ( BlockStmt ) javaASTNode;
					stmt.getStatements().forEach( it -> {
						invokeMethod.getBody().get().addStatement( it );
						// statements.add( it );
					} );
				} else if ( statement instanceof BoxImport ) {
					// For import statements, we add an argument to the constructor of the static List of imports
					MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
					imp.getArguments().add( ( MethodCallExpr ) javaASTNode );
				} else {
					// All other statements are added to the _invoke() method
					invokeMethod.getBody().orElseThrow().addStatement( ( Statement ) javaASTNode );
					// statements.add( ( Statement ) javaASTNode );
				}
			}
		}

		// Add the keys to the static keys array
		ArrayCreationExpr keysImp = ( ArrayCreationExpr ) keys.getVariable( 0 ).getInitializer().orElseThrow();
		for ( Map.Entry<String, BoxExpr> entry : transpiler.getKeys().entrySet() ) {
			MethodCallExpr methodCallExpr = new MethodCallExpr( new NameExpr( "Key" ), "of" );
			if ( entry.getValue() instanceof BoxStringLiteral str ) {
				methodCallExpr.addArgument( new StringLiteralExpr( str.getValue() ) );
			} else if ( entry.getValue() instanceof BoxIntegerLiteral id ) {
				methodCallExpr.addArgument( new IntegerLiteralExpr( id.getValue() ) );
			} else {
				throw new IllegalStateException( "Unsupported key type: " + entry.getValue().getClass().getSimpleName() );
			}
			keysImp.getInitializer().get().getValues().add( methodCallExpr );
		}

		transpiler.popContextName();

		// Only try to return a value if the class has a return type for the _invoke() method...
		if ( ! ( invokeMethod.getType() instanceof com.github.javaparser.ast.type.VoidType ) ) {
			int			lastIndex	= invokeMethod.getBody().get().getStatements().size() - 1;
			Statement	last		= invokeMethod.getBody().get().getStatements().get( lastIndex );
			// ... and the last BL AST node was a returnable expression and the last Java AST node is an expression statement
			if ( lastStatementIsReturnable && last instanceof ExpressionStmt stmt ) {
				invokeMethod.getBody().get().getStatements().remove( lastIndex );
				invokeMethod.getBody().get().getStatements().add( new ReturnStmt( stmt.getExpression() ) );
			} else {
				// If our base class requires a return value and we have none, then add a statement to return null.
				invokeMethod.getBody().orElseThrow().addStatement( new ReturnStmt( new NullLiteralExpr() ) );
			}

		}

		return entryPoint;
	}

}
