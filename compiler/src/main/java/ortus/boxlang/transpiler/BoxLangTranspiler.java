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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import ortus.boxlang.ast.*;
import ortus.boxlang.ast.expression.*;
import ortus.boxlang.ast.statement.*;
import ortus.boxlang.executor.JavaSourceString;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.transpiler.transformer.*;
import ortus.boxlang.transpiler.transformer.expression.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.transpiler.transformer.indexer.CrossReference;
import ortus.boxlang.transpiler.transformer.indexer.IndexPrettyPrinterVisitor;
import ortus.boxlang.transpiler.transformer.statement.*;

import javax.tools.*;

/**
 * BoxLang AST to Java AST transpiler
 * The registry maps a AST node to the corresponding Transformer Java class instance.
 * Each transformer implements the logic to convert the BoxLang AST nodes into Java
 * AST nodes.
 *
 */
public class BoxLangTranspiler {

	static Logger								logger		= LoggerFactory.getLogger( BoxLangTranspiler.class );

	private static HashMap<Class, Transformer>	registry	= new HashMap<>() {

																{

																	put( BoxScript.class, new BoxScriptTransformer() );
																	put( BoxAssignment.class, new BoxAssignmentTransformer() );
																	put( BoxArrayAccess.class, new BoxArrayAccessTransformer() );
																	put( BoxExpression.class, new BoxExpressionTransformer() );

																	// Expressions
																	put( BoxIdentifier.class, new BoxIdentifierTransformer() );
																	put( BoxScope.class, new BoxScopeTransformer() );
																	// Literals
																	put( BoxStringLiteral.class, new BoxStringLiteralTransformer() );
																	put( BoxIntegerLiteral.class, new BoxIntegerLiteralTransformer() );
																	put( BoxBooleanLiteral.class, new BoxBooleanLiteralTransformer() );
																	put( BoxDecimalLiteral.class, new BoxDecimalLiteralTransformer() );
																	put( BoxStringInterpolation.class, new BoxStringInterpolationTransformer() );
																	put( BoxArgument.class, new BoxArgumentTransformer() );
																	put( BoxFQN.class, new BoxFQNTransformer() );

																	put( BoxParenthesis.class, new BoxParenthesisTransformer() );
																	put( BoxBinaryOperation.class, new BoxBinaryOperationTransformer() );
																	put( BoxTernaryOperation.class, new BoxTernaryOperationTransformer() );
																	put( BoxNegateOperation.class, new BoxNegateOperationTransformer() );
																	put( BoxComparisonOperation.class, new BoxComparisonOperationTransformer() );
																	put( BoxUnaryOperation.class, new BoxUnaryOperationTransformer() );
																	put( BoxObjectAccess.class, new BoxObjectAccessTransformer() );

																	put( BoxMethodInvocation.class, new BoxMethodInvocationTransformer() );
																	put( BoxFunctionInvocation.class, new BoxFunctionInvocationTransformer() );
																	put( BoxLocalDeclaration.class, new BoxLocalDeclarationTransformer() );
																	put( BoxIfElse.class, new BoxIfElseTransformer() );
																	put( BoxWhile.class, new BoxWhileTransformer() );
																	put( BoxDo.class, new BoxDoTransformer() );
																	put( BoxSwitch.class, new BoxSwitchTransformer() );
																	put( BoxBreak.class, new BoxBreakTransformer() );
																	put( BoxContinue.class, new BoxContinueTransformer() );
																	put( BoxForIn.class, new BoxForInTransformer() );
																	put( BoxForIndex.class, new BoxForIndexTransformer() );
																	put( BoxAssert.class, new BoxAssertTransformer() );
																	put( BoxTry.class, new BoxTryTransformer() );
																	put( BoxThrow.class, new BoxThrowTransformer() );
																	put( BoxNewOperation.class, new BoxNewOperationTransformer() );

																}
															};

	private List<Statement>						statements	= new ArrayList<>();
	private List<CrossReference>				crossReferences	= new ArrayList<>();

