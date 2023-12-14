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

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxBinaryOperation;
import ortus.boxlang.ast.expression.BoxBinaryOperator;
import ortus.boxlang.ast.expression.BoxComparisonOperation;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.expression.BoxUnaryOperation;
import ortus.boxlang.ast.expression.BoxUnaryOperator;
import ortus.boxlang.ast.statement.BoxAnnotation;
import ortus.boxlang.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.transpiler.Transpiler;
import ortus.boxlang.transpiler.transformer.indexer.BoxLangCrossReferencer;
import ortus.boxlang.transpiler.transformer.indexer.BoxLangCrossReferencerDefault;

/**
 * Abstract Transformer class
 * Implements common functionality used by all the transformer sub classes
 */
public abstract class AbstractTransformer implements Transformer {

	protected Transpiler					transpiler;
	protected static JavaParser				javaParser		= new JavaParser();
	protected static BoxLangCrossReferencer	crossReferencer	= new BoxLangCrossReferencerDefault();

	public AbstractTransformer( Transpiler transpiler ) {
		this.transpiler = transpiler;
	}

	@Override
	public abstract Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException;

	public Node transform( BoxNode node ) throws IllegalStateException {
		return this.transform( node, TransformerContext.NONE );
	}

	/**
	 * Returns the Java Parser AST nodes for the given template
	 *
	 * @param template a string template with the expression to parse
	 * @param values   a map of values to be replaced in the template
	 *
	 * @return the Java Parser AST representation of the expression
	 */
	protected Node parseExpression( String template, Map<String, String> values ) {
		String code = PlaceholderHelper.resolve( template, values );
		try {
			ParseResult<Expression> result = javaParser.parseExpression( code );
			if ( !result.isSuccessful() ) {
				// System.out.println( code );
				throw new IllegalStateException( result.toString() );
			}
			return result.getResult().get();
		} catch ( Throwable e ) {
			throw new RuntimeException( "Error parsing expression: " + code, e );
		}

	}

	/**
	 * Returns the Java Parser AST for the given template
	 *
	 * @param template a string template with the statement to parse
	 * @param values   a map of values to be replaced in the template
	 *
	 * @return the Java Parser AST representation of the statement
	 */
	protected Node parseStatement( String template, Map<String, String> values ) {
		String					code	= PlaceholderHelper.resolve( template, values );
		ParseResult<Statement>	result	= javaParser.parseStatement( code );
		if ( !result.isSuccessful() ) {
			throw new IllegalStateException( result.toString() );
		}
		return result.getResult().get();
	}

	/**
	 * Create a Key instance out of any expression. May optimize requests for the same key more than once in a template
	 *
	 * @param expr expression to be turned into a Key. May be a literal, or expression
	 *
	 * @return
	 */
	protected Node createKey( BoxExpr expr ) {
		// TODO: optimize for the same key more than once in a template
		NameExpr		nameExpr		= new NameExpr( "Key" );
		MethodCallExpr	methodCallExpr	= new MethodCallExpr( nameExpr, "of" );
		methodCallExpr.addArgument( ( Expression ) transpiler.transform( expr ) );

		return methodCallExpr;
	}

	/**
	 * Create a Key instance from a string literal. May optimize requests for the same key more than once in a template
	 *
	 * @param expr expression to be turned into a Key. May be a literal, or expression
	 *
	 * @return
	 */
	protected Node createKey( String expr ) {
		return createKey( new BoxStringLiteral( expr, null, expr ) );
	}

	/**
	 * Detects if a statement requires a BooleanCaster
	 *
	 * @param condition the expression to evaluate
	 *
	 * @return true if the BooleanCaster is required
	 */
	protected boolean requiresBooleanCaster( BoxExpr condition ) {
		if ( condition instanceof BoxBinaryOperation op ) {
			if ( op.getOperator() == BoxBinaryOperator.Or )
				return false;
			if ( op.getOperator() == BoxBinaryOperator.And )
				return false;
			if ( op.getOperator() == BoxBinaryOperator.Contains )
				return false;
			if ( op.getOperator() == BoxBinaryOperator.InstanceOf )
				return false;
			if ( op.getOperator() == BoxBinaryOperator.NotContains )
				return false;
			if ( op.getOperator() == BoxBinaryOperator.Xor )
				return false;
		}
		if ( condition instanceof BoxUnaryOperation op ) {
			if ( op.getOperator() == BoxUnaryOperator.Not )
				return false;
		}
		if ( condition instanceof BoxComparisonOperation op ) {
			return false;
		}
		return true;
	}

	/**
	 * Add cross-reference index entry
	 *
	 * @param javaNode Java Parser Node
	 * @param boxNode  BoxLang Node
	 *
	 * @return the Java Parser Node
	 */
	protected Node addIndex( Node javaNode, BoxNode boxNode ) {
		if ( crossReferencer != null ) {
			crossReferencer.storeReference( javaNode, boxNode );
		}
		return javaNode;
	}

	/**
	 * Transforms a collection of documentation annotations in a BoxLang Struct
	 *
	 * @param documentation list of documentation annotation
	 *
	 * @return an Expression node
	 */
	protected Expression transformDocumentation( List<BoxDocumentationAnnotation> documentation ) {
		List<Expression> members = new ArrayList<>();
		documentation.forEach( doc -> {
			Map<String, String>	values			= Map.ofEntries( Map.entry( "key", doc.getKey().getValue() ) );
			MethodCallExpr		annotationKey	= ( MethodCallExpr ) parseExpression( "Key.of( \"${key}\" )", values );
			members.add( annotationKey );
			Expression value = ( Expression ) transpiler.transform( doc.getValue() );
			members.add( value );
		} );
		if ( members.isEmpty() ) {
			return ( Expression ) parseExpression( "Struct.EMPTY", new HashMap<>() );
		} else {
			MethodCallExpr documentationStruct = ( MethodCallExpr ) parseExpression( "Struct.of()", new HashMap<>() );
			documentationStruct.getArguments().addAll( members );
			return documentationStruct;
		}
	}

	/**
	 * Transforms a collection of annotations in a BoxLang Struct
	 *
	 * @param annotations list of annotation
	 *
	 * @return an Expression node
	 */
	protected Expression transformAnnotations( List<BoxAnnotation> annotations ) {
		List<Expression> members = new ArrayList<>();
		annotations.forEach( annotation -> {
			Map<String, String>	values			= Map.ofEntries( Map.entry( "key", annotation.getKey().getValue() ) );
			MethodCallExpr		annotationKey	= ( MethodCallExpr ) parseExpression( "Key.of( \"${key}\" )", values );
			;
			members.add( annotationKey );
			Expression value = ( Expression ) transpiler.transform( annotation.getValue() );
			members.add( value );
		} );
		if ( annotations.isEmpty() ) {
			return ( Expression ) parseExpression( "Struct.EMPTY", new HashMap<>() );
		} else {
			MethodCallExpr annotationStruct = ( MethodCallExpr ) parseExpression( "Struct.of()", new HashMap<>() );
			annotationStruct.getArguments().addAll( members );
			return annotationStruct;
		}
	}
}
