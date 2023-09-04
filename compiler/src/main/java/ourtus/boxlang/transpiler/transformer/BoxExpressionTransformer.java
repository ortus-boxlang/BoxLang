package ourtus.boxlang.transpiler.transformer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.statement.BoxExpression;
import ourtus.boxlang.transpiler.BoxLangTranspiler;

public class BoxExpressionTransformer extends AbstractTransformer {

	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxExpression exprStmt = (BoxExpression)node;
		Expression expr = (Expression) BoxLangTranspiler.transform(exprStmt.getExpression());

//		Referencer.getAndInvoke(
//
//			// Object
//			Referencer.get( variablesScope.get( Key.of( "SYSTEM" ) ), Key.of( "OUT" ) ),
//							variablesScope.get(  Key.of("system") ) .get(Key.of("out") ))
//			// Method
//			"println",
//
//			// Arguments
//			new Object[] {
//
//				Concat.invoke( context, context.scopeFindNearby( Key.of( "GREETING" ), null ).value(), " world" )
//
//			}
//
//		);

		throw new IllegalStateException("not implemented");
	}
}