	public BoxLangTranspiler() {
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
	public static Node transform( BoxNode node ) throws IllegalStateException {
		return BoxLangTranspiler.transform( node, TransformerContext.NONE );
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
	public static Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			Node javaNode = transformer.transform( node, context );
			// logger.info(transformer.getClass().getSimpleName() + " : " + node.getSourceText() + " -> " + javaNode );
			return javaNode;
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}

	/**
	 * Transpile a BoxLang AST into a Java Parser AST
	 *
	 * @return a Java Parser CompilationUnit representing the equivalent Java code
	 *
	 * @throws IllegalStateException
	 *
	 * @see CompilationUnit
	 */
	public CompilationUnit transpile( BoxNode node ) throws IllegalStateException {
		BoxScript			source			= ( BoxScript ) node;
		CompilationUnit		javaClass		= ( CompilationUnit ) transform( source );

		String				className		= getClassName( source.getPosition().getSource() );
		MethodDeclaration	invokeMethod	= javaClass.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "invoke" ).get( 0 );

		for ( BoxStatement statement : source.getStatements() ) {
			Node javaStmt = transform( statement );
			if ( javaStmt instanceof BlockStmt ) {
				BlockStmt stmt = ( BlockStmt ) javaStmt;
				stmt.getStatements().stream().forEach( it -> {
					invokeMethod.getBody().get().addStatement( it );
					statements.add( it );
				} );
			} else {
				invokeMethod.getBody().get().addStatement( ( Statement ) javaStmt );
				statements.add( ( Statement ) javaStmt );
			}
		}

		IndexPrettyPrinterVisitor visitor = new IndexPrettyPrinterVisitor( new DefaultPrinterConfiguration() );
		javaClass.accept( visitor, null );
		this.crossReferences.addAll(visitor.getCrossReferences());
		return javaClass;
	}

	public List<Statement> getStatements() {
		return statements;
	}

	public List<CrossReference> getCrossReferences() {
		return crossReferences;
	}

	/**
	 * Transforms the filename into the class name
	 *
	 * @param source
	 *
	 * @return returns the class name according the name conventions Test.ext - Test$ext
	 */
	public static String getClassName( Source source ) {
		if ( source instanceof SourceFile file && file.getFile() != null ) {
			String name = file.getFile().getName().replace( ".", "$" );
			name = name.substring( 0, 1 ).toUpperCase() + name.substring( 1 );
			return name;
		}
		return "TestClass";
	}

	/**
	 * Transforms the path into the package name
	 *
	 * @param source
	 *
	 * @return returns the class name according the name conventions Test.ext - Test$ext
	 */
	public static String getPackageName( Source source ) throws IllegalStateException {
		if ( source instanceof SourceFile file && file.getFile() != null ) {
			try {
				File	path	= file.getFile().getCanonicalFile();
				String	packg	= path.toString().replace( File.separatorChar + path.getName(), "" );
				packg = packg.substring( 1 ).replaceAll( "/", "." );
				return packg;
			} catch ( IOException e ) {
				throw new IllegalStateException( e );
			}
		}
		return "ortus.test";
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
		String								pkg				= cu.getPackageDeclaration().get().getName().toString();
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
		StandardJavaFileManager				stdFileManager	= compiler.getStandardFileManager( null, null, null );

		List<String>						options			= new ArrayList<>(
		    List.of( "-g",
		        "-cp",
		        compilerClassPath,
		        "-d",
		        outputPath
		    )
		);

		JavaCompiler.CompilationTask		task			= compiler.getTask( output, stdFileManager, diagnostics, options, null, sourceFiles );
		boolean								result			= task.call();

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

			rt.executeTemplate( ( BaseTemplate ) instance );

			rt.shutdown();

		} catch ( Throwable e ) {
			throw e;
		}

	}


	public BoxNode resloveReference(int lineNumber) {
		for (var entry : crossReferences) {
			if(entry.destination.begin.line == lineNumber) {
				return entry.origin;
			}
		}
		return null;
	}
}
