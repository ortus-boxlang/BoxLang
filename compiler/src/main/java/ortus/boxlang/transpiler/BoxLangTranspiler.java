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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitor;
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
import ortus.boxlang.ast.statement.BoxAssert;
import ortus.boxlang.ast.statement.BoxAssignment;
import ortus.boxlang.ast.statement.BoxBreak;
import ortus.boxlang.ast.statement.BoxContinue;
import ortus.boxlang.ast.statement.BoxDo;
import ortus.boxlang.ast.statement.BoxExpression;
import ortus.boxlang.ast.statement.BoxForIn;
import ortus.boxlang.ast.statement.BoxForIndex;
import ortus.boxlang.ast.statement.BoxIfElse;
import ortus.boxlang.ast.statement.BoxLocalDeclaration;
import ortus.boxlang.ast.statement.BoxSwitch;
import ortus.boxlang.ast.statement.BoxThrow;
import ortus.boxlang.ast.statement.BoxTry;
import ortus.boxlang.ast.statement.BoxWhile;
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
import ortus.boxlang.transpiler.transformer.indexer.IndexPrettyPrinterVisitor;
import ortus.boxlang.transpiler.transformer.statement.BoxAssertTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxAssignmentTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxBreakTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxContinueTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxDoTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxExpressionTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxForInTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxForIndexTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxIfElseTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxLocalDeclarationTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxScriptTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxSwitchTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxThrowTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxTryTransformer;
import ortus.boxlang.transpiler.transformer.statement.BoxWhileTransformer;

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

		VoidVisitor<Void> visitor = new IndexPrettyPrinterVisitor( new DefaultPrinterConfiguration() );
		javaClass.accept( visitor, null );

		return javaClass;
	}

	public List<Statement> getStatements() {
		return statements;
	}

	public String getStatemesAsString() {
		List<Statement> statements = getStatements();
		return statements.stream().map( it -> it.toString() )
		    .collect( Collectors.joining( "\n" ) );
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
				packg	= packg.replaceAll( "/", "." );
				packg	= packg.replaceAll( ":", "" );
				packg	= packg.replaceAll( "\\\\", "." );
				return packg;
			} catch ( IOException e ) {
				throw new IllegalStateException( e );
			}
		}
		return "TestClass";
	}
}
