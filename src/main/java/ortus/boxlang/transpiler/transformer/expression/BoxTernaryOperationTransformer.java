package ortus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxTernaryOperation;
import ortus.boxlang.transpiler.BoxLangTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxTernaryOperationTransformer extends AbstractTransformer {

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxTernaryOperation	operation	= ( BoxTernaryOperation ) node;
		Expression			condition	= ( Expression ) BoxLangTranspiler.transform( operation.getCondition() /* , TransformerContext.DEREFERENCING */ );
		Expression			whenTrue	= ( Expression ) BoxLangTranspiler.transform( operation.getWhenTrue() );
		Expression			whenFalse	= ( Expression ) BoxLangTranspiler.transform( operation.getWhenFalse() );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "condition", condition.toString() );
												put( "whenTrue", whenTrue.toString() );
												put( "whenFalse", whenFalse.toString() );
											}
										};
		if ( condition instanceof NameExpr name ) {
			String tmp = "context.scopeFindNearby( Key.of( \"" + name + "\" ), variablesScope).value()";
			values.put( "condition", tmp );
		}

		String template = "Ternary.invoke(${condition},${whenTrue},${whenFalse})";

		return parseExpression( template, values );
	}
}
