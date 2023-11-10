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
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Source;
import ortus.boxlang.ast.SourceFile;
import ortus.boxlang.ast.expression.BoxArgument;
import ortus.boxlang.ast.expression.BoxArrayAccess;
import ortus.boxlang.ast.expression.BoxBinaryOperation;
import ortus.boxlang.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.ast.expression.BoxComparisonOperation;
import ortus.boxlang.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.ast.expression.BoxFQN;
import ortus.boxlang.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.ast.expression.BoxMethodInvocation;
import ortus.boxlang.ast.expression.BoxNegateOperation;
import ortus.boxlang.ast.expression.BoxNewOperation;
import ortus.boxlang.ast.expression.BoxObjectAccess;
import ortus.boxlang.ast.expression.BoxParenthesis;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.ast.expression.BoxStringInterpolation;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.expression.BoxTernaryOperation;
import ortus.boxlang.ast.expression.BoxUnaryOperation;
import ortus.boxlang.ast.statement.*;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.runnables.compiler.JavaSourceString;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.transpiler.transformer.Transformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;
import ortus.boxlang.transpiler.transformer.expression.BoxArgumentTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxArrayAccessTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxBinaryOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxBooleanLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxComparisonOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxDecimalLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxFQNTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxFunctionInvocationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxIdentifierTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxIntegerLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxMethodInvocationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxNegateOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxNewOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxObjectAccessTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxParenthesisTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxScopeTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxStringInterpolationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxTernaryOperationTransformer;
import ortus.boxlang.transpiler.transformer.expression.BoxUnaryOperationTransformer;
import ortus.boxlang.transpiler.transformer.indexer.CrossReference;
import ortus.boxlang.transpiler.transformer.indexer.IndexPrettyPrinterVisitor;
import ortus.boxlang.transpiler.transformer.statement.*;

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

	public JavaTranspiler() {
		registry.put( BoxScript.class, new BoxScriptTransformer( this ) );
		registry.put( BoxAssignment.class, new BoxAssignmentTransformer() );
		registry.put( BoxArrayAccess.class, new BoxArrayAccessTransformer() );
		registry.put( BoxExpression.class, new BoxExpressionTransformer() );

		// Expressions
		registry.put( BoxIdentifier.class, new BoxIdentifierTransformer() );
		registry.put( BoxScope.class, new BoxScopeTransformer() );
		// Literals
		registry.put( BoxStringLiteral.class, new BoxStringLiteralTransformer() );
		registry.put( BoxIntegerLiteral.class, new BoxIntegerLiteralTransformer() );
		registry.put( BoxBooleanLiteral.class, new BoxBooleanLiteralTransformer() );
		registry.put( BoxDecimalLiteral.class, new BoxDecimalLiteralTransformer() );
		registry.put( BoxStringInterpolation.class, new BoxStringInterpolationTransformer() );
		registry.put( BoxArgument.class, new BoxArgumentTransformer() );
		registry.put( BoxFQN.class, new BoxFQNTransformer() );

		registry.put( BoxParenthesis.class, new BoxParenthesisTransformer() );
		registry.put( BoxBinaryOperation.class, new BoxBinaryOperationTransformer() );
		registry.put( BoxTernaryOperation.class, new BoxTernaryOperationTransformer() );
		registry.put( BoxNegateOperation.class, new BoxNegateOperationTransformer() );
		registry.put( BoxComparisonOperation.class, new BoxComparisonOperationTransformer() );
		registry.put( BoxUnaryOperation.class, new BoxUnaryOperationTransformer() );
		registry.put( BoxObjectAccess.class, new BoxObjectAccessTransformer() );

		registry.put( BoxMethodInvocation.class, new BoxMethodInvocationTransformer() );
		registry.put( BoxFunctionInvocation.class, new BoxFunctionInvocationTransformer() );
		registry.put( BoxLocalDeclaration.class, new BoxLocalDeclarationTransformer() );
		registry.put( BoxIfElse.class, new BoxIfElseTransformer() );
		registry.put( BoxWhile.class, new BoxWhileTransformer() );
		registry.put( BoxDo.class, new BoxDoTransformer() );
		registry.put( BoxSwitch.class, new BoxSwitchTransformer() );
		registry.put( BoxBreak.class, new BoxBreakTransformer() );
		registry.put( BoxContinue.class, new BoxContinueTransformer() );
		registry.put( BoxForIn.class, new BoxForInTransformer() );
		registry.put( BoxForIndex.class, new BoxForIndexTransformer() );
		registry.put( BoxAssert.class, new BoxAssertTransformer() );
		registry.put( BoxTry.class, new BoxTryTransformer() );
		registry.put( BoxThrow.class, new BoxThrowTransformer() );
		registry.put( BoxNewOperation.class, new BoxNewOperationTransformer() );
		registry.put( BoxFunctionDeclaration.class, new BoxFunctionDeclarationTransformer() );
		registry.put( BoxReturn.class, new BoxReturnTransformer() );
		registry.put( BoxRethrow.class, new BoxRethrowTransformer() );
		registry.put( BoxImport.class, new BoxImportTransformer() );

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
		return JavaTranspiler.transform( node, TransformerContext.NONE );
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
		// Workaround for regressin where static calls to this class to not initialize the registry
		if ( registry.size() == 0 ) {
			new JavaTranspiler();
		}
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
	 * @deprecated will be replaced by transpileMany
	 *
	 * @see CompilationUnit
	 */
	public CompilationUnit transpileToJava( BoxNode node ) throws IllegalStateException {
		BoxScript		source		= ( BoxScript ) node;
		CompilationUnit	javaClass	= ( CompilationUnit ) transform( source );

		statements.clear();

		String				className		= getClassName( source.getPosition().getSource() );
		MethodDeclaration	invokeMethod	= javaClass.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "_invoke" ).get( 0 );
		FieldDeclaration	imports			= javaClass.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "imports" ).get();

		for ( BoxStatement statement : source.getStatements() ) {
			if ( statement instanceof BoxFunctionDeclaration ) {
				// a function declaration generate

			} else {
				Node javaStmt = transform( statement );
				if ( javaStmt instanceof BlockStmt ) {
					BlockStmt stmt = ( BlockStmt ) javaStmt;
					stmt.getStatements().stream().forEach( it -> {
						invokeMethod.getBody().get().addStatement( it );
						statements.add( it );
					} );
				}
				if ( javaStmt instanceof MethodCallExpr ) {
					MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
					imp.getArguments().add( ( MethodCallExpr ) javaStmt );
				} else {
					invokeMethod.getBody().get().addStatement( ( Statement ) javaStmt );
					statements.add( ( Statement ) javaStmt );
				}
			}
		}

		IndexPrettyPrinterVisitor visitor = new IndexPrettyPrinterVisitor( new DefaultPrinterConfiguration() );
		javaClass.accept( visitor, null );
		this.crossReferences.addAll( visitor.getCrossReferences() );
		return javaClass;
	}

	/**
	 * Deprecated
	 *
	 * @param node
	 *
	 * @return
	 *
	 * @throws IllegalStateException
	 *
	 * @deprecated will be replaced by transpileMany
	 */
	public List<CompilationUnit> transpileMany( BoxNode node ) throws IllegalStateException {
		List<CompilationUnit>	compilationUnits	= new ArrayList<>();
		BoxScript				source				= ( BoxScript ) node;
		CompilationUnit			javaClass			= ( CompilationUnit ) transform( source );

		String					className			= getClassName( source.getPosition().getSource() );
		MethodDeclaration		invokeMethod		= javaClass.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "_invoke" ).get( 0 );

		FieldDeclaration		imports				= javaClass.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "imports" ).get();

		for ( BoxStatement statement : source.getStatements() ) {
			Node function = transform( statement );
			if ( statement instanceof BoxFunctionDeclaration ) {
				// a function declaration generate
				compilationUnits.add( ( CompilationUnit ) function );
				Node registrer = transform( statement, TransformerContext.REGISTER );
				invokeMethod.getBody().get().addStatement( 0, ( Statement ) registrer );

			} else {
				if ( function instanceof BlockStmt ) {
					BlockStmt stmt = ( BlockStmt ) function;
					stmt.getStatements().stream().forEach( it -> {
						invokeMethod.getBody().get().addStatement( it );
						statements.add( it );
					} );
				} else if ( function instanceof MethodCallExpr ) {
					MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
					imp.getArguments().add( ( MethodCallExpr ) function );
				} else {
					invokeMethod.getBody().get().addStatement( ( Statement ) function );
					statements.add( ( Statement ) function );
				}
			}
		}

		IndexPrettyPrinterVisitor visitor = new IndexPrettyPrinterVisitor( new DefaultPrinterConfiguration() );
		javaClass.accept( visitor, null );
		this.crossReferences.addAll( visitor.getCrossReferences() );
		compilationUnits.add( javaClass );
		return compilationUnits;
	}

	public List<Statement> getStatements() {
		return statements;
	}

	public String getStatementsAsString() {
		return getStatements().stream().map( it -> it.toString() )
		    .collect( Collectors.joining( "\n" ) );
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
				if ( packg.startsWith( "/" ) ) {
					packg = packg.substring( 1 );
				}
				// TODO: This needs a lot more work. There are tons of disallowed edge cases such as a folder that is a number.
				// We probably need to iterate each path segment and clean or remove as neccessary to make it a valid package name.
				// Also, I'd like cfincluded files to use the relative path as the package name, which will require some refactoring.

				// Take out periods in folder names
				packg	= packg.replaceAll( "\\.", "" );
				// Replace / with .
				packg	= packg.replaceAll( "/", "." );
				// Remove any : from Windows drives
				packg	= packg.replaceAll( ":", "" );
				// Replace \ with .
				packg	= packg.replaceAll( "\\\\", "." );
				// Remove any non alpha-numeric chars.
				packg	= packg.replaceAll( "[^a-zA-Z0-9\\\\.]", "" );
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

	public BoxNode resloveReference( int lineNumber ) {
		for ( var entry : crossReferences ) {
			if ( entry.destination.begin.line == lineNumber ) {
				return entry.origin;
			}
		}
		return null;
	}

	@Override
	public TranspiledCode transpile( BoxNode node ) throws ApplicationException {
		BoxScript				source			= ( BoxScript ) node;
		CompilationUnit			entryPoint		= ( CompilationUnit ) transform( source );
		List<CompilationUnit>	callables		= new ArrayList<>();

		String					className		= getProperty( "classname" ) != null ? getProperty( "classname" )
		    : getClassName( source.getPosition().getSource() );

		MethodDeclaration		invokeMethod	= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "_invoke" ).get( 0 );

		FieldDeclaration		imports			= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "imports" ).get();

		for ( BoxStatement statement : source.getStatements() ) {
			Node function = transform( statement );
			if ( statement instanceof BoxFunctionDeclaration ) {
				// a function declaration generate
				callables.add( ( CompilationUnit ) function );
				Node registrer = transform( statement, TransformerContext.REGISTER );
				invokeMethod.getBody().get().addStatement( 0, ( Statement ) registrer );

			} else {
				if ( function instanceof BlockStmt ) {
					BlockStmt stmt = ( BlockStmt ) function;
					stmt.getStatements().stream().forEach( it -> {
						invokeMethod.getBody().get().addStatement( it );
						statements.add( it );
					} );
				} else if ( function instanceof MethodCallExpr ) {
					MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
					imp.getArguments().add( ( MethodCallExpr ) function );
				} else {
					invokeMethod.getBody().get().addStatement( ( Statement ) function );
					statements.add( ( Statement ) function );
				}
			}
		}
		/* if has a return type add the return statement, replace with the Box AST nodes suggested */
		if ( ! ( invokeMethod.getType() instanceof com.github.javaparser.ast.type.VoidType ) ) {
			invokeMethod.getBody().get().addStatement( new ReturnStmt( new NullLiteralExpr() ) );
		}

		IndexPrettyPrinterVisitor visitor = new IndexPrettyPrinterVisitor( new DefaultPrinterConfiguration() );
		entryPoint.accept( visitor, null );
		this.crossReferences.addAll( visitor.getCrossReferences() );

		return new TranspiledCode( entryPoint, callables );
	}
}
