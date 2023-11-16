package ortus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxArrayLiteral;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a BoxArrayLiteral Node the equivalent Java Parser AST nodes
 * The array type in BoxLang is represented by the ortus.boxlang.runtime.types.Array
 * class, which implements the Java List interface,
 * and provides several methods of construction:
 * {@snippet :
 * //empty array
 * new Array()
 * // From a native Java array
 * Array.fromArray(new Object[]{"foo","bar"})
 * // From another Java List
 * Array.fromList(List.of("foo","bar","baz"))
 * // Varargs
 * Array.of("foo","bar","baz")
 * }
 */
public class BoxArrayLiteralTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxArrayLiteralTransformer.class );

	public BoxArrayLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a BoxArrayLiteral expression
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
		BoxArrayLiteral		arrayLiteral	= ( BoxArrayLiteral ) node;
		Map<String, String>	values			= new HashMap<>();

		if ( arrayLiteral.getValues().isEmpty() ) {
			Node javaExpr = parseExpression( "new Array()", values );
			logger.info( "{} -> {}", node.getSourceText(), javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		}
		MethodCallExpr listOf = ( MethodCallExpr ) parseExpression( "List.of()", values );
		for ( BoxExpr expr : arrayLiteral.getValues() ) {
			Expression value = ( Expression ) transpiler.transform( expr, context );
			listOf.getArguments().add( value );
		}
		MethodCallExpr javaExpr = ( MethodCallExpr ) parseExpression( "Array.fromList()", values );
		javaExpr.getArguments().add( listOf );
		logger.info( "{} -> {}", node.getSourceText(), listOf );
		addIndex( listOf, node );
		return javaExpr;

	}
}
