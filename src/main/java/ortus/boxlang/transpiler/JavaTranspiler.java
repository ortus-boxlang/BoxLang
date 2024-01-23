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
package ortus.boxlang.transpiler;

import java.io.File;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;

import ortus.boxlang.ast.BoxBufferOutput;
import ortus.boxlang.ast.BoxClass;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxScriptIsland;
import ortus.boxlang.ast.expression.BoxArgument;
import ortus.boxlang.ast.expression.BoxArrayAccess;
import ortus.boxlang.ast.expression.BoxArrayLiteral;
import ortus.boxlang.ast.expression.BoxAssignment;
import ortus.boxlang.ast.expression.BoxBinaryOperation;
import ortus.boxlang.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.ast.expression.BoxClosure;
import ortus.boxlang.ast.expression.BoxComparisonOperation;
import ortus.boxlang.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.ast.expression.BoxDotAccess;
import ortus.boxlang.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.ast.expression.BoxFQN;
import ortus.boxlang.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.ast.expression.BoxLambda;
import ortus.boxlang.ast.expression.BoxMethodInvocation;
import ortus.boxlang.ast.expression.BoxNegateOperation;
import ortus.boxlang.ast.expression.BoxNewOperation;
import ortus.boxlang.ast.expression.BoxNull;
import ortus.boxlang.ast.expression.BoxParenthesis;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.ast.expression.BoxStringConcat;
import ortus.boxlang.ast.expression.BoxStringInterpolation;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.expression.BoxStructLiteral;
import ortus.boxlang.ast.expression.BoxTernaryOperation;
import ortus.boxlang.ast.expression.BoxUnaryOperation;
import ortus.boxlang.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.ast.statement.BoxAssert;
import ortus.boxlang.ast.statement.BoxBreak;
import ortus.boxlang.ast.statement.BoxContinue;
import ortus.boxlang.ast.statement.BoxDo;
import ortus.boxlang.ast.statement.BoxExpression;
import ortus.boxlang.ast.statement.BoxForIn;
import ortus.boxlang.ast.statement.BoxForIndex;
import ortus.boxlang.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.ast.statement.BoxIfElse;
import ortus.boxlang.ast.statement.BoxImport;
import ortus.boxlang.ast.statement.BoxInclude;
import ortus.boxlang.ast.statement.BoxRethrow;
import ortus.boxlang.ast.statement.BoxReturn;
import ortus.boxlang.ast.statement.BoxSwitch;
import ortus.boxlang.ast.statement.BoxThrow;
import ortus.boxlang.ast.statement.BoxTry;
import ortus.boxlang.ast.statement.BoxWhile;
import ortus.boxlang.ast.statement.tag.BoxOutput;
import ortus.boxlang.ast.statement.tag.BoxTagIsland;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.runnables.compiler.JavaSourceString;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.transpiler.transformer.BoxClassTransformer;
import ortus.boxlang.transpiler.transformer.Transformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;
import ortus.boxlang.transpiler.transformer.expression.BoxAccessTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxArgumentTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxArrayLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxAssignmentTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxBinaryOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxBooleanLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxClosureTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxComparisonOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxDecimalLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxExpressionInvocationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxFQNTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxFunctionInvocationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxIdentifierTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxIntegerLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxLambdaTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxMethodInvocationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxNegateOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxNewOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxNullTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxParenthesisTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxScopeTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxStringConcatTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxStringInterpolationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxStructLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxTernaryOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxUnaryOperationTransformer;
import ortus.boxlang.transpiler.transformer.indexer.CrossReference;
import ortus.boxlang.transpiler.transformer.indexer.IndexPrettyPrinterVisitor;
import ortus.boxlang.transpiler.transformer.statement.BoxArgumentDeclarationTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxAssertTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxBreakTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxBufferOutputTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxContinueTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxDoTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxExpressionTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxForInTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxForIndexTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxFunctionDeclarationTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxIfElseTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxImportTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxIncludeTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxRethrowTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxReturnTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxScriptTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxSwitchTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxTemplateTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxThrowTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxTryTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxWhileTransformer;
import ortus.boxlang.transpiler.transformer.statement.tag.BoxOutputTransformer;
import ortus.boxlang.transpiler.transformer.statement.tag.BoxScriptIslandTransformer;
import ortus.boxlang.transpiler.transformer.statement.tag.BoxTagIslandTransformer;

