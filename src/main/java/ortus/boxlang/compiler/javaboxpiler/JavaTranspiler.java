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
package ortus.boxlang.compiler.javaboxpiler;

import java.io.File;
import java.io.Writer;
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

import ortus.boxlang.compiler.JavaSourceString;
import ortus.boxlang.compiler.ast.BoxBufferOutput;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNegateOperation;
import ortus.boxlang.compiler.ast.expression.BoxNewOperation;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxAssert;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxParam;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.compiler.javaboxpiler.transformer.BoxClassTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxAccessTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxArgumentTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxArrayLiteralTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxAssignmentTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxBinaryOperationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxBooleanLiteralTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxClosureTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxComparisonOperationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxDecimalLiteralTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxExpressionInvocationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxFQNTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxFunctionInvocationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxIdentifierTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxIntegerLiteralTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxLambdaTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxMethodInvocationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxNegateOperationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxNewOperationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxNullTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxParenthesisTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxScopeTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxStringConcatTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxStringInterpolationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxStructLiteralTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxTernaryOperationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxUnaryOperationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxArgumentDeclarationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxAssertTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxBreakTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxBufferOutputTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxContinueTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxDoTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxExpressionStatementTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxForInTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxForIndexTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxFunctionDeclarationTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxIfElseTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxImportTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxParamTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxRethrowTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxReturnTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxScriptTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxSwitchTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxTemplateTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxThrowTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxTryTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.BoxWhileTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.component.BoxComponentTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.component.BoxScriptIslandTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.statement.component.BoxTemplateIslandTransformer;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
	private Map<Key, CompilationUnit>			UDFcallables	= new HashMap<Key, CompilationUnit>();
	private List<CompilationUnit>				callables		= new ArrayList<>();
	private List<Statement>						UDFDeclarations	= new ArrayList<>();

	public JavaTranspiler() {
		registry.put( BoxScript.class, new BoxScriptTransformer( this ) );
		registry.put( BoxExpressionStatement.class, new BoxExpressionStatementTransformer( this ) );

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
		registry.put( BoxExpressionInvocation.class, new BoxExpressionInvocationTransformer( this ) );
		registry.put( BoxClosure.class, new BoxClosureTransformer( this ) );
		registry.put( BoxClass.class, new BoxClassTransformer( this ) );
		registry.put( BoxParam.class, new BoxParamTransformer( this ) );

		// Templating Components
		registry.put( BoxTemplate.class, new BoxTemplateTransformer( this ) );
		registry.put( BoxBufferOutput.class, new BoxBufferOutputTransformer( this ) );
		registry.put( BoxScriptIsland.class, new BoxScriptIslandTransformer( this ) );
		registry.put( BoxTemplateIsland.class, new BoxTemplateIslandTransformer( this ) );
		registry.put( BoxComponent.class, new BoxComponentTransformer( this ) );

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
			// logger.atTrace().log( "Transforming {} node with source {} - node is {}", transformer.getClass().getSimpleName(), node.getSourceText(), javaNode
			// );
			return javaNode;
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}

	public List<Statement> getStatements() {
		return statements;
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
	public TranspiledCode transpile( BoxNode node ) throws BoxRuntimeException {
		CompilationUnit			entryPoint		= ( CompilationUnit ) transform( node );
		List<CompilationUnit>	allCallables	= getCallables();
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

	/**
	 * Get the list of UDF declarations that will get hoisted to the top
	 *
	 * @return the UDF declarations
	 */
	public List<Statement> getUDFDeclarations() {
		return UDFDeclarations;
	}

}
