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
package ortus.boxlang.transpiler.transformer;

import java.time.LocalDateTime;
import java.util.Map;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.ast.BoxClass;
import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Source;
import ortus.boxlang.ast.SourceFile;
import ortus.boxlang.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.ast.statement.BoxImport;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.transpiler.JavaTranspiler;

public class BoxClassTransformer extends AbstractTransformer {

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
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
		import ortus.boxlang.runtime.types.*;
		import ortus.boxlang.runtime.types.exceptions.*;
		import ortus.boxlang.runtime.runnables.IClassRunnable;
		import ortus.boxlang.runtime.dynamic.IReferenceable;
		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.context.ClassBoxContext;

		import java.nio.file.Path;
		import java.nio.file.Paths;
		import java.time.LocalDateTime;
		import java.util.List;
		import java.util.Iterator;
		import java.util.Map;
		import java.util.HashMap;

		public class ${className} implements IClassRunnable, IReferenceable {

			private static final List<ImportDefinition>	imports			= List.of();
			private static final Path					path			= Paths.get( "${fileFolderPath}" );
			private static final long					compileVersion	= ${compileVersion};
			private static final LocalDateTime			compiledOn		= ${compiledOnTimestamp};
			private static final Object					ast				= null;
			public static final Key[]					keys			= new Key[] {};

			private final static Struct	annotations;
			private final static Struct	documentation;

			private IScope variablesScope = new VariablesScope();
			private IScope thisScope = new ThisScope();

			public ${className}() {
			}

