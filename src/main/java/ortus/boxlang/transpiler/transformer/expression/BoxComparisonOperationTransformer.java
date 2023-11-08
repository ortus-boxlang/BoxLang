package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxComparisonOperation;
import ortus.boxlang.ast.expression.BoxComparisonOperator;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxComparisonOperation Node the equivalent Java Parser AST nodes
 */
public class BoxComparisonOperationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxComparisonOperationTransformer.class );

	/**
	 * Transform BoxComparisonOperation operator
	 *
	 * @param node    a BoxComparisonOperation instance
	 * @param context transformation context
	 *
	 * @return generates a Java Parser Method invocation to the corresponding runtime implementation
	 *
	 * @throws IllegalStateException
	 *
	 * @see BoxComparisonOperation
	 * @see BoxComparisonOperator foe the supported comparision operators
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxComparisonOperation	operation	= ( BoxComparisonOperation ) node;
		Expression				left		= ( Expression ) resolveScope( JavaTranspiler.transform( operation.getLeft() ), context );
		Expression				right		= ( Expression ) resolveScope( JavaTranspiler.transform( operation.getRight() ), context );

		Map<String, String>		values		= new HashMap<>() {

												{
													put( "left", left.toString() );
													put( "right", right.toString() );

												}
											};
		String					template	= "";

		if ( operation.getOperator() == BoxComparisonOperator.Equal ) {
			template = "EqualsEquals.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.NotEqual ) {
			template = "!EqualsEquals.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.TEqual ) {
			template = "EqualsEqualsEquals.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.GreaterThan ) {
			template = "GreaterThan.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.GreaterThanEquals ) {
			template = "GreaterThanEqual.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.LessThan ) {
			template = "LessThan.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.LesslThanEqual ) {
			template = "LessThanEqual.invoke(${left},${right})";
		} else {
			throw new IllegalStateException( "not implemented" );
		}
		Node javaExpr = parseExpression( template, values );
		logger.info( node.getSourceText() + " (" + context.name() + ") -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}

}
