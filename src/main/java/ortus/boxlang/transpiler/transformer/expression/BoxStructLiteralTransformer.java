package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxStructLiteral;
import ortus.boxlang.ast.expression.BoxStructType;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxStructUnorderedLiteral Node the equivalent Java Parser AST nodes
 * {@snippet :
 *
 * }
 */
public class BoxStructLiteralTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxArrayLiteralTransformer.class );

	public BoxStructLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a BoxStructOrderedLiteral expression
	 *
	 * @param node    a BoxNewOperation instance
	 * @param context transformation context
	 *
	 * @return generates the corresponding of runtime representation
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStructLiteral	structLiteral	= ( BoxStructLiteral ) node;
		Map<String, String>	values			= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
												}
											};
		boolean				empty			= structLiteral.getValues().isEmpty();

		if ( structLiteral.getType() == BoxStructType.Unordered ) {
			if ( empty ) {
				Node javaExpr = parseExpression( "new Struct( Struct.Type.LINKED )", values );
				logger.info( "{} -> {}", node.getSourceText(), javaExpr );
				addIndex( javaExpr, node );
				return javaExpr;
			}

			MethodCallExpr javaExpr = ( MethodCallExpr ) parseExpression( "Struct.of()", values );
			for ( BoxExpr expr : structLiteral.getValues() ) {
				Expression value = ( Expression ) transpiler.transform( expr, context );
				javaExpr.getArguments().add( value );
			}
			logger.info( "{} -> {}", node.getSourceText(), javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		} else {
			if ( empty ) {
				Node javaExpr = parseExpression( "new Struct()", values );
				logger.info( "{} -> {}", node.getSourceText(), javaExpr );
				addIndex( javaExpr, node );
				return javaExpr;
			}

			MethodCallExpr javaExpr = ( MethodCallExpr ) parseExpression( "Struct.of()", values );
			for ( BoxExpr expr : structLiteral.getValues() ) {
				Expression value = ( Expression ) transpiler.transform( expr, context );
				javaExpr.getArguments().add( value );
			}
			logger.info( "{} -> {}", node.getSourceText(), javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		}

	}
}
