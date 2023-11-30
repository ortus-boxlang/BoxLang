package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxTernaryOperation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxTernaryOperationTransformer extends AbstractTransformer {

	public BoxTernaryOperationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxTernaryOperation	operation	= ( BoxTernaryOperation ) node;
		Expression			condition	= ( Expression ) resolveScope( transpiler.transform( operation.getCondition() ), TransformerContext.RIGHT );
		Expression			whenTrue	= ( Expression ) transpiler.transform( operation.getWhenTrue() );
		Expression			whenFalse	= ( Expression ) transpiler.transform( operation.getWhenFalse() );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "condition", condition.toString() );
												put( "whenTrue", whenTrue.toString() );
												put( "whenFalse", whenFalse.toString() );
												put( "contextName", transpiler.peekContextName() );
											}
										};

		String				template	= "Ternary.invoke(${condition},${whenTrue},${whenFalse})";

		return parseExpression( template, values );
	}
}
