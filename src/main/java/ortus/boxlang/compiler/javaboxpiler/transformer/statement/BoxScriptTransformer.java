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
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import java.util.Map;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxScriptTransformer extends AbstractTransformer {

	// @formatter:off
	private final String template = """
		package ${packageName};

		// BoxLang Auto Imports
		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.components.Component;
		import ortus.boxlang.runtime.context.*;
		import ortus.boxlang.runtime.context.ClassBoxContext;
		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
		import ortus.boxlang.runtime.dynamic.Referencer;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.loader.ClassLocator;
		import ortus.boxlang.runtime.loader.ImportDefinition;
		import ortus.boxlang.runtime.operators.*;
		import ortus.boxlang.runtime.runnables.BoxScript;
		import ortus.boxlang.runtime.runnables.BoxTemplate;
		import ortus.boxlang.runtime.runnables.IBoxRunnable;
		import ortus.boxlang.runtime.scopes.*;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.types.*;
		import ortus.boxlang.runtime.types.util.*;
		import ortus.boxlang.runtime.types.exceptions.*;
		import ortus.boxlang.runtime.types.meta.FunctionMeta;
		import ortus.boxlang.runtime.util.*;
		import ortus.boxlang.compiler.parser.BoxSourceType;
		import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
		import ortus.boxlang.runtime.runnables.BoxClassSupport;
		import ortus.boxlang.compiler.BoxByteCodeVersion;

		// Java Imports
		import java.nio.file.Path;
		import java.nio.file.Paths;
		import java.time.LocalDateTime;
		import java.util.ArrayList;
		import java.util.Arrays;
		import java.util.HashMap;
		import java.util.Iterator;
		import java.util.LinkedHashMap;
		import java.util.List;
		import java.util.Map;
		import java.util.Optional;

		@BoxByteCodeVersion(boxlangVersion="${boxlangVersion}", bytecodeVersion=${bytecodeVersion})
		public class ${className} extends ${baseclass} {

			private static ${className} instance;

			private static final List<ImportDefinition>	imports			= List.of();
			private static final ResolvedFilePath					path			= ${resolvedFilePath};
			private static final BoxSourceType			sourceType		= BoxSourceType.${sourceType};
			public static final Key[]					keys			= new Key[] {};
			private static Map<Key,UDF>					udfs			= StructUtil.<UDF>linkedMapOf();
			private static List<Lambda>					lambdas			= new ArrayList<>( Arrays.asList( new Lambda[] {} ) );
			private static List<ClosureDefinition>		closures		= new ArrayList<>( Arrays.asList( new ClosureDefinition[] {} ) );

			public ${className}() {
			}

			public static ${className} getInstance() {
				if ( instance == null ) {
					synchronized ( ${className}.class ) {
						if ( instance == null ) {
							instance = new ${className}();
						}
					}
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
				* The path to the template
			*/
			public ResolvedFilePath getRunnablePath() {
				return ${className}.path;
			}

			/**
			 * The original source type
			 */
			public BoxSourceType getSourceType() {
				return sourceType;
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

		BoxScript	script			= ( BoxScript ) node;
		Source		source			= script.getPosition().getSource();
		String		packageName		= transpiler.getProperty( "packageName" );
		String		className		= transpiler.getProperty( "classname" );
		String		mappingName		= transpiler.getProperty( "mappingName" );
		String		mappingPath		= transpiler.getProperty( "mappingPath" );
		String		relativePath	= transpiler.getProperty( "relativePath" );
		String		fileName		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String		fileExt			= fileName.substring( fileName.lastIndexOf( "." ) + 1 );
		String		filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath() : "unknown";

		//
		className	= transpiler.getProperty( "classname" ) != null ? transpiler.getProperty( "classname" ) : className;
		packageName	= transpiler.getProperty( "packageName" ) != null ? transpiler.getProperty( "packageName" ) : packageName;
		String	baseClass	= transpiler.getProperty( "baseclass" ) != null ? transpiler.getProperty( "baseclass" ) : "BoxScript";
		String	returnType	= baseClass.equals( "BoxScript" ) ? "Object" : "void";
		returnType = transpiler.getProperty( "returnType" ) != null ? transpiler.getProperty( "returnType" ) : returnType;
		String							sourceType	= transpiler.getProperty( "sourceType" );

		Map<String, String>				values		= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "fileName", fileName ),
		    Map.entry( "baseclass", baseClass ),
		    Map.entry( "resolvedFilePath", transpiler.getResolvedFilePath( mappingName, mappingPath, relativePath, filePath ) ),
		    Map.entry( "returnType", returnType ),
		    Map.entry( "sourceType", sourceType ),
		    Map.entry( "boxlangVersion", BoxRuntime.getInstance().getVersionInfo().getAsString( Key.version ) ),
		    Map.entry( "bytecodeVersion", String.valueOf( IBoxpiler.BYTECODE_VERSION ) ),
		    Map.entry( "fileExtension", fileExt )
		);
		String							code		= PlaceholderHelper.resolve( template, values );
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

		CompilationUnit				entryPoint		= result.getResult().get();
		ClassOrInterfaceDeclaration	thisClass		= entryPoint.getClassByName( className ).orElseThrow();

		MethodDeclaration			invokeMethod	= thisClass.getMethodsByName( "_invoke" ).get( 0 );

		FieldDeclaration			imports			= thisClass.getFieldByName( "imports" ).orElseThrow();

		FieldDeclaration			keys			= thisClass.getFieldByName( "keys" ).orElseThrow();

		MethodCallExpr				udfs			= ( MethodCallExpr ) thisClass.getFieldByName( "udfs" ).orElseThrow()
		    .getVariable( 0 ).getInitializer().get();

		transpiler.pushContextName( "context" );

		BlockStmt	invokeBody					= invokeMethod.getBody().get();

		// Track if the latest BL AST node we encountered was a returnable expression
		boolean		lastStatementIsReturnable	= false;
		for ( BoxStatement statement : script.getStatements() ) {
			// Expressions are returnable
			lastStatementIsReturnable = statement instanceof BoxExpressionStatement;

			Node javaASTNode = transpiler.transform( statement );
			// These get left behind from UDF declarations
			if ( javaASTNode instanceof EmptyStmt ) {
				continue;
			}
			// For Function declarations, we add the transformed function itself as a compilation unit
			// and also hoist the declaration itself to the top of the _invoke() method.

			// Java block get each statement in their block added
			if ( javaASTNode instanceof BlockStmt ) {
				BlockStmt stmt = ( BlockStmt ) javaASTNode;
				stmt.getStatements().forEach( it -> {
					invokeBody.addStatement( it );
					// statements.add( it );
				} );
			} else {
				// All other statements are added to the _invoke() method
				invokeBody.addStatement( ( Statement ) javaASTNode );
			}
		}
		// loop over UDF registrations and add them to the _invoke() method
		( ( JavaTranspiler ) transpiler ).getUDFDeclarations().forEach( it -> {
			invokeBody.addStatement( 0, it );
		} );

		// loop over UDF registrations, add the static method to the class, and the UDF instantiation to the static block that initializes the UDFs map
		( ( JavaTranspiler ) transpiler ).getUDFInvokers().forEach( ( key, value ) -> {
			udfs.addArgument( createKey( key.getName() ) );
			udfs.addArgument( value.getSecond() );
			thisClass.addMember( value.getFirst() );
		} );

		// Process lambda invokers - add static methods and build the lambdas list initializer
		FieldDeclaration		lambdasField	= thisClass.getFieldByName( "lambdas" ).orElseThrow();
		ArrayCreationExpr		lambdasArray	= new ArrayCreationExpr( new ClassOrInterfaceType( null, "Lambda" ) );
		ArrayInitializerExpr	lambdasInit		= new ArrayInitializerExpr();
		lambdasArray.setInitializer( lambdasInit );
		( ( JavaTranspiler ) transpiler ).getLambdaInvokers().forEach( value -> {
			lambdasInit.getValues().add( value.getSecond() );
			thisClass.addMember( value.getFirst() );
		} );
		ObjectCreationExpr lambdasListExpr = new ObjectCreationExpr( null, new ClassOrInterfaceType( null, "ArrayList<>" ), new NodeList<>() );
		lambdasListExpr.addArgument( new MethodCallExpr( new NameExpr( "Arrays" ), "asList", new NodeList<>( lambdasArray ) ) );
		lambdasField.getVariable( 0 ).setInitializer( lambdasListExpr );

		// Process closure invokers - add static methods and build the closures list initializer
		FieldDeclaration		closuresField	= thisClass.getFieldByName( "closures" ).orElseThrow();
		ArrayCreationExpr		closuresArray	= new ArrayCreationExpr( new ClassOrInterfaceType( null, "ClosureDefinition" ) );
		ArrayInitializerExpr	closuresInit	= new ArrayInitializerExpr();
		closuresArray.setInitializer( closuresInit );
		( ( JavaTranspiler ) transpiler ).getClosureInvokers().forEach( value -> {
			closuresInit.getValues().add( value.getSecond() );
			thisClass.addMember( value.getFirst() );
		} );
		ObjectCreationExpr closuresListExpr = new ObjectCreationExpr( null, new ClassOrInterfaceType( null, "ArrayList<>" ), new NodeList<>() );
		closuresListExpr.addArgument( new MethodCallExpr( new NameExpr( "Arrays" ), "asList", new NodeList<>( closuresArray ) ) );
		closuresField.getVariable( 0 ).setInitializer( closuresListExpr );

		// For import statements, we add an argument to the constructor of the static List of imports
		MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
		imp.getArguments().addAll( transpiler.getJImports() );

		// Add the keys to the static keys array
		ArrayCreationExpr keysImp = ( ArrayCreationExpr ) keys.getVariable( 0 ).getInitializer().orElseThrow();
		for ( Map.Entry<String, BoxExpression> entry : transpiler.getKeys().entrySet() ) {
			MethodCallExpr methodCallExpr = new MethodCallExpr( new NameExpr( "Key" ), "of" );
			if ( entry.getValue() instanceof BoxStringLiteral str ) {
				methodCallExpr.addArgument( BoxStringLiteralTransformer.transform( str.getValue() ) );
			} else if ( entry.getValue() instanceof BoxIntegerLiteral id ) {
				methodCallExpr.addArgument( new IntegerLiteralExpr( id.getValue() ) );
			} else {
				throw new ExpressionException( "Unsupported key type: " + entry.getValue().getClass().getSimpleName(), entry.getValue() );
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