/**
 * BoxLang AST to Java AST transpiler
 * The registry maps a AST node to the corresponding Transformer Java class instance.
 * Each transformer implements the logic to convert the BoxLang AST nodes into Java
 * AST nodes.
 *
 */
public class JavaTranspiler extends Transpiler {

	static Logger								logger			= LoggerFactory.getLogger( JavaTranspiler.class );

	private static HashMap<Class, Transformer>	registry		= new HashMap<>();
	private List<Statement>						statements		= new ArrayList<>();
	private List<CrossReference>				crossReferences	= new ArrayList<>();
	private Map<Key, CompilationUnit>			UDFcallables	= new HashMap<Key, CompilationUnit>();
	private List<CompilationUnit>				callables		= new ArrayList<>();

	public JavaTranspiler() {
		registry.put( BoxScript.class, new BoxScriptTransformer( this ) );
		registry.put( BoxExpression.class, new BoxExpressionTransformer( this ) );

		// Expressions
		registry.put( BoxIdentifier.class, new BoxIdentifierTransformer( this ) );
		registry.put( BoxScope.class, new BoxScopeTransformer( this ) );
		// Literals
		registry.put( BoxStringLiteral.class, new BoxStringLiteralTransformer( this ) );
		registry.put( BoxIntegerLiteral.class, new BoxIntegerLiteralTransformer( this ) );
		registry.put( BoxBooleanLiteral.class, new BoxBooleanLiteralTransformer( this ) );
		registry.put( BoxDecimalLiteral.class, new BoxDecimalLiteralTransformer( this ) );
		registry.put( BoxStringInterpolation.class, new BoxStringInterpolationTransformer( this ) );
		registry.put( BoxStringConcat.class, new BoxStringConcatTransformer( this ) );
		registry.put( BoxArgument.class, new BoxArgumentTransformer( this ) );
		registry.put( BoxFQN.class, new BoxFQNTransformer( this ) );

		registry.put( BoxParenthesis.class, new BoxParenthesisTransformer( this ) );
		registry.put( BoxBinaryOperation.class, new BoxBinaryOperationTransformer( this ) );
		registry.put( BoxTernaryOperation.class, new BoxTernaryOperationTransformer( this ) );
		registry.put( BoxNegateOperation.class, new BoxNegateOperationTransformer( this ) );
		registry.put( BoxComparisonOperation.class, new BoxComparisonOperationTransformer( this ) );
		registry.put( BoxUnaryOperation.class, new BoxUnaryOperationTransformer( this ) );

		// All access nodes use the same base transformer
		registry.put( BoxDotAccess.class, new BoxAccessTransformer( this ) );
		registry.put( BoxArrayAccess.class, new BoxAccessTransformer( this ) );

		registry.put( BoxMethodInvocation.class, new BoxMethodInvocationTransformer( this ) );
		registry.put( BoxFunctionInvocation.class, new BoxFunctionInvocationTransformer( this ) );
		registry.put( BoxIfElse.class, new BoxIfElseTransformer( this ) );
		registry.put( BoxWhile.class, new BoxWhileTransformer( this ) );
		registry.put( BoxDo.class, new BoxDoTransformer( this ) );
		registry.put( BoxSwitch.class, new BoxSwitchTransformer( this ) );
		registry.put( BoxBreak.class, new BoxBreakTransformer( this ) );
		registry.put( BoxContinue.class, new BoxContinueTransformer( this ) );
		registry.put( BoxForIn.class, new BoxForInTransformer( this ) );
		registry.put( BoxForIndex.class, new BoxForIndexTransformer( this ) );
		registry.put( BoxAssert.class, new BoxAssertTransformer( this ) );
		registry.put( BoxTry.class, new BoxTryTransformer( this ) );
		registry.put( BoxThrow.class, new BoxThrowTransformer( this ) );
		registry.put( BoxNewOperation.class, new BoxNewOperationTransformer( this ) );
		registry.put( BoxFunctionDeclaration.class, new BoxFunctionDeclarationTransformer( this ) );
		registry.put( BoxArgumentDeclaration.class, new BoxArgumentDeclarationTransformer( this ) );
		registry.put( BoxReturn.class, new BoxReturnTransformer( this ) );
		registry.put( BoxRethrow.class, new BoxRethrowTransformer( this ) );
		registry.put( BoxImport.class, new BoxImportTransformer( this ) );
		registry.put( BoxArrayLiteral.class, new BoxArrayLiteralTransformer( this ) );
		registry.put( BoxStructLiteral.class, new BoxStructLiteralTransformer( this ) );
		registry.put( BoxAssignment.class, new BoxAssignmentTransformer( this ) );
		registry.put( BoxNull.class, new BoxNullTransformer( this ) );
		registry.put( BoxLambda.class, new BoxLambdaTransformer( this ) );
		registry.put( BoxInclude.class, new BoxIncludeTransformer( this ) );
		registry.put( BoxExpressionInvocation.class, new BoxExpressionInvocationTransformer( this ) );
		registry.put( BoxClosure.class, new BoxClosureTransformer( this ) );
		registry.put( BoxClass.class, new BoxClassTransformer( this ) );

		// Tags
		registry.put( ortus.boxlang.ast.BoxTemplate.class, new BoxTemplateTransformer( this ) );
		registry.put( BoxBufferOutput.class, new BoxBufferOutputTransformer( this ) );
		registry.put( BoxOutput.class, new BoxOutputTransformer( this ) );
		registry.put( BoxScriptIsland.class, new BoxScriptIslandTransformer( this ) );
		registry.put( BoxTagIsland.class, new BoxTagIslandTransformer( this ) );
	}