			public void pseudoConstructor( IBoxContext context ) {
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

			/**
			 * Get the variables scope
			 */
			public IScope getVariablesScope() {
				return variablesScope;
			}

			/**
			 * Get the this scope
			 */
			public IScope getThisScope() {
				return thisScope;
			}			

			public Struct getAnnotations() {
				return annotations;
			}

			public Struct getDocumentation() {
				return documentation;
			}

			

			/**
			 * --------------------------------------------------------------------------
			 * IReferenceable Interface Methods
			 * --------------------------------------------------------------------------
			 */

			/**
			 * Assign a value to a key
			 *
			 * @param key   The key to assign
			 * @param value The value to assign
			 */
			public Object assign( Key key, Object value ) {
				// TODO: implicit setters
				thisScope.assign( key, value );
				return value;
			}

			/**
			 * Dereference this object by a key and return the value, or throw exception
			 *
			 * @param key  The key to dereference
			 * @param safe Whether to throw an exception if the key is not found
			 *
			 * @return The requested object
			 */
			public Object dereference( Key key, Boolean safe ) {
				// TODO: implicit getters
				return thisScope.dereference( key, safe );
			}

			/**
			 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
			 *
			 * @param name                The key to dereference
			 * @param positionalArguments The positional arguments to pass to the invokable
			 * @param safe                Whether to throw an exception if the key is not found
			 *
			 * @return The requested object
			 */
			public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
				// TODO: component member methods?
				
				Object value = thisScope.get( name );
				if ( value != null ) {

					if ( value instanceof Function function ) {
						FunctionBoxContext functionContext = Function.generateFunctionContext(
								function,
								// Function contexts' parent is the caller.  The function will "know" about the CFC it's executing in 
								// because we've pushed the CFC onto the template stack in the function context.
								context,
								name,
								function.createArgumentsScope( positionalArguments )
							);
							functionContext.pushTemplate( this );
						try {
							return function.invoke( functionContext );
						} finally{
							functionContext.popTemplate();
						}
					} else {
						throw new BoxRuntimeException(
							"key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
					}
				}

				throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
			}

			/**
			 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
			 *
			 * @param name           The name of the key to dereference, which becomes the method name
			 * @param namedArguments The arguments to pass to the invokable
			 * @param safe           If true, return null if the method is not found, otherwise throw an exception
			 *
			 * @return The requested return value or null
			 */
			public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

				Object value = thisScope.get( name );
				if ( value != null ) {
					if ( value instanceof Function function ) {
						FunctionBoxContext functionContext = Function.generateFunctionContext(
								function,
								// Function contexts' parent is the caller.  The function will "know" about the CFC it's executing in 
								// because we've pushed the CFC onto the template stack in the function context.
								context,
								name,
								function.createArgumentsScope( namedArguments )
							);
							functionContext.pushTemplate( this );
						try {
							return function.invoke( functionContext );
						} finally{
							functionContext.popTemplate();
						}
					} else {
						throw new BoxRuntimeException(
							"key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function "
						);
					}
				}

				throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
			}


		}
	""";
	// @formatter:on

	/**
	 * Constructor
	 *
	 * @param transpiler parent transpiler
	 */
	public BoxClassTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxClass	boxClass	= ( BoxClass ) node;
		Source		source		= boxClass.getPosition().getSource();
		String		packageName	= transpiler.getProperty( "packageName" );
		String		className	= transpiler.getProperty( "classname" );
		String		fileName	= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String		fileExt		= fileName.substring( fileName.lastIndexOf( "." ) + 1 );
		String		filePath	= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath() : "unknown";

		//
		className	= transpiler.getProperty( "classname" ) != null ? transpiler.getProperty( "classname" ) : className;
		packageName	= transpiler.getProperty( "packageName" ) != null ? transpiler.getProperty( "packageName" ) : packageName;

		Map<String, String>				values	= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "fileName", fileName ),
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

		CompilationUnit		entryPoint				= result.getResult().get();

		MethodDeclaration	pseudoConstructorMethod	= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "pseudoConstructor" ).get( 0 );

		FieldDeclaration	imports					= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "imports" ).orElseThrow();

		FieldDeclaration	keys					= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "keys" ).orElseThrow();

		/* Transform the annotations creating the initialization value */
		Expression			annotationStruct		= transformAnnotations( boxClass.getAnnotations() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "annotations" ).orElseThrow().getVariable( 0 ).setInitializer( annotationStruct );

		/* Transform the documentation creating the initialization value */
		Expression documentationStruct = transformDocumentation( boxClass.getDocumentation() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "documentation" ).orElseThrow().getVariable( 0 )
		    .setInitializer( documentationStruct );

		transpiler.pushContextName( "context" );
		// Add imports
		for ( BoxImport statement : boxClass.getImports() ) {
			Node			javaASTNode	= transpiler.transform( statement );
			// For import statements, we add an argument to the constructor of the static List of imports
			MethodCallExpr	imp			= ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
			imp.getArguments().add( ( MethodCallExpr ) javaASTNode );
		}
		// Add body
		for ( BoxStatement statement : boxClass.getBody() ) {

			Node javaASTNode = transpiler.transform( statement );
			// For Function declarations, we add the transformed function itself as a compilation unit
			// and also hoist the declaration itself to the top of the _invoke() method.
			if ( statement instanceof BoxFunctionDeclaration BoxFunc ) {
				// a function declaration generate
				( ( JavaTranspiler ) transpiler ).getUDFcallables().put( Key.of( BoxFunc.getName() ), ( CompilationUnit ) javaASTNode );
				Node registrer = transpiler.transform( statement, TransformerContext.REGISTER );
				pseudoConstructorMethod.getBody().orElseThrow().addStatement( 0, ( Statement ) registrer );

			} else {
				// Java block get each statement in their block added
				if ( javaASTNode instanceof BlockStmt ) {
					BlockStmt stmt = ( BlockStmt ) javaASTNode;
					stmt.getStatements().forEach( it -> {
						pseudoConstructorMethod.getBody().get().addStatement( it );
						// statements.add( it );
					} );
				} else if ( statement instanceof BoxImport ) {
					// For import statements, we add an argument to the constructor of the static List of imports
					MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
					imp.getArguments().add( ( MethodCallExpr ) javaASTNode );
				} else {
					// All other statements are added to the _invoke() method
					pseudoConstructorMethod.getBody().orElseThrow().addStatement( ( Statement ) javaASTNode );
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
		return entryPoint;
	}

}