	/**
	 * Utility method to transform a node
	 *
	 * @param node a BoxLang AST Node
	 *
	 * @return a JavaParser AST Node
	 *
	 * @throws IllegalStateException
	 */
	public Node transform( BoxNode node ) throws IllegalStateException {
		return this.transform( node, TransformerContext.NONE );
	}

	/**
	 * Utility method to transform a node with a transformation context
	 *
	 * @param node    a BoxLang AST Node
	 * @param context transformation context
	 *
	 * @return
	 *
	 * @throws IllegalStateException
	 *
	 * @see TransformerContext
	 */
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			Node javaNode = transformer.transform( node, context );
			// logger.debug(transformer.getClass().getSimpleName() + " : " + node.getSourceText() + " -> " + javaNode );
			return javaNode;
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}

	public List<Statement> getStatements() {
		return statements;
	}

	/**
	 * Cross reference
	 *
	 * @return the list of references between the source and the generated code
	 */

	public List<CrossReference> getCrossReferences() {
		return crossReferences;
	}

	/**
	 * Write a class bytecode
	 *
	 * @param cu         java compilation unit
	 * @param outputPath output directory
	 * @param classPath  classpath
	 *
	 * @throws IllegalStateException in the compilation fails
	 */
	public String compileJava( CompilationUnit cu, String outputPath, List<String> classPath ) throws IllegalStateException {
		JavaCompiler						compiler		= ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject>	diagnostics		= new DiagnosticCollector<>();
		String								pkg				= cu.getPackageDeclaration().orElseThrow().getName().toString();
		String								name			= cu.getType( 0 ).getName().asString();
		String								fqn				= pkg + "." + name;
		List<JavaFileObject>				sourceFiles		= Collections.singletonList( new JavaSourceString( fqn, cu.toString() ) );

		Writer								output			= null;

		ArrayList<String>					classPathList	= new ArrayList<>();
		classPathList.add( System.getProperty( "java.class.path" ) );
		classPathList.addAll( classPath );
		classPathList.add( outputPath );
		String compilerClassPath = classPathList
		    .stream()
		    .map( it -> {
			    return it;
		    } )
		    .collect( Collectors.joining( File.pathSeparator ) );
		;
		StandardJavaFileManager			stdFileManager	= compiler.getStandardFileManager( null, null, null );

		List<String>					options			= new ArrayList<>(
		    List.of( "-g",
		        "-cp",
		        compilerClassPath,
		        "-d",
		        outputPath
		    )
		);

		JavaCompiler.CompilationTask	task			= compiler.getTask( output, stdFileManager, diagnostics, options, null, sourceFiles );
		boolean							result			= task.call();

		if ( !result ) {
			diagnostics.getDiagnostics()
			    .forEach( d -> logger.error( String.valueOf( d ) ) );
			throw new IllegalStateException( "Compiler Error" );
		}
		try {
			stdFileManager.close();
		} catch ( Exception e ) {
			throw new IllegalStateException( "Compiler Error" );
		}

		return fqn;
	}

	/**
	 * Runa a java class
	 *
	 * @param fqn
	 * @param classPath
	 *
	 * @throws ClassNotFoundException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 */
	public void runJavaClass( String fqn, List<String> classPath )
	    throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		List<URL> finalClassPath = new ArrayList<>();
		for ( String path : classPath ) {
			try {
				finalClassPath.add( new File( path ).toURI().toURL() );

			} catch ( MalformedURLException e ) {
				throw new RuntimeException( e );
			}
		}

		try {
			URL[]			classLoaderClassPath	= finalClassPath.toArray( new URL[ 0 ] );
			URLClassLoader	classLoader				= new URLClassLoader(
			    classLoaderClassPath,
			    this.getClass().getClassLoader()
			);
			Class			boxClass				= Class.forName( fqn, true, classLoader );
			Method			method					= boxClass.getDeclaredMethod( "getInstance" );
			Object			instance				= method.invoke( boxClass );

			// Runtime
			BoxRuntime		rt						= BoxRuntime.getInstance();

			rt.executeTemplate( ( BoxTemplate ) instance );

			rt.shutdown();

		} catch ( Throwable e ) {
			throw e;
		}

	}

	/**
	 * Transpile a BoxLang AST into a Java Parser AST
	 *
	 * @return a Java Parser TranspiledCode representing the equivalent Java code
	 *
	 * @throws IllegalStateException
	 *
	 *
	 * @see TranspiledCode
	 */
	@Override
	public TranspiledCode transpile( BoxNode node ) throws ApplicationException {

		CompilationUnit				entryPoint	= ( CompilationUnit ) transform( node );

		IndexPrettyPrinterVisitor	visitor		= new IndexPrettyPrinterVisitor( new DefaultPrinterConfiguration() );
		entryPoint.accept( visitor, null );
		this.crossReferences.addAll( visitor.getCrossReferences() );

		List<CompilationUnit> allCallables = getCallables();
		allCallables.addAll( getUDFcallables().values() );
		return new TranspiledCode( entryPoint, allCallables );
	}

	/**
	 * Get the list of compilation units that represent the callable functions
	 *
	 * @return the list of compilation units
	 */
	public List<CompilationUnit> getCallables() {
		return callables;
	}

	/**
	 * Get the list of compilation units that represent the callable functions
	 *
	 * @return the list of compilation units
	 */
	public Map<Key, CompilationUnit> getUDFcallables() {
		return UDFcallables;
	}

}
